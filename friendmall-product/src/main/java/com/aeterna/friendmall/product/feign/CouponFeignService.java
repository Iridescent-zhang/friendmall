package com.aeterna.friendmall.product.feign;

import com.aeterna.common.to.SkuReductionTo;
import com.aeterna.common.to.SpuBoundTo;
import com.aeterna.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.feign
 * @ClassName : .java
 * @createTime : 2024/6/23 17:55
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@FeignClient("friendmall-coupon")
public interface CouponFeignService {
    /**
     * OpenFeign的逻辑
     * 如果有服务调用CouponFeignService下的这个方法
     * 1. @RequestBody将对象转为json
     * 2. 首先去nacos找friendmall-coupon服务，然后给/coupon/spubounds/save发请求，上一步的json被放在请求体里
     * 3. 对方服务收到请求，请求体里有json数据
     *    所以就算被调用的函数是这样的@RequestBody SpuBoundsEntity spuBounds，没有用spuBoundTo，只要成员对应，请求体里的json一样可以转换
     * 只要json数据模型兼容，双方服务也无需使用同一个vo
     * 当然实际中为了方便，调用时直接将要调用函数的声明/函数签名(如 public R save(@RequestBody SpuBoundTo spuBoundTo))
     */
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);

    @PostMapping("/coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
