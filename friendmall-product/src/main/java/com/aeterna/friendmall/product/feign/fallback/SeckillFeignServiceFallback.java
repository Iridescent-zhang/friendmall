package com.aeterna.friendmall.product.feign.fallback;

import com.aeterna.common.exception.BizCodeEnum;
import com.aeterna.common.utils.R;
import com.aeterna.friendmall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.feign.fallback
 * @ClassName : .java
 * @createTime : 2024/8/29 21:28
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Slf4j
@Component
public class SeckillFeignServiceFallback implements SeckillFeignService {
    // 实现远程服务接口的方法，意思是当远程调用服务失败的时候调用本地方法，也就是我们现在实现的方法作为一个fallback【远程调用失败执行的方法】
    @Override
    public R getSkuSeckillInfo(Long skuId) {
        log.info("熔断方法调用...{}", "getSkuSeckillInfo");
        return R.error(BizCodeEnum.TOO_MANY_REQUEST.getCode(), BizCodeEnum.TOO_MANY_REQUEST.getMsg());
    }
}
