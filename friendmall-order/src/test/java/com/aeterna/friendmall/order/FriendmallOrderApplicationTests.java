package com.aeterna.friendmall.order;

//import org.junit.jupiter.api.Test;
import com.aeterna.friendmall.order.entity.OrderEntity;
import com.aeterna.friendmall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class FriendmallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 1、创建Exchange[hello-java.exchange]、Queue、Binding
     *      1）使用 AmqpAdmin 创建
     * 2、收发消息
     */
    @Test
    public void createExchange() {
        /**
         * 创建Exchange并用amqpAdmin声明 DirectExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
         */
        DirectExchange directExchange = new DirectExchange("hello-java.exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功", "hello-java.exchange");
    }

    @Test
    public void createQueue(){
        /**
         * exclusive：这个队列是否只能被声明者连接
         * 创建 Queue并用amqpAdmin声明 Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
         */
        Queue queue = new Queue("hello-java.queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功", "hello-java.queue");
    }

    @Test
    public void createBinding(){
        /**
         * 创建 binding并用amqpAdmin声明
         *     Binding(String destination【目的地的名】, DestinationType destinationType【目的地类型：可以连到交换机或队列】, String exchange【用哪个交换机去连】,
         *          String routingKey【其实就是交换机和队列之间的BindingKey，很重要，消息携带的routingKey就是要和这个进行匹配】,
         *          Map<String, Object> arguments【一些自定义参数】
         *     )
         * 将[exchange]交换机和[destination]目的地进行绑定，使用的BingingKey是[routingKey]
         */
        Binding binding = new Binding("hello-java.queue", Binding.DestinationType.QUEUE, "hello-java.exchange", "hello.java", null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功", "hello-java.binding");
    }

    @Test
    public void sendMassageTest(){
        /**
         * 1、会将对象自动转化为message并发送【前提是这个对象必须实现序列化 implements Serializable】，选择交换机，然后指定发送的消息的routingKey
         *
         * 2、现在想要将对象以 JSON 形式发出去
         */
        for (int i = 0; i < 10; i++) {
            if (i%2==0) {
                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
                reasonEntity.setId(1L);
                reasonEntity.setCreateTime(new Date());
                reasonEntity.setName("哈哈==="+i);
                rabbitTemplate.convertAndSend("hello-java.exchange", "hello.java", reasonEntity);
            }else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello-java.exchange", "hello.java", orderEntity);
            }
            log.info("消息发送完成[{}]");
        }
    }

}
