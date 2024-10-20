package com.aeterna.friendmall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.vo
 * @ClassName : .java
 * @createTime : 2024/7/19 0:03
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
// 全参无参构造器
@NoArgsConstructor
@AllArgsConstructor
@Data
// 二级分类 Vo
public class Catalog2Vo {
    private String catalog1Id;  // 其1级父分类id
    private List<Catalog3Vo> catalog3List;  // 三级子分类
    private String id;
    private String name;

    /**
     * 三级分类的 Vo 写成 静态内部类
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Catalog3Vo {
        private String catalog2Id;  // 其2级父分类id
        private String id;
        private String name;
    }
}
