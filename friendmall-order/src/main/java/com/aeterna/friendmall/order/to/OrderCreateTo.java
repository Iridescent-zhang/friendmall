package com.aeterna.friendmall.order.to;

import com.aeterna.friendmall.order.entity.OrderEntity;
import com.aeterna.friendmall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.to
 * @ClassName : .java
 * @createTime : 2024/8/21 13:22
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class OrderCreateTo {

    private OrderEntity order;

    private List<OrderItemEntity> orderItems;

    private BigDecimal fare; // 运费

    private BigDecimal payPrice;  // 订单计算的应付价格

}
