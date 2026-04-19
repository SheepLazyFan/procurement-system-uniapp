package com.procurement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurement.common.result.R;
import com.procurement.common.result.ResultCode;
import com.procurement.entity.SysEnterprise;
import com.procurement.mapper.EnterpriseMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器 — 从请求头提取 Bearer Token，验证后注入 SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final EnterpriseMapper enterpriseMapper;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            try {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                if (!(userDetails instanceof LoginUser loginUser)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (isEnterpriseSessionInvalid(loginUser, token)) {
                    writeUnauthorized(response, ResultCode.SESSION_INVALIDATED);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.error("JWT 认证失败: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isEnterpriseSessionInvalid(LoginUser loginUser, String token) {
        if (loginUser.getEnterpriseId() == null || "BUYER".equals(loginUser.getRole())) {
            return false;
        }

        SysEnterprise enterprise = enterpriseMapper.selectById(loginUser.getEnterpriseId());
        if (enterprise == null) {
            log.warn("JWT 认证跳过：企业不存在 userId={}, enterpriseId={}",
                    loginUser.getUserId(), loginUser.getEnterpriseId());
            return true;
        }

        if (jwtTokenProvider.isIssuedBefore(token, enterprise.getSessionInvalidAfter())) {
            log.warn("JWT 认证失败：企业登录态已失效 userId={}, enterpriseId={}, sessionInvalidAfter={}",
                    loginUser.getUserId(), loginUser.getEnterpriseId(), enterprise.getSessionInvalidAfter());
            return true;
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(resultCode)));
        response.getWriter().flush();
    }
}
