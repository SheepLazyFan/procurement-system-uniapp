package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.common.constant.UserConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.WxLoginRequest;
import com.procurement.dto.response.LoginResponse;
import com.procurement.entity.SysTeamMember;
import com.procurement.entity.SysUser;
import com.procurement.mapper.TeamMemberMapper;
import com.procurement.mapper.UserMapper;
import com.procurement.security.JwtTokenProvider;
import com.procurement.service.AuthService;
import com.procurement.service.WxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final WxService wxService;

    @Override
    @Transactional
    public LoginResponse wxLogin(WxLoginRequest request) {
        // 调用微信 code2Session
        Map<String, Object> wxResult = wxService.code2Session(request.getCode());

        if (wxResult.containsKey("errcode") && (int) wxResult.get("errcode") != 0) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }

        String openid = (String) wxResult.get("openid");
        if (openid == null || openid.isEmpty()) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }

        // 查询或自动注册商家用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getWxOpenid, openid));

        if (user == null) {
            user = new SysUser();
            user.setWxOpenid(openid);
            user.setRole(UserConstants.ROLE_MEMBER);
            user.setNickName(request.getNickName());
            user.setAvatarUrl(request.getAvatarUrl());
            user.setLastLoginAt(LocalDateTime.now());
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException e) {
                // 并发插入冲突，重新查询已有用户
                user = userMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getWxOpenid, openid));
            }
        } else {
            // 更新昵称、头像（仅覆盖非空字段）
            if (request.getNickName() != null && !request.getNickName().isEmpty()) {
                user.setNickName(request.getNickName());
            }
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
                user.setAvatarUrl(request.getAvatarUrl());
            }
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        // 生成 JWT Token
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getPhone(), user.getRole(), user.getEnterpriseId());

        return buildLoginResponse(token, user);
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        jwtTokenProvider.blacklistToken(token);
    }

    @Override
    public String updateAvatar(Long userId, String avatarUrl) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
        return avatarUrl;
    }

    @Override
    public void updateNickName(Long userId, String nickName) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        user.setNickName(nickName);
        userMapper.updateById(user);
    }

    /**
     * 构建登录响应
     */
    private LoginResponse buildLoginResponse(String token, SysUser user) {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setPhone(user.getPhone());
        userInfo.setRole(user.getRole());
        userInfo.setNickName(user.getNickName());
        userInfo.setAvatarUrl(user.getAvatarUrl());
        userInfo.setEnterpriseId(user.getEnterpriseId());
        userInfo.setWxOpenid(user.getWxOpenid());

        // 若为团队成员，查询其团队角色（ADMIN / SALES / WAREHOUSE）
        if (UserConstants.ROLE_MEMBER.equals(user.getRole()) && user.getEnterpriseId() != null) {
            SysTeamMember member = teamMemberMapper.selectOne(
                    new LambdaQueryWrapper<SysTeamMember>()
                            .eq(SysTeamMember::getUserId, user.getId())
                            .eq(SysTeamMember::getEnterpriseId, user.getEnterpriseId()));
            if (member != null) {
                userInfo.setMemberRole(member.getRole());
            }
        }

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(userInfo);
        return response;
    }
}
