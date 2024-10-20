package com.aeterna.friendmall.ware.service;

import com.aeterna.friendmall.ware.vo.FareVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.friendmall.ware.entity.WareInfoEntity;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 16:49:59
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    FareVo getFare(Long addrId);
}

