package com.aeterna.common.to.mq;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.common.to.mq
 * @ClassName : .java
 * @createTime : 2024/8/24 13:13
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class StockLockedTo {

    private Long id;  // 库存工作单的id（每一个订单号order_sn在表wms_ware_order_task创建一个工作单，在wms_ware_order_task_detail中描述每个订单在哪个仓库各保存了几件商品，对应的外键task_id是工作单的id）
                      // 完全理解了，就是一个工作单有很多工作单详情，每个工作单详情是在哪个仓库保存哪个商品各几件
    private StockDetailTo detailTo;  // 一个工作单的详情
}
