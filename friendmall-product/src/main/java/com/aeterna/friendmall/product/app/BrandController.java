package com.aeterna.friendmall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.aeterna.common.valid.AddGroup;
import com.aeterna.common.valid.UpdateGroup;
import com.aeterna.common.valid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.aeterna.friendmall.product.entity.BrandEntity;
import com.aeterna.friendmall.product.service.BrandService;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.R;


/**
 * 品牌
 *
 * @author lczhang
 * @email lczhang93@gmail.com
 * @date 2024-05-12 19:03:41
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:brand:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }

    @GetMapping("/infos")
    public R info(@RequestParam("brandIds") List<Long> brandIds) {
        List<BrandEntity> brandEntities = brandService.getBrandsByIds(brandIds);
        return R.ok().put("brand", brandEntities);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    //@RequiresPermissions("product:brand:info")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    /**
     * 使用统一异常处理的话这里只需要考虑正确的业务逻辑，出现异常抛出去即可
     *
     * 这里的情况是：如果不去获取BindingResult进行处理，真出现异常的话自己就会抛出去了，然后被统一异常处理类的方法捕获处理
     */
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand/*, BindingResult bindingResult*/){

//        if(bindingResult.hasErrors()){
//            Map<String, String> data = new HashMap<>();
//            /**
//             * 获取校验结果并封装到R里面
//             */
//            bindingResult.getFieldErrors().forEach( (item)->{
//                // item 即 FieldError
//                // 获取字段错误提示
//                String message = item.getDefaultMessage();
//                // 获取错误的属性名（或者说获取错误的字段名）
//                String field = item.getField();
//                data.put(field, message);
//            } );
//            return R.error(400,"提交的数据不合法").put("data",data);
//        }
//        else {
//            brandService.save(brand);
//        }

        /**
         * 只执行正确时的业务逻辑，不去获取BindingResult处理的话真出现异常自己就会抛出去
         */
        brandService.save(brand);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:brand:update")
    public R update(@Validated({UpdateGroup.class}) @RequestBody BrandEntity brand){
		brandService.updateDetail(brand);

        return R.ok();
    }

    /**
     * 为修改status专门写一个函数，因为单独修改status不会把品牌名带上，这样会校验失败，所以两种修改用不同函数
     */
    @RequestMapping("/update/status")
    //@RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated({UpdateStatusGroup.class}) @RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:brand:delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
