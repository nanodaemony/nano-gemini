package com.naon.grid.utils;

import com.alibaba.fastjson2.JSON;
import com.naon.grid.domain.common.TextTranslation;

import java.util.List;

public class JsonUtils {

    private JsonUtils() {
    }

    /**
     * 将 TextTranslation 列表序列化为 JSON 字符串
     *
     * @param list 翻译列表
     * @return JSON 字符串，null 或空列表返回 null
     */
    public static String toTranslationJson(List<TextTranslation> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(list);
    }

    /**
     * 将 JSON 字符串反序列化为 TextTranslation 列表
     *
     * @param json JSON 字符串
     * @return 翻译列表，null 或空白字符串返回 null
     */
    public static List<TextTranslation> parseTranslationList(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JSON.parseArray(json, TextTranslation.class);
    }
}
