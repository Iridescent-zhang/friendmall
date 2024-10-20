package com.aeterna.friendmall.product.exception;

import com.aeterna.common.exception.BizCodeEnum;
import com.aeterna.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.product.exception
 * @ClassName : .java
 * @createTime : 2024/6/1 23:50
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

/**
 * 集中处理所有异常
 */
@Slf4j
//@ResponseBody
//@ControllerAdvice(basePackages = "com.aeterna.friendmall.product.controller")  // 监听这里面出现的所有异常
@RestControllerAdvice(basePackages = "com.aeterna.friendmall.product.controller")  // 监听这里面出现的所有异常
public class FriendmallExceptionControllerAdvice {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)  // 指定这个方法能处理哪些异常，这里精确匹配到了
    public R handleValidException(MethodArgumentNotValidException e){
        log.error("数据校验出现问题:{}，异常类型:{}", e.getMessage(), e.getClass());
        BindingResult bindingResult = e.getBindingResult();

        Map<String, String> errorMap = new HashMap<>();
        /**
         * 获取校验结果并封装到R里面
         */
        bindingResult.getFieldErrors().forEach( (fieldError)->{
            // fieldError 即 FieldError
            // 获取字段错误提示
            String message = fieldError.getDefaultMessage();
            // 获取错误的属性名（或者说获取错误的字段名）
            String field = fieldError.getField();
            errorMap.put(field, message);
        } );
//        return R.error(400,"数据校验出现问题").put("data",errorMap);
        /**
         * 使用 错误码枚举类 的写法
         */
        return R.error(BizCodeEnum.VALID_EXCEPTION.getCode(),BizCodeEnum.VALID_EXCEPTION.getMsg()).put("data",errorMap);
    }

    @ExceptionHandler(value = Throwable.class)  // 处理最大的异常，因为有的异常无法精确匹配，那么就来到这里
    public R handleException(Throwable throwable){

        log.error("错误：{}", throwable);
        return R.error(BizCodeEnum.UNKNOW_EXCEPTION.getCode(), BizCodeEnum.VALID_EXCEPTION.getMsg());
    }

}
