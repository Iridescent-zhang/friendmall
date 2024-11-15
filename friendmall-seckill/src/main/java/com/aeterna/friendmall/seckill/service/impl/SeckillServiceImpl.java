package com.aeterna.friendmall.seckill.service.impl;

import com.aeterna.common.to.SeckillOrderTo;
import com.aeterna.common.utils.R;
import com.aeterna.common.vo.MemberRespVo;
import com.aeterna.friendmall.seckill.feign.CouponFeignService;
import com.aeterna.friendmall.seckill.feign.ProductFeignService;
import com.aeterna.friendmall.seckill.interceptor.LoginUserInterceptor;
import com.aeterna.friendmall.seckill.service.SeckillService;
import com.aeterna.friendmall.seckill.to.SecKillSkuRedisTo;
import com.aeterna.friendmall.seckill.vo.SeckillSessionsWithSkus;
import com.aeterna.friendmall.seckill.vo.SeckillSkuVo;
import com.aeterna.friendmall.seckill.vo.SkuInfoVo;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.seckill.service.impl
 * @ClassName : .java
 * @createTime : 2024/8/26 16:32
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

/**
 * 上架秒杀商品
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private final String SESSION__CACHE_PREFIX = "seckill:sessions:";

    private final String SECKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";    //+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 扫描最近三天的秒杀活动和相应的商品
        R r = couponFeignService.getLatest3DaysSession();
        if (r.getCode() == 0) {
            List<SeckillSessionsWithSkus> sessionData = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            // 缓存到redis
            // 1、缓存活动信息：
            saveSessionInfos(sessionData);

            // 2、缓存活动的关联商品信息
            saveSessionSkuInfos(sessionData);
        }
    }

    /**
     * @SentinelResource 还提供了其它额外的属性如 blockHandler，blockHandlerClass，fallback 用于表示限流或降级的操作（注意有方法签名要求）
     *                      可以返回被限流后需要返回的数据【因为函数签名要和原方法一样】
     * blockHandler 在原方法被限流/降级时调用
     * fallback 会针对所有类型的异常，其实fallback写法和blockHandler很类似
     */
    public List<SecKillSkuRedisTo> blockHandler(BlockException e) {
        log.error("getCurrentSeckillSkusResource被限流了...");
        return null;
    }

    /**
     * 返回当前时间可以参与秒杀的商品信息
     */
    @SentinelResource(value = "getCurrentSeckillSkusResource", blockHandler = "blockHandler")
    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        // 1、确定当前时间属于的秒杀场次  TODO这里时间转换好像有bug 明明时间在后面但有可能数据还更小，很奇怪，比如现在8/28/13点结果还比8/28/06小
        long time = new Date().getTime();  // 这个是当前时间距离1970的差值

        /**
         * TODO：定义一段受保护的资源，从而可以执行流控熔断限流等策略，当被限流时就抛出异常 BlockException ，处理它就好了
         */
        try (Entry entry = SphU.entry("seckillSkus")){
            Set<String> keys = redisTemplate.keys(SESSION__CACHE_PREFIX + "*");
            for (String key : keys) {
                // 这里的话其实只能获得一个秒杀场次的商品信息，如果同时有多个场次实际上只获取了一个场次的商品信息就返回了，不过现实中应该同时也就只有一个场次
                // seckill:sessions:1724652000000_1724677200000
                String replace = key.replace("seckill:sessions:", "");
                String[] s = replace.split("_");
                long start = Long.parseLong(s[0]);
                long end = Long.parseLong(s[1]);
                if (time>=start && time<=end) {
                    // 2、获取这个秒杀场次需要的商品信息
                    List<String> range = redisTemplate.opsForList().range(key, 0, 100);
                    BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
                    List<String> list = hashOps.multiGet(range);
                    if (list != null) {
                        List<SecKillSkuRedisTo> collect = list.stream().map(item -> {
                            SecKillSkuRedisTo redisTo = JSON.parseObject( item, SecKillSkuRedisTo.class);
                            // 秒杀已经开始的话返回页面就带上随机码，否则不带
//                        redisTo.setRandomCode(null);
                            return redisTo;
                        }).collect(Collectors.toList());
                        return collect;
                    }
                    break;
                }
            }
        }catch (BlockException e) {
            log.error("资源被限流{}",e.getMessage());
        }

        return null;
    }

    @Override
    public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        // 找到所有参与秒杀的商品的key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            // 4_33
            String regx = "\\d_"+skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SecKillSkuRedisTo redisTo = JSON.parseObject(json, SecKillSkuRedisTo.class);

                    // 如果当前是在秒杀时间内，返回的时候就带上随机码，否则不带上
                    long current = new Date().getTime();
                    if (current >= redisTo.getStartTime() && current<=redisTo.getEndTime()) {
                    }else {
                        redisTo.setRandomCode("");
                    }
                    return redisTo;
                }
            }
        }
        return null;
    }

    // TODO 上架秒杀商品的时候每一个数据都要设置过期时间，秒杀后续的流程简化了收货地址等信息
    @Override
    public String kill(String killId, String key, Integer num) {

        long s1 = System.currentTimeMillis();

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        // 1、合法性校验【先获取当前秒杀商品的详细信息】
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
        // killId 是组装好session的标准格式
        String json = hashOps.get(killId);
        if (StringUtils.isEmpty(json)){
            return null;
        }else {
            SecKillSkuRedisTo redisTo = JSON.parseObject(json, SecKillSkuRedisTo.class);
            // 检验合法性
            // 1.校验秒杀时间的合法性
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            long time = new Date().getTime();

            long ttl = endTime - time;  // 毫秒

            if (time >= startTime && time <= endTime) {
                // 2.随机码和商品id是否正确
                String randomCode = redisTo.getRandomCode();
                String skuId = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    // 前端传来的随机码和skuId匹配上了redis里面存的对应的随机码、skuId，说明这个请求合法
                    // 3、验证购物数量是否合理
                    if (num <= redisTo.getSeckillLimit()){
                        // 4、验证这个人是否购买过了【幂等性字段】：只要秒杀成功就去redis占位：userId_sessionId_skuId，标志这个人买过了这个东西且买了这么几件
                        String redisUserKey = memberRespVo.getId() + "_" + skuId;
                        // 设置超时时间
                        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent(redisUserKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (ifAbsent) {
                            // 占位成功，说明之前没买过，允许买
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                            // 一定用tryAcquire不用acqueire，因为acquire没拿到信号量会一直在那阻塞等
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                // 秒杀成功，快速下单，给MQ发个消息【订单号】
                                String orderSn = IdWorker.getTimeId();
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(orderSn);
                                orderTo.setMemberId(memberRespVo.getId());
                                orderTo.setNum(num);
                                orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                orderTo.setSkuId(redisTo.getSkuId());
                                orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);

                                long s2 = System.currentTimeMillis();
                                log.info("整个耗时时间：{}",(s2-s1));
                                return orderSn;
                            }
                            return null;
                        }else {
                            return null;
                        }
                    }else {
                        return null;
                    }
                }else {
                    return null;
                }
            }else {
                return null;
            }
        }
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions){
        if (sessions!=null) {
            sessions.stream().forEach(session -> {
                Long startTime = session.getStartTime().getTime();
                Long endTime = session.getEndTime().getTime();
                String key = SESSION__CACHE_PREFIX + startTime + "_" + endTime;
                Boolean hasKey = redisTemplate.hasKey(key);
                // TODO 幂等性处理，没有这个key才执行
                if (!hasKey) {
                    List<String> skuIds = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId()+"_"+item.getSkuId().toString()).collect(Collectors.toList());
                    // 缓存秒杀活动信息
                    redisTemplate.opsForList().leftPushAll(key, skuIds);
                }

            });
        }
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            // 准备一个hash绑定操作
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(seckillSkuVo -> {

                // 4、当前商品的随机码  seckill?skuId=1&key=token，随机码是一种保护机制，秒杀开始的时候带上才能参与秒杀，只带skuId不能参与，防止被爆破
                String token = UUID.randomUUID().toString().replace("-", "");

                // TODO 幂等性处理，没有这个key才执行，由于不同场次可能上架同一skuId的商品，所以我们用来维护幂等性状态的key应该设为秒杀场次_skuId
                if (!hashOps.hasKey(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString())){
                    // 缓存商品
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();

                    // 1、sku的基本数据
                    R r = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo info = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(info);
                    }

                    // 2、sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);

                    // 3、当前商品的秒杀开始结束时间
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    // 4、当前商品的随机码  seckill?skuId=1&key=token，随机码是一种保护机制，秒杀开始的时候带上才能参与秒杀，指代skuId不能参与，防止被爆破
                    redisTo.setRandomCode(token);

                    String jsonString = JSON.toJSONString(redisTo);
//                    redisTo.setSeckillLimit(seckillSkuVo.getSeckillCount());
                    // TODO 幂等性处理，由于不同场次可能上架同一skuId的商品，所以我们用来维护幂等性状态的key应该设为秒杀场次_skuId
                    hashOps.put(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString(), jsonString);

                    /**
                     * 分布式锁：信号量相当于停车位，一共就这么多，你百万请求进来也只能秒杀这么些商品，抢到了就放过去，没抢到就只直接拒绝，这样才能处理高并发。
                     * 信号量的一大作用：【限流】
                     */
                    // TODO 幂等性：如果当前场次的这个skuId商品的库存信息已经上架就不需要上架，注意不同场次可能上架同一个商品
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    // 商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());  // 设置信号量
                }
            });
        });
    }
}
