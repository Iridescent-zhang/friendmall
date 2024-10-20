package com.aeterna.friendmall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.aeterna.friendmall.product.entity.AttrEntity;
import com.aeterna.friendmall.product.service.AttrAttrgroupRelationService;
import com.aeterna.friendmall.product.service.AttrService;
import com.aeterna.friendmall.product.service.CategoryService;
import com.aeterna.friendmall.product.vo.AttrGroupRelationVo;
import com.aeterna.friendmall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.aeterna.friendmall.product.entity.AttrGroupEntity;
import com.aeterna.friendmall.product.service.AttrGroupService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.R;



/**
 * 属性分组
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-12 19:03:41
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    AttrService attrService;

    @Autowired
    AttrAttrgroupRelationService relationService;

    /**
     * product/attrgroup/225/withattr
     * 发布商品时需要 获取这个商品对应的三级分类下所有的属性分组，并且带上所有的属性
     */
    @GetMapping("{catelogId}/withattr")
    public R getAttrGroupWithAttr(@PathVariable("catelogId") Long catelogId) {
        // 1. 查出当前分类的所有属性分组
        // 2. 查出每个属性分组的所有属性
        List<AttrGroupWithAttrsVo> vos = attrGroupService.getAttrGroupWithAttrsByCatelogId(catelogId);
        return R.ok().put("data", vos);
    }

    /**
     * attr/relation
     * 新建关联关系
     * 前端传来 AttrGroupRelationVo的数组，或者列表也一样，这次用链表
     * @param relationVos
     * @return
     */
    @PostMapping("attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> relationVos){

        relationService.saveBatch(relationVos);

        return R.ok();
    }

    // {attrgroupId}/attr/relation
    @GetMapping("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId){

        List<AttrEntity> attrEntityList = attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data", attrEntityList);
    }

    /**
     * {attrgroupId}/noattr/relation
     * 查询还没有关联分组的属性
     * @param params 页面传来的参数，是一个Map对象，携带分页参数和key(key可能为null)
     */
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R attrNoRelation(@PathVariable("attrgroupId") Long attrgroupId,
                            @RequestParam Map<String, Object> params){

        PageUtils page = attrService.getNoRelationAttr(params, attrgroupId);
        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list/{catelogId}")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params, @PathVariable("catelogId") Long catelogId){
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params, catelogId);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        Long catelogId = attrGroup.getCatelogId();

        Long[] catelogPath = categoryService.findCatelogPath(catelogId);

        attrGroup.setCatelogPath(catelogPath);

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    // /attr/relation/delete
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] attrGroupRelationVos){
        attrService.deleteRelation(attrGroupRelationVos);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
