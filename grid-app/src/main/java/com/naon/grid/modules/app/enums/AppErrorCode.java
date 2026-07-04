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

    // 订阅相关 1200-1299
    SUBSCRIPTION_REQUIRED(1200, "需要订阅后才能访问此内容"),
    SUBSCRIPTION_EXPIRED(1201, "订阅已过期，请续费"),

    // 授权相关 1400-1499
    FORBIDDEN(1403, "没有权限"),

    // 参数错误 1100-1199
    INVALID_PHONE(1100, "手机号格式错误"),
    INVALID_PASSWORD(1101, "密码格式错误"),

    // 第三方登录相关 1102-1109
    SOCIAL_AUTH_FAILED(1102, "第三方登录验证失败"),
    SOCIAL_BIND_TOKEN_EXPIRED(1103, "操作超时，请重新登录"),
    SOCIAL_EMAIL_CONFLICT(1104, "该邮箱已绑定其他登录方式"),
    SOCIAL_ACCOUNT_DISABLED(1105, "账号已被禁用"),
    SOCIAL_PROVIDER_UNSUPPORTED(1106, "不支持的第三方登录方式"),

    // 系统错误 5000-5999
    SYSTEM_ERROR(5000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    AppErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
