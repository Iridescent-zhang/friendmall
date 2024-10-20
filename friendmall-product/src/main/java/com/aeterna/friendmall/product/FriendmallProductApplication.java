package com.aeterna.friendmall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1. 整合 MyBatis—Plus
 *    配置：
 *      1.配置数据源
 *          1)导入数据库驱动
 *          2）application.yml配置数据源信息
 *      2.配置MybatisPlus
 *          1)使用@MapperScan
 *          2）告诉MybatisPlus sql映射文件地址
 *
 * 2. 逻辑删除
 *    1.加逻辑删除注解@TableLogic
 *
 * 3. JSR303 数据校验规则
 *   之前学SSM的时候学过了
 *    1.给Bean加上校验注解：javax.validation.constraints 并 定义自己的message提示
 *    2.在controller里面对应的保存方法的参数上要加@Valid注解让校验功能生效，否则上一步在Entity上家的注解是不会生效的
 *         效果：校验错误后会有对应的相应
 *    3.在第一步校验的Bean后紧跟BindingResult，这个相当于校验结果，我们可以把它封装到我们的R里面，毕竟返回R是我们的业务需求
 *    4.  分组校验（多场景复杂校验）
 *        比如新增和修改时对参数的要求是不一样的，比如新增要求必须提交logo，修改可以不提交（用postman不带logo属性就代表不改logo）
 *        1. 给校验注解标注分组（需要写一些分组接口，仅为标识作用）
 *        2. @Validated({AddGroup.class})  使用Spring提供的@Validated注解，里面可以指定分组，@Valid只是实现
 *        意思就是，在Entity的属性上指定这个字段归类为哪些组，在保存或修改函数上@Validated({AddGroup.class}指定了某些组，
 *                                           于是这个函数就只对这个组下的字段校验，其他就忽略
 *        3. 使用分组校验后，没有指定分组的校验是不会生效的，想让这些校验在某个函数上生效，这个函数就别指定校验哪个组@Validated()
 *     5. 自定义校验（可能@Pattern()都不够用）
 *        1. 编写一个自定义的校验注解
 *        2. 编写一个自定义的校验器
 *        3. 关联二者
 *
 * 4. 做一个统一的异常处理
 *      用 springmvc 的 @ControllerAdvice 注意这个不是增强，而是一个统一的异常处理类
 *
 * 5. 模板引擎
 *   这里开始就不是前后端分离了，但没关系一样学，之前的前后端分离的controller到app里面了
 *      1）thymelead-starter ：关闭缓存
 *      2）静态资源都放在static文件夹下就可以按照路径直接访问
 *      3）页面放在template文件夹下，也可以直接访问
 *              并且springboot做了默认配置，访问项目时默认找 index.html
 *      4） 页面修改不重启服务器就能实时更新 热部署
 *         1.  导入 devtools
 *         2.  修改后重新build项目(ctrl+F9)或者reconpile index.html(ctrl+shift+F9) 傻逼联想要多按个Fn 配合 F9
 *         非前端代码配置还是推荐重启
 *
 * 6. 整合 redis
 *      1） 引入 data-redis
 *      2） 简单配置redis的host等信息
 *      3） 使用 springboot 自动配置好的 StringRedisTemplate 来操作redis
 *              可以将redis当成 map
 *
 * 7. 整合redisson作为分布式锁等功能框架
 *       <groupId>org.redisson</groupId>
 *       <artifactId>redisson</artifactId>
 *
 * 8. 整合 SpringCache 简化缓存开发
 *      1）引入依赖 spring-boot-starter-cache， 要以redis作为缓存：spring-boot-starter-data-redis
 *      2）写配置
 *          2.1 自动配置
 *               CacheAutoConfiguration 导入 RedisCacheConfiguration.class
 *               自动配好了缓存管理器 RedisCacheManager
 *          2.2 手动配置
 *               配置使用 redis 作为缓存  spring.cache.type=redis
 *      3）测试
 *          @Cacheable: Triggers cache population. 触发将数据保存的缓存的操作
 *          @CacheEvict: Triggers cache eviction. 触发将数据从缓存中删除，用于 失效模式
 *          @CachePut: Updates the cache without interfering with the method execution. 不影响方法执行更新缓存，将返回值（注意要有返回值）也放到Cache，这个就是双写模式
 *          @Caching: Regroups multiple cache operations to be applied on a method. 组合以上多个操作
 *          @CacheConfig: Shares some common cache-related settings at class-level. 在类级别共享缓存的相同配置
 *          1）开启缓存功能 @EnableCaching
 *          2）直接使用注解就能完成缓存操作
 *      4) 原理
 *          CacheAutoConfiguration -> RedisCacheConfiguration -> 自动配置 RedisCacheManager
 *            -> 以名字初始化所有缓存，缓存的配置为（如果配置了就用，没配置用默认的defaultCacheConfig）
 *            -> 如果想要改缓存配置，只需要给容器中放一个RedisCacheConfiguration的配置，就会应用到当前RedisCacheManager管理的所有缓存分区中
 *
 *
 */

@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.aeterna.friendmall.product.feign")
@SpringBootApplication
@MapperScan("com.aeterna.friendmall.product.dao")
@EnableDiscoveryClient
public class FriendmallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendmallProductApplication.class, args);
    }

}
