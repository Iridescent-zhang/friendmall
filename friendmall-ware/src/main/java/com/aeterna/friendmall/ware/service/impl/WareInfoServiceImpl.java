package com.aeterna.friendmall.ware.service.impl;

import com.aeterna.common.utils.R;
import com.aeterna.friendmall.ware.feign.MemberFeignService;
import com.aeterna.friendmall.ware.vo.FareVo;
import com.aeterna.friendmall.ware.vo.MemberAddressVo;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.ware.dao.WareInfoDao;
import com.aeterna.friendmall.ware.entity.WareInfoEntity;
import com.aeterna.friendmall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)) {
            wrapper.eq("id", key).or().like("name", key)
                    .or().like("address", key)
                    .or().like("areacode", key);
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    // 根据收货地址计算运费
    @Override
    public FareVo getFare(Long addrId) {

        R r = memberFeignService.addrInfo(addrId);
        MemberAddressVo data = r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {
        });
        if (data != null) {
            String phone = data.getPhone();
            String substring = phone.substring(phone.length() - 1, phone.length());
            BigDecimal bigDecimal = new BigDecimal(substring);

            FareVo fareVo = new FareVo();
            fareVo.setFare(bigDecimal);
            fareVo.setAddress(data);

            return fareVo;
        }
        return null;
    }

}