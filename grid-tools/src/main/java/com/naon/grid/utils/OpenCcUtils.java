package com.naon.grid.utils;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

/**
 * OpenCC 简繁转换工具类
 *
 * @author nano
 * @date 2026-07-11
 */
public class OpenCcUtils {

    private OpenCcUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 简体转繁体（s2t 标准模式）。
     * 输入为 null 或空白时返回 null。
     *
     * @param simplified 简体中文字符串
     * @return 繁体中文字符串，输入为空时返回 null
     */
    public static String toTraditional(String simplified) {
        if (simplified == null || simplified.trim().isEmpty()) {
            return null;
        }
        return ZhConverterUtil.toTraditional(simplified.trim());
    }
}
