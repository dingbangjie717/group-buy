package com.groupbuy.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.groupbuy.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单表
 * status：0待支付 1已支付 2商家已接单 3配送中 4已完成 5已取消 6已退款
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {

    private String orderNo;
    private Long userId;
    private Long shopId;

    /** 商品总额 */
    private BigDecimal goodsAmount;
    /** 优惠金额 */
    private BigDecimal discountAmount;
    /** 实付金额 */
    private BigDecimal payAmount;
    /** 使用的优惠券记录 ID */
    private Long userCouponId;

    private Integer status;
    private Integer payType;
    private LocalDateTime payTime;

    private String receiver;
    private String phone;
    private String address;
    private String remark;

    @TableField(exist = false)
    private List<OrderItem> items;

    @TableField(exist = false)
    private String shopName;

    @TableField(exist = false)
    private String nickname;
}
