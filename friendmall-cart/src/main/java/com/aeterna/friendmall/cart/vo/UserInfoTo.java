package com.aeterna.friendmall.cart.vo;

import lombok.Data;
import lombok.ToString;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.cart.vo
 * @ClassName : .java
 * @createTime : 2024/8/16 16:09
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@ToString
@Data
public class UserInfoTo {

    private Long userId;
    private String userKey;  // 一定会有这个，不管登录还是非登录状态

    private boolean tempUser = false;

}
