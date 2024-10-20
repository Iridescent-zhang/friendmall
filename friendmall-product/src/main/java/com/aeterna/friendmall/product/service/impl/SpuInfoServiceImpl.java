package com.aeterna.friendmall.product.service.impl;

import com.aeterna.common.constant.ProductConstant;
import com.aeterna.common.to.SkuHasStockVo;
import com.aeterna.common.to.SkuReductionTo;
import com.aeterna.common.to.SpuBoundTo;
import com.aeterna.common.to.es.SkuEsModel;
import com.aeterna.common.utils.R;
import com.aeterna.friendmall.product.entity.*;
import com.aeterna.friendmall.product.feign.CouponFeignService;
import com.aeterna.friendmall.product.feign.SearchFeignService;
import com.aeterna.friendmall.product.feign.WareFeignService;
import com.aeterna.friendmall.product.service.*;
import com.aeterna.friendmall.product.vo.*;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * // TODO 高级部分完善：事务失败了怎么办：分布式事务
     *  @GlobalTransactional
     * 因为这个函数要保存很多东西，所以赋予它事务属性
     */
    // 这里就很适合 Seata 的AT模式，因为这个模式不能适应高并发请求的分布式事务，在这个场景下就很适用
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        /**
         * 1. 保存Spu基本信息
         *    一共三张表 主表pms_spu_info基本信息  pms_spu_images后面SKU(特定规格的产品)选图时候的图片集  pms_spu_info_desc用来介绍的图片集
         */
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);


        /**
         * 2. 保存Spu的描述图片
         *          private List<String> decript;
         *          pms_spu_info_desc用来介绍的图片集
         */
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",",decript));
        spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);

        /**
         * 3. 保存Spu的图片集
         *          private List<String> images;
         *          pms_spu_images后面SKU选图时候的图片集
         */
        List<String> images = vo.getImages();
        spuImagesService.saveImages(spuInfoEntity.getId(), images);

         /**
         * 4. 保存spu的规格参数
         *          private List<BaseAttrs> baseAttrs;
         *          在表pms_product_attr_value里有对应的三个属性
         */
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map((attr) -> {
            ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
            productAttrValueEntity.setAttrId(attr.getAttrId());
            productAttrValueEntity.setAttrName(attrService.getById(attr.getAttrId()).getAttrName());  // 页面没提交过来，自己查
//            productAttrValueEntity.setAttrName(attrService.getAttrInfo(attr.getAttrId()).getAttrName());  // 页面没提交过来，自己查
            productAttrValueEntity.setAttrValue(attr.getAttrValues());
            productAttrValueEntity.setQuickShow(attr.getShowDesc());
            productAttrValueEntity.setSpuId(spuInfoEntity.getId());
            return productAttrValueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(collect);

        /**
         * 6. 保存spu的积分信息 这里开始就要远程调用coupon服务了
         *      private Bounds bounds; 操作表 `friendmall_sms`->sms_spu_bounds
         */
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("远程调用服务保存spu bound积分信息失败");
        }

        /**
         * 5. 保存当前spu对应的所有sku信息
         *     private List<Skus> skus;
          */
        List<Skus> skus = vo.getSkus();
        if(skus!=null && skus.size()!=0) {
            skus.forEach(item->{
                // 这里找默认图片
                String defaultImage = "";
                for (Images image : item.getImages()) {
                    if(image.getDefaultImg()==1) {
                        defaultImage = image.getImgUrl();
                    }
                }
                //     5.1 sku的基本信息 pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImage);  // 图片集里每个Image都有个属性判断是否是默认图片
                skuInfoService.saveSkuInfo(skuInfoEntity);

                // 上面插入Sku之后自增的主键就出来了，后面的操作就可以用这个主键
                Long skuId = skuInfoEntity.getSkuId();

                //     5.2 sku的图片信息 pms_sku_images
                List<SkuImagesEntity> imagesEntityList = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
//                    BeanUtils.copyProperties(img, skuImagesEntity); 等效下面两步
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity->{
                    // 返回true即为需要 返回false剔除
                    return !StringUtils.isEmpty(entity.getImgUrl());  // 用条件表达式过滤空的ImgUrl
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(imagesEntityList);

                //     5.3 sku的销售属性信息 pms_sku_sale_attr_value
                List<Attr> attrs = item.getAttr();
                List<SkuSaleAttrValueEntity> saleAttrValueEntities = attrs.stream().map(attr -> {
                    SkuSaleAttrValueEntity saleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, saleAttrValueEntity);
                    saleAttrValueEntity.setSkuId(skuId);
                    return saleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(saleAttrValueEntities);

                //     5.4 sku的优惠、满减信息 `friendmall_sms`->sms_sku_ladder打折表/sms_sku_full_reduction满减表/sms_member_price会员价格表
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getFullCount()>0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0"))==1) {
                    // 满减有意义时才调用远程服务
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("远程调用服务保存sku 优惠信息失败");
                    }
                }
            });
        }

    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        /**
         *         status: 1
         *         key:
         *         brandId: 9
         *         catelogId: 225
         *     拼接查询条件
         */
        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status)) {
            wrapper.eq("publish_status",status);
        }

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)) {
            /**
             * 这种wrapper.and(()->{})相当于最外层是一个and，而里面可以包含许多条件
             *  比如这里，key与其他的就应该为and关系，同时key在内部使用时又有一个或的关系，这个或不能放出来和status等拼在一块
             *  status=1 and (id=1 or spu_name like xx) 也就是要这样的效果
             */
            wrapper.and((w)->{
                w.eq("id", key).or().like("spu_name",key);
            });
        }
        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId)&&!"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId)&&!"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 上架spu是指上架这个spu下的所有sku
     */
    @Override
    public void up(Long spuId) {

        // 1. 查出当前spuid对应的所有sku信息、品牌名
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);

        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        // 3. 查询当前sku对应的spu的所有可以被检索的的规格属性  只查一遍就行
        List<ProductAttrValueEntity> baseAttrListForSpu = productAttrValueService.baseAttrListForSpu(spuId);
        List<Long> attrIds = baseAttrListForSpu.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        // 挑出能被检索的attrId
        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);

        Set<Long> idSet = new HashSet<>(searchAttrIds);

        // 过滤出能被检索的attrId，用上面的 searchAttrIds
        List<SkuEsModel.Attrs> attrsList = baseAttrListForSpu.stream().filter(item -> {
            return idSet.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
//            attrs.setAttrId(item.getAttrId());
//            attrs.setAttrValue(item.getAttrValue());
//            attrs.setAttrName(item.getAttrName());
            BeanUtils.copyProperties(item, attrs);
            return attrs;
        }).collect(Collectors.toList());

        Map<Long, Boolean> hasStockMap = null;
        try {
            // 1. 发送远程调用问ware有没有库存hasStock
            R r = wareFeignService.getSkusHasStock(skuIdList);
            /**
             * 这是 alibaba 的 fastJSON 里面的 TypeReference 的用法，有点难度
             *      主要是为了方便在R里面存取数据
             */
            // 因为 typeReference 的构造器受保护，所以写成这样的内部类对象
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>(){};
            // 封装成map
            hasStockMap = r.getData(typeReference).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        }catch (Exception e) {
            log.error("远程查询库存服务异常：原因{}",e);
        }
        Map<Long, Boolean> finalHasStockMap = hasStockMap;

        // 2. 封装每个 SkuInfoEntity 成 SkuEsModel
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            // 属性对拷
            BeanUtils.copyProperties(sku, esModel);

            // 由于名字不一样导致要另外弄
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            // 由于没有对应的成员导致要另外弄 hasStock hotScore
            // 设置是否有库存
            if (finalHasStockMap ==null) {
                // 远程调用ware服务失败，默认由数据
                esModel.setHasStock(true);
            }
            else {
                esModel.setHasStock(finalHasStockMap.get(sku.getSkuId()));
            }

            //TODO：2. hotScore 热度评分 可以先给个默认0
            esModel.setHotScore(0L);

            // 3. 查询品牌和分类名
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());

            CategoryEntity category = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(category.getName());

            //  设置 可检索 的属性
            esModel.setAttrs(attrsList);

            return esModel;
        }).collect(Collectors.toList());

        //TODO 4. 将数据发送个给es，通过调用friendmall-search
        R r = searchFeignService.productStatusUp(upProducts);
        if(r.getCode() == 0) {
            // 远程调用成功
            // 5. 该spu旗下的所有sku上架成功后要修改该spu的状态为已上架
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }
        else {
            // 远程调用失败
            // TODO 重复调用？ 接口幂等性； 重试机制
            /**
             * Feign调用流程 Feign是有重试机制的
             *      Feign 调用流程
             * 1. 构造请求数据，将对象转为json
             *      SynchronousMethodHandler.java->类invoke->语句RequestTemplate template = buildTemplateFromArgs.create(argv);
             * 2. 发送请求并执行（执行成功会解码响应数据）
             *      return executeAndDecode(template);
             * 3. 执行请求会有重试机制
             *   while(true) {
             *       try {
             *         return executeAndDecode(template);   // 尝试执行
             *       } catch (RetryableException e) {  // 两个不同异常两个try catch，各自处理，这回理解了
             *          try {
             *             retryer.continueOrPropagate(e);  // 重试器
             *          } catch (RetryableException th) {  // 这是重试次数超了,不允许重试时抛的异常
             *            处理异常
             *          }
             *          continue;
             *       }
             *   }
             */

        }
    }

    // 按照skuId获取spu信息
    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {

        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);
        Long spuId = skuInfo.getSpuId();
        SpuInfoEntity byId = this.getById(spuId);

        return byId;
    }
}