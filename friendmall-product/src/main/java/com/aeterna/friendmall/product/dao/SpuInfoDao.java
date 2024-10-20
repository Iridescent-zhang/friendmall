package com.aeterna.friendmall.product.dao;

import com.aeterna.friendmall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 * 
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-12 16:35:25
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    // 多参数一定要先生成各个参数的签名@Param
    void updateSpuStatus(@Param("spuId") Long spuId, @Param("code") int code);
}
