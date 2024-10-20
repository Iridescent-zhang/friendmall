package com.aeterna.friendmall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.config
 * @ClassName : .java
 * @createTime : 2024/7/20 17:36
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Configuration
public class MyRedissonConfig {

    /**
     * 所有对Redisson的使用都通过RedissonClient对象
     */
    @Bean(destroyMethod="shutdown")
    RedissonClient redisson() throws IOException {
        // 1. 创建配置
        Config config = new Config();
        // 注意redis://
        config.useSingleServer().setAddress("redis://192.168.56.10:6379");
        // 2. 根据Config创建出RedissonClient实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
