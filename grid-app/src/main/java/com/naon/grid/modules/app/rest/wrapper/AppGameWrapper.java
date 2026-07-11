package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO.GameExplanationDTO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO.GameOptionDTO;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppGameQuestionVO;
import com.naon.grid.modules.app.rest.vo.AppGameQuestionVO.GameExplanationVO;
import com.naon.grid.modules.app.rest.vo.AppGameQuestionVO.GameOptionVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户端游戏包装器 — DTO → VO 转换 + 单语言筛选。
 * <p>
 * 遵循 AppCharCharacterWrapper 的静态工具类模式。
 */
public class AppGameWrapper {

    /**
     * 批量转换题目 DTO 列表为单语言 VO 列表。
     *
     * @param dtos     多语言题目 DTO 列表
     * @param language 目标语言，如 "zh"、"en"
     * @return 单语言 VO 列表
     */
    public static List<AppGameQuestionVO> toQuestionVOList(List<GameQuestionDTO> dtos, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        List<AppGameQuestionVO> vos = new ArrayList<>(dtos.size());
        for (GameQuestionDTO dto : dtos) {
            vos.add(toQuestionVO(dto, language));
        }
        return vos;
    }

    /**
     * 转换单个题目 DTO 为单语言 VO。
     */
    public static AppGameQuestionVO toQuestionVO(GameQuestionDTO dto, String language) {
        AppGameQuestionVO vo = new AppGameQuestionVO();
        vo.setGameType(dto.getGameType());
        vo.setQuestionIndex(dto.getQuestionIndex());
        vo.setStem(dto.getStem());
        vo.setCharacter(dto.getCharacter());
        vo.setPinyin(dto.getPinyin());
        vo.setCorrectKey(dto.getCorrectKey());
        vo.setOptions(toOptionVOList(dto.getOptions()));
        vo.setExplanation(toExplanationVO(dto.getExplanation(), language));
        return vo;
    }

    private static List<GameOptionVO> toOptionVOList(List<GameOptionDTO> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        List<GameOptionVO> vos = new ArrayList<>(dtos.size());
        for (GameOptionDTO dto : dtos) {
            GameOptionVO vo = new GameOptionVO();
            vo.setKey(dto.getKey());
            vo.setText(dto.getText());
            vo.setIsCorrect(dto.getIsCorrect());
            vos.add(vo);
        }
        return vos;
    }

    private static GameExplanationVO toExplanationVO(GameExplanationDTO dto, String language) {
        if (dto == null) {
            return null;
        }
        GameExplanationVO vo = new GameExplanationVO();

        // 部首游戏解析
        vo.setRadical(dto.getRadical());
        vo.setRadicalName(dto.getRadicalName());
        vo.setRadicalMeaning(filterByLanguage(dto.getRadicalMeaning(), language));

        // 形近字辨析解析
        vo.setComparisonChar(dto.getComparisonChar());
        vo.setComparisonPinyin(dto.getComparisonPinyin());
        vo.setComparisonDesc(filterByLanguage(dto.getComparisonDesc(), language));

        // 组词游戏解析
        vo.setCorrectWord(dto.getCorrectWord());
        vo.setCorrectWordPinyin(dto.getCorrectWordPinyin());
        vo.setCorrectWordPos(dto.getCorrectWordPos());
        vo.setCorrectWordMeaning(filterByLanguage(dto.getCorrectWordMeaning(), language));

        return vo;
    }

    /**
     * 从多语言翻译列表中筛选匹配目标语言的单个翻译 VO。
     * <p>
     * 复刻 AppCharCharacterWrapper.filterByLanguage 的实现逻辑。
     */
    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) {
            return null;
        }
        for (TextTranslation t : translations) {
            if (language.equals(t.getLanguage())) {
                TextTranslationVO vo = new TextTranslationVO();
                vo.setLanguage(t.getLanguage());
                vo.setTranslation(t.getTranslation());
                return vo;
            }
        }
        return null;
    }
}
