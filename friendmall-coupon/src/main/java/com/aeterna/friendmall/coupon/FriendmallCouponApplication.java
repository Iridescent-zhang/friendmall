package com.aeterna.friendmall.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 将Nacos作为配置中心：本来这些配置我们可能会写在 application.yml 里面，但这样就不好改了
 *  1. 引入依赖
 *  2. 创建 bootstrap配置文件，填写相关配置
 *  3. 在配置中心创建 数据集 (Data id)，在里面填写各个服务想要读取的配置信息
 *  4. 动态获取配置 在controller上加 @RefreshScope 注解
 *       使用 @Value 读取配置项的值，并且配置中心的配置优先于本地配置
 *
 * 扩展：
 *  1. 命名空间 ：配置隔离
 *      1） 开发、测试、生产等多个环境之间隔离
 *      2） 每一个微服务之间互相隔离配置，都创建自己的命名空间，使用自己命名空间下的配置文件
 *  2. 配置分组
 *     给配置文件分组，默认都属于 DEFAULT_GROUP
 * 最后使用：每个微服务创建自己的命名空间，再用配置分组区分环境如dev、test、prod
 *
 * 同时加载多个配置集：
 * 由于配置项会越来越多，我们尝试将配置拆分为多个配置集，如数据源、MySQL、MyBatis的配置集并由nacos同时加载这些配置文件
 *      这样以后就可以把 application.yml 的内容全都交给 nacos 进行管理了
 *   只需要在 bootstrap 中说明每个想要加载的配置文件即可
 *   以前读取配置文件application.yml的注解如@Value、@ConfigurationProperties一样用
 *   配置中心的配置依然优先
 */
@SpringBootApplication
@EnableDiscoveryClient  // nacos：开启服务注册与发现功能
public class FriendmallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendmallCouponApplication.class, args);
    }

}
