package com.groupbuy.module.auth.service;

import com.groupbuy.module.auth.dto.LoginDTO;
import com.groupbuy.module.auth.dto.RegisterDTO;
import com.groupbuy.module.auth.vo.LoginVO;

/**
 * 认证服务
 */
public interface AuthService {

    LoginVO login(LoginDTO loginDTO);

    Long register(RegisterDTO registerDTO);
}
