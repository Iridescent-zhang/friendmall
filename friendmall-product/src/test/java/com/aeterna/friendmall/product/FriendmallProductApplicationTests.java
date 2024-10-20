package com.aeterna.friendmall.product;

import com.aeterna.friendmall.product.dao.AttrGroupDao;
import com.aeterna.friendmall.product.dao.SkuSaleAttrValueDao;
import com.aeterna.friendmall.product.entity.BrandEntity;
import com.aeterna.friendmall.product.service.BrandService;
import com.aeterna.friendmall.product.service.CategoryService;
import com.aeterna.friendmall.product.vo.SkuItemSaleAttrVo;
import com.aeterna.friendmall.product.vo.SkuItemVo;
import com.aeterna.friendmall.product.vo.SpuItemAttrGroupVo;
import com.aliyun.oss.OSSClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import org.junit.Test;  // 这是junit4的写法
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;  // 这是junit5 高版本springboot默认集成这个
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@SpringBootTest
public class FriendmallProductApplicationTests {

    @Autowired
    private BrandService brandService;

    /**
     * 这里由于我们把OSS业务全交给thirdparty了，common里面已经没有相关的依赖了，所以会报错，注掉就行了
     */
//    @Autowired
//    OSSClient ossClient;

    @Autowired
    CategoryService categoryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;

    @Test
    public void testFindParentPath(){
        Long[] catelogPath = categoryService.findCatelogPath(new Long(225));
        log.info("完整路径：{}", Arrays.asList(catelogPath));
    }

    @Test
    public void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();

//        brandEntity.setName("华为");
//        brandService.save(brandEntity);
//        System.out.println("brandEntity = " + brandEntity);

//        brandEntity.setBrandId(1L);
//        brandEntity.setDescript("海斯");
//        brandService.updateById(brandEntity);
        List<BrandEntity> brandEntityList = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1));
        brandEntityList.forEach((item)->{
            System.out.println("item = " + item);
        });
    }

    /**
     * 1.引入oss-starter
     * 2.配置key，endpoint
     * 3.使用OSSClient
     * @throws FileNotFoundException
     */


    @Test
    public void testStringRedisTemplate() {
        // key：hello   value：world
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        // 保存简单数据
        ops.set("hello", "world" + UUID.randomUUID().toString());

        // 查询
        String s = ops.get("hello");

        System.out.println("之前保存的数据 = " + s);
    }

    @Test
    public void testRedisson(){
        System.out.println(redissonClient);
    }

    @Test
    public void testAttr(){
//        List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(100L, 225L);
//        System.out.println("attrGroupWithAttrsBySpuId = " + attrGroupWithAttrsBySpuId);
        List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(23L);
        System.out.println("saleAttrsBySpuId = " + saleAttrsBySpuId);
    }
}
