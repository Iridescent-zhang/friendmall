package com.aeterna.friendmall.cart.service;

import com.aeterna.friendmall.cart.vo.Cart;
import com.aeterna.friendmall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.cart.service
 * @ClassName : .java
 * @createTime : 2024/8/16 15:27
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
public interface CartService {
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;


    CartItem getCartItem(Long skuId);

    Cart getCart() throws ExecutionException, InterruptedException;

    // 清空购物车数据
    void clearCart(String cartKey);

    void checkItem(Long skuId, Integer check);

    void changeItemCount(Long skuId, Integer num);

    void deleteItem(Long skuId);

    List<CartItem> getUserCartItems();
}
