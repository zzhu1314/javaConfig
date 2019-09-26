package com.cigit.config.mq;


import com.cigit.config.ConstantCollections;
import com.cigit.utils.SecretUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * 如果消息没有到exchange,则confirm回调,ack=false
 * 
 * 如果消息到达exchange,则confirm回调,ack=true
 * 
 * exchange到queue成功,则不回调return
 * 
 * exchange到queue失败,则回调return(需设置mandatory=true,否则不回回调,消息就丢了)
 * 
 * @author WangJian
 *
 */
@Service
public class ReviewMsgSender implements RabbitTemplate.ReturnCallback, ConfirmCallback {

	protected static Logger LOGGER = LoggerFactory.getLogger(ReviewMsgSender.class);

	private ConcurrentHashMap<String, String> msgs = new ConcurrentHashMap<>();

	@Autowired
	private RabbitTemplate rabbitTemplate;

	// 如果消息队列满了，就放弃发送，返回失败，由外部处理发送失败的消息
	public boolean send(String exchange, String msg) {
		CorrelationData correlationData = new CorrelationData(SecretUtils.uuid32());
		if ((msgs.size() + 1) > ConstantCollections.MAX_RABBITMQ_HASH_MAP_QUEUE_SIZE) {
			return false;
		}
		msgs.put(correlationData.getId(), msg);
		rabbitTemplate.setReturnCallback(this);
		rabbitTemplate.setConfirmCallback(this);
		LOGGER.info("{} is sending", correlationData.getId());
		this.rabbitTemplate.convertAndSend(exchange, "", msg, correlationData);
		return true;
	}

	// 消息未发送到exchange 绑定的queue上面
	@Override
	public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {

		LOGGER.error("msg:{}send error,replayCode:{},replyText:{},exchange name:{},routintkey:{}", message.toString(),
				replyCode, replyText, exchange, routingKey);

		handleSendFailMsg(new String(message.getBody()));

	}
	// 判断消息是否发送到exchange上面
	@Override
	public void confirm(CorrelationData correlationData, boolean ack, String cause) {
		if (ack) {
			LOGGER.info("{} send success", correlationData.getId());
			msgs.remove(correlationData.getId());
		} else {

			handleSendFailMsg(msgs.remove(correlationData.getId()));
		}

	}

	private void handleSendFailMsg(String msg) {
		// 消息发送失败,保存本地重发
		LOGGER.error("mq send msg error:{}",msg);
	}
}
