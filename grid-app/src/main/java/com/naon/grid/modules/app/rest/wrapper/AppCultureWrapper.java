package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureKeywordDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppCultureBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCultureDetailVO;
import com.naon.grid.modules.app.rest.vo.AppExampleSentenceVO;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AppCultureWrapper {

    public static List<AppCultureBaseVO> toBaseVOList(List<CultureDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(AppCultureWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static AppCultureBaseVO toBaseVO(CultureDto dto) {
        AppCultureBaseVO vo = new AppCultureBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setLevel(dto.getLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        return vo;
    }

    public static AppCultureDetailVO toDetailVO(
            CultureDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {

        AppCultureDetailVO vo = new AppCultureDetailVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(parseTranslations(dto.getTranslations()), language));
        vo.setLevel(dto.getLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());

        // 封面图
        if (dto.getCoverImageId() != null && imageMap != null) {
            AliOssStorageDto img = imageMap.get(dto.getCoverImageId());
            if (img != null) {
                AppCultureDetailVO.ImageVO imageVO = new AppCultureDetailVO.ImageVO();
                imageVO.setImageUrl(img.getFileUrl());
                vo.setCoverImage(imageVO);
            }
        }

        // 音频
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audio = audioMap.get(dto.getAudioId());
            if (audio != null) {
                AppCultureDetailVO.AudioVO audioVO = new AppCultureDetailVO.AudioVO();
                audioVO.setAudioUrl(audio.getFileUrl());
                vo.setAudio(audioVO);
            }
        }

        // 一句话介绍
        vo.setOneSentenceIntro(dto.getOneSentenceIntro());
        vo.setOneSentenceIntroTranslation(filterByLanguage(parseTranslations(dto.getOneSentenceIntroTranslations()), language));
        setAudio(vo::setOneSentenceIntroAudio, dto.getOneSentenceIntroAudioId(), audioMap);
        setImage(vo::setOneSentenceIntroImage, dto.getOneSentenceIntroImageId(), imageMap);

        // 详细介绍
        vo.setDetailedIntro(dto.getDetailedIntro());
        vo.setDetailedIntroTranslation(filterByLanguage(parseTranslations(dto.getDetailedIntroTranslations()), language));
        setAudio(vo::setDetailedIntroAudio, dto.getDetailedIntroAudioId(), audioMap);
        setImage(vo::setDetailedIntroImage, dto.getDetailedIntroImageId(), imageMap);

        // 关键词
        vo.setKeywords(toKeywordVOList(dto.getKeywords(), audioMap, imageMap, language));

        // 学一学例句
        vo.setSentences(toSentenceVOList(dto.getSentences(), audioMap, imageMap, language));

        // 练一练习题 — passed through as-is from ExerciseQuestionService (pre-resolved in controller)

        return vo;
    }

    // --- Keywords ---

    private static List<AppCultureDetailVO.CultureKeywordVO> toKeywordVOList(
            List<CultureKeywordDto> dtos,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getOrder() != null ? b.getOrder() : 0,
                        a.getOrder() != null ? a.getOrder() : 0))
                .map(dto -> toKeywordVO(dto, audioMap, imageMap, language))
                .collect(Collectors.toList());
    }

    private static AppCultureDetailVO.CultureKeywordVO toKeywordVO(
            CultureKeywordDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppCultureDetailVO.CultureKeywordVO vo = new AppCultureDetailVO.CultureKeywordVO();
        vo.setKeyword(dto.getKeyword());
        vo.setKeywordDescription(dto.getKeywordDescription());
        vo.setTranslation(filterByLanguage(parseTranslations(dto.getKeywordTranslations()), language));
        vo.setDescriptionTranslation(filterByLanguage(parseTranslations(dto.getKeywordDescriptionTranslations()), language));
        vo.setOrder(dto.getOrder());

        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audio = audioMap.get(dto.getAudioId());
            if (audio != null) {
                AppCultureDetailVO.AudioVO audioVO = new AppCultureDetailVO.AudioVO();
                audioVO.setAudioUrl(audio.getFileUrl());
                vo.setAudio(audioVO);
            }
        }
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto img = imageMap.get(dto.getImageId());
            if (img != null) {
                AppCultureDetailVO.ImageVO imageVO = new AppCultureDetailVO.ImageVO();
                imageVO.setImageUrl(img.getFileUrl());
                vo.setImage(imageVO);
            }
        }
        return vo;
    }

    // --- Example Sentences ---

    private static List<AppExampleSentenceVO> toSentenceVOList(
            List<ExampleSentenceDto> dtos,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toSentenceVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppExampleSentenceVO toSentenceVO(
            ExampleSentenceDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppExampleSentenceVO vo = new AppExampleSentenceVO();
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));
        vo.setOrder(dto.getOrder());

        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audio = audioMap.get(dto.getAudioId());
            if (audio != null) {
                AppCultureDetailVO.AudioVO audioVO = new AppCultureDetailVO.AudioVO();
                audioVO.setAudioUrl(audio.getFileUrl());
                vo.setAudio(audioVO);
            }
        }
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto img = imageMap.get(dto.getImageId());
            if (img != null) {
                AppCultureDetailVO.ImageVO imageVO = new AppCultureDetailVO.ImageVO();
                imageVO.setImageUrl(img.getFileUrl());
                vo.setImage(imageVO);
            }
        }
        return vo;
    }

    // --- Utilities ---

    @FunctionalInterface
    private interface AudioSetter {
        void set(AppCultureDetailVO.AudioVO audio);
    }

    @FunctionalInterface
    private interface ImageSetter {
        void set(AppCultureDetailVO.ImageVO image);
    }

    private static void setAudio(AudioSetter setter, Long audioId, Map<Long, AudioResourceDto> audioMap) {
        if (audioId != null && audioMap != null) {
            AudioResourceDto audio = audioMap.get(audioId);
            if (audio != null) {
                AppCultureDetailVO.AudioVO audioVO = new AppCultureDetailVO.AudioVO();
                audioVO.setAudioUrl(audio.getFileUrl());
                setter.set(audioVO);
            } else {
                log.error("音频资源未找到, audioId={}", audioId);
            }
        }
    }

    private static void setImage(ImageSetter setter, Long imageId, Map<Long, AliOssStorageDto> imageMap) {
        if (imageId != null && imageMap != null) {
            AliOssStorageDto img = imageMap.get(imageId);
            if (img != null) {
                AppCultureDetailVO.ImageVO imageVO = new AppCultureDetailVO.ImageVO();
                imageVO.setImageUrl(img.getFileUrl());
                setter.set(imageVO);
            } else {
                log.error("图片资源未找到, imageId={}", imageId);
            }
        }
    }

    private static List<TextTranslation> parseTranslations(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        return JSON.parseArray(json, TextTranslation.class);
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
