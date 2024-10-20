package com.aeterna.friendmall.coupon.dao;

import com.aeterna.friendmall.coupon.entity.SeckillSessionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀活动场次
 * 
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 13:14:11
 */
@Mapper
public interface SeckillSessionDao extends BaseMapper<SeckillSessionEntity> {
	
}
