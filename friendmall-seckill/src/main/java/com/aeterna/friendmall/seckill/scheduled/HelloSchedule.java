package com.aeterna.friendmall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.seckill.scheduled
 * @ClassName : .java
 * @createTime : 2024/8/26 15:38
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

/**
 * 定时任务：
 *      1、@EnableScheduling 开启定时任务
 *      2、@Scheduled 并不是整合 cron，但是基本语法是一样的。这个注解用来开启一个定时任务
 *      3、自动配置类：TaskSchedulingAutoConfiguration，属性绑定在@ConfigurationProperties("spring.task.scheduling")
 *
 * 异步任务：【执行异步任务除了可以自己把它丢到线程池，也可以使用这个方法直接创建异步任务】
 *      1、@EnableAsync开启异步任务功能
 *      2、给希望异步执行的方法上标注：@Async
 *      3、自动配置类：TaskExecutionAutoConfiguration，属性绑定在@ConfigurationProperties("spring.task.execution")
 *
 *
 */

@Slf4j
@Component
//@EnableAsync
//@EnableScheduling
public class HelloSchedule {
    /**
     * 【Spring定时和 Cron 定时】区别：
     * 1、Spring 中只有六位组成，不要第七位的年
     * 2、周一到周五在 spring中就是1->7，在纯cron中不是【周日到周六对应7->1】
     * 3、定时任务不应该阻塞，意思是有一个定时任务超时了也不应该阻塞其他的定时任务。但默认是阻塞的
     *     如何改为不阻塞的：
     *        1、让业务以异步方式运行，自己提交结果到线程池
     *        CompletableFuture.runAsync(()->{
     *             xxxxService.hello();
     *         }, excutor);
     *        2、配置文件中设置"spring.task.scheduling"
     *             因为类TaskSchedulingProperties注解@ConfigurationProperties("spring.task.scheduling")，里面的线程池实际上只有一个线程，难怪是阻塞的。
     *            【这个方法有可能不好用】
     *        3、让定时任务异步执行
     *             使用异步任务@Async
     * 解决：使用 定时+异步 来实现定时任务不阻塞
     */
//    @Async
//    @Scheduled(cron = "* * * ? * 1")  // 秒分时日月周年(Spring 没有)，且日和周必须不能同时指定，可以使用？来表示其中一个
//    public void hello() throws InterruptedException {
//        log.info("hello...");
//        Thread.sleep(3000);
//    }
}
