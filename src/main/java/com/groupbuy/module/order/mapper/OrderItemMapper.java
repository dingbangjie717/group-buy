package com.groupbuy.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.groupbuy.module.order.entity.OrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细 Mapper
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
