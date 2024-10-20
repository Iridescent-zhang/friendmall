package com.aeterna.friendmall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.ware.config
 * @ClassName : .java
 * @createTime : 2024/8/23 15:06
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Configuration
public class MyRabbitConfig {

    /**
     * 使用JSON序列化机制进行消息序列化
     */
    @Bean
    public MessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Exchange stockEventExchange(){
        // stock-event-exchange
        return new TopicExchange("stock-event-exchange", true, false);
    }

    @Bean
    public Queue stockReleaseStockQueue(){
        // stock-release-stock-queue
        Queue queue = new Queue("stock.release.stock.queue", true, false, false);
        return queue;
    }

    @Bean
    public Queue stockDelayQueue(){
        // stock-delay-queue
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "stock-event-exchange");
        arguments.put("x-dead-letter-routing-key", "stock.release");
        arguments.put("x-message-ttl", 120000);
        Queue queue = new Queue("stock.delay.queue", true, false, false, arguments);
        return queue;
    }

    @Bean
    public Binding stockReleaseBinding(){
        // stock-release
        Binding binding = new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE, "stock-event-exchange", "stock.release.#", null);
        return binding;
    }

    @Bean
    public Binding stockLockedBinding(){
        // stock-locked
        Binding binding = new Binding("stock.delay.queue", Binding.DestinationType.QUEUE, "stock-event-exchange", "stock.locked", null);
        return binding;
    }

    // 只有在第一次监听消息的时候，rabbitMQ发现没有这些组件才会去自动创建这些组件，所有要先写个监听方法
//    @RabbitListener(queues = "stock.release.stock.queue")
//    public void handle(Message message){
//    }
}
