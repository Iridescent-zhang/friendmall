package com.aeterna.friendmall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.friendmall.coupon.entity.CouponSpuCategoryRelationEntity;

import java.util.Map;

/**
 * 优惠券分类关联
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 13:14:11
 */
public interface CouponSpuCategoryRelationService extends IService<CouponSpuCategoryRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

