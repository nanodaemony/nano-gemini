package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.request.TopicCreateRequest;
import com.naon.grid.backend.rest.request.TopicQueryRequest;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.TopicBaseVO;
import com.naon.grid.backend.rest.vo.TopicChatVO;
import com.naon.grid.backend.rest.vo.TopicPatternVO;
import com.naon.grid.backend.rest.vo.TopicVO;
import com.naon.grid.backend.service.topic.dto.TopicChatDto;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicPatternDto;
import com.naon.grid.backend.service.topic.dto.TopicQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;
import com.naon.grid.modules.system.service.AiContentMarkerService.MarkerFields;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopicWrapper {

    public static TopicQueryCriteria toCriteria(TopicQueryRequest request) {
        TopicQueryCriteria criteria = new TopicQueryCriteria();
        if (request != null) {
            criteria.setBlurry(request.getBlurry());
            criteria.setPublishStatus(request.getPublishStatus());
            criteria.setEditStatus(request.getEditStatus());
        }
        return criteria;
    }

    public static TopicDto toDto(TopicCreateRequest request) {
        TopicDto dto = new TopicDto();
        dto.setName(request.getName());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setCoverImageId(request.getCoverImageId());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setPatterns(toPatternDtoList(request.getPatterns()));
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        return dto;
    }

    private static List<TopicPatternDto> toPatternDtoList(
            List<TopicCreateRequest.TopicPatternRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(TopicWrapper::toPatternDto).collect(Collectors.toList());
    }

    private static TopicPatternDto toPatternDto(TopicCreateRequest.TopicPatternRequest req) {
        TopicPatternDto dto = new TopicPatternDto();
        dto.setPattern(req.getPattern());
        dto.setImageId(req.getImageId());
        dto.setOrder(req.getOrder());
        dto.setChats(toChatDtoList(req.getChats()));
        dto.setAiGeneratedFields(req.getAiGeneratedFields());
        return dto;
    }

    private static List<TopicChatDto> toChatDtoList(
            List<TopicCreateRequest.TopicChatRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(TopicWrapper::toChatDto).collect(Collectors.toList());
    }

    private static TopicChatDto toChatDto(TopicCreateRequest.TopicChatRequest req) {
        TopicChatDto dto = new TopicChatDto();
        dto.setRole(req.getRole());
        dto.setContent(req.getContent());
        dto.setPinyin(req.getPinyin());
        dto.setTranslations(toTextTranslationList(req.getTranslations()));
        dto.setAudioId(req.getAudioId());
        dto.setOrder(req.getOrder());
        dto.setAiGeneratedFields(req.getAiGeneratedFields());
        return dto;
    }

    // === DTO -> VO ===

    public static List<TopicBaseVO> toBaseVOList(List<TopicDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(TopicWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static TopicBaseVO toBaseVO(TopicDto dto) {
        TopicBaseVO vo = new TopicBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setPatternCount(dto.getPatternCount() != null ? dto.getPatternCount() : 0);
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static TopicVO toVO(TopicDto dto, Map<String, MarkerFields> aiMarkers) {
        TopicVO vo = new TopicVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setCoverImageId(dto.getCoverImageId());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setPatterns(toPatternVOList(dto.getPatterns(), aiMarkers));
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());

        vo.setAiGeneratedFields(dto.getAiGeneratedFields() != null ? dto.getAiGeneratedFields() : Collections.emptyList());
        vo.setAiReviewedFields(Collections.emptyList());

        String key = AiContentMarkerHelper.key("topic", dto.getId());
        if (key != null && aiMarkers != null && aiMarkers.containsKey(key)) {
            MarkerFields fields = aiMarkers.get(key);
            vo.setAiGeneratedFields(fields.getGenerated());
            vo.setAiReviewedFields(fields.getReviewed());
        }
        return vo;
    }

    private static List<TopicPatternVO> toPatternVOList(List<TopicPatternDto> dtos,
            Map<String, MarkerFields> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toPatternVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static TopicPatternVO toPatternVO(TopicPatternDto dto, Map<String, MarkerFields> aiMarkers) {
        TopicPatternVO vo = new TopicPatternVO();
        vo.setId(dto.getId());
        vo.setPattern(dto.getPattern());
        vo.setImageId(dto.getImageId());
        vo.setOrder(dto.getOrder());
        vo.setChats(toChatVOList(dto.getChats(), aiMarkers));

        vo.setAiGeneratedFields(dto.getAiGeneratedFields() != null ? dto.getAiGeneratedFields() : Collections.emptyList());
        vo.setAiReviewedFields(Collections.emptyList());

        String key = AiContentMarkerHelper.key("topic_pattern", dto.getId());
        if (key != null && aiMarkers != null && aiMarkers.containsKey(key)) {
            MarkerFields fields = aiMarkers.get(key);
            vo.setAiGeneratedFields(fields.getGenerated());
            vo.setAiReviewedFields(fields.getReviewed());
        }
        return vo;
    }

    private static List<TopicChatVO> toChatVOList(List<TopicChatDto> dtos,
            Map<String, MarkerFields> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toChatVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static TopicChatVO toChatVO(TopicChatDto dto, Map<String, MarkerFields> aiMarkers) {
        TopicChatVO vo = new TopicChatVO();
        vo.setId(dto.getId());
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setAudioId(dto.getAudioId());
        vo.setOrder(dto.getOrder());

        vo.setAiGeneratedFields(dto.getAiGeneratedFields() != null ? dto.getAiGeneratedFields() : Collections.emptyList());
        vo.setAiReviewedFields(Collections.emptyList());

        String key = AiContentMarkerHelper.key("topic_chat", dto.getId());
        if (key != null && aiMarkers != null && aiMarkers.containsKey(key)) {
            MarkerFields fields = aiMarkers.get(key);
            vo.setAiGeneratedFields(fields.getGenerated());
            vo.setAiReviewedFields(fields.getReviewed());
        }
        return vo;
    }

    // === TextTranslation helpers ===

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(TopicWrapper::toTextTranslation).collect(Collectors.toList());
    }

    private static TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) return null;
        TextTranslation t = new TextTranslation();
        t.setLanguage(request.getLanguage());
        t.setTranslation(request.getTranslation());
        return t;
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(TopicWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation t) {
        if (t == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(t.getLanguage());
        vo.setTranslation(t.getTranslation());
        return vo;
    }
}
