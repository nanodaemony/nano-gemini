package com.naon.grid.service.impl;

import com.naon.grid.domain.DynamicConfig;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.repository.DynamicConfigRepository;
import com.naon.grid.service.DynamicConfigService;
import com.naon.grid.service.dto.DynamicConfigDto;
import com.naon.grid.service.dto.DynamicConfigQueryCriteria;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.PostConstruct;
import javax.persistence.criteria.Predicate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DynamicConfigServiceImpl implements DynamicConfigService {

    private final DynamicConfigRepository repository;

    /** 内存缓存: namespace:configKey → value */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    @Override
    public void initCache() {
        configCache.clear();
        List<DynamicConfig> list = repository.findByStatus(StatusEnum.ENABLED.getCode());
        for (DynamicConfig cfg : list) {
            configCache.put(key(cfg.getNamespace(), cfg.getConfigKey()), cfg.getValue());
        }
    }

    @Override
    public String get(String namespace, String configKey) {
        return configCache.get(key(namespace, configKey));
    }

    @Override
    public PageResult<DynamicConfigDto> queryAll(DynamicConfigQueryCriteria criteria, Pageable pageable) {
        Page<DynamicConfig> page = repository.findAll((root, query, cb) -> {
            Predicate predicate = QueryHelp.getPredicate(root, criteria, cb);
            Predicate status = cb.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return cb.and(predicate, status);
        }, pageable);
        return PageUtil.toPage(page.map(this::toDto));
    }

    @Override
    public DynamicConfigDto findById(Long id) {
        DynamicConfig entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(DynamicConfig.class, "id", id.toString()));
        return toDto(entity);
    }

    @Override
    @Transactional
    public void create(DynamicConfigDto dto) {
        DynamicConfig entity = new DynamicConfig();
        entity.setNamespace(dto.getNamespace());
        entity.setName(dto.getName());
        entity.setConfigKey(dto.getConfigKey());
        entity.setValue(dto.getValue());
        entity.setDescription(dto.getDescription());
        entity.setStatus(StatusEnum.ENABLED.getCode());
        repository.save(entity);
        configCache.put(key(entity.getNamespace(), entity.getConfigKey()), entity.getValue());
    }

    @Override
    @Transactional
    public void update(Long id, DynamicConfigDto dto) {
        DynamicConfig entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(DynamicConfig.class, "id", id.toString()));

        // 记录旧 cache key，如果 namespace 或 configKey 改变需要移除旧条目
        String oldKey = key(entity.getNamespace(), entity.getConfigKey());
        boolean keyChanged = !oldKey.equals(key(dto.getNamespace(), dto.getConfigKey()));

        entity.setNamespace(dto.getNamespace());
        entity.setName(dto.getName());
        entity.setConfigKey(dto.getConfigKey());
        entity.setValue(dto.getValue());
        entity.setDescription(dto.getDescription());
        repository.save(entity);

        // 处理缓存
        if (keyChanged) {
            configCache.remove(oldKey);
        }
        configCache.put(key(entity.getNamespace(), entity.getConfigKey()), entity.getValue());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DynamicConfig entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(DynamicConfig.class, "id", id.toString()));
        entity.setStatus(StatusEnum.DISABLED.getCode());
        repository.save(entity);
        configCache.remove(key(entity.getNamespace(), entity.getConfigKey()));
    }

    private DynamicConfigDto toDto(DynamicConfig entity) {
        if (entity == null) return null;
        DynamicConfigDto dto = new DynamicConfigDto();
        dto.setId(entity.getId());
        dto.setNamespace(entity.getNamespace());
        dto.setName(entity.getName());
        dto.setConfigKey(entity.getConfigKey());
        dto.setValue(entity.getValue());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private String key(String namespace, String configKey) {
        return namespace + ":" + configKey;
    }
}
