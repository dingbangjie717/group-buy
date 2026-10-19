package com.groupbuy.common.context;

import com.groupbuy.common.exception.BizException;
import com.groupbuy.common.result.ResultCode;
import com.groupbuy.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户上下文
 */
public final class UserContext {

    private UserContext() {
    }

    public static LoginUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }

    public static Long userId() {
        LoginUser loginUser = current();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static Long requireUserId() {
        Long userId = userId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static String role() {
        LoginUser loginUser = current();
        return loginUser == null ? null : loginUser.getRole();
    }
}
