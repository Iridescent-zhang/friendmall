package com.aeterna.friendmall.cart.vo;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.cart.vo
 * @ClassName : .java
 * @createTime : 2024/8/16 15:00
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 * 需要计算的属性需要重写get方法，这样能保证每次获取属性时都会重新计算
 */
public class Cart {

    private List<CartItem> items;

    private Integer countNum;  // 商品总数量

    private Integer countType;  // 商品类型数量

    private BigDecimal totalAmount;  // 商品总价

    private BigDecimal reduce = new BigDecimal("0.00");  // 减免价格

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int count = 0;
        if (items!=null && items.size()>0) {
            for (CartItem item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    public Integer getCountType() {
        int count = 0;
        if (items!=null && items.size()>0) {
            for (CartItem item : items) {
                count += 1;
            }
        }
        return countType;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        // 计算购物项总价
        if (items!=null && items.size()>0) {
            for (CartItem item : items) {
                if (item.getCheck() == true) {
                    amount = amount.add(item.getTotalPrice());
                }
            }
        }
        // 减去优惠
        BigDecimal totalAmount = amount.subtract(getReduce());
        return totalAmount;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
