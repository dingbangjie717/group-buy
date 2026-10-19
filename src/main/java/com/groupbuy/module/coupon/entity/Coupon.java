package com.groupbuy.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.groupbuy.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券表
 * type：1 满减 2 折扣
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon")
public class Coupon extends BaseEntity {

    private String name;
    private Integer type;
    /** 满减门槛 */
    private BigDecimal threshold;
    /** 满减金额 / 折扣率（0.85 表示 85 折） */
    private BigDecimal discount;
    private Integer total;
    private Integer remain;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 1 启用 0 停用 */
    private Integer status;
}
