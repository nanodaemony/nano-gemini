package com.naon.grid.backend.service.character.impl;

import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.domain.character.CharDiscrimination;
import com.naon.grid.backend.domain.character.CharWord;
import com.naon.grid.backend.repo.character.CharCharacterRepository;
import com.naon.grid.backend.repo.character.CharDiscriminationRepository;
import com.naon.grid.backend.repo.character.CharWordRepository;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.backend.service.character.dto.CharDiscriminationDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.character.mapstruct.CharCharacterMapper;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
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
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CharCharacterServiceImpl implements CharCharacterService {

    private final CharCharacterRepository charCharacterRepository;
    private final CharDiscriminationRepository charDiscriminationRepository;
    private final CharWordRepository charWordRepository;
    private final CharCharacterMapper charCharacterMapper;

    @Override
    public PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable) {
        Page<CharCharacter> page = charCharacterRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        return PageUtil.toPage(page.map(charCharacterMapper::toDto));
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
            dto.setDiscriminations(sortDiscriminationsDesc(dto.getDiscriminations()));
            dto.setWords(sortWordsDesc(dto.getWords()));
            return dto;
        }

        // If in PUBLISHED status, return main table + child tables
        CharCharacterDto charCharacterDto = charCharacterMapper.toDto(charCharacter);
        charCharacterDto.setDiscriminations(sortDiscriminationsDesc(convertToDiscriminationDtos(charDiscriminationRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
        charCharacterDto.setWords(sortWordsDesc(convertToWordDtos(charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
        return charCharacterDto;
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
        CharCharacterDto charCharacterDto = charCharacterMapper.toDto(charCharacter);
        charCharacterDto.setDiscriminations(sortDiscriminationsDesc(convertToDiscriminationDtos(charDiscriminationRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
        charCharacterDto.setWords(sortWordsDesc(convertToWordDtos(charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
        return charCharacterDto;
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
        return characters.stream().map(charCharacterMapper::toDto).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<CharCharacterDto> searchPublishedByCharacter(String blurry) {
        List<CharCharacter> characters = charCharacterRepository.findByCharacterContainingAndStatusAndPublishStatus(blurry, StatusEnum.ENABLED.getCode(), "published");
        return characters.stream().map(charCharacterMapper::toDto).collect(java.util.stream.Collectors.toList());
    }

    private void saveChildren(CharCharacterDto resources, Integer charId) {
        if (resources.getDiscriminations() != null) {
            for (CharDiscriminationDto discriminationDto : resources.getDiscriminations()) {
                CharDiscrimination discrimination = convertToDiscriminationEntity(discriminationDto, charId);
                charDiscriminationRepository.save(discrimination);
            }
        }
        if (resources.getWords() != null) {
            for (CharWordDto wordDto : resources.getWords()) {
                CharWord word = convertToWordEntity(wordDto, charId);
                charWordRepository.save(word);
            }
        }
    }

    private void deleteChildren(Integer charId) {
        List<CharDiscrimination> discriminations = charDiscriminationRepository.findByCharIdAndStatus(charId, StatusEnum.ENABLED.getCode());
        for (CharDiscrimination d : discriminations) {
            d.setStatus(StatusEnum.DISABLED.getCode());
            charDiscriminationRepository.save(d);
        }
        List<CharWord> words = charWordRepository.findByCharIdAndStatus(charId, StatusEnum.ENABLED.getCode());
        for (CharWord w : words) {
            w.setStatus(StatusEnum.DISABLED.getCode());
            charWordRepository.save(w);
        }
    }

    private void syncDiscriminations(Integer charId, List<CharDiscriminationDto> submittedDtos) {
        List<CharDiscriminationDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<CharDiscrimination> existing = charDiscriminationRepository.findByCharIdAndStatus(charId, StatusEnum.ENABLED.getCode());
        Map<Integer, CharDiscrimination> existingMap = new HashMap<>();
        for (CharDiscrimination discrimination : existing) {
            existingMap.put(discrimination.getId(), discrimination);
        }

        Set<Integer> submittedIds = new HashSet<>();
        List<CharDiscrimination> toSave = new ArrayList<>();

        for (CharDiscriminationDto dto : submitted) {
            if (dto.getId() == null) {
                toSave.add(convertToDiscriminationEntity(dto, charId));
                continue;
            }
            if (!submittedIds.add(dto.getId())) {
                throw new BadRequestException("辨析ID重复: " + dto.getId());
            }
            CharDiscrimination discrimination = existingMap.get(dto.getId());
            if (discrimination == null) {
                throw new BadRequestException("辨析ID不属于当前汉字: " + dto.getId());
            }
            updateDiscrimination(discrimination, dto);
            toSave.add(discrimination);
        }

        List<CharDiscrimination> toDelete = new ArrayList<>();
        for (CharDiscrimination discrimination : existing) {
            if (!submittedIds.contains(discrimination.getId())) {
                toDelete.add(discrimination);
            }
        }

        for (CharDiscrimination d : toDelete) {
            d.setStatus(StatusEnum.DISABLED.getCode());
            charDiscriminationRepository.save(d);
        }
        charDiscriminationRepository.saveAll(toSave);
    }

    private void syncWords(Integer charId, List<CharWordDto> submittedDtos) {
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

        for (CharWord w : toDelete) {
            w.setStatus(StatusEnum.DISABLED.getCode());
            charWordRepository.save(w);
        }
        charWordRepository.saveAll(toSave);
    }

    private List<CharDiscriminationDto> convertToDiscriminationDtos(List<CharDiscrimination> discriminations) {
        List<CharDiscriminationDto> discriminationDtos = new ArrayList<>();
        for (CharDiscrimination discrimination : discriminations) {
            CharDiscriminationDto discriminationDto = convertToDiscriminationDto(discrimination);
            discriminationDtos.add(discriminationDto);
        }
        return discriminationDtos;
    }

    private CharDiscriminationDto convertToDiscriminationDto(CharDiscrimination discrimination) {
        CharDiscriminationDto dto = new CharDiscriminationDto();
        dto.setId(discrimination.getId());
        dto.setCharId(discrimination.getCharId());
        dto.setDiscrimChar(discrimination.getDiscrimChar());
        dto.setDiscrimPinyin(discrimination.getDiscrimPinyin());
        dto.setDiscrimCharTranslations(JsonUtils.parseTranslationList(discrimination.getDiscrimCharTranslations()));
        dto.setComparisonTranslations(JsonUtils.parseTranslationList(discrimination.getComparisonTranslations()));
        dto.setDiscriminationOrder(discrimination.getDiscriminationOrder());
        dto.setCreateTime(discrimination.getCreateTime());
        dto.setUpdateTime(discrimination.getUpdateTime());
        dto.setStatus(discrimination.getStatus());
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
        dto.setLevel(word.getLevel());
        dto.setPinyin(word.getPinyin());
        dto.setPartOfSpeech(word.getPartOfSpeech());
        dto.setWordItemTranslations(JsonUtils.parseTranslationList(word.getWordItemTranslations()));
        dto.setExampleSentence(word.getExampleSentence());
        dto.setExamplePinyin(word.getExamplePinyin());
        dto.setExampleTranslations(JsonUtils.parseTranslationList(word.getExampleTranslations()));
        dto.setExampleImage(word.getExampleImage());
        dto.setWordOrder(word.getWordOrder());
        dto.setCreateTime(word.getCreateTime());
        dto.setUpdateTime(word.getUpdateTime());
        dto.setStatus(word.getStatus());
        return dto;
    }

    private void updateDiscrimination(CharDiscrimination entity, CharDiscriminationDto dto) {
        entity.setDiscrimChar(dto.getDiscrimChar());
        entity.setDiscrimPinyin(dto.getDiscrimPinyin());
        entity.setDiscrimCharTranslations(JsonUtils.toTranslationJson(dto.getDiscrimCharTranslations()));
        entity.setComparisonTranslations(JsonUtils.toTranslationJson(dto.getComparisonTranslations()));
        entity.setDiscriminationOrder(dto.getDiscriminationOrder() != null ? dto.getDiscriminationOrder() : 0);
    }

    private void updateWord(CharWord entity, CharWordDto dto) {
        entity.setWordItem(dto.getWordItem());
        entity.setLevel(dto.getLevel());
        entity.setPinyin(dto.getPinyin());
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setWordItemTranslations(JsonUtils.toTranslationJson(dto.getWordItemTranslations()));
        entity.setExampleSentence(dto.getExampleSentence());
        entity.setExamplePinyin(dto.getExamplePinyin());
        entity.setExampleTranslations(JsonUtils.toTranslationJson(dto.getExampleTranslations()));
        entity.setExampleImage(dto.getExampleImage());
        entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
    }

    private CharDiscrimination convertToDiscriminationEntity(CharDiscriminationDto dto, Integer charId) {
        CharDiscrimination entity = new CharDiscrimination();
        entity.setCharId(charId);
        entity.setDiscrimChar(dto.getDiscrimChar());
        entity.setDiscrimPinyin(dto.getDiscrimPinyin());
        entity.setDiscrimCharTranslations(JsonUtils.toTranslationJson(dto.getDiscrimCharTranslations()));
        entity.setComparisonTranslations(JsonUtils.toTranslationJson(dto.getComparisonTranslations()));
        entity.setDiscriminationOrder(dto.getDiscriminationOrder() != null ? dto.getDiscriminationOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

    private CharWord convertToWordEntity(CharWordDto dto, Integer charId) {
        CharWord entity = new CharWord();
        entity.setCharId(charId);
        entity.setWordItem(dto.getWordItem());
        entity.setLevel(dto.getLevel());
        entity.setPinyin(dto.getPinyin());
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setWordItemTranslations(JsonUtils.toTranslationJson(dto.getWordItemTranslations()));
        entity.setExampleSentence(dto.getExampleSentence());
        entity.setExamplePinyin(dto.getExamplePinyin());
        entity.setExampleTranslations(JsonUtils.toTranslationJson(dto.getExampleTranslations()));
        entity.setExampleImage(dto.getExampleImage());
        entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

    private List<CharDiscriminationDto> sortDiscriminationsDesc(List<CharDiscriminationDto> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        list.sort(Comparator.comparing(
            CharDiscriminationDto::getDiscriminationOrder,
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
        charCharacter.setSequenceNo(draftDto.getSequenceNo());
        charCharacter.setLevel(draftDto.getLevel());
        charCharacter.setPinyin(draftDto.getPinyin());
        charCharacter.setAudioId(draftDto.getAudioId());
        charCharacter.setTraditional(draftDto.getTraditional());
        charCharacter.setRadical(draftDto.getRadical());
        charCharacter.setStroke(draftDto.getStroke());
        charCharacter.setCharDesc(draftDto.getCharDesc());
        charCharacter.setDescTranslations(JsonUtils.toTranslationJson(draftDto.getDescTranslations()));

        // Update child tables
        syncDiscriminations(id, draftDto.getDiscriminations());
        syncWords(id, draftDto.getWords());

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
