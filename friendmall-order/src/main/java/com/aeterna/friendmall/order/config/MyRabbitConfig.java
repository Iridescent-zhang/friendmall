package com.aeterna.friendmall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : config
 * @ClassName : .java
 * @createTime : 2024/8/18 17:24
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Configuration
public class MyRabbitConfig {

//    @Autowired
    private RabbitTemplate rabbitTemplate;

    // TODO RabbitMQ还需要配置点其他东西
    @Primary  // 由于@Autowired容易出现循环依赖，所以自己配置一个rabbitTemplate放到容器中，并且设置为@primary
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        initRabbitTemplate();
        return rabbitTemplate;
    }

    /**
     * 使用JSON序列化机制进行消息序列化
     */
    @Bean
    public MessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制 RabbitTemplate
     * 1、消息抵达服务器的确认回调 ConfirmCallback【服务器Broker收到消息时回调】
     *        1）publisher-confirms: true
     *        2）rabbitTemplate.setConfirmCallback()
     * 2、消息抵达队列的确认回调 ReturnConfirm【注意这个是消息没有抵达队列时回调，顺利到达不会回调，类似于失败回调】
     *        1）publisher-returns: true
     *        2）mandatory: true
     *        3）rabbitTemplate.setReturnCallback()
     * 3、消费端确认（保证每个消息被正确消费，此时才可以在队列中删除消息）
     *        1）默认是自动确认的，只要消息接收到，客户端就会自动确认，之后服务端会移除这个消息
     *           潜在问题：
     *              我们收到消息自动回复给服务器ack，但是只有一个消息处理成功我这个客户端就宕机了，结果服务器已经删除了这个消息，出现消息丢失。
     *              意思就是即便消息传给消费者端了，如果消费者没有处理这个消息，那这个消息还是不能删
     *           因此我们要手动确认，处理一个确认一个，然后服务端再删除。手动模式下只要我们没有明确告诉MQ签收了消息，消息就一直 unacked，一直呆在队列里。
     *              此时如果客户端宕机，这些消息会变成ready，表示在队列里随时准备发出去
     *        2）如何手动签收：【listener.simple.acknowledge-mode: manual】
     *              1. channel.basicAck(deliveryTag, false); 签收，业务成功完成后手动签收
     *              2. channel.basicNack(deliveryTag, false, true);  拒签，业务失败拒签
     *              3. channel.basicReject();  拒签
     */
//    @PostConstruct  // 在MyRabbitConfig这个配置类对象构造完成之后再执行这个方法
    public void initRabbitTemplate(){
        // 设置确认回调：当消息从Publisher传到Broker时调用
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * 只要消息抵达Broker就ack=true
             * @param correlationData correlation data for the callback. 当前消息的唯一关联数据(可认为是消息的唯一id)
             * @param ack true for ack, false for nack 消息是否成功收到
             * @param cause An optional cause, for nack, when available, otherwise null. 失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                /**
                 * 保证消息可靠性-防止消息丢失
                 * 1、做好消息确认机制（publisher[confirm回调]、consumer[手动ack]）
                 * 2、每个发送的消息都在数据库做好记录【包括失败原因】，定期将失败的消息再次发送一遍
                 */
                // confirm 说明服务器收到了并持久到磁盘里面。注意之后还会再将这消息持久化到消息队列里面，但这是服务器的事情了，我们只要确认confirm就行
                System.out.println("confirm...CorrelationData["+correlationData+"]==>ack["+ack+"]==>cause["+cause+"]");
            }
        });

        // 消息抵达队列的确认回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 消息没抵达指定队列【比如routingKey错了等原因】时会触发这个失败回调
             * @param message the returned message. 投递失败的消息的详细信息
             * @param replyCode the reply code. 回复的状态码
             * @param replyText the reply text. 回复的文本内容
             * @param exchange the exchange.    这消息是发给哪个交换机的
             * @param routingKey the routing key.   消息带的是哪个路由键
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                // 来到这里就是出错误了，修改数据库当前消息的错误状态-》错误【没持久化消息到队列里，定期重发】
                System.out.println("Fail Message["+message+"]==>replyCode["+replyCode+"]==>exchange["+exchange+"]==>replyText["+replyText+"]==>routingKey["+routingKey+"]");
            }
        });
    }
}
