package com.naon.grid.modules.app.security;

import com.alibaba.fastjson2.JSON;
import com.naon.grid.utils.RedisUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis 会话管理器 — 追踪活跃设备，限制同时登录设备数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {

    private static final String SESSION_KEY_PREFIX = "app:sessions:";

    private final RedisUtils redisUtils;

    @Value("${app.auth.token-expire-seconds:604800}")
    private long tokenExpireSeconds;

    @Value("${app.auth.max-devices:3}")
    private int maxDevices;

    // ── public API ──

    /** O(1) 检查设备是否在活跃会话中。Redis 不可用时抛出异常，由调用方决定降级策略。 */
    public boolean isActive(Long userId, String deviceId) {
        return redisUtils.hHasKey(sessionKey(userId), deviceId);
    }

    /** 获取当前活跃设备数。Redis 不可用时抛出异常。 */
    public int getActiveCount(Long userId) {
        Map<Object, Object> sessions = redisUtils.hmget(sessionKey(userId));
        return sessions == null ? 0 : sessions.size();
    }

    /** 添加或更新活跃会话（hset 覆盖写入），并刷新 key 的 TTL。 */
    public void addSession(Long userId, String deviceId, String deviceName) {
        try {
            String key = sessionKey(userId);
            SessionData data = new SessionData(
                    System.currentTimeMillis() / 1000,
                    deviceName != null ? deviceName : ""
            );
            redisUtils.hset(key, deviceId, JSON.toJSONString(data));
            redisUtils.expire(key, tokenExpireSeconds);
        } catch (Exception e) {
            log.warn("Failed to add session to Redis userId={} deviceId={}", userId, deviceId, e);
        }
    }

    /** 从活跃会话中移除指定设备。 */
    public void removeSession(Long userId, String deviceId) {
        try {
            redisUtils.hdel(sessionKey(userId), deviceId);
        } catch (Exception e) {
            log.warn("Failed to remove session from Redis userId={} deviceId={}", userId, deviceId, e);
        }
    }

    /** 遍历所有会话，找出 loginTime 最早的 deviceId。返回 null 表示无活跃会话。 */
    public String findOldestDeviceId(Long userId) {
        Map<Object, Object> sessions = redisUtils.hmget(sessionKey(userId));
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }
        String oldest = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<Object, Object> entry : sessions.entrySet()) {
            String did = (String) entry.getKey();
            String json = entry.getValue() != null ? entry.getValue().toString() : null;
            if (json == null) continue;
            try {
                SessionData data = JSON.parseObject(json, SessionData.class);
                if (data != null && data.loginTime < oldestTime) {
                    oldestTime = data.loginTime;
                    oldest = did;
                }
            } catch (Exception e) {
                log.warn("Corrupt session data for deviceId={} userId={}", did, userId);
            }
        }
        return oldest;
    }

    // ── internal ──

    private String sessionKey(Long userId) {
        return SESSION_KEY_PREFIX + userId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SessionData {
        private long loginTime;
        private String deviceName;
    }
}
