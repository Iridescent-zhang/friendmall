package com.aeterna.friendmall.product.service;

import com.aeterna.friendmall.product.vo.AttrGroupRelationVo;
import com.aeterna.friendmall.product.vo.AttrRespVo;
import com.aeterna.friendmall.product.vo.AttrVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.friendmall.product.entity.AttrEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-12 16:35:25
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    List<AttrEntity> getRelationAttr(Long attrgroupId);

    void deleteRelation(AttrGroupRelationVo[] attrGroupRelationVos);

    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);

    // 挑出能被检索的attrId
    List<Long> selectSearchAttrIds(List<Long> attrIds);
}

