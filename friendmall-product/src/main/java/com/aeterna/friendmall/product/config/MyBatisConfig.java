package com.aeterna.friendmall.product.config;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.config
 * @ClassName : .java
 * @createTime : 2024/6/2 22:57
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis配置类
 *      比如配置分页插件
 */
@Configuration
@EnableTransactionManagement  // 开启事务
@MapperScan("com.aeterna.friendmall.product.dao")
public class MyBatisConfig {

    /**
     * 添加分页插件
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        // 如果请求的页码超过最大页数 true返回首页 false就返回空的
        paginationInterceptor.setOverflow(true);

        // 设置最大单页限制数量 最大显示1000条
        paginationInterceptor.setLimit(1000);
        return paginationInterceptor;
    }
}
