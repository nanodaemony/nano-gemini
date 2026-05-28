package com.naon.grid.backend.enums;

/**
 * 语言Code枚举
 */
public enum LanguageCodeEnum {

    // 中文区
    CHINESE(0, "cn", "中文"),
    ENGLISH(1, "en", "英语"),
    MALAYSIAN(2, "ms", "马来语"),

    // 东南亚
    VIETNAMESE(3, "vi", "越南语"),
    THAI(4, "th", "泰语"),
    INDONESIAN(5, "id", "印尼语"),
    FILIPINO(6, "fil", "菲律宾语"),

    // 东亚
    JAPANESE(7, "ja", "日语"),
    KOREAN(8, "ko", "韩语"),

    // 欧洲及其他地区
    ARABIC(9, "ar", "阿拉伯语"),
    FRENCH(10, "fr", "法语"),
    GERMAN(11, "de", "德语"),
    SPANISH(12, "es", "西班牙语"),
    PORTUGUESE(13, "pt", "葡萄牙语"),
    RUSSIAN(14, "ru", "俄语");

    ;

    private final Integer id;

    private final String code;

    private final String description;

    LanguageCodeEnum(Integer id, String code, String description) {
        this.code = code;
        this.id = id;
        this.description = description;
    }

}
