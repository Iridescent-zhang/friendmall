package com.aeterna.friendmall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.friendmall.coupon.entity.HomeAdvEntity;

import java.util.Map;

/**
 * 首页轮播广告
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 13:14:11
 */
public interface HomeAdvService extends IService<HomeAdvEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

