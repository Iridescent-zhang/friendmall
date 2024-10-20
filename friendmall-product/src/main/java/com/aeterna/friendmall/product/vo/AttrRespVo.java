package com.aeterna.friendmall.product.vo;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.vo
 * @ClassName : .java
 * @createTime : 2024/6/3 20:37
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class AttrRespVo extends AttrVo{

    /**
     * 前端要查询数据，咱要多返回两个：
     *    "catelogName" 手机/数码/手机 //所属分类名字
     *    "groupName" 主体 //所属分组名字     每个分类比如手机下面会有很多属性分组
     */

    private String catelogName;
    private String groupName;

    private Long[] catelogPath;  // 修改回显时需要catelogPath
}
