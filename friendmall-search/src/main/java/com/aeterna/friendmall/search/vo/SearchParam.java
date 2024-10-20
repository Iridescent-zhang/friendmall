package com.aeterna.friendmall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.search.vo
 * @ClassName : .java
 * @createTime : 2024/7/24 11:39
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

/**
 * 封装页面所有可能传递过来的查询条件
 *
 * 完整的url参数：
 * catalog3Id=225&keyword=小米&sort=saleCount_asc&hasStock=0/1&skuPrice=400_1900&brandId=1
 * &catalogId=1&attrs=1_3G:4G:5G&attrs=2_骁龙845&attrs=4_高清屏
 */
@Data
public class SearchParam {

    public String keyword;  // 全文匹配关键字

    private Long catalog3Id;  // 三级分类id

    /**
     * sort=saleCount_asc/desc  // 按销量排序
     * sort=skuPrice_asc/desc  // 按价格排序
     * sort=hotScore_asc/desc  // 按热度评分(综合)排序
     */
    private String sort;  // 排序条件

    /**
     * 还有很多过滤条件
     * 过滤：hasStock（是否有货）、skuPrice 区间、brandId、attrs、catalogId
     * hasStock = 0/1
     * skuPrice = 500_1000/_1000/500_
     * brandId = 1
     * attrs = 2_5寸:6寸  // 格式为 几号属性_冒号分割多个取值，这里是2号属性尺寸，取值为5寸或6寸
     */
    private Integer hasStock;  // 是否只显示有货的商品  0（无库存） 1（有库存）
    private String skuPrice;  // 价格区间查询
    private List<Long> brandId;  // 可以选中多个品牌
    private List<String> attrs;  // 按照属性进行筛选
    private Integer pageNum = 1;  // 页码

    private String _queryString;  // 原生的所有查询条件
}
