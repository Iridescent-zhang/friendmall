package com.aeterna.friendmall.product.vo;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.vo
 * @ClassName : .java
 * @createTime : 2024/6/3 19:58
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * vo：可以理解为 View Object
 * 用来接收页面传递来的数据封装为对象
 * 也用于将业务处理完成的对象封装成用户要的数据
 * 比如前端发的请求带了一些有用的字段，但不是数据库表里面的字段(或者说不是Entity里面有的字段)，如果每次都在Entity里面加上这个属性再加@TableField
 */
@Data
public class AttrVo {

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
}
