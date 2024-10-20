package com.aeterna.friendmall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.friendmall.ware.entity.PurchaseDetailEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 16:49:59
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<PurchaseDetailEntity> listDetailByPurchaseId(Long id);
}

