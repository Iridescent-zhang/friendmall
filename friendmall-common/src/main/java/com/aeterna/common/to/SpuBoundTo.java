package com.aeterna.common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.common.to
 * @ClassName : .java
 * @createTime : 2024/6/23 18:03
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

/**
 * to类型和vo类型一样 就是一种o 也就是一种object
 * 当远程调用时两个服务间需要传输双方都要使用的对象，传输用的就是to，过程中会被springcloud转换为json(要用@RequestBody)
 */
@Data
public class SpuBoundTo {
    private Long spuId;
    private BigDecimal buyBounds;
    private BigDecimal growBounds;
}
