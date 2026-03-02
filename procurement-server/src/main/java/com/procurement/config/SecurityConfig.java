package com.procurement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurement.common.result.R;
import com.procurement.common.result.ResultCode;
import com.procurement.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.PrintWriter;

/**
 * Spring Security 配置 — JWT 无状态认证、白名单、CORS
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    /**
     * 白名单路径 — 无需 JWT 认证
     */
    private static final String[] WHITE_LIST = {
            "/auth/sms/send",
            "/auth/login",
            "/auth/wx-login",
            "/buyer/store/**",
            "/buyer/product/**",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（前后端分离 + JWT 无状态）
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用 Session（JWT 无状态）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 路由权限
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        // 买家订单相关 — 需认证，任何角色可访问
                        .requestMatchers("/buyer/orders/**").authenticated()
                        // 卖家/团队管理 — 仅 SELLER 和 MEMBER
                        .requestMatchers(
                                "/sales-orders/**", "/purchase-orders/**",
                                "/products/**", "/categories/**",
                                "/customers/**", "/suppliers/**",
                                "/statistics/**", "/team/**",
                                "/backup/**", "/enterprise/**",
                                "/file/**"
                        ).hasAnyRole("SELLER", "MEMBER")
                        .anyRequest().authenticated()
                )
                // 异常处理
                .exceptionHandling(exception -> exception
                        // 未认证
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(objectMapper.writeValueAsString(
                                    R.fail(ResultCode.UNAUTHORIZED)));
                            writer.flush();
                        })
                        // 无权限
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(objectMapper.writeValueAsString(
                                    R.fail(ResultCode.FORBIDDEN)));
                            writer.flush();
                        })
                )
                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
