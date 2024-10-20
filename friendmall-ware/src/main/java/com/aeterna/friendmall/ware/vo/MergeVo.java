package com.aeterna.friendmall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.ware.vo
 * @ClassName : .java
 * @createTime : 2024/7/1 14:33
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class MergeVo {

    private Long purchaseId;  // 采购单Id
    private List<Long> items;  // 要合并的采购需求项
}
