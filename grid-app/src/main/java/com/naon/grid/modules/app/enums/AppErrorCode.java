package com.naon.grid.modules.app.enums;

import lombok.Getter;

@Getter
public enum AppErrorCode {
    // 认证相关 1000-1099
    USERNAME_EXISTS(1000, "用户名已存在"),
    PHONE_EXISTS(1001, "手机号已注册"),
    INVALID_CREDENTIALS(1002, "手机号或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    TOKEN_INVALID(1005, "无效的Token"),
    DEVICE_LIMIT_EXCEEDED(1006, "设备数量超出限制"),

    // 参数错误 1100-1199
    INVALID_PHONE(1100, "手机号格式错误"),
    INVALID_PASSWORD(1101, "密码格式错误"),

    // 系统错误 5000-5999
    SYSTEM_ERROR(5000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    AppErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
