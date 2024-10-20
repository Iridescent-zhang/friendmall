package com.aeterna.friendmall.product.service.impl;

import com.aeterna.friendmall.product.dao.BrandDao;
import com.aeterna.friendmall.product.dao.CategoryDao;
import com.aeterna.friendmall.product.entity.BrandEntity;
import com.aeterna.friendmall.product.entity.CategoryEntity;
import com.aeterna.friendmall.product.service.BrandService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.product.dao.CategoryBrandRelationDao;
import com.aeterna.friendmall.product.entity.CategoryBrandRelationEntity;
import com.aeterna.friendmall.product.service.CategoryBrandRelationService;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    BrandDao brandDao;

    // 一般业务逻辑可以注入别的业务逻辑的Service，因为Service功能要比Dao更丰富，并且Dao不好接着扩展
    @Autowired
    BrandService brandService;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationDao categoryBrandRelationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 用来保存前端传过来的新的品牌和类的关联关系，这个请求只传两个id过来，我们自己去找两个id分别对应的名字，
     *          不要去做数据库的表关联，因为大表做数据库关联查询性能会很低，在后端分布也得解决了
     */
    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();
        // 查询品牌名
        BrandEntity brandEntity = brandDao.selectById(brandId);

        // 查询类名
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);

        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatelogName(categoryEntity.getName());

        this.save(categoryBrandRelation);
    }

    @Override
    public void updateBrand(Long brandId, String name) {

        CategoryBrandRelationEntity categoryBrandRelationEntity = new CategoryBrandRelationEntity();
        categoryBrandRelationEntity.setBrandId(brandId);
        categoryBrandRelationEntity.setBrandName(name);

        this.update(categoryBrandRelationEntity, new UpdateWrapper<CategoryBrandRelationEntity>()
                .eq("brand_id", brandId));
    }

    @Override
    public void updateCategory(Long catId, String name) {
        // 这里尝试另一种方法
        this.baseMapper.updateCategory(catId, name);
    }

    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {
        List<CategoryBrandRelationEntity> relationEntities = categoryBrandRelationDao.selectList(new QueryWrapper<CategoryBrandRelationEntity>()
                .eq("catelog_id", catId));
        List<BrandEntity> brandEntities = relationEntities.stream().map((relationEntity) -> {
            Long brandId = relationEntity.getBrandId();
            BrandEntity byId = brandService.getById(brandId);
            return byId;
        }).collect(Collectors.toList());

        return brandEntities;
    }

}