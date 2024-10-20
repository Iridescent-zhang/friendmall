package com.aeterna.friendmall.order.service.impl;

import com.aeterna.friendmall.order.entity.OrderEntity;
import com.aeterna.friendmall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.order.dao.OrderItemDao;
import com.aeterna.friendmall.order.entity.OrderItemEntity;
import com.aeterna.friendmall.order.service.OrderItemService;

@RabbitListener(queues = {"hello-java.queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * queues是一个数组，指定要监听的队列
     * 参数可以写以下类型：
     * 1. Message message 原生的消息详细信息，头加体
     * 2. 发送的消息的类型，可以直接获得。OrderReturnReasonEntity
     * 3. Channel channel：当时传输数据的通道
     *
     * 由于队列可以被多服务同时监听，但只能有一个获得，获得后队列数据会删除
     * 场景：
     *      1、订单服务启动多个；同个消息只能一个客户端获得，是竞争关系
     *      2、业务处理很耗时怎么办？只有一个消息被接收并完全处理完，这个方法运行结束了才可以接收到下一个消息
     */
    @RabbitHandler
    public void receiveMessage(Message message, OrderReturnReasonEntity content, Channel channel) throws InterruptedException {
        // 消息体内容
        byte[] body = message.getBody();
        // 消息头属性信息
        MessageProperties properties = message.getMessageProperties();
        System.out.println("接受到消息..." +content.toString());
//        Thread.sleep(3000);
        System.out.println("消息处理完成..." + content.getName());
        // channel 通道内按顺序自增，理解为运单号吧
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        System.out.println("deliveryTag = " + deliveryTag);
        // 签收货物 basicAck(long deliveryTag, boolean multiple)。multiple选择非批量模式，一个一个签收
        try {
            if (deliveryTag%2==0) {
                // 手动签收货物
                channel.basicAck(deliveryTag, false);
                System.out.println("签收了货物.."+deliveryTag);
            }else {
                // 拒绝签收货物
                // boolean requeue=false：这个消息丢弃了。=true：重新入队，服务器之后再处理
                channel.basicNack(deliveryTag, false, true);  // basicNack(long deliveryTag, boolean multiple, boolean requeue)
//                channel.basicReject();  // basicReject(long deliveryTag, boolean requeue)
                System.out.println("拒绝签收货物.."+deliveryTag);
            }
        }catch (Exception e) {
            // 网络中断，签收状态发不过去
        }
    }

    // @RabbitHandler用途：比如一个队列里面不只有一种数据类型，或者要同时接收多个队列的不同类型消息，可以用RabbitHandler进行重载处理(多些几个方法)
    @RabbitHandler
    public void receiveMessage2(OrderEntity content) throws InterruptedException {
        System.out.println("接受到消息..." +content.toString());
    }

}