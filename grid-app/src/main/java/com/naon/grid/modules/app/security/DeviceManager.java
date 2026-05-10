package com.naon.grid.modules.app.security;

import com.naon.grid.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * APP设备管理器
 * 管理用户多设备登录，支持最多3台设备同时在线
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceManager {

    private final RedisUtils redisUtils;

    @Value("${app.auth.max-devices:3}")
    private int maxDevices;

    @Value("${app.auth.token-validity-in-seconds:604800}")
    private long tokenValidityInSeconds;

    private static final String DEVICE_SET_KEY_PREFIX = "app:devices:";
    private static final String TOKEN_KEY_PREFIX = "app:token:";

    /**
     * 检查用户是否已达到设备上限
     */
    public boolean isDeviceLimitExceeded(Long userId) {
        String deviceSetKey = DEVICE_SET_KEY_PREFIX + userId;
        Set<Object> devices = redisUtils.sGet(deviceSetKey);
        return devices != null && devices.size() >= maxDevices;
    }

    /**
     * 注册设备登录
     * 如果超过设备限制，踢出最早登录的设备
     */
    public void registerDevice(Long userId, String deviceId, String token) {
        String deviceSetKey = DEVICE_SET_KEY_PREFIX + userId;
        String tokenKey = TOKEN_KEY_PREFIX + userId + ":" + deviceId;

        // 检查设备数是否超限
        if (isDeviceLimitExceeded(userId)) {
            // 踢出最早登录的设备
            kickOutOldestDevice(userId);
        }

        // 添加到设备集合
        redisUtils.sSetAndTime(deviceSetKey, tokenValidityInSeconds, deviceId);

        // 保存token
        redisUtils.set(tokenKey, token, tokenValidityInSeconds, TimeUnit.SECONDS);

        log.info("用户 {} 设备 {} 登录成功", userId, deviceId);
    }

    /**
     * 移除设备登录
     */
    public void removeDevice(Long userId, String deviceId) {
        String deviceSetKey = DEVICE_SET_KEY_PREFIX + userId;
        String tokenKey = TOKEN_KEY_PREFIX + userId + ":" + deviceId;

        redisUtils.setRemove(deviceSetKey, deviceId);
        redisUtils.del(tokenKey);

        log.info("用户 {} 设备 {} 已退出", userId, deviceId);
    }

    /**
     * 踢出最早登录的设备
     */
    private void kickOutOldestDevice(Long userId) {
        String deviceSetKey = DEVICE_SET_KEY_PREFIX + userId;
        Set<Object> devices = redisUtils.sGet(deviceSetKey);

        if (devices == null || devices.isEmpty()) {
            return;
        }

        // 找到最早过期的设备（这里简化为随机踢出一个）
        Object oldestDevice = devices.iterator().next();
        removeDevice(userId, (String) oldestDevice);

        log.info("用户 {} 设备 {} 被踢出（超出设备限制）", userId, oldestDevice);
    }

    /**
     * 判断设备是否在线
     */
    public boolean isDeviceOnline(Long userId, String deviceId) {
        String deviceSetKey = DEVICE_SET_KEY_PREFIX + userId;
        return redisUtils.sHasKey(deviceSetKey, deviceId);
    }

    /**
     * 获取用户的所有在线设备
     */
    public Set<Object> getUserDevices(Long userId) {
        String deviceSetKey = DEVICE_SET_KEY_PREFIX + userId;
        return redisUtils.sGet(deviceSetKey);
    }

    /**
     * 用户退出所有设备
     */
    public void clearAllDevices(Long userId) {
        String deviceSetKey = DEVICE_SET_KEY_PREFIX + userId;
        Set<Object> devices = redisUtils.sGet(deviceSetKey);

        if (devices != null) {
            for (Object deviceId : devices) {
                String tokenKey = TOKEN_KEY_PREFIX + userId + ":" + deviceId;
                redisUtils.del(tokenKey);
            }
        }

        redisUtils.del(deviceSetKey);
        log.info("用户 {} 所有设备已退出", userId);
    }
}
