package com.procurement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 跨域配置
 * <p>
 * 开发环境（application.yml 默认）：允许所有来源（方便本地调试）
 * 生产环境（application-prod.yml）：通过 cors.allowed-origins 指定精确来源
 * <br>
 * 注：微信小程序 wx.request 请求不受浏览器 CORS 约束，
 *     此配置主要保护 REST 端点不被任意浏览器来源跨域访问。
 * </p>
 */
@Configuration
public class CorsConfig {

    /**
     * 允许的跨域来源，多个地址以英文逗号分隔。
     * dev 默认 "*"，prod 通过环境变量 CORS_ALLOWED_ORIGINS 注入具体域名。
     */
    @Value("${cors.allowed-origins:*}")
    private String allowedOriginsRaw;

    @Bean
    public CorsFilter corsFilter() {
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();

        if (origins.size() == 1 && "*".equals(origins.get(0))) {
            // 开发环境：允许所有来源（不能同时开启 allowCredentials）
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            // 生产环境：精确来源列表 + 允许携带凭据
            config.setAllowedOrigins(origins);
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
