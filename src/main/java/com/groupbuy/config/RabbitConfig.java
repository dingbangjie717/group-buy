package com.groupbuy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：用「消息 TTL + 死信交换机」实现订单超时取消
 * <p>
 * 流程：
 * 1. 下单成功后把订单号投递到 order.delay.queue（无消费者、设置 TTL）
 * 2. 消息过期后被投递到死信交换机 order.exchange，routingKey = order.timeout
 * 3. order.timeout.queue 的消费端收到订单号，回查订单状态，若仍未支付则取消并回补库存
 * <p>
 * 说明：TTL + 死信是队列级别的固定延迟；如果需要「每单不同延迟时间」，
 * 可以改用 rabbitmq_delayed_message_exchange 插件的 x-delayed-message 交换机。
 */
@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ROUTING_TIMEOUT = "order.timeout";

    private final OrderProperties orderProperties;

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    /** 真正被消费的队列 */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderExchange()).with(ROUTING_TIMEOUT);
    }

    /** 延迟队列：无消费者，消息过期后进入死信交换机 */
    @Bean
    public Queue orderDelayQueue() {
        long ttl = (long) (orderProperties.getTimeoutMinutes() == null ? 15 : orderProperties.getTimeoutMinutes())
                * 60 * 1000;
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .ttl((int) ttl)                                   // 消息存活时间
                .deadLetterExchange(ORDER_EXCHANGE)               // 过期后转发到该交换机
                .deadLetterRoutingKey(ROUTING_TIMEOUT)
                .build();
    }
}
