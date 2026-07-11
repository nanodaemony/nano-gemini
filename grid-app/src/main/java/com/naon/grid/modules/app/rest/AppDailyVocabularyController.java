package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.vocabulary.DailyVocabularyService;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyQueryCriteria;
import com.naon.grid.modules.app.rest.request.AppDailyVocabularyHistoryRequest;
import com.naon.grid.modules.app.rest.request.AppDailyVocabularyShareImageRequest;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyBaseVO;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyDetailVO;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyTodayVO;
import com.naon.grid.modules.app.rest.wrapper.AppDailyVocabularyWrapper;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.utils.PageResult;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/daily-vocabulary")
@Api(tags = "用户：每日一词接口")
public class AppDailyVocabularyController {

    private final DailyVocabularyService dailyVocabularyService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;

    @ApiOperation("今日每日一词（主推+备选池）")
    @AnonymousGetMapping("/today")
    public ResponseEntity<AppDailyVocabularyTodayVO> today(@RequestParam(defaultValue = "zh") String language) {
        DailyVocabularyDto main = dailyVocabularyService.getTodayMain();
        List<DailyVocabularyDto> backups = dailyVocabularyService.getTodayBackups();

        // 收集音频/图片 ID
        List<Long> audioIds = collectAudioIds(main, backups);
        List<Long> imageIds = collectImageIds(main, backups);

        Map<Long, AudioResourceDto> audioMap = batchQueryAudios(audioIds);
        Map<Long, AliOssStorageDto> imageMap = batchQueryImages(imageIds);

        AppDailyVocabularyTodayVO vo = AppDailyVocabularyWrapper.toTodayVO(main, backups, audioMap, imageMap, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    @ApiOperation("每日一词详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppDailyVocabularyDetailVO> detail(@PathVariable Integer id,
                                                              @RequestParam String language) {
        DailyVocabularyDto dto = dailyVocabularyService.findPublishedById(id);
        Map<Long, AudioResourceDto> audioMap = batchQueryAudios(collectAudioIds(dto, null));
        Map<Long, AliOssStorageDto> imageMap = batchQueryImages(collectImageIds(dto, null));
        AppDailyVocabularyDetailVO vo = AppDailyVocabularyWrapper.toDetailVO(dto, audioMap, imageMap, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    @ApiOperation("历史归档")
    @AnonymousGetMapping("/history")
    public ResponseEntity<PageResult<AppDailyVocabularyBaseVO>> history(
            AppDailyVocabularyHistoryRequest request) {
        DailyVocabularyQueryCriteria criteria = new DailyVocabularyQueryCriteria();
        criteria.setPhraseType(request.getPhraseType());
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            criteria.setBlurry(request.getKeyword());
        }
        if (request.getMonth() != null && !request.getMonth().isEmpty()) {
            YearMonth ym = YearMonth.parse(request.getMonth());
            criteria.setDisplayDateStart(ym.atDay(1));
            criteria.setDisplayDateEnd(ym.atEndOfMonth());
        }
        criteria.setPublishedOnly(true);

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "displayDate"));

        PageResult<DailyVocabularyDto> pageResult = dailyVocabularyService.queryHistory(criteria, pageable);

        // 批量查询图片
        List<Long> imageIds = pageResult.getContent().stream()
                .map(DailyVocabularyDto::getImageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, AliOssStorageDto> imageMap = batchQueryImages(imageIds);

        List<AppDailyVocabularyBaseVO> vos = AppDailyVocabularyWrapper.toBaseVOList(
                pageResult.getContent(), imageMap);
        return new ResponseEntity<>(
                new PageResult<>(vos, pageResult.getTotalElements()), HttpStatus.OK);
    }

    @ApiOperation("日历视图（月有内容日期）")
    @AnonymousGetMapping("/calendar")
    public ResponseEntity<List<LocalDate>> calendar(@RequestParam int year, @RequestParam int month) {
        List<LocalDate> dates = dailyVocabularyService.getCalendarDates(year, month);
        return new ResponseEntity<>(dates, HttpStatus.OK);
    }

    @ApiOperation("保存分享图")
    @AnonymousPostMapping("/{id}/share-image")
    public ResponseEntity<Void> saveShareImage(@PathVariable Integer id,
                                                @RequestBody AppDailyVocabularyShareImageRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        dailyVocabularyService.saveShareImage(id, request.getImageId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ==================== 私有辅助 ====================

    private List<Long> collectAudioIds(DailyVocabularyDto main, List<DailyVocabularyDto> backups) {
        List<Long> ids = new ArrayList<>();
        if (main != null && main.getAudioId() != null) ids.add(main.getAudioId());
        if (main != null && main.getExampleSentence() != null
                && main.getExampleSentence().getAudioId() != null) {
            ids.add(main.getExampleSentence().getAudioId());
        }
        if (backups != null) {
            for (DailyVocabularyDto d : backups) {
                if (d.getAudioId() != null) ids.add(d.getAudioId());
                if (d.getExampleSentence() != null && d.getExampleSentence().getAudioId() != null) {
                    ids.add(d.getExampleSentence().getAudioId());
                }
            }
        }
        return ids;
    }

    private List<Long> collectImageIds(DailyVocabularyDto main, List<DailyVocabularyDto> backups) {
        List<Long> ids = new ArrayList<>();
        if (main != null && main.getImageId() != null) ids.add(main.getImageId());
        if (main != null && main.getExampleSentence() != null
                && main.getExampleSentence().getImageId() != null) {
            ids.add(main.getExampleSentence().getImageId());
        }
        if (backups != null) {
            for (DailyVocabularyDto d : backups) {
                if (d.getImageId() != null) ids.add(d.getImageId());
                if (d.getExampleSentence() != null && d.getExampleSentence().getImageId() != null) {
                    ids.add(d.getExampleSentence().getImageId());
                }
            }
        }
        return ids;
    }

    private Map<Long, AudioResourceDto> batchQueryAudios(List<Long> audioIds) {
        if (audioIds.isEmpty()) return Collections.emptyMap();
        List<AudioResourceDto> dtos = audioResourceService.findByIds(audioIds);
        return dtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
    }

    private Map<Long, AliOssStorageDto> batchQueryImages(List<Long> imageIds) {
        if (imageIds.isEmpty()) return Collections.emptyMap();
        List<AliOssStorageDto> dtos = aliOssStorageService.findByIds(imageIds);
        return dtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, i -> i, (a, b) -> a));
    }
}
