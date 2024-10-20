package com.aeterna.friendmall.order.web;

import com.aeterna.friendmall.order.config.AlipayTemplate;
import com.aeterna.friendmall.order.service.OrderService;
import com.aeterna.friendmall.order.vo.PayVo;
import com.alipay.api.AlipayApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.web
 * @ClassName : .java
 * @createTime : 2024/8/25 17:02
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Controller
public class PayWebController {

    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    /**
     * 1、将支付页交给浏览器展示
     * 2、支付成功后我们要跳到用户的订单列表页
     */
    @ResponseBody
    @GetMapping(value = "/payOrder", produces = "text/html")  // 明确地告诉浏览器我会产生一个html内容
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        // 返回我们需要的PayVo
        PayVo payVo = orderService.getOrderPay(orderSn);

        // 支付完后给我们返回的是一个页面，将此页面直接交给浏览器就行
        String pay = alipayTemplate.pay(payVo);
        System.out.println("pay = " + pay);
        return pay;
    }
}
