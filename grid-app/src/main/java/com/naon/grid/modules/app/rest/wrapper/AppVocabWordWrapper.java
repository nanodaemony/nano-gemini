package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabRelationDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppVocabWordBaseVO;
import com.naon.grid.modules.app.rest.vo.AppVocabWordDetailVO;
import com.naon.grid.service.dto.AliOssStorageDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户端词汇包装器
 */
@Slf4j
public class AppVocabWordWrapper {

    public static List<AppVocabWordBaseVO> toBaseVOList(List<VocabWordDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppVocabWordWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static AppVocabWordBaseVO toBaseVO(VocabWordDto dto) {
        AppVocabWordBaseVO vo = new AppVocabWordBaseVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setHskLevel(dto.getHskLevel());
        return vo;
    }

    public static AppVocabWordDetailVO toDetailVO(VocabWordDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        AppVocabWordDetailVO vo = new AppVocabWordDetailVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        if (dto.getAudioId() != null && audioMap != null) {
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

    private static List<AppVocabWordDetailVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> dtos,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toSenseVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppVocabWordDetailVO.VocabSenseVO toSenseVO(VocabSenseDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        AppVocabWordDetailVO.VocabSenseVO vo = new AppVocabWordDetailVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        if (dto.getDefAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getDefAudioId());
            if (audioDto != null) {
                AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setDefAudio(audioVO);
            } else {
                log.error("义项释义音频资源未找到, audioId={}", dto.getDefAudioId());
            }
        }
        if (dto.getDefImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getDefImageId());
            if (imgDto != null) {
                AppVocabWordDetailVO.ImageVO imageVO = new AppVocabWordDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setDefImage(imageVO);
            } else {
                log.error("义项释义图片资源未找到, imageId={}", dto.getDefImageId());
            }
        }
        vo.setTranslation(filterByLanguage(dto.getDefTranslations(), language));
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

    private static List<AppVocabWordDetailVO.SynonymVO> toSynonymVOList(List<VocabRelationDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> {
            AppVocabWordDetailVO.SynonymVO vo = new AppVocabWordDetailVO.SynonymVO();
            vo.setContent(dto.getRelationWord());
            return vo;
        }).collect(Collectors.toList());
    }

    private static List<AppVocabWordDetailVO.AntonymVO> toAntonymVOList(List<VocabRelationDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> {
            AppVocabWordDetailVO.AntonymVO vo = new AppVocabWordDetailVO.AntonymVO();
            vo.setContent(dto.getRelationWord());
            return vo;
        }).collect(Collectors.toList());
    }

    private static List<AppVocabWordDetailVO.RelatedWordVO> toRelatedWordVOList(List<VocabRelationDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> {
            AppVocabWordDetailVO.RelatedWordVO vo = new AppVocabWordDetailVO.RelatedWordVO();
            vo.setContent(dto.getRelationWord());
            return vo;
        }).collect(Collectors.toList());
    }

    private static List<AppVocabWordDetailVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> dtos,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toStructureVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppVocabWordDetailVO.VocabStructureVO toStructureVO(VocabStructureDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        AppVocabWordDetailVO.VocabStructureVO vo = new AppVocabWordDetailVO.VocabStructureVO();
        vo.setPattern(dto.getPattern());
        vo.setPatternDef(dto.getPatternDef());
        vo.setPatternDefTranslation(filterByLanguage(dto.getPatternDefTranslations(), language));
        vo.setOrder(dto.getStructureOrder());
        vo.setExamples(toExampleVOList(dto.getStructureSentences(), audioMap, imageMap, language));
        return vo;
    }

    private static AppVocabWordDetailVO.VocabExampleVO toExampleVO(ExampleSentenceDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        AppVocabWordDetailVO.VocabExampleVO vo = new AppVocabWordDetailVO.VocabExampleVO();
        vo.setSentence(dto.getSentence());
        if (dto.getAudioId() != null && audioMap != null) {
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
        if (dto.getImageId() != null && imageMap != null) {
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

    private static List<AppVocabWordDetailVO.VocabExampleVO> toExampleVOList(List<ExampleSentenceDto> dtos,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toExampleVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) {
            return null;
        }
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(AppVocabWordWrapper::toTextTranslationVO)
                .orElse(null);
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation translation) {
        if (translation == null) {
            return null;
        }
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
