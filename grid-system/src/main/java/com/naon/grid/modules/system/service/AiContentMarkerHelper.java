package com.naon.grid.modules.system.service;

import java.util.*;

/**
 * AI 标记辅助工具 — 构建 entity_key 和收集实体 ID。
 */
public class AiContentMarkerHelper {

    private AiContentMarkerHelper() {}

    /** 构建 entity_key: "entity_type:entity_id" */
    public static String key(String entityType, Object entityId) {
        if (entityId == null) return null;
        return entityType + ":" + entityId;
    }

    /** 收集 entity_key 列表 */
    public static List<String> collect(String entityType, Collection<?> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) return Collections.emptyList();
        List<String> keys = new ArrayList<>();
        for (Object id : entityIds) {
            if (id != null) keys.add(key(entityType, id));
        }
        return keys;
    }

    /** 收集单个 entity_key */
    public static List<String> collectOne(String entityType, Object entityId) {
        String key = key(entityType, entityId);
        return key == null ? Collections.emptyList() : Collections.singletonList(key);
    }

    /** 合并多个列表 */
    @SafeVarargs
    public static List<String> merge(List<String>... lists) {
        List<String> result = new ArrayList<>();
        for (List<String> list : lists) {
            if (list != null) result.addAll(list);
        }
        return result;
    }
}
