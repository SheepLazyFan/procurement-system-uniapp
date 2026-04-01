package com.procurement.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.common.constant.UserConstants;
import com.procurement.entity.SysTeamMember;
import com.procurement.entity.SysUser;
import com.procurement.mapper.TeamMemberMapper;
import com.procurement.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义用户认证加载 — 通过手机号或用户 ID 加载用户，MEMBER 角色同时加载细分权限
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final TeamMemberMapper teamMemberMapper;

    /**
     * 通过手机号加载用户（Spring Security 标准方法）
     */
    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, phone));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + phone);
        }
        return new LoginUser(user);
    }

    /**
     * 通过用户 ID 加载用户（JWT 过滤器调用）。
     * MEMBER 用户会额外查询 sys_team_member 获取细分角色（ADMIN/SALES/WAREHOUSE），
     * 以便 @PreAuthorize 注解可以区分成员权限。
     */
    public UserDetails loadUserById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: ID=" + userId);
        }

        String memberRole = null;
        if (UserConstants.ROLE_MEMBER.equals(user.getRole()) && user.getEnterpriseId() != null) {
            SysTeamMember member = teamMemberMapper.selectOne(
                    new LambdaQueryWrapper<SysTeamMember>()
                            .eq(SysTeamMember::getUserId, userId)
                            .eq(SysTeamMember::getEnterpriseId, user.getEnterpriseId()));
            if (member != null) {
                memberRole = member.getRole();
            }
        }
        return new LoginUser(user, memberRole);
    }
}
