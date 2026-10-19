package com.groupbuy.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.groupbuy.common.context.UserContext;
import com.groupbuy.common.result.PageResult;
import com.groupbuy.common.result.R;
import com.groupbuy.module.order.dto.OrderCreateDTO;
import com.groupbuy.module.order.entity.Order;
import com.groupbuy.module.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 试算金额 */
    @PostMapping("/preview")
    public R<BigDecimal> preview(@RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.previewAmount(dto));
    }

    @PostMapping("/create")
    public R<String> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok("下单成功，请及时支付", orderService.create(dto));
    }

    @PostMapping("/pay/{orderNo}")
    public R<Void> pay(@PathVariable String orderNo) {
        orderService.pay(orderNo);
        return R.ok();
    }

    @PostMapping("/cancel/{orderId}")
    public R<Void> cancel(@PathVariable Long orderId,
                          @RequestParam(required = false, defaultValue = "用户主动取消") String reason) {
        orderService.cancel(orderId, reason);
        return R.ok();
    }

    @GetMapping("/{orderId}")
    public R<Order> detail(@PathVariable Long orderId) {
        return R.ok(orderService.detail(orderId));
    }

    @GetMapping("/my")
    public R<PageResult<Order>> myOrders(@RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") Integer pageNo,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<Order> page = orderService.pageMyOrders(status, pageNo, pageSize);
        return R.ok(PageResult.of(page));
    }

    // ---------- 商家端 ----------

    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @GetMapping("/merchant/list")
    public R<PageResult<Order>> shopOrders(@RequestParam(required = false) Long shopId,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Integer pageNo,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<Order> page = orderService.pageShopOrders(shopId, status, pageNo, pageSize);
        return R.ok(PageResult.of(page));
    }

    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @PostMapping("/merchant/{orderId}/accept")
    public R<Void> accept(@PathVariable Long orderId) {
        orderService.accept(orderId);
        return R.ok();
    }

    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @PostMapping("/merchant/{orderId}/deliver")
    public R<Void> deliver(@PathVariable Long orderId) {
        orderService.deliver(orderId);
        return R.ok();
    }

    @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
    @PostMapping("/merchant/{orderId}/finish")
    public R<Void> finish(@PathVariable Long orderId) {
        orderService.finish(orderId);
        return R.ok();
    }
}
