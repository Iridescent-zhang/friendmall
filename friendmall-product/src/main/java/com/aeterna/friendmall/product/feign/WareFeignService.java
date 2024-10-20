package com.aeterna.friendmall.product.feign;

import com.aeterna.common.to.SkuHasStockVo;
import com.aeterna.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.feign
 * @ClassName : .java
 * @createTime : 2024/7/17 15:31
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@FeignClient("friendmall-ware")
public interface WareFeignService {

    /**
     * 查询 sku 是否有库存
     */
    @PostMapping("/ware/waresku/hasstock")
    R getSkusHasStock(@RequestBody List<Long> skuIds);
}
