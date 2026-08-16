package com.lottery.config;

import com.lottery.dto.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArg(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("参数错误 [{} {}]: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return ApiResult.fail(400, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValid(Exception e) {
        return ApiResult.fail(400, "参数校验失败: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleGeneral(Exception e, HttpServletRequest req) {
        log.error("服务器内部错误 [{} {}]", req.getMethod(), req.getRequestURI(), e);
        return ApiResult.fail(500, "服务器内部错误: " + e.getMessage());
    }
}
