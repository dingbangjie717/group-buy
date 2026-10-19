package com.groupbuy.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单消息推送：按 userId 维护连接，订单状态变更时定向推送
 * <p>
 * 单机场景用 ConcurrentHashMap 维护即可；多实例部署时需要换成
 * Redis 发布订阅 / WebSocket 广播中间件，否则推送只能落到建立连接的那一台机器。
 */
@Slf4j
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WebSocket 建立连接 userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = resolveUserId(session);
        if (userId != null && sessions.get(userId) != null) {
            sessions.get(userId).remove(session);
        }
    }

    /**
     * 向指定用户推送消息
     */
    public void push(Long userId, String message) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null || set.isEmpty()) {
            log.debug("用户 {} 当前无在线 WebSocket 连接，消息丢弃", userId);
            return;
        }
        for (WebSocketSession session : set) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.warn("WebSocket 推送失败：{}", e.getMessage());
                }
            }
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        // 连接时优先从 attributes 读（握手拦截器写入），其次从 query 参数读
        Object attr = session.getAttributes().get("userId");
        if (attr != null) {
            return Long.valueOf(String.valueOf(attr));
        }
        String query = session.getUri() == null ? null : session.getUri().getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && "userId".equals(kv[0])) {
                try {
                    return Long.valueOf(kv[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
