package com.aeterna.friendmall.seckill.interceptor;

import com.aeterna.common.constant.AuthServerConstant;
import com.aeterna.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.interceptor
 * @ClassName : .java
 * @createTime : 2024/8/19 14:38
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

/**
 * 整个订单都要求在登录环境下进行，所以要一个拦截器拦截未登录用户，拦截器都要实现 HandlerInterceptor
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //  远程调用这个路径/order/order/status/{orderSn}的服务就不拦截了
        String uri = request.getRequestURI();  // url还会加上整个服务器的地址名，URI就是请求后面的那串路径这里就是/order/order/status/{orderSn}
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/kill", uri);  // 只对kill这种路径进行登录拦截，其他放行，因为有的会远程调用秒杀服务查东西

        if (match) {
            MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
            if (attribute != null) {
                loginUser.set(attribute);
                return true;
            }else {
                // 没登录，重定向到登录页面去登录
                // 在 session 里面放一个消息提示一下
                request.getSession().setAttribute("msg","请先进行登录");
                response.sendRedirect("http://auth.friendmall.com/login.html");
                return false;
            }
        }
        return  true;
    }
}
