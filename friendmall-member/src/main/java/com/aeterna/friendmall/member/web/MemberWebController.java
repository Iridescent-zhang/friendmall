package com.aeterna.friendmall.member.web;

import com.aeterna.common.utils.R;
import com.aeterna.friendmall.member.feign.OrderFeignService;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.member.web
 * @ClassName : .java
 * @createTime : 2024/8/25 18:28
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum, Model model, HttpServletRequest request){
        // 获取到支付宝给我们传来的所有数据从而修改订单页面的订单状态
//        方式一：request 验证签名：保证数据真实
//        方式二：用户支付完成后支付宝会根据API中客户传入的notify_url通过post请求的形式将支付结果作为参数通知到商户系统，支付宝会不断重发这个请求直到我们商户回复“success”。【这就像分布式事务里面的最大努力通知型方案，达到最终一致】

        HashMap<String, Object> page = new HashMap<>();
        page.put("page", pageNum.toString());
        R r = orderFeignService.listWithItem(page);
        System.out.println(JSON.toJSONString(r));
        model.addAttribute("orders", r);
        // 查出当前登录用户的所有订单列表数据
        return "orderList";
    }
}
