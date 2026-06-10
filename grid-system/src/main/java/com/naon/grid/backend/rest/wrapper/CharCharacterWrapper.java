package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.CharCharacterCreateRequest;
import com.naon.grid.backend.rest.request.CharCharacterQueryRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.CharCharacterBaseVO;
import com.naon.grid.backend.rest.vo.CharCharacterVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.backend.service.character.dto.CharDiscriminationDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 汉字包装器
 *
 * @author chenzeng
 * @version 0.0.1
 * @date 2026/6/6 11:04
 */
public class CharCharacterWrapper {

    public static CharCharacterQueryCriteria toCriteria(CharCharacterQueryRequest request) {
        CharCharacterQueryCriteria criteria = new CharCharacterQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    public static  CharCharacterDto toDto(CharCharacterCreateRequest request) {
        CharCharacterDto dto = new CharCharacterDto();
        dto.setCharacter(request.getCharacter());
        dto.setLevel(request.getLevel());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTraditional(request.getTraditional());
        dto.setRadical(request.getRadical());
        dto.setStroke(request.getStroke());
        dto.setCharDesc(request.getCharDesc());
        dto.setDescTranslations(toTextTranslationList(request.getCharDescTranslations()));
        dto.setDiscriminations(toDiscriminationDtoList(request.getComparisons()));
        dto.setWords(toWordDtoList(request.getWords()));
        return dto;
    }

    private static List<CharDiscriminationDto> toDiscriminationDtoList(List<CharCharacterCreateRequest.CharComparisonRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(CharCharacterWrapper::toDiscriminationDto).collect(Collectors.toList());
    }

    private static CharDiscriminationDto toDiscriminationDto(CharCharacterCreateRequest.CharComparisonRequest request) {
        CharDiscriminationDto dto = new CharDiscriminationDto();
        dto.setDiscrimChar(request.getComparisonChar());
        dto.setDiscrimPinyin(request.getComparisonPinyin());
        dto.setDiscrimCharTranslations(toTextTranslationList(request.getComparisonCharTranslations()));
        dto.setComparisonTranslations(toTextTranslationList(request.getComparisonDescTranslations()));
        dto.setDiscriminationOrder(request.getOrder());
        return dto;
    }

    private static List<CharWordDto> toWordDtoList(List<CharCharacterCreateRequest.CharWordRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(CharCharacterWrapper::toWordDto).collect(Collectors.toList());
    }

    private static CharWordDto toWordDto(CharCharacterCreateRequest.CharWordRequest request) {
        CharWordDto dto = new CharWordDto();
        dto.setId(request.getId());
        dto.setWordItem(request.getWordItem());
        dto.setLevel(request.getLevel());
        dto.setPinyin(request.getPinyin());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setWordItemTranslations(toTextTranslationList(request.getWordItemTranslations()));
        dto.setExampleSentence(request.getExampleSentence());
        dto.setExamplePinyin(request.getExamplePinyin());
        dto.setExampleTranslations(toTextTranslationList(request.getExampleTranslations()));
        dto.setExampleImage(request.getExampleImage());
        dto.setWordOrder(request.getOrder() != null ? request.getOrder() : 0);
        return dto;
    }

    public static List<CharCharacterBaseVO> toBaseVOList(List<CharCharacterDto> resources) {
        return resources.stream().map(CharCharacterWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static CharCharacterBaseVO toBaseVO(CharCharacterDto dto) {
        CharCharacterBaseVO vo = new CharCharacterBaseVO();
        vo.setId(dto.getId());
        vo.setSequenceNo(dto.getSequenceNo());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setStroke(dto.getStroke());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static CharCharacterVO toVO(CharCharacterDto dto) {
        CharCharacterVO vo = new CharCharacterVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setStroke(dto.getStroke());
        vo.setCharDesc(dto.getCharDesc());
        vo.setCharDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setComparisons(toDiscriminationVOList(dto.getDiscriminations()));
        vo.setWords(toWordVOList(dto.getWords()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<CharCharacterVO.CharComparisonVO> toDiscriminationVOList(List<CharDiscriminationDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(CharCharacterWrapper::toDiscriminationVO).collect(Collectors.toList());
    }

    private static CharCharacterVO.CharComparisonVO toDiscriminationVO(CharDiscriminationDto dto) {
        CharCharacterVO.CharComparisonVO vo = new CharCharacterVO.CharComparisonVO();
        vo.setCharComparisonId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setComparisonChar(dto.getDiscrimChar());
        vo.setComparisonPinyin(dto.getDiscrimPinyin());
        vo.setComparisonCharTranslations(toTextTranslationVOList(dto.getDiscrimCharTranslations()));
        vo.setComparisonDescTranslations(toTextTranslationVOList(dto.getComparisonTranslations()));
        vo.setOrder(dto.getDiscriminationOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<CharCharacterVO.CharWordVO> toWordVOList(List<CharWordDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(CharCharacterWrapper::toWordVO).collect(Collectors.toList());
    }

    private static CharCharacterVO.CharWordVO toWordVO(CharWordDto dto) {
        CharCharacterVO.CharWordVO vo = new CharCharacterVO.CharWordVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setWordItem(dto.getWordItem());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setWordItemTranslations(toTextTranslationVOList(dto.getWordItemTranslations()));
        vo.setExampleSentence(dto.getExampleSentence());
        vo.setExamplePinyin(dto.getExamplePinyin());
        vo.setExampleTranslations(toTextTranslationVOList(dto.getExampleTranslations()));
        vo.setExampleImage(dto.getExampleImage());
        vo.setOrder(dto.getWordOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(CharCharacterWrapper::toTextTranslation).collect(Collectors.toList());
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
        return translations.stream().map(CharCharacterWrapper::toTextTranslationVO).collect(Collectors.toList());
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