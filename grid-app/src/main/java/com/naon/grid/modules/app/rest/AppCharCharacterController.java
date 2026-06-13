package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.vo.CharStrokeVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.wrapper.CharStrokeWrapper;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.CharStrokeService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharComparisonDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.request.AppCharCharacterSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterDetailVO;
import com.naon.grid.modules.app.rest.vo.AppCharStrokeVO;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端汉字接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/character")
@Api(tags = "用户：汉字接口")
public class AppCharCharacterController {

    private final CharCharacterService charCharacterService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;
    private final CharStrokeService charStrokeService;

    @ApiOperation("搜索汉字（仅匹配汉字字段）")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppCharCharacterBaseVO>> search(AppCharCharacterSearchRequest request) {
        List<CharCharacterDto> dtos = charCharacterService.searchPublishedByCharacter(request.getBlurry());
        List<AppCharCharacterBaseVO> vos = toBaseVOList(dtos);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("根据ID查询汉字详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppCharCharacterDetailVO> getDetail(
            @PathVariable Integer id,
            @RequestParam String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        CharCharacterDto dto = charCharacterService.findPublishedById(id);
        AppCharCharacterDetailVO vo = toDetailVO(dto, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    @ApiOperation("根据汉字查询笔顺数据（SVG路径、坐标参考线）")
    @AnonymousGetMapping("/stroke/{character}")
    public ResponseEntity<AppCharStrokeVO> findStrokeByCharacter(@PathVariable String character) {
        String strokeJson = charStrokeService.findByCharacter(character);
        CharStrokeVO adminVo = CharStrokeWrapper.toStrokeVO(character, strokeJson);
        AppCharStrokeVO vo = new AppCharStrokeVO();
        vo.setCharacter(adminVo.getCharacter());
        vo.setStrokes(adminVo.getStrokes());
        vo.setMedians(adminVo.getMedians());
        vo.setRadStrokes(adminVo.getRadStrokes());
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
        vo.setCharacter(dto.getCharacter());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        return vo;
    }

    private AppCharCharacterDetailVO toDetailVO(CharCharacterDto dto, String language) {
        AppCharCharacterDetailVO vo = new AppCharCharacterDetailVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        if (dto.getAudioId() != null) {
            try {
                AudioResourceDto audioDto = audioResourceService.findById(dto.getAudioId());
                if (audioDto != null) {
                    AppCharCharacterDetailVO.AudioVO audioVO = new AppCharCharacterDetailVO.AudioVO();
                    audioVO.setAudioUrl(audioDto.getFileUrl());
                    vo.setAudio(audioVO);
                }
            } catch (Exception e) {
                log.error("读音音频资源未找到, audioId={}", dto.getAudioId(), e);
            }
        }
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setComponentCombination(dto.getComponentCombination());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslation(filterByLanguage(dto.getDescTranslations(), language));
        vo.setDiscriminations(toDiscriminationVOList(dto.getComparisons(), language));
        vo.setWords(toWordVOList(dto.getWords(), language));
        return vo;
    }

    private List<AppCharCharacterDetailVO.CharDiscriminationVO> toDiscriminationVOList(
            List<CharComparisonDto> dtos, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toDiscriminationVO(dto, language)).collect(Collectors.toList());
    }

    private AppCharCharacterDetailVO.CharDiscriminationVO toDiscriminationVO(
            CharComparisonDto dto, String language) {
        AppCharCharacterDetailVO.CharDiscriminationVO vo = new AppCharCharacterDetailVO.CharDiscriminationVO();
        vo.setComparisonChar(dto.getComparisonChar());
        vo.setComparisonPinyin(dto.getComparisonPinyin());
        vo.setComparisonCharTranslation(filterByLanguage(dto.getComparisonCharTranslations(), language));
        vo.setComparisonDescTranslation(filterByLanguage(dto.getComparisonDescTranslations(), language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    private List<AppCharCharacterDetailVO.CharWordVO> toWordVOList(
            List<CharWordDto> dtos, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toWordVO(dto, language)).collect(Collectors.toList());
    }

    private AppCharCharacterDetailVO.CharWordVO toWordVO(CharWordDto dto, String language) {
        AppCharCharacterDetailVO.CharWordVO vo = new AppCharCharacterDetailVO.CharWordVO();
        vo.setWordItem(dto.getWordItem());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setWordItemTranslation(filterByLanguage(dto.getWordItemTranslations(), language));
        ExampleSentenceDto sentenceDto = dto.getWordItemSentence();
        if (sentenceDto != null) {
            vo.setExampleSentence(sentenceDto.getSentence());
            vo.setExamplePinyin(sentenceDto.getPinyin());
            vo.setExampleTranslation(filterByLanguage(sentenceDto.getTranslations(), language));
            if (sentenceDto.getImageId() != null) {
                try {
                    AliOssStorageDto ossDto = aliOssStorageService.findById(sentenceDto.getImageId());
                    if (ossDto != null) {
                        AppCharCharacterDetailVO.ImageVO imageVO = new AppCharCharacterDetailVO.ImageVO();
                        imageVO.setImageUrl(ossDto.getFileUrl());
                        vo.setExampleImage(imageVO);
                    }
                } catch (Exception e) {
                    log.error("例句图片资源未找到, imageId={}", sentenceDto.getImageId(), e);
                }
            }
        }
        return vo;
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

    private TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) {
            return null;
        }
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(this::toTextTranslationVO)
                .orElse(null);
    }
}
