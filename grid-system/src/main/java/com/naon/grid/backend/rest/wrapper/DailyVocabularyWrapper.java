package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.DailyVocabularyCreateRequest;
import com.naon.grid.backend.rest.request.DailyVocabularyQueryRequest;
import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.DailyVocabularyBaseVO;
import com.naon.grid.backend.rest.vo.DailyVocabularyVO;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;
import com.naon.grid.modules.system.service.AiContentMarkerService.MarkerFields;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DailyVocabularyWrapper {

    public static DailyVocabularyQueryCriteria toCriteria(DailyVocabularyQueryRequest request) {
        DailyVocabularyQueryCriteria criteria = new DailyVocabularyQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPhraseType(request.getPhraseType());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setDisplayDateStart(request.getDisplayDateStart());
        criteria.setDisplayDateEnd(request.getDisplayDateEnd());
        return criteria;
    }

    public static DailyVocabularyDto toDto(DailyVocabularyCreateRequest request) {
        DailyVocabularyDto dto = new DailyVocabularyDto();
        dto.setPhrase(request.getPhrase());
        dto.setPhraseType(request.getPhraseType());
        dto.setPinyin(request.getPinyin());
        dto.setPhraseTranslations(toTextTranslationList(request.getPhraseTranslations()));
        dto.setAudioId(request.getAudioId());
        dto.setImageId(request.getImageId());
        dto.setPlainExplanation(request.getPlainExplanation());
        dto.setExplanationTranslations(toTextTranslationList(request.getExplanationTranslations()));
        dto.setOriginStory(request.getOriginStory());
        dto.setExampleSentenceId(request.getExampleSentenceId());
        dto.setDisplayDate(request.getDisplayDate());
        dto.setOrder(request.getOrder());
        dto.setRelatedWordId(request.getRelatedWordId());
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        return dto;
    }

    public static List<DailyVocabularyBaseVO> toBaseVOList(List<DailyVocabularyDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(DailyVocabularyWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static DailyVocabularyBaseVO toBaseVO(DailyVocabularyDto dto) {
        DailyVocabularyBaseVO vo = new DailyVocabularyBaseVO();
        vo.setId(dto.getId());
        vo.setPhrase(dto.getPhrase());
        vo.setPhraseType(dto.getPhraseType());
        vo.setPinyin(dto.getPinyin());
        vo.setDisplayDate(dto.getDisplayDate());
        vo.setOrder(dto.getOrder());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static DailyVocabularyVO toVO(DailyVocabularyDto dto, Map<String, MarkerFields> aiMarkers) {
        DailyVocabularyVO vo = new DailyVocabularyVO();
        vo.setId(dto.getId());
        vo.setPhrase(dto.getPhrase());
        vo.setPhraseType(dto.getPhraseType());
        vo.setPinyin(dto.getPinyin());
        vo.setPhraseTranslations(toTextTranslationVOList(dto.getPhraseTranslations()));
        vo.setAudioId(dto.getAudioId());
        vo.setImageId(dto.getImageId());
        vo.setPlainExplanation(dto.getPlainExplanation());
        vo.setExplanationTranslations(toTextTranslationVOList(dto.getExplanationTranslations()));
        vo.setOriginStory(dto.getOriginStory());
        if (dto.getExampleSentence() != null) {
            vo.setExampleSentence(toExampleSentenceVO(dto.getExampleSentence()));
        }
        vo.setDisplayDate(dto.getDisplayDate());
        vo.setOrder(dto.getOrder());
        vo.setRelatedWordId(dto.getRelatedWordId());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        // Default from DTO (draft entities have no DB IDs, use draft_content data)
        vo.setAiGeneratedFields(dto.getAiGeneratedFields() != null ? dto.getAiGeneratedFields() : Collections.emptyList());
        vo.setAiReviewedFields(Collections.emptyList());

        // Override with authoritative ai_content_marker data for published entities
        String key = AiContentMarkerHelper.key("daily_vocabulary", dto.getId());
        if (key != null && aiMarkers != null && aiMarkers.containsKey(key)) {
            MarkerFields fields = aiMarkers.get(key);
            vo.setAiGeneratedFields(fields.getGenerated());
            vo.setAiReviewedFields(fields.getReviewed());
        }
        return vo;
    }

    // ==================== 私有工具方法 ====================

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(r -> {
            TextTranslation t = new TextTranslation();
            t.setLanguage(r.getLanguage());
            t.setTranslation(r.getTranslation());
            return t;
        }).collect(Collectors.toList());
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(t -> {
            TextTranslationVO vo = new TextTranslationVO();
            vo.setLanguage(t.getLanguage());
            vo.setTranslation(t.getTranslation());
            return vo;
        }).collect(Collectors.toList());
    }

    private static ExampleSentenceVO toExampleSentenceVO(ExampleSentenceDto dto) {
        if (dto == null) return null;
        ExampleSentenceVO vo = new ExampleSentenceVO();
        vo.setId(dto.getId());
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setImageId(dto.getImageId());
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
}
