package com.aeterna.friendmall.member.dao;

import com.aeterna.friendmall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 16:30:08
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
