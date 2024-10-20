package com.aeterna.friendmall.ssoserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.ssoserver.controller
 * @ClassName : .java
 * @createTime : 2024/8/10 9:56
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Controller
public class LoginController {

    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * client1在登录时给ssoserver.com域名下保存了一个cookie，浏览器在访问ssoserver.com时会带上这个域名下的所有cookie。
     * 所有的子系统要登录时都会来到这个界面，我们需要根据是否有关键cookie-》sso_token来判断是否登录过，进而决定要不要展示登录页
     * 没登过就展示登录页给他登录，否则直接返回之前页面
     */
    @GetMapping("/login.html")
    public String loginPage(@RequestParam("redirect_url") String url, Model model,
                            @CookieValue(value = "sso_token", required = false) String sso_token){
        /**
         * 如果之前有系统登陆过了，会在登录服务器留下痕迹(名为sso_token的cookie，这个业务是我们写的)
         * 所以当另外的系统又要登录时，会来到登陆服务器此时一看有这个cookie，说明其他系统登过了，所以自己不用再登了
         */
        if (!StringUtils.isEmpty(sso_token)){
            // 说明之前有人登录过，浏览器留下了痕迹
            return "redirect:"+url+"?token="+sso_token;
        }
        model.addAttribute("url", url);
        return "login";
    }

    @PostMapping("/doLogin")
    public String doLogin(String username, String password, String url,
                          HttpServletResponse response){
        // 登录成功跳转回之前系统的页面，从哪来回哪去
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            // 登录成功
            // 把登陆成功的用户的session存起来
            String uuid = UUID.randomUUID().toString().replace("-", "");
            redisTemplate.opsForValue().set(uuid, username);

            //我们要在response里面放一个cookie让浏览器保存
            Cookie ssoToken = new Cookie("sso_token", uuid);
            response.addCookie(ssoToken);
            return "redirect:"+url+"?token="+uuid;
        }else {
            return "login";
        }
    }

    // 去获取当前token真正对应的用户信息
    @ResponseBody
    @GetMapping("/userInfo")
    public String userInfo(@RequestParam("token") String token){
        String userInfo = redisTemplate.opsForValue().get(token);
        return userInfo;
    }
}
