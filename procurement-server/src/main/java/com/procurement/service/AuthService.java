package com.procurement.service;

import com.procurement.dto.request.WxLoginRequest;
import com.procurement.dto.response.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 微信授权登录
     */
    LoginResponse wxLogin(WxLoginRequest request);

    /**
     * 退出登录（Token 加入黑名单）
     */
    void logout(String token);

    /**
     * 更新用户头像
     */
    String updateAvatar(Long userId, String avatarUrl);

    /**
     * 更新用户昵称
     */
    void updateNickName(Long userId, String nickName);
}
