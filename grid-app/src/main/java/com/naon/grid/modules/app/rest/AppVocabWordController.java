package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabRelationDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.modules.app.rest.request.AppVocabWordSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppVocabWordBaseVO;
import com.naon.grid.modules.app.rest.vo.AppVocabWordDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/vocab")
@Api(tags = "用户：词汇接口")
public class AppVocabWordController {

    private final VocabWordService vocabWordService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;
    private final VocabOutlineRecordService vocabOutlineRecordService;

    @ApiOperation("搜索词汇")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppVocabWordBaseVO>> search(AppVocabWordSearchRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus("published");
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"));
        List<VocabWordDto> dtos = vocabWordService.queryAll(criteria, pageable).getContent();
        List<AppVocabWordBaseVO> vos = toBaseVOList(dtos);

        // 如果搜索结果为空，记录纲外词
        if (vos.isEmpty()) {
            vocabOutlineRecordService.recordIfNeeded(request.getBlurry());
        }

        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("词汇详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppVocabWordDetailVO> getDetail(
            @PathVariable Integer id,
            @RequestParam String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        VocabWordDto dto = vocabWordService.findPublishedById(id);
        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);
        Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImages(dto);
        AppVocabWordDetailVO vo = toDetailVO(dto, audioMap, imageMap, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    private List<AppVocabWordBaseVO> toBaseVOList(List<VocabWordDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private AppVocabWordBaseVO toBaseVO(VocabWordDto dto) {
        AppVocabWordBaseVO vo = new AppVocabWordBaseVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setHskLevel(dto.getHskLevel());
        return vo;
    }

    private Map<Long, AudioResourceDto> collectAndBatchQueryAudios(VocabWordDto dto) {
        List<Long> audioIds = new ArrayList<>();
        if (dto.getAudioId() != null) {
            audioIds.add(dto.getAudioId());
        }
        if (dto.getSenses() != null) {
            for (VocabSenseDto sense : dto.getSenses()) {
                if (sense.getDefAudioId() != null) {
                    audioIds.add(sense.getDefAudioId());
                }
                if (sense.getDefImageSentence() != null && sense.getDefImageSentence().getAudioId() != null) {
                    audioIds.add(sense.getDefImageSentence().getAudioId());
                }
                if (sense.getStructures() != null) {
                    for (VocabStructureDto structure : sense.getStructures()) {
                        if (structure.getStructureSentences() != null) {
                            for (ExampleSentenceDto sentence : structure.getStructureSentences()) {
                                if (sentence.getAudioId() != null) {
                                    audioIds.add(sentence.getAudioId());
                                }
                            }
                        }
                    }
                }
            }
        }
        List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
        return audioDtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, audio -> audio, (existing, replacement) -> existing));
    }

    private Map<Long, AliOssStorageDto> collectAndBatchQueryImages(VocabWordDto dto) {
        List<Long> imageIds = new ArrayList<>();
        if (dto.getSenses() != null) {
            for (VocabSenseDto sense : dto.getSenses()) {
                if (sense.getDefImageId() != null) {
                    imageIds.add(sense.getDefImageId());
                }
                if (sense.getDefImageSentence() != null && sense.getDefImageSentence().getImageId() != null) {
                    imageIds.add(sense.getDefImageSentence().getImageId());
                }
                if (sense.getStructures() != null) {
                    for (VocabStructureDto structure : sense.getStructures()) {
                        if (structure.getStructureSentences() != null) {
                            for (ExampleSentenceDto sentence : structure.getStructureSentences()) {
                                if (sentence.getImageId() != null) {
                                    imageIds.add(sentence.getImageId());
                                }
                            }
                        }
                    }
                }
            }
        }
        List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
        return imageDtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, img -> img, (existing, replacement) -> existing));
    }

    private AppVocabWordDetailVO toDetailVO(VocabWordDto dto, Map<Long, AudioResourceDto> audioMap,
                                             Map<Long, AliOssStorageDto> imageMap, String language) {
        AppVocabWordDetailVO vo = new AppVocabWordDetailVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        if (dto.getAudioId() != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            } else {
                log.error("词汇音频资源未找到, audioId={}", dto.getAudioId());
            }
        }
        vo.setHskLevel(dto.getHskLevel());
        vo.setSenses(toSenseVOList(dto.getSenses(), audioMap, imageMap, language));
        return vo;
    }

    private List<AppVocabWordDetailVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> dtos,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toSenseVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabSenseVO toSenseVO(VocabSenseDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        AppVocabWordDetailVO.VocabSenseVO vo = new AppVocabWordDetailVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        // defAudio
        if (dto.getDefAudioId() != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getDefAudioId());
            if (audioDto != null) {
                AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setDefAudio(audioVO);
            } else {
                log.error("义项释义音频资源未找到, audioId={}", dto.getDefAudioId());
            }
        }
        // defImage
        if (dto.getDefImageId() != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getDefImageId());
            if (imgDto != null) {
                AppVocabWordDetailVO.ImageVO imageVO = new AppVocabWordDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setDefImage(imageVO);
            } else {
                log.error("义项释义图片资源未找到, imageId={}", dto.getDefImageId());
            }
        }
        // translation（按语言筛选单条）
        vo.setTranslation(filterByLanguage(dto.getDefTranslations(), language));
        // defImageSentence（释义图片例句）
        if (dto.getDefImageSentence() != null) {
            vo.setDefImageSentence(toExampleVO(dto.getDefImageSentence(), audioMap, imageMap, language));
        }
        vo.setSynonymWords(toSynonymVOList(dto.getSynonymWords()));
        vo.setAntonymWords(toAntonymVOList(dto.getAntonymWords()));
        vo.setSequentialWords(toRelatedWordVOList(dto.getSequentialWords()));
        vo.setReverseSequentialWords(toRelatedWordVOList(dto.getReverseSequentialWords()));
        vo.setJumbledWords(toRelatedWordVOList(dto.getJumbledWords()));
        vo.setOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures(), audioMap, imageMap, language));
        return vo;
    }

    private List<AppVocabWordDetailVO.SynonymVO> toSynonymVOList(List<VocabRelationDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> {
            AppVocabWordDetailVO.SynonymVO vo = new AppVocabWordDetailVO.SynonymVO();
            vo.setContent(dto.getRelationWord());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AppVocabWordDetailVO.AntonymVO> toAntonymVOList(List<VocabRelationDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> {
            AppVocabWordDetailVO.AntonymVO vo = new AppVocabWordDetailVO.AntonymVO();
            vo.setContent(dto.getRelationWord());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AppVocabWordDetailVO.RelatedWordVO> toRelatedWordVOList(List<VocabRelationDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> {
            AppVocabWordDetailVO.RelatedWordVO vo = new AppVocabWordDetailVO.RelatedWordVO();
            vo.setContent(dto.getRelationWord());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AppVocabWordDetailVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> dtos,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toStructureVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabStructureVO toStructureVO(VocabStructureDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        AppVocabWordDetailVO.VocabStructureVO vo = new AppVocabWordDetailVO.VocabStructureVO();
        vo.setPattern(dto.getPattern());
        vo.setPatternDef(dto.getPatternDef());
        vo.setPatternDefTranslation(filterByLanguage(dto.getPatternDefTranslations(), language));
        vo.setOrder(dto.getStructureOrder());
        vo.setExamples(toExampleVOList(dto.getStructureSentences(), audioMap, imageMap, language));
        return vo;
    }

    private AppVocabWordDetailVO.VocabExampleVO toExampleVO(ExampleSentenceDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        AppVocabWordDetailVO.VocabExampleVO vo = new AppVocabWordDetailVO.VocabExampleVO();
        vo.setSentence(dto.getSentence());
        if (dto.getAudioId() != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            } else {
                log.error("例句音频资源未找到, audioId={}", dto.getAudioId());
            }
        }
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));
        if (dto.getImageId() != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                AppVocabWordDetailVO.ImageVO imageVO = new AppVocabWordDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setImage(imageVO);
            } else {
                log.error("例句图片资源未找到, imageId={}", dto.getImageId());
            }
        }
        vo.setOrder(dto.getOrder());
        return vo;
    }

    private List<AppVocabWordDetailVO.VocabExampleVO> toExampleVOList(List<ExampleSentenceDto> dtos,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toExampleVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private TextTranslationVO filterByLanguage(List<com.naon.grid.domain.common.TextTranslation> translations, String language) {
        if (translations == null || language == null) {
            return null;
        }
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(this::toTextTranslationVO)
                .orElse(null);
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
