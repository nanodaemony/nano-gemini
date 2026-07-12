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
        repository.deleteByEntityTypeAndEntityId(entityType, entityId);
        if (aiFields != null && !aiFields.isEmpty()) {
            List<AiContentMarker> markers = aiFields.stream().map(field -> {
                AiContentMarker m = new AiContentMarker();
                m.setEntityType(entityType);
                m.setEntityId(entityId);
                m.setFieldName(field);
                m.setAiGenerated(1);
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
            repository.deleteByEntityTypeAndEntityId(
                    entry.getEntityType(), entry.getEntityId());
        }
        List<AiContentMarker> all = entries.stream()
                .filter(e -> e.getAiFields() != null && !e.getAiFields().isEmpty())
                .flatMap(e -> e.getAiFields().stream().map(field -> {
                    AiContentMarker m = new AiContentMarker();
                    m.setEntityType(e.getEntityType());
                    m.setEntityId(e.getEntityId());
                    m.setFieldName(field);
                    m.setAiGenerated(1);
                    return m;
                })).collect(Collectors.toList());
        if (!all.isEmpty()) {
            repository.saveAll(all);
        }
    }

    @Override
    public Map<String, List<String>> batchQuery(List<String> entityKeys) {
        if (entityKeys == null || entityKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AiContentMarker> markers = repository.findByEntityKeys(entityKeys);
        return markers.stream().collect(Collectors.groupingBy(
                m -> m.getEntityType() + ":" + m.getEntityId(),
                Collectors.mapping(AiContentMarker::getFieldName, Collectors.toList())
        ));
    }
}
