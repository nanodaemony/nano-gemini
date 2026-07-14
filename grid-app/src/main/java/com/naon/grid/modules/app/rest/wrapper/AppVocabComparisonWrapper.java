package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonChatDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonItemDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppVocabComparisonGroupVO;
import com.naon.grid.modules.app.rest.vo.AppVocabComparisonGroupVO.AppChatVO;
import com.naon.grid.modules.app.rest.vo.AppVocabComparisonGroupVO.AppItemVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端词汇辨析包装器
 */
public class AppVocabComparisonWrapper {

    public static List<AppVocabComparisonGroupVO> toAppVOList(List<VocabComparisonGroupDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(AppVocabComparisonWrapper::toAppVO).collect(Collectors.toList());
    }

    public static AppVocabComparisonGroupVO toAppVO(VocabComparisonGroupDto dto) {
        AppVocabComparisonGroupVO vo = new AppVocabComparisonGroupVO();
        vo.setGroupId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setItems(toAppItemVOList(dto.getItems()));
        vo.setChats(toAppChatVOList(dto.getChats()));
        return vo;
    }

    public static List<AppItemVO> toAppItemVOList(List<VocabComparisonItemDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(AppVocabComparisonWrapper::toAppItemVO).collect(Collectors.toList());
    }

    public static AppItemVO toAppItemVO(VocabComparisonItemDto dto) {
        AppItemVO vo = new AppItemVO();
        vo.setWordId(dto.getWordId());
        vo.setWord(dto.getWord());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setUsageComparison(dto.getUsageComparison());
        vo.setUsageComparisonTranslations(toTextTranslationVOList(dto.getUsageComparisonTranslations()));
        vo.setCommonUsage(dto.getCommonUsage());
        vo.setCommonUsageTranslations(toTextTranslationVOList(dto.getCommonUsageTranslations()));
        vo.setExampleSentences(dto.getExampleSentences());
        vo.setOrder(dto.getOrder());
        return vo;
    }

    public static List<AppChatVO> toAppChatVOList(List<VocabComparisonChatDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(AppVocabComparisonWrapper::toAppChatVO).collect(Collectors.toList());
    }

    public static AppChatVO toAppChatVO(VocabComparisonChatDto dto) {
        AppChatVO vo = new AppChatVO();
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> translations) {
        if (translations == null) return Collections.emptyList();
        return translations.stream().map(AppVocabComparisonWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation translation) {
        if (translation == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
