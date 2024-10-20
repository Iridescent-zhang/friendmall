package com.aeterna.friendmall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 1. 开启服务注册发现，这样网关将请求路由到其他服务时就知道其他服务在哪了
 *      @EnableDiscoveryClient
 * 2.
 *
 */
@EnableDiscoveryClient  // nacos：开启服务注册与发现功能
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class FriendmallGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendmallGatewayApplication.class, args);
    }

}
