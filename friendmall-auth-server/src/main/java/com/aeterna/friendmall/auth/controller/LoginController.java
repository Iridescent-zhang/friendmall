package com.aeterna.friendmall.auth.controller;

import com.aeterna.common.constant.AuthServerConstant;
import com.aeterna.common.exception.BizCodeEnum;
import com.aeterna.common.utils.R;
import com.aeterna.common.vo.MemberRespVo;
import com.aeterna.friendmall.auth.feign.MemberFeignService;
import com.aeterna.friendmall.auth.feign.ThirdPartFeignService;
import com.aeterna.friendmall.auth.vo.UserLoginVo;
import com.aeterna.friendmall.auth.vo.UserRegistVo;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.auth.controller
 * @ClassName : .java
 * @createTime : 2024/8/5 18:40
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Controller
public class LoginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 后面希望发送一个请求就能直接跳转到另一个页面
     * 利用 SpringMVC ViewController 将请求和页面进行映射
     * 省的写一个个这样的空Controller
     * @GetMapping("/login.html")
     *     public String loginPage() {
     *         return "login";
     *     }
     */

    @ResponseBody
    @PostMapping("/sms/send")
    public R sendCode(@RequestParam("phone") String phone) {
        // TODO 1. 接口防刷
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis()-l < 60000) {
                // 60秒内不能再发
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        // 2. 验证码的再次校验 存到redis  key是手机号，值为验证码  格式为：sms:code:电话->验证码
//        String code = UUID.randomUUID().toString().substring(0, 4);
        String code = "6379";
        String redisCachedCode = code + "_"+System.currentTimeMillis();

        // redis 缓存验证码 防止刷新页面后同一个手机号在60s内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone, redisCachedCode, 10, TimeUnit.MINUTES);
        thirdPartFeignService.sendCode(phone, code);

        return R.ok();
    }

    /**
     * RedirectAttributes 模拟重定向时携带数据
     * 重定向相当于模拟 HttpSession session也是在同服务器内跨页面时用来共享数据的
     * TODO 重定向携带数据利用的是session原理，将数据放在session中，只要跳到下一个
     *          页面取出数据后session里面的数据就会删掉，所以只是一次性的，比如错误校验再刷新一次就没了
     * TODO 1、分布式下session的问题
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result,
                         RedirectAttributes redirectAttributes
                         ){
        if(result.hasErrors()){
            // BindingResult 是参数校验的结果
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField,
                    FieldError::getDefaultMessage));
//            model.addAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("errors", errors);
            // 重要出错 HttpRequestMethodNotSupportedException: Request method 'POST' not supported
            // 用户注册->/regist[post]->检验出错后转发forward:/reg.html（结果这种路径映射默认都是get方式访问）
            // 校验出错转发到注册页
//            return "forward:/reg.html";  所以不用这种方法
            return "redirect:http://auth.friendmall.com/reg.html";  // 重定向
        }
        // 检验验证码
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(s)){
            if (code.equals(s.split("_")[0])) {
                // 删除验证码 令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                // 验证码通过 真正注册 调用远程服务
                R registed = memberFeignService.regist(vo);
                if (registed.getCode() == 0) {
                    // 只要是0状态的，就是成功了
                    return "redirect:http://auth.friendmall.com/login.html";
                }else {
                    // 重定向方式放错误信息
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", registed.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.friendmall.com/reg.html";  // 重定向
                }
            }else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.friendmall.com/reg.html";  // 重定向
            }
        }else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.friendmall.com/reg.html";  // 重定向
        }
    }

    // 前端提交的是表单，这里接值是接key value数据，不是json，所以不用@RequestBody
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes,
                        HttpSession session){
        // 发给远程服务进行登录
        // 由于调用的服务有@RequestBody，所以传vo过去的时候springmvc帮我们转为json传过去
        R login = memberFeignService.login(vo);
        if (login.getCode()==0) {
            MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {
            });
            // 登录成功，数据放到session，重定向到商城首页
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            return "redirect:http://friendmall.com";
        }else {
            HashMap<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.friendmall.com/login.html";
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        // 去看session是否有数据来判断当前是否是登录状态
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute==null) {
            // 没登录就去登录
            return "login";
        }else {
            // 登录过了想进login.html是不允许的，回去首页
            return "redirect:http://friendmall.com";
        }
    }
}
