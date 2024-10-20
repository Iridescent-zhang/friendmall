package com.aeterna.friendmall.member.service;

import com.aeterna.friendmall.member.exception.PhoneExistException;
import com.aeterna.friendmall.member.exception.UserNameExistException;
import com.aeterna.friendmall.member.vo.MemberLoginVo;
import com.aeterna.friendmall.member.vo.MemberRegistVo;
import com.aeterna.friendmall.member.vo.SocialUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.friendmall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-13 16:30:08
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;

    void checkUserNameUnique(String userName) throws UserNameExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser socialUser) throws Exception;
}

