package com.aeterna.friendmall.thirdparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FriendmallThirdPartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendmallThirdPartyApplication.class, args);
    }

}
