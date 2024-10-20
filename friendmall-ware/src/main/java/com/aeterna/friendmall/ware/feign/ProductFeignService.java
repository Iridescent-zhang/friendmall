package com.aeterna.friendmall.ware.feign;

import com.aeterna.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.ware.feign
 * @ClassName : .java
 * @createTime : 2024/7/1 20:55
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@FeignClient("friendmall-gateway")
public interface ProductFeignService {

    // 记得拿这个方法完整的路径，比如/product/skuinfo是用RequestMapping写在类上的，也是这个方法路径的一部分
    /**
     * feignService调用的方法的路径有两种写法：
     *      第一种：写/product/skuinfo/info/{skuId}，让去找friendmall-product这个服务
     *      第二种：写/api/product/skuinfo/info/{skuId}，让去找friendmall-gateway网关（网关会截掉/api）
     */
    @RequestMapping("/api/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);
}
