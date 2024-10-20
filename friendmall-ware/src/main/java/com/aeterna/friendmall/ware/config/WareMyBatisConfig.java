package com.aeterna.friendmall.ware.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.ware.config
 * @ClassName : .java
 * @createTime : 2024/7/1 20:21
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Configuration
@EnableTransactionManagement  // 开启事务，事务的注解其实是跟数据库相关的，所以就写在mybatis的配置类里
@MapperScan("com.aeterna.friendmall.ware.dao")
public class WareMyBatisConfig {
    /**
     * 添加分页插件
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
//        // 如果请求的页码超过最大页数 true返回首页 false就返回空的
//        paginationInterceptor.setOverflow(true);
//
//        // 设置最大单页限制数量 最大显示1000条
//        paginationInterceptor.setLimit(1000);
        return paginationInterceptor;
    }

    @Autowired
    DataSourceProperties dataSourceProperties;
    // 配置 seata 要用的代理数据源
    @Bean
    public DataSource dataSource(DataSourceProperties dataSourceProperties){
        // 默认用的就是数据源就是Hikari。DataSourceConfiguration.Hikari.class ：properties.initializeDataSourceBuilder().type(type).build();
        HikariDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        if (StringUtils.hasText(dataSourceProperties.getName())) {
            dataSource.setPoolName(dataSourceProperties.getName());
        }
        // 将返回的数据源用 seata 一包装，包装成 seata 的数据源代理对象
        return new DataSourceProxy(dataSource);
    }
}
