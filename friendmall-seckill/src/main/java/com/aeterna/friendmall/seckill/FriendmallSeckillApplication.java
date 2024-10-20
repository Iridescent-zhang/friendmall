package com.aeterna.friendmall.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1、整合Sentinel
 *   1）导入依赖
 *   2）下载sentinel控制台，可视化调整监控
 *   3）配置sentinel控制台地址信息，之后可以在控制台调整参数，但是默认所有流控设置保存在内存中，重启会失效
 *   4）
 * 2、每个微服务都导入信息审计模块
 * 3、自定义Sentinel返回
 *
 * 4、使用Sentinel来保护Feign的远程调用【熔断机制】
 *     1）调用方的熔断保护 ：feign.sentinel.enabled=true，Sentinel能感知到feign的远程调用
 *     2）在控制台(调用方处)手动指定调用的远程服务的降级策略，被调用方被降级其实就和宕机是一样的，调用方调用远程服务时同样调用该服务的对应熔断方法【fallback里的FeignServiceFallback】
 *          远程服务被降级处理，会触发我们的熔断回调
 *     3）远程服务主动降级：超大流量的时候必须牺牲一些远程服务。在服务的提供方（远程服务）指定降级策略，提供方是在运行中的，但是调用时得到默认的降级数据（其实同样也是限流数据，我们在SeckillSentinelConfig指定了返回怎样的限流数据）
 *
 * 5、自定义受保护的资源
 *      1）方式一：try (Entry entry = SphU.entry("seckillSkus")){
*                     业务逻辑
*                 } catch (BlockException e) 定义一段受保护的资源【灵活性高，可以保护一段代码】
 *      2）方式二：基于注解
 *      @SentinelResource(value = "getCurrentSeckillSkusResource", blockHandler = "blockHandler")
 *
 *      无论方式二一定要配置被限流后的默认返回【在blockHandler里设置】，url请求这种资源的返回可以统一设置【FriendmallSentinelConfig】
 *
 *
 *
 */
@EnableRedisHttpSession
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})  // 不用数据库就要排除掉数据源设置
public class FriendmallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendmallSeckillApplication.class, args);
    }

}
