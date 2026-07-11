package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.game.GameCharacterService;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO;
import com.naon.grid.enums.HskLevelRange;
import com.naon.grid.modules.app.rest.vo.AppExerciseQuestionDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppGameWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端汉字大挑战接口。
 * <p>
 * 三种游戏各返回 10 道题目，匿名访问，复用 {@link AppExerciseQuestionDetailVO} 通用题目结构。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/character/challenge")
@Api(tags = "用户：汉字大挑战")
public class AppCharacterChallengeController {

    private final GameCharacterService gameCharacterService;

    @AnonymousGetMapping("/radical")
    @ApiOperation("部首识记（10题）")
    public ResponseEntity<List<AppExerciseQuestionDetailVO>> getRadicalQuestions(
            @RequestParam @ApiParam(value = "难度: elementary|intermediate|advanced", required = true)
            String level) {
        List<String> hskLevels = HskLevelRange.fromKey(level);
        List<GameQuestionDTO> dtos = gameCharacterService.generateRadicalQuestions(hskLevels);
        return new ResponseEntity<>(AppGameWrapper.toQuestionVOList(dtos), HttpStatus.OK);
    }

    @AnonymousGetMapping("/comparison")
    @ApiOperation("形近字辨析（10题）")
    public ResponseEntity<List<AppExerciseQuestionDetailVO>> getComparisonQuestions() {
        List<GameQuestionDTO> dtos = gameCharacterService.generateComparisonQuestions();
        return new ResponseEntity<>(AppGameWrapper.toQuestionVOList(dtos), HttpStatus.OK);
    }

    @AnonymousGetMapping("/word-formation")
    @ApiOperation("汉字组词（10题）")
    public ResponseEntity<List<AppExerciseQuestionDetailVO>> getWordFormationQuestions(
            @RequestParam @ApiParam(value = "难度: elementary|intermediate|advanced", required = true)
            String level) {
        List<String> hskLevels = HskLevelRange.fromKey(level);
        List<GameQuestionDTO> dtos = gameCharacterService.generateWordFormationQuestions(hskLevels);
        return new ResponseEntity<>(AppGameWrapper.toQuestionVOList(dtos), HttpStatus.OK);
    }
}
