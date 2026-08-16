package com.lottery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private int code;       // 0=成功, 非0=错误码
    private String msg;
    private T data;

    public static <T> ApiResult<T> ok() {
        return new ApiResult<>(0, "success", null);
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "success", data);
    }

    public static <T> ApiResult<T> ok(String msg, T data) {
        return new ApiResult<>(0, msg, data);
    }

    public static <T> ApiResult<T> fail(String msg) {
        return new ApiResult<>(500, msg, null);
    }

    public static <T> ApiResult<T> fail(int code, String msg) {
        return new ApiResult<>(code, msg, null);
    }
}
