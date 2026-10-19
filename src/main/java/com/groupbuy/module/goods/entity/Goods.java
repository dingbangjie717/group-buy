package com.groupbuy.module.goods.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.groupbuy.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品（菜品）表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods")
public class Goods extends BaseEntity {

    private Long shopId;
    private Long categoryId;
    private String name;
    private String image;
    private String description;
    /** 单价 */
    private BigDecimal price;
    /** 库存 */
    private Integer stock;
    /** 销量 */
    private Integer sales;
    /** 1 上架 0 下架 */
    private Integer status;
    /** 乐观锁版本号，配合 UPDATE ... WHERE version = ? 防止并发超卖 */
    @Version
    private Integer version;

    @TableField(exist = false)
    private String shopName;

    @TableField(exist = false)
    private String categoryName;
}
