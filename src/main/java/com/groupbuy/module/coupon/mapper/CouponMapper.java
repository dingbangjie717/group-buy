package com.groupbuy.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.groupbuy.module.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券 Mapper
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 领取时原子扣减剩余数量，避免超发
     */
    @Update("UPDATE coupon SET remain = remain - 1 WHERE id = #{id} AND remain > 0")
    int decreaseRemain(@Param("id") Long id);
}
