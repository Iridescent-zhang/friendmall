package com.aeterna.friendmall.product.service.impl;

import com.aeterna.common.constant.ProductConstant;
import com.aeterna.friendmall.product.dao.AttrAttrgroupRelationDao;
import com.aeterna.friendmall.product.dao.AttrGroupDao;
import com.aeterna.friendmall.product.dao.CategoryDao;
import com.aeterna.friendmall.product.entity.AttrAttrgroupRelationEntity;
import com.aeterna.friendmall.product.entity.AttrGroupEntity;
import com.aeterna.friendmall.product.entity.CategoryEntity;
import com.aeterna.friendmall.product.service.CategoryService;
import com.aeterna.friendmall.product.vo.AttrGroupRelationVo;
import com.aeterna.friendmall.product.vo.AttrRespVo;
import com.aeterna.friendmall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.product.dao.AttrDao;
import com.aeterna.friendmall.product.entity.AttrEntity;
import com.aeterna.friendmall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        // 批量拷贝属性，要求属性名相同
        BeanUtils.copyProperties(attr, attrEntity);
        // 1. 保存基本数据
        this.save(attrEntity);

        // 只有基本属性需要保存关联关系
        // 并且需要这个属性有指定对应的分组，这样才能保存这个关联关系（新建属性时可能就没有设置分组） attr.getAttrGroupId()!=null
        // 意味着，在关联关系这张表里，不会有记录的attr_group_id为null
        if(attr.getAttrType()==ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId()!=null){
            // 2. 保存关联关系
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(relationEntity);
        }
    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>();

        // 根据属性的类型去查，是规格参数或者销售属性
        queryWrapper.eq("attr_type", "base".equalsIgnoreCase(attrType) ? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

        if(catelogId!=0){
            queryWrapper.eq("catelog_id", catelogId);
        }

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)) {
            // attr_id attr_name
            queryWrapper.and((wrapper)->{
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        List<AttrEntity> records = page.getRecords();

        /**
         * 基于业务前端要求，我们查询数据库查到AttrEntity并包装成AttrRespVo返回，但我们可以发现
         *      数据库中数据只有catelog_id，而没有attr_group_id，想要获得获取就要去 属性属性组关联表 里面查询
         *                   catelogName  和   groupName 就比较麻烦，看操作
         */
        List<AttrRespVo> respVoList = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            // 设置分组名字  销售属性不分组
            if("base".equalsIgnoreCase(attrType)) {
                AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>()
                        .eq("attr_id", attrEntity.getAttrId()));
                // 这里因为可能我们新建属性的时候就没有设置属性属于的分组，那显示的时候自然就不会显示分组名字，那我们也不应该去查分组
                if (relationEntity != null && relationEntity.getAttrGroupId()!=null) {
                    String attrGroupName = attrGroupDao.selectById(relationEntity.getAttrGroupId()).getAttrGroupName();
                    attrRespVo.setGroupName(attrGroupName);
                }
            }
            // 设置分类名字
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(respVoList);  // 使用最新的结果集

        return pageUtils;
    }

    /**
     * 修改时回显数据
     */
    @Cacheable(value = "attr", key = "'attrInfo'+#root.args[0]")
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo attrRespVo = new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, attrRespVo);

        /**
         * 只有基本属性才需要回显属性分组
         */
        if(attrEntity.getAttrType()==ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 设置属性分组id和名
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>()
                    .eq("attr_id", attrId));
            if(relationEntity!=null) {
                attrRespVo.setAttrGroupId(relationEntity.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                if(attrGroupEntity!=null) {
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        // 设置所属分类的Path
        Long catelogId = attrEntity.getCatelogId();
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        attrRespVo.setCatelogPath(catelogPath);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if(categoryEntity!=null) {
            attrRespVo.setCatelogName(categoryEntity.getName());

        }
        return attrRespVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);

        /**
         * 只有基本属性才需要修改分组关联
         *
         * 修改分组关联 比如我们在前端修改了这个属性对应的属性分组
         *
         * 由于更新操作是更新已有数据，如果某个属性没有对应的属性组，在关联关系表里面肯定没有这项，那就没法更新，此时改为新增
         */
        if(attrEntity.getAttrType()==ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            Integer count = attrAttrgroupRelationDao.selectCount(new UpdateWrapper<AttrAttrgroupRelationEntity>()
                    .eq("attr_id", attr.getAttrId()));

            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());  // 这步好像没有必要？

            if (count==0) {
                attrAttrgroupRelationDao.insert(relationEntity);
            }
            else {
                attrAttrgroupRelationDao.update(relationEntity, new QueryWrapper<AttrAttrgroupRelationEntity>()
                        .eq("attr_id", attr.getAttrId()));
            }
        }
    }

    /**
     * 根据属性分组的id查找出该组的所有基本属性
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> relationEntityList = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .eq("attr_group_id", attrgroupId));

        List<Long> attrIds = relationEntityList.stream().map((relationEntity) -> {
            return relationEntity.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds == null || attrIds.size() == 0) {
            return null;
        }
        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);
        return (List<AttrEntity>) attrEntities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] attrGroupRelationVos) {
//        attrAttrgroupRelationDao.delete(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", xx).eq("attr_id", xx));
        // 实现批量删除
        List<AttrAttrgroupRelationEntity> relationEntityList = Arrays.asList(attrGroupRelationVos).stream().map((attrGroupRelationVo) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(attrGroupRelationVo, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        attrAttrgroupRelationDao.deleteBatchRelation(relationEntityList);
    }

    /**
     * 获取当前分组没有关联的所有属性
     *
     * 1. 当前分组只能关联自己所属的三级分类下的所有属性
     * 2. 当前分组只能关联别的分组没有引用的属性
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        // 1. 当前分组只能关联自己所属的三级分类下的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        // 2. 当前分组只能关联别的分组没有引用的属性，也不能关联自己已经关联的属性。意思就是要去排除所有已经关联了分组的属性
        // 2.1) 先获取当前分类的其他分组
        List<AttrGroupEntity> groupElse = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        //      获取这些组的组id
        List<Long> groupIds = groupElse.stream().map((group) -> {
            return group.getAttrGroupId();
        }).collect(Collectors.toList());

        // 2.2) 获取这些其他分组已经关联的属性
        List<Long> relationAttrIds = new ArrayList<>();
        if(groupIds!=null && groupIds.size()>0) {
            List<AttrAttrgroupRelationEntity> relationEntityList = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                    .in("attr_group_id", groupIds));
            relationAttrIds = relationEntityList.stream().map((relationEntity) -> {
                return relationEntity.getAttrId();
            }).collect(Collectors.toList());
        }

        // 2.3) 从当前分类的所有属性中排除掉这些属性
        //      baseMapper就相当于Dao，可以直接用
        //      并且只查基本属性不查销售属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId).eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if(relationAttrIds != null && relationAttrIds.size()>0){
            wrapper.notIn("attr_id", relationAttrIds);
        }

        // 如果有key，模糊查询
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w)->{
                w.eq("attr_id", key).or().like("attr_name", key);
            });
            //为什么不能直接用这种写法呢，还要在and里面写consumer
//            wrapper.eq("attr_id", key).or().like("attr_name", key);
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        return new PageUtils(page);
    }

    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {
        // SELECT attr_id FROM `pms_attr` WHERE attr_id IN(?) AND search_type = 1
        return baseMapper.selectSearchAttrIds(attrIds);
    }
}