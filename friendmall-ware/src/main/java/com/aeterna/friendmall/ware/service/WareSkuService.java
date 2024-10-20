package com.aeterna.friendmall.ware.service;

import com.aeterna.common.to.mq.OrderTo;
import com.aeterna.common.to.mq.StockLockedTo;
import com.aeterna.friendmall.ware.vo.LockStockResult;
import com.aeterna.friendmall.ware.vo.SkuHasStockVo;
import com.aeterna.friendmall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.friendmall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 16:49:59
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo orderTo);
}

