package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.request.GrammarPointCreateRequest;
import com.naon.grid.backend.rest.request.GrammarPointQueryRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.rest.vo.GrammarPointBaseVO;
import com.naon.grid.backend.rest.vo.GrammarPointVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.grammar.dto.GrammarErrorDto;
import com.naon.grid.backend.service.grammar.dto.GrammarMeaningDto;
import com.naon.grid.backend.service.grammar.dto.GrammarNoticeDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointQueryCriteria;
import com.naon.grid.backend.service.grammar.dto.GrammarStructureDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GrammarPointWrapper {

    // ===== Request -> Criteria =====
    public static GrammarPointQueryCriteria toCriteria(GrammarPointQueryRequest request) {
        GrammarPointQueryCriteria criteria = new GrammarPointQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        criteria.setHskLevel(request.getHskLevel());
        criteria.setCategory(request.getCategory());
        return criteria;
    }

    // ===== Request -> Dto =====
    public static GrammarPointDto toDto(GrammarPointCreateRequest request) {
        GrammarPointDto dto = new GrammarPointDto();
        dto.setName(request.getName());
        dto.setHskLevel(request.getHskLevel());
        dto.setProject(request.getProject());
        dto.setCategory(request.getCategory());
        dto.setSubCategory(request.getSubCategory());
        dto.setMeanings(toMeaningDtoList(request.getMeanings()));
        dto.setStructures(toStructureDtoList(request.getStructures()));
        dto.setNotices(toNoticeDtoList(request.getNotices()));
        dto.setErrors(toErrorDtoList(request.getErrors()));
        dto.setQuestionIds(request.getQuestionIds());
        return dto;
    }

    // ===== Dto -> VO (detail) =====
    public static GrammarPointVO toVO(GrammarPointDto dto, Map<String, List<String>> aiMarkers) {
        if (dto == null) return null;
        GrammarPointVO vo = new GrammarPointVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setHskLevel(dto.getHskLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setSubCategory(dto.getSubCategory());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setMeanings(toMeaningVOList(dto.getMeanings(), aiMarkers));
        vo.setStructures(toStructureVOList(dto.getStructures(), aiMarkers));
        vo.setNotices(toNoticeVOList(dto.getNotices(), aiMarkers));
        vo.setErrors(toErrorVOList(dto.getErrors(), aiMarkers));
        vo.setQuestionIds(dto.getQuestionIds());
        return vo;
    }

    // ===== Dto List -> BaseVO List =====
    public static List<GrammarPointBaseVO> toBaseVOList(List<GrammarPointDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(GrammarPointWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static GrammarPointBaseVO toBaseVO(GrammarPointDto dto) {
        GrammarPointBaseVO vo = new GrammarPointBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setHskLevel(dto.getHskLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setSubCategory(dto.getSubCategory());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setMeaningCount(dto.getMeaningCount());
        vo.setStructureCount(dto.getStructureCount());
        vo.setNoticeCount(dto.getNoticeCount());
        vo.setErrorCount(dto.getErrorCount());
        vo.setQuestionIds(dto.getQuestionIds());
        return vo;
    }

    // ===== Sub DTO conversion methods =====
    private static List<GrammarMeaningDto> toMeaningDtoList(List<GrammarPointCreateRequest.GrammarMeaningRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toMeaningDto).collect(Collectors.toList());
    }

    private static GrammarMeaningDto toMeaningDto(GrammarPointCreateRequest.GrammarMeaningRequest request) {
        GrammarMeaningDto dto = new GrammarMeaningDto();
        dto.setId(request.getId());
        dto.setMeaningContent(request.getMeaningContent());
        dto.setMeaningContentTranslations(toTextTranslationList(request.getMeaningContentTranslations()));
        dto.setImageId(request.getImageId());
        dto.setSentences(toExampleSentenceDtoList(request.getSentences()));
        dto.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        return dto;
    }

    private static List<GrammarStructureDto> toStructureDtoList(List<GrammarPointCreateRequest.GrammarStructureRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toStructureDto).collect(Collectors.toList());
    }

    private static GrammarStructureDto toStructureDto(GrammarPointCreateRequest.GrammarStructureRequest request) {
        GrammarStructureDto dto = new GrammarStructureDto();
        dto.setId(request.getId());
        dto.setStructureContent(request.getStructureContent());
        dto.setSentences(toExampleSentenceDtoList(request.getSentences()));
        dto.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        return dto;
    }

    private static List<GrammarNoticeDto> toNoticeDtoList(List<GrammarPointCreateRequest.GrammarNoticeRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toNoticeDto).collect(Collectors.toList());
    }

    private static GrammarNoticeDto toNoticeDto(GrammarPointCreateRequest.GrammarNoticeRequest request) {
        GrammarNoticeDto dto = new GrammarNoticeDto();
        dto.setId(request.getId());
        dto.setNoticeContent(request.getNoticeContent());
        dto.setNoticeContentTranslations(toTextTranslationList(request.getNoticeContentTranslations()));
        dto.setSentences(toExampleSentenceDtoList(request.getSentences()));
        dto.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        return dto;
    }

    private static List<GrammarErrorDto> toErrorDtoList(List<GrammarPointCreateRequest.GrammarErrorRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toErrorDto).collect(Collectors.toList());
    }

    private static GrammarErrorDto toErrorDto(GrammarPointCreateRequest.GrammarErrorRequest request) {
        GrammarErrorDto dto = new GrammarErrorDto();
        dto.setId(request.getId());
        dto.setErrorContent(request.getErrorContent());
        dto.setErrorAnalysis(request.getErrorAnalysis());
        dto.setErrorAnalysisTranslations(toTextTranslationList(request.getErrorAnalysisTranslations()));
        dto.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        return dto;
    }

    // ===== Sub VO conversion methods =====
    private static List<GrammarPointVO.GrammarMeaningVO> toMeaningVOList(List<GrammarMeaningDto> dtos,
            Map<String, List<String>> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toMeaningVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static GrammarPointVO.GrammarMeaningVO toMeaningVO(GrammarMeaningDto dto,
            Map<String, List<String>> aiMarkers) {
        GrammarPointVO.GrammarMeaningVO vo = new GrammarPointVO.GrammarMeaningVO();
        vo.setId(dto.getId());
        vo.setGrammarId(dto.getGrammarId());
        vo.setMeaningContent(dto.getMeaningContent());
        vo.setMeaningContentTranslations(toTextTranslationVOList(dto.getMeaningContentTranslations()));
        vo.setImageId(dto.getImageId());
        vo.setSentences(toExampleSentenceVOList(dto.getSentences(), aiMarkers));
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        String key = AiContentMarkerHelper.key("grammar_meaning", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    private static List<GrammarPointVO.GrammarStructureVO> toStructureVOList(List<GrammarStructureDto> dtos,
            Map<String, List<String>> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toStructureVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static GrammarPointVO.GrammarStructureVO toStructureVO(GrammarStructureDto dto,
            Map<String, List<String>> aiMarkers) {
        GrammarPointVO.GrammarStructureVO vo = new GrammarPointVO.GrammarStructureVO();
        vo.setId(dto.getId());
        vo.setGrammarId(dto.getGrammarId());
        vo.setStructureContent(dto.getStructureContent());
        vo.setSentences(toExampleSentenceVOList(dto.getSentences(), aiMarkers));
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        String key = AiContentMarkerHelper.key("grammar_structure", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    private static List<GrammarPointVO.GrammarNoticeVO> toNoticeVOList(List<GrammarNoticeDto> dtos,
            Map<String, List<String>> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toNoticeVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static GrammarPointVO.GrammarNoticeVO toNoticeVO(GrammarNoticeDto dto,
            Map<String, List<String>> aiMarkers) {
        GrammarPointVO.GrammarNoticeVO vo = new GrammarPointVO.GrammarNoticeVO();
        vo.setId(dto.getId());
        vo.setGrammarId(dto.getGrammarId());
        vo.setNoticeContent(dto.getNoticeContent());
        vo.setNoticeContentTranslations(toTextTranslationVOList(dto.getNoticeContentTranslations()));
        vo.setSentences(toExampleSentenceVOList(dto.getSentences(), aiMarkers));
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        String key = AiContentMarkerHelper.key("grammar_notice", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    private static List<GrammarPointVO.GrammarErrorVO> toErrorVOList(List<GrammarErrorDto> dtos,
            Map<String, List<String>> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toErrorVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static GrammarPointVO.GrammarErrorVO toErrorVO(GrammarErrorDto dto,
            Map<String, List<String>> aiMarkers) {
        GrammarPointVO.GrammarErrorVO vo = new GrammarPointVO.GrammarErrorVO();
        vo.setId(dto.getId());
        vo.setGrammarId(dto.getGrammarId());
        vo.setErrorContent(dto.getErrorContent());
        vo.setErrorAnalysis(dto.getErrorAnalysis());
        vo.setErrorAnalysisTranslations(toTextTranslationVOList(dto.getErrorAnalysisTranslations()));
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        String key = AiContentMarkerHelper.key("grammar_error", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    // ===== Example Sentence conversion =====
    private static List<ExampleSentenceDto> toExampleSentenceDtoList(List<ExampleSentenceRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toExampleSentenceDto).collect(Collectors.toList());
    }

    private static ExampleSentenceDto toExampleSentenceDto(ExampleSentenceRequest request) {
        if (request == null) return null;
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(request.getId());
        dto.setSentence(request.getSentence());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setImageId(request.getImageId());
        dto.setOrder(request.getOrder());
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        return dto;
    }

    private static List<ExampleSentenceVO> toExampleSentenceVOList(List<ExampleSentenceDto> dtos,
            Map<String, List<String>> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toExampleSentenceVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static ExampleSentenceVO toExampleSentenceVO(ExampleSentenceDto dto,
            Map<String, List<String>> aiMarkers) {
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
        String key = AiContentMarkerHelper.key("example_sentence", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    // ===== TextTranslation conversion =====
    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toTextTranslation).collect(Collectors.toList());
    }

    private static TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) return null;
        TextTranslation translation = new TextTranslation();
        translation.setLanguage(request.getLanguage());
        translation.setTranslation(request.getTranslation());
        return translation;
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> translations) {
        if (translations == null) return Collections.emptyList();
        return translations.stream().map(GrammarPointWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation translation) {
        if (translation == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
