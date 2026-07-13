package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.topic.dto.TopicChatDto;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicPatternDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppTopicBaseVO;
import com.naon.grid.modules.app.rest.vo.AppTopicChatVO;
import com.naon.grid.modules.app.rest.vo.AppTopicDetailVO;
import com.naon.grid.modules.app.rest.vo.AppTopicPatternVO;
import com.naon.grid.service.dto.AliOssStorageDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppTopicWrapper {

    public static List<AppTopicBaseVO> toBaseVOList(List<TopicDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppTopicWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static AppTopicBaseVO toBaseVO(TopicDto dto) {
        AppTopicBaseVO vo = new AppTopicBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setPatternCount(dto.getPatternCount());
        return vo;
    }

    public static AppTopicDetailVO toDetailVO(
            TopicDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppTopicDetailVO vo = new AppTopicDetailVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));

        // Cover image
        if (dto.getCoverImageId() != null && imageMap != null) {
            AliOssStorageDto ossDto = imageMap.get(dto.getCoverImageId());
            if (ossDto != null) {
                AppTopicBaseVO.ImageVO imageVO = new AppTopicBaseVO.ImageVO();
                imageVO.setImageUrl(ossDto.getFileUrl());
                vo.setCoverImage(imageVO);
            }
        }

        // Audio
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppTopicBaseVO.AudioVO audioVO = new AppTopicBaseVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            }
        }

        // Patterns
        vo.setPatterns(toPatternVOList(dto.getPatterns(), audioMap, imageMap, language));
        return vo;
    }

    private static List<AppTopicPatternVO> toPatternVOList(
            List<TopicPatternDto> dtos,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toPatternVO(dto, audioMap, imageMap, language))
                .collect(Collectors.toList());
    }

    private static AppTopicPatternVO toPatternVO(
            TopicPatternDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppTopicPatternVO vo = new AppTopicPatternVO();
        vo.setPattern(dto.getPattern());
        vo.setOrder(dto.getOrder());

        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto ossDto = imageMap.get(dto.getImageId());
            if (ossDto != null) {
                AppTopicBaseVO.ImageVO imageVO = new AppTopicBaseVO.ImageVO();
                imageVO.setImageUrl(ossDto.getFileUrl());
                vo.setImage(imageVO);
            }
        }

        vo.setChats(toChatVOList(dto.getChats(), audioMap, language));
        return vo;
    }

    private static List<AppTopicChatVO> toChatVOList(
            List<TopicChatDto> dtos,
            Map<Long, AudioResourceDto> audioMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toChatVO(dto, audioMap, language))
                .collect(Collectors.toList());
    }

    private static AppTopicChatVO toChatVO(
            TopicChatDto dto,
            Map<Long, AudioResourceDto> audioMap,
            String language) {
        AppTopicChatVO vo = new AppTopicChatVO();
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));

        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppTopicBaseVO.AudioVO audioVO = new AppTopicBaseVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
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
                .map(AppTopicWrapper::toTextTranslationVO)
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
