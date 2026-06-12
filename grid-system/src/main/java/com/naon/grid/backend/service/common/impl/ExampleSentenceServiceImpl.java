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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExampleSentenceServiceImpl implements ExampleSentenceService {

    private final ExampleSentenceRepository exampleSentenceRepository;

    @Override
    public ExampleSentenceDto findOne(String bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            return null;
        }
        List<ExampleSentence> sentences = exampleSentenceRepository.findByBizTypeAndBizIdAndStatus(
                bizType, bizId, StatusEnum.ENABLED.getCode());
        if (sentences == null || sentences.isEmpty()) {
            return null;
        }
        sentences.sort(activeSentenceComparator());
        return toDto(sentences.get(0));
    }

    @Override
    public Map<Long, ExampleSentenceDto> findByBizIds(String bizType, Collection<Long> bizIds) {
        if (bizType == null || bizIds == null || bizIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ExampleSentence> sentences = exampleSentenceRepository.findByBizTypeAndBizIdInAndStatus(
                bizType, bizIds, StatusEnum.ENABLED.getCode());
        if (sentences == null || sentences.isEmpty()) {
            return Collections.emptyMap();
        }
        sentences.sort(activeSentenceComparator());
        Map<Long, ExampleSentenceDto> result = new LinkedHashMap<>();
        for (ExampleSentence sentence : sentences) {
            if (!result.containsKey(sentence.getBizId())) {
                result.put(sentence.getBizId(), toDto(sentence));
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExampleSentenceDto syncOne(String bizType, Long bizId, ExampleSentenceDto sentence) {
        if (bizType == null || bizId == null) {
            throw new BadRequestException("例句业务类型或业务ID不能为空");
        }
        if (sentence == null || StringUtils.isBlank(sentence.getSentence())) {
            disableExisting(bizType, bizId, null);
            return null;
        }

        ExampleSentence entity;
        if (sentence.getId() == null) {
            entity = new ExampleSentence();
            entity.setBizType(bizType);
            entity.setBizId(bizId);
        } else {
            entity = exampleSentenceRepository.findById(sentence.getId())
                    .orElseThrow(() -> new BadRequestException("例句不存在: " + sentence.getId()));
            if (!bizType.equals(entity.getBizType()) || !bizId.equals(entity.getBizId())
                    || StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
                throw new BadRequestException("例句ID不属于当前业务对象: " + sentence.getId());
            }
        }

        apply(entity, sentence);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        entity = exampleSentenceRepository.save(entity);
        disableExisting(bizType, bizId, entity.getId());
        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableByBizIds(String bizType, Collection<Long> bizIds) {
        if (bizType == null || bizIds == null || bizIds.isEmpty()) {
            return;
        }
        List<ExampleSentence> existing = exampleSentenceRepository.findByBizTypeAndBizIdInAndStatus(
                bizType, bizIds, StatusEnum.ENABLED.getCode());
        if (existing == null || existing.isEmpty()) {
            return;
        }
        for (ExampleSentence sentence : existing) {
            sentence.setStatus(StatusEnum.DISABLED.getCode());
            exampleSentenceRepository.save(sentence);
        }
    }

    private void disableExisting(String bizType, Long bizId, Long keepId) {
        List<ExampleSentence> existing = exampleSentenceRepository.findByBizTypeAndBizIdAndStatus(
                bizType, bizId, StatusEnum.ENABLED.getCode());
        if (existing == null || existing.isEmpty()) {
            return;
        }
        for (ExampleSentence sentence : existing) {
            if (keepId != null && keepId.equals(sentence.getId())) {
                continue;
            }
            sentence.setStatus(StatusEnum.DISABLED.getCode());
            exampleSentenceRepository.save(sentence);
        }
    }

    private void apply(ExampleSentence entity, ExampleSentenceDto dto) {
        entity.setSentence(dto.getSentence());
        entity.setPinyin(dto.getPinyin());
        entity.setAudioId(dto.getAudioId());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setImageId(dto.getImageId());
        entity.setSentenceOrder(dto.getOrder() != null ? dto.getOrder() : 0);
    }

    private ExampleSentenceDto toDto(ExampleSentence entity) {
        if (entity == null) {
            return null;
        }
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(entity.getId());
        dto.setBizType(entity.getBizType());
        dto.setBizId(entity.getBizId());
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