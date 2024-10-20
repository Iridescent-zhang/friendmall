package com.aeterna.friendmall.member.service.impl;

import com.aeterna.common.utils.HttpUtils;
import com.aeterna.friendmall.member.dao.MemberLevelDao;
import com.aeterna.friendmall.member.entity.MemberLevelEntity;
import com.aeterna.friendmall.member.exception.PhoneExistException;
import com.aeterna.friendmall.member.exception.UserNameExistException;
import com.aeterna.friendmall.member.vo.MemberLoginVo;
import com.aeterna.friendmall.member.vo.MemberRegistVo;
import com.aeterna.friendmall.member.vo.SocialUser;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.member.dao.MemberDao;
import com.aeterna.friendmall.member.entity.MemberEntity;
import com.aeterna.friendmall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {

        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = new MemberEntity();

        // 设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getLDefaultevel();
        entity.setLevelId(levelEntity.getId());

        // 检查用户名和手机号是否唯一，为了让controller能感知到报错，使用异常机制
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());
        entity.setNickname(vo.getUserName());

        // 设置密码，前端传来的密码是明文，需要加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);

        // 其他默认信息

        // 在数据库中保存
        memberDao.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException{
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count>0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUserNameUnique(String userName) throws UserNameExistException{
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (count>0) {
            throw new UserNameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword(); // 没加密的明文，如123456

        // 用用户名或手机号去查数据库里的加密的密码，然后看是否能匹配
        // select * FROM ums_member where username=? or mobile=?
        MemberEntity selectOne = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct)
                .or().eq("mobile", loginacct));
        if (selectOne==null) {
            // 登录失败
            return null;
        }else {
            // 数据库里这个账号对应的密码
            String passwordDb = selectOne.getPassword(); // 123456加密后的密文
            // 用Spring的BCryptPasswordEncoder进行匹配
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if (matches){
                return selectOne;
            }else {
                return null;
            }
        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        // 登录和注册合并逻辑
        String uid = socialUser.getUid();
        // 1. 判断当前社交用户是否登录过系统
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (memberEntity != null) {
            // 1. 用户注册过了，这个社交账号的信息在member的数据库里有，但是要更新access_token等数据
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            memberDao.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        }else {
            // 2. 没查到当前社交用户的账号信息，所以注册一个
            MemberEntity regist = new MemberEntity();
            try {
                // 查社交用户信息，除了uid是必要的，其它的这些信息都不算必要，没取到也没关系，所以try catch，不要把异常抛出去
                Map<String, String> query = new HashMap<>();
                query.put("access_token", socialUser.getAccess_token());
                HttpResponse SocialInfo = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", new HashMap<>(), query);
                if (SocialInfo.getStatusLine().getStatusCode() == 200) {
                    String jsonInfo = EntityUtils.toString(SocialInfo.getEntity());
                    JSONObject jsonObject = JSON.parseObject(jsonInfo);
                    // 昵称
                    String name = jsonObject.getString("name");
                    // gitee没性别，随便写
                    String gender = "m";
                    // ....
                    regist.setNickname(name);
                    regist.setGender(1);
                    // ....
                }
            }catch (Exception e){}
            regist.setSocialUid(socialUser.getUid());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());

            memberDao.insert(regist);

            return regist;
        }
    }
}