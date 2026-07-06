package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarErrorDto;
import com.naon.grid.backend.service.grammar.dto.GrammarMeaningDto;
import com.naon.grid.backend.service.grammar.dto.GrammarNoticeDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarStructureDto;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonChatDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonItemDto;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.modules.app.rest.request.AppGrammarPointSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointBaseVO;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppGrammarPointWrapper;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/grammar")
@Api(tags = "用户：语法接口")
public class AppGrammarPointController {

    private final GrammarPointService grammarPointService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final ExampleSentenceService exampleSentenceService;

    @ApiOperation("搜索语法点")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppGrammarPointBaseVO>> search(AppGrammarPointSearchRequest request) {
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"));
        List<GrammarPointDto> dtos = grammarPointService.searchPublished(request.getKeyword(), pageable).getContent();
        return new ResponseEntity<>(AppGrammarPointWrapper.toBaseVOList(dtos), HttpStatus.OK);
    }

    @ApiOperation("语法点详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppGrammarPointDetailVO> getDetail(
            @PathVariable Long id,
            @RequestParam String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        GrammarPointDto dto = grammarPointService.findPublishedById(id);

        // 预加载音频和图片资源
        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);
        Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImages(dto);

        // 预加载辨析组（含条目、对话例句等）
        List<GrammarComparisonGroupDto> comparisons = grammarComparisonGroupService.searchByGrammarId(id);
        // 从辨析组中收集额外的资源：usageSentenceId → ExampleSentenceDto + 音频ID
        Map<Long, ExampleSentenceDto> sentenceMap = collectComparisonSentences(comparisons);
        List<Long> comparisonAudioIds = collectSentenceAudios(sentenceMap);
        List<Long> comparisonImageIds = collectSentenceImages(sentenceMap);
        audioMap = mergeAudioMap(audioMap, comparisonAudioIds);
        imageMap = mergeImageMap(imageMap, comparisonImageIds);

        AppGrammarPointDetailVO vo = AppGrammarPointWrapper.toDetailVO(dto, audioMap, imageMap, sentenceMap, comparisons, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    // ===== 音频/图片预加载 =====

    private Map<Long, AudioResourceDto> collectAndBatchQueryAudios(GrammarPointDto dto) {
        List<Long> audioIds = new ArrayList<>();
        collectFromMeanings(dto.getMeanings(), audioIds);
        collectFromStructures(dto.getStructures(), audioIds);
        collectFromNotices(dto.getNotices(), audioIds);
        // errors 没有例句，跳过
        return batchQueryAudios(audioIds);
    }

    private void collectFromMeanings(List<GrammarMeaningDto> meanings, List<Long> audioIds) {
        if (meanings == null) return;
        for (GrammarMeaningDto m : meanings) {
            collectFromSentences(m.getSentences(), audioIds);
        }
    }

    private void collectFromStructures(List<GrammarStructureDto> structures, List<Long> audioIds) {
        if (structures == null) return;
        for (GrammarStructureDto s : structures) {
            collectFromSentences(s.getSentences(), audioIds);
        }
    }

    private void collectFromNotices(List<GrammarNoticeDto> notices, List<Long> audioIds) {
        if (notices == null) return;
        for (GrammarNoticeDto n : notices) {
            collectFromSentences(n.getSentences(), audioIds);
        }
    }

    private void collectFromSentences(List<ExampleSentenceDto> sentences, List<Long> audioIds) {
        if (sentences == null) return;
        for (ExampleSentenceDto s : sentences) {
            if (s.getAudioId() != null) {
                audioIds.add(s.getAudioId());
            }
        }
    }

    private Map<Long, AudioResourceDto> batchQueryAudios(List<Long> audioIds) {
        if (audioIds.isEmpty()) return Collections.emptyMap();
        List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
        return audioDtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
    }

    private Map<Long, AliOssStorageDto> collectAndBatchQueryImages(GrammarPointDto dto) {
        List<Long> imageIds = new ArrayList<>();
        if (dto.getMeanings() != null) {
            for (GrammarMeaningDto m : dto.getMeanings()) {
                if (m.getImageId() != null) {
                    imageIds.add(m.getImageId());
                }
                if (m.getSentences() != null) {
                    for (ExampleSentenceDto s : m.getSentences()) {
                        if (s.getImageId() != null) {
                            imageIds.add(s.getImageId());
                        }
                    }
                }
            }
        }
        if (dto.getStructures() != null) {
            for (GrammarStructureDto s : dto.getStructures()) {
                if (s.getSentences() != null) {
                    for (ExampleSentenceDto se : s.getSentences()) {
                        if (se.getImageId() != null) {
                            imageIds.add(se.getImageId());
                        }
                    }
                }
            }
        }
        if (dto.getNotices() != null) {
            for (GrammarNoticeDto n : dto.getNotices()) {
                if (n.getSentences() != null) {
                    for (ExampleSentenceDto se : n.getSentences()) {
                        if (se.getImageId() != null) {
                            imageIds.add(se.getImageId());
                        }
                    }
                }
            }
        }
        if (imageIds.isEmpty()) return Collections.emptyMap();
        List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
        return imageDtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, i -> i, (i, j) -> i));
    }

    // ===== 辨析组资源收集 =====

    private Map<Long, ExampleSentenceDto> collectComparisonSentences(List<GrammarComparisonGroupDto> comparisons) {
        List<Long> sentenceIds = new ArrayList<>();
        if (comparisons != null) {
            for (GrammarComparisonGroupDto group : comparisons) {
                if (group.getItems() != null) {
                    for (GrammarComparisonItemDto item : group.getItems()) {
                        if (item.getUsageSentenceId() != null) {
                            sentenceIds.add(item.getUsageSentenceId());
                        }
                    }
                }
            }
        }
        if (sentenceIds.isEmpty()) return Collections.emptyMap();
        return exampleSentenceService.findByIds(sentenceIds);
    }

    private List<Long> collectSentenceAudios(Map<Long, ExampleSentenceDto> sentenceMap) {
        List<Long> audioIds = new ArrayList<>();
        for (ExampleSentenceDto s : sentenceMap.values()) {
            if (s.getAudioId() != null) {
                audioIds.add(s.getAudioId());
            }
        }
        return audioIds;
    }

    private List<Long> collectSentenceImages(Map<Long, ExampleSentenceDto> sentenceMap) {
        List<Long> imageIds = new ArrayList<>();
        for (ExampleSentenceDto s : sentenceMap.values()) {
            if (s.getImageId() != null) {
                imageIds.add(s.getImageId());
            }
        }
        return imageIds;
    }

    private Map<Long, AudioResourceDto> mergeAudioMap(Map<Long, AudioResourceDto> existing, List<Long> newIds) {
        if (newIds.isEmpty()) return existing;
        List<Long> missing = newIds.stream()
                .filter(id -> !existing.containsKey(id))
                .collect(Collectors.toList());
        if (missing.isEmpty()) return existing;
        Map<Long, AudioResourceDto> merged = new java.util.HashMap<>(existing);
        List<AudioResourceDto> newDtos = audioResourceService.findByIds(missing);
        for (AudioResourceDto a : newDtos) {
            merged.put(a.getId(), a);
        }
        return merged;
    }

    private Map<Long, AliOssStorageDto> mergeImageMap(Map<Long, AliOssStorageDto> existing, List<Long> newIds) {
        if (newIds.isEmpty()) return existing;
        List<Long> missing = newIds.stream()
                .filter(id -> !existing.containsKey(id))
                .collect(Collectors.toList());
        if (missing.isEmpty()) return existing;
        Map<Long, AliOssStorageDto> merged = new java.util.HashMap<>(existing);
        List<AliOssStorageDto> newDtos = aliOssStorageService.findByIds(missing);
        for (AliOssStorageDto i : newDtos) {
            merged.put(i.getId(), i);
        }
        return merged;
    }
}
