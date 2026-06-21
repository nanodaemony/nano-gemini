package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.vo.CharStrokeVO;
import com.naon.grid.backend.rest.wrapper.CharStrokeWrapper;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.CharStrokeService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.modules.app.rest.request.AppCharCharacterSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterDetailVO;
import com.naon.grid.modules.app.rest.vo.AppCharStrokeVO;
import com.naon.grid.modules.app.rest.wrapper.AppCharCharacterWrapper;
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
import java.util.Map;
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
        return new ResponseEntity<>(AppCharCharacterWrapper.toBaseVOList(dtos), HttpStatus.OK);
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

        // 预加载音频资源
        AudioResourceDto audioDto = null;
        if (dto.getAudioId() != null) {
            try {
                audioDto = audioResourceService.findById(dto.getAudioId());
            } catch (Exception e) {
                log.error("读音音频资源未找到, audioId={}", dto.getAudioId(), e);
            }
        }

        // 预加载图片资源（从例句中收集）
        Map<Long, AliOssStorageDto> imageMap = Collections.emptyMap();
        if (dto.getWords() != null) {
            List<Long> imageIds = dto.getWords().stream()
                    .map(CharWordDto::getWordItemSentence)
                    .filter(s -> s != null && s.getImageId() != null)
                    .map(s -> s.getImageId())
                    .collect(Collectors.toList());
            if (!imageIds.isEmpty()) {
                imageMap = aliOssStorageService.findByIds(imageIds).stream()
                        .collect(Collectors.toMap(AliOssStorageDto::getId, img -> img, (a, b) -> a));
            }
        }

        AppCharCharacterDetailVO vo = AppCharCharacterWrapper.toDetailVO(dto, audioDto, imageMap, language);
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

}
