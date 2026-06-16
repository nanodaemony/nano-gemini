package com.naon.grid.enums;

/**
 * 会员级别。
 * VIP=1, SVIP=2
 * 级别数值越高，权限越大。SVIP.includes(VIP)=true
 */
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
}
