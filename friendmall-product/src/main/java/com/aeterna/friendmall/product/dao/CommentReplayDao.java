package com.aeterna.friendmall.product.dao;

import com.aeterna.friendmall.product.entity.CommentReplayEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品评价回复关系
 * 
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-12 16:35:25
 */
@Mapper
public interface CommentReplayDao extends BaseMapper<CommentReplayEntity> {
	
}
