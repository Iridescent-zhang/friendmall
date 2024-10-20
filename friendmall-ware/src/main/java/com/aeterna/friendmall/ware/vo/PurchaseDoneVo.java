package com.aeterna.friendmall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.ware.vo
 * @ClassName : .java
 * @createTime : 2024/7/1 18:41
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class PurchaseDoneVo {
    @NotNull
    private Long id;  // 采购单Id
    private List<PurchaseItemDoneVo> items;  // 采购单里面的采购项
}
