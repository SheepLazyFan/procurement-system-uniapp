package com.procurement.security;

import com.procurement.entity.SysUser;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 安全上下文用户对象 — 封装 SysUser 实体，实现 Spring Security UserDetails
 * <p>
 * 权限体系：
 * <ul>
 *   <li>SELLER → ROLE_SELLER（店主，最高权限）</li>
 *   <li>MEMBER → ROLE_MEMBER + ROLE_{memberRole}（ADMIN/SALES/WAREHOUSE）</li>
 *   <li>BUYER  → ROLE_BUYER（买家端，只能访问 /buyer/** 白名单）</li>
 * </ul>
 * </p>
 */
@Data
public class LoginUser implements UserDetails {

    private final SysUser user;
    /** 团队成员细分角色（ADMIN/SALES/WAREHOUSE），SELLER/BUYER 为 null */
    private final String memberRole;

    /** 展期封装：无成员角色（SELLER/BUYER） */
    public LoginUser(SysUser user) {
        this(user, null);
    }

    public LoginUser(SysUser user, String memberRole) {
        this.user = user;
        this.memberRole = memberRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 顶级角色 ROLE_SELLER / ROLE_MEMBER / ROLE_BUYER
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        // 成员细分角色 ROLE_ADMIN / ROLE_SALES / ROLE_WAREHOUSE
        if (memberRole != null && !memberRole.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + memberRole));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getPhone() != null ? user.getPhone() : user.getWxOpenid();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getIsDeleted() == 0;
    }

    // ========== 便捷方法 ==========

    public Long getUserId() {
        return user.getId();
    }

    public String getRole() {
        return user.getRole();
    }

    public Long getEnterpriseId() {
        return user.getEnterpriseId();
    }
}
