package com.groupbuy.module.coupon.controller;

import com.groupbuy.common.context.UserContext;
import com.groupbuy.common.result.R;
import com.groupbuy.module.coupon.entity.Coupon;
import com.groupbuy.module.coupon.entity.UserCoupon;
import com.groupbuy.module.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券接口
 */
@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/list")
    public R<List<Coupon>> list() {
        return R.ok(couponService.listAvailable());
    }

    @PostMapping("/receive/{couponId}")
    public R<Long> receive(@PathVariable Long couponId) {
        return R.ok("领取成功", couponService.receive(couponId, UserContext.requireUserId()));
    }

    @GetMapping("/my")
    public R<List<UserCoupon>> my() {
        return R.ok(couponService.myCoupons(UserContext.requireUserId()));
    }
}
