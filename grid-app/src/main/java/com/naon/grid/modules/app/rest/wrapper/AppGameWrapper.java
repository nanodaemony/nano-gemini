package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.service.game.dto.GameQuestionDTO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO.GameOptionDTO;
import com.naon.grid.modules.app.rest.vo.AppExerciseQuestionDetailVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户端游戏包装器 — GameQuestionDTO → AppExerciseQuestionDetailVO。
 * <p>
 * 复用项目通用题目结构，与词汇大挑战（AppVocabChallengeWrapper）输出一致。
 */
public class AppGameWrapper {

    private static final String QUESTION_TYPE_PREFIX = "char_";

    /**
     * 批量转换题目 DTO 列表为通用题目 VO 列表。
     *
     * @param dtos 题目 DTO 列表
     * @return 通用题目 VO 列表
     */
    public static List<AppExerciseQuestionDetailVO> toQuestionVOList(List<GameQuestionDTO> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        List<AppExerciseQuestionDetailVO> vos = new ArrayList<>(dtos.size());
        for (GameQuestionDTO dto : dtos) {
            vos.add(toQuestionVO(dto));
        }
        return vos;
    }

    /**
     * 转换单个题目 DTO 为通用题目 VO。
     */
    public static AppExerciseQuestionDetailVO toQuestionVO(GameQuestionDTO dto) {
        AppExerciseQuestionDetailVO vo = new AppExerciseQuestionDetailVO();
        vo.setId(dto.getQuestionIndex() != null ? (long) dto.getQuestionIndex() : null);
        vo.setQuestionType(QUESTION_TYPE_PREFIX + dto.getGameType());
        vo.setStem(dto.getStem());
        vo.setContent(buildContent(dto));
        vo.setOptions(buildOptions(dto.getOptions()));
        vo.setAnswer(Collections.singletonList(dto.getCorrectKey()));
        vo.setExplanation(null);
        vo.setAudio(null);
        vo.setAudioText(null);
        vo.setSort(dto.getQuestionIndex());
        vo.setChildren(null);
        return vo;
    }

    private static AppExerciseQuestionDetailVO.QuestionContentVO buildContent(GameQuestionDTO dto) {
        AppExerciseQuestionDetailVO.QuestionContentVO content =
                new AppExerciseQuestionDetailVO.QuestionContentVO();
        if (dto.getCharacter() != null) {
            StringBuilder sb = new StringBuilder(dto.getCharacter());
            if (dto.getPinyin() != null) {
                sb.append(" (").append(dto.getPinyin()).append(")");
            }
            content.setContentText(sb.toString());
        }
        content.setImage(null);
        return content;
    }

    private static List<AppExerciseQuestionDetailVO.QuestionOptionVO> buildOptions(
            List<GameOptionDTO> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        List<AppExerciseQuestionDetailVO.QuestionOptionVO> vos = new ArrayList<>(dtos.size());
        for (GameOptionDTO dto : dtos) {
            AppExerciseQuestionDetailVO.QuestionOptionVO vo =
                    new AppExerciseQuestionDetailVO.QuestionOptionVO();
            vo.setOption(dto.getKey());
            vo.setOptionText(dto.getText());
            vo.setImage(null);
            vos.add(vo);
        }
        return vos;
    }
}
