package com.aeterna.friendmall.auth.controller;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.auth.controller
 * @ClassName : .java
 * @createTime : 2024/8/9 1:47
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

import com.aeterna.common.constant.AuthServerConstant;
import com.aeterna.common.utils.HttpUtils;
import com.aeterna.common.utils.R;
import com.aeterna.friendmall.auth.feign.MemberFeignService;
import com.aeterna.common.vo.MemberRespVo;
import com.aeterna.friendmall.auth.vo.SocialUser;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    // code以请求参数的形式过来 http://friendmall.com/oauth2.0/gitee/success?code=...
    // 社交登录成功(社交账号密码输对了)后回调到这里
    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(@RequestParam("code") String code, HttpSession httpSession, HttpServletResponse httpServletResponse) throws Exception {
        // 1、根据code换取accessToken，这个code码会变的，用一次就会变，所以要接变化的值
        /**
         * 要发这么个请求
         * https://gitee.com/oauth/token?
         * grant_type=authorization_code
         * &code=705ae5acef8f6dca622ac3a0fea579e2c27173997884438efd4f8556bd3c11e1
         * &client_id=0c7b4697de75d983a873f9e495125e22e0942f09dd4ffa2c779d1574f0170ddf
         * &redirect_uri=http://friendmall.com/oauth2.0/gitee/success
         * &client_secret=55c7a771af4d421086f4f545e55b10d252fa7d2486549da0d78c3c6e57d439f6
         *
         * post请求没有查询参数querys，但是有请求体
         *      * @param host
         *      * @param path
         *      * @param method
         *      * @param headers
         *      * @param querys
         *      * @param bodys
         */
        Map<String, String> body = new HashMap<>();
        body.put("client_id","0c7b4697de75d983a873f9e495125e22e0942f09dd4ffa2c779d1574f0170ddf");
        body.put("client_secret","55c7a771af4d421086f4f545e55b10d252fa7d2486549da0d78c3c6e57d439f6");
        body.put("grant_type","authorization_code");
        body.put("redirect_uri","http://auth.friendmall.com/oauth2.0/gitee/success");  // 认证完成后的跳转链接
        body.put("code",code);
        HttpResponse response = HttpUtils.doPost("https://gitee.com", "/oauth/token", "post", new HashMap<>(), new HashMap<>(), body);

        if (response.getStatusLine().getStatusCode() == 200) {
            // 获取到access_token
            // 这个能将HttpEntity entity转换为json串
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            // 目前知道了当前是哪个社交用户
            // 1、如果用户是第一次进网站，则自动为这个社交账号注册一个member会员信息，以后这个社交账号和这个member绑定
            // gitee需要自己查一下UserId，这是用户的唯一标识，我们用这个id与member信息关联
            Map<String, String> query = new HashMap<>();
            query.put("access_token", socialUser.getAccess_token());
            // doGet 里面放查询参数：Access_token
            HttpResponse SocialInfo = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", new HashMap<>(), query);
            if (SocialInfo.getStatusLine().getStatusCode() == 200){
                String jsonInfo = EntityUtils.toString(SocialInfo.getEntity());
                JSONObject jsonObject = JSON.parseObject(jsonInfo);
                String UserId = jsonObject.getString("id");
                socialUser.setUid(UserId);
            }

            // 登录或注册这个社交用户
            R oauthLogin = memberFeignService.oauthLogin(socialUser);
            if (oauthLogin.getCode() == 0) {
                MemberRespVo data = oauthLogin.getData("data", new TypeReference<MemberRespVo>() {
                });
                System.out.println("登录成功：用户信息 = " + data);
                log.info("登录成功：用户信息={}", data.toString());
                /**
                 * 第一次使用session，命令浏览器保存带有JSESSIONID的cookie（相当于银行卡号，JSESSIONID就是用来找对应的session的）
                 * 以后浏览器访问哪个网站就会带上这个网站的cookie（cookie都是有作用的域名的：Domain）
                 * 要解决不同服务不同子域之间能共享session：friendmall.com、auth.friendmall.com、product.friendmall.com
                 * 所以发卡的时候，即便是子域系统发的卡，也要能让整个父域都使用。所以要指定域名为父域名
                 *
                 * cookie：每次访问网站时，浏览器在发请求时都会将可使用的cookie带上(就放在请求头里)，什么是可使用，就是要访问的这个网站的域名和这个cookie的作用域(Domain)是相同的
                 */
                httpSession.setAttribute(AuthServerConstant.LOGIN_USER, data);
                /**
                 * new Cookie("JSESSIONID", "dddd").setDomain();
                 * httpServletResponse.addCookie();
                 * cookie是在响应里面携带过去的，tomcat在第一次使用session时就创建了cookie，默认的作用域是当前域名
                 * TODO：1.默认发的令牌的session作用域是当前域，我们要改为父域
                 * TODO：2.使用JSON的序列化方式来序列化session的对象数据到redis中
                 */
                // SpringSession都整合了以上两个问题(不同服务共享session，同服务分布式下共享session(因为session就是内存，是存在特定机器上的，考虑到负载均衡...))

                // 2、登录成功即退回首页
                return "redirect:http://friendmall.com";
            }else {
                // 失败就重登
                return "redirect:http://auth.friendmall.com/login.html";
            }

        }else {
            return "redirect:http://auth.friendmall.com/login.html";
        }
    }
}
