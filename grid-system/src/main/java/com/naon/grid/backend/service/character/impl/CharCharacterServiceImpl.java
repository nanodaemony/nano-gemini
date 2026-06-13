package com.naon.grid.backend.service.character.impl;

import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.domain.character.CharComparison;
import com.naon.grid.backend.domain.character.CharWord;
import com.naon.grid.backend.repo.character.CharCharacterRepository;
import com.naon.grid.backend.repo.character.CharComparisonRepository;
import com.naon.grid.backend.repo.character.CharWordRepository;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.backend.service.character.dto.CharComparisonDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.character.mapstruct.CharCharacterMapper;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.SentenceBizTypeEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CharCharacterServiceImpl implements CharCharacterService {

    private static final String CHAR_WORD_SENTENCE_BIZ_TYPE = SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode();

    private final CharCharacterRepository charCharacterRepository;
    private final CharComparisonRepository charComparisonRepository;
    private final CharWordRepository charWordRepository;
    private final CharCharacterMapper charCharacterMapper;
    private final ExampleSentenceService exampleSentenceService;

    @Override
    public PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable) {
        Page<CharCharacter> page = charCharacterRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        PageResult<CharCharacterDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populateCharListStats(pageResult.getContent());
        return pageResult;
    }

    /**
     * 批量填充列表统计数据和状态：辨析数、组词数、翻译/拼音/音频状态
     */
    private void populateCharListStats(List<CharCharacterDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<Integer> ids = dtos.stream().map(CharCharacterDto::getId).collect(Collectors.toList());

        Map<Integer, Long> comparisonCountMap = charComparisonRepository
                .countByCharIdInGroupByCharId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Long) row[1]
                ));

        Map<Integer, Long> wordCountMap = charWordRepository
                .countByCharIdInGroupByCharId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Long) row[1]
                ));

        for (CharCharacterDto dto : dtos) {
            dto.setComparisonCount(comparisonCountMap.getOrDefault(dto.getId(), 0L).intValue());
            dto.setWordCount(wordCountMap.getOrDefault(dto.getId(), 0L).intValue());
            dto.setTranslationStatus(
                dto.getDescTranslations() != null && !dto.getDescTranslations().isEmpty()
                    ? "generated" : "not_generated"
            );
            dto.setPinyinStatus(
                dto.getPinyin() != null && !dto.getPinyin().isEmpty()
                    ? "generated" : "not_generated"
            );
            dto.setAudioStatus(
                dto.getAudioId() != null
                    ? "generated" : "not_generated"
            );
        }
    }

    /**
     * 主表实体 → DTO；若处于 draft/reviewed，将 draftContent 中的业务字段覆盖回 DTO。
     * 仅供 {@link #queryAll} 调用。
     */
    private CharCharacterDto toDtoWithDraftOverlay(CharCharacter entity) {
        CharCharacterDto dto = charCharacterMapper.toDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    /**
     * 把 draftContent JSON 中"列表页需要的业务字段"覆盖到 DTO 上。
     *
     * 覆盖范围应与 {@link com.naon.grid.backend.rest.vo.CharCharacterBaseVO} 暴露的业务字段保持一致。
     * BaseVO 新增业务字段时，请同步在此方法添加覆盖。
     *
     * 永远不覆盖：id、status、publishStatus、editStatus、createBy、updateBy、
     * createTime、updateTime —— 这些字段以主表为准。
     *
     * 不读取：comparisons、words —— 列表页不返回子表。
     *
     * @throws BadRequestException 草稿数据缺失或解析失败
     */
    private void applyDraftOverlay(CharCharacterDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        CharCharacterDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, CharCharacterDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        if (draft.getCharacter() != null)             dto.setCharacter(draft.getCharacter());
        if (draft.getHskLevel() != null)                 dto.setHskLevel(draft.getHskLevel());
        if (draft.getPinyin() != null)                dto.setPinyin(draft.getPinyin());
        if (draft.getAudioId() != null)               dto.setAudioId(draft.getAudioId());
        if (draft.getTraditional() != null)           dto.setTraditional(draft.getTraditional());
        if (draft.getRadicalId() != null)             dto.setRadicalId(draft.getRadicalId());
        if (draft.getRadical() != null)               dto.setRadical(draft.getRadical());
        if (draft.getComponentCombination() != null)  dto.setComponentCombination(draft.getComponentCombination());
        if (draft.getCharDesc() != null)              dto.setCharDesc(draft.getCharDesc());
        if (draft.getDescTranslations() != null)      dto.setDescTranslations(draft.getDescTranslations());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CharCharacterDto findById(Integer id) {
        if (id == null) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }
        CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
        if (charCharacter.getId() == null || StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }

        // If in DRAFT or REVIEWED status, return draftContent
        if (EditStatusEnum.DRAFT.getCode().equals(charCharacter.getEditStatus()) ||
            EditStatusEnum.REVIEWED.getCode().equals(charCharacter.getEditStatus())) {
            if (charCharacter.getDraftContent() == null) {
                throw new BadRequestException("Draft content not found");
            }
            CharCharacterDto dto = JsonUtils.fromJson(charCharacter.getDraftContent(), CharCharacterDto.class);
            dto.setId(charCharacter.getId());
            dto.setStatus(charCharacter.getStatus());
            dto.setPublishStatus(charCharacter.getPublishStatus());
            dto.setEditStatus(charCharacter.getEditStatus());
            dto.setCreateBy(charCharacter.getCreateBy());
            dto.setUpdateBy(charCharacter.getUpdateBy());
            dto.setCreateTime(charCharacter.getCreateTime());
            dto.setUpdateTime(charCharacter.getUpdateTime());
            dto.setComparisons(sortComparisonsDesc(dto.getComparisons()));
            dto.setWords(sortWordsDesc(dto.getWords()));
            return dto;
        }

        return toPublishedDetailDto(charCharacter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CharCharacterDto findPublishedById(Integer id) {
        if (id == null) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }
        CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
        if (charCharacter.getId() == null
            || StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())
            || !PublishStatusEnum.PUBLISHED.getCode().equals(charCharacter.getPublishStatus())) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }
        return toPublishedDetailDto(charCharacter);
    }

    private CharCharacterDto toPublishedDetailDto(CharCharacter charCharacter) {
        Integer id = charCharacter.getId();
        CharCharacterDto charCharacterDto = charCharacterMapper.toDto(charCharacter);
        charCharacterDto.setComparisons(sortComparisonsDesc(convertToComparisonDtos(charComparisonRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
        charCharacterDto.setWords(hydrateWordSentences(sortWordsDesc(convertToWordDtos(charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())))));
        return charCharacterDto;
    }

    private List<CharWordDto> hydrateWordSentences(List<CharWordDto> words) {
        if (words == null || words.isEmpty()) {
            return words;
        }
        List<Long> wordIds = words.stream()
                .map(CharWordDto::getId)
                .filter(id -> id != null)
                .map(Integer::longValue)
                .collect(Collectors.toList());
        if (wordIds.isEmpty()) {
            return words;
        }
        Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByBizIds(CHAR_WORD_SENTENCE_BIZ_TYPE, wordIds);
        if (sentenceMap == null || sentenceMap.isEmpty()) {
            return words;
        }
        for (CharWordDto word : words) {
            if (word.getId() != null) {
                word.setWordItemSentence(sentenceMap.get(word.getId().longValue()));
            }
        }
        return words;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(CharCharacterDto resources) {
        CharCharacter charCharacter = new CharCharacter();
        charCharacter.setStatus(StatusEnum.ENABLED.getCode());
        charCharacter.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        charCharacter.setEditStatus(EditStatusEnum.DRAFT.getCode());
        charCharacter.setCharacter(resources.getCharacter());
        charCharacter.setDraftContent(JsonUtils.toJson(resources));
        charCharacter = charCharacterRepository.save(charCharacter);
        return charCharacter.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, CharCharacterDto resources) {
        CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
        if (charCharacter.getId() == null || StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }

        // If current status is REVIEWED or PUBLISHED, revert to DRAFT
        if (EditStatusEnum.REVIEWED.getCode().equals(charCharacter.getEditStatus()) ||
            EditStatusEnum.PUBLISHED.getCode().equals(charCharacter.getEditStatus())) {
            charCharacter.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        charCharacter.setDraftContent(JsonUtils.toJson(resources));
        charCharacterRepository.save(charCharacter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
        if (charCharacter.getId() == null) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }
        // Only set status to DISABLED, don't change child tables or publishStatus
        charCharacter.setStatus(StatusEnum.DISABLED.getCode());
        charCharacterRepository.save(charCharacter);
    }

    @Override
    public List<CharCharacterDto> searchByCharacter(String blurry) {
        List<CharCharacter> characters = charCharacterRepository.findByCharacterContainingAndStatus(blurry, StatusEnum.ENABLED.getCode());
        return characters.stream().map(charCharacterMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<CharCharacterDto> searchPublishedByCharacter(String blurry) {
        List<CharCharacter> characters = charCharacterRepository.findByCharacterContainingAndStatusAndPublishStatus(blurry, StatusEnum.ENABLED.getCode(), PublishStatusEnum.PUBLISHED.getCode());
        return characters.stream().map(charCharacterMapper::toDto).collect(Collectors.toList());
    }

    private void syncComparisons(Integer charId, List<CharComparisonDto> submittedDtos) {
        List<CharComparisonDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<CharComparison> existing = charComparisonRepository.findByCharIdAndStatus(charId, StatusEnum.ENABLED.getCode());
        Map<Integer, CharComparison> existingMap = new HashMap<>();
        for (CharComparison comparison : existing) {
            existingMap.put(comparison.getId(), comparison);
        }

        Set<Integer> submittedIds = new HashSet<>();
        List<CharComparison> toSave = new ArrayList<>();

        for (CharComparisonDto dto : submitted) {
            if (dto.getId() == null) {
                toSave.add(convertToComparisonEntity(dto, charId));
                continue;
            }
            if (!submittedIds.add(dto.getId())) {
                throw new BadRequestException("辨析ID重复: " + dto.getId());
            }
            CharComparison comparison = existingMap.get(dto.getId());
            if (comparison == null) {
                throw new BadRequestException("辨析ID不属于当前汉字: " + dto.getId());
            }
            updateComparison(comparison, dto);
            toSave.add(comparison);
        }

        List<CharComparison> toDelete = new ArrayList<>();
        for (CharComparison comparison : existing) {
            if (!submittedIds.contains(comparison.getId())) {
                toDelete.add(comparison);
            }
        }

        for (CharComparison comparison : toDelete) {
            comparison.setStatus(StatusEnum.DISABLED.getCode());
            charComparisonRepository.save(comparison);
        }
        charComparisonRepository.saveAll(toSave);
    }

    private List<CharWord> syncWords(Integer charId, List<CharWordDto> submittedDtos) {
        List<CharWordDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<CharWord> existing = charWordRepository.findByCharIdAndStatus(charId, StatusEnum.ENABLED.getCode());
        Map<Integer, CharWord> existingMap = new HashMap<>();
        for (CharWord word : existing) {
            existingMap.put(word.getId(), word);
        }

        Set<Integer> submittedIds = new HashSet<>();
        List<CharWord> toSave = new ArrayList<>();

        for (CharWordDto dto : submitted) {
            if (dto.getId() == null) {
                toSave.add(convertToWordEntity(dto, charId));
                continue;
            }
            if (!submittedIds.add(dto.getId())) {
                throw new BadRequestException("组词ID重复: " + dto.getId());
            }
            CharWord word = existingMap.get(dto.getId());
            if (word == null) {
                throw new BadRequestException("组词ID不属于当前汉字: " + dto.getId());
            }
            updateWord(word, dto);
            toSave.add(word);
        }

        List<CharWord> toDelete = new ArrayList<>();
        for (CharWord word : existing) {
            if (!submittedIds.contains(word.getId())) {
                toDelete.add(word);
            }
        }

        for (CharWord word : toDelete) {
            word.setStatus(StatusEnum.DISABLED.getCode());
            charWordRepository.save(word);
        }
        disableWordSentences(toDelete);

        Iterable<CharWord> savedIterable = charWordRepository.saveAll(toSave);
        List<CharWord> savedWords = new ArrayList<>();
        if (savedIterable != null) {
            for (CharWord word : savedIterable) {
                savedWords.add(word);
            }
        }
        return savedWords;
    }

    private void disableWordSentences(Collection<CharWord> words) {
        if (words == null || words.isEmpty()) {
            return;
        }
        List<Long> wordIds = words.stream()
                .map(CharWord::getId)
                .filter(id -> id != null)
                .map(Integer::longValue)
                .collect(Collectors.toList());
        if (!wordIds.isEmpty()) {
            exampleSentenceService.disableByBizIds(CHAR_WORD_SENTENCE_BIZ_TYPE, wordIds);
        }
    }

    private void syncWordSentences(List<CharWord> savedWords, List<CharWordDto> submittedDtos) {
        if (savedWords == null || savedWords.isEmpty() || submittedDtos == null || submittedDtos.isEmpty()) {
            return;
        }
        int pairCount = Math.min(savedWords.size(), submittedDtos.size());
        for (int i = 0; i < pairCount; i++) {
            CharWord savedWord = savedWords.get(i);
            CharWordDto submittedDto = submittedDtos.get(i);
            if (savedWord == null || savedWord.getId() == null || submittedDto == null) {
                continue;
            }
            exampleSentenceService.syncOne(
                    CHAR_WORD_SENTENCE_BIZ_TYPE,
                    savedWord.getId().longValue(),
                    submittedDto.getWordItemSentence()
            );
        }
    }

    private List<CharComparisonDto> convertToComparisonDtos(List<CharComparison> comparisons) {
        List<CharComparisonDto> comparisonDtos = new ArrayList<>();
        for (CharComparison comparison : comparisons) {
            CharComparisonDto comparisonDto = convertToComparisonDto(comparison);
            comparisonDtos.add(comparisonDto);
        }
        return comparisonDtos;
    }

    private CharComparisonDto convertToComparisonDto(CharComparison comparison) {
        CharComparisonDto dto = new CharComparisonDto();
        dto.setId(comparison.getId());
        dto.setCharId(comparison.getCharId());
        dto.setComparisonChar(comparison.getComparisonChar());
        dto.setComparisonPinyin(comparison.getComparisonPinyin());
        dto.setComparisonCharTranslations(JsonUtils.parseTranslationList(comparison.getComparisonCharTranslations()));
        dto.setComparisonDescTranslations(JsonUtils.parseTranslationList(comparison.getComparisonDescTranslations()));
        dto.setOrder(comparison.getComparisonOrder());
        dto.setCreateTime(comparison.getCreateTime());
        dto.setUpdateTime(comparison.getUpdateTime());
        dto.setStatus(comparison.getStatus());
        return dto;
    }

    private List<CharWordDto> convertToWordDtos(List<CharWord> words) {
        List<CharWordDto> wordDtos = new ArrayList<>();
        for (CharWord word : words) {
            CharWordDto wordDto = convertToWordDto(word);
            wordDtos.add(wordDto);
        }
        return wordDtos;
    }

    private CharWordDto convertToWordDto(CharWord word) {
        CharWordDto dto = new CharWordDto();
        dto.setId(word.getId());
        dto.setCharId(word.getCharId());
        dto.setWordItem(word.getWordItem());
        dto.setHskLevel(word.getLevel());
        dto.setPinyin(word.getPinyin());
        dto.setPartOfSpeech(word.getPartOfSpeech());
        dto.setWordItemTranslations(JsonUtils.parseTranslationList(word.getWordItemTranslations()));
        dto.setWordOrder(word.getWordOrder());
        dto.setCreateTime(word.getCreateTime());
        dto.setUpdateTime(word.getUpdateTime());
        dto.setStatus(word.getStatus());
        return dto;
    }

    private void updateComparison(CharComparison entity, CharComparisonDto dto) {
        entity.setComparisonChar(dto.getComparisonChar());
        entity.setComparisonPinyin(dto.getComparisonPinyin());
        entity.setComparisonCharTranslations(JsonUtils.toTranslationJson(dto.getComparisonCharTranslations()));
        entity.setComparisonDescTranslations(JsonUtils.toTranslationJson(dto.getComparisonDescTranslations()));
        entity.setComparisonOrder(dto.getOrder() != null ? dto.getOrder() : 0);
    }

    private void updateWord(CharWord entity, CharWordDto dto) {
        entity.setWordItem(dto.getWordItem());
        entity.setLevel(dto.getHskLevel());
        entity.setPinyin(dto.getPinyin());
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setWordItemTranslations(JsonUtils.toTranslationJson(dto.getWordItemTranslations()));
        entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
    }

    private CharComparison convertToComparisonEntity(CharComparisonDto dto, Integer charId) {
        CharComparison entity = new CharComparison();
        entity.setCharId(charId);
        entity.setComparisonChar(dto.getComparisonChar());
        entity.setComparisonPinyin(dto.getComparisonPinyin());
        entity.setComparisonCharTranslations(JsonUtils.toTranslationJson(dto.getComparisonCharTranslations()));
        entity.setComparisonDescTranslations(JsonUtils.toTranslationJson(dto.getComparisonDescTranslations()));
        entity.setComparisonOrder(dto.getOrder() != null ? dto.getOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

    private CharWord convertToWordEntity(CharWordDto dto, Integer charId) {
        CharWord entity = new CharWord();
        entity.setCharId(charId);
        entity.setWordItem(dto.getWordItem());
        entity.setLevel(dto.getHskLevel());
        entity.setPinyin(dto.getPinyin());
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setWordItemTranslations(JsonUtils.toTranslationJson(dto.getWordItemTranslations()));
        entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

    private List<CharComparisonDto> sortComparisonsDesc(List<CharComparisonDto> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        list.sort(Comparator.comparing(
            CharComparisonDto::getOrder,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return list;
    }

    private List<CharWordDto> sortWordsDesc(List<CharWordDto> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        list.sort(Comparator.comparing(
            CharWordDto::getWordOrder,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Integer id) {
        CharCharacter charCharacter = charCharacterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }

        if (charCharacter.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }

        if (!EditStatusEnum.DRAFT.getCode().equals(charCharacter.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }

        charCharacter.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        charCharacterRepository.save(charCharacter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Integer id) {
        CharCharacter charCharacter = charCharacterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }

        if (charCharacter.getDraftContent() == null) {
            throw new BadRequestException("Draft content not found");
        }

        if (!EditStatusEnum.REVIEWED.getCode().equals(charCharacter.getEditStatus())) {
            throw new BadRequestException("Only reviewed drafts can be published");
        }

        // Parse draft content
        CharCharacterDto draftDto = JsonUtils.fromJson(charCharacter.getDraftContent(), CharCharacterDto.class);

        // Update main table fields (character字段不更新)
        charCharacter.setLevel(draftDto.getHskLevel());
        charCharacter.setPinyin(draftDto.getPinyin());
        charCharacter.setAudioId(draftDto.getAudioId());
        charCharacter.setTraditional(draftDto.getTraditional());
        charCharacter.setRadicalId(draftDto.getRadicalId());
        charCharacter.setRadical(draftDto.getRadical());
        charCharacter.setComponentCombination(draftDto.getComponentCombination());
        charCharacter.setCharDesc(draftDto.getCharDesc());
        charCharacter.setDescTranslations(JsonUtils.toTranslationJson(draftDto.getDescTranslations()));

        // Update child tables
        syncComparisons(id, draftDto.getComparisons());
        List<CharWord> savedWords = syncWords(id, draftDto.getWords());
        syncWordSentences(savedWords, draftDto.getWords());

        // Update status
        charCharacter.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        charCharacter.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        charCharacter.setDraftContent(null);
        charCharacterRepository.save(charCharacter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Integer id) {
        CharCharacter charCharacter = charCharacterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }

        // Only update publish status, don't change child tables
        charCharacter.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        charCharacterRepository.save(charCharacter);
    }

}
