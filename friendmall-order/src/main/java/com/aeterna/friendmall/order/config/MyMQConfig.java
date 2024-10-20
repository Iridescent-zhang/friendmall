package com.aeterna.friendmall.order.config;

import com.aeterna.friendmall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.config
 * @ClassName : .java
 * @createTime : 2024/8/23 13:46
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Configuration
public class MyMQConfig {

    /**
     * @Bean Queue Binding Exchange，容器中的这些都会在 RabbitMQ 中自动创建（RabbitMQ没有）
     * RabbitMQ 只要有，@Bean声明属性发生变化也是不能覆盖的
     */
    @Bean
    public Queue orderDelayQueue(){
        /**
         * exclusive：这个队列是否只能被声明者连接
         * 创建 Queue并用amqpAdmin声明 Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
         * 队列需要设置参数：
         * x-dead-letter-exchange: order-event-exchange
         * x-dead-letter-routing-key: order.release.order
         * x-message-ttl: 60000
         */
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "order-event-exchange");
        arguments.put("x-dead-letter-routing-key", "order.release.order");
        arguments.put("x-message-ttl", 60000);
        Queue queue = new Queue("order.delay.queue", true, false, false, arguments);
        return queue;
    }

    @Bean
    public Queue orderReleaseOrderQueue(){
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Exchange orderEventExchange(){
        // (String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
        return new TopicExchange("order-event-exchange", true, false);
    }

    @Bean
    public Binding orderCreateOrderBinding(){
        /**
         * 创建 binding并用amqpAdmin声明
         *     Binding(String destination【目的地的名】, DestinationType destinationType【目的地类型：可以连到交换机或队列】, String exchange【用哪个交换机去连】,
         *          String routingKey【其实就是交换机和队列之间的BindingKey，很重要，消息携带的routingKey就是要和这个进行匹配】,
         *          Map<String, Object> arguments【一些自定义参数】
         *     )
         * 将[exchange]交换机和[destination]目的地进行绑定，使用的BingingKey是[routingKey]
         */
        Binding binding = new Binding("order.delay.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.create.order", null);
        return binding;
    }

    @Bean
    public Binding orderReleaseOrderBinding(){
        Binding binding = new Binding("order.release.order.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.release.order", null);
        return binding;
    }

    /**
     * 订单释放和库存解锁直接绑定，是为了订单释放时发一个消息给库存解锁服务，这是为了二次确认，因为有可能网络等原因造成卡顿，原先的库存解锁服务反而在订单释放服务之前进行了。
     */
    @Bean
    public Binding orderReleaseOtherBinding(){
        Binding binding = new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.release.other.#", null);
        return binding;
    }

    // TODO 存储秒杀业务创建的订单号的队列，由订单服务监听
    @Bean
    public Queue orderSeckillOrderQueue(){
        Queue queue = new Queue("order.seckill.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Binding orderSeckillOrderBinding(){
        Binding binding = new Binding("order.seckill.order.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.seckill.order", null);
        return binding;
    }

}
