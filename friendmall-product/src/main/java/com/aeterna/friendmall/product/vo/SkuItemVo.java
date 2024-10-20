package com.aeterna.friendmall.product.vo;

import com.aeterna.friendmall.product.entity.SkuImagesEntity;
import com.aeterna.friendmall.product.entity.SkuInfoEntity;
import com.aeterna.friendmall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.vo
 * @ClassName : .java
 * @createTime : 2024/8/3 19:00
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class SkuItemVo {

    // 1. sku的基本信息获取 pms_sku_info
    SkuInfoEntity info;
    boolean hasStock = true;

    // 2. sku的图片信息 pms_sku_images
    List<SkuImagesEntity> images;

    // 3. 获取sku对应的spu所有的销售属性组合，就是能组成多少种sku，要显示在界面的
    List<SkuItemSaleAttrVo> saleAttr;

    // 4. 获取spu的介绍 pms_spu_info_desc
    SpuInfoDescEntity desp;

    // 5. 获取spu的规格参数信息(比如在主体这个属性组下包含了多少属性，每个属性的值又是多少)
    List<SpuItemAttrGroupVo> groupAttrs;

    SeckillInfoVo seckillInfo;  // 当前商品的秒杀优惠信息

}
