package com.aeterna.friendmall.ware.service.impl;

import com.aeterna.common.constant.WareConstant;
import com.aeterna.friendmall.ware.entity.PurchaseDetailEntity;
import com.aeterna.friendmall.ware.service.PurchaseDetailService;
import com.aeterna.friendmall.ware.service.WareSkuService;
import com.aeterna.friendmall.ware.vo.MergeVo;
import com.aeterna.friendmall.ware.vo.PurchaseDoneVo;
import com.aeterna.friendmall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.ware.dao.PurchaseDao;
import com.aeterna.friendmall.ware.entity.PurchaseEntity;
import com.aeterna.friendmall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", 0)
                        .or().eq("status", 1)
        );

        return new PageUtils(page);
    }

    @Transactional  // 大事务
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if(purchaseId==null) {
            // 需要自己新建一个采购单
            PurchaseEntity purchaseEntity = new PurchaseEntity();

            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);

            purchaseId = purchaseEntity.getId();
        }

        //TODO 确认采购单状态是新建0或者已分配1才能把采购需求合并到这里，其他状态的采购单不能再加采购需求了

        // 修改这几项采购需求的 两个字段（采购单id 和 状态status）
        //TODO 只有状态为新建或已分配的采购需求才能被合并到采购单里面 这里需要过滤
        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(item -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();

            purchaseDetailEntity.setId(item);  // 采购需求的主键
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());
        // 批量修改
        purchaseDetailService.updateBatchById(collect);

        // 采购单被修改了 所以改一下更新时间
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    /**
     * @param ids 采购单的id（可能有不符合要求的采购单，要过滤）
     */
    @Override
    public void received(List<Long> ids) {
        /**
         * 1. 只能领取新建或已分配的采购单，2. 并改变这个采购单的状态，3. 这个采购单下的所有采购需求的状态也要改
         */
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            PurchaseEntity entity = this.getById(id);
            return entity;
        }).filter(entity -> {
            if (entity.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode()
                    || entity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;  // return true 表示要这个 entity
            }
            return false;
        }).map(entity->{
            entity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            entity.setUpdateTime(new Date());
            return entity;
        }).collect(Collectors.toList());

        // 2. 并改变这个采购单的状态
        this.updateBatchById(collect);

        // 3. 这个采购单下的所有采购需求的状态也要改
        collect.forEach((item)->{
            // 通过采购单Id获取其中的采购需求
            List<PurchaseDetailEntity> detailEntities = purchaseDetailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> detailEntityList = detailEntities.stream().map(entity -> {
                PurchaseDetailEntity entity1 = new PurchaseDetailEntity();
                entity1.setId(entity.getId());
                entity1.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return entity1;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(detailEntityList);
        });
    }

    @Override
    public void done(PurchaseDoneVo doneVo) {
        // 1.改变采购单状态 2.改变采购项状态 3. 将成功采购的进行入库

        // 1.改变采购单状态 采购单状态可能是有异常，表示其中一部分采购需求成功了，一部分失败了
        Long id = doneVo.getId();

        // 2.改变采购项状态
        Boolean flag = true;  // 用来标志采购单到底是全成功了还是有异常
        List<PurchaseItemDoneVo> items = doneVo.getItems();

        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            // 遍历采购项的状态
            if(item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                flag = false;
                detailEntity.setStatus(item.getStatus());
            }else {
                detailEntity.setStatus(item.getStatus());
                // 3. 将成功采购的进行入库
                PurchaseDetailEntity byId = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(byId.getSkuId(), byId.getWareId(), byId.getSkuNum());
            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }
        purchaseDetailService.updateBatchById(updates);

        // 1.改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);  // id = doneVo.getId()
        purchaseEntity.setStatus(flag? WareConstant.PurchaseStatusEnum.FINISH.getCode()
                : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);


    }

}