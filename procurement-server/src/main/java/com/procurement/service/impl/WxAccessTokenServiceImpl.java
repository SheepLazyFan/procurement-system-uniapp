package com.procurement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurement.config.WxConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信 access_token 获取与缓存服务
 * <p>
 * access_token 有效期 2 小时，通过 Redis 缓存避免频繁请求微信接口。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxAccessTokenServiceImpl {

    private final WxConfig wxConfig;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY = "wx:access_token";
    /** 缓存 110 分钟（微信 token 有效期 120 分钟，留 10 分钟余量） */
    private static final long CACHE_MINUTES = 110;

    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";

    /**
     * 获取 access_token（优先从 Redis 缓存读取）
     */
    @SuppressWarnings("unchecked")
    public String getAccessToken() {
        // 1. 尝试从缓存获取
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        // 2. 缓存未命中，调用微信接口获取
        String url = String.format(TOKEN_URL, wxConfig.getAppId(), wxConfig.getAppSecret());
        RestTemplate restTemplate = new RestTemplate();

        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("微信 getAccessToken 响应: {}", response);

            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            if (result.containsKey("access_token")) {
                String accessToken = (String) result.get("access_token");
                // 缓存到 Redis
                redisTemplate.opsForValue().set(CACHE_KEY, accessToken, CACHE_MINUTES, TimeUnit.MINUTES);
                return accessToken;
            } else {
                log.error("获取 access_token 失败: {}", result.get("errmsg"));
                return null;
            }
        } catch (Exception e) {
            log.error("获取 access_token 异常", e);
            return null;
        }
    }
}
