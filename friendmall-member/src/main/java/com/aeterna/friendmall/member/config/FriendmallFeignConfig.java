package com.aeterna.friendmall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.cart.config
 * @ClassName : .java
 * @createTime : 2024/8/19 18:29
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Configuration
public class FriendmallFeignConfig {
    // 写我们自己的Feign构造远程请求时使用的请求拦截器
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor(){
            /**
             * Feign创建每个新请求时都会被这个拦截然后调用apply方法
             */
            @Override
            public void apply(RequestTemplate template) {
                /**
                 * 1、 RequestContextHolder拿到刚进来的请求【/toTrade,从浏览器发来的那个最早的请求】的请求参数。
                 *    Spring提供的获取当前请求的上下文，实际上也是用的ThreadLocal获得原请求参数。
                 */
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    System.out.println("RequestInterceptor线程..."+Thread.currentThread().getId());
                    HttpServletRequest request = attributes.getRequest();
                    if (request != null) {
                        // 同步request请求头数据【主要是cookie】到要创建的新请求template里
                        String cookie = request.getHeader("Cookie");
                        template.header("Cookie", cookie);
                    }
                }
            }
        };
    }
}
