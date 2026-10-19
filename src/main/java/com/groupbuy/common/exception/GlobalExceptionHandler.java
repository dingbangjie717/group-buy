package com.groupbuy.common.exception;

import com.groupbuy.common.result.R;
import com.groupbuy.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常 uri={}, msg={}", request.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).collect(Collectors.joining("; "));
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).collect(Collectors.joining("; "));
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        return R.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 uri={}", request.getRequestURI(), e);
        return R.fail(ResultCode.ERROR.getCode(), "系统繁忙，请稍后再试");
    }
}
