package com.aeterna.friendmall.order.web;

import com.aeterna.common.exception.NoStockException;
import com.aeterna.friendmall.order.service.OrderService;
import com.aeterna.friendmall.order.to.OrderCreateTo;
import com.aeterna.friendmall.order.vo.OrderConfirmVo;
import com.aeterna.friendmall.order.vo.OrderSubmitVo;
import com.aeterna.friendmall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.order.web
 * @ClassName : .java
 * @createTime : 2024/8/19 14:36
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    /**
     * 结算购物车已选中商品
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {

        OrderConfirmVo confirmVo = orderService.confirmOrder();

        model.addAttribute("orderConfirmData", confirmVo);

        // 展示订单确认的数据
        return "confirm";
    }

    /**
     * 下订单功能
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){

        try {
            // 创建订单、验令牌、验价格、锁库存
            SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
            if (responseVo.getCode() == 0) {
                //下单成功来到支付选择页
                model.addAttribute("submitOrderResp", responseVo);
                return "pay";
            } else {
                String msg = "下单失败；";
                switch (responseVo.getCode()) {
                    case 1:
                        msg += "订单信息过期，请刷新再次提交";
                        break;
                    case 2:
                        msg += "订单商品价格发生变化，请确认后再次提交";
                        break;
                    case 3:
                        msg += "库存锁定失败，商品库存不足";
                        break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                //下单失败回到订单确认页重新确认订单信息
                return "redirect:http://order.friendmall.com/toTrade";
            }
        }catch (Exception e){
            if (e instanceof NoStockException) {
                String message = ((NoStockException)e).getMessage();
                redirectAttributes.addFlashAttribute("msg",message);
            }
            return "redirect:http://order.friendmall.com/toTrade";
        }
    }

}
