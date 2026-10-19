package com.groupbuy.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.groupbuy.module.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户优惠券 Mapper
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 查询用户可用优惠券（联表带出券面信息）
     */
    @Select("SELECT uc.*, c.name AS coupon_name, c.type AS type, c.threshold AS threshold, c.discount AS discount " +
            "FROM user_coupon uc INNER JOIN coupon c ON c.id = uc.coupon_id " +
            "WHERE uc.user_id = #{userId} AND uc.status = 0 AND uc.deleted = 0 " +
            "AND (c.end_time IS NULL OR c.end_time >= NOW()) " +
            "ORDER BY uc.id DESC")
    List<UserCoupon> selectAvailable(@Param("userId") Long userId);
}
