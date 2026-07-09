package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonItemDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonItemDto;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterBaseVO;
import com.naon.grid.modules.app.rest.vo.AppComparisonGroupVO;
import com.naon.grid.modules.app.rest.vo.AppComparisonItemVO;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointBaseVO;
import com.naon.grid.modules.app.rest.vo.AppSearchResultVO;
import com.naon.grid.modules.app.rest.vo.AppVocabWordBaseVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端统一搜索包装器
 */
public class AppSearchWrapper {

    /**
     * 组装搜索结果为统一 VO
     */
    public static AppSearchResultVO toResultVO(
            List<AppVocabWordBaseVO> vocab,
            List<AppCharCharacterBaseVO> character,
            List<AppGrammarPointBaseVO> grammar,
            List<AppComparisonGroupVO> comparison) {
        AppSearchResultVO vo = new AppSearchResultVO();
        vo.setVocab(vocab != null ? vocab : Collections.emptyList());
        vo.setCharacter(character != null ? character : Collections.emptyList());
        vo.setGrammar(grammar != null ? grammar : Collections.emptyList());
        vo.setComparison(comparison != null ? comparison : Collections.emptyList());
        return vo;
    }

    /**
     * 词汇辨析组 → 精简 VO
     */
    public static AppComparisonGroupVO toComparisonGroupVO(
            VocabComparisonGroupDto dto, String type, List<AppComparisonItemVO> items) {
        AppComparisonGroupVO vo = new AppComparisonGroupVO();
        vo.setGroupId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setType(type);
        vo.setItems(items);
        return vo;
    }

    /**
     * 语法辨析组 → 精简 VO
     */
    public static AppComparisonGroupVO toComparisonGroupVO(
            GrammarComparisonGroupDto dto, String type, List<AppComparisonItemVO> items) {
        AppComparisonGroupVO vo = new AppComparisonGroupVO();
        vo.setGroupId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setType(type);
        vo.setItems(items);
        return vo;
    }

    /**
     * 词汇辨析条目 → 精简 VO 列表（仅 wordId + word）
     */
    public static List<AppComparisonItemVO> toVocabComparisonItemVOList(List<VocabComparisonItemDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppSearchWrapper::toVocabComparisonItemVO).collect(Collectors.toList());
    }

    private static AppComparisonItemVO toVocabComparisonItemVO(VocabComparisonItemDto dto) {
        AppComparisonItemVO vo = new AppComparisonItemVO();
        vo.setWordId(dto.getWordId());
        vo.setWord(dto.getWord());
        return vo;
    }

    /**
     * 语法辨析条目 → 精简 VO 列表（仅 grammarId + grammarName）
     */
    public static List<AppComparisonItemVO> toGrammarComparisonItemVOList(List<GrammarComparisonItemDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppSearchWrapper::toGrammarComparisonItemVO).collect(Collectors.toList());
    }

    private static AppComparisonItemVO toGrammarComparisonItemVO(GrammarComparisonItemDto dto) {
        AppComparisonItemVO vo = new AppComparisonItemVO();
        vo.setGrammarId(dto.getGrammarId());
        vo.setGrammarName(dto.getGrammarName());
        return vo;
    }
}
