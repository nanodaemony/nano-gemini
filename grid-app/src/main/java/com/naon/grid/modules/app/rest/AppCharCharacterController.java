package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharDiscriminationDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.modules.app.rest.request.AppCharCharacterSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterDetailVO;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端汉字接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/character")
@Api(tags = "用户：汉字接口")
public class AppCharCharacterController {

    private final CharCharacterService charCharacterService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;

    @ApiOperation("搜索汉字（仅匹配汉字字段）")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppCharCharacterBaseVO>> search(AppCharCharacterSearchRequest request) {
        List<CharCharacterDto> dtos = charCharacterService.searchByCharacter(request.getBlurry());
        List<AppCharCharacterBaseVO> vos = toBaseVOList(dtos);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("根据ID查询汉字详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppCharCharacterDetailVO> getDetail(@PathVariable Integer id) {
        CharCharacterDto dto = charCharacterService.findById(id);
        AppCharCharacterDetailVO vo = toDetailVO(dto);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    private List<AppCharCharacterBaseVO> toBaseVOList(List<CharCharacterDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private AppCharCharacterBaseVO toBaseVO(CharCharacterDto dto) {
        AppCharCharacterBaseVO vo = new AppCharCharacterBaseVO();
        vo.setId(dto.getId());
        vo.setSequenceNo(dto.getSequenceNo());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        return vo;
    }

    private AppCharCharacterDetailVO toDetailVO(CharCharacterDto dto) {
        AppCharCharacterDetailVO vo = new AppCharCharacterDetailVO();
        vo.setId(dto.getId());
        vo.setSequenceNo(dto.getSequenceNo());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        if (dto.getAudioId() != null) {
            AudioResourceDto audioDto = audioResourceService.findById(dto.getAudioId());
            if (audioDto != null) {
                AppCharCharacterDetailVO.AudioVO audioVO = new AppCharCharacterDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            }
        }
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setStroke(dto.getStroke());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
        vo.setDiscriminations(toDiscriminationVOList(dto.getDiscriminations()));
        vo.setWords(toWordVOList(dto.getWords()));
        return vo;
    }

    private List<AppCharCharacterDetailVO.CharDiscriminationVO> toDiscriminationVOList(List<CharDiscriminationDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toDiscriminationVO).collect(Collectors.toList());
    }

    private AppCharCharacterDetailVO.CharDiscriminationVO toDiscriminationVO(CharDiscriminationDto dto) {
        AppCharCharacterDetailVO.CharDiscriminationVO vo = new AppCharCharacterDetailVO.CharDiscriminationVO();
        vo.setId(dto.getId());
        vo.setDiscrimChar(dto.getDiscrimChar());
        vo.setDiscrimPinyin(dto.getDiscrimPinyin());
        vo.setDiscrimCharTranslations(toTextTranslationVOList(dto.getDiscrimCharTranslations()));
        vo.setComparisonTranslations(toTextTranslationVOList(dto.getComparisonTranslations()));
        return vo;
    }

    private List<AppCharCharacterDetailVO.CharWordVO> toWordVOList(List<CharWordDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toWordVO).collect(Collectors.toList());
    }

    private AppCharCharacterDetailVO.CharWordVO toWordVO(CharWordDto dto) {
        AppCharCharacterDetailVO.CharWordVO vo = new AppCharCharacterDetailVO.CharWordVO();
        vo.setWordItem(dto.getWordItem());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setWordItemTranslations(toTextTranslationVOList(dto.getWordItemTranslations()));
        vo.setExampleSentence(dto.getExampleSentence());
        vo.setExamplePinyin(dto.getExamplePinyin());
        vo.setExampleTranslations(toTextTranslationVOList(dto.getExampleTranslations()));
        if (dto.getExampleImage() != null) {
            try {
                Long imageId = Long.parseLong(dto.getExampleImage());
                AliOssStorageDto ossDto = aliOssStorageService.findById(imageId);
                if (ossDto != null) {
                    AppCharCharacterDetailVO.ImageVO imageVO = new AppCharCharacterDetailVO.ImageVO();
                    imageVO.setImageUrl(ossDto.getFileUrl());
                    vo.setExampleImage(imageVO);
                }
            } catch (NumberFormatException e) {
                // 不是有效的ID，忽略
            }
        }
        return vo;
    }

    private List<TextTranslationVO> toTextTranslationVOList(List<com.naon.grid.domain.common.TextTranslation> translations) {
        if (translations == null) {
            return Collections.emptyList();
        }
        return translations.stream().map(this::toTextTranslationVO).collect(Collectors.toList());
    }

    private TextTranslationVO toTextTranslationVO(com.naon.grid.domain.common.TextTranslation translation) {
        if (translation == null) {
            return null;
        }
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
