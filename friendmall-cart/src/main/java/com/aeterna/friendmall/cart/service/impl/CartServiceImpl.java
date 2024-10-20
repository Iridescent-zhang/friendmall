package com.aeterna.friendmall.cart.service.impl;

import com.aeterna.common.utils.R;
import com.aeterna.friendmall.cart.feign.ProductFeignService;
import com.aeterna.friendmall.cart.interceptor.CartInterceptor;
import com.aeterna.friendmall.cart.service.CartService;
import com.aeterna.friendmall.cart.vo.Cart;
import com.aeterna.friendmall.cart.vo.CartItem;
import com.aeterna.friendmall.cart.vo.SkuInfoVo;
import com.aeterna.friendmall.cart.vo.UserInfoTo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.cart.service.impl
 * @ClassName : .java
 * @createTime : 2024/8/16 15:27
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "friendmall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)){
            CartItem cartItem = new CartItem();
            // 将新商品添加到购物车
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                // 远程查询商品信息
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setSkuId(skuId);
                cartItem.setPrice(data.getPrice());
            }, executor);

            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                // 查sku属性组合
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(values);
            }, executor);

            // 要等前两个进程就完成才能往redis里放数据
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValues).get();

            String jsonString = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), jsonString);
            return cartItem;
        }else {
            // 购物车里已经有这个商品了，需要修改数量
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount()+num);

            // 更新redis购物车中商品的数量
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
    }

    // 查购物车
    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
    }

    // 获取购物车数据
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();

        // 快速获得用户信息，UserInfoTo 判断是否登录进而决定操作哪个购物车
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null) {
            // 登录的话要先看临时购物车中是否有数据，有的话要合并到登录购物车
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems != null) {
                for (CartItem tempCartItem : tempCartItems) {
                    addToCart(tempCartItem.getSkuId(), tempCartItem.getCount());
                }
                // 合并之后要清除临时购物车
                clearCart(tempCartKey);
            }

            // friendmall:cart:userId 登录的购物车，此时已经合并临时购物车的数据
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> loginCartItems = getCartItems(cartKey);
            cart.setItems(loginCartItems);
        }else {
            // friendmall:cart:user-key 只拿临时购物车的数据
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    // 根据购物车名获取购物车中所有数据
    private List<CartItem> getCartItems(String cartKey) {
        // 这是和redis的一个绑定操作，即之后都是对reids的这个项对应的值进行操作
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values!=null && values.size()>0) {
            List<CartItem> collect = values.stream().map((obj) -> {
                CartItem cartItem = JSON.parseObject((String) obj, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    // 获取到我们要操作的redis中的购物车
    private BoundHashOperations<String, Object, Object> getCartOps() {
        // 快速获得用户信息，UserInfoTo 判断是否登录进而决定操作哪个购物车
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            // friendmall:cart:userId
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        }else {
            // friendmall:cart:user-key
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);  // 这是和redis的一个绑定操作，即之后都是对reids的这个项对应的值进行操作
        return operations;
    }

    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    // 勾选购物项时的操作
    @Override
    public void checkItem(Long skuId, Integer check) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), s);
    }

    // 修改购物项数量
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        String s = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), s);
    }

    // 删除购物项
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getUserCartItems() {
        // 这里是远程调用了购物车的服务，所以也会被购物车的拦截器拦截
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() == null) {
            // 由于有临时购物车的存在，所以可能是没登录的状态
            return null;
        }else {
            // 登录了
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            // 获取所有被选中的购物项
            List<CartItem> collect = cartItems.stream().filter(items -> items.getCheck()).map(item->{
                // 更新价格，加入购物车可能是一年前加的了【当然购物车最好是每次打开都重新查询价格】
                R price = productFeignService.getPrice(item.getSkuId());
                String data = (String) price.get("data");
                item.setPrice(new BigDecimal(data));
                return item;
            }) .collect(Collectors.toList());
            return collect;
        }
    }
}
