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
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
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
        CharCharacterDto charCharacterDto = charCharacterMapper.toDto(charCharacter);
        charCharacterDto.setDiscriminations(convertToDiscriminationDtos(charDiscriminationRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())));
        charCharacterDto.setWords(convertToWordDtos(charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())));
        return charCharacterDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(CharCharacterDto resources) {
        CharCharacter charCharacter = charCharacterMapper.toEntity(resources);
        charCharacter.setStatus(StatusEnum.ENABLED.getCode());
        charCharacter = charCharacterRepository.save(charCharacter);
        saveChildren(resources, charCharacter.getId());
        return charCharacter.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, CharCharacterDto resources) {
        CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
        if (charCharacter.getId() == null) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }
        charCharacter.setSequenceNo(resources.getSequenceNo());
        charCharacter.setCharacter(resources.getCharacter());
        charCharacter.setLevel(resources.getLevel());
        charCharacter.setPinyin(resources.getPinyin());
        charCharacter.setAudioId(resources.getAudioId());
        charCharacter.setTraditional(resources.getTraditional());
        charCharacter.setRadical(resources.getRadical());
        charCharacter.setStroke(resources.getStroke());
        charCharacter.setCharDesc(resources.getCharDesc());
        charCharacter.setDescTranslations(resources.getDescTranslations());
        charCharacterRepository.save(charCharacter);
        syncDiscriminations(id, resources.getDiscriminations());
        syncWords(id, resources.getWords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
        if (charCharacter.getId() == null) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }
        deleteChildren(id);
        charCharacter.setStatus(StatusEnum.DISABLED.getCode());
        charCharacterRepository.save(charCharacter);
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
        dto.setDiscrimCharTranslations(discrimination.getDiscrimCharTranslations());
        dto.setComparisonTranslations(discrimination.getComparisonTranslations());
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
        dto.setWordItemTranslations(word.getWordItemTranslations());
        dto.setExampleSentence(word.getExampleSentence());
        dto.setExamplePinyin(word.getExamplePinyin());
        dto.setExampleTranslations(word.getExampleTranslations());
        dto.setExampleImage(word.getExampleImage());
        dto.setCreateTime(word.getCreateTime());
        dto.setUpdateTime(word.getUpdateTime());
        dto.setStatus(word.getStatus());
        return dto;
    }

    private void updateDiscrimination(CharDiscrimination entity, CharDiscriminationDto dto) {
        entity.setDiscrimChar(dto.getDiscrimChar());
        entity.setDiscrimPinyin(dto.getDiscrimPinyin());
        entity.setDiscrimCharTranslations(dto.getDiscrimCharTranslations());
        entity.setComparisonTranslations(dto.getComparisonTranslations());
    }

    private void updateWord(CharWord entity, CharWordDto dto) {
        entity.setWordItem(dto.getWordItem());
        entity.setLevel(dto.getLevel());
        entity.setPinyin(dto.getPinyin());
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setWordItemTranslations(dto.getWordItemTranslations());
        entity.setExampleSentence(dto.getExampleSentence());
        entity.setExamplePinyin(dto.getExamplePinyin());
        entity.setExampleTranslations(dto.getExampleTranslations());
        entity.setExampleImage(dto.getExampleImage());
    }

    private CharDiscrimination convertToDiscriminationEntity(CharDiscriminationDto dto, Integer charId) {
        CharDiscrimination entity = new CharDiscrimination();
        entity.setCharId(charId);
        entity.setDiscrimChar(dto.getDiscrimChar());
        entity.setDiscrimPinyin(dto.getDiscrimPinyin());
        entity.setDiscrimCharTranslations(dto.getDiscrimCharTranslations());
        entity.setComparisonTranslations(dto.getComparisonTranslations());
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
        entity.setWordItemTranslations(dto.getWordItemTranslations());
        entity.setExampleSentence(dto.getExampleSentence());
        entity.setExamplePinyin(dto.getExamplePinyin());
        entity.setExampleTranslations(dto.getExampleTranslations());
        entity.setExampleImage(dto.getExampleImage());
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }
}
