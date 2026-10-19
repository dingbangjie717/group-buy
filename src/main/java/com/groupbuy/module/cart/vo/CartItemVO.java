package com.groupbuy.module.cart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车条目（存 Redis，不建表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemVO {

    private Long goodsId;
    private String goodsName;
    private String goodsImage;
    private BigDecimal price;
    private Integer quantity;
    /** 是否勾选结算 */
    private Boolean checked;
    /** 所属店铺，结算时按店铺拆单 */
    private Long shopId;
}
