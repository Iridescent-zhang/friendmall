package com.aeterna.friendmall.order.service.impl;

import com.aeterna.common.exception.NoStockException;
import com.aeterna.common.to.SeckillOrderTo;
import com.aeterna.common.to.mq.OrderTo;
import com.aeterna.common.utils.R;
import com.aeterna.common.vo.MemberRespVo;
import com.aeterna.friendmall.order.constant.OrderConstant;
import com.aeterna.friendmall.order.dao.OrderItemDao;
import com.aeterna.friendmall.order.entity.OrderItemEntity;
import com.aeterna.friendmall.order.entity.PaymentInfoEntity;
import com.aeterna.friendmall.order.enume.OrderStatusEnum;
import com.aeterna.friendmall.order.feign.CartFeignService;
import com.aeterna.friendmall.order.feign.MemberFeignService;
import com.aeterna.friendmall.order.feign.ProductFeignService;
import com.aeterna.friendmall.order.feign.WmsFeignService;
import com.aeterna.friendmall.order.interceptor.LoginUserInterceptor;
import com.aeterna.friendmall.order.service.OrderItemService;
import com.aeterna.friendmall.order.service.PaymentInfoService;
import com.aeterna.friendmall.order.to.OrderCreateTo;
import com.aeterna.friendmall.order.vo.*;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.order.dao.OrderDao;
import com.aeterna.friendmall.order.entity.OrderEntity;
import com.aeterna.friendmall.order.service.OrderService;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static java.math.BigDecimal.ROUND_UP;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    WmsFeignService wmsFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    PaymentInfoService paymentInfoService;

    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    // 返回订单确认页需要的数据
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        // 通过拦截器里面的ThreadLocal(static类型的loginUser)获取到当前用户
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        System.out.println("主线程..."+Thread.currentThread().getId());

        // 获得老请求的请求参数，创建异步线程时都放到那些异步线程的RequestContextHolder里，让每个新线程都共享之前的请求数据。这个老指的是比我创建的feign新请求老，也就是指调用这个confirmOrder()的请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            System.out.println("第一个副线程..."+Thread.currentThread().getId());
            /**
             * 在每个新的线程里面都把老请求放进去，然后在各自feign调用时创建新请求时，通过这个【RequestContextHolder，里面共享数据用的就是ThreadLocal】获得老请求的请求头，
             *      正如ThreadLocal是每个线程都有的，RequestContextHolder也是每个线程都有的，每个新线程都保存一份老请求的请求参数requestAttributes，因为feign调用时还是在同一个线程内，所以可以获得这些数据
             */
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 1、远程查询用户所有的收货地址列表
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            System.out.println("第二个副线程..."+Thread.currentThread().getId());
            // 在每个新的线程里面都把老请求放进去，然后在各自feign调用时创建新请求时，通过这个【RequestContextHolder，里面共享数据用的就是ThreadLocal】获得老请求的请求头
            RequestContextHolder.setRequestAttributes(requestAttributes);
            /**
             * Feign在远程调用之前要构造新的请求【区别于我们这个函数被调用是由于浏览器发过来的/toTrade请求】，会调用很多的拦截器 : for (RequestInterceptor interceptor : requestInterceptors)
             * 用新的请求模板创建远程服务请求 ： Request request = targetRequest(template) ，结果这个模板 template 丢失了我们的请求头数据【cookie里的session】
             *      这样创建出来的 request 自然是有问题滴，没有任何请求头，没有session，所以购物车认为没有登录
             *
             * 因此我们来构造这些Feign的请求拦截器来为我们最终发送的请求做“增强”
             * 这些 public interface RequestInterceptor ：void apply(RequestTemplate template);会调用apply方法来增强request
             */
            // 之所以这个远程服务这么麻烦，就是因为没有传关键数据用户id，而我又想在没有关键数据的情况下自己从session里获得数据
            // 2、远程获取购物车中选中的所有购物项
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(()->{
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R skusHasStock = wmsFeignService.getSkusHasStock(collect);
            List<SkuStockVo> data = skusHasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                // 这种写法就得写SkuStockVo，item::getSkuId
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);

        // 3、查询用户积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);

        // 其他数据自动计算

        // 4、防重复提交令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(token);
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, TimeUnit.MINUTES);

        CompletableFuture.allOf(getAddressFuture, cartFuture).get();

        return confirmVo;
    }

    /**
     * 举个事务的例子
     * spring存在的bug：在同一个service(对象)里互相调事务方法相当于代码拷贝【原来是这样，解决了createOrder()里调用computePrice(orderEntity, orderItemEntities)结果似乎还能传递内存里的数据这个疑惑，原来只是代码拷贝】
     *                代码拷贝意味着没有走代理，而事务实际上是使用代理来控制的，所以这时调用的b、c做任何事务设置都没用（设置Propagation.REQUIRES_NEW的也没用）。
     *                除非b、c来自其他service，如 bService.b(), cService.c()，这时候使用其他服务的代理，事务设置当然能生效了
     */
    @Transactional(timeout = 30)  // a事务的所有设置传播到了和它共用一个事务的方法
    public void a(){
//        this.b();  // 没用
//        this.c();  // 没用
        // 用Aop上下文 AopContext 拿到当前代理对象
        OrderServiceImpl orderService = (OrderServiceImpl) AopContext.currentProxy();
        // 这样使用代理对象调方法，事务设置才能生效
        orderService.b();
        orderService.c();

//        bService.b();  // 属于a事务
//        cService.c();  // 新事务（不回滚）
        int i = 10/0;
    }

    @Transactional(propagation = Propagation.REQUIRED, timeout = 2)
    public void b(){
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 20)
    public void c(){
    }


    // 下单功能
    // 本地事务，在分布式系统下，只能控住自己的回滚，控制不了其他服务(远程调用的服务)的回滚
    // 使用分布式事务：最大适用场景：解决网络问题+分布式机器下的事务回滚
    /**
     * 事务隔离级别
     * @Transactional(isolation = Isolation.REPEATABLE_READ) MYSQL默认的可重复读，在事务运行期间即便其他事务修改了数据，我读到的依然不变。会有幻读现象
     * Isolation.READ_COMMITTED ：读已提交，其他事务已提交的我能读到，也叫不可重复读，因为这次读完可能又有事务提交了修改了这个数据
     * Isolation.READ_UNCOMMITTED ：读未提交，其他事务还没提交成功的我都能读到，可能导致脏读，就是我读到了还未提交的数据，结果后面其他事务回滚了，这个数据是无效的
     * Isolation.SERIALIZABLE：事务都是串行执行的，没有任何并发能力了，隔离级别最高
     */
    /**
     * 事务传播行为(这两个最常用)
     * 所谓传播行为就是一个事务调用了其他事务，这些其他事务是否被调用的事务传播(不会描述...)
     * propagation = Propagation.REQUIRED : 如果当前没有事务，就创建一个新事务，如果当前存在事务，
     * 就加入该事务，该设置是最常用的设置。
     * propagation = Propagation.REQUIRES_NEW : 创建新事务，无论当前存不存在事务，都创建新事务。
     */
//    @GlobalTransactional  // 下单是高并发场景，在这个场景下使用AT模式是不适合的
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        // 在ThreadLocal里面放数据，方便传递数据
        submitVoThreadLocal.set(vo);

        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);

        // 创建订单、验令牌、验价格、锁库存
        /**
         * 1、验令牌【验证和删除令牌是原子的】
         *    要求redis查令牌，和删除令牌这二者合起来为一个原子操作，原因如下：
         *      比如两个很近的请求，第一个查到了并且对比成功，执行业务并删除令牌，结果令牌还没删第二个也一查，发现redis中还有并且也匹配，然后执行业务，这样就会执行两次业务
         *      所以让查令牌和删令牌成为一个原子操作，这样第一个请求查了并删完第二个请求才会去查，注意理解原子(相当于二者合为一个整体)
         *    之前做过了，我们只需要给redis发送一个Lua脚本让它执行就行
         */
        String orderToken = vo.getOrderToken();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        // Lua 脚本，返回0/1，0：删除令牌失败，1：对比成功并且删除令牌成功
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
        if (result == 0L) {
            // 令牌失败
            responseVo.setCode(1);
            return responseVo;
        }else {
            // 令牌对比成功并且删除令牌成功，之后创建订单、验价格、锁库存
            // 创建了订单、订单项等信息
            OrderCreateTo order = createOrder();
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();  // 页面提交来的应付金额
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 3、金额对比成功，保存到数据库
                saveOrder(order);
                // 4、库存锁定，只要锁不了库存【有异常】就要回滚订单数据，所以设置事务
                //    订单号，所有订单项（skuId，num，skuName）
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                // 【很重要的操作】远程锁库存，返回锁定结果
                /**
                 * 库存锁成功了，但是网络原因返回结果超时抛出异常导致订单回滚了
                 * 高并发下使用分布式事务。
                 *     1、让库存服务自己回滚，具体就是订单回滚了然后努力一直发消息去让库存自己回滚，这样就没什么性能损耗了
                 *     2、库存服务自解锁，使用自动解锁模式，参与消息队列
                 */
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    // 锁库存成功
                    responseVo.setOrder(order.getOrder());
                    // 5、远程扣减积分（模拟这里出异常了，前面的锁定库存能否回滚：不能）
//                    int i = 10/0;  // 订单回滚，没有分布式事务库存不回滚
                    // TODO 订单创建成功发消息给 mq
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    return responseVo;
                }else {
                    // 锁定失败
                    // 为了让订单在锁库存失败的情况下也回滚，我们将整个submitOrder设为@Transactional，并且在此时抛出异常
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
//                    responseVo.setCode(3);
//                    return responseVo;
                }
            }else {
                // 金额对比失败验证码
                responseVo.setCode(2);
                return responseVo;
            }
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity entity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return entity;
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        // 查询订单状态，因为所有订单都会进延时队列，包括已支付的订单
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()){  // 到现在还是待付款
            // 关单=改状态
            OrderEntity update = new OrderEntity();
            update.setId(orderEntity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            // 发一个消息给MQ   "stock.release.stock.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.release.other.#"
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            try {
                // TODO 保证消息一定会发送出去。每个消息做好日志记录(给数据库保存每个消息的详细信息)，之后定期扫描数据库将失败的消息再发送一遍
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            }catch (Exception e) {
                // TODO 将没发送成功的消息进行重试发送

            }
        }

    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderByOrderSn(orderSn);

        BigDecimal payAmount = order.getPayAmount().setScale(2, ROUND_UP);
        payVo.setTotal_amount(payAmount.toString());  // 订单金额

        payVo.setOut_trade_no(order.getOrderSn());  // 订单号

        List<OrderItemEntity> list = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
        OrderItemEntity entity = list.get(0);
        payVo.setSubject(entity.getSkuName());  // 订单名称、主题，这里以订单中一个商品名作为标题

        payVo.setBody(entity.getSkuAttrsVals());  // 订单备注，这里设置为销售属性

        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id")
        );

        List<OrderEntity> orderSn = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> entities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(entities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(orderSn);

        return new PageUtils(page);
    }

    /**
     * 支付宝给我们发了异步通知告知支付结果，我们根据这个结果该修改数据库就修改数据库
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        // 保存交易流水：oms_payment_info
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());  // 支付宝的支付订单号
        infoEntity.setOrderSn(vo.getOut_trade_no());  // 这个是我们商城的订单号
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);

        // 修改订单状态
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            // 支付成功状态
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
        }
        
        return "success";
    }

    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrder) {
        // TODO 保存订单信息即可
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrder.getOrderSn());
        orderEntity.setMemberId(seckillOrder.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        BigDecimal multiply = seckillOrder.getSeckillPrice().multiply(new BigDecimal("" + seckillOrder.getNum()));
        orderEntity.setPayAmount(multiply);
        this.save(orderEntity);

        // 保存订单项信息
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderSn(seckillOrder.getOrderSn());
        itemEntity.setRealAmount(multiply);
        itemEntity.setSkuQuantity(seckillOrder.getNum());
        // 可以远程查当前skuId商品的详细信息
        orderItemService.save(itemEntity);
    }

    // 保存订单数据，包括订单和订单项
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder(){
        OrderCreateTo createTo = new OrderCreateTo();
        // 1、创建订单 OrderEntity entity
        // 生成订单号【用MyBatis的工具类】
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);

        // 2、 获取购物车内的订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);

        // 3、 计算价格相关【验价】
        computePrice(orderEntity, orderItemEntities);

        createTo.setOrder(orderEntity);
        createTo.setOrderItems(orderItemEntities);

        return createTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        // 订单总额是叠加每个订单项的总额
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        for (OrderItemEntity entity : orderItemEntities) {
            total = total.add(entity.getRealAmount());
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            gift = gift.add(BigDecimal.valueOf(entity.getGiftIntegration())) ;
            growth = growth.add(BigDecimal.valueOf(entity.getGiftGrowth()));
        }
        orderEntity.setTotalAmount(total);
        // 应付总额：上面的总金额加上运费
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);
        // 设置积分等信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());

        orderEntity.setDeleteStatus(0);  // 0代表未删除
    }

    // 构建所有订单项
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size()>0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity =  buildOrderItem(cartItem);
                // 1、订单信息
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    // 帮我们从某一个购物车项OrderItemVo构建订单项OrderItemEntity
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // 1、订单信息

        // 2、商品spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());

        // 3、商品sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());

        // 4、优惠信息略

        // 5、积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        // 6、订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));  // 促销
        itemEntity.setCouponAmount(new BigDecimal("0"));  // 优惠券
        itemEntity.setIntegrationAmount(new BigDecimal("0"));  // 积分
        BigDecimal origin = itemEntity.getSkuPrice().multiply(BigDecimal.valueOf(itemEntity.getSkuQuantity()));
        BigDecimal subtract = origin.subtract(itemEntity.getCouponAmount()).
                subtract(itemEntity.getPromotionAmount()).
                subtract(itemEntity.getIntegrationAmount());
        // 当前订单项的实际金额等于总额减各种优惠
        itemEntity.setRealAmount(subtract);

        return itemEntity;
    }

    // 生成订单实体 OrderEntity
    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(respVo.getId());

        OrderSubmitVo submitVo = submitVoThreadLocal.get();
        // 调用远程服务获取FareVo
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });

        // 设置运费
        entity.setFreightAmount(fareResp.getFare());
        // 设置收货地址
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());

        // 设置订单的状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);


        return entity;
    }
}