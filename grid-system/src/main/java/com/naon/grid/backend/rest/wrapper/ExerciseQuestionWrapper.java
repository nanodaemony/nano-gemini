package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.ExerciseQuestionCreateRequest;
import com.naon.grid.backend.rest.request.ExerciseQuestionQueryRequest;
import com.naon.grid.backend.rest.vo.ExerciseQuestionBaseVO;
import com.naon.grid.backend.rest.vo.ExerciseQuestionVO;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionQueryCriteria;
import com.naon.grid.domain.common.QuestionContent;
import com.naon.grid.domain.common.QuestionOption;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExerciseQuestionWrapper {

    public static ExerciseQuestionQueryCriteria toCriteria(ExerciseQuestionQueryRequest request) {
        if (request == null) return null;
        ExerciseQuestionQueryCriteria criteria = new ExerciseQuestionQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setQuestionType(request.getQuestionType());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    public static ExerciseQuestionDto toDto(ExerciseQuestionCreateRequest request) {
        if (request == null) return null;
        ExerciseQuestionDto dto = new ExerciseQuestionDto();
        dto.setQuestionType(request.getQuestionType());
        dto.setStem(request.getStem());
        dto.setContent(toQuestionContent(request.getContent()));
        dto.setOptions(toQuestionOptionList(request.getOptions()));
        dto.setAnswer(request.getAnswer());
        dto.setExplanation(request.getExplanation());
        dto.setAudioId(request.getAudioId());
        dto.setAudioText(request.getAudioText());
        dto.setSort(request.getSort());
        dto.setChildren(toDtoList(request.getChildren()));
        return dto;
    }

    private static List<ExerciseQuestionDto> toDtoList(List<ExerciseQuestionCreateRequest> children) {
        if (children == null) return Collections.emptyList();
        return children.stream().map(ExerciseQuestionWrapper::toDto).collect(Collectors.toList());
    }

    private static QuestionContent toQuestionContent(ExerciseQuestionCreateRequest.QuestionContentRequest req) {
        if (req == null) return null;
        QuestionContent content = new QuestionContent();
        content.setContentText(req.getContentText());
        content.setContentImageId(req.getContentImageId());
        return content;
    }

    private static List<QuestionOption> toQuestionOptionList(List<ExerciseQuestionCreateRequest.QuestionOptionRequest> reqs) {
        if (reqs == null) return Collections.emptyList();
        return reqs.stream().map(ExerciseQuestionWrapper::toQuestionOption).collect(Collectors.toList());
    }

    private static QuestionOption toQuestionOption(ExerciseQuestionCreateRequest.QuestionOptionRequest req) {
        if (req == null) return null;
        QuestionOption option = new QuestionOption();
        option.setOption(req.getOption());
        option.setOptionText(req.getOptionText());
        option.setOptionImageId(req.getOptionImageId());
        return option;
    }

    public static List<ExerciseQuestionBaseVO> toBaseVOList(List<ExerciseQuestionDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(ExerciseQuestionWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static ExerciseQuestionBaseVO toBaseVO(ExerciseQuestionDto dto) {
        if (dto == null) return null;
        ExerciseQuestionBaseVO vo = new ExerciseQuestionBaseVO();
        vo.setId(dto.getId());
        vo.setQuestionType(dto.getQuestionType());
        vo.setStem(dto.getStem());
        vo.setAudioId(dto.getAudioId());
        vo.setAudioText(dto.getAudioText());
        vo.setSort(dto.getSort());
        vo.setChildCount(dto.getChildCount());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static ExerciseQuestionVO toVO(ExerciseQuestionDto dto) {
        if (dto == null) return null;
        ExerciseQuestionVO vo = new ExerciseQuestionVO();
        vo.setId(dto.getId());
        vo.setQuestionType(dto.getQuestionType());
        vo.setStem(dto.getStem());
        vo.setContent(toQuestionContentVO(dto.getContent()));
        vo.setOptions(toQuestionOptionVOList(dto.getOptions()));
        vo.setAnswer(dto.getAnswer());
        vo.setExplanation(dto.getExplanation());
        vo.setAudioId(dto.getAudioId());
        vo.setAudioText(dto.getAudioText());
        vo.setSort(dto.getSort());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setChildren(toVOList(dto.getChildren()));
        return vo;
    }

    private static List<ExerciseQuestionVO> toVOList(List<ExerciseQuestionDto> children) {
        if (children == null) return Collections.emptyList();
        return children.stream().map(ExerciseQuestionWrapper::toVO).collect(Collectors.toList());
    }

    private static ExerciseQuestionVO.QuestionContentVO toQuestionContentVO(QuestionContent content) {
        if (content == null) return null;
        ExerciseQuestionVO.QuestionContentVO vo = new ExerciseQuestionVO.QuestionContentVO();
        vo.setContentText(content.getContentText());
        vo.setContentImageId(content.getContentImageId());
        return vo;
    }

    private static List<ExerciseQuestionVO.QuestionOptionVO> toQuestionOptionVOList(List<QuestionOption> options) {
        if (options == null) return Collections.emptyList();
        return options.stream().map(ExerciseQuestionWrapper::toQuestionOptionVO).collect(Collectors.toList());
    }

    private static ExerciseQuestionVO.QuestionOptionVO toQuestionOptionVO(QuestionOption option) {
        if (option == null) return null;
        ExerciseQuestionVO.QuestionOptionVO vo = new ExerciseQuestionVO.QuestionOptionVO();
        vo.setOption(option.getOption());
        vo.setOptionText(option.getOptionText());
        vo.setOptionImageId(option.getOptionImageId());
        return vo;
    }
}
