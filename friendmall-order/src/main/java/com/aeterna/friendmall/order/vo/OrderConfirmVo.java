package com.aeterna.friendmall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.vo
 * @ClassName : .java
 * @createTime : 2024/8/19 15:24
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
// 订单确认页需要的数据
public class OrderConfirmVo {

    //收货地址 ums_member_receive_address
    @Setter @Getter
    List<MemberAddressVo> address;

    // 所有选中的购物项
    @Setter @Getter
    List<OrderItemVo> items;

    // 发票记录....略

    // 优惠券信息... MemberEntity的积分
    @Setter @Getter
    Integer integration;

    // 防重复提交令牌
    @Setter @Getter
    String orderToken;

    @Setter @Getter
    Map<Long, Boolean> stocks;

    public Integer getCount(){
        Integer count = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    // 订单总额
//    BigDecimal total;
    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(BigDecimal.valueOf(item.getCount()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }

    // 应付价格
//    BigDecimal payPrice;
    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
