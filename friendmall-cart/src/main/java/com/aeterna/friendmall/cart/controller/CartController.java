package com.aeterna.friendmall.cart.controller;

import com.aeterna.common.constant.AuthServerConstant;
import com.aeterna.friendmall.cart.interceptor.CartInterceptor;
import com.aeterna.friendmall.cart.service.CartService;
import com.aeterna.friendmall.cart.vo.Cart;
import com.aeterna.friendmall.cart.vo.CartItem;
import com.aeterna.friendmall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.cart.controller
 * @ClassName : .java
 * @createTime : 2024/8/16 15:40
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Controller
public class CartController {

    @Autowired
    CartService cartService;

    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems(){
        return cartService.getUserCartItems();
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check){
        cartService.checkItem(skuId, check);

        return "redirect:http://cart.friendmall.com/cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num){
        cartService.changeItemCount(skuId, num);
        return "redirect:http://cart.friendmall.com/cart.html";
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        return "redirect:http://cart.friendmall.com/cart.html";
    }

    /**
     * 浏览器cookie里会放一个：user-key，标识用户身份，一个月后过期
     * 第一次使用京东的购物车功能都会给一个临时购物车身份
     * 浏览器以后保存，每次访问都会带上这个cookie
     *
     * 登录：session里有用户
     * 没登录：按照cookie里面带的user-key来做
     * 第一次进来：没有临时用户要创建一个临时用户
     * 【上述用拦截器实现】
     *
     */
    // 跳转购物车页面
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {

        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "cartList";
    }

    /**
     * 添加商品到购物车
     * redirectAttributes.addFlashAttribute() 模拟session，将数据放在session中，但是只能取一次
     * redirectAttributes.addAttribute() 重定向时将数据拼在url后面
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {

        cartService.addToCart(skuId, num);
        // 重定向redirect的时候携带上skuId，并自动放到url后边
        redirectAttributes.addAttribute("skuId", skuId);

        return "redirect:http://cart.friendmall.com/addToCartSuccess.html";
    }

    // @GetMapping("/addToCart")重定向到成功页面addToCartSuccess.html，再次查询购物车数据。
    // 这样的目的是防止加入购物车后停留在/addToCart页面，这样刷新页面会重复往购物车放数据
    // 而在这个页面刷新只会反复查询购物车数据
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,
                                       Model model){
        CartItem item = cartService.getCartItem(skuId);
        model.addAttribute("item", item);
        return "success";
    }
}
