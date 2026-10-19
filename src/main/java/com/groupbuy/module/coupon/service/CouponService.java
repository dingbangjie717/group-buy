package com.groupbuy.module.coupon.service;

import com.groupbuy.module.coupon.entity.Coupon;
import com.groupbuy.module.coupon.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务
 */
public interface CouponService {

    /**
     * 可领取的券列表
     */
    List<Coupon> listAvailable();

    /**
     * 领取优惠券（原子扣减剩余数量，防超发）
     */
    Long receive(Long couponId, Long userId);

    /**
     * 我的优惠券
     */
    List<UserCoupon> myCoupons(Long userId);

    /**
     * 计算优惠金额：1 满减 / 2 折扣
     */
    BigDecimal calcDiscount(Long userCouponId, BigDecimal goodsAmount);

    /**
     * 核销优惠券（支付成功后调用）
     */
    void consume(Long userCouponId, Long orderId);
}
