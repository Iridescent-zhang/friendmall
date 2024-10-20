package com.aeterna.friendmall.order.vo;

import com.aeterna.friendmall.order.entity.OrderEntity;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.vo
 * @ClassName : .java
 * @createTime : 2024/8/21 12:42
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;
    private Integer code;  // 错误状态码 0：成功，其他失败

}
