package com.naon.grid.rest.wrapper;

import com.naon.grid.rest.request.DynamicConfigCreateRequest;
import com.naon.grid.rest.request.DynamicConfigQueryRequest;
import com.naon.grid.rest.request.DynamicConfigUpdateRequest;
import com.naon.grid.service.dto.DynamicConfigDto;
import com.naon.grid.service.dto.DynamicConfigQueryCriteria;

/**
 * 动态配置包装器
 */
public class DynamicConfigWrapper {

    public static DynamicConfigQueryCriteria toCriteria(DynamicConfigQueryRequest request) {
        if (request == null) return null;
        DynamicConfigQueryCriteria criteria = new DynamicConfigQueryCriteria();
        criteria.setNamespace(request.getNamespace());
        criteria.setName(request.getName());
        criteria.setConfigKey(request.getConfigKey());
        return criteria;
    }

    public static DynamicConfigDto toDto(DynamicConfigCreateRequest request) {
        DynamicConfigDto dto = new DynamicConfigDto();
        dto.setNamespace(request.getNamespace());
        dto.setName(request.getName());
        dto.setConfigKey(request.getConfigKey());
        dto.setValue(request.getValue());
        dto.setDescription(request.getDescription());
        return dto;
    }

    public static DynamicConfigDto toDto(DynamicConfigUpdateRequest request) {
        DynamicConfigDto dto = new DynamicConfigDto();
        dto.setNamespace(request.getNamespace());
        dto.setName(request.getName());
        dto.setConfigKey(request.getConfigKey());
        dto.setValue(request.getValue());
        dto.setDescription(request.getDescription());
        return dto;
    }
}
