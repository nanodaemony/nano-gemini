package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.CharCharacterCreateRequest;
import com.naon.grid.backend.rest.request.CharCharacterQueryRequest;
import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.CharCharacterBaseVO;
import com.naon.grid.backend.rest.vo.CharCharacterVO;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.backend.service.character.dto.CharComparisonDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.naon.grid.modules.system.service.AiContentMarkerHelper;

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

    public static CharCharacterDto toDto(CharCharacterCreateRequest request) {
        CharCharacterDto dto = new CharCharacterDto();
        dto.setCharacter(request.getCharacter());
        dto.setHskLevel(request.getHskLevel());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTraditional(request.getTraditional());
        dto.setRadicalId(request.getRadicalId());
        dto.setRadical(request.getRadical());
        dto.setComponentCombination(request.getComponentCombination());
        dto.setCharDesc(request.getCharDesc());
        dto.setDescTranslations(toTextTranslationList(request.getCharDescTranslations()));
        dto.setComparisons(toComparisonDtoList(request.getComparisons()));
        dto.setWords(toWordDtoList(request.getWords()));
        return dto;
    }

    private static List<CharComparisonDto> toComparisonDtoList(List<CharCharacterCreateRequest.CharComparisonRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(CharCharacterWrapper::toComparisonDto).collect(Collectors.toList());
    }

    private static CharComparisonDto toComparisonDto(CharCharacterCreateRequest.CharComparisonRequest request) {
        CharComparisonDto dto = new CharComparisonDto();
        dto.setId(request.getId());
        dto.setComparisonChar(request.getComparisonChar());
        dto.setComparisonPinyin(request.getComparisonPinyin());
        dto.setComparisonCharTranslations(toTextTranslationList(request.getComparisonCharTranslations()));
        dto.setComparisonDescTranslations(toTextTranslationList(request.getComparisonDescTranslations()));
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        dto.setOrder(request.getOrder());
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
        dto.setHskLevel(request.getHskLevel());
        dto.setPinyin(request.getPinyin());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setWordItemTranslations(toTextTranslationList(request.getWordItemTranslations()));
        dto.setWordItemSentence(toExampleSentenceDto(request.getSentenceContent()));
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        dto.setWordOrder(request.getOrder() != null ? request.getOrder() : 0);
        return dto;
    }

    private static ExampleSentenceDto toExampleSentenceDto(ExampleSentenceRequest request) {
        if (request == null) {
            return null;
        }
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(request.getId());
        dto.setSentence(request.getSentence());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setImageId(request.getImageId());
        dto.setOrder(request.getOrder());
        return dto;
    }

    public static List<CharCharacterBaseVO> toBaseVOList(List<CharCharacterDto> resources) {
        return resources.stream().map(CharCharacterWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static CharCharacterBaseVO toBaseVO(CharCharacterDto dto) {
        CharCharacterBaseVO vo = new CharCharacterBaseVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadicalId(dto.getRadicalId());
        vo.setRadical(dto.getRadical());
        vo.setComponentCombination(dto.getComponentCombination());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
        vo.setComparisonCount(dto.getComparisonCount());
        vo.setWordCount(dto.getWordCount());
        vo.setTranslationStatus(dto.getTranslationStatus());
        vo.setPinyinStatus(dto.getPinyinStatus());
        vo.setAudioStatus(dto.getAudioStatus());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static CharCharacterVO toVO(CharCharacterDto dto, Map<String, List<String>> aiMarkers) {
        CharCharacterVO vo = new CharCharacterVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadicalId(dto.getRadicalId());
        vo.setRadical(dto.getRadical());
        vo.setComponentCombination(dto.getComponentCombination());
        vo.setCharDesc(dto.getCharDesc());
        vo.setCharDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setComparisons(toComparisonVOList(dto.getComparisons(), aiMarkers));
        vo.setWords(toWordVOList(dto.getWords(), aiMarkers));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<CharCharacterVO.CharComparisonVO> toComparisonVOList(List<CharComparisonDto> resources,
            Map<String, List<String>> aiMarkers) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(dto -> toComparisonVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static CharCharacterVO.CharComparisonVO toComparisonVO(CharComparisonDto dto,
            Map<String, List<String>> aiMarkers) {
        CharCharacterVO.CharComparisonVO vo = new CharCharacterVO.CharComparisonVO();
        vo.setId(dto.getId());
        vo.setComparisonChar(dto.getComparisonChar());
        vo.setComparisonPinyin(dto.getComparisonPinyin());
        vo.setComparisonCharTranslations(toTextTranslationVOList(dto.getComparisonCharTranslations()));
        vo.setComparisonDescTranslations(toTextTranslationVOList(dto.getComparisonDescTranslations()));
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        String key = AiContentMarkerHelper.key("char_comparison", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    private static List<CharCharacterVO.CharWordVO> toWordVOList(List<CharWordDto> resources,
            Map<String, List<String>> aiMarkers) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(dto -> toWordVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static CharCharacterVO.CharWordVO toWordVO(CharWordDto dto,
            Map<String, List<String>> aiMarkers) {
        CharCharacterVO.CharWordVO vo = new CharCharacterVO.CharWordVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setWordItem(dto.getWordItem());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setWordItemTranslations(toTextTranslationVOList(dto.getWordItemTranslations()));
        vo.setWordItemSentence(toExampleSentenceVO(dto.getWordItemSentence(), aiMarkers));
        vo.setOrder(dto.getWordOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        String key = AiContentMarkerHelper.key("char_word", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
        return vo;
    }

    private static ExampleSentenceVO toExampleSentenceVO(ExampleSentenceDto dto,
            Map<String, List<String>> aiMarkers) {
        if (dto == null) {
            return null;
        }
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