package com.aeterna.friendmall.order;

import com.alibaba.cloud.seata.GlobalTransactionAutoConfiguration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用RabbitMQ
 * 1、引入amqp场景  RabbitAutoConfiguration（容器中自动配置了 RabbitTemplate【收发消息】、AmqpAdmin【创建交换机、队列、binding】、CachingConnectionFactory、RabbitConnectionFactory）
 *          通过配置文件绑定属性 @ConfigurationProperties(prefix = "spring.rabbitmq").所以要配置一些信息
 * 2、@EnableRabbit ；开启功能
 * 3. @RabbitListener：监听消息，必须先有@EnableRabbit。这个可以标在 类和方法 上（用来标注监听哪些队列即可）
 *    @RabbitHandler：只能标在方法上（可以用来重载区分不同的消息）
 *
 *
 *
 * 本地事务失效问题：
 *      在同一个service(对象)里互相调事务方法相当于代码拷贝【原来是这样，解决了createOrder()里调用computePrice(orderEntity, orderItemEntities)结果似乎还能传递内存里的数据这个疑惑，原来只是代码拷贝】
 *      代码拷贝意味着没有走代理(绕过了代理对象)，而事务实际上是使用代理来控制的，所以这时调用的b、c做任何事务设置都没用（设置Propagation.REQUIRES_NEW的想设置超时等也没用）。
 *      除非b、c来自其他service，如 bService.b(), cService.c()，这时候使用其他服务的代理，事务设置当然能生效了
 * 解决：使用代理对象来调用事务方法
 *      1）引入 aop-starter-》引入了 aspectjweaver（更强大的动态代理）
 *      2）@EnableAspectJAutoProxy(exposeProxy = true)开启aspectj动态代理功能，不使用这个注解的话默认会用使用接口的那个JDK动态代理。之后所有动态代理都由aspectj创建（即使没有接口也能创建动态代理）。
 *              (exposeProxy = true)：对外暴露代理对象
 *      3）本类互调用调用对象
 *         // 用Aop上下文 AopContext 拿到当前代理对象
 *         OrderServiceImpl orderService = (OrderServiceImpl) AopContext.currentProxy();
 *         // 这样使用代理对象调方法，事务设置才能生效
 *         orderService.b();
 *         orderService.c();
 *
 *  Seata控制分布式事务
 *  1）、每个微服务先在数据库创建 UNDO_LOG 表
 *  2）、安装事务协调器 TC ：seata-server
 *  3）、整合
 *         1. 导入依赖spring-cloud-starter-alibaba-seata  0.7.1-seata-all
 *         2. 启动 seata-server
 *              registry.conf：注册中心配置；修改 registry type=nacos
 *              file.conf：
 *         3. 所有想要用到分布式事务的微服务使用 seata DataSourceProxy 代理自己的数据源
 *         4. 每个微服务必须导入registry.conf、file.conf，修改file.conf:
 *              vgroup_mapping.{spring.application.name}-fescar-service-group = "default"
 *         5. 给分布式的大事务标 @GlobalTransactional，这里就是给订单服务标，订单服务调用锁库存和查积分两个小事务
 *         6. 每个远程的小事务用 @Transactional 就可以了
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableRedisHttpSession
@EnableDiscoveryClient
@EnableFeignClients
@EnableRabbit
@SpringBootApplication(exclude = GlobalTransactionAutoConfiguration.class)
public class FriendmallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendmallOrderApplication.class, args);
    }

}
