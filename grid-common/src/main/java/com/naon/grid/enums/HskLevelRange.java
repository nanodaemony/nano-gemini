package com.naon.grid.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 汉字大挑战 — 难度分层枚举
 * <p>
 * 将前端语义 key（elementary/intermediate/advanced）映射为 HSK 等级代码列表。
 */
public enum HskLevelRange {

    ELEMENTARY("elementary", Arrays.asList("1", "2")),
    INTERMEDIATE("intermediate", Arrays.asList("3", "4")),
    ADVANCED("advanced", Arrays.asList("5", "6"));

    private final String key;
    private final List<String> levels;

    HskLevelRange(String key, List<String> levels) {
        this.key = key;
        this.levels = levels;
    }

    public String getKey() {
        return key;
    }

    public List<String> getLevels() {
        return levels;
    }

    /**
     * 根据前端语义 key 获取对应的 HSK 等级代码列表。
     *
     * @param key 前端语义 key，如 "elementary"、"intermediate"、"advanced"
     * @return HSK 等级代码列表，如 ["1", "2"]
     * @throws IllegalArgumentException 如果 key 不合法
     */
    public static List<String> fromKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("level 参数不能为空，有效值: elementary, intermediate, advanced");
        }
        for (HskLevelRange r : values()) {
            if (r.key.equals(key.trim())) {
                return r.levels;
            }
        }
        throw new IllegalArgumentException("无效的 level: " + key + "，有效值: elementary, intermediate, advanced");
    }
}
