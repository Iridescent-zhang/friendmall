package com.aeterna.friendmall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * SpringSession 核心原理
 * 1）、@EnableRedisHttpSession：Import(RedisHttpSessionConfiguration.class)配置
 *      1.在容器中放入组件
 *          SessionRepository(session的仓库)==>RedisOperationsSessionRepository：一般Repository就是像DAO这种操作持久化层，所以这个是redis操作session的增删改查的封装类
 *      2.SessionRepositoryFilter--extend-->HTTP的Filter：session存储的过滤器，每个请求过来都必须经过filter
 *          1. 创建时自动从容器中获取 SessionRepository
 *          2. 原始的request、response都被包装为：SessionRepositoryRequestWrapper、SessionRepositoryResponseWrapper
 *          3. 原先获取session是request.getSession(),现在变成了SessionRepositoryRequestWrapper.getSession()【装饰者模式】
 *                 后者是从SessionRepository去获取session的，而联系1)、1.知它正是在redis中找
 *【装饰者模式】，所有请求过来，将原生请求一包装，以后获取session都是调用的我的wrapper里规定的方法
 *
 * 完全模拟了以前的session的功能，比如不关浏览器会自动延期、关浏览器一段时间后自动过期
 */
@EnableRedisHttpSession  // 整合redid作为session的存储
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class FriendmallAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendmallAuthServerApplication.class, args);
    }

}
