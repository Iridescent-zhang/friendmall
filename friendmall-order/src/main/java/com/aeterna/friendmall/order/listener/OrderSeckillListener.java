package com.aeterna.friendmall.order.listener;

import com.aeterna.common.to.SeckillOrderTo;
import com.aeterna.friendmall.order.entity.OrderEntity;
import com.aeterna.friendmall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.listener
 * @ClassName : .java
 * @createTime : 2024/8/28 20:01
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Slf4j
@RabbitListener(queues = "order.seckill.order.queue")  // TODO 监听秒杀单
@Component
public class OrderSeckillListener {
    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void listener(SeckillOrderTo seckillOrder, Channel channel, Message message) throws IOException {
        try {
            log.info("准备创建秒杀单的详细信息...");
            // 创建一个秒杀单
            orderService.createSeckillOrder(seckillOrder);
            // 手动调用支付宝收单，保证这边关订单同时把支付宝那边单一收，让他没法支付了，这块略，用自动收单，实际上这手动挺重要
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
