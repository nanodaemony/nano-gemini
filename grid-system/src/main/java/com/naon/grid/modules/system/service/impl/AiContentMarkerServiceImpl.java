package com.naon.grid.modules.system.service.impl;

import com.naon.grid.modules.system.domain.AiContentMarker;
import com.naon.grid.modules.system.repository.AiContentMarkerRepository;
import com.naon.grid.modules.system.service.AiContentMarkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiContentMarkerServiceImpl implements AiContentMarkerService {

    private final AiContentMarkerRepository repository;

    @Override
    @Transactional
    public void replaceFields(String entityType, Long entityId, List<String> aiFields) {
        // 保存旧记录的 reviewed 状态
        List<AiContentMarker> existing = repository.findByEntityTypeAndEntityId(entityType, entityId);
        Map<String, Integer> reviewedMap = existing.stream()
                .collect(Collectors.toMap(
                        AiContentMarker::getFieldName,
                        AiContentMarker::getReviewed,
                        (a, b) -> a));

        repository.deleteByEntityTypeAndEntityId(entityType, entityId);
        if (aiFields != null && !aiFields.isEmpty()) {
            List<AiContentMarker> markers = aiFields.stream().map(field -> {
                AiContentMarker m = new AiContentMarker();
                m.setEntityType(entityType);
                m.setEntityId(entityId);
                m.setFieldName(field);
                m.setAiGenerated(1);
                m.setReviewed(reviewedMap.getOrDefault(field, 0));
                return m;
            }).collect(Collectors.toList());
            repository.saveAll(markers);
        }
    }

    @Override
    @Transactional
    public void batchReplace(List<MarkerEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        for (MarkerEntry entry : entries) {
            replaceFields(entry.getEntityType(), entry.getEntityId(), entry.getAiFields());
        }
    }

    @Override
    public Map<String, MarkerFields> batchQuery(List<String> entityKeys) {
        if (entityKeys == null || entityKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AiContentMarker> markers = repository.findByEntityKeys(entityKeys);
        return markers.stream().collect(Collectors.groupingBy(
                m -> m.getEntityType() + ":" + m.getEntityId(),
                Collectors.collectingAndThen(Collectors.toList(), list -> {
                    List<String> generated = list.stream()
                            .filter(m -> m.getAiGenerated() != null && m.getAiGenerated() == 1)
                            .map(AiContentMarker::getFieldName)
                            .collect(Collectors.toList());
                    List<String> reviewed = list.stream()
                            .filter(m -> m.getAiGenerated() != null && m.getAiGenerated() == 1
                                    && m.getReviewed() != null && m.getReviewed() == 1)
                            .map(AiContentMarker::getFieldName)
                            .collect(Collectors.toList());
                    return new MarkerFields(generated, reviewed);
                })
        ));
    }

    @Override
    @Transactional
    public void reviewField(String entityType, Long entityId, String fieldName, boolean reviewed) {
        List<AiContentMarker> existing = repository.findByEntityTypeAndEntityId(entityType, entityId);
        Optional<AiContentMarker> marker = existing.stream()
                .filter(m -> m.getFieldName().equals(fieldName) && m.getAiGenerated() == 1)
                .findFirst();
        if (marker.isPresent()) {
            marker.get().setReviewed(reviewed ? 1 : 0);
            repository.save(marker.get());
        }
        // 如果字段不在表中（非AI生成），忽略
    }
}
