package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.CultureCreateRequest;
import com.naon.grid.backend.rest.request.CultureQueryRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.CultureBaseVO;
import com.naon.grid.backend.rest.vo.CultureCreateVO;
import com.naon.grid.backend.rest.vo.CultureVO;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureKeywordDto;
import com.naon.grid.backend.service.culture.dto.CultureQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;
import com.alibaba.fastjson2.JSON;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CultureWrapper {

    public static CultureQueryCriteria toCriteria(CultureQueryRequest request) {
        CultureQueryCriteria criteria = new CultureQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        criteria.setLevel(request.getLevel());
        criteria.setProject(request.getProject());
        criteria.setCategory(request.getCategory());
        return criteria;
    }

    public static CultureDto toDto(CultureCreateRequest request) {
        CultureDto dto = new CultureDto();
        dto.setName(request.getName());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTranslations(serializeTranslations(request.getTranslations()));
        dto.setCoverImageId(request.getCoverImageId());
        dto.setLevel(request.getLevel());
        dto.setProject(request.getProject());
        dto.setCategory(request.getCategory());
        dto.setOneSentenceIntro(request.getOneSentenceIntro());
        dto.setOneSentenceIntroTranslations(serializeTranslations(request.getOneSentenceIntroTranslations()));
        dto.setOneSentenceIntroAudioId(request.getOneSentenceIntroAudioId());
        dto.setOneSentenceIntroImageId(request.getOneSentenceIntroImageId());
        dto.setDetailedIntro(request.getDetailedIntro());
        dto.setDetailedIntroTranslations(serializeTranslations(request.getDetailedIntroTranslations()));
        dto.setDetailedIntroAudioId(request.getDetailedIntroAudioId());
        dto.setDetailedIntroImageId(request.getDetailedIntroImageId());
        dto.setSentenceIds(request.getSentenceIds());
        dto.setQuestionIds(request.getQuestionIds());
        dto.setKeywords(toKeywordDtoList(request.getKeywords()));
        return dto;
    }

    private static String serializeTranslations(List<TextTranslationRequest> requests) {
        if (requests == null || requests.isEmpty()) return null;
        List<TextTranslation> list = requests.stream().map(r -> {
            TextTranslation t = new TextTranslation();
            t.setLanguage(r.getLanguage());
            t.setTranslation(r.getTranslation());
            return t;
        }).collect(Collectors.toList());
        return JSON.toJSONString(list);
    }

    private static List<CultureKeywordDto> toKeywordDtoList(
            List<CultureCreateRequest.CultureKeywordRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(r -> {
            CultureKeywordDto dto = new CultureKeywordDto();
            dto.setId(r.getId());
            dto.setKeyword(r.getKeyword());
            dto.setKeywordDescription(r.getKeywordDescription());
            dto.setKeywordTranslations(serializeTranslations(r.getKeywordTranslations()));
            dto.setKeywordDescriptionTranslations(serializeTranslations(r.getKeywordDescriptionTranslations()));
            dto.setAudioId(r.getAudioId());
            dto.setImageId(r.getImageId());
            dto.setOrder(r.getOrder() != null ? r.getOrder() : 0);
            return dto;
        }).collect(Collectors.toList());
    }

    public static List<CultureBaseVO> toBaseVOList(List<CultureDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(CultureWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static CultureBaseVO toBaseVO(CultureDto dto) {
        CultureBaseVO vo = new CultureBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setCoverImageId(dto.getCoverImageId());
        vo.setLevel(dto.getLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setKeywordCount(dto.getKeywordCount());
        vo.setSentenceCount(dto.getSentenceCount());
        vo.setQuestionCount(dto.getQuestionCount());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setTranslations(toTextTranslationVOList(parseTranslations(dto.getTranslations())));
        return vo;
    }

    public static CultureVO toVO(CultureDto dto) {
        CultureVO vo = new CultureVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTranslations(toTextTranslationVOList(parseTranslations(dto.getTranslations())));
        vo.setCoverImageId(dto.getCoverImageId());
        vo.setLevel(dto.getLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setOneSentenceIntro(dto.getOneSentenceIntro());
        vo.setOneSentenceIntroTranslations(toTextTranslationVOList(parseTranslations(dto.getOneSentenceIntroTranslations())));
        vo.setOneSentenceIntroAudioId(dto.getOneSentenceIntroAudioId());
        vo.setOneSentenceIntroImageId(dto.getOneSentenceIntroImageId());
        vo.setDetailedIntro(dto.getDetailedIntro());
        vo.setDetailedIntroTranslations(toTextTranslationVOList(parseTranslations(dto.getDetailedIntroTranslations())));
        vo.setDetailedIntroAudioId(dto.getDetailedIntroAudioId());
        vo.setDetailedIntroImageId(dto.getDetailedIntroImageId());
        vo.setSentenceIds(dto.getSentenceIds());
        vo.setQuestionIds(dto.getQuestionIds());
        vo.setKeywords(toKeywordVOList(dto.getKeywords()));
        vo.setSentences(toSentenceVOList(dto.getSentences()));
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<CultureVO.CultureKeywordVO> toKeywordVOList(List<CultureKeywordDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(CultureWrapper::toKeywordVO).collect(Collectors.toList());
    }

    private static CultureVO.CultureKeywordVO toKeywordVO(CultureKeywordDto dto) {
        CultureVO.CultureKeywordVO vo = new CultureVO.CultureKeywordVO();
        vo.setId(dto.getId());
        vo.setKeyword(dto.getKeyword());
        vo.setKeywordDescription(dto.getKeywordDescription());
        vo.setKeywordTranslations(toTextTranslationVOList(parseTranslations(dto.getKeywordTranslations())));
        vo.setKeywordDescriptionTranslations(toTextTranslationVOList(parseTranslations(dto.getKeywordDescriptionTranslations())));
        vo.setAudioId(dto.getAudioId());
        vo.setImageId(dto.getImageId());
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<ExampleSentenceVO> toSentenceVOList(List<ExampleSentenceDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(CultureWrapper::toSentenceVO).collect(Collectors.toList());
    }

    private static ExampleSentenceVO toSentenceVO(ExampleSentenceDto dto) {
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

    private static List<TextTranslation> parseTranslations(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        return JSON.parseArray(json, TextTranslation.class);
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> translations) {
        if (translations == null) return Collections.emptyList();
        return translations.stream().map(t -> {
            TextTranslationVO vo = new TextTranslationVO();
            vo.setLanguage(t.getLanguage());
            vo.setTranslation(t.getTranslation());
            return vo;
        }).collect(Collectors.toList());
    }
}
