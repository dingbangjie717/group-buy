package com.groupbuy.module.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.groupbuy.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    private Long shopId;
    private String name;
    private Integer sort;
}
