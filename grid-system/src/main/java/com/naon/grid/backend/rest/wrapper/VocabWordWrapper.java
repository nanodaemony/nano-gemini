package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.request.VocabWordCreateRequest;
import com.naon.grid.backend.rest.request.VocabWordQueryRequest;
import com.naon.grid.backend.rest.vo.ExerciseOptionVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.VocabWordBaseVO;
import com.naon.grid.backend.rest.vo.VocabWordVO;
import com.naon.grid.backend.service.vocabulary.dto.*;
import com.naon.grid.domain.common.ExerciseOption;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 词汇包装器
 *
 * @author chenzeng
 * @version 0.0.1
 * @date 2026/6/6 11:04
 */
public class VocabWordWrapper {

    public static VocabWordQueryCriteria toCriteria(VocabWordQueryRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    public static VocabWordDto toDto(VocabWordCreateRequest request) {
        VocabWordDto dto = new VocabWordDto();
        dto.setWord(request.getWord());
        dto.setWordTraditional(request.getWordTraditional());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setHskLevel(request.getHskLevel());
        dto.setSenses(toSenseDtoList(request.getSenses()));
        return dto;
    }

    private static List<VocabSenseDto> toSenseDtoList(List<VocabWordCreateRequest.VocabSenseRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(VocabWordWrapper::toSenseDto).collect(Collectors.toList());
    }

    private static VocabSenseDto toSenseDto(VocabWordCreateRequest.VocabSenseRequest request) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(request.getId());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setChineseDef(request.getChineseDef());
        dto.setDefAudioId(request.getDefAudioId());
        dto.setDefImage(request.getDefImageId());
        dto.setTranslations(toTextTranslationList(request.getDefTranslations()));
        dto.setSenseOrder(request.getOrder() != null ? request.getOrder() : 0);
        dto.setStructures(toStructureDtoList(request.getStructures()));
        return dto;
    }

    private static List<VocabStructureDto> toStructureDtoList(List<VocabWordCreateRequest.VocabStructureRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(VocabWordWrapper::toStructureDto).collect(Collectors.toList());
    }

    private static VocabStructureDto toStructureDto(VocabWordCreateRequest.VocabStructureRequest request) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(request.getId());
        dto.setPattern(request.getPattern());
        dto.setPatternDef(request.getPatternDef());
        dto.setPatternDefTranslations(toTextTranslationList(request.getPatternDefTranslations()));
        dto.setStructureOrder(request.getOrder() != null ? request.getOrder() : 0);
        return dto;
    }

    public static List<VocabWordBaseVO> toBaseVOList(List<VocabWordDto> resources) {
        return resources.stream().map(VocabWordWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static VocabWordBaseVO toBaseVO(VocabWordDto dto) {
        VocabWordBaseVO vo = new VocabWordBaseVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static VocabWordVO toVO(VocabWordDto dto) {
        VocabWordVO vo = new VocabWordVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setSenses(toSenseVOList(dto.getSenses()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<VocabWordVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(VocabWordWrapper::toSenseVO).collect(Collectors.toList());
    }

    private static VocabWordVO.VocabSenseVO toSenseVO(VocabSenseDto dto) {
        VocabWordVO.VocabSenseVO vo = new VocabWordVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        vo.setDefAudioId(dto.getDefAudioId());
        vo.setDefImageId(dto.getDefImage());
        vo.setDefTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures()));
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<VocabWordVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(VocabWordWrapper::toStructureVO).collect(Collectors.toList());
    }

    private static VocabWordVO.VocabStructureVO toStructureVO(VocabStructureDto dto) {
        VocabWordVO.VocabStructureVO vo = new VocabWordVO.VocabStructureVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setSenseId(dto.getSenseId());
        vo.setPattern(dto.getPattern());
        vo.setPatternDef(dto.getPatternDef());
        vo.setPatternDefTranslations(toTextTranslationVOList(dto.getPatternDefTranslations()));
        vo.setOrder(dto.getStructureOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(VocabWordWrapper::toTextTranslation).collect(Collectors.toList());
    }

    private static TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) {
            return null;
        }
        TextTranslation translation = new TextTranslation();
        translation.setLanguage(request.getLanguage());
        translation.setTranslation(request.getTranslation());
        return translation;
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> translations) {
        if (translations == null) {
            return Collections.emptyList();
        }
        return translations.stream().map(VocabWordWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation translation) {
        if (translation == null) {
            return null;
        }
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }

}