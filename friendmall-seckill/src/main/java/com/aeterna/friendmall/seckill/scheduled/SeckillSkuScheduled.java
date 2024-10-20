package com.aeterna.friendmall.seckill.scheduled;

import com.aeterna.friendmall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.seckill.scheduled
 * @ClassName : .java
 * @createTime : 2024/8/26 16:21
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

/**
 * 秒杀商品定时上架
 *    每天凌晨三点上架最近三天需要秒杀的商品
 *    当天00:00:00-->23:59:59
 *    明天00:00:00-->23:59:59
 *    后天00:00:00-->23:59:59
 */
@Slf4j
@Service
public class SeckillSkuScheduled {

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedissonClient redissonClient;

    private final String upload_lock = "seckill:upload:lock";

    /**
     * TODO 幂等性处理，保证不会反复上架已经上架的商品
     * 包含两步：1、多台机器的分布式锁【注意，就算代码里已经有幂等性判断了，这个分布式锁也一定要有，比如两台机器同时进来，同时到redis判断键是否存在那里(前一个还没把键设置进去，另一个看诶还没有这键也跟着执行了)，二者同时执行了代码相当于幂等失败了，所以这锁很重要】
     *                      所以要保证同一时刻只有一台机器执行这段代码，业务完成后更新状态并把状态持久化住，这样别人就无法获得我的中间状态，结合下面的2、才能达到幂等；
     *          2、判断那个关键 key 是否已经存在于redis了，key要根据业务决策出一个合理值；
     */
    @Scheduled(cron = "*/3 * * * * ?")
//    @Scheduled(cron = "0 0 3 * * ?")
    public void uploadSeckillSkuLatest3Days(){
        // 1、虽然是三点上架，但加入今天有要秒杀的商品，其实在三天前已经被上架了，无需处理
        log.info("上架秒杀的商品信息");

        // 上分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);  // 释放时间，10s后释放
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        }finally {
            lock.unlock();
        }
    }
}
