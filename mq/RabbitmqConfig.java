package com.cigit.config.mq;

import com.cigit.utils.RSAToolsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.util.StringUtils;

/**
 * @Author:zhuzhou
 * @Date: 2019/9/24  14:52
 **/
@Configuration
public class RabbitmqConfig {
    private final static Logger LOGGER = LoggerFactory.getLogger(RabbitmqConfig.class);
    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.username:#{null}}")
    private String userName;
    @Value("${spring.rabbitmq.password:#{null}}")
    private String password;
    @Value("${spring.rabbitmq.password.public_key:#{null}}")
    private String publicKey;
    @Value("${rabbitmq.fanout.exchange_name}")
    private  String  fanout_exchange_name;
    @Bean
    public FanoutExchange fanoutExchange(){
        LOGGER.info("交换机实例创建成功:{}",fanout_exchange_name);
        return new FanoutExchange(fanout_exchange_name);
    }
    /**
     * 设置连接信息
     * @return
     * @throws Exception
     */
    @Bean
    public ConnectionFactory connectionFactory() throws Exception {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        if(!StringUtils.isEmpty(userName)){
            password = RSAToolsConfig.decrypt(publicKey, password);
            connectionFactory.setUsername(userName);
            connectionFactory.setPassword(password);
        }else{
            connectionFactory.setHost(host);
            connectionFactory.setConnectionTimeout(5000);
            connectionFactory.setCloseTimeout(5000);
           // 发送到exchange的确认
            connectionFactory.setPublisherConfirms(true); // 必须要设置
            // // 发送到confirm的确认
            connectionFactory.setPublisherReturns(true);
        }
        return connectionFactory;
    }


}
