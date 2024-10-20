package com.aeterna.friendmall.order.dao;

import com.aeterna.friendmall.order.entity.PaymentInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付信息表
 * 
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 16:41:55
 */
@Mapper
public interface PaymentInfoDao extends BaseMapper<PaymentInfoEntity> {
	
}
