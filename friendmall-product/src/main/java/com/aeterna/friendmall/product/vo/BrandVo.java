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
 * @createTime : 2024/6/4 17:29
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class BrandVo {
    /**
     * 要求返回两个数据的列表
     * brandId
     * brandName
     */
    private Long brandId;
    private String brandName;

}
