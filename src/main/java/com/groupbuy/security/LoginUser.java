package com.groupbuy.security;

import com.groupbuy.common.constant.Constants;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 登录用户主体
 */
@Data
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    /** 本项目采用「一用户一角色」的轻量模型，区别于考试系统的完整 RBAC */
    private String role;
    private boolean enabled = true;

    public LoginUser() {
    }

    public LoginUser(Long userId, String username, String password, String role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(Constants.ROLE_PREFIX + role));
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
        return enabled;
    }
}
