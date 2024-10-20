package com.aeterna.friendmall.order.listener;

import com.aeterna.friendmall.order.entity.OrderEntity;
import com.aeterna.friendmall.order.service.OrderService;
import com.aeterna.friendmall.order.service.impl.OrderServiceImpl;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.listener
 * @ClassName : .java
 * @createTime : 2024/8/24 17:34
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@RabbitListener(queues = "order.release.order.queue")
@Service
public class OrderCloseListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void listener(OrderEntity entity, Channel channel, Message message) throws IOException {
        System.out.println("收到过期的订单消息，没付款的话准备关闭订单 ：" + entity.getOrderSn());
        try {
            // 关单逻辑：支付超时或用户取消
            orderService.closeOrder(entity);
            // 手动调用支付宝收单，保证这边关订单同时把支付宝那边单一收，让他没法支付了，这块略，用自动收单，实际上这手动挺重要
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
