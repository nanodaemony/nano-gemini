package com.naon.grid.utils;

import com.alibaba.fastjson2.JSON;
import com.naon.grid.domain.common.ExerciseOption;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JsonUtils {

    private JsonUtils() {
    }

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串，null 返回 null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj);
    }

    /**
     * 将 JSON 字符串反序列化为对象
     *
     * @param json JSON 字符串
     * @param clazz 对象类型
     * @return 对象，null 或空白字符串返回 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JSON.parseObject(json, clazz);
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
        // 过滤掉空对象
        List<TextTranslation> filteredList = list.stream()
                .filter(item -> !isEmptyTranslation(item))
                .collect(Collectors.toList());
        if (filteredList.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(filteredList);
    }

    /**
     * 将 JSON 字符串反序列化为 TextTranslation 列表
     *
     * @param json JSON 字符串
     * @return 翻译列表，null 或空白字符串返回空列表，会过滤掉空对象
     */
    public static List<TextTranslation> parseTranslationList(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        List<TextTranslation> list = JSON.parseArray(json, TextTranslation.class);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 过滤掉空对象（language和translation都为null或空字符串的）
        return list.stream()
                .filter(item -> !isEmptyTranslation(item))
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为空的翻译对象
     */
    private static boolean isEmptyTranslation(TextTranslation item) {
        if (item == null) {
            return true;
        }
        return StringUtils.isBlank(item.getLanguage()) && StringUtils.isBlank(item.getTranslation());
    }

    /**
     * 将字符串列表序列化为 JSON 字符串
     *
     * @param list 字符串列表
     * @return JSON 字符串，null 或空列表返回 null
     */
    public static String toStringListJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        // 过滤掉 null 和空白字符串
        List<String> filteredList = list.stream()
                .filter(item -> !StringUtils.isBlank(item))
                .collect(Collectors.toList());
        if (filteredList.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(filteredList);
    }

    /**
     * 将 JSON 字符串反序列化为字符串列表
     *
     * @param json JSON 字符串
     * @return 字符串列表，null 或空白字符串返回空列表，会过滤掉空白字符串
     */
    public static List<String> parseStringList(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        List<String> list = JSON.parseArray(json, String.class);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 过滤掉 null 和空白字符串
        return list.stream()
                .filter(item -> !StringUtils.isBlank(item))
                .collect(Collectors.toList());
    }

    /**
     * 将 ExerciseOption 列表序列化为 JSON 字符串
     *
     * @param list 选项列表
     * @return JSON 字符串，null 或空列表返回 null
     */
    public static String toExerciseOptionListJson(List<ExerciseOption> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        // 过滤掉空对象
        List<ExerciseOption> filteredList = list.stream()
                .filter(item -> !isEmptyExerciseOption(item))
                .collect(Collectors.toList());
        if (filteredList.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(filteredList);
    }

    /**
     * 将 JSON 字符串反序列化为 ExerciseOption 列表
     *
     * @param json JSON 字符串
     * @return 选项列表，null 或空白字符串返回空列表，会过滤掉空对象
     */
    public static List<ExerciseOption> parseExerciseOptionList(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        List<ExerciseOption> list = JSON.parseArray(json, ExerciseOption.class);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 过滤掉空对象
        return list.stream()
                .filter(item -> !isEmptyExerciseOption(item))
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为空的 ExerciseOption 对象
     */
    private static boolean isEmptyExerciseOption(ExerciseOption item) {
        if (item == null) {
            return true;
        }
        return StringUtils.isBlank(item.getOption()) && StringUtils.isBlank(item.getText());
    }
}
