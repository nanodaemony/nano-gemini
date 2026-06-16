package com.naon.grid.enums;

import lombok.Getter;

/**
 * 会员级别。
 * VIP=1, SVIP=2
 * 级别数值越高，权限越大。SVIP.includes(VIP)=true
 */
@Getter
public enum UserLevel {
    VIP(1),
    SVIP(2);

    private final int level;

    UserLevel(int level) {
        this.level = level;
    }

    /**
     * 当前级别是否包含 other 级别的权限。
     * 例：SVIP.includes(VIP) → true
     */
    public boolean includes(UserLevel other) {
        return this.level >= other.level;
    }

    /**
     * 根据级别数值查找枚举。
     */
    public static UserLevel fromLevel(int level) {
        for (UserLevel item : values()) {
            if (item.getLevel() == level) {
                return item;
            }
        }
        return null;
    }

    /**
     * 根据角色编码查找枚举。
     */
    public static UserLevel fromRoleCode(String roleCode) {
        if (roleCode == null) {
            return null;
        }
        for (UserLevel item : values()) {
            if (item.name().equals(roleCode)) {
                return item;
            }
        }
        return null;
    }
}
