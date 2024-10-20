package com.aeterna.friendmall.order.vo;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.vo
 * @ClassName : .java
 * @createTime : 2024/8/20 22:45
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class SkuStockVo {
    private Long skuId;
    private Boolean hasStock;
}
