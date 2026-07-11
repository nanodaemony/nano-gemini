package com.naon.grid.backend.service.game;

import com.naon.grid.backend.service.game.dto.GameQuestionDTO;

import java.util.List;

/**
 * 汉字大挑战 — 出题服务接口。
 * <p>
 * 三种游戏各返回 10 道题目；数据不足时返回实际可用数量，不抛异常。
 */
public interface GameCharacterService {

    /**
     * 部首识记 — 生成 10 道部首选择题。
     *
     * @param hskLevels HSK 等级代码列表，如 ["1", "2"]
     * @return 题目列表
     */
    List<GameQuestionDTO> generateRadicalQuestions(List<String> hskLevels);

    /**
     * 形近字辨析 — 生成 10 道形近字选择题。
     *
     * @return 题目列表
     */
    List<GameQuestionDTO> generateComparisonQuestions();

    /**
     * 汉字组词 — 生成 10 道组词选择题。
     *
     * @param hskLevels HSK 等级代码列表，如 ["1", "2"]
     * @return 题目列表
     */
    List<GameQuestionDTO> generateWordFormationQuestions(List<String> hskLevels);
}
