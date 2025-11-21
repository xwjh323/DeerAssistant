package com.wang.deerassistant.common;

public class ResponseUtil {

    private static final int SUCCESS_CODE = 0;
    private static final int DEFAULT_ERROR_CODE = 400;

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(SUCCESS_CODE, "success", null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, "success", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(DEFAULT_ERROR_CODE, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
