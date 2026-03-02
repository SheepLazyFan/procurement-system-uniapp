package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.common.constant.CommonConstants;
import com.procurement.common.constant.UserConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.LoginRequest;
import com.procurement.dto.request.WxLoginRequest;
import com.procurement.dto.response.LoginResponse;
import com.procurement.entity.SysUser;
import com.procurement.mapper.UserMapper;
import com.procurement.security.JwtTokenProvider;
import com.procurement.service.AuthService;
import com.procurement.service.SmsService;
import com.procurement.service.WxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final SmsService smsService;
    private final WxService wxService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void sendSmsCode(String phone) {
        // 频率限制：每个手机号60秒内只能发送1次
        String limitKey = "sms:limit:" + phone;
        Boolean limited = redisTemplate.opsForValue().setIfAbsent(limitKey, "1", 60, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(limited)) {
            throw new BusinessException(ResultCode.SMS_SEND_TOO_FREQUENT);
        }

        // 生成6位随机验证码（SecureRandom）
        String code = String.format("%06d", new SecureRandom().nextInt(999999));

        // 存入 Redis（5分钟过期）
        String redisKey = CommonConstants.REDIS_SMS_PREFIX + phone;
        redisTemplate.opsForValue().set(redisKey, code,
                CommonConstants.SMS_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 发送短信（当前为控制台打印模式）
        smsService.sendSmsCode(phone, code);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();

        // 验证短信验证码
        String redisKey = CommonConstants.REDIS_SMS_PREFIX + phone;
        String cachedCode = redisTemplate.opsForValue().get(redisKey);

        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new BusinessException(ResultCode.SMS_CODE_INVALID);
        }

        // 验证通过，删除验证码
        redisTemplate.delete(redisKey);

        // 查询或自动注册用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));

        if (user == null) {
            // 自动注册（捕获唯一索引冲突防并发重复）
            user = new SysUser();
            user.setPhone(phone);
            user.setRole(UserConstants.ROLE_SELLER);
            user.setLastLoginAt(LocalDateTime.now());
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException e) {
                // 并发插入冲突，重新查询已有用户
                user = userMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
            }
        } else {
            // 更新最后登录时间
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        // 生成 JWT Token
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getPhone(), user.getRole(), user.getEnterpriseId());

        // 构建响应
        return buildLoginResponse(token, user);
    }

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

        // 查询或自动注册买家用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getWxOpenid, openid));

        if (user == null) {
            user = new SysUser();
            user.setWxOpenid(openid);
            user.setRole(UserConstants.ROLE_BUYER);
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
            // 更新昵称和头像
            if (request.getNickName() != null) {
                user.setNickName(request.getNickName());
            }
            if (request.getAvatarUrl() != null) {
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

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(userInfo);
        return response;
    }
}
