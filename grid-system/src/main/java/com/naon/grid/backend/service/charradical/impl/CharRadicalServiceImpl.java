package com.naon.grid.backend.service.charradical.impl;

import com.naon.grid.backend.domain.charradical.CharRadical;
import com.naon.grid.backend.repo.charradical.CharRadicalRepository;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.backend.service.charradical.dto.CharRadicalQueryCriteria;
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

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class CharRadicalServiceImpl implements CharRadicalService {

    private final CharRadicalRepository charRadicalRepository;

    @Override
    public PageResult<CharRadicalDto> queryAll(CharRadicalQueryCriteria criteria, Pageable pageable) {
        Page<CharRadical> page = charRadicalRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        return PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CharRadicalDto findById(Long id) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            if (entity.getDraftContent() == null) {
                return toBaseDto(entity);
            }
            CharRadicalDto dto;
            try {
                dto = JsonUtils.fromJson(entity.getDraftContent(), CharRadicalDto.class);
            } catch (Exception e) {
                throw new BadRequestException("草稿数据解析失败");
            }
            if (dto == null) {
                throw new BadRequestException("草稿内容不存在");
            }
            dto.setId(entity.getId());
            dto.setStatus(entity.getStatus());
            dto.setPublishStatus(entity.getPublishStatus());
            dto.setEditStatus(entity.getEditStatus());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setCreateBy(entity.getCreateBy());
            dto.setUpdateBy(entity.getUpdateBy());
            return dto;
        }

        return toBaseDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CharRadicalDto resources) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.PUBLISHED.getCode().equals(entity.getEditStatus())) {
            entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        entity.setDraftContent(JsonUtils.toJson(resources));
        charRadicalRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        entity.setStatus(StatusEnum.DISABLED.getCode());
        charRadicalRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }
        entity.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        charRadicalRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅已审核状态可发布");
        }

        CharRadicalDto draftDto;
        try {
            draftDto = JsonUtils.fromJson(entity.getDraftContent(), CharRadicalDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draftDto == null) {
            throw new BadRequestException("草稿数据解析失败");
        }

        // 校验必填字段
        if (draftDto.getStrokeNum() == null) {
            throw new BadRequestException("笔画数不能为空");
        }

        // 回写主表字段（radical 字段创建后不允许修改）
        entity.setRadicalName(draftDto.getRadicalName());
        entity.setStrokeNum(draftDto.getStrokeNum());
        entity.setRelationId(draftDto.getRelationId());
        entity.setEvolutionDesc(draftDto.getEvolutionDesc());
        entity.setEvolutionDescTranslations(JsonUtils.toTranslationJson(draftDto.getEvolutionDescTranslations()));
        entity.setEvolutionImageId(draftDto.getEvolutionImageId());

        // 更新状态，清除草稿
        entity.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        entity.setDraftContent(null);
        charRadicalRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        charRadicalRepository.save(entity);
    }

    @Override
    public List<CharRadicalDto> findAllPublished() {
        List<CharRadical> list = charRadicalRepository.findByStatusAndPublishStatusOrderByIdAsc(
                StatusEnum.ENABLED.getCode(), PublishStatusEnum.PUBLISHED.getCode());
        return list.stream().map(this::toBaseDto).collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    private CharRadicalDto toBaseDto(CharRadical entity) {
        CharRadicalDto dto = new CharRadicalDto();
        dto.setId(entity.getId());
        dto.setRadical(entity.getRadical());
        dto.setRadicalName(entity.getRadicalName());
        dto.setStrokeNum(entity.getStrokeNum());
        dto.setRelationId(entity.getRelationId());
        dto.setEvolutionDesc(entity.getEvolutionDesc());
        dto.setEvolutionDescTranslations(JsonUtils.parseTranslationList(entity.getEvolutionDescTranslations()));
        dto.setEvolutionImageId(entity.getEvolutionImageId());
        dto.setStatus(entity.getStatus());
        dto.setPublishStatus(entity.getPublishStatus());
        dto.setEditStatus(entity.getEditStatus());
        dto.setDraftContent(entity.getDraftContent());
        dto.setCreateBy(entity.getCreateBy());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    private CharRadicalDto toDtoWithDraftOverlay(CharRadical entity) {
        CharRadicalDto dto = toBaseDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    private void applyDraftOverlay(CharRadicalDto dto, String draftJson) {
        if (draftJson == null) {
            return;
        }
        CharRadicalDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, CharRadicalDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        if (draft.getRadical() != null)                dto.setRadical(draft.getRadical());
        if (draft.getRadicalName() != null)            dto.setRadicalName(draft.getRadicalName());
        if (draft.getStrokeNum() != null)              dto.setStrokeNum(draft.getStrokeNum());
        if (draft.getRelationId() != null)             dto.setRelationId(draft.getRelationId());
        if (draft.getEvolutionDesc() != null)          dto.setEvolutionDesc(draft.getEvolutionDesc());
        if (draft.getEvolutionDescTranslations() != null) dto.setEvolutionDescTranslations(draft.getEvolutionDescTranslations());
        if (draft.getEvolutionImageId() != null)       dto.setEvolutionImageId(draft.getEvolutionImageId());
    }
}
