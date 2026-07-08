package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalDetailVO;
import com.naon.grid.modules.app.rest.vo.AppRadicalPracticeVO;
import com.naon.grid.modules.app.rest.wrapper.AppCharRadicalWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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

    @ApiOperation("部首练习：获取目标部首+2个随机部首，各附带最多10个随机汉字")
    @AnonymousGetMapping("/{id}/practice")
    public ResponseEntity<AppRadicalPracticeVO> practice(@PathVariable Long id) {
        // 1. 查询目标部首
        CharRadicalDto targetRadical = charRadicalService.findPublishedById(id);

        // 2. 获取目标部首的随机汉字
        List<CharCharacterDto> targetChars = charCharacterService.findPublishedListByRadicalId(id);
        List<CharCharacterDto> randomTargetChars = pickRandom(targetChars, 10);

        // 3. 随机选2个其他部首
        List<CharRadicalDto> allRadicals = charRadicalService.findAllPublished();
        List<CharRadicalDto> others = allRadicals.stream()
                .filter(r -> !r.getId().equals(id))
                .collect(Collectors.toList());
        Collections.shuffle(others);
        List<CharRadicalDto> randomRadicals = others.stream().limit(2).collect(Collectors.toList());

        // 4. 获取随机部首的随机汉字
        List<AppRadicalPracticeVO.RadicalGroup> radicalGroups = new ArrayList<>();
        radicalGroups.add(AppCharRadicalWrapper.toGroup(targetRadical, randomTargetChars));
        for (CharRadicalDto radical : randomRadicals) {
            List<CharCharacterDto> chars = charCharacterService.findPublishedListByRadicalId(radical.getId());
            radicalGroups.add(AppCharRadicalWrapper.toGroup(radical, pickRandom(chars, 10)));
        }

        return new ResponseEntity<>(AppRadicalPracticeVO.withRadicals(radicalGroups), HttpStatus.OK);
    }

    /**
     * 从列表中随机取最多 limit 个元素
     */
    private static <T> List<T> pickRandom(List<T> list, int limit) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(limit, copy.size()));
    }
}
