package com.procurement.security;

import com.procurement.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 安全上下文用户对象 — 封装 SysUser 实体，实现 Spring Security UserDetails
 */
@Data
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private final SysUser user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
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
