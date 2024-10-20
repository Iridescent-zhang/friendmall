package com.aeterna.friendmall.cart.interceptor;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.cart.interceptor
 * @ClassName : .java
 * @createTime : 2024/8/16 16:01
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

import com.aeterna.common.constant.AuthServerConstant;
import com.aeterna.common.constant.CartConstant;
import com.aeterna.common.vo.MemberRespVo;
import com.aeterna.friendmall.cart.vo.UserInfoTo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 在进入目标方法之前，判断用户的登录状态（共三种情况），并把这状态封装后传递给Controller对应的请求
 */
public class CartInterceptor implements HandlerInterceptor {

    /**
     * Tomcat对从拦截器->Controller->Service->Dao从始至终都是同一个线程，所以用 ThreadLocal 可以很方便的传递数据
     */
    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * preHandler：在执行目标方法之前拦截下并执行。
     * @param request current HTTP request： 由SpringSession装饰者模式包装过的，所以读取到的session都是从redis中读的
     * @param response current HTTP response
     * @param handler chosen handler to execute, for type and/or instance evaluation
     * @return true放行，false不放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();
        HttpSession session = request.getSession();
        MemberRespVo member = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (member != null) {
            // 登录了
            userInfoTo.setUserId(member.getId());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length>0) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if (name.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }
        }

        // 没有临时用户一定要分配一个临时用户（这个意思是说有没有user-key，登录的用户可能也没有user-key，同样也要分配一个临时用户(user-key)）
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }

        // 在回到 Controller 之前将数据放进去
        threadLocal.set(userInfoTo);

        return true;
    }

    /**
     * 业务执行之后让浏览器保存 user-key 这个 cookie
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

        UserInfoTo userInfoTo = threadLocal.get();
        // 如果没有临时用户user-key，就要分配个新的
        if (!userInfoTo.isTempUser()){
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("friendmall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }
}
