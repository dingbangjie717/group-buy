package com.groupbuy.module.auth.controller;

import com.groupbuy.common.result.R;
import com.groupbuy.module.auth.dto.LoginDTO;
import com.groupbuy.module.auth.dto.RegisterDTO;
import com.groupbuy.module.auth.service.AuthService;
import com.groupbuy.module.auth.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return R.ok("登录成功", authService.login(loginDTO));
    }

    @PostMapping("/register")
    public R<Long> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return R.ok("注册成功", authService.register(registerDTO));
    }

    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        return R.ok(com.groupbuy.common.context.UserContext.current() == null
                ? Map.of()
                : Map.of(
                "userId", com.groupbuy.common.context.UserContext.current().getUserId(),
                "username", com.groupbuy.common.context.UserContext.current().getUsername(),
                "nickname", com.groupbuy.common.context.UserContext.current().getNickname(),
                "role", com.groupbuy.common.context.UserContext.current().getRole()));
    }
}
