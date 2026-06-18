package com.naon.grid.service;

import com.naon.grid.service.dto.DynamicConfigDto;
import com.naon.grid.service.dto.DynamicConfigQueryCriteria;
import org.springframework.data.domain.Pageable;
import com.naon.grid.utils.PageResult;

public interface DynamicConfigService {

    /** 业务读取：从内存缓存获取配置值，不存在返回 null */
    String get(String namespace, String configKey);

    /** 分页查询配置列表 */
    PageResult<DynamicConfigDto> queryAll(DynamicConfigQueryCriteria criteria, Pageable pageable);

    /** 根据 ID 查询单条 */
    DynamicConfigDto findById(Long id);

    /** 新增配置 */
    void create(DynamicConfigDto dto);

    /** 修改配置 */
    void update(Long id, DynamicConfigDto dto);

    /** 删除配置（软删除） */
    void delete(Long id);

    /** 重新加载缓存 */
    void initCache();
}
