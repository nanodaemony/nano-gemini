package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.GrammarComparisonGroupCreateRequest;
import com.naon.grid.backend.rest.request.GrammarComparisonGroupQueryRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.GrammarComparisonChatVO;
import com.naon.grid.backend.rest.vo.GrammarComparisonGroupBaseVO;
import com.naon.grid.backend.rest.vo.GrammarComparisonGroupVO;
import com.naon.grid.backend.rest.vo.GrammarComparisonItemVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonChatDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupQueryCriteria;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonItemDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GrammarComparisonGroupWrapper {

    // ==================== Request → DTO / Criteria ====================

    public static GrammarComparisonGroupQueryCriteria toCriteria(GrammarComparisonGroupQueryRequest request) {
        GrammarComparisonGroupQueryCriteria criteria = new GrammarComparisonGroupQueryCriteria();
        criteria.setGrammarId(request.getGrammarId());
        criteria.setGrammarName(request.getGrammarName());
        criteria.setGroupKey(request.getGroupKey());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    public static GrammarComparisonGroupDto toDto(GrammarComparisonGroupCreateRequest request) {
        GrammarComparisonGroupDto dto = new GrammarComparisonGroupDto();
        dto.setGroupKey(request.getGroupKey());
        dto.setExerciseQuestionIds(request.getExerciseQuestionIds());
        dto.setGroupOrder(request.getGroupOrder());
        dto.setItems(toItemDtoList(request.getItems()));
        dto.setChats(toChatDtoList(request.getChats()));
        return dto;
    }

    private static List<GrammarComparisonItemDto> toItemDtoList(
            List<GrammarComparisonGroupCreateRequest.GrammarItemRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarComparisonGroupWrapper::toItemDto).collect(Collectors.toList());
    }

    private static GrammarComparisonItemDto toItemDto(GrammarComparisonGroupCreateRequest.GrammarItemRequest req) {
        GrammarComparisonItemDto dto = new GrammarComparisonItemDto();
        dto.setGrammarId(req.getGrammarId());
        dto.setGrammarName(req.getGrammarName());
        dto.setUsageComparison(req.getUsageComparison());
        dto.setUsageComparisonTranslations(toTextTranslationList(req.getUsageComparisonTranslations()));
        dto.setExampleSentences(req.getExampleSentences());
        dto.setUsageSentenceId(req.getUsageSentenceId());
        dto.setOrder(req.getOrder());
        dto.setAiGeneratedFields(req.getAiGeneratedFields());
        return dto;
    }

    private static List<GrammarComparisonChatDto> toChatDtoList(
            List<GrammarComparisonGroupCreateRequest.GrammarChatRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarComparisonGroupWrapper::toChatDto).collect(Collectors.toList());
    }

    private static GrammarComparisonChatDto toChatDto(GrammarComparisonGroupCreateRequest.GrammarChatRequest req) {
        GrammarComparisonChatDto dto = new GrammarComparisonChatDto();
        dto.setRole(req.getRole());
        dto.setContent(req.getContent());
        dto.setPinyin(req.getPinyin());
        dto.setTranslations(toTextTranslationList(req.getTranslations()));
        dto.setAudioId(req.getAudioId());
        dto.setOrder(req.getOrder());
        dto.setAiGeneratedFields(req.getAiGeneratedFields());
        return dto;
    }

    // ==================== DTO → VO ====================

    public static List<GrammarComparisonGroupBaseVO> toBaseVOList(List<GrammarComparisonGroupDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(GrammarComparisonGroupWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static GrammarComparisonGroupBaseVO toBaseVO(GrammarComparisonGroupDto dto) {
        GrammarComparisonGroupBaseVO vo = new GrammarComparisonGroupBaseVO();
        vo.setId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setGroupOrder(dto.getGroupOrder());
        vo.setItemCount(dto.getItemCount() != null ? dto.getItemCount() : 0);
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static GrammarComparisonGroupVO toVO(GrammarComparisonGroupDto dto,
            Map<String, List<String>> aiMarkers) {
        GrammarComparisonGroupVO vo = new GrammarComparisonGroupVO();
        vo.setId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setExerciseQuestionIds(dto.getExerciseQuestionIds());
        vo.setGroupOrder(dto.getGroupOrder());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setItems(toItemVOList(dto.getItems(), aiMarkers));
        vo.setChats(toChatVOList(dto.getChats(), aiMarkers));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<GrammarComparisonItemVO> toItemVOList(List<GrammarComparisonItemDto> dtos,
            Map<String, List<String>> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toItemVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static GrammarComparisonItemVO toItemVO(GrammarComparisonItemDto dto,
            Map<String, List<String>> aiMarkers) {
        GrammarComparisonItemVO vo = new GrammarComparisonItemVO();
        vo.setId(dto.getId());
        vo.setGrammarId(dto.getGrammarId());
        vo.setGrammarName(dto.getGrammarName());
        vo.setUsageComparison(dto.getUsageComparison());
        vo.setUsageComparisonTranslations(toTextTranslationVOList(dto.getUsageComparisonTranslations()));
        vo.setExampleSentences(dto.getExampleSentences());
        vo.setUsageSentenceId(dto.getUsageSentenceId());
        vo.setOrder(dto.getOrder());
        String key = AiContentMarkerHelper.key("grammar_comparison_item", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    private static List<GrammarComparisonChatVO> toChatVOList(List<GrammarComparisonChatDto> dtos,
            Map<String, List<String>> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toChatVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static GrammarComparisonChatVO toChatVO(GrammarComparisonChatDto dto,
            Map<String, List<String>> aiMarkers) {
        GrammarComparisonChatVO vo = new GrammarComparisonChatVO();
        vo.setId(dto.getId());
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setAudioId(dto.getAudioId());
        vo.setOrder(dto.getOrder());
        String key = AiContentMarkerHelper.key("grammar_comparison_chat", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    // ==================== TextTranslation 转换工具 ====================

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarComparisonGroupWrapper::toTextTranslation).collect(Collectors.toList());
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
        return list.stream().map(GrammarComparisonGroupWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation t) {
        if (t == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(t.getLanguage());
        vo.setTranslation(t.getTranslation());
        return vo;
    }
}
