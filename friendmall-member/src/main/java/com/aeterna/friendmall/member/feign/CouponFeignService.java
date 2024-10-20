package com.aeterna.friendmall.member.feign;

import com.aeterna.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.member.feign
 * @ClassName : .java
 * @createTime : 2024/5/13 21:09
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

/**
 * @FeignClient 告诉springcloud这是一个远程客户端，会调用一个远程服务
 *        调用哪个服务由参数指定，参数写 在nacos中注册的服务名
 *            由于服务有很多功能，此时只需要将要调用方法的完整签名(函数声明 如 public R save(@RequestBody SpuBoundTo spuBoundTo))复制过来即可 (记得补全@RequestMapping的路径 就是类的mapping也要加上)
 *            之后找到这个对应的远程服务后，可以理解为把原请求转发到 friendmall-coupon 的 请求 /coupon/coupon/member/list 上
 */
@FeignClient("friendmall-coupon")
public interface CouponFeignService {

    /**
     * 作用就是，以后调用这个接口的这个方法的话，就会去friendmall-coupon里找@RequestMapping路径对应的方法执行
     */
    @RequestMapping("/coupon/coupon/member/list")
    public R memberCoupons();

}
