package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.domain.common.QuestionContent;
import com.naon.grid.domain.common.QuestionOption;
import com.naon.grid.modules.app.rest.vo.AppExerciseQuestionDetailVO;
import com.naon.grid.service.dto.AliOssStorageDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AppExerciseQuestionWrapper {

    public static AppExerciseQuestionDetailVO toDetailVO(ExerciseQuestionDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        if (dto == null) {
            return null;
        }
        AppExerciseQuestionDetailVO vo = new AppExerciseQuestionDetailVO();
        vo.setId(dto.getId());
        vo.setQuestionType(dto.getQuestionType());
        vo.setStem(dto.getStem());
        vo.setContent(toQuestionContentVO(dto.getContent(), imageMap));
        vo.setOptions(toQuestionOptionVOList(dto.getOptions(), imageMap));
        vo.setAnswer(dto.getAnswer());
        vo.setExplanation(dto.getExplanation());
        vo.setAudioText(dto.getAudioText());
        vo.setSort(dto.getSort());

        // 音频
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppExerciseQuestionDetailVO.AudioVO audioVO = new AppExerciseQuestionDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            } else {
                log.error("题目音频资源未找到, audioId={}", dto.getAudioId());
            }
        }

        // 子题
        vo.setChildren(toDetailVOList(dto.getChildren(), audioMap, imageMap));

        return vo;
    }

    public static List<AppExerciseQuestionDetailVO> toDetailVOList(List<ExerciseQuestionDto> dtos,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toDetailVO(d, audioMap, imageMap)).collect(Collectors.toList());
    }

    private static AppExerciseQuestionDetailVO.QuestionContentVO toQuestionContentVO(
            QuestionContent content, Map<Long, AliOssStorageDto> imageMap) {
        if (content == null) {
            return null;
        }
        AppExerciseQuestionDetailVO.QuestionContentVO vo = new AppExerciseQuestionDetailVO.QuestionContentVO();
        vo.setContentText(content.getContentText());
        if (content.getContentImageId() != null && imageMap != null) {
            try {
                Long imageId = Long.parseLong(content.getContentImageId());
                AliOssStorageDto imgDto = imageMap.get(imageId);
                if (imgDto != null) {
                    AppExerciseQuestionDetailVO.ImageVO imageVO = new AppExerciseQuestionDetailVO.ImageVO();
                    imageVO.setImageUrl(imgDto.getFileUrl());
                    vo.setImage(imageVO);
                } else {
                    log.error("题目内容图片资源未找到, imageId={}", imageId);
                }
            } catch (NumberFormatException e) {
                log.error("题目内容图片ID格式错误, contentImageId={}", content.getContentImageId());
            }
        }
        return vo;
    }

    private static List<AppExerciseQuestionDetailVO.QuestionOptionVO> toQuestionOptionVOList(
            List<QuestionOption> options, Map<Long, AliOssStorageDto> imageMap) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream().map(o -> toQuestionOptionVO(o, imageMap)).collect(Collectors.toList());
    }

    private static AppExerciseQuestionDetailVO.QuestionOptionVO toQuestionOptionVO(
            QuestionOption option, Map<Long, AliOssStorageDto> imageMap) {
        if (option == null) {
            return null;
        }
        AppExerciseQuestionDetailVO.QuestionOptionVO vo = new AppExerciseQuestionDetailVO.QuestionOptionVO();
        vo.setOption(option.getOption());
        vo.setOptionText(option.getOptionText());
        if (option.getOptionImageId() != null && imageMap != null) {
            try {
                Long imageId = Long.parseLong(option.getOptionImageId());
                AliOssStorageDto imgDto = imageMap.get(imageId);
                if (imgDto != null) {
                    AppExerciseQuestionDetailVO.ImageVO imageVO = new AppExerciseQuestionDetailVO.ImageVO();
                    imageVO.setImageUrl(imgDto.getFileUrl());
                    vo.setImage(imageVO);
                } else {
                    log.error("题目选项图片资源未找到, imageId={}", imageId);
                }
            } catch (NumberFormatException e) {
                log.error("题目选项图片ID格式错误, optionImageId={}", option.getOptionImageId());
            }
        }
        return vo;
    }
}
