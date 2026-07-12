package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.request.VocabComparisonGroupCreateRequest;
import com.naon.grid.backend.rest.request.VocabComparisonGroupQueryRequest;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.VocabComparisonChatVO;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupBaseVO;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupVO;
import com.naon.grid.backend.rest.vo.VocabComparisonItemVO;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonChatDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupQueryCriteria;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonItemDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VocabComparisonGroupWrapper {

    public static VocabComparisonGroupQueryCriteria toCriteria(VocabComparisonGroupQueryRequest request) {
        VocabComparisonGroupQueryCriteria criteria = new VocabComparisonGroupQueryCriteria();
        criteria.setWord(request.getWord());
        criteria.setWordId(request.getWordId());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    public static VocabComparisonGroupDto toDto(VocabComparisonGroupCreateRequest request) {
        VocabComparisonGroupDto dto = new VocabComparisonGroupDto();
        dto.setGroupKey(request.getGroupKey());
        dto.setExerciseQuestionIds(request.getExerciseQuestionIds());
        dto.setGroupOrder(request.getGroupOrder());
        dto.setItems(toItemDtoList(request.getItems()));
        dto.setChats(toChatDtoList(request.getChats()));
        return dto;
    }

    private static List<VocabComparisonItemDto> toItemDtoList(
            List<VocabComparisonGroupCreateRequest.VocabItemRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabComparisonGroupWrapper::toItemDto).collect(Collectors.toList());
    }

    private static VocabComparisonItemDto toItemDto(VocabComparisonGroupCreateRequest.VocabItemRequest req) {
        VocabComparisonItemDto dto = new VocabComparisonItemDto();
        dto.setWordId(req.getWordId());
        dto.setWord(req.getWord());
        dto.setPartOfSpeech(req.getPartOfSpeech());
        dto.setUsageComparison(req.getUsageComparison());
        dto.setUsageComparisonTranslations(toTextTranslationList(req.getUsageComparisonTranslations()));
        dto.setCommonUsage(req.getCommonUsage());
        dto.setCommonUsageTranslations(toTextTranslationList(req.getCommonUsageTranslations()));
        dto.setOrder(req.getOrder());
        dto.setAiGeneratedFields(req.getAiGeneratedFields());
        return dto;
    }

    private static List<VocabComparisonChatDto> toChatDtoList(
            List<VocabComparisonGroupCreateRequest.VocabChatRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabComparisonGroupWrapper::toChatDto).collect(Collectors.toList());
    }

    private static VocabComparisonChatDto toChatDto(VocabComparisonGroupCreateRequest.VocabChatRequest req) {
        VocabComparisonChatDto dto = new VocabComparisonChatDto();
        dto.setRole(req.getRole());
        dto.setContent(req.getContent());
        dto.setPinyin(req.getPinyin());
        dto.setTranslations(toTextTranslationList(req.getTranslations()));
        dto.setAudioId(req.getAudioId());
        dto.setOrder(req.getOrder());
        dto.setAiGeneratedFields(req.getAiGeneratedFields());
        return dto;
    }

    // === DTO → VO ===

    public static List<VocabComparisonGroupBaseVO> toBaseVOList(List<VocabComparisonGroupDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(VocabComparisonGroupWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static VocabComparisonGroupBaseVO toBaseVO(VocabComparisonGroupDto dto) {
        VocabComparisonGroupBaseVO vo = new VocabComparisonGroupBaseVO();
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

    public static VocabComparisonGroupVO toVO(VocabComparisonGroupDto dto, Map<String, List<String>> aiMarkers) {
        VocabComparisonGroupVO vo = new VocabComparisonGroupVO();
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

    private static List<VocabComparisonItemVO> toItemVOList(List<VocabComparisonItemDto> dtos,
            Map<String, List<String>> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toItemVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static VocabComparisonItemVO toItemVO(VocabComparisonItemDto dto,
            Map<String, List<String>> aiMarkers) {
        VocabComparisonItemVO vo = new VocabComparisonItemVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setWord(dto.getWord());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setUsageComparison(dto.getUsageComparison());
        vo.setUsageComparisonTranslations(toTextTranslationVOList(dto.getUsageComparisonTranslations()));
        vo.setCommonUsage(dto.getCommonUsage());
        vo.setCommonUsageTranslations(toTextTranslationVOList(dto.getCommonUsageTranslations()));
        vo.setOrder(dto.getOrder());
        String key = AiContentMarkerHelper.key("vocab_comparison_item", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    private static List<VocabComparisonChatVO> toChatVOList(List<VocabComparisonChatDto> dtos,
            Map<String, List<String>> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toChatVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static VocabComparisonChatVO toChatVO(VocabComparisonChatDto dto,
            Map<String, List<String>> aiMarkers) {
        VocabComparisonChatVO vo = new VocabComparisonChatVO();
        vo.setId(dto.getId());
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setAudioId(dto.getAudioId());
        vo.setOrder(dto.getOrder());
        String key = AiContentMarkerHelper.key("vocab_comparison_chat", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    // === TextTranslation 转换工具 ===

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabComparisonGroupWrapper::toTextTranslation).collect(Collectors.toList());
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
        return list.stream().map(VocabComparisonGroupWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation t) {
        if (t == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(t.getLanguage());
        vo.setTranslation(t.getTranslation());
        return vo;
    }
}
