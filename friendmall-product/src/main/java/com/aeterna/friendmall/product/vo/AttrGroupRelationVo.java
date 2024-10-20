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
 * @createTime : 2024/6/3 23:53
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class AttrGroupRelationVo {

    /**
     * 用来删除关联关系，前端发的请求携带AttrGroupRelationVo的数组，让我们执行删除操作
     * attrGroupId
     * attrId
     */
    public Long attrGroupId;
    public Long attrId;
}
