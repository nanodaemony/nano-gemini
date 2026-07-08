package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalDetailVO;
import com.naon.grid.modules.app.rest.vo.AppPracticeCharVO;
import com.naon.grid.modules.app.rest.vo.AppRadicalCharVO;
import com.naon.grid.modules.app.rest.vo.AppRadicalPracticeVO;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端部首包装器
 */
public class AppCharRadicalWrapper {

    public static List<AppCharRadicalBaseVO> toBaseVOList(List<CharRadicalDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(AppCharRadicalWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static AppCharRadicalBaseVO toBaseVO(CharRadicalDto dto) {
        AppCharRadicalBaseVO vo = new AppCharRadicalBaseVO();
        vo.setId(dto.getId());
        vo.setRadical(dto.getRadical());
        vo.setRadicalName(dto.getRadicalName());
        vo.setStrokeNum(dto.getStrokeNum());
        vo.setRelationId(dto.getRelationId());
        return vo;
    }

    public static AppCharRadicalDetailVO toDetailVO(CharRadicalDto radicalDto, Page<CharCharacterDto> charPage) {
        AppCharRadicalDetailVO vo = new AppCharRadicalDetailVO();
        vo.setId(radicalDto.getId());
        vo.setRadical(radicalDto.getRadical());
        vo.setRadicalName(radicalDto.getRadicalName());
        vo.setStrokeNum(radicalDto.getStrokeNum());
        vo.setEvolutionDesc(radicalDto.getEvolutionDesc());
        vo.setRelationId(radicalDto.getRelationId());

        List<AppRadicalCharVO> charVOs = charPage.getContent().stream()
                .map(AppCharRadicalWrapper::toCharVO).collect(Collectors.toList());
        vo.setCharacters(charVOs);
        vo.setHasNext(charPage.hasNext());

        return vo;
    }

    public static AppRadicalCharVO toCharVO(CharCharacterDto dto) {
        AppRadicalCharVO vo = new AppRadicalCharVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        return vo;
    }

    public static AppRadicalPracticeVO.RadicalGroup toGroup(
            CharRadicalDto radicalDto, List<CharCharacterDto> charDtos) {
        AppRadicalPracticeVO.RadicalGroup group = new AppRadicalPracticeVO.RadicalGroup();
        group.setRadicalId(radicalDto.getId());
        group.setRadical(radicalDto.getRadical());
        group.setRadicalName(radicalDto.getRadicalName());
        List<AppPracticeCharVO> chars = charDtos.stream().map(dto -> {
            AppPracticeCharVO vo = new AppPracticeCharVO();
            vo.setId(dto.getId());
            vo.setCharacter(dto.getCharacter());
            return vo;
        }).collect(Collectors.toList());
        group.setCharacters(chars);
        return group;
    }
}
