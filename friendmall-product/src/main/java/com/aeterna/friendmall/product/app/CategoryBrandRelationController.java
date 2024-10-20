package com.aeterna.friendmall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aeterna.friendmall.product.entity.BrandEntity;
import com.aeterna.friendmall.product.vo.BrandVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.aeterna.friendmall.product.entity.CategoryBrandRelationEntity;
import com.aeterna.friendmall.product.service.CategoryBrandRelationService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.R;



/**
 * 品牌分类关联
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-12 19:03:41
 */
@RestController
@RequestMapping("product/categorybrandrelation")
public class CategoryBrandRelationController {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    /**
     * categorybrandrelation 获取当前品牌关联的所有分类的列表
     */
    @GetMapping("/catelog/list")
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R categoryList(@RequestParam("brandId") Long brandId){
        List<CategoryBrandRelationEntity> data = categoryBrandRelationService.list(
                new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId)
        );

        return R.ok().put("data", data);
    }

    /**
     * /categorybrandrelation/brands/list
     *
     */
    @GetMapping("brands/list")
    public R relationBrandsList(@RequestParam(value = "catId", required = true) Long catId){

        /**
         * 先查出所有品牌，自己再组装成vo
         * 因为这个函数可能会被其他调用，返回数据更多的BrandEntity适用性更广
         *          // 我保持质疑
         *  1. Controller：处理请求，接收和校验数据
         *  2. Service： 被Controller调用执行业务
         *  3. Controller： 接收Service处理完的数据并封装为页面需要的 Vo
         */
        List<BrandEntity> brandEntities = categoryBrandRelationService.getBrandsByCatId(catId);

        List<BrandVo> brandVoList = brandEntities.stream().map((brandEntity) -> {
            // 因为属性名不同无法直接属性对拷
            BrandVo brandVo = new BrandVo();
            brandVo.setBrandId(brandEntity.getBrandId());
            brandVo.setBrandName(brandEntity.getName());
            return brandVo;
        }).collect(Collectors.toList());

        return R.ok().put("data", brandVoList);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = categoryBrandRelationService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("product:categorybrandrelation:info")
    public R info(@PathVariable("id") Long id){
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok().put("categoryBrandRelation", categoryBrandRelation);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:categorybrandrelation:save")
    public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){

		categoryBrandRelationService.saveDetail(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:categorybrandrelation:update")
    public R update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:categorybrandrelation:delete")
    public R delete(@RequestBody Long[] ids){
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
