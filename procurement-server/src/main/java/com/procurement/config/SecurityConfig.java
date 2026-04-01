package com.procurement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurement.common.result.R;
import com.procurement.common.result.ResultCode;
import com.procurement.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置 — JWT 无状态认证、白名单、CORS
 * <p>
 * swagger.security.permit=true（dev）：Swagger UI 端点无需认证，便于开发调试
 * swagger.security.permit=false（prod）：所有 Swagger 端点均需认证，防止 API 泄露
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    /**
     * 生产环境通过 application-prod.yml 将此值设为 false，
     * 彻底关闭 Swagger/API-docs 的匿名访问。
     */
    @Value("${swagger.security.permit:true}")
    private boolean swaggerPermitAll;

    /**
     * 所有环境均放行的路径（业务必要白名单）
     */
    private static final String[] BUSINESS_WHITE_LIST = {
            "/auth/wx-login",
            "/buyer/store/**",
            "/buyer/product/**",
            "/local-files/**",     // 本地文件访问（开发阶段）— TODO: 部署COS后移除
            "/favicon.ico"
    };

    /**
     * 仅开发环境放行的 Swagger 路径（生产通过 swagger.security.permit=false 关闭）
     */
    private static final String[] SWAGGER_PATHS = {
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 动态组装白名单：生产环境不包含 Swagger 路径
        List<String> whiteList = new ArrayList<>(Arrays.asList(BUSINESS_WHITE_LIST));
        if (swaggerPermitAll) {
            whiteList.addAll(Arrays.asList(SWAGGER_PATHS));
        }
        String[] effectiveWhiteList = whiteList.toArray(new String[0]);

        http
                // 禁用 CSRF（前后端分离 + JWT 无状态）
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用 Session（JWT 无状态）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 路由权限
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(effectiveWhiteList).permitAll()
                        // 卖家/团队管理 — 仅 SELLER 和 MEMBER
                        .requestMatchers(
                                "/sales-orders/**", "/purchase-orders/**",
                                "/products/**", "/categories/**",
                                "/customers/**", "/suppliers/**",
                                "/statistics/**", "/team/**",
                                "/backup/**", "/enterprise/**",
                                "/file/**", "/files/**", "/subscribe/**"
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
