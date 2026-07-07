package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.backend.service.question.ExerciseQuestionService;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.domain.common.QuestionContent;
import com.naon.grid.domain.common.QuestionOption;
import com.naon.grid.modules.app.rest.request.AppExerciseQuestionBatchRequest;
import com.naon.grid.modules.app.rest.vo.AppExerciseQuestionDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppExerciseQuestionWrapper;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/exercise-question")
@Api(tags = "用户：练习题目接口")
public class AppExerciseQuestionController {

    private final ExerciseQuestionService exerciseQuestionService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;

    @ApiOperation("题目详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppExerciseQuestionDetailVO> getDetail(@PathVariable Long id) {
        ExerciseQuestionDto dto = exerciseQuestionService.findPublishedById(id);

        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);
        Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImages(dto);

        AppExerciseQuestionDetailVO vo = AppExerciseQuestionWrapper.toDetailVO(dto, audioMap, imageMap);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    @ApiOperation("批量查询题目详情")
    @AnonymousPostMapping("/batch")
    public ResponseEntity<List<AppExerciseQuestionDetailVO>> batchGetDetail(
            @RequestBody AppExerciseQuestionBatchRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("ids 不能为空");
        }
        List<ExerciseQuestionDto> dtos = exerciseQuestionService.findPublishedByIds(request.getIds());

        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudiosFromList(dtos);
        Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImagesFromList(dtos);

        List<AppExerciseQuestionDetailVO> vos = AppExerciseQuestionWrapper.toDetailVOList(dtos, audioMap, imageMap);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    // ===== 单个 DTO 资源收集 =====

    private Map<Long, AudioResourceDto> collectAndBatchQueryAudios(ExerciseQuestionDto dto) {
        List<Long> audioIds = new ArrayList<>();
        collectAudioIds(dto, audioIds);
        return batchQueryAudios(audioIds);
    }

    private Map<Long, AliOssStorageDto> collectAndBatchQueryImages(ExerciseQuestionDto dto) {
        List<Long> imageIds = new ArrayList<>();
        collectImageIds(dto, imageIds);
        return batchQueryImages(imageIds);
    }

    // ===== 多个 DTO 资源收集 =====

    private Map<Long, AudioResourceDto> collectAndBatchQueryAudiosFromList(List<ExerciseQuestionDto> dtos) {
        List<Long> audioIds = new ArrayList<>();
        for (ExerciseQuestionDto dto : dtos) {
            collectAudioIds(dto, audioIds);
        }
        return batchQueryAudios(audioIds);
    }

    private Map<Long, AliOssStorageDto> collectAndBatchQueryImagesFromList(List<ExerciseQuestionDto> dtos) {
        List<Long> imageIds = new ArrayList<>();
        for (ExerciseQuestionDto dto : dtos) {
            collectImageIds(dto, imageIds);
        }
        return batchQueryImages(imageIds);
    }

    // ===== 音频ID收集 =====

    private void collectAudioIds(ExerciseQuestionDto dto, List<Long> audioIds) {
        if (dto == null) {
            return;
        }
        if (dto.getAudioId() != null) {
            audioIds.add(dto.getAudioId());
        }
        if (dto.getChildren() != null) {
            for (ExerciseQuestionDto child : dto.getChildren()) {
                collectAudioIds(child, audioIds);
            }
        }
    }

    // ===== 图片ID收集 =====

    private void collectImageIds(ExerciseQuestionDto dto, List<Long> imageIds) {
        if (dto == null) {
            return;
        }
        collectImageIdsFromContent(dto.getContent(), imageIds);
        collectImageIdsFromOptions(dto.getOptions(), imageIds);
        if (dto.getChildren() != null) {
            for (ExerciseQuestionDto child : dto.getChildren()) {
                collectImageIds(child, imageIds);
            }
        }
    }

    private void collectImageIdsFromContent(QuestionContent content, List<Long> imageIds) {
        if (content == null || content.getContentImageId() == null) {
            return;
        }
        try {
            imageIds.add(Long.parseLong(content.getContentImageId()));
        } catch (NumberFormatException e) {
            log.error("题目内容图片ID格式错误, contentImageId={}", content.getContentImageId());
        }
    }

    private void collectImageIdsFromOptions(List<QuestionOption> options, List<Long> imageIds) {
        if (options == null) {
            return;
        }
        for (QuestionOption option : options) {
            if (option.getOptionImageId() != null) {
                try {
                    imageIds.add(Long.parseLong(option.getOptionImageId()));
                } catch (NumberFormatException e) {
                    log.error("题目选项图片ID格式错误, optionImageId={}", option.getOptionImageId());
                }
            }
        }
    }

    // ===== 批量查询 =====

    private Map<Long, AudioResourceDto> batchQueryAudios(List<Long> audioIds) {
        if (audioIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
        return audioDtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
    }

    private Map<Long, AliOssStorageDto> batchQueryImages(List<Long> imageIds) {
        if (imageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
        return imageDtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, i -> i, (i, j) -> i));
    }
}
