package com.groupbuy.common.constant;

/**
 * 系统常量
 */
public interface Constants {

    String TOKEN_PREFIX = "Bearer ";
    String ROLE_PREFIX = "ROLE_";

    /** 角色编码 */
    String ROLE_USER = "USER";
    String ROLE_MERCHANT = "MERCHANT";
    String ROLE_ADMIN = "ADMIN";

    /** 订单状态：0待支付 1已支付 2商家已接单 3配送中 4已完成 5已取消 6已退款 */
    int ORDER_WAIT_PAY = 0;
    int ORDER_PAID = 1;
    int ORDER_ACCEPTED = 2;
    int ORDER_DELIVERING = 3;
    int ORDER_FINISHED = 4;
    int ORDER_CANCELED = 5;
    int ORDER_REFUNDED = 6;

    /** 优惠券：1满减 2折扣 */
    int COUPON_FULL_REDUCE = 1;
    int COUPON_DISCOUNT = 2;

    /** 优惠券状态：0未使用 1已使用 2已过期 */
    int USER_COUPON_UNUSED = 0;
    int USER_COUPON_USED = 1;

    /** Redis Key 前缀 */
    String KEY_CART = "cart:user:";
    String KEY_STOCK = "stock:goods:";
    String KEY_ORDER_LOCK = "lock:order:create:";
    String KEY_GOODS_CACHE = "cache:goods:";
}
