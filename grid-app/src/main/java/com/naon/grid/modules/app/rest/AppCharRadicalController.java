package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppCharRadicalWrapper;

import java.util.List;
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
        return new ResponseEntity<>(AppCharRadicalWrapper.toBaseVOList(charRadicalService.findAllPublished()), HttpStatus.OK);
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

        return new ResponseEntity<>(AppCharRadicalWrapper.toDetailVO(radicalDto, charPage), HttpStatus.OK);
    }
}
