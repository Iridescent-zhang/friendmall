package com.aeterna.friendmall.member.exception;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.member.exception
 * @ClassName : .java
 * @createTime : 2024/8/6 21:01
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
public class UserNameExistException extends RuntimeException{

    public UserNameExistException() {
        super("用户名存在");
    }
}
