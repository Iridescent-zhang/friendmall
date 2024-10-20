package com.aeterna.friendmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.gateway.config
 * @ClassName : .java
 * @createTime : 2024/5/17 18:41
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Configuration
public class FriendmallCorsConfiguration {
    @Bean // 添加过滤器
    public CorsWebFilter corsWebFilter(){
// 基于url跨域，选择reactive包下的
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
// 跨域配置信息
        CorsConfiguration corsConfiguration = new CorsConfiguration();
// 允许跨域的头
        corsConfiguration.addAllowedHeader("*");
// 允许跨域的请求方式
        corsConfiguration.addAllowedMethod("*");
// 允许跨域的请求来源
        corsConfiguration.addAllowedOrigin("*");
// 是否允许携带cookie跨域
        corsConfiguration.setAllowCredentials(true);

// 任意url都要进行跨域配置
        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }

//    @Bean
//    public CorsWebFilter corsWebFilter(){
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//
//        CorsConfiguration corsConfiguration = new CorsConfiguration();
//        // 1. 配置跨域
//        corsConfiguration.addAllowedHeader("*");
//        corsConfiguration.addAllowedMethod("*");
//        corsConfiguration.addAllowedOrigin("*");
//        corsConfiguration.setAllowCredentials(true);
//
//        source.registerCorsConfiguration("/**", corsConfiguration);
//
//        return new CorsWebFilter(source);
//    }
}
