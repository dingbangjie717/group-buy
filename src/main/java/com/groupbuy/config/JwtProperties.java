package com.groupbuy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "groupbuy.jwt")
public class JwtProperties {

    private String secret;
    private Long expireMinutes = 180L;
}
