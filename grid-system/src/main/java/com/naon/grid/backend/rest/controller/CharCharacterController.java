package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.CharCharacterCreateRequest;
import com.naon.grid.backend.rest.request.CharCharacterQueryRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.CharCharacterBaseVO;
import com.naon.grid.backend.rest.vo.CharCharacterCreateVO;
import com.naon.grid.backend.rest.vo.CharCharacterVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterDraftDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.backend.service.character.dto.CharDiscriminationDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.domain.common.TextTranslation;
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

import javax.validation.Valid;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：汉字-汉字管理")
@RequestMapping("/api/character")
public class CharCharacterController {

    private final CharCharacterService charCharacterService;

    @Log("查询汉字详情")
    @ApiOperation("根据ID查询汉字详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<CharCharacterVO> findById(@PathVariable Integer id) {
        return new ResponseEntity<>(toVO(charCharacterService.findById(id)), HttpStatus.OK);
    }

    @Log("查询汉字列表")
    @ApiOperation("分页查询汉字列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<CharCharacterBaseVO>> queryAll(CharCharacterQueryRequest request, Pageable pageable) {
        PageResult<CharCharacterDto> pageResult = charCharacterService.queryAll(toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("新增汉字")
    @ApiOperation("新增汉字")
    @AnonymousPostMapping
    public ResponseEntity<CharCharacterCreateVO> create(@Valid @RequestBody CharCharacterCreateRequest request) {
        CharCharacterCreateVO vo = new CharCharacterCreateVO();
        vo.setId(charCharacterService.create(toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改汉字")
    @ApiOperation("修改汉字")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Integer id, @Valid @RequestBody CharCharacterCreateRequest request) {
        charCharacterService.update(id, toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除汉字")
    @ApiOperation("删除汉字")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Integer id) {
        charCharacterService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询汉字草稿详情")
    @ApiOperation("根据ID查询汉字草稿详情")
    @AnonymousGetMapping("/{id}/draft")
    public ResponseEntity<CharCharacterVO> getDraft(@PathVariable Integer id) {
        CharCharacterDraftDto draftDto = charCharacterService.getDraft(id);
        // 转换为VO
        CharCharacterVO vo = new CharCharacterVO();
        vo.setId(draftDto.getId());
        vo.setSequenceNo(draftDto.getSequenceNo());
        vo.setCharacter(draftDto.getCharacter());
        vo.setLevel(draftDto.getLevel());
        vo.setPinyin(draftDto.getPinyin());
        vo.setAudioId(draftDto.getAudioId());
        vo.setTraditional(draftDto.getTraditional());
        vo.setRadical(draftDto.getRadical());
        vo.setStroke(draftDto.getStroke());
        vo.setCharDesc(draftDto.getCharDesc());
        vo.setDescTranslations(toTextTranslationVOList(draftDto.getDescTranslations()));
        vo.setDiscriminations(toDiscriminationVOList(draftDto.getDiscriminations()));
        vo.setWords(toWordVOList(draftDto.getWords()));
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    @Log("新增汉字草稿")
    @ApiOperation("新增汉字草稿")
    @AnonymousPostMapping("/draft")
    public ResponseEntity<CharCharacterCreateVO> createDraft(@Valid @RequestBody CharCharacterCreateRequest request) {
        CharCharacterDraftDto draftDto = convertToDraftDto(request);
        CharCharacterCreateVO vo = new CharCharacterCreateVO();
        vo.setId(charCharacterService.createDraft(draftDto));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改汉字草稿")
    @ApiOperation("修改汉字草稿")
    @AnonymousPutMapping("/{id}/draft")
    public ResponseEntity<Object> updateDraft(@PathVariable Integer id, @Valid @RequestBody CharCharacterCreateRequest request) {
        CharCharacterDraftDto draftDto = convertToDraftDto(request);
        charCharacterService.saveDraft(id, draftDto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("从已发布内容创建草稿")
    @ApiOperation("从已发布内容创建草稿")
    @AnonymousPostMapping("/{id}/draft/from-published")
    public ResponseEntity<Object> createDraftFromPublished(@PathVariable Integer id) {
        charCharacterService.createDraftFromPublished(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("审核汉字草稿")
    @ApiOperation("审核汉字草稿（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Object> reviewDraft(@PathVariable Integer id) {
        charCharacterService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布汉字")
    @ApiOperation("发布汉字（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Object> publishDraft(@PathVariable Integer id) {
        charCharacterService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线汉字")
    @ApiOperation("下线汉字")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Object> offline(@PathVariable Integer id) {
        charCharacterService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private CharCharacterQueryCriteria toCriteria(CharCharacterQueryRequest request) {
        CharCharacterQueryCriteria criteria = new CharCharacterQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        return criteria;
    }

    private CharCharacterDto toDto(CharCharacterCreateRequest request) {
        CharCharacterDto dto = new CharCharacterDto();
        dto.setSequenceNo(request.getSequenceNo());
        dto.setCharacter(request.getCharacter());
        dto.setLevel(request.getLevel());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTraditional(request.getTraditional());
        dto.setRadical(request.getRadical());
        dto.setStroke(request.getStroke());
        dto.setCharDesc(request.getCharDesc());
        dto.setDescTranslations(toTextTranslationList(request.getDescTranslations()));
        dto.setDiscriminations(toDiscriminationDtoList(request.getDiscriminations()));
        dto.setWords(toWordDtoList(request.getWords()));
        return dto;
    }

    private CharCharacterDraftDto convertToDraftDto(CharCharacterCreateRequest request) {
        CharCharacterDraftDto dto = new CharCharacterDraftDto();
        dto.setSequenceNo(request.getSequenceNo());
        dto.setCharacter(request.getCharacter());
        dto.setLevel(request.getLevel());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTraditional(request.getTraditional());
        dto.setRadical(request.getRadical());
        dto.setStroke(request.getStroke());
        dto.setCharDesc(request.getCharDesc());
        dto.setDescTranslations(toTextTranslationList(request.getDescTranslations()));
        dto.setDiscriminations(toDiscriminationDtoList(request.getDiscriminations()));
        dto.setWords(toWordDtoList(request.getWords()));
        return dto;
    }

    private List<CharDiscriminationDto> toDiscriminationDtoList(List<CharCharacterCreateRequest.CharDiscriminationRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toDiscriminationDto).collect(Collectors.toList());
    }

    private CharDiscriminationDto toDiscriminationDto(CharCharacterCreateRequest.CharDiscriminationRequest request) {
        CharDiscriminationDto dto = new CharDiscriminationDto();
        dto.setId(request.getId());
        dto.setDiscrimChar(request.getDiscrimChar());
        dto.setDiscrimPinyin(request.getDiscrimPinyin());
        dto.setDiscrimCharTranslations(toTextTranslationList(request.getDiscrimCharTranslations()));
        dto.setComparisonTranslations(toTextTranslationList(request.getComparisonTranslations()));
        return dto;
    }

    private List<CharWordDto> toWordDtoList(List<CharCharacterCreateRequest.CharWordRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toWordDto).collect(Collectors.toList());
    }

    private CharWordDto toWordDto(CharCharacterCreateRequest.CharWordRequest request) {
        CharWordDto dto = new CharWordDto();
        dto.setId(request.getId());
        dto.setWordItem(request.getWordItem());
        dto.setLevel(request.getLevel());
        dto.setPinyin(request.getPinyin());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setWordItemTranslations(toTextTranslationList(request.getWordItemTranslations()));
        dto.setExampleSentence(request.getExampleSentence());
        dto.setExamplePinyin(request.getExamplePinyin());
        dto.setExampleTranslations(toTextTranslationList(request.getExampleTranslations()));
        dto.setExampleImage(request.getExampleImage());
        return dto;
    }

    private List<CharCharacterBaseVO> toBaseVOList(List<CharCharacterDto> resources) {
        return resources.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private CharCharacterBaseVO toBaseVO(CharCharacterDto dto) {
        CharCharacterBaseVO vo = new CharCharacterBaseVO();
        vo.setId(dto.getId());
        vo.setSequenceNo(dto.getSequenceNo());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setStroke(dto.getStroke());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setHasDraft(dto.getDraftContent() != null);
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private CharCharacterVO toVO(CharCharacterDto dto) {
        CharCharacterVO vo = new CharCharacterVO();
        vo.setId(dto.getId());
        vo.setSequenceNo(dto.getSequenceNo());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setStroke(dto.getStroke());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setHasDraft(dto.getDraftContent() != null);
        vo.setDiscriminations(toDiscriminationVOList(dto.getDiscriminations()));
        vo.setWords(toWordVOList(dto.getWords()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<CharCharacterVO.CharDiscriminationVO> toDiscriminationVOList(List<CharDiscriminationDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toDiscriminationVO).collect(Collectors.toList());
    }

    private CharCharacterVO.CharDiscriminationVO toDiscriminationVO(CharDiscriminationDto dto) {
        CharCharacterVO.CharDiscriminationVO vo = new CharCharacterVO.CharDiscriminationVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setDiscrimChar(dto.getDiscrimChar());
        vo.setDiscrimPinyin(dto.getDiscrimPinyin());
        vo.setDiscrimCharTranslations(toTextTranslationVOList(dto.getDiscrimCharTranslations()));
        vo.setComparisonTranslations(toTextTranslationVOList(dto.getComparisonTranslations()));
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<CharCharacterVO.CharWordVO> toWordVOList(List<CharWordDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toWordVO).collect(Collectors.toList());
    }

    private CharCharacterVO.CharWordVO toWordVO(CharWordDto dto) {
        CharCharacterVO.CharWordVO vo = new CharCharacterVO.CharWordVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setWordItem(dto.getWordItem());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setWordItemTranslations(toTextTranslationVOList(dto.getWordItemTranslations()));
        vo.setExampleSentence(dto.getExampleSentence());
        vo.setExamplePinyin(dto.getExamplePinyin());
        vo.setExampleTranslations(toTextTranslationVOList(dto.getExampleTranslations()));
        vo.setExampleImage(dto.getExampleImage());
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
