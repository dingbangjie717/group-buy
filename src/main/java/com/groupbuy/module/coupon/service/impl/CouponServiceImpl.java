package com.groupbuy.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.groupbuy.common.constant.Constants;
import com.groupbuy.common.exception.BizException;
import com.groupbuy.module.coupon.entity.Coupon;
import com.groupbuy.module.coupon.entity.UserCoupon;
import com.groupbuy.module.coupon.mapper.CouponMapper;
import com.groupbuy.module.coupon.mapper.UserCouponMapper;
import com.groupbuy.module.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券服务实现
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public List<Coupon> listAvailable() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> all = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .gt(Coupon::getRemain, 0)
                .orderByDesc(Coupon::getCreateTime));
        // 过期判断放在 Java 里做，避免空 endTime 拼进 SQL 造成漏数据
        return all.stream()
                .filter(c -> c.getEndTime() == null || !c.getEndTime().isBefore(now))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long receive(Long couponId, Long userId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new BizException("优惠券不存在或已下架");
        }
        if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BizException("优惠券已过期");
        }
        // 用条件更新保证并发下不会超发
        int rows = couponMapper.decreaseRemain(couponId);
        if (rows == 0) {
            throw new BizException("优惠券已被抢完");
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(Constants.USER_COUPON_UNUSED);
        userCoupon.setObtainTime(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);
        return userCoupon.getId();
    }

    @Override
    public List<UserCoupon> myCoupons(Long userId) {
        return userCouponMapper.selectAvailable(userId);
    }

    @Override
    public BigDecimal calcDiscount(Long userCouponId, BigDecimal goodsAmount) {
        if (userCouponId == null) {
            return BigDecimal.ZERO;
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || userCoupon.getStatus() != Constants.USER_COUPON_UNUSED) {
            throw new BizException("优惠券不可用");
        }
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            return BigDecimal.ZERO;
        }
        if (coupon.getType() == Constants.COUPON_FULL_REDUCE) {
            // 满减：订单金额达到门槛才可用
            if (goodsAmount.compareTo(coupon.getThreshold() == null ? BigDecimal.ZERO : coupon.getThreshold()) < 0) {
                throw new BizException("未达到优惠券使用门槛");
            }
            return coupon.getDiscount();
        }
        // 折扣券：discount 为折扣率，如 0.85 表示 85 折
        BigDecimal rate = coupon.getDiscount() == null ? BigDecimal.ONE : coupon.getDiscount();
        return goodsAmount.multiply(BigDecimal.ONE.subtract(rate))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void consume(Long userCouponId, Long orderId) {
        if (userCouponId == null) {
            return;
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            return;
        }
        userCoupon.setStatus(Constants.USER_COUPON_USED);
        userCoupon.setUseTime(LocalDateTime.now());
        userCoupon.setOrderId(orderId);
        userCouponMapper.updateById(userCoupon);
    }
}
