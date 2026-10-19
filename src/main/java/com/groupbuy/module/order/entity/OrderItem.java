package com.groupbuy.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.groupbuy.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细表（下单时把商品名与单价做快照，后续改价不影响历史订单）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItem extends BaseEntity {

    private Long orderId;
    private Long goodsId;
    private String goodsName;
    private String goodsImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
}
