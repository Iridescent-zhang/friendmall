package com.aeterna.friendmall.ssoclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.ssoclient.controller
 * @ClassName : .java
 * @createTime : 2024/8/10 9:32
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Controller
public class HelloController {

    @Value("${sso.server.url}")
    String ssoServerUrl;

    // 无需登录即可访问
    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }

    /**
     * 需要能够判断这次是否是去ssoserver登录成功后跳转回来的，我们用token来描述
     * 如果是去ssoserver登录成功后跳转回来的就会带上这个标识@RequestParam(value = "token", required = false)
     */
    @GetMapping("/boss")
    public String employees(Model model, HttpSession session, @RequestParam(value = "token", required = false) String token) {

        if (!StringUtils.isEmpty(token)) {
            // 去ssoserver登录成功后跳转回来的
            // TODO：去session获取当前token真正对应的用户信息再放到session里的"loginUser"，这里先用"zhangsan"代替
            // 于是用Spring家的RestTemplate去发请求拿用户信息，不能用feign，因为服务端可能使用php写的
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.getForEntity("http://ssoserver.com:25000/userInfo?token=" + token, String.class);
            String body = responseEntity.getBody();
            session.setAttribute("loginUser", body);
        }
        // 查session中是否有用户"loginUser"，就是上面的TODO
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            // 没登录，跳转到登录服务器进行登录
            // 跳转过去前，使用url上的查询参数标识我们自己是哪个页面 ?redirect_url=http://clent1.com:25001/employees
            return "redirect:"+ssoServerUrl+"?redirect_url=http://client2.com:25002/boss";
        }else {
            // 登录了，模拟数据返回
            List<String> emps = new ArrayList<>();
            emps.add("张三");
            emps.add("李四");
            model.addAttribute("emps", emps);
            return "list";
        }
    }
}
