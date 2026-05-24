package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.backend.rest.request.VocabWordCreateRequest;
import com.naon.grid.backend.rest.request.VocabWordQueryRequest;
import com.naon.grid.backend.rest.vo.VocabWordCreateVO;
import com.naon.grid.backend.rest.vo.VocabWordVO;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabExampleDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabExerciseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Api(tags = "词汇：词汇管理")
@RequestMapping("/api/vocabulary")
public class VocabWordController {

    private final VocabWordService vocabWordService;

    @Log("查询词汇列表")
    @ApiOperation("分页查询词汇列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<VocabWordVO>> queryAll(VocabWordQueryRequest request, Pageable pageable) {
        PageResult<VocabWordDto> pageResult = vocabWordService.queryAll(toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(toVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("查询词汇详情")
    @ApiOperation("根据ID查询词汇详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<VocabWordVO> findById(@PathVariable Integer id) {
        return new ResponseEntity<>(toVO(vocabWordService.findById(id)), HttpStatus.OK);
    }

    @Log("新增词汇")
    @ApiOperation("新增词汇")
    @AnonymousPostMapping
    public ResponseEntity<VocabWordCreateVO> create(@RequestBody VocabWordCreateRequest request) {
        VocabWordCreateVO vo = new VocabWordCreateVO();
        vo.setId(vocabWordService.create(toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    private VocabWordQueryCriteria toCriteria(VocabWordQueryRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        return criteria;
    }

    private VocabWordDto toDto(VocabWordCreateRequest request) {
        VocabWordDto dto = new VocabWordDto();
        dto.setWord(request.getWord());
        dto.setWordTraditional(request.getWordTraditional());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setHskLevel(request.getHskLevel());
        dto.setSenses(toSenseDtoList(request.getSenses()));
        return dto;
    }

    private List<VocabSenseDto> toSenseDtoList(List<VocabWordCreateRequest.VocabSenseRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toSenseDto).collect(Collectors.toList());
    }

    private VocabSenseDto toSenseDto(VocabWordCreateRequest.VocabSenseRequest request) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setChineseDef(request.getChineseDef());
        dto.setDefAudioId(request.getDefAudioId());
        dto.setTranslations(request.getTranslations());
        dto.setSynonyms(request.getSynonyms());
        dto.setAntonyms(request.getAntonyms());
        dto.setRelatedForward(request.getRelatedForward());
        dto.setRelatedBackward(request.getRelatedBackward());
        dto.setSenseOrder(request.getSenseOrder());
        dto.setStructures(toStructureDtoList(request.getStructures()));
        dto.setExercises(toExerciseDtoList(request.getExercises()));
        return dto;
    }

    private List<VocabStructureDto> toStructureDtoList(List<VocabWordCreateRequest.VocabStructureRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toStructureDto).collect(Collectors.toList());
    }

    private VocabStructureDto toStructureDto(VocabWordCreateRequest.VocabStructureRequest request) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setPattern(request.getPattern());
        dto.setStructureOrder(request.getStructureOrder());
        dto.setExamples(toExampleDtoList(request.getExamples()));
        return dto;
    }

    private List<VocabExerciseDto> toExerciseDtoList(List<VocabWordCreateRequest.VocabExerciseRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toExerciseDto).collect(Collectors.toList());
    }

    private VocabExerciseDto toExerciseDto(VocabWordCreateRequest.VocabExerciseRequest request) {
        VocabExerciseDto dto = new VocabExerciseDto();
        dto.setQuestionType(request.getQuestionType());
        dto.setQuestionText(request.getQuestionText());
        dto.setOptions(request.getOptions());
        dto.setAnswers(request.getAnswers());
        dto.setExerciseOrder(request.getExerciseOrder());
        return dto;
    }

    private List<VocabExampleDto> toExampleDtoList(List<VocabWordCreateRequest.VocabExampleRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toExampleDto).collect(Collectors.toList());
    }

    private VocabExampleDto toExampleDto(VocabWordCreateRequest.VocabExampleRequest request) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setSentence(request.getSentence());
        dto.setAudioId(request.getAudioId());
        dto.setPinyin(request.getPinyin());
        dto.setTranslations(request.getTranslations());
        dto.setExampleOrder(request.getExampleOrder());
        return dto;
    }

    private List<VocabWordVO> toVOList(List<VocabWordDto> resources) {
        return resources.stream().map(this::toVO).collect(Collectors.toList());
    }

    private VocabWordVO toVO(VocabWordDto dto) {
        VocabWordVO vo = new VocabWordVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setSenses(toSenseVOList(dto.getSenses()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<VocabWordVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toSenseVO).collect(Collectors.toList());
    }

    private VocabWordVO.VocabSenseVO toSenseVO(VocabSenseDto dto) {
        VocabWordVO.VocabSenseVO vo = new VocabWordVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        vo.setDefAudioId(dto.getDefAudioId());
        vo.setTranslations(dto.getTranslations());
        vo.setSynonyms(dto.getSynonyms());
        vo.setAntonyms(dto.getAntonyms());
        vo.setRelatedForward(dto.getRelatedForward());
        vo.setRelatedBackward(dto.getRelatedBackward());
        vo.setSenseOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures()));
        vo.setExercises(toExerciseVOList(dto.getExercises()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<VocabWordVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toStructureVO).collect(Collectors.toList());
    }

    private VocabWordVO.VocabStructureVO toStructureVO(VocabStructureDto dto) {
        VocabWordVO.VocabStructureVO vo = new VocabWordVO.VocabStructureVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setSenseId(dto.getSenseId());
        vo.setPattern(dto.getPattern());
        vo.setStructureOrder(dto.getStructureOrder());
        vo.setExamples(toExampleVOList(dto.getExamples()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<VocabWordVO.VocabExerciseVO> toExerciseVOList(List<VocabExerciseDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toExerciseVO).collect(Collectors.toList());
    }

    private VocabWordVO.VocabExerciseVO toExerciseVO(VocabExerciseDto dto) {
        VocabWordVO.VocabExerciseVO vo = new VocabWordVO.VocabExerciseVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setQuestionType(dto.getQuestionType());
        vo.setQuestionText(dto.getQuestionText());
        vo.setOptions(dto.getOptions());
        vo.setAnswers(dto.getAnswers());
        vo.setExerciseOrder(dto.getExerciseOrder());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<VocabWordVO.VocabExampleVO> toExampleVOList(List<VocabExampleDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toExampleVO).collect(Collectors.toList());
    }

    private VocabWordVO.VocabExampleVO toExampleVO(VocabExampleDto dto) {
        VocabWordVO.VocabExampleVO vo = new VocabWordVO.VocabExampleVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setSenseId(dto.getSenseId());
        vo.setStructureId(dto.getStructureId());
        vo.setSentence(dto.getSentence());
        vo.setAudioId(dto.getAudioId());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(dto.getTranslations());
        vo.setExampleOrder(dto.getExampleOrder());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
}
