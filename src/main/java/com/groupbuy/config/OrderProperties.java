package com.groupbuy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "groupbuy.order")
public class OrderProperties {

    /** 超时未支付自动取消的分钟数 */
    private Integer timeoutMinutes = 15;
}
