package com.aeterna.friendmall.ware.service.impl;

import com.aeterna.common.exception.NoStockException;
import com.aeterna.common.to.mq.OrderTo;
import com.aeterna.common.to.mq.StockDetailTo;
import com.aeterna.common.to.mq.StockLockedTo;
import com.aeterna.common.utils.R;
import com.aeterna.friendmall.ware.entity.WareOrderTaskDetailEntity;
import com.aeterna.friendmall.ware.entity.WareOrderTaskEntity;
import com.aeterna.friendmall.ware.feign.OrderFeignService;
import com.aeterna.friendmall.ware.feign.ProductFeignService;
import com.aeterna.friendmall.ware.service.WareOrderTaskDetailService;
import com.aeterna.friendmall.ware.service.WareOrderTaskService;
import com.aeterna.friendmall.ware.vo.*;
import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.ware.dao.WareSkuDao;
import com.aeterna.friendmall.ware.entity.WareSkuEntity;
import com.aeterna.friendmall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    OrderFeignService orderFeignService;

    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        // 解锁库存 update wms_ware_sku set stock_locked=stock_locked-#{} where sku_id=#{} and ware_id=#{}
        wareSkuDao.unLockStock(skuId, wareId, num);
        // 更新库存详细工作单的状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);  // 设为已解锁
        wareOrderTaskDetailService.updateById(entity);
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        /**
         * skuId: 2
         * wareId: 2
         */

        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 如果没有这个库存记录就是新增操作而不是更新
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (entities==null || entities.size()==0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            // 多查一些前端显示用的数据
            // 远程查询sku的名字 ，并且怕由于feign不稳定查询失败，不能因为这个字段没查到就回滚整个事务，所以把异常catch处理，不要抛出去，没有异常抛出事务就不会回滚
            //TODO 还有什么方法能让异常出现后不回滚呢？@Transactional(rollbackFor=Exception.class)？编程式事务?高级部分
            try {
                R info = productFeignService.info(skuId);
                // 这个用法挺奇怪，强转SkuInfoEntity为Map，能否直接用SkuInfoEntity接值，还是都转为Map？实体类转Map效果为何
                Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                if(info.getCode()==0) {
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            }catch (Exception e){

            }
            wareSkuEntity.setStockLocked(0);
            wareSkuDao.insert(wareSkuEntity);
        }
        else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();

            // 查询当前sku的总库存量
            //SELECT SUM(stock-stock_locked) from `wms_ware_sku` WHERE sku_id = 1
            Long count = baseMapper.getSkuStock(skuId);

            vo.setSkuId(skuId);
            vo.setHasStock(count==null ? false : count>0);

            return vo;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 为某个订单锁定库存
     * @Transactional(rollbackFor = NoStockException.class)，不标也可以，因为默认运行时异常就会回滚
     * 库存解锁场景：
     *    1、下订单成功，订单过期没有支付自动取消或用户取消订单；
     *    2.1、下订单成功，(部分)库存锁定成功，接下来的业务调用失败或解锁库存返回结果由于网络原因超时，导致订单回滚。之前锁定的库存需要自动解锁。
     *    2.2、 库存一部分商品锁定失败，区别于上面，库存服务自身回滚了，数据库里没有工作单详情了，但消息还是发出去了
     */
    @Transactional(rollbackFor = NoStockException.class)  // 某一个商品锁不了，它之前的商品锁了的也要回滚
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单【WareOrderTaskEntity】的详情，为了追溯哪个仓库当时锁了多少方便后面查找问题
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(taskEntity);

        // 1、按照下单收货地址找一个就近仓库然后锁库存。太复杂，改成找每个商品在那个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            // 查询这个商品在哪个仓库有库存
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        // 2、锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;

            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds==null || wareIds.size()==0) {
                // 没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            /**
             * 1、如果每一个商品锁定成功，将这个商品锁定了几件的详情工作单发送给MQ
             * 2、如果不全锁定成功，前面保存的工作单信息就回滚了【因为这里是本地事务】，前面发出去的消息带的详情单在数据库里已经没有了，但是我们通过发送详情单完整信息还能回到wms_ware_sku把锁定的库存再解锁掉
             */
            for (Long wareId : wareIds) {
                // 成功返回1.否则0就是锁库存失败
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    skuStocked = true;
                    //TODO 告诉MQ库存锁定成功
                    WareOrderTaskDetailEntity orderTaskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1);
                    wareOrderTaskDetailService.save(orderTaskDetailEntity);
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(taskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(orderTaskDetailEntity, stockDetailTo);
                    // 只给mq发工作单详情id是不够的，防止库存锁了一部分结果还回滚之后找不到数据无法去解锁那些锁了的
                    lockedTo.setDetailTo(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    break;
                }else {
                    // 当前仓库锁失败，尝试下一个仓库
                }
            }
            if (skuStocked == false) {
                // 当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        // 3、能走到这就是全部锁成功了
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {
        StockDetailTo detail = to.getDetailTo();
        Long detailId = detail.getId();
        /**
         * 查询数据库关于这个订单的工作单详情情况：
         *    有：上面情况2.1
         *        1、订单没了，必须要解锁
         *        2、延时到现在，有订单的话要根据订单状态进行一个判定
         *            1)已取消，解锁库存
         *            2)没取消，不能解锁
         *    没有：库存锁定失败，上面情况2.2，此时无需解锁
         */
        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
        if (byId != null) {
            // 有：上面情况2.1
            Long id = to.getId();  // 库存工作单的id
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();  // 根据订单号查询订单状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                //订单数据返回成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });
                // 判断订单状态
                if (data == null || data.getStatus() == 4) {  // "已取消"
                    // 订单不存在了对应情况2.1 或 订单被取消对应情况1，需要解锁库存
                    if (byId.getLockStatus() == 1) {
                        // 只有当前库存工作单详情的状态是锁定的【状态1】才需要去解锁
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                // 不拒绝消息了，错误应该就出异常了，异常抛出去
                throw new RuntimeException("远程服务失败");
            }
        } else {
            // 没有：库存锁定失败，上面情况2.2，此时无需解锁
        }
    }

    // 防止订单服务卡顿，导致订单状态没改而库存消息优先到期，此时查订单状态发现还是新建状态，什么都没做就走了，结果回头订单取消了，那这库存就一直解不了了
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        // 查最新库存工作单详情的状态，防止重复解锁库存
        WareOrderTaskEntity task = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long taskId = task.getId();
        // 按照库存工作单id找到左右没有解锁的库存进行解锁
        List<WareOrderTaskDetailEntity> entities = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("lock_status", 1)
                .eq("task_id", taskId));
        // Long skuId, Long wareId, Integer num, Long taskDetailId
        for (WareOrderTaskDetailEntity entity : entities) {
            unLockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }
    }

    // 用内部类来保存商品在那些仓库有库存
    @Data
    class SkuWareHasStock{
        private Long skuId;
        private Integer num;  // 要锁几件
        private List<Long> wareId;
    }

}