package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyBaseVO;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyDetailVO;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyTodayVO;
import com.naon.grid.service.dto.AliOssStorageDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppDailyVocabularyWrapper {

    public static AppDailyVocabularyTodayVO toTodayVO(
            DailyVocabularyDto main, List<DailyVocabularyDto> backups,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppDailyVocabularyTodayVO vo = new AppDailyVocabularyTodayVO();
        vo.setMain(toDetailVO(main, audioMap, imageMap, language));
        if (backups != null) {
            vo.setBackups(backups.stream()
                    .map(d -> toDetailVO(d, audioMap, imageMap, language))
                    .collect(Collectors.toList()));
        } else {
            vo.setBackups(Collections.emptyList());
        }
        return vo;
    }

    public static AppDailyVocabularyDetailVO toDetailVO(
            DailyVocabularyDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppDailyVocabularyDetailVO vo = new AppDailyVocabularyDetailVO();
        vo.setId(dto.getId() != null ? dto.getId().longValue() : null);
        vo.setPhrase(dto.getPhrase());
        vo.setPhraseType(dto.getPhraseType());
        vo.setPinyin(dto.getPinyin());
        vo.setPhraseTranslation(filterByLanguage(dto.getPhraseTranslations(), language));
        vo.setPlainExplanation(dto.getPlainExplanation());
        vo.setExplanationTranslation(filterByLanguage(dto.getExplanationTranslations(), language));
        vo.setOriginStory(dto.getOriginStory());
        vo.setDisplayDate(dto.getDisplayDate());
        vo.setRelatedWordId(dto.getRelatedWordId());

        // 音频
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppDailyVocabularyDetailVO.AudioVO audioVO = new AppDailyVocabularyDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            }
        }

        // 配图
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                AppDailyVocabularyDetailVO.ImageVO imageVO = new AppDailyVocabularyDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setImage(imageVO);
            }
        }

        // 例句
        if (dto.getExampleSentence() != null) {
            vo.setExampleSentence(toExampleVO(dto.getExampleSentence(), audioMap, imageMap, language));
        }

        return vo;
    }

    public static List<AppDailyVocabularyBaseVO> toBaseVOList(
            List<DailyVocabularyDto> dtos, Map<Long, AliOssStorageDto> imageMap) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(d -> toBaseVO(d, imageMap)).collect(Collectors.toList());
    }

    public static AppDailyVocabularyBaseVO toBaseVO(
            DailyVocabularyDto dto, Map<Long, AliOssStorageDto> imageMap) {
        AppDailyVocabularyBaseVO vo = new AppDailyVocabularyBaseVO();
        vo.setId(dto.getId() != null ? dto.getId().longValue() : null);
        vo.setPhrase(dto.getPhrase());
        vo.setPhraseType(dto.getPhraseType());
        vo.setPinyin(dto.getPinyin());
        vo.setDisplayDate(dto.getDisplayDate());
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                vo.setImageUrl(imgDto.getFileUrl());
            }
        }
        return vo;
    }

    // ==================== 私有 ====================

    private static AppDailyVocabularyDetailVO.VocabExampleVO toExampleVO(
            ExampleSentenceDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppDailyVocabularyDetailVO.VocabExampleVO vo = new AppDailyVocabularyDetailVO.VocabExampleVO();
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));

        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppDailyVocabularyDetailVO.AudioVO audioVO = new AppDailyVocabularyDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            }
        }

        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                AppDailyVocabularyDetailVO.ImageVO imageVO = new AppDailyVocabularyDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setImage(imageVO);
            }
        }
        return vo;
    }

    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) return null;
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(t -> {
                    TextTranslationVO vo = new TextTranslationVO();
                    vo.setLanguage(t.getLanguage());
                    vo.setTranslation(t.getTranslation());
                    return vo;
                })
                .orElse(null);
    }
}
