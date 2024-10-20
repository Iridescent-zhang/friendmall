package com.aeterna.friendmall.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.auth.config
 * @ClassName : .java
 * @createTime : 2024/8/9 22:50
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

/**
 * 每次创建新项目都要注意：
 * 1、SpringSession依赖
 * 2、配置文件
 * 3、配置类
 * 4、引入LoginInterceptor拦截器做登录拦截，以及一个实现WebMvcConfigure的配置类
 */
@Configuration
public class FriendmallSessionConfig {
    // 设置作用域为父域
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("FRIENDSESSION");
        serializer.setDomainName("friendmall.com");
        return serializer;
    }

    // 序列化对象为json存到redis
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
