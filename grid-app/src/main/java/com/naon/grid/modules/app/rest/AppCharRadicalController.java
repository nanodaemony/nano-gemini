package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalDetailVO;
import com.naon.grid.modules.app.rest.vo.AppRadicalCharVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/char/radical")
@Api(tags = "用户：部首学习")
public class AppCharRadicalController {

    private final CharRadicalService charRadicalService;
    private final CharCharacterService charCharacterService;

    @ApiOperation("部首列表")
    @AnonymousGetMapping
    public ResponseEntity<List<AppCharRadicalBaseVO>> list() {
        List<CharRadicalDto> dtos = charRadicalService.findAllPublished();
        List<AppCharRadicalBaseVO> vos = toBaseVOList(dtos);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("部首详情（含关联汉字分页）")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppCharRadicalDetailVO> detail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        CharRadicalDto radicalDto = charRadicalService.findPublishedById(id);
        Page<CharCharacterDto> charPage = charCharacterService.findPublishedByRadicalId(
                id, PageRequest.of(page, size));

        AppCharRadicalDetailVO vo = toDetailVO(radicalDto, charPage);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    // ==================== 转换方法 ====================

    private List<AppCharRadicalBaseVO> toBaseVOList(List<CharRadicalDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private AppCharRadicalBaseVO toBaseVO(CharRadicalDto dto) {
        AppCharRadicalBaseVO vo = new AppCharRadicalBaseVO();
        vo.setId(dto.getId());
        vo.setRadical(dto.getRadical());
        vo.setRadicalName(dto.getRadicalName());
        vo.setStrokeNum(dto.getStrokeNum());
        vo.setRelationId(dto.getRelationId());
        return vo;
    }

    private AppCharRadicalDetailVO toDetailVO(CharRadicalDto radicalDto, Page<CharCharacterDto> charPage) {
        AppCharRadicalDetailVO vo = new AppCharRadicalDetailVO();
        vo.setId(radicalDto.getId());
        vo.setRadical(radicalDto.getRadical());
        vo.setRadicalName(radicalDto.getRadicalName());
        vo.setStrokeNum(radicalDto.getStrokeNum());
        vo.setEvolutionDesc(radicalDto.getEvolutionDesc());
        vo.setRelationId(radicalDto.getRelationId());

        List<AppRadicalCharVO> charVOs = charPage.getContent().stream().map(this::toCharVO).collect(Collectors.toList());
        vo.setCharacters(charVOs);
        vo.setHasNext(charPage.hasNext());

        return vo;
    }

    private AppRadicalCharVO toCharVO(CharCharacterDto dto) {
        AppRadicalCharVO vo = new AppRadicalCharVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        return vo;
    }
}
