package com.aeterna.friendmall.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * feign 远程调用：
 * 1. 引入 open-feign 依赖(这是一个声明式的远程调用方法)
 * 2. 编写一个接口告诉 springcloud 这个接口需要调用远程服务
 *      1. 声明接口的每一个方法都是调用哪个远程服务的哪个请求
 * 3. 开启远程调用功能
 *      @EnableFeignClients(basePackages = "com.aeterna.friendmall.member.feign")
 *      basePackages 指定上一步中接口所在的位置，我们都放在 feign 包下
 *      服务一启动就会扫描 basePackages 下指定了 @FeignClient 的接口，并且这每个接口如2.中都详细描述了自己的作用
 */
@EnableRedisHttpSession  // 开启 SpringSession 功能
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.aeterna.friendmall.member.feign")
public class FriendmallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendmallMemberApplication.class, args);
    }

}
