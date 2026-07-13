package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.topic.TopicService;
import com.naon.grid.backend.service.topic.dto.TopicChatDto;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicPatternDto;
import com.naon.grid.modules.app.rest.request.AppTopicSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppTopicBaseVO;
import com.naon.grid.modules.app.rest.vo.AppTopicDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppTopicWrapper;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/topic")
@Api(tags = "用户：话题接口")
public class AppTopicController {

    private final TopicService topicService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;

    @ApiOperation("搜索话题")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppTopicBaseVO>> search(AppTopicSearchRequest request) {
        List<TopicDto> dtos = topicService.searchPublished(request.getBlurry());
        return new ResponseEntity<>(AppTopicWrapper.toBaseVOList(dtos), HttpStatus.OK);
    }

    @ApiOperation("话题详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppTopicDetailVO> getDetail(
            @PathVariable Long id,
            @RequestParam String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        TopicDto dto = topicService.findPublishedById(id);

        // Preload audio resources
        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);

        // Preload image resources
        Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImages(dto);

        AppTopicDetailVO vo = AppTopicWrapper.toDetailVO(dto, audioMap, imageMap, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    private Map<Long, AudioResourceDto> collectAndBatchQueryAudios(TopicDto dto) {
        List<Long> audioIds = new ArrayList<>();
        if (dto.getAudioId() != null) {
            audioIds.add(dto.getAudioId());
        }
        if (dto.getPatterns() != null) {
            for (TopicPatternDto pattern : dto.getPatterns()) {
                if (pattern.getChats() != null) {
                    for (TopicChatDto chat : pattern.getChats()) {
                        if (chat.getAudioId() != null) {
                            audioIds.add(chat.getAudioId());
                        }
                    }
                }
            }
        }
        if (audioIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
            return audioDtos.stream()
                    .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
        } catch (Exception e) {
            log.error("音频资源批量查询失败, audioIds={}", audioIds, e);
            return Collections.emptyMap();
        }
    }

    private Map<Long, AliOssStorageDto> collectAndBatchQueryImages(TopicDto dto) {
        List<Long> imageIds = new ArrayList<>();
        if (dto.getCoverImageId() != null) {
            imageIds.add(dto.getCoverImageId());
        }
        if (dto.getPatterns() != null) {
            for (TopicPatternDto pattern : dto.getPatterns()) {
                if (pattern.getImageId() != null) {
                    imageIds.add(pattern.getImageId());
                }
            }
        }
        if (imageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
            return imageDtos.stream()
                    .collect(Collectors.toMap(AliOssStorageDto::getId, i -> i, (a, b) -> a));
        } catch (Exception e) {
            log.error("图片资源批量查询失败, imageIds={}", imageIds, e);
            return Collections.emptyMap();
        }
    }
}
