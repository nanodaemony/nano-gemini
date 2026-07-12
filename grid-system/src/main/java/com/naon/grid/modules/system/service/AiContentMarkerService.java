package com.naon.grid.modules.system.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

public interface AiContentMarkerService {

    /**
     * 全量替换指定实体的 AI 标记字段。
     */
    void replaceFields(String entityType, Long entityId, List<String> aiFields);

    /**
     * 批量替换。
     */
    void batchReplace(List<MarkerEntry> entries);

    /**
     * 批量查询 AI 标记。
     * @param entityKeys 格式 ["vocab_sense:88", "example_sentence:42"]
     * @return key="entity_type:entity_id", value=标记字段（含 AI生成列表和已审核列表）
     */
    Map<String, MarkerFields> batchQuery(List<String> entityKeys);

    /**
     * 设置某个字段的审核状态。
     */
    void reviewField(String entityType, Long entityId, String fieldName, boolean reviewed);

    @Data
    @AllArgsConstructor
    class MarkerEntry {
        private String entityType;
        private Long entityId;
        private List<String> aiFields;
    }

    @Data
    @AllArgsConstructor
    class MarkerFields {
        /** AI生成的字段名列表 */
        private List<String> generated;
        /** 已人工审核的字段名列表（是 generated 的子集） */
        private List<String> reviewed;
    }
}
