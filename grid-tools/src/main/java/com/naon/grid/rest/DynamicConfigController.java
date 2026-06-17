package com.naon.grid.rest;

import com.naon.grid.annotation.Log;
import com.naon.grid.rest.request.DynamicConfigCreateRequest;
import com.naon.grid.rest.request.DynamicConfigQueryRequest;
import com.naon.grid.rest.request.DynamicConfigUpdateRequest;
import com.naon.grid.service.DynamicConfigService;
import com.naon.grid.service.dto.DynamicConfigDto;
import com.naon.grid.service.dto.DynamicConfigQueryCriteria;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@Api(tags = "工具：动态配置管理")
@RequestMapping("/api/dynamic-config")
public class DynamicConfigController {

    private final DynamicConfigService dynamicConfigService;

    @Log("新增动态配置")
    @ApiOperation("新增动态配置")
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody DynamicConfigCreateRequest request) {
        DynamicConfigDto dto = toDto(request);
        dynamicConfigService.create(dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Log("删除动态配置")
    @ApiOperation("删除动态配置")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dynamicConfigService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("修改动态配置")
    @ApiOperation("修改动态配置")
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody DynamicConfigUpdateRequest request) {
        DynamicConfigDto dto = toDto(request);
        dynamicConfigService.update(id, dto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询动态配置详情")
    @ApiOperation("根据ID查询动态配置详情")
    @GetMapping("/{id}")
    public ResponseEntity<DynamicConfigDto> findById(@PathVariable Long id) {
        DynamicConfigDto dto = dynamicConfigService.findById(id);
        return ResponseEntity.ok(dto);
    }

    @Log("查询动态配置列表")
    @ApiOperation("分页查询动态配置列表")
    @GetMapping
    public ResponseEntity<PageResult<DynamicConfigDto>> queryAll(
            DynamicConfigQueryRequest request, Pageable pageable) {
        DynamicConfigQueryCriteria criteria = toCriteria(request);
        PageResult<DynamicConfigDto> page = dynamicConfigService.queryAll(criteria, pageable);
        return ResponseEntity.ok(page);
    }

    // -- 转换方法 --

    private DynamicConfigQueryCriteria toCriteria(DynamicConfigQueryRequest request) {
        if (request == null) return null;
        DynamicConfigQueryCriteria criteria = new DynamicConfigQueryCriteria();
        criteria.setNamespace(request.getNamespace());
        criteria.setName(request.getName());
        criteria.setConfigKey(request.getConfigKey());
        return criteria;
    }

    private DynamicConfigDto toDto(DynamicConfigCreateRequest request) {
        DynamicConfigDto dto = new DynamicConfigDto();
        dto.setNamespace(request.getNamespace());
        dto.setName(request.getName());
        dto.setConfigKey(request.getConfigKey());
        dto.setValue(request.getValue());
        dto.setDescription(request.getDescription());
        return dto;
    }

    private DynamicConfigDto toDto(DynamicConfigUpdateRequest request) {
        DynamicConfigDto dto = new DynamicConfigDto();
        dto.setNamespace(request.getNamespace());
        dto.setName(request.getName());
        dto.setConfigKey(request.getConfigKey());
        dto.setValue(request.getValue());
        dto.setDescription(request.getDescription());
        return dto;
    }
}
