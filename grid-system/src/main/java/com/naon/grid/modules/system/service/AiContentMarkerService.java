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
     * @return key="entity_type:entity_id", value=AI生成的字段名列表
     */
    Map<String, List<String>> batchQuery(List<String> entityKeys);

    @Data
    @AllArgsConstructor
    class MarkerEntry {
        private String entityType;
        private Long entityId;
        private List<String> aiFields;
    }
}
