package com.groupbuy.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.groupbuy.common.constant.Constants;
import com.groupbuy.common.context.UserContext;
import com.groupbuy.common.exception.BizException;
import com.groupbuy.common.result.ResultCode;
import com.groupbuy.config.OrderProperties;
import com.groupbuy.config.RabbitConfig;
import com.groupbuy.module.cart.service.CartService;
import com.groupbuy.module.cart.vo.CartItemVO;
import com.groupbuy.module.coupon.service.CouponService;
import com.groupbuy.module.goods.entity.Goods;
import com.groupbuy.module.goods.mapper.GoodsMapper;
import com.groupbuy.module.goods.mapper.ShopMapper;
import com.groupbuy.module.order.dto.OrderCreateDTO;
import com.groupbuy.module.order.entity.Order;
import com.groupbuy.module.order.entity.OrderItem;
import com.groupbuy.module.order.mapper.OrderItemMapper;
import com.groupbuy.module.order.mapper.OrderMapper;
import com.groupbuy.module.order.service.OrderService;
import com.groupbuy.websocket.OrderWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现（本项目核心）
 * <p>
 * 防超卖三板斧：
 * 1. Redis Lua 脚本原子预扣库存（判断 + 扣减一步完成）
 * 2. 数据库条件更新 UPDATE ... WHERE stock >= num，影响行数为 0 即库存不足
 * 3. 下单接口用 SETNX 做幂等，防止用户重复点击生成重复订单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;
    private final CartService cartService;
    private final CouponService couponService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> deductStockScript;
    private final DefaultRedisScript<Long> restoreStockScript;
    private final RabbitTemplate rabbitTemplate;
    private final OrderWebSocketHandler webSocketHandler;
    private final OrderProperties orderProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(OrderCreateDTO dto) {
        Long userId = UserContext.requireUserId();

        // 幂等锁：同一用户 5 秒内不能重复下单，防止前端重复点击 / 网络重试
        String lockKey = Constants.KEY_ORDER_LOCK + userId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException(ResultCode.TOO_MANY_REQUEST, "订单正在处理中，请勿重复提交");
        }

        try {
            List<BuyItem> buyItems = resolveItems(userId, dto);
            if (buyItems.isEmpty()) {
                throw new BizException("请选择要购买的商品");
            }

            // 1）计算商品总额
            BigDecimal goodsAmount = BigDecimal.ZERO;
            for (BuyItem item : buyItems) {
                goodsAmount = goodsAmount.add(item.price.multiply(BigDecimal.valueOf(item.quantity)));
            }

            // 2）优惠券试算
            BigDecimal discountAmount = couponService.calcDiscount(dto.getUserCouponId(), goodsAmount);
            BigDecimal payAmount = goodsAmount.subtract(discountAmount);
            if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
                payAmount = BigDecimal.ZERO;
            }

            // 3）扣库存：先 Redis 预扣，再 DB 条件更新；任一步失败都要回滚已扣的 Redis 库存
            List<Long> deductedGoods = new ArrayList<>();
            try {
                for (BuyItem item : buyItems) {
                    deductStockWithLua(item.goodsId, item.quantity);
                    deductedGoods.add(item.goodsId);
                }
                for (BuyItem item : buyItems) {
                    int rows = goodsMapper.deductStock(item.goodsId, item.quantity);
                    if (rows == 0) {
                        throw new BizException(ResultCode.STOCK_NOT_ENOUGH,
                                "商品「" + item.goodsName + "」库存不足");
                    }
                }
            } catch (RuntimeException e) {
                // 任何一步失败都要把已预扣的 Redis 库存还回去
                rollbackStock(buyItems, deductedGoods);
                throw e;
            }

            // 4）落库
            Order order = new Order();
            order.setOrderNo(generateOrderNo(userId));
            order.setUserId(userId);
            order.setShopId(dto.getShopId());
            order.setGoodsAmount(goodsAmount.setScale(2, RoundingMode.HALF_UP));
            order.setDiscountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP));
            order.setPayAmount(payAmount.setScale(2, RoundingMode.HALF_UP));
            order.setUserCouponId(dto.getUserCouponId());
            order.setStatus(Constants.ORDER_WAIT_PAY);
            order.setReceiver(dto.getReceiver());
            order.setPhone(dto.getPhone());
            order.setAddress(dto.getAddress());
            order.setRemark(dto.getRemark());
            orderMapper.insert(order);

            for (BuyItem item : buyItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setGoodsId(item.goodsId);
                // 商品名与单价做快照，后续改价不影响历史订单
                orderItem.setGoodsName(item.goodsName);
                orderItem.setGoodsImage(item.goodsImage);
                orderItem.setPrice(item.price);
                orderItem.setQuantity(item.quantity);
                orderItem.setAmount(item.price.multiply(BigDecimal.valueOf(item.quantity))
                        .setScale(2, RoundingMode.HALF_UP));
                orderItemMapper.insert(orderItem);
            }

            // 5）清理购物车中已结算的商品
            cartService.removeBatch(userId, buyItems.stream().map(i -> i.goodsId).toList());

            // 6）投递延迟消息，超时未支付自动取消
            rabbitTemplate.convertAndSend("", RabbitConfig.ORDER_DELAY_QUEUE, order.getOrderNo());

            log.info("下单成功 orderNo={}, userId={}, payAmount={}", order.getOrderNo(), userId, payAmount);
            return order.getOrderNo();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(String orderNo) {
        Long userId = UserContext.requireUserId();
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BizException(403, "只能支付自己的订单");
        }
        if (order.getStatus() != Constants.ORDER_WAIT_PAY) {
            throw new BizException("订单状态不正确，无法支付");
        }
        order.setStatus(Constants.ORDER_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayType(1);
        orderMapper.updateById(order);

        couponService.consume(order.getUserCouponId(), order.getId());
        notifyStatus(order, "订单已支付，等待商家接单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (order.getStatus() != Constants.ORDER_WAIT_PAY && order.getStatus() != Constants.ORDER_PAID) {
            throw new BizException("当前状态不允许取消");
        }
        order.setStatus(Constants.ORDER_CANCELED);
        order.setRemark(reason);
        orderMapper.updateById(order);

        // 取消要回补库存：Redis 与 DB 都要还回去
        restoreStock(order.getId());
        notifyStatus(order, "订单已取消：" + reason);
    }

    @Override
    public void accept(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != Constants.ORDER_PAID) {
            throw new BizException("订单状态不正确，无法接单");
        }
        order.setStatus(Constants.ORDER_ACCEPTED);
        orderMapper.updateById(order);
        notifyStatus(order, "商家已接单，正在备货");
    }

    @Override
    public void deliver(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != Constants.ORDER_ACCEPTED) {
            throw new BizException("订单状态不正确，无法配送");
        }
        order.setStatus(Constants.ORDER_DELIVERING);
        orderMapper.updateById(order);
        notifyStatus(order, "订单配送中，请耐心等待");
    }

    @Override
    public void finish(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != Constants.ORDER_DELIVERING) {
            throw new BizException("订单状态不正确，无法完成");
        }
        order.setStatus(Constants.ORDER_FINISHED);
        orderMapper.updateById(order);
        notifyStatus(order, "订单已完成，感谢您的购买");
    }

    @Override
    public Order detail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        Long userId = UserContext.userId();
        String role = UserContext.role();
        boolean owner = userId != null && userId.equals(order.getUserId());
        boolean staff = Constants.ROLE_MERCHANT.equals(role) || Constants.ROLE_ADMIN.equals(role);
        if (!owner && !staff) {
            throw new BizException(403, "无权查看该订单");
        }
        order.setItems(orderItemMapper.selectByOrderId(orderId));
        var shop = shopMapper.selectById(order.getShopId());
        if (shop != null) {
            order.setShopName(shop.getName());
        }
        return order;
    }

    @Override
    public IPage<Order> pageMyOrders(Integer status, Integer pageNo, Integer pageSize) {
        Long userId = UserContext.requireUserId();
        return page(Page.of(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(status != null, Order::getStatus, status)
                        .orderByDesc(Order::getCreateTime));
    }

    @Override
    public IPage<Order> pageShopOrders(Long shopId, Integer status, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(shopId != null, Order::getShopId, shopId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime);
        IPage<Order> page = page(Page.of(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        fillShopName(page.getRecords());
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleTimeout(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return;
        }
        // 关键：回查订单状态。用户可能刚好在消息过期前完成支付
        if (order.getStatus() != Constants.ORDER_WAIT_PAY) {
            log.info("订单 {} 已支付或已处理，跳过超时取消", orderNo);
            return;
        }
        order.setStatus(Constants.ORDER_CANCELED);
        order.setRemark("超时未支付，系统自动取消");
        orderMapper.updateById(order);
        restoreStock(order.getId());
        notifyStatus(order, "订单超时未支付，已自动取消");
        log.info("订单 {} 超时未支付，已自动取消", orderNo);
    }

    @Override
    public BigDecimal previewAmount(OrderCreateDTO dto) {
        Long userId = UserContext.requireUserId();
        List<BuyItem> items = resolveItems(userId, dto);
        BigDecimal goodsAmount = BigDecimal.ZERO;
        for (BuyItem item : items) {
            goodsAmount = goodsAmount.add(item.price.multiply(BigDecimal.valueOf(item.quantity)));
        }
        BigDecimal discount = couponService.calcDiscount(dto.getUserCouponId(), goodsAmount);
        BigDecimal pay = goodsAmount.subtract(discount);
        return pay.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pay;
    }

    // ---------------- 私有方法 ----------------

    /**
     * 结算商品来源：优先使用前端传入的 items，否则取购物车中该店铺已勾选的商品
     */
    private List<BuyItem> resolveItems(Long userId, OrderCreateDTO dto) {
        List<BuyItem> result = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dto.getItems())) {
            for (OrderCreateDTO.ItemDTO item : dto.getItems()) {
                Goods goods = goodsMapper.selectById(item.getGoodsId());
                if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
                    throw new BizException("商品不存在或已下架");
                }
                result.add(BuyItem.of(goods, item.getQuantity()));
            }
            return result;
        }
        List<CartItemVO> cartItems = cartService.listCheckedByShop(userId, dto.getShopId());
        if (cartItems.isEmpty()) {
            throw new BizException("购物车中没有勾选商品");
        }
        for (CartItemVO cartItem : cartItems) {
            Goods goods = goodsMapper.selectById(cartItem.getGoodsId());
            if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
                throw new BizException("商品「" + cartItem.getGoodsName() + "」已下架");
            }
            result.add(BuyItem.of(goods, cartItem.getQuantity()));
        }
        return result;
    }

    /**
     * Redis 预扣库存：-1 表示未预热（回源后再试一次），-2 表示库存不足
     */
    private void deductStockWithLua(Long goodsId, Integer quantity) {
        String key = Constants.KEY_STOCK + goodsId;
        Long remain = redisTemplate.execute(deductStockScript, Collections.singletonList(key), quantity);
        if (remain == null) {
            throw new BizException("库存服务异常，请稍后重试");
        }
        if (remain == -1L) {
            // 缓存未预热，回源数据库后重试一次
            Goods goods = goodsMapper.selectById(goodsId);
            if (goods == null) {
                throw new BizException("商品不存在");
            }
            redisTemplate.opsForValue().set(key, goods.getStock() == null ? 0 : goods.getStock(),
                    1, TimeUnit.DAYS);
            remain = redisTemplate.execute(deductStockScript, Collections.singletonList(key), quantity);
            if (remain == null || remain < 0) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }
        }
        if (remain == -2L) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }
    }

    /**
     * 回滚已经预扣的 Redis 库存
     */
    private void rollbackStock(List<BuyItem> items, List<Long> deductedGoods) {
        for (Long goodsId : deductedGoods) {
            Integer quantity = items.stream()
                    .filter(i -> i.goodsId.equals(goodsId))
                    .map(i -> i.quantity)
                    .findFirst().orElse(0);
            if (quantity > 0) {
                redisTemplate.execute(restoreStockScript,
                        Collections.singletonList(Constants.KEY_STOCK + goodsId), quantity);
            }
        }
    }

    /**
     * 回补库存（订单取消 / 超时取消）
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            goodsMapper.restoreStock(item.getGoodsId(), item.getQuantity());
            redisTemplate.execute(restoreStockScript,
                    Collections.singletonList(Constants.KEY_STOCK + item.getGoodsId()),
                    item.getQuantity());
        }
    }

    /**
     * WebSocket 推送订单状态变更
     */
    private void notifyStatus(Order order, String message) {
        Map<String, Object> payload = Map.of(
                "orderId", String.valueOf(order.getId()),
                "orderNo", order.getOrderNo(),
                "status", String.valueOf(order.getStatus()),
                "message", message
        );
        try {
            webSocketHandler.push(order.getUserId(), cn.hutool.json.JSONUtil.toJsonStr(payload));
        } catch (Exception e) {
            log.warn("WebSocket 推送失败：{}", e.getMessage());
        }
    }

    private void fillShopName(List<Order> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        Map<Long, String> shopNames = orders.stream()
                .map(Order::getShopId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> {
                    var shop = shopMapper.selectById(id);
                    return shop == null ? "" : shop.getName();
                }));
        orders.forEach(o -> o.setShopName(shopNames.get(o.getShopId())));
    }

    private String generateOrderNo(Long userId) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = (int) (Math.random() * 9000) + 1000;
        return time + String.format("%04d", userId % 10000) + rand;
    }

    /**
     * 下单商品的内部载体
     */
    private static class BuyItem {
        Long goodsId;
        String goodsName;
        String goodsImage;
        BigDecimal price;
        Integer quantity;

        static BuyItem of(Goods goods, Integer quantity) {
            BuyItem item = new BuyItem();
            item.goodsId = goods.getId();
            item.goodsName = goods.getName();
            item.goodsImage = goods.getImage();
            item.price = goods.getPrice() == null ? BigDecimal.ZERO : goods.getPrice();
            item.quantity = quantity == null || quantity <= 0 ? 1 : quantity;
            return item;
        }
    }
}
