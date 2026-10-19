package com.groupbuy.module.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.groupbuy.module.goods.entity.Category;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分类 Mapper
 */
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("SELECT * FROM category WHERE shop_id = #{shopId} ORDER BY sort ASC")
    List<Category> selectByShopId(@Param("shopId") Long shopId);
}
