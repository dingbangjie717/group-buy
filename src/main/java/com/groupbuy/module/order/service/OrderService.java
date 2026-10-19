package com.groupbuy.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.groupbuy.module.order.dto.OrderCreateDTO;
import com.groupbuy.module.order.entity.Order;

import java.math.BigDecimal;

/**
 * 订单服务
 */
public interface OrderService {

    /**
     * 创建订单（防超卖 + 优惠券 + 延迟消息）
     *
     * @return 订单号
     */
    String create(OrderCreateDTO dto);

    /**
     * 模拟支付
     */
    void pay(String orderNo);

    /**
     * 取消订单（含回补库存）
     */
    void cancel(Long orderId, String reason);

    /**
     * 商家接单
     */
    void accept(Long orderId);

    /**
     * 开始配送
     */
    void deliver(Long orderId);

    /**
     * 完成订单
     */
    void finish(Long orderId);

    /**
     * 订单详情（含明细）
     */
    Order detail(Long orderId);

    /**
     * 我的订单
     */
    IPage<Order> pageMyOrders(Integer status, Integer pageNo, Integer pageSize);

    /**
     * 商家 / 管理员视角的订单列表
     */
    IPage<Order> pageShopOrders(Long shopId, Integer status, Integer pageNo, Integer pageSize);

    /**
     * 超时未支付自动取消（MQ 消费者调用）
     */
    void handleTimeout(String orderNo);

    /**
     * 试算金额（下单前预览）
     */
    BigDecimal previewAmount(OrderCreateDTO dto);
}
