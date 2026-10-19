package com.groupbuy.config;

import com.groupbuy.websocket.OrderWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：订单状态变更实时推送给前端
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderWebSocketHandler orderWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 前端连接：ws://localhost:8082/api/ws/order?userId=1
        registry.addHandler(orderWebSocketHandler, "/ws/order")
                .setAllowedOriginPatterns("*");
    }
}
