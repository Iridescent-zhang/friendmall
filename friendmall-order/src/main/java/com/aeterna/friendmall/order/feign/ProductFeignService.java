package com.aeterna.friendmall.order.feign;

import com.aeterna.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.feign
 * @ClassName : .java
 * @createTime : 2024/8/21 17:23
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@FeignClient("friendmall-product")
public interface ProductFeignService {
    @GetMapping("/product/spuinfo/{skuId}/id")
    R getSpuInfoBySkuId(@PathVariable("skuId") Long skuId);
}
