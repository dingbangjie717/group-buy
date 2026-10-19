package com.groupbuy.module.auth.service.impl;

import com.groupbuy.common.constant.Constants;
import com.groupbuy.common.exception.BizException;
import com.groupbuy.common.result.ResultCode;
import com.groupbuy.module.auth.dto.LoginDTO;
import com.groupbuy.module.auth.dto.RegisterDTO;
import com.groupbuy.module.auth.service.AuthService;
import com.groupbuy.module.auth.vo.LoginVO;
import com.groupbuy.module.user.entity.SysUser;
import com.groupbuy.module.user.mapper.SysUserMapper;
import com.groupbuy.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, loginDTO.getUsername())
                        .last("LIMIT 1"));
        if (user == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRoleCode());
        return LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expireSeconds(jwtTokenProvider.getExpireSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername())
                .avatar(user.getAvatar())
                .role(user.getRoleCode())
                .shopId(user.getShopId())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO registerDTO) {
        Long count = sysUserMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, registerDTO.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.CONFLICT, "用户名已被注册");
        }
        // 注册只允许成为普通用户或商家，管理员只能由后台创建
        String role = StringUtils.hasText(registerDTO.getRole()) ? registerDTO.getRole() : Constants.ROLE_USER;
        if (!Constants.ROLE_USER.equals(role) && !Constants.ROLE_MERCHANT.equals(role)) {
            throw new BizException("不支持注册该角色");
        }
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(StringUtils.hasText(registerDTO.getNickname())
                ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setPhone(registerDTO.getPhone());
        user.setRoleCode(role);
        user.setStatus(1);
        sysUserMapper.insert(user);
        return user.getId();
    }
}
