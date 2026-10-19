package com.groupbuy.module.goods.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.groupbuy.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 店铺表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop")
public class Shop extends BaseEntity {

    private String name;
    private String logo;
    private String address;
    private String phone;
    private String description;
    /** 1 营业中 0 休息 */
    private Integer status;

    /** 商品数量，非数据库字段 */
    @TableField(exist = false)
    private Integer goodsCount;
}
