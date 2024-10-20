package com.aeterna.friendmall.search.vo;

import com.aeterna.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.search.vo
 * @ClassName : .java
 * @createTime : 2024/7/24 12:16
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class SearchResult {

    // 查询到的所有商品的信息
    private List<SkuEsModel> products;

    // 分页信息
    private Integer pageNum;  // 当前页码
    private Long total;  // 总记录数
    private Integer totalPages;  // 总页码数
    private List<Integer> pageNavs;  // 可选择的导航页码

    private List<BrandVo> brands;  // 当前查询到的结果涉及到的品牌
    private List<CatalogVo> catalogs;  // 当前查询到的结果涉及到的分类
    private List<AttrVo> attrs;  // 当前查询到的结果涉及到的所有属性

    // 面包屑导航数据
    private List<NavVo> navs = new ArrayList<>();
    // 用来表示哪些属性id出现在了url中，我们要根据这个在页面中剔除这个属性
    private List<Long> attrIds = new ArrayList<>();

    @Data
    public static class NavVo{
        private String navName;
        private String navValue;
        private String link;
    }

    //==============以上是返回给页面的所有信息================

    // 静态内部类 品牌和分类是固定显示的(前端加粗显示)，属性也要显示
    // 品牌Brand
    @Data
    public static class BrandVo{
        private Long brandId;

        private String brandName;

        private String brandImg;
    }

    // 分类Catalog
    @Data
    public static class CatalogVo{
        private Long catalogId;

        private String catalogName;
    }

    /**
     * 有个注意点是当前可供选择的属性应该是当前已筛选出的商品所具有的属性，不能点进一个属性结果一个商品都没有
     *      所以属性是在ES里面动态查询的，这是最难的，品牌和分类同样也要聚合分析
     */
    @Data
    public static class AttrVo{
        private Long attrId;

        private String attrName;

        private List<String> attrValue;
    }
}
