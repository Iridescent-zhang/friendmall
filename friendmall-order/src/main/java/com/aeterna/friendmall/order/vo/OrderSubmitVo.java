package com.aeterna.friendmall.order.vo;

import com.sun.org.apache.xpath.internal.SourceTree;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.vo
 * @ClassName : .java
 * @createTime : 2024/8/21 10:52
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
// 订单提交的数据
@Data
public class OrderSubmitVo {

    private Long addrId;  // 收货地址Id
    private Integer payType;  // 支付方式，写死
    // 无需提交需要购买的商品，假设订单的商品会随着购物车选中商品而变化，所以要再获取一遍
    // 优惠、发票等
    private String orderToken;  // 防重令牌
    private BigDecimal payPrice;  // 应付价格，用来验价，支付时价格可能变化(类似携程)

    private String note;  // 订单备注

    // 用户相关信息在session里面
}
