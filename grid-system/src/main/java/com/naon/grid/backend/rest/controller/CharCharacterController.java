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
import com.naon.grid.backend.rest.wrapper.CharCharacterWrapper;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
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

import static com.naon.grid.backend.rest.wrapper.CharCharacterWrapper.toBaseVOList;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：汉字-汉字管理")
@RequestMapping("/api/character")
public class CharCharacterController {

    private final CharCharacterService charCharacterService;

    @Log("新增汉字")
    @ApiOperation("新增汉字")
    @AnonymousPostMapping
    public ResponseEntity<CharCharacterCreateVO> create(@Valid @RequestBody CharCharacterCreateRequest request) {
        CharCharacterCreateVO vo = new CharCharacterCreateVO();
        vo.setId(charCharacterService.create(CharCharacterWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改汉字内容")
    @ApiOperation("修改汉字内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Integer id, @Valid @RequestBody CharCharacterCreateRequest request) {
        charCharacterService.update(id, CharCharacterWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("汉字草稿审核通过")
    @ApiOperation("汉字草稿审核通过（草稿→已审核）")
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

    @Log("查询汉字详情")
    @ApiOperation("根据ID查询汉字详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<CharCharacterVO> findById(@PathVariable Integer id) {
        return new ResponseEntity<>(CharCharacterWrapper.toVO(charCharacterService.findById(id)), HttpStatus.OK);
    }

    @Log("查询汉字列表")
    @ApiOperation("分页查询汉字列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<CharCharacterBaseVO>> queryAll(CharCharacterQueryRequest request, Pageable pageable) {
        PageResult<CharCharacterDto> pageResult = charCharacterService.queryAll(CharCharacterWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(CharCharacterWrapper.toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除汉字")
    @ApiOperation("删除汉字")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Integer id) {
        charCharacterService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线汉字")
    @ApiOperation("下线汉字")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Object> offline(@PathVariable Integer id) {
        charCharacterService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
