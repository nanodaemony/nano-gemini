package com.naon.grid.backend.service.common.impl;

import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExampleSentenceServiceImpl implements ExampleSentenceService {

    private final ExampleSentenceRepository exampleSentenceRepository;

    @Override
    public ExampleSentenceDto findById(Long id) {
        if (id == null) return null;
        return exampleSentenceRepository.findById(id)
                .filter(e -> StatusEnum.ENABLED.getCode().equals(e.getStatus()))
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public Map<Long, ExampleSentenceDto> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        List<ExampleSentence> all = exampleSentenceRepository.findAllById(ids);
        if (all == null || all.isEmpty()) return Collections.emptyMap();
        Map<Long, ExampleSentenceDto> result = new LinkedHashMap<>();
        for (ExampleSentence e : all) {
            if (StatusEnum.ENABLED.getCode().equals(e.getStatus())) {
                result.put(e.getId(), toDto(e));
            }
        }
        return result;
    }

    @Override
    public List<ExampleSentenceDto> findByStructureId(Long structureId) {
        if (structureId == null) return Collections.emptyList();
        List<ExampleSentence> sentences = exampleSentenceRepository
                .findByStructureIdAndStatus(structureId, StatusEnum.ENABLED.getCode());
        if (sentences == null || sentences.isEmpty()) return Collections.emptyList();
        sentences.sort(activeSentenceComparator());
        return sentences.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<ExampleSentenceDto>> findByStructureIds(Collection<Long> structureIds) {
        if (structureIds == null || structureIds.isEmpty()) return Collections.emptyMap();
        List<ExampleSentence> sentences = exampleSentenceRepository
                .findByStructureIdInAndStatus(structureIds, StatusEnum.ENABLED.getCode());
        if (sentences == null || sentences.isEmpty()) return Collections.emptyMap();
        Map<Long, List<ExampleSentenceDto>> result = new LinkedHashMap<>();
        for (ExampleSentence s : sentences) {
            result.computeIfAbsent(s.getStructureId(), k -> new ArrayList<>())
                  .add(toDto(s));
        }
        // Sort each structure's sentences by order descending
        for (List<ExampleSentenceDto> list : result.values()) {
            list.sort(Comparator.comparing(ExampleSentenceDto::getOrder,
                    Comparator.nullsLast(Comparator.reverseOrder())));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExampleSentenceDto save(ExampleSentenceDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getSentence())) {
            return null;
        }

        ExampleSentence entity;
        if (dto.getId() == null) {
            entity = new ExampleSentence();
        } else {
            entity = exampleSentenceRepository.findById(dto.getId())
                    .orElseThrow(() -> new BadRequestException("例句不存在: " + dto.getId()));
        }

        apply(entity, dto);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        entity = exampleSentenceRepository.save(entity);
        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableById(Long id) {
        if (id == null) return;
        exampleSentenceRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(StatusEnum.DISABLED.getCode());
            exampleSentenceRepository.save(entity);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<ExampleSentence> existing = exampleSentenceRepository.findAllById(ids);
        if (existing == null || existing.isEmpty()) return;
        for (ExampleSentence e : existing) {
            if (StatusEnum.ENABLED.getCode().equals(e.getStatus())) {
                e.setStatus(StatusEnum.DISABLED.getCode());
            }
        }
        exampleSentenceRepository.saveAll(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableByStructureId(Long structureId) {
        if (structureId == null) return;
        List<ExampleSentence> existing = exampleSentenceRepository
                .findByStructureIdAndStatus(structureId, StatusEnum.ENABLED.getCode());
        if (existing == null || existing.isEmpty()) return;
        for (ExampleSentence e : existing) {
            e.setStatus(StatusEnum.DISABLED.getCode());
        }
        exampleSentenceRepository.saveAll(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableByStructureIds(Collection<Long> structureIds) {
        if (structureIds == null || structureIds.isEmpty()) return;
        List<ExampleSentence> existing = exampleSentenceRepository
                .findByStructureIdInAndStatus(structureIds, StatusEnum.ENABLED.getCode());
        if (existing == null || existing.isEmpty()) return;
        for (ExampleSentence e : existing) {
            e.setStatus(StatusEnum.DISABLED.getCode());
        }
        exampleSentenceRepository.saveAll(existing);
    }

    private void apply(ExampleSentence entity, ExampleSentenceDto dto) {
        entity.setSentence(dto.getSentence());
        entity.setPinyin(dto.getPinyin());
        entity.setAudioId(dto.getAudioId());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setImageId(dto.getImageId());
        entity.setSentenceOrder(dto.getOrder() != null ? dto.getOrder() : 0);
        if (dto.getStructureId() != null) {
            entity.setStructureId(dto.getStructureId());
        }
    }

    private ExampleSentenceDto toDto(ExampleSentence entity) {
        if (entity == null) return null;
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(entity.getId());
        dto.setStructureId(entity.getStructureId());
        dto.setSentence(entity.getSentence());
        dto.setPinyin(entity.getPinyin());
        dto.setAudioId(entity.getAudioId());
        dto.setTranslations(JsonUtils.parseTranslationList(entity.getTranslations()));
        dto.setImageId(entity.getImageId());
        dto.setOrder(entity.getSentenceOrder());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private Comparator<ExampleSentence> activeSentenceComparator() {
        return Comparator.comparing(ExampleSentence::getSentenceOrder,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExampleSentence::getUpdateTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExampleSentence::getId,
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
