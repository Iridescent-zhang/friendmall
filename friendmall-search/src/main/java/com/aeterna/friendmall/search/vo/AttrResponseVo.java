package com.aeterna.friendmall.search.vo;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.search.vo
 * @ClassName : .java
 * @createTime : 2024/7/25 20:45
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class AttrResponseVo {

    /**
     * 前端要新增数据，多传一个attrGroupId
     *
     * 这些全都是AttrEntity的属性，然后再创几个他没有的
     * 并且数据库相关的注解就没必要了
     */

    /**
     * 属性id
     */
    private Long attrId;
    /**
     * 属性名
     */
    private String attrName;
    /**
     * 是否需要检索[0-不需要，1-需要]
     */
    private Integer searchType;
    /**
     * 值类型[0-为单个值，1-可以选择多个值]
     */
    private Integer valueType;
    /**
     * 属性图标
     */
    private String icon;
    /**
     * 可选值列表[用逗号分隔]
     */
    private String valueSelect;
    /**
     * 属性类型[0-销售属性，1-基本属性，2-既是销售属性又是基本属性]
     */
    private Integer attrType;
    /**
     * 启用状态[0 - 禁用，1 - 启用]
     */
    private Long enable;
    /**
     * 所属分类
     */
    private Long catelogId;
    /**
     * 快速展示【是否展示在介绍上；0-否 1-是】，在sku中仍然可以调整
     */
    private Integer showDesc;

    /**
     * 这个属性所关联的分组
     * 前端发的请求带的额外字段
     */
    private Long attrGroupId;

    /**
     * 前端要查询数据，咱要多返回两个：
     *    "catelogName" 手机/数码/手机 //所属分类名字
     *    "groupName" 主体 //所属分组名字     每个分类比如手机下面会有很多属性分组
     */

    private String catelogName;
    private String groupName;

    private Long[] catelogPath;  // 修改回显时需要catelogPath
}
