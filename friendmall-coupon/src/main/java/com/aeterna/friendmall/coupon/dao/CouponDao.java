package com.aeterna.friendmall.coupon.dao;

import com.aeterna.friendmall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 13:14:11
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
