package com.groupbuy.module.order.mq;

import com.groupbuy.config.RabbitConfig;
import com.groupbuy.module.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息消费者
 * <p>
 * 消息由 order.delay.queue 过期后经死信交换机转发到这里。
 * 消费时一定要回查订单状态，因为用户可能在消息过期前刚好完成支付。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitConfig.ORDER_TIMEOUT_QUEUE)
    public void onTimeout(String orderNo) {
        log.info("收到订单超时消息 orderNo={}", orderNo);
        try {
            orderService.handleTimeout(orderNo);
        } catch (Exception e) {
            // 捕获异常，避免消息无限重投把服务打挂；真实项目应记录到「异常订单表」并告警
            log.error("处理订单超时失败 orderNo={}", orderNo, e);
        }
    }
}
