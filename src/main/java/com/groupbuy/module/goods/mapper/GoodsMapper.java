package com.groupbuy.module.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.groupbuy.module.goods.entity.Goods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 Mapper
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 数据库层防超卖：带库存条件的原子扣减，返回影响行数
     * 用「条件更新」而不是「先查后改」，避免并发下把 stock 改成负数
     */
    @Update("UPDATE goods SET stock = stock - #{num}, sales = sales + #{num} " +
            "WHERE id = #{id} AND stock >= #{num}")
    int deductStock(@Param("id") Long id, @Param("num") Integer num);

    /**
     * 回补库存（订单取消 / 退款）
     */
    @Update("UPDATE goods SET stock = stock + #{num} WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("num") Integer num);
}
