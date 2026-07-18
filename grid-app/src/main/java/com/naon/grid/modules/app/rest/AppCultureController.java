package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.culture.CultureService;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureKeywordDto;
import com.naon.grid.backend.service.question.ExerciseQuestionService;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.modules.app.rest.request.AppCultureSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppCultureBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCultureDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppCultureWrapper;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.naon.grid.modules.app.rest.wrapper.AppExerciseQuestionWrapper.toDetailVOList;

/**
 * 用户端文化点接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/culture")
@Api(tags = "用户：文化点接口")
public class AppCultureController {

    private final CultureService cultureService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;
    private final ExerciseQuestionService exerciseQuestionService;

    @ApiOperation("搜索文化点（仅匹配名称字段）")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppCultureBaseVO>> search(AppCultureSearchRequest request) {
        List<CultureDto> dtos = cultureService.searchPublished(request.getBlurry());
        return new ResponseEntity<>(AppCultureWrapper.toBaseVOList(dtos), HttpStatus.OK);
    }

    @ApiOperation("根据ID查询文化点详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppCultureDetailVO> getDetail(
            @PathVariable Long id,
            @RequestParam String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        CultureDto dto = cultureService.findPublishedById(id);

        // 收集所有 audio IDs
        Set<Long> audioIds = new HashSet<>();
        if (dto.getAudioId() != null) audioIds.add(dto.getAudioId());
        if (dto.getOneSentenceIntroAudioId() != null) audioIds.add(dto.getOneSentenceIntroAudioId());
        if (dto.getDetailedIntroAudioId() != null) audioIds.add(dto.getDetailedIntroAudioId());
        if (dto.getKeywords() != null) {
            dto.getKeywords().stream()
                    .map(CultureKeywordDto::getAudioId)
                    .filter(Objects::nonNull)
                    .forEach(audioIds::add);
        }
        if (dto.getSentences() != null) {
            dto.getSentences().stream()
                    .map(ExampleSentenceDto::getAudioId)
                    .filter(Objects::nonNull)
                    .forEach(audioIds::add);
        }

        // 收集所有 image IDs
        Set<Long> imageIds = new HashSet<>();
        if (dto.getCoverImageId() != null) imageIds.add(dto.getCoverImageId());
        if (dto.getOneSentenceIntroImageId() != null) imageIds.add(dto.getOneSentenceIntroImageId());
        if (dto.getDetailedIntroImageId() != null) imageIds.add(dto.getDetailedIntroImageId());
        if (dto.getKeywords() != null) {
            dto.getKeywords().stream()
                    .map(CultureKeywordDto::getImageId)
                    .filter(Objects::nonNull)
                    .forEach(imageIds::add);
        }
        if (dto.getSentences() != null) {
            dto.getSentences().stream()
                    .map(ExampleSentenceDto::getImageId)
                    .filter(Objects::nonNull)
                    .forEach(imageIds::add);
        }

        // 批量查询资源
        Map<Long, AudioResourceDto> audioMap = new HashMap<>();
        if (!audioIds.isEmpty()) {
            try {
                audioMap = audioResourceService.findByIds(new ArrayList<>(audioIds)).stream()
                        .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
            } catch (Exception e) {
                log.error("批量查询音频资源失败", e);
            }
        }

        Map<Long, AliOssStorageDto> imageMap = new HashMap<>();
        if (!imageIds.isEmpty()) {
            try {
                imageMap = aliOssStorageService.findByIds(new ArrayList<>(imageIds)).stream()
                        .collect(Collectors.toMap(AliOssStorageDto::getId, img -> img, (a, b) -> a));
            } catch (Exception e) {
                log.error("批量查询图片资源失败", e);
            }
        }

        AppCultureDetailVO vo = AppCultureWrapper.toDetailVO(dto, audioMap, imageMap, language);

        // 批量查询练一练习题并转换
        if (dto.getQuestionIds() != null && !dto.getQuestionIds().isEmpty()) {
            try {
                List<ExerciseQuestionDto> questionDtos = exerciseQuestionService.findPublishedByIds(dto.getQuestionIds());
                // 收集练习题中的音频ID，追加到已有集合
                for (ExerciseQuestionDto q : questionDtos) {
                    if (q.getAudioId() != null) audioIds.add(q.getAudioId());
                }
                if (!audioIds.isEmpty()) {
                    audioMap = audioResourceService.findByIds(new ArrayList<>(audioIds)).stream()
                            .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
                }
                if (!imageIds.isEmpty()) {
                    imageMap = aliOssStorageService.findByIds(new ArrayList<>(imageIds)).stream()
                            .collect(Collectors.toMap(AliOssStorageDto::getId, img -> img, (a, b) -> a));
                }
                vo.setQuestions(toDetailVOList(questionDtos, audioMap, imageMap));
            } catch (Exception e) {
                log.error("查询练习题失败", e);
            }
        }

        return new ResponseEntity<>(vo, HttpStatus.OK);
    }
}
