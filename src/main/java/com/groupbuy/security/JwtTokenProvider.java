package com.groupbuy.security;

import com.groupbuy.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getExpireMinutes() * 60 * 1000);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public Long getUserId(String token) {
        Object v = parseToken(token).get("userId");
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public String getRole(String token) {
        Object v = parseToken(token).get("role");
        return v == null ? null : String.valueOf(v);
    }

    public boolean validate(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("token 校验失败：{}", e.getMessage());
            return false;
        }
    }

    public long getExpireSeconds() {
        return jwtProperties.getExpireMinutes() * 60;
    }
}
