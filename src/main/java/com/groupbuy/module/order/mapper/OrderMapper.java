package com.groupbuy.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.groupbuy.module.order.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单 Mapper
 */
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} LIMIT 1")
    Order selectByOrderNo(@Param("orderNo") String orderNo);
}
