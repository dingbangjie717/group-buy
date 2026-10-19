package com.groupbuy.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 下单参数
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "店铺不能为空")
    private Long shopId;

    /** 不传则默认结算该店铺下购物车中已勾选的商品 */
    private List<ItemDTO> items;

    /** 使用的用户优惠券 ID */
    private Long userCouponId;

    @NotBlank(message = "收货人不能为空")
    private String receiver;

    @NotBlank(message = "联系电话不能为空")
    private String phone;

    @NotBlank(message = "收货地址不能为空")
    private String address;

    private String remark;

    @Data
    public static class ItemDTO {
        @NotNull(message = "商品 ID 不能为空")
        private Long goodsId;
        @NotNull(message = "购买数量不能为空")
        private Integer quantity;
    }
}
