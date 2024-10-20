package com.aeterna.friendmall.order.vo;

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
 * @createTime : 2024/8/21 13:48
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class FareVo {
    private MemberAddressVo address;
    private BigDecimal fare;
}
