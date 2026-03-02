package com.procurement.security;

import com.procurement.entity.SysUser;
import com.procurement.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义用户认证加载 — 通过手机号或用户 ID 加载用户
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    /**
     * 通过手机号加载用户（Spring Security 标准方法）
     */
    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, phone));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + phone);
        }
        return new LoginUser(user);
    }

    /**
     * 通过用户 ID 加载用户（JWT 过滤器调用）
     */
    public UserDetails loadUserById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: ID=" + userId);
        }
        return new LoginUser(user);
    }
}
