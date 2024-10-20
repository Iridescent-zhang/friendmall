package com.aeterna.friendmall.product.vo;

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
 * @createTime : 2024/8/3 22:14
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@ToString
@Data
public class SkuItemSaleAttrVo {
    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
}
