package com.aeterna.friendmall.member;

//import org.junit.jupiter.api.Test;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;

@SpringBootTest
public class FriendmallMemberApplicationTests {

    @Test
    public void contextLoads() {
        // e10adc3949ba59abbe56e057f20f883e
        // 抗修改性(只要原文改了一点就会有很大变化) 强碰撞性(两个不同的原文，对应的MD5肯定不同)
        // 利用这两点 可以使用彩虹表暴力破解MD5，所以不能简单使用MD5
//        String s = DigestUtils.md5Hex("123456 ");
//        System.out.println("s = " + s);

        // 盐值加密：用一个随机数去调制MD5加密的程度(可以这么理解) 也就是说利用原文和随机数进行加密后得到MD5码
        // 加的盐是："$1$"+8位字符
        // $1$qqqqqqqq$AZofg3QwurbxV3KEOzwuI1
        // 验证的时候：由于密文不可逆(MD5是消息摘要有损失信息，不能得到原本的完整信息的)，所以我们对原文再加密一次(这就要求我们保存当时的盐值)，然后对比MD5码
//        String md5Crypt = Md5Crypt.md5Crypt("123456".getBytes(StandardCharsets.UTF_8), "$1$qqqqqqqq");
//        System.out.println("md5Crypt = " + md5Crypt);

        // 保存盐值太麻烦，spring的BCryptPasswordEncoder可以理解为能在MD5码中存储对应的盐值，并且即使很多密码都是123465，因为盐值不同，MD5也不同，并且都能匹配上
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        //$2a$10$5fcyZ66GhyhKf0MNsYyBOOcPAQlccQ3V3jN1t45NEg89tMCMke3CS
        //$2a$10$65nyI1QHV1E7SLc0QoB4P.Un3wAxf86yAJPQzWKmJJ9BfVxKIEgKa
        String encode = passwordEncoder.encode("123456");
        boolean matches = passwordEncoder.matches("123456", "$2a$10$65nyI1QHV1E7SLc0QoB4P.Un3wAxf86yAJPQzWKmJJ9BfVxKIEgKa");
        System.out.println(encode + "=>" + matches);
    }

}
