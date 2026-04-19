package com.procurement.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token 工具类 — 生成 / 解析 / 验证
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiration;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expiration,
                            StringRedisTemplate redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(Long userId, String phone, String role, Long enterpriseId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", phone)
                .claim("role", role)
                .claim("enterpriseId", enterpriseId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取 Token 签发时间
     */
    public LocalDateTime getIssuedAt(String token) {
        Date issuedAt = parseToken(token).getIssuedAt();
        if (issuedAt == null) {
            return null;
        }
        return LocalDateTime.ofInstant(issuedAt.toInstant(), ZoneId.systemDefault());
    }

    /**
     * 判断 Token 是否早于给定失效时间签发
     */
    public boolean isIssuedBefore(String token, LocalDateTime invalidAfter) {
        if (invalidAfter == null) {
            return false;
        }
        LocalDateTime issuedAt = getIssuedAt(token);
        return issuedAt != null && issuedAt.isBefore(invalidAfter);
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 检查是否在黑名单中
            if (isTokenBlacklisted(token)) {
                log.warn("Token 已被加入黑名单");
                return false;
            }
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT Token 无效: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 将 Token 加入黑名单（退出登录时调用）
     */
    public void blacklistToken(String token) {
        try {
            Claims claims = parseToken(token);
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        TOKEN_BLACKLIST_PREFIX + token, "1", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("Token 加入黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token));
    }
}
