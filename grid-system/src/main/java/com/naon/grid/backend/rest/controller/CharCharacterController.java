package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.CharCharacterCreateRequest;
import com.naon.grid.backend.rest.request.CharCharacterQueryRequest;
import com.naon.grid.backend.rest.vo.CharCharacterBaseVO;
import com.naon.grid.backend.rest.vo.CharCharacterCreateVO;
import com.naon.grid.backend.rest.vo.CharCharacterVO;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.backend.service.character.dto.CharDiscriminationDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
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
@Api(tags = "汉字：汉字管理")
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
    public ResponseEntity<CharCharacterCreateVO> create(@RequestBody CharCharacterCreateRequest request) {
        CharCharacterCreateVO vo = new CharCharacterCreateVO();
        vo.setId(charCharacterService.create(toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改汉字")
    @ApiOperation("修改汉字")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Integer id, @RequestBody CharCharacterCreateRequest request) {
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
        dto.setDescTranslations(request.getDescTranslations());
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
        dto.setDiscrimCharTranslations(request.getDiscrimCharTranslations());
        dto.setComparisonTranslations(request.getComparisonTranslations());
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
        dto.setWordItemTranslations(request.getWordItemTranslations());
        dto.setExampleSentence(request.getExampleSentence());
        dto.setExamplePinyin(request.getExamplePinyin());
        dto.setExampleTranslations(request.getExampleTranslations());
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
        vo.setDescTranslations(dto.getDescTranslations());
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
        vo.setDescTranslations(dto.getDescTranslations());
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
        vo.setDiscrimCharTranslations(dto.getDiscrimCharTranslations());
        vo.setComparisonTranslations(dto.getComparisonTranslations());
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
        vo.setWordItemTranslations(dto.getWordItemTranslations());
        vo.setExampleSentence(dto.getExampleSentence());
        vo.setExamplePinyin(dto.getExamplePinyin());
        vo.setExampleTranslations(dto.getExampleTranslations());
        vo.setExampleImage(dto.getExampleImage());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
}
