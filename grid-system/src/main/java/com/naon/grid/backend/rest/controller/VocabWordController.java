package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.ExerciseOptionRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.request.VocabWordCreateRequest;
import com.naon.grid.backend.rest.request.VocabWordQueryRequest;
import com.naon.grid.backend.rest.vo.ExerciseOptionVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.VocabOutlineRecordVO;
import com.naon.grid.backend.rest.vo.VocabWordBaseVO;
import com.naon.grid.backend.rest.vo.VocabWordCreateVO;
import com.naon.grid.backend.rest.vo.VocabWordVO;
import com.naon.grid.domain.common.ExerciseOption;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabExampleDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabExerciseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordQueryCriteria;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabOutlineRecordMapper;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：词汇-词汇管理")
@RequestMapping("/api/vocabulary")
public class VocabWordController {

    private final VocabWordService vocabWordService;
    private final VocabOutlineRecordService vocabOutlineRecordService;
    private final VocabOutlineRecordMapper vocabOutlineRecordMapper;

    @Log("查询词汇详情")
    @ApiOperation("根据ID查询词汇详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<VocabWordVO> findById(@PathVariable Integer id) {
        return new ResponseEntity<>(toVO(vocabWordService.findById(id)), HttpStatus.OK);
    }

    @Log("查询词汇列表")
    @ApiOperation("分页查询词汇列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<VocabWordBaseVO>> queryAll(VocabWordQueryRequest request, Pageable pageable) {
        PageResult<VocabWordDto> pageResult = vocabWordService.queryAll(toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("新增词汇")
    @ApiOperation("新增词汇")
    @AnonymousPostMapping
    public ResponseEntity<VocabWordCreateVO> create(@Valid @RequestBody VocabWordCreateRequest request) {
        VocabWordCreateVO vo = new VocabWordCreateVO();
        vo.setId(vocabWordService.create(toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("更新词汇")
    @ApiOperation("更新词汇")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Integer id, @Valid @RequestBody VocabWordCreateRequest request) {
        vocabWordService.update(id, toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除词汇")
    @ApiOperation("删除词汇")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Integer id) {
        vocabWordService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询纲外词列表")
    @ApiOperation("分页查询纲外词列表")
    @AnonymousGetMapping("/outline")
    public ResponseEntity<PageResult<VocabOutlineRecordVO>> queryOutline(
            VocabOutlineRecordQueryCriteria criteria,
            Pageable pageable) {
        // 默认按搜索次数降序、创建时间降序
        if (pageable.getSort().isEmpty()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "searchCount")
                            .and(Sort.by(Sort.Direction.DESC, "createTime"))
            );
        }
        PageResult<VocabOutlineRecordDto> pageResult = vocabOutlineRecordService.queryAll(criteria, pageable);
        List<VocabOutlineRecordVO> vos = pageResult.getContent().stream()
                .map(vocabOutlineRecordMapper::toVo)
                .collect(Collectors.toList());
        return new ResponseEntity<>(new PageResult<>(vos, pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("标记纲外词已处理")
    @ApiOperation("标记纲外词为已处理")
    @AnonymousPutMapping("/outline/{id}/complete")
    public ResponseEntity<Object> completeOutline(@PathVariable Integer id) {
        vocabOutlineRecordService.markAsCompleted(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
        dto.setExercises(toExerciseDtoList(request.getExercises()));
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
        dto.setId(request.getId());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setChineseDef(request.getChineseDef());
        dto.setDefAudioId(request.getDefAudioId());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setSynonyms(request.getSynonyms());
        dto.setAntonyms(request.getAntonyms());
        dto.setRelatedForward(request.getRelatedForward());
        dto.setRelatedBackward(request.getRelatedBackward());
        dto.setSenseOrder(request.getSenseOrder() != null ? request.getSenseOrder() : 0);
        dto.setStructures(toStructureDtoList(request.getStructures()));
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
        dto.setId(request.getId());
        dto.setPattern(request.getPattern());
        dto.setStructureOrder(request.getStructureOrder() != null ? request.getStructureOrder() : 0);
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
        dto.setId(request.getId());
        dto.setQuestionType(request.getQuestionType());
        dto.setQuestionText(request.getQuestionText());
        dto.setOptions(toExerciseOptionList(request.getOptions()));
        dto.setAnswers(request.getAnswers());
        dto.setExerciseOrder(request.getExerciseOrder() != null ? request.getExerciseOrder() : 0);
        return dto;
    }

    private List<ExerciseOption> toExerciseOptionList(List<ExerciseOptionRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toExerciseOption).collect(Collectors.toList());
    }

    private ExerciseOption toExerciseOption(ExerciseOptionRequest request) {
        if (request == null) {
            return null;
        }
        ExerciseOption option = new ExerciseOption();
        option.setOption(request.getOption());
        option.setText(request.getText());
        return option;
    }

    private List<VocabExampleDto> toExampleDtoList(List<VocabWordCreateRequest.VocabExampleRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toExampleDto).collect(Collectors.toList());
    }

    private VocabExampleDto toExampleDto(VocabWordCreateRequest.VocabExampleRequest request) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(request.getId());
        dto.setSentence(request.getSentence());
        dto.setAudioId(request.getAudioId());
        dto.setPinyin(request.getPinyin());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setExampleOrder(request.getExampleOrder() != null ? request.getExampleOrder() : 0);
        return dto;
    }

    private List<VocabWordBaseVO> toBaseVOList(List<VocabWordDto> resources) {
        return resources.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private VocabWordBaseVO toBaseVO(VocabWordDto dto) {
        VocabWordBaseVO vo = new VocabWordBaseVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
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
        vo.setExercises(toExerciseVOList(dto.getExercises()));
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
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setSynonyms(dto.getSynonyms());
        vo.setAntonyms(dto.getAntonyms());
        vo.setRelatedForward(dto.getRelatedForward());
        vo.setRelatedBackward(dto.getRelatedBackward());
        vo.setSenseOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures()));
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
        vo.setOptions(toExerciseOptionVOList(dto.getOptions()));
        vo.setAnswers(dto.getAnswers());
        vo.setExerciseOrder(dto.getExerciseOrder());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<ExerciseOptionVO> toExerciseOptionVOList(List<ExerciseOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream().map(this::toExerciseOptionVO).collect(Collectors.toList());
    }

    private ExerciseOptionVO toExerciseOptionVO(ExerciseOption option) {
        if (option == null) {
            return null;
        }
        ExerciseOptionVO vo = new ExerciseOptionVO();
        vo.setOption(option.getOption());
        vo.setText(option.getText());
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
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setExampleOrder(dto.getExampleOrder());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toTextTranslation).collect(Collectors.toList());
    }

    private TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) {
            return null;
        }
        TextTranslation translation = new TextTranslation();
        translation.setLanguage(request.getLanguage());
        translation.setTranslation(request.getTranslation());
        return translation;
    }

    private List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> translations) {
        if (translations == null) {
            return Collections.emptyList();
        }
        return translations.stream().map(this::toTextTranslationVO).collect(Collectors.toList());
    }

    private TextTranslationVO toTextTranslationVO(TextTranslation translation) {
        if (translation == null) {
            return null;
        }
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
