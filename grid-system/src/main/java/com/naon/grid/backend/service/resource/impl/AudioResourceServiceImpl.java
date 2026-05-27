package com.naon.grid.backend.service.resource.impl;

import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceQueryCriteria;
import lombok.RequiredArgsConstructor;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.backend.domain.resource.AudioResource;
import com.naon.grid.backend.repo.resource.AudioResourceRepository;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.vocabulary.mapstruct.AudioResourceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AudioResourceServiceImpl implements AudioResourceService {

    private final AudioResourceRepository audioResourceRepository;
    private final AudioResourceMapper audioResourceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AudioResourceDto resources) {
        AudioResource entity = audioResourceMapper.toEntity(resources);
        entity = audioResourceRepository.save(entity);
        return entity.getId();
    }

    @Override
    public PageResult<AudioResourceDto> queryAll(AudioResourceQueryCriteria criteria, Pageable pageable) {
        Page<AudioResource> page = audioResourceRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            javax.persistence.criteria.Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            javax.persistence.criteria.Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), 1);
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        return PageUtil.toPage(page.map(audioResourceMapper::toDto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudioResourceDto findById(Long id) {
        AudioResource audioResource = audioResourceRepository.findById(id).orElseGet(AudioResource::new);
        if (audioResource.getId() == null || audioResource.getStatus() == 0) {
            throw new EntityNotFoundException(AudioResource.class, "id", String.valueOf(id));
        }
        return audioResourceMapper.toDto(audioResource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AudioResource audioResource = audioResourceRepository.findById(id).orElseGet(AudioResource::new);
        if (audioResource.getId() == null) {
            throw new EntityNotFoundException(AudioResource.class, "id", String.valueOf(id));
        }
        audioResource.setStatus(0);
        audioResourceRepository.save(audioResource);
    }
}
