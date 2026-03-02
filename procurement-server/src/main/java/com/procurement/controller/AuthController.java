package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.LoginRequest;
import com.procurement.dto.request.SmsSendRequest;
import com.procurement.dto.request.WxLoginRequest;
import com.procurement.dto.response.LoginResponse;
import com.procurement.entity.SysUser;
import com.procurement.security.LoginUser;
import com.procurement.service.AuthService;
import com.procurement.service.EnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证控制器 — 登录 / 注册 / 短信验证码
 */
@Tag(name = "认证模块")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EnterpriseService enterpriseService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public R<Map<String, Object>> sendSmsCode(@Valid @RequestBody SmsSendRequest request) {
        authService.sendSmsCode(request.getPhone());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("expireSeconds", 300);
        return R.ok(data);
    }

    @Operation(summary = "手机号 + 验证码登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "微信授权登录（买家端）")
    @PostMapping("/wx-login")
    public R<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return R.ok(authService.wxLogin(request));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public R<Map<String, Object>> getProfile(@AuthenticationPrincipal LoginUser loginUser) {
        SysUser user = loginUser.getUser();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("phone", user.getPhone());
        data.put("role", user.getRole());
        data.put("nickName", user.getNickName());
        data.put("avatarUrl", user.getAvatarUrl());

        // 附带企业信息
        if (user.getEnterpriseId() != null) {
            data.put("enterprise", enterpriseService.getByUser(user.getId()));
        }

        return R.ok(data);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return R.ok();
    }
}
