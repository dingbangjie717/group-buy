package com.groupbuy.module.cart.service;

import com.groupbuy.module.cart.vo.CartItemVO;

import java.util.List;

/**
 * 购物车服务（基于 Redis Hash：cart:user:{userId}，field = 商品ID）
 */
public interface CartService {

    /**
     * 加入购物车
     */
    void add(Long userId, Long goodsId, Integer quantity);

    /**
     * 修改数量（<=0 视为移除）
     */
    void updateQuantity(Long userId, Long goodsId, Integer quantity);

    /**
     * 勾选 / 取消勾选
     */
    void check(Long userId, Long goodsId, Boolean checked);

    /**
     * 移除某商品
     */
    void remove(Long userId, Long goodsId);

    /**
     * 购物车列表
     */
    List<CartItemVO> list(Long userId);

    /**
     * 清空购物车
     */
    void clear(Long userId);

    /**
     * 取指定店铺中已勾选的条目（结算用）
     */
    List<CartItemVO> listCheckedByShop(Long userId, Long shopId);

    /**
     * 下单成功后移除已结算的商品
     */
    void removeBatch(Long userId, List<Long> goodsIds);
}
