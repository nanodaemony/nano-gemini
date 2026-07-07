package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.repo.vocabulary.VocabSenseRepository;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.modules.app.rest.request.AppVocabWordSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppVocabWordBaseVO;
import com.naon.grid.modules.app.rest.vo.AppVocabWordDetailVO;
import com.naon.grid.modules.app.rest.vo.AppExerciseQuestionDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppVocabWordWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppVocabChallengeWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppVocabChallengeWrapper.ChallengeItem;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final GridUserRepository gridUserRepository;
    private final VocabSenseRepository vocabSenseRepository;

    @ApiOperation("搜索词汇")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppVocabWordBaseVO>> search(AppVocabWordSearchRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus("published");
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"));
        List<VocabWordDto> dtos = vocabWordService.queryAll(criteria, pageable).getContent();
        List<AppVocabWordBaseVO> vos = AppVocabWordWrapper.toBaseVOList(dtos);

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
        AppVocabWordDetailVO vo = AppVocabWordWrapper.toDetailVO(dto, audioMap, imageMap, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    @ApiOperation("词汇大挑战（10道图片选择题）")
    @GetMapping("/challenge")
    public ResponseEntity<List<AppExerciseQuestionDetailVO>> challenge() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        GridUser user = gridUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(GridUser.class, "id", userId.toString()));
        String hskLevel = user.getHskLevel();

        // 确定出题等级
        String levelA, levelB;
        if (hskLevel == null || "0".equals(hskLevel)) {
            levelA = "4";
            levelB = "5";
        } else {
            int n;
            try {
                n = Integer.parseInt(hskLevel);
            } catch (NumberFormatException e) {
                log.warn("Invalid HSK level '{}' for user {}, falling back to default", hskLevel, userId);
                levelA = "4";
                levelB = "5";
                n = -1; // sentinel to skip remaining level logic
            }
            if (n >= 9) {
                levelA = "9";
                levelB = "9";
            } else if (n > 0) {
                levelA = hskLevel;
                levelB = String.valueOf(n + 1);
            }
        }

        List<VocabWordDto> answerWords = new ArrayList<>();
        answerWords.addAll(vocabWordService.findRandomPublishedWithImage(levelA, 5));
        answerWords.addAll(vocabWordService.findRandomPublishedWithImage(levelB, 5));

        // 同一等级时去重并补足
        if (levelA.equals(levelB)) {
            List<VocabWordDto> deduped = new ArrayList<>();
            Set<Integer> seen = new HashSet<>();
            for (VocabWordDto w : answerWords) {
                if (seen.add(w.getId())) {
                    deduped.add(w);
                }
            }
            answerWords = deduped;
            if (answerWords.size() < 10) {
                int need = 10 - answerWords.size();
                List<VocabWordDto> extra = vocabWordService.findRandomPublishedWithImage(levelA, need + 5);
                for (VocabWordDto w : extra) {
                    if (answerWords.size() >= 10) break;
                    if (seen.add(w.getId())) {
                        answerWords.add(w);
                    }
                }
            }
        }

        if (answerWords.size() > 10) {
            answerWords = answerWords.subList(0, 10);
        }

        // 组装每道题
        List<ChallengeItem> items = new ArrayList<>();
        List<Long> imageIds = new ArrayList<>();

        for (VocabWordDto answerWord : answerWords) {
            Long defImageId = findFirstImageId(answerWord.getId());
            if (defImageId != null) {
                imageIds.add(defImageId);
            }
            String wordLevel = answerWord.getHskLevel() != null ? answerWord.getHskLevel() : levelA;
            List<VocabWordDto> distractors = getDistractors(wordLevel, 3,
                    Collections.singletonList(answerWord.getId()));
            items.add(new ChallengeItem(answerWord, defImageId, distractors));
        }

        Map<Long, AliOssStorageDto> imageMap = batchQueryImages(imageIds);
        List<AppExerciseQuestionDetailVO> vos = AppVocabChallengeWrapper.toChallengeVOList(items, imageMap);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    private Long findFirstImageId(Integer wordId) {
        List<com.naon.grid.backend.domain.vocabulary.VocabSense> senses =
                vocabSenseRepository.findByWordIdAndStatus(wordId, 1);
        for (com.naon.grid.backend.domain.vocabulary.VocabSense sense : senses) {
            if (sense.getDefImageId() != null) {
                return sense.getDefImageId();
            }
        }
        return null;
    }

    private List<VocabWordDto> getDistractors(String hskLevel, int count, List<Integer> excludeIds) {
        int fetchCount = count + (excludeIds != null ? excludeIds.size() : 0) + 5;
        List<VocabWordDto> candidates = vocabWordService.findRandomPublishedWithImage(
                hskLevel, Math.min(fetchCount, 50));
        List<VocabWordDto> result = new ArrayList<>();
        for (VocabWordDto w : candidates) {
            if (result.size() >= count) break;
            boolean excluded = false;
            if (excludeIds != null) {
                for (Integer eid : excludeIds) {
                    if (eid.equals(w.getId())) {
                        excluded = true;
                        break;
                    }
                }
            }
            if (!excluded) {
                result.add(w);
            }
        }
        if (result.size() < count) {
            log.warn("Only {} distractors available for HSK level {}, needed {}", result.size(), hskLevel, count);
        }
        return result;
    }

    private Map<Long, AliOssStorageDto> batchQueryImages(List<Long> imageIds) {
        if (imageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
        return imageDtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, i -> i, (a, b) -> a));
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
}
