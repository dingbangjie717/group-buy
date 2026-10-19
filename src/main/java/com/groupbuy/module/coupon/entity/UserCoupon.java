package com.groupbuy.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.groupbuy.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户优惠券表
 * status：0 未使用 1 已使用 2 已过期
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_coupon")
public class UserCoupon extends BaseEntity {

    private Long userId;
    private Long couponId;
    private Integer status;
    private LocalDateTime obtainTime;
    private LocalDateTime useTime;
    private Long orderId;

    @TableField(exist = false)
    private String couponName;

    @TableField(exist = false)
    private Integer type;

    @TableField(exist = false)
    private java.math.BigDecimal threshold;

    @TableField(exist = false)
    private java.math.BigDecimal discount;
}
