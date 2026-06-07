package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.vo.ExerciseOptionVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabExampleDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabExerciseDto;
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
    public ResponseEntity<AppVocabWordDetailVO> getDetail(@PathVariable Integer id) {
        VocabWordDto dto = vocabWordService.findPublishedById(id);
        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);
        Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImages(dto);
        AppVocabWordDetailVO vo = toDetailVO(dto, audioMap, imageMap);
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
                if (sense.getStructures() != null) {
                    for (VocabStructureDto structure : sense.getStructures()) {
                        if (structure.getExamples() != null) {
                            for (VocabExampleDto example : structure.getExamples()) {
                                if (example.getAudioId() != null) {
                                    audioIds.add(example.getAudioId());
                                }
                            }
                        }
                    }
                }
            }
        }
        List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
        return audioDtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, audio -> audio));
    }

    private Map<Long, AliOssStorageDto> collectAndBatchQueryImages(VocabWordDto dto) {
        List<Long> imageIds = new ArrayList<>();
        if (dto.getSenses() != null) {
            for (VocabSenseDto sense : dto.getSenses()) {
                if (sense.getDefImage() != null) {
                    imageIds.add(sense.getDefImage());
                }
                if (sense.getStructures() != null) {
                    for (VocabStructureDto structure : sense.getStructures()) {
                        if (structure.getExamples() != null) {
                            for (VocabExampleDto example : structure.getExamples()) {
                                if (example.getImage() != null) {
                                    imageIds.add(example.getImage());
                                }
                            }
                        }
                    }
                }
            }
        }
        List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
        return imageDtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, img -> img));
    }

    private AppVocabWordDetailVO toDetailVO(VocabWordDto dto, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        AppVocabWordDetailVO vo = new AppVocabWordDetailVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        if (dto.getAudioId() != null && audioMap.containsKey(dto.getAudioId())) {
            AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
            audioVO.setAudioUrl(audioMap.get(dto.getAudioId()).getFileUrl());
            vo.setAudio(audioVO);
        }
        vo.setHskLevel(dto.getHskLevel());
        vo.setSenses(toSenseVOList(dto.getSenses(), audioMap, imageMap));
        vo.setExercises(toExerciseVOList(dto.getExercises()));
        return vo;
    }

    private List<AppVocabWordDetailVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> dtos, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toSenseVO(dto, audioMap, imageMap)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabSenseVO toSenseVO(VocabSenseDto dto, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        AppVocabWordDetailVO.VocabSenseVO vo = new AppVocabWordDetailVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        if (dto.getDefAudioId() != null && audioMap.containsKey(dto.getDefAudioId())) {
            AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
            audioVO.setAudioUrl(audioMap.get(dto.getDefAudioId()).getFileUrl());
            vo.setDefAudio(audioVO);
        }
        if (dto.getDefImage() != null && imageMap.containsKey(dto.getDefImage())) {
            AppVocabWordDetailVO.ImageVO imageVO = new AppVocabWordDetailVO.ImageVO();
            imageVO.setImageUrl(imageMap.get(dto.getDefImage()).getFileUrl());
            vo.setDefImage(imageVO);
        }
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setSynonyms(toSynonymVOList(dto.getSynonyms()));
        vo.setAntonyms(toAntonymVOList(dto.getAntonyms()));
        vo.setRelatedForward(toRelatedWordVOList(dto.getRelatedForward()));
        vo.setRelatedBackward(toRelatedWordVOList(dto.getRelatedBackward()));
        vo.setRelatedOther(toRelatedWordVOList(dto.getRelatedOther()));
        vo.setSenseOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures(), audioMap, imageMap));
        return vo;
    }

    private List<AppVocabWordDetailVO.SynonymVO> toSynonymVOList(List<String> contents) {
        if (contents == null) {
            return Collections.emptyList();
        }
        return contents.stream().map(content -> {
            AppVocabWordDetailVO.SynonymVO vo = new AppVocabWordDetailVO.SynonymVO();
            vo.setContent(content);
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AppVocabWordDetailVO.AntonymVO> toAntonymVOList(List<String> contents) {
        if (contents == null) {
            return Collections.emptyList();
        }
        return contents.stream().map(content -> {
            AppVocabWordDetailVO.AntonymVO vo = new AppVocabWordDetailVO.AntonymVO();
            vo.setContent(content);
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AppVocabWordDetailVO.RelatedWordVO> toRelatedWordVOList(List<String> contents) {
        if (contents == null) {
            return Collections.emptyList();
        }
        return contents.stream().map(content -> {
            AppVocabWordDetailVO.RelatedWordVO vo = new AppVocabWordDetailVO.RelatedWordVO();
            vo.setContent(content);
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AppVocabWordDetailVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> dtos, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toStructureVO(dto, audioMap, imageMap)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabStructureVO toStructureVO(VocabStructureDto dto, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        AppVocabWordDetailVO.VocabStructureVO vo = new AppVocabWordDetailVO.VocabStructureVO();
        vo.setId(dto.getId());
        vo.setPattern(dto.getPattern());
        vo.setPatternDef(dto.getPatternDef());
        vo.setPatternDefTranslations(toTextTranslationVOList(dto.getPatternDefTranslations()));
        vo.setStructureOrder(dto.getStructureOrder());
        vo.setExamples(toExampleVOList(dto.getExamples(), audioMap, imageMap));
        return vo;
    }

    private List<AppVocabWordDetailVO.VocabExampleVO> toExampleVOList(List<VocabExampleDto> dtos, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toExampleVO(dto, audioMap, imageMap)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabExampleVO toExampleVO(VocabExampleDto dto, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        AppVocabWordDetailVO.VocabExampleVO vo = new AppVocabWordDetailVO.VocabExampleVO();
        vo.setId(dto.getId());
        vo.setSentence(dto.getSentence());
        if (dto.getAudioId() != null && audioMap.containsKey(dto.getAudioId())) {
            AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
            audioVO.setAudioUrl(audioMap.get(dto.getAudioId()).getFileUrl());
            vo.setAudio(audioVO);
        }
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        if (dto.getImage() != null && imageMap.containsKey(dto.getImage())) {
            AppVocabWordDetailVO.ImageVO imageVO = new AppVocabWordDetailVO.ImageVO();
            imageVO.setImageUrl(imageMap.get(dto.getImage()).getFileUrl());
            vo.setImage(imageVO);
        }
        vo.setExampleOrder(dto.getExampleOrder());
        return vo;
    }

    private List<AppVocabWordDetailVO.VocabExerciseVO> toExerciseVOList(List<VocabExerciseDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toExerciseVO).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabExerciseVO toExerciseVO(VocabExerciseDto dto) {
        AppVocabWordDetailVO.VocabExerciseVO vo = new AppVocabWordDetailVO.VocabExerciseVO();
        vo.setId(dto.getId());
        vo.setQuestionType(dto.getQuestionType());
        vo.setQuestionText(dto.getQuestionText());
        vo.setOptions(toExerciseOptionVOList(dto.getOptions()));
        vo.setAnswers(dto.getAnswers());
        vo.setExerciseOrder(dto.getExerciseOrder());
        return vo;
    }

    private List<ExerciseOptionVO> toExerciseOptionVOList(List<com.naon.grid.domain.common.ExerciseOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream().map(this::toExerciseOptionVO).collect(Collectors.toList());
    }

    private ExerciseOptionVO toExerciseOptionVO(com.naon.grid.domain.common.ExerciseOption option) {
        if (option == null) {
            return null;
        }
        ExerciseOptionVO vo = new ExerciseOptionVO();
        vo.setOption(option.getOption());
        vo.setText(option.getText());
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
