package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.CharRadicalQueryRequest;
import com.naon.grid.backend.rest.request.CharRadicalUpdateRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.CharRadicalBaseVO;
import com.naon.grid.backend.rest.vo.CharRadicalVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.backend.service.charradical.dto.CharRadicalQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CharRadicalWrapper {

    // === Request → Criteria ===

    public static CharRadicalQueryCriteria toCriteria(CharRadicalQueryRequest request) {
        if (request == null) return null;
        CharRadicalQueryCriteria criteria = new CharRadicalQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    // === Request → Dto ===

    public static CharRadicalDto toDto(CharRadicalUpdateRequest request) {
        if (request == null) return null;
        CharRadicalDto dto = new CharRadicalDto();
        dto.setStrokeNum(request.getStrokeNum());
        dto.setRelationId(request.getRelationId());
        dto.setEvolutionDesc(request.getEvolutionDesc());
        dto.setEvolutionDescTranslations(toTextTranslationList(request.getEvolutionDescTranslations()));
        dto.setEvolutionImageId(request.getEvolutionImageId());
        return dto;
    }

    // === Dto → VO ===

    public static CharRadicalVO toVO(CharRadicalDto dto) {
        if (dto == null) return null;
        CharRadicalVO vo = new CharRadicalVO();
        vo.setId(dto.getId());
        vo.setRadical(dto.getRadical());
        vo.setStrokeNum(dto.getStrokeNum());
        vo.setRelationId(dto.getRelationId());
        vo.setEvolutionDesc(dto.getEvolutionDesc());
        vo.setEvolutionDescTranslations(toTextTranslationVOList(dto.getEvolutionDescTranslations()));
        vo.setEvolutionImageId(dto.getEvolutionImageId());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static CharRadicalBaseVO toBaseVO(CharRadicalDto dto) {
        if (dto == null) return null;
        CharRadicalBaseVO vo = new CharRadicalBaseVO();
        vo.setId(dto.getId());
        vo.setRadical(dto.getRadical());
        vo.setStrokeNum(dto.getStrokeNum());
        vo.setRelationId(dto.getRelationId());
        vo.setEvolutionImageId(dto.getEvolutionImageId());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static List<CharRadicalBaseVO> toBaseVOList(List<CharRadicalDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(CharRadicalWrapper::toBaseVO).collect(Collectors.toList());
    }

    // === TextTranslation 转换工具 ===

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(CharRadicalWrapper::toTextTranslation).collect(Collectors.toList());
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
        return list.stream().map(CharRadicalWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation t) {
        if (t == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(t.getLanguage());
        vo.setTranslation(t.getTranslation());
        return vo;
    }
}
