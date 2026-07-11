package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.game.GameCharacterService;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO;
import com.naon.grid.enums.HskLevelRange;
import com.naon.grid.modules.app.rest.vo.AppGameQuestionVO;
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
 * 用户端汉字游戏接口。
 * <p>
 * 三种游戏各返回 10 道题目，匿名访问。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/character/game")
@Api(tags = "用户：汉字游戏接口")
public class AppGameController {

    private final GameCharacterService gameCharacterService;

    @AnonymousGetMapping("/radical")
    @ApiOperation("获取部首识记题目（10题）")
    public ResponseEntity<List<AppGameQuestionVO>> getRadicalQuestions(
            @RequestParam @ApiParam(value = "难度: elementary|intermediate|advanced", required = true)
            String level,
            @RequestParam @ApiParam(value = "语言: zh|en", required = true)
            String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        List<String> hskLevels = HskLevelRange.fromKey(level);
        List<GameQuestionDTO> dtos = gameCharacterService.generateRadicalQuestions(hskLevels);
        List<AppGameQuestionVO> vos = AppGameWrapper.toQuestionVOList(dtos, language);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @AnonymousGetMapping("/comparison")
    @ApiOperation("获取形近字辨析题目（10题）")
    public ResponseEntity<List<AppGameQuestionVO>> getComparisonQuestions(
            @RequestParam @ApiParam(value = "语言: zh|en", required = true)
            String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        List<GameQuestionDTO> dtos = gameCharacterService.generateComparisonQuestions();
        List<AppGameQuestionVO> vos = AppGameWrapper.toQuestionVOList(dtos, language);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @AnonymousGetMapping("/word-formation")
    @ApiOperation("获取组词游戏题目（10题）")
    public ResponseEntity<List<AppGameQuestionVO>> getWordFormationQuestions(
            @RequestParam @ApiParam(value = "难度: elementary|intermediate|advanced", required = true)
            String level,
            @RequestParam @ApiParam(value = "语言: zh|en", required = true)
            String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        List<String> hskLevels = HskLevelRange.fromKey(level);
        List<GameQuestionDTO> dtos = gameCharacterService.generateWordFormationQuestions(hskLevels);
        List<AppGameQuestionVO> vos = AppGameWrapper.toQuestionVOList(dtos, language);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }
}
