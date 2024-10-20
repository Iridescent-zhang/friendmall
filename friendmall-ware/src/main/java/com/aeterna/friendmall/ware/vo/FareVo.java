package com.aeterna.friendmall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.ware.vo
 * @ClassName : .java
 * @createTime : 2024/8/21 9:35
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class FareVo {

    private MemberAddressVo address;
    private BigDecimal fare;
}
