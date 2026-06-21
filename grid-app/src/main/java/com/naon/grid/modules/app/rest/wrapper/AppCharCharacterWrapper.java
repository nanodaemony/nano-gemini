package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharComparisonDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterDetailVO;
import com.naon.grid.modules.app.rest.vo.AppExampleSentenceVO;
import com.naon.grid.service.dto.AliOssStorageDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户端汉字包装器
 */
public class AppCharCharacterWrapper {

    public static List<AppCharCharacterBaseVO> toBaseVOList(List<CharCharacterDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppCharCharacterWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static AppCharCharacterBaseVO toBaseVO(CharCharacterDto dto) {
        AppCharCharacterBaseVO vo = new AppCharCharacterBaseVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        return vo;
    }

    public static AppCharCharacterDetailVO toDetailVO(
            CharCharacterDto dto, AudioResourceDto audioDto,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        AppCharCharacterDetailVO vo = new AppCharCharacterDetailVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        if (audioDto != null) {
            AppCharCharacterDetailVO.AudioVO audioVO = new AppCharCharacterDetailVO.AudioVO();
            audioVO.setAudioUrl(audioDto.getFileUrl());
            vo.setAudio(audioVO);
        }
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setComponentCombination(dto.getComponentCombination());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslation(filterByLanguage(dto.getDescTranslations(), language));
        vo.setComparisons(toDiscriminationVOList(dto.getComparisons(), language));
        vo.setWords(toWordVOList(dto.getWords(), imageMap, language));
        return vo;
    }

    private static List<AppCharCharacterDetailVO.CharComparisonVO> toDiscriminationVOList(
            List<CharComparisonDto> dtos, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toDiscriminationVO(dto, language)).collect(Collectors.toList());
    }

    private static AppCharCharacterDetailVO.CharComparisonVO toDiscriminationVO(
            CharComparisonDto dto, String language) {
        AppCharCharacterDetailVO.CharComparisonVO vo = new AppCharCharacterDetailVO.CharComparisonVO();
        vo.setComparisonChar(dto.getComparisonChar());
        vo.setComparisonPinyin(dto.getComparisonPinyin());
        vo.setComparisonCharTranslation(filterByLanguage(dto.getComparisonCharTranslations(), language));
        vo.setComparisonDescTranslation(filterByLanguage(dto.getComparisonDescTranslations(), language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    private static List<AppCharCharacterDetailVO.CharWordVO> toWordVOList(
            List<CharWordDto> dtos, Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toWordVO(dto, imageMap, language)).collect(Collectors.toList());
    }

    private static AppCharCharacterDetailVO.CharWordVO toWordVO(
            CharWordDto dto, Map<Long, AliOssStorageDto> imageMap, String language) {
        AppCharCharacterDetailVO.CharWordVO vo = new AppCharCharacterDetailVO.CharWordVO();
        vo.setWordItem(dto.getWordItem());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setWordItemTranslation(filterByLanguage(dto.getWordItemTranslations(), language));
        ExampleSentenceDto sentenceDto = dto.getWordItemSentence();
        if (sentenceDto != null) {
            AppExampleSentenceVO exampleSentence = new AppExampleSentenceVO();
            exampleSentence.setSentence(sentenceDto.getSentence());
            exampleSentence.setPinyin(sentenceDto.getPinyin());
            exampleSentence.setTranslation(filterByLanguage(sentenceDto.getTranslations(), language));
            if (sentenceDto.getImageId() != null && imageMap != null) {
                AliOssStorageDto ossDto = imageMap.get(sentenceDto.getImageId());
                if (ossDto != null) {
                    AppCharCharacterDetailVO.ImageVO imageVO = new AppCharCharacterDetailVO.ImageVO();
                    imageVO.setImageUrl(ossDto.getFileUrl());
                    exampleSentence.setImage(imageVO);
                }
            }
        }
        return vo;
    }

    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) {
            return null;
        }
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(AppCharCharacterWrapper::toTextTranslationVO)
                .orElse(null);
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
