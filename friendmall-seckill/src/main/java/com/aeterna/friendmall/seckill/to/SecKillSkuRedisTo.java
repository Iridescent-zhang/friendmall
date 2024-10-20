package com.aeterna.friendmall.seckill.to;

import com.aeterna.friendmall.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.seckill.to
 * @ClassName : .java
 * @createTime : 2024/8/26 20:22
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class SecKillSkuRedisTo {

    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private Integer seckillCount;
    /**
     * 每人限购数量
     */
    private Integer seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;

    // 当前商品秒杀的开始时间
    private Long startTime;

    // 当前商品秒杀的结束时间
    private Long endTime;

    // 商品的秒杀随机码，不带不能参与秒杀
    private String randomCode;

    // sku详细信息
    private SkuInfoVo skuInfo;
}
