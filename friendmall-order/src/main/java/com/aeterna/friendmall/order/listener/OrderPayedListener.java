package com.aeterna.friendmall.order.listener;

import com.aeterna.friendmall.order.config.AlipayTemplate;
import com.aeterna.friendmall.order.service.OrderService;
import com.aeterna.friendmall.order.vo.PayAsyncVo;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.listener
 * @ClassName : .java
 * @createTime : 2024/8/25 23:06
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@RestController
public class OrderPayedListener {

    @Autowired
    OrderService orderService;

    @Autowired
    AlipayTemplate alipayTemplate;

    /**
     * 用来监听支付成功后支付宝发给我们的通知，要求我们：
     *      1. 收到通知后必须打印 success 字符串，否则支付宝会不断重发这个消息，所以用 @RestController
     *      2. 不能跳转页面
     *      3. 是post方式
     */
    @PostMapping("/payed/notify")
    public String handleAlipayed(PayAsyncVo vo, HttpServletRequest request) throws AlipayApiException, UnsupportedEncodingException {  // 这里不能写@RequestBody vo，不是每次接实体类都要写这个，应该是对方发且我们收json数据时才写这个
        // 验证数据签名【防止有人模仿支付宝发这个异步通知】
        Map<String,String> params = new HashMap<String,String>();
        Map<String,String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
//            valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.getAlipay_public_key(), alipayTemplate.getCharset(), alipayTemplate.getSign_type()); //调用SDK验证签名
        if (signVerified) {
            // 签名验证成功
            System.out.println("签名验证成功...");
            String result = orderService.handlePayResult(vo);
            return result;
        }else {
            // 签名验证失败
            System.out.println("签名验证失败...");
            return "error";  // 只要不是 success 就行
        }
    }
}
