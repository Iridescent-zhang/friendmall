package com.aeterna.friendmall.ware.listener;

import com.aeterna.common.to.mq.OrderTo;
import com.aeterna.common.to.mq.StockDetailTo;
import com.aeterna.common.to.mq.StockLockedTo;
import com.aeterna.common.utils.R;
import com.aeterna.friendmall.ware.entity.WareOrderTaskDetailEntity;
import com.aeterna.friendmall.ware.entity.WareOrderTaskEntity;
import com.aeterna.friendmall.ware.service.WareSkuService;
import com.aeterna.friendmall.ware.vo.OrderVo;
import com.alibaba.fastjson.TypeReference;
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
 * @Package : com.aeterna.friendmall.ware.listener
 * @ClassName : .java
 * @createTime : 2024/8/24 16:43
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    /**
     * 库存解锁场景：【所谓的解锁都是为了操作wms_ware_sku这个表里被锁住的库存，其他工作单表、工作单详情表都是为了这个服务的】
     *      1、下订单成功，订单过期没有支付自动取消或用户取消订单；
     *      2.1、下订单成功，库存锁定成功，接下来的业务调用失败或解锁库存返回结果由于网络原因超时，导致订单回滚。之前锁定的库存需要自动解锁。
     *      2.2、库存一部分商品锁定失败，区别于上面，库存服务自身回滚了，数据库里没有工作单详情了，但消息还是发出去了，但是此时无需解锁【因为整个商品远程服务本地事务都回滚了】
     *
     * TODO：只要解锁库存的消息失败，一定要告诉服务器此次解锁失败，所以要不断重发消息，使用手动ACK机制。手动确认消息：channel.basicAck();手动拒绝消息并重发：channel.basicReject
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        try {
            // 当前消息是否是第二次及以后重新派发过来的
//            Boolean redelivered = message.getMessageProperties().getRedelivered();
            System.out.println("收到解锁库存的消息");
            wareSkuService.unlockStock(to);
            // 执行成功就确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            // 有任何异常说明没调用成功，这时候再统一入队就好了
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    // 害怕服务器网路等造成卡顿，在订单关闭时主动发一个消息给库存解锁服务进行二次确认
    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        try {
            System.out.println("订单关闭，准备解锁库存");
            wareSkuService.unlockStock(orderTo);
            // 执行成功就确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            // 有任何异常说明没调用成功，这时候再统一入队就好了
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
