package com.aeterna.friendmall.product.web;

import com.aeterna.friendmall.product.entity.CategoryEntity;
import com.aeterna.friendmall.product.service.CategoryService;
import com.aeterna.friendmall.product.vo.Catalog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.web
 * @ClassName : .java
 * @createTime : 2024/7/18 23:04
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * GetMapping 参数为数组表示 可以映射多个路径
     * 这里希望这两个路径映射到首页
     *     拼接 路径字符串（表示一个页面视图地址）return时默认的前后缀
     *     视图解析器进行拼串
     * DEFAULT_PREFIX = "classpath:/templates/";    // classpath:/ 指的是 resource 文件夹
     * DEFAULT_SUFFIX = ".html";
     * 所以是    classpath:/templates/ + 返回值 + .html
     *
     * Model 是 springmvc 提供的接口，把数据放到里面，前端就能获得了
     */
    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model){

        // TODO ： 查出所有的 1级分类 动态填进前端页面
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();

        model.addAttribute("categorys", categoryEntities);
        return "index";
    }

    // 原来是 ajax 请求返回json数据 index/catalog.json
    @ResponseBody  // 直接返回json数据而不是跳转页面  json就是Map
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catalog2Vo>> getCatalogJson(){

//        long l = System.currentTimeMillis();

        Map<String, List<Catalog2Vo>> catalogJson = categoryService.getCatalogJson();

//        System.out.println("消耗时间：" + (System.currentTimeMillis()-l));  // 看方法执行时间

        return catalogJson;
    }

    // 简单服务压力测试nginx gateway中间件
    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        // 1.获取一把锁，只要锁名一样就是同一把锁
        RLock lock = redisson.getLock("my-lock");

        // 2.加锁
        lock.lock();  // 阻塞式等待：默认加的锁都是30s
        /**
         * redisson强大：
         *  1. 锁的自动续期，如果业务超长，运行期间看门狗自动给锁加上新的30s，不用担心业务时间长，锁自动过期被删除
         *  2. 加锁的业务运行完成或者宕机结束了，看门狗不再给当前锁续期，一段时间后自然就解锁了，不用手动解锁也不会死锁
         */
        // try finally 表示：无论业务是否出现问题，最终都要解锁

        //lock.lock(10, TimeUnit.SECONDS);  // 10s自动解锁,这种写法不会有看门狗给锁自动续期,所以自动解锁时间一定要大于业务运行时间
        // 1.如果传递了锁的超时时间,就发送redis lua脚本进行占锁,默认超时就是指定的时间    lock.lock(10, TimeUnit.SECONDS);
        // 2.如果未指定锁的超时时间,就使用[getLockWatchdogTimeout()看门狗默认时间]=30*1000ms    lock.lock();
        //    只要占锁成功,就会启动一个定时任务[重新给锁设置过期时间,新的过期时间就是看门狗默认时间30s],每隔10s设置一次,设成30s
        //    而且续期时间也指定了[internalLockLeaseTime(看门狗时间) / 3]=10*1000ms,也就是10s后续期

        //最佳实战
        //lock.lock(30, TimeUnit.SECONDS);省掉了续期操作,明显指定超时时间,通过给大时间限制避免问题,手动解锁
        try {
            // 打印线程号，具体是那个线程操作锁
            System.out.println("加锁成功，执行业务..."+Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 3.解锁
            System.out.println("释放锁..."+Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }

    /**
     * RReadWriteLock 读写锁
     * 数据修改期间写锁是一个排他锁(互斥锁/独享锁),只能有一个获取, 而读锁是一个共享锁,大家都能用
     * 只要写锁没释放, 读锁也不会获得, 读必须等待, 这样保证总能获得最新的数据
     *
     * 写 + 读: 等待写锁释放
     * 写 + 写: 阻塞
     * 读 + 写: 写锁也要等读锁
     * 读 + 读: 相当于无锁, 并发读, 会在redis中记录所有的读锁
     * 只有有写的存在, 就都要等待(后面那个等前面那个)
     */
    @GetMapping("/write")
    @ResponseBody
    public String writeValue() {
        // 改数据加写锁,读数据加读锁,只要这个锁是同一个名即可
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        // 获得写锁
        RLock writeLock = lock.writeLock();
        // 加写锁
        writeLock.lock();
        String s = "";
        try {
            System.out.println("写锁加锁成功..."+Thread.currentThread().getId());
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            stringRedisTemplate.opsForValue().set("writeValue", s);
        }catch (Exception e) {
        }finally {
            writeLock.unlock();
            System.out.println("写锁释放..."+Thread.currentThread().getId());
        }
        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue() {
        // 改数据加写锁,读数据加读锁,只要这个锁是同一个名即可
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        RLock readLock = lock.readLock();
        // 加读锁
        readLock.lock();
        String s = "";
        try {
            System.out.println("读锁加锁成功..."+Thread.currentThread().getId());
            s = stringRedisTemplate.opsForValue().get("writeValue");
            Thread.sleep(30000);
        }catch (Exception e) {
        }finally {
            System.out.println("读锁释放..."+Thread.currentThread().getId());
            readLock.unlock();
        }
        return s;
        /**
         * JUC 写法 , 这些接口都是一样的
         * ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
         */
    }

    /**
     * 闭锁 RCountDownLatch , 类似 JUC 下的 CountDownLatch
     * 模拟全部都执行完了才行
     * 放假,锁门 五个班全部走完才能锁大门
     *
     * 只有gogogo()被调用5次后 lockDoor()上锁之后的代码才能继续执行
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {

        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();  // 等待闭锁都完成, 等待五个班的人都走完

        return "放假了...";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) {

        // 同一把锁
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();  // 计数减一

        return id + "班的人都走了..";
    }

    /**
     * 车库停车,共有三车位,车来车去
     * 信号量 , 大概就是增增减减 , 但是总和是固定的
     * 信号量 RSemaphore , 同 JUC 下的 Semaphore
     *
     * 这玩意可以用来实现分布式限流操作: 比如我的服务可承受的高并发流量就是10000,那10000相当于车位
     *   在流量进来前先去拿一个信号量,能拿到说明我的服务还有空余,这时候才让流量进来
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
//        park.acquire();  // 获取一个信号 / 获得锁 / 获得一个车位 , 阻塞方法,没获取就阻塞在这,代码不继续执行
        boolean tried = park.tryAcquire();// 表示试着拿一下,有就停,没有就算了,这样不会阻塞了    其他锁的方法也有这个 try !
        if (tried) {
            // 执行业务
        }else {
            return "当前流量大, 请稍后再来";
        }

        return "ok=>" + tried;
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() {

        RSemaphore park = redisson.getSemaphore("park");
        park.release();  // 释放一个车位

        return "ok";

        /**
         *  JUC 下的信号量 Semaphore
         *         \Semaphore semaphore = new Semaphore(5);
         *         semaphore.release();  // 走一辆车
         *         semaphore.acquire();  // 停一辆车
         */
    }
}
