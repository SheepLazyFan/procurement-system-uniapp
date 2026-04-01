package com.procurement.service;

import com.procurement.common.constant.UserConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.WxLoginRequest;
import com.procurement.dto.response.LoginResponse;
import com.procurement.entity.SysUser;
import com.procurement.mapper.TeamMemberMapper;
import com.procurement.mapper.UserMapper;
import com.procurement.security.JwtTokenProvider;
import com.procurement.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthServiceImpl 单元测试
 * <p>
 * 测试范围：微信登录核心逻辑、用户注册默认角色、更新用户信息。
 * 依赖全部 Mock，不启动 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl — 认证服务")
class AuthServiceImplTest {

    // ===== Mocks =====
    @Mock private UserMapper userMapper;
    @Mock private TeamMemberMapper teamMemberMapper;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private WxService wxService;

    @InjectMocks
    private AuthServiceImpl authService;

    // ===========================================================
    // 1. wxLogin — 新用户注册
    // ===========================================================

    @Test
    @DisplayName("Should assign MEMBER role when new user registers via WeChat login")
    void should_assignMemberRole_when_newUserRegisters() {
        // Arrange
        WxLoginRequest request = new WxLoginRequest();
        request.setCode("valid_code");
        request.setNickName("张三");
        request.setAvatarUrl("https://avatar.example.com/1.jpg");

        when(wxService.code2Session("valid_code"))
                .thenReturn(Map.of("openid", "wx_openid_new_user"));
        when(userMapper.selectOne(any())).thenReturn(null); // 新用户
        when(jwtTokenProvider.generateToken(any(), any(), any(), any()))
                .thenReturn("mock_jwt_token");

        // Act
        LoginResponse response = authService.wxLogin(request);

        // Assert — 验证插入的用户角色必须是 MEMBER
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole())
                .as("新用户默认角色必须是 MEMBER，不能是 SELLER")
                .isEqualTo(UserConstants.ROLE_MEMBER);
    }

    @Test
    @DisplayName("Should NOT assign SELLER role when new user registers — critical business rule")
    void should_notAssignSellerRole_when_newUserRegisters() {
        // Arrange
        WxLoginRequest request = new WxLoginRequest();
        request.setCode("valid_code");

        when(wxService.code2Session("valid_code"))
                .thenReturn(Map.of("openid", "wx_openid_abc"));
        when(userMapper.selectOne(any())).thenReturn(null);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any()))
                .thenReturn("token");

        // Act
        authService.wxLogin(request);

        // Assert — 插入用户的 role 绝对不能是 SELLER
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getRole())
                .isNotEqualTo(UserConstants.ROLE_SELLER);
    }

    @Test
    @DisplayName("Should throw WX_LOGIN_FAILED when WeChat code is invalid (errcode != 0)")
    void should_throwWxLoginFailed_when_wxCodeInvalid() {
        // Arrange
        WxLoginRequest request = new WxLoginRequest();
        request.setCode("bad_code");

        when(wxService.code2Session("bad_code"))
                .thenReturn(Map.of("errcode", 40029, "errmsg", "invalid code"));

        // Act & Assert
        assertThatThrownBy(() -> authService.wxLogin(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.WX_LOGIN_FAILED.getCode()));
    }

    @Test
    @DisplayName("Should throw WX_LOGIN_FAILED when openid is null in WeChat response")
    void should_throwWxLoginFailed_when_openidIsNull() {
        // Arrange
        WxLoginRequest request = new WxLoginRequest();
        request.setCode("code_no_openid");

        when(wxService.code2Session("code_no_openid"))
                .thenReturn(Map.of("session_key", "abc123")); // no openid key

        // Act & Assert
        assertThatThrownBy(() -> authService.wxLogin(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Should update nickname and avatarUrl when existing user logs in again")
    void should_updateNicknameAndAvatar_when_existingUserLogsIn() {
        // Arrange
        SysUser existingUser = new SysUser();
        existingUser.setId(100L);
        existingUser.setWxOpenid("existing_openid");
        existingUser.setRole(UserConstants.ROLE_SELLER);
        existingUser.setNickName("旧昵称");

        WxLoginRequest request = new WxLoginRequest();
        request.setCode("existing_code");
        request.setNickName("新昵称");
        request.setAvatarUrl("https://new-avatar.com/img.jpg");

        when(wxService.code2Session("existing_code"))
                .thenReturn(Map.of("openid", "existing_openid"));
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any()))
                .thenReturn("token");

        // Act
        authService.wxLogin(request);

        // Assert — 旧用户的昵称和头像应被更新
        verify(userMapper).updateById(argThat(u ->
                "新昵称".equals(u.getNickName())
                        && "https://new-avatar.com/img.jpg".equals(u.getAvatarUrl())));
    }

    @Test
    @DisplayName("Should not overwrite nickname when new login request has null nickName")
    void should_notOverwriteNickname_when_newNickNameIsNull() {
        // Arrange
        SysUser existingUser = new SysUser();
        existingUser.setId(200L);
        existingUser.setWxOpenid("openid_200");
        existingUser.setNickName("保留旧昵称");

        WxLoginRequest request = new WxLoginRequest();
        request.setCode("code_200");
        request.setNickName(null); // 不传昵称

        when(wxService.code2Session("code_200"))
                .thenReturn(Map.of("openid", "openid_200"));
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        when(jwtTokenProvider.generateToken(any(), any(), any(), any()))
                .thenReturn("token");

        // Act
        authService.wxLogin(request);

        // Assert — 昵称不应被 null 覆盖
        verify(userMapper).updateById(argThat(u -> "保留旧昵称".equals(u.getNickName())));
    }

    // ===========================================================
    // 2. logout — Token 黑名单
    // ===========================================================

    @Test
    @DisplayName("Should blacklist token when logout is called with Bearer token")
    void should_blacklistToken_when_logoutWithBearerToken() {
        // Arrange
        String token = "Bearer actual_token_value";

        // Act
        authService.logout(token);

        // Assert — 去掉 "Bearer " 前缀后加入黑名单
        verify(jwtTokenProvider).blacklistToken("actual_token_value");
    }

    @Test
    @DisplayName("Should blacklist raw token when logout is called without Bearer prefix")
    void should_blacklistToken_when_logoutWithRawToken() {
        // Arrange
        String token = "raw_token_without_bearer";

        // Act
        authService.logout(token);

        // Assert
        verify(jwtTokenProvider).blacklistToken("raw_token_without_bearer");
    }

    // ===========================================================
    // 3. updateNickName — 昵称修改
    // ===========================================================

    @Test
    @DisplayName("Should update nickname when user exists")
    void should_updateNickname_when_userExists() {
        // Arrange
        SysUser user = new SysUser();
        user.setId(1L);
        user.setNickName("旧名");
        when(userMapper.selectById(1L)).thenReturn(user);

        // Act
        authService.updateNickName(1L, "新名字");

        // Assert
        verify(userMapper).updateById(argThat(u -> "新名字".equals(u.getNickName())));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when updating nickname of non-existent user")
    void should_throwNotFound_when_updatingNicknameOfNonExistentUser() {
        // Arrange
        when(userMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> authService.updateNickName(999L, "name"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ResultCode.NOT_FOUND.getCode()));
    }
}
