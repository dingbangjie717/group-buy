package com.groupbuy.module.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录结果
 */
@Data
@Builder
public class LoginVO {

    private String token;
    private String tokenType;
    private Long expireSeconds;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private Long shopId;
}
