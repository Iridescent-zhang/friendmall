package com.aeterna.friendmall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.vo
 * @ClassName : .java
 * @createTime : 2024/8/21 20:14
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class WareSkuLockVo {

    private String orderSn;  // 订单号
    private List<OrderItemVo> locks;  // 需要锁住的订单项
    
}
