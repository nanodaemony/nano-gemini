package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.grammar.dto.GrammarErrorDto;
import com.naon.grid.backend.service.grammar.dto.GrammarMeaningDto;
import com.naon.grid.backend.service.grammar.dto.GrammarNoticeDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarStructureDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonChatDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonItemDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointBaseVO;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointDetailVO;
import com.naon.grid.service.dto.AliOssStorageDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AppGrammarPointWrapper {

    // ===== 搜索列表 =====

    public static List<AppGrammarPointBaseVO> toBaseVOList(List<GrammarPointDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppGrammarPointWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static AppGrammarPointBaseVO toBaseVO(GrammarPointDto dto) {
        AppGrammarPointBaseVO vo = new AppGrammarPointBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setHskLevel(dto.getHskLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setSubCategory(dto.getSubCategory());
        return vo;
    }

    // ===== 详情 =====

    public static AppGrammarPointDetailVO toDetailVO(GrammarPointDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            Map<Long, ExampleSentenceDto> sentenceMap,
            List<GrammarComparisonGroupDto> comparisons,
            String language) {
        AppGrammarPointDetailVO vo = new AppGrammarPointDetailVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setHskLevel(dto.getHskLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setSubCategory(dto.getSubCategory());
        vo.setMeanings(toMeaningVOList(dto.getMeanings(), audioMap, imageMap, language));
        vo.setStructures(toStructureVOList(dto.getStructures(), audioMap, imageMap, language));
        vo.setNotices(toNoticeVOList(dto.getNotices(), audioMap, imageMap, language));
        vo.setErrors(toErrorVOList(dto.getErrors(), language));
        vo.setQuestionIds(dto.getQuestionIds());
        vo.setComparisons(toComparisonVOList(comparisons, audioMap, imageMap, sentenceMap, language));
        return vo;
    }

    // ===== 意义 =====

    private static List<AppGrammarPointDetailVO.GrammarMeaningVO> toMeaningVOList(
            List<GrammarMeaningDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toMeaningVO(d, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarMeaningVO toMeaningVO(
            GrammarMeaningDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        AppGrammarPointDetailVO.GrammarMeaningVO vo = new AppGrammarPointDetailVO.GrammarMeaningVO();
        vo.setId(dto.getId());
        vo.setContent(dto.getMeaningContent());
        vo.setTranslation(filterByLanguage(dto.getMeaningContentTranslations(), language));
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                AppGrammarPointDetailVO.ImageVO imageVO = new AppGrammarPointDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setImage(imageVO);
            } else {
                log.error("语法意义图片资源未找到, imageId={}", dto.getImageId());
            }
        }
        vo.setSentences(toExampleVOList(dto.getSentences(), audioMap, imageMap, language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 结构 =====

    private static List<AppGrammarPointDetailVO.GrammarStructureVO> toStructureVOList(
            List<GrammarStructureDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toStructureVO(d, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarStructureVO toStructureVO(
            GrammarStructureDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        AppGrammarPointDetailVO.GrammarStructureVO vo = new AppGrammarPointDetailVO.GrammarStructureVO();
        vo.setId(dto.getId());
        vo.setContent(dto.getStructureContent());
        vo.setSentences(toExampleVOList(dto.getSentences(), audioMap, imageMap, language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 注意 =====

    private static List<AppGrammarPointDetailVO.GrammarNoticeVO> toNoticeVOList(
            List<GrammarNoticeDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toNoticeVO(d, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarNoticeVO toNoticeVO(
            GrammarNoticeDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        AppGrammarPointDetailVO.GrammarNoticeVO vo = new AppGrammarPointDetailVO.GrammarNoticeVO();
        vo.setId(dto.getId());
        vo.setContent(dto.getNoticeContent());
        vo.setTranslation(filterByLanguage(dto.getNoticeContentTranslations(), language));
        vo.setSentences(toExampleVOList(dto.getSentences(), audioMap, imageMap, language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 偏误 =====

    private static List<AppGrammarPointDetailVO.GrammarErrorVO> toErrorVOList(
            List<GrammarErrorDto> dtos, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toErrorVO(d, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarErrorVO toErrorVO(GrammarErrorDto dto, String language) {
        AppGrammarPointDetailVO.GrammarErrorVO vo = new AppGrammarPointDetailVO.GrammarErrorVO();
        vo.setId(dto.getId());
        vo.setContent(dto.getErrorContent());
        vo.setAnalysis(dto.getErrorAnalysis());
        vo.setAnalysisTranslation(filterByLanguage(dto.getErrorAnalysisTranslations(), language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 辨析组 =====

    private static List<AppGrammarPointDetailVO.GrammarComparisonVO> toComparisonVOList(
            List<GrammarComparisonGroupDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, Map<Long, ExampleSentenceDto> sentenceMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toComparisonVO(d, audioMap, imageMap, sentenceMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarComparisonVO toComparisonVO(
            GrammarComparisonGroupDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, Map<Long, ExampleSentenceDto> sentenceMap,
            String language) {
        AppGrammarPointDetailVO.GrammarComparisonVO vo = new AppGrammarPointDetailVO.GrammarComparisonVO();
        vo.setId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setItems(toComparisonItemVOList(dto.getItems(), audioMap, imageMap, sentenceMap, language));
        vo.setChats(toComparisonChatVOList(dto.getChats(), audioMap, language));
        return vo;
    }

    private static List<AppGrammarPointDetailVO.ComparisonItemVO> toComparisonItemVOList(
            List<GrammarComparisonItemDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, Map<Long, ExampleSentenceDto> sentenceMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toComparisonItemVO(d, audioMap, imageMap, sentenceMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.ComparisonItemVO toComparisonItemVO(
            GrammarComparisonItemDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, Map<Long, ExampleSentenceDto> sentenceMap,
            String language) {
        AppGrammarPointDetailVO.ComparisonItemVO vo = new AppGrammarPointDetailVO.ComparisonItemVO();
        vo.setGrammarId(dto.getGrammarId());
        vo.setGrammarName(dto.getGrammarName());
        vo.setUsageComparison(dto.getUsageComparison());
        vo.setUsageComparisonTranslation(filterByLanguage(dto.getUsageComparisonTranslations(), language));
        vo.setExampleSentences(dto.getExampleSentences());
        if (dto.getUsageSentenceId() != null && sentenceMap != null) {
            ExampleSentenceDto sentenceDto = sentenceMap.get(dto.getUsageSentenceId());
            if (sentenceDto != null) {
                vo.setUsageSentence(toExampleVO(sentenceDto, audioMap, imageMap, language));
            } else {
                log.error("辨析条目的用法例句未找到, usageSentenceId={}", dto.getUsageSentenceId());
            }
        }
        return vo;
    }

    // ===== 情景对话 =====

    private static List<AppGrammarPointDetailVO.ComparisonChatVO> toComparisonChatVOList(
            List<GrammarComparisonChatDto> dtos, Map<Long, AudioResourceDto> audioMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toComparisonChatVO(d, audioMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.ComparisonChatVO toComparisonChatVO(
            GrammarComparisonChatDto dto, Map<Long, AudioResourceDto> audioMap,
            String language) {
        AppGrammarPointDetailVO.ComparisonChatVO vo = new AppGrammarPointDetailVO.ComparisonChatVO();
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppGrammarPointDetailVO.AudioVO audioVO = new AppGrammarPointDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            } else {
                log.error("情景对话音频资源未找到, audioId={}", dto.getAudioId());
            }
        }
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 例句（公共） =====

    private static List<AppGrammarPointDetailVO.ExampleVO> toExampleVOList(
            List<ExampleSentenceDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toExampleVO(d, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.ExampleVO toExampleVO(
            ExampleSentenceDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        AppGrammarPointDetailVO.ExampleVO vo = new AppGrammarPointDetailVO.ExampleVO();
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppGrammarPointDetailVO.AudioVO audioVO = new AppGrammarPointDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            } else {
                log.error("例句音频资源未找到, audioId={}", dto.getAudioId());
            }
        }
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                AppGrammarPointDetailVO.ImageVO imageVO = new AppGrammarPointDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setImage(imageVO);
            } else {
                log.error("例句图片资源未找到, imageId={}", dto.getImageId());
            }
        }
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 翻译过滤（公共） =====

    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) {
            return null;
        }
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(AppGrammarPointWrapper::toTextTranslationVO)
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
