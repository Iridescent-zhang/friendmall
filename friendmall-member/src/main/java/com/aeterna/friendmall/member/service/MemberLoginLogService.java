package com.aeterna.friendmall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.friendmall.member.entity.MemberLoginLogEntity;

import java.util.Map;

/**
 * 会员登录记录
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 16:30:08
 */
public interface MemberLoginLogService extends IService<MemberLoginLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

