package com.aeterna.friendmall.member.vo;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.auth.vo
 * @ClassName : .java
 * @createTime : 2024/8/9 12:15
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

import lombok.Data;

/**
 * Copyright 2024 bejson.com
 */
@Data
public class SocialUser {
    private String access_token;
    private String token_type;
    private long expires_in;
    private String refresh_token;
    private String uid;

    private String scope;
    private long created_at;
}
