package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.request.VocabRelationRequest;
import com.naon.grid.backend.rest.request.VocabWordCreateRequest;
import com.naon.grid.backend.rest.request.VocabWordQueryRequest;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.VocabRelationVO;
import com.naon.grid.backend.rest.vo.VocabWordBaseSearchVO;
import com.naon.grid.backend.rest.vo.VocabWordBaseVO;
import com.naon.grid.backend.rest.vo.VocabWordVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabRelationDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.naon.grid.modules.system.service.AiContentMarkerHelper;
import com.naon.grid.modules.system.service.AiContentMarkerService.MarkerFields;

public class VocabWordWrapper {

    public static VocabWordQueryCriteria toCriteria(VocabWordQueryRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    public static VocabWordDto toDto(VocabWordCreateRequest request) {
        VocabWordDto dto = new VocabWordDto();
        dto.setWord(request.getWord());
        dto.setWordTraditional(request.getWordTraditional());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setHskLevel(request.getHskLevel());
        dto.setSenses(toSenseDtoList(request.getSenses()));
        return dto;
    }

    private static List<VocabSenseDto> toSenseDtoList(List<VocabWordCreateRequest.VocabSenseRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toSenseDto).collect(Collectors.toList());
    }

    private static VocabSenseDto toSenseDto(VocabWordCreateRequest.VocabSenseRequest request) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(request.getId());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setChineseDef(request.getChineseDef());
        dto.setDefAudioId(request.getDefAudioId());
        dto.setDefImageId(request.getDefImageId());
        dto.setDefTranslations(toTextTranslationList(request.getDefTranslations()));
        dto.setDefImageSentence(toExampleSentenceDto(request.getDefImageSentence()));
        dto.setSynonymWords(toRelationDtoList(request.getSynonymWords()));
        dto.setAntonymWords(toRelationDtoList(request.getAntonymWords()));
        dto.setSequentialWords(toRelationDtoList(request.getSequentialWords()));
        dto.setReverseSequentialWords(toRelationDtoList(request.getReverseSequentialWords()));
        dto.setJumbledWords(toRelationDtoList(request.getJumbledWords()));
        dto.setSenseOrder(request.getOrder() != null ? request.getOrder() : 0);
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        dto.setStructures(toStructureDtoList(request.getStructures()));
        return dto;
    }

    private static List<VocabStructureDto> toStructureDtoList(List<VocabWordCreateRequest.VocabStructureRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toStructureDto).collect(Collectors.toList());
    }

    private static VocabStructureDto toStructureDto(VocabWordCreateRequest.VocabStructureRequest request) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(request.getId());
        dto.setPattern(request.getPattern());
        dto.setPatternDef(request.getPatternDef());
        dto.setPatternDefTranslations(toTextTranslationList(request.getPatternDefTranslations()));
        dto.setStructureOrder(request.getOrder() != null ? request.getOrder() : 0);
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        dto.setStructureSentences(toExampleSentenceDtoList(request.getStructureSentences()));
        return dto;
    }

    private static List<VocabRelationDto> toRelationDtoList(List<VocabRelationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toRelationDto).collect(Collectors.toList());
    }

    private static VocabRelationDto toRelationDto(VocabRelationRequest request) {
        if (request == null) return null;
        VocabRelationDto dto = new VocabRelationDto();
        dto.setRelationType(request.getRelationType());
        dto.setRelationWordId(request.getRelationWordId());
        dto.setRelationSenseId(request.getRelationSenseId());
        dto.setRelationWord(request.getRelationWord());
        dto.setOrder(request.getOrder());
        return dto;
    }

    private static ExampleSentenceDto toExampleSentenceDto(ExampleSentenceRequest request) {
        if (request == null) return null;
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(request.getId());
        dto.setSentence(request.getSentence());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setImageId(request.getImageId());
        dto.setOrder(request.getOrder());
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        return dto;
    }

    private static List<ExampleSentenceDto> toExampleSentenceDtoList(List<ExampleSentenceRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toExampleSentenceDto).collect(Collectors.toList());
    }

    public static List<VocabWordBaseVO> toBaseVOList(List<VocabWordDto> resources) {
        return resources.stream().map(VocabWordWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static VocabWordBaseVO toBaseVO(VocabWordDto dto) {
        VocabWordBaseVO vo = new VocabWordBaseVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setSenseCount(dto.getSenseCount() != null ? dto.getSenseCount() : 0);
        vo.setStructureCount(dto.getStructureCount() != null ? dto.getStructureCount() : 0);
        vo.setTranslationStatus(dto.getTranslationStatus());
        vo.setPinyinStatus(dto.getPinyinStatus());
        vo.setAudioStatus(dto.getAudioStatus());
        vo.setImageStatus(dto.getImageStatus());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static List<VocabWordBaseSearchVO> toSearchVOList(List<VocabWordDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(VocabWordWrapper::toSearchVO).collect(Collectors.toList());
    }

    public static VocabWordBaseSearchVO toSearchVO(VocabWordDto dto) {
        VocabWordBaseSearchVO vo = new VocabWordBaseSearchVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setSenses(toSenseSearchItemVOList(dto.getSenses()));
        return vo;
    }

    private static List<VocabWordBaseSearchVO.VocabSenseSearchItemVO> toSenseSearchItemVOList(
            List<VocabSenseDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(VocabWordWrapper::toSenseSearchItemVO).collect(Collectors.toList());
    }

    private static VocabWordBaseSearchVO.VocabSenseSearchItemVO toSenseSearchItemVO(VocabSenseDto dto) {
        VocabWordBaseSearchVO.VocabSenseSearchItemVO vo =
                new VocabWordBaseSearchVO.VocabSenseSearchItemVO();
        vo.setId(dto.getId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        vo.setDefTranslations(toTextTranslationVOList(dto.getDefTranslations()));
        return vo;
    }

    public static VocabWordVO toVO(VocabWordDto dto, Map<String, MarkerFields> aiMarkers) {
        VocabWordVO vo = new VocabWordVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setSenses(toSenseVOList(dto.getSenses(), aiMarkers));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<VocabWordVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> resources,
            Map<String, MarkerFields> aiMarkers) {
        if (resources == null) return Collections.emptyList();
        return resources.stream().map(dto -> toSenseVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static VocabWordVO.VocabSenseVO toSenseVO(VocabSenseDto dto,
            Map<String, MarkerFields> aiMarkers) {
        VocabWordVO.VocabSenseVO vo = new VocabWordVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        vo.setDefAudioId(dto.getDefAudioId());
        vo.setDefImageId(dto.getDefImageId());
        vo.setDefTranslations(toTextTranslationVOList(dto.getDefTranslations()));
        vo.setDefImageSentence(toExampleSentenceVO(dto.getDefImageSentence(), aiMarkers));
        vo.setSynonymWords(toRelationVOList(dto.getSynonymWords()));
        vo.setAntonymWords(toRelationVOList(dto.getAntonymWords()));
        vo.setSequentialWords(toRelationVOList(dto.getSequentialWords()));
        vo.setReverseSequentialWords(toRelationVOList(dto.getReverseSequentialWords()));
        vo.setJumbledWords(toRelationVOList(dto.getJumbledWords()));
        vo.setOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures(), aiMarkers));
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        String key = AiContentMarkerHelper.key("vocab_sense", dto.getId());
        if (key != null && aiMarkers != null) {
            MarkerFields fields = aiMarkers.get(key);
            if (fields != null) {
                vo.setAiGeneratedFields(fields.getGenerated());
                vo.setAiReviewedFields(fields.getReviewed());
            }
        }
        return vo;
    }

    private static List<VocabWordVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> resources,
            Map<String, MarkerFields> aiMarkers) {
        if (resources == null) return Collections.emptyList();
        return resources.stream().map(dto -> toStructureVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static VocabWordVO.VocabStructureVO toStructureVO(VocabStructureDto dto,
            Map<String, MarkerFields> aiMarkers) {
        VocabWordVO.VocabStructureVO vo = new VocabWordVO.VocabStructureVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setSenseId(dto.getSenseId());
        vo.setPattern(dto.getPattern());
        vo.setPatternDef(dto.getPatternDef());
        vo.setPatternDefTranslations(toTextTranslationVOList(dto.getPatternDefTranslations()));
        vo.setOrder(dto.getStructureOrder());
        vo.setStructureExamples(toExampleSentenceVOList(dto.getStructureSentences(), aiMarkers));
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        String key = AiContentMarkerHelper.key("vocab_structure", dto.getId());
        if (key != null && aiMarkers != null) {
            MarkerFields fields = aiMarkers.get(key);
            if (fields != null) {
                vo.setAiGeneratedFields(fields.getGenerated());
                vo.setAiReviewedFields(fields.getReviewed());
            }
        }
        return vo;
    }

    private static List<VocabRelationVO> toRelationVOList(List<VocabRelationDto> resources) {
        if (resources == null) return Collections.emptyList();
        return resources.stream().map(VocabWordWrapper::toRelationVO).collect(Collectors.toList());
    }

    private static VocabRelationVO toRelationVO(VocabRelationDto dto) {
        if (dto == null) return null;
        VocabRelationVO vo = new VocabRelationVO();
        vo.setRelationId(dto.getId() != null ? dto.getId() : 0L);
        vo.setRelationType(dto.getRelationType());
        vo.setRelationWordId(dto.getRelationWordId() != null ? dto.getRelationWordId() : 0L);
        vo.setRelationSenseId(dto.getRelationSenseId() != null ? dto.getRelationSenseId() : 0L);
        vo.setRelationWord(dto.getRelationWord());
        vo.setOrder(dto.getOrder() != null ? dto.getOrder() : 0);
        return vo;
    }

    private static ExampleSentenceVO toExampleSentenceVO(ExampleSentenceDto dto,
            Map<String, MarkerFields> aiMarkers) {
        if (dto == null) return null;
        ExampleSentenceVO vo = new ExampleSentenceVO();
        vo.setId(dto.getId());
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setImageId(dto.getImageId());
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        String key = AiContentMarkerHelper.key("example_sentence", dto.getId());
        if (key != null && aiMarkers != null) {
            MarkerFields fields = aiMarkers.get(key);
            if (fields != null) {
                vo.setAiGeneratedFields(fields.getGenerated());
                vo.setAiReviewedFields(fields.getReviewed());
            }
        }
        return vo;
    }

    private static List<ExampleSentenceVO> toExampleSentenceVOList(List<ExampleSentenceDto> resources,
            Map<String, MarkerFields> aiMarkers) {
        if (resources == null) return Collections.emptyList();
        return resources.stream().map(dto -> toExampleSentenceVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toTextTranslation).collect(Collectors.toList());
    }

    private static TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) return null;
        TextTranslation t = new TextTranslation();
        t.setLanguage(request.getLanguage());
        t.setTranslation(request.getTranslation());
        return t;
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(VocabWordWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation t) {
        if (t == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(t.getLanguage());
        vo.setTranslation(t.getTranslation());
        return vo;
    }
}
