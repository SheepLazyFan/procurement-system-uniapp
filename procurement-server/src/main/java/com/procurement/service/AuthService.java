package com.procurement.service;

import com.procurement.dto.request.LoginRequest;
import com.procurement.dto.request.WxLoginRequest;
import com.procurement.dto.response.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 发送短信验证码
     */
    void sendSmsCode(String phone);

    /**
     * 手机号 + 验证码登录（自动注册）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 微信授权登录（买家端）
     */
    LoginResponse wxLogin(WxLoginRequest request);

    /**
     * 退出登录（Token 加入黑名单）
     */
    void logout(String token);
}
