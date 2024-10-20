package com.aeterna.friendmall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.friendmall.product.entity.ProductAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * spu属性值
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-12 16:35:25
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

    void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities);

    PageUtils queryPage(Map<String, Object> params);

    void saveProductAttr(List<ProductAttrValueEntity> collect);

    List<ProductAttrValueEntity> baseAttrListForSpu(Long spuId);
}

