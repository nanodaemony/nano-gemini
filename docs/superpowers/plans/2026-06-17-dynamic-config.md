# 动态配置 (Dynamic Config) 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为系统添加动态配置功能，支持后台接口管理 KV 配置，实时同步到内存缓存。

**Architecture:** 在 `grid-tools` 模块内新增 7 个文件。MySQL 通过 JPA 持久化，进程内 `ConcurrentHashMap` 缓存，`@PostConstruct` 启动加载，每次写操作实时同步缓存。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Spring MVC, Lombok, Fastjson2, BaseEntity/BaseDTO 自 grid-common。

## Global Constraints

- 所有代码在 `grid-tools` 模块下，包路径 `com.naon.grid`
- 实体必须继承 `BaseEntity`，DTO 必须继承 `BaseDTO`
- 使用 `javax.persistence.*`（非 jakarta），Java 8 兼容
- 遵循 grid-tools 现有包布局（domain/, repository/, service/, rest/）
- 不加 `@PreAuthorize` 权限控制
- 软删除：`status = 0`（`StatusEnum.DISABLED`）
- ID 类型：`Long` + `GenerationType.IDENTITY`
- 字段名 `config_key` 以避开 MySQL 保留字 `key`
- 控制器直接返回 DTO（不额外建 VO/Wrapper 层），与 grid-tools 现有模式一致

---

### Task 1: 实体 + 仓库

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/domain/DynamicConfig.java`
- Create: `grid-tools/src/main/java/com/naon/grid/repository/DynamicConfigRepository.java`

**Interfaces:**
- Produces: `DynamicConfig` entity (`Long id, String namespace, String name, String configKey, String value, String description, Integer status`)
- Produces: `DynamicConfigRepository` extends `JpaRepository<DynamicConfig, Long> + JpaSpecificationExecutor<DynamicConfig>` with `findByStatus(Integer status): List<DynamicConfig>`

- [ ] **Step 1: Create entity class**

```java
package com.naon.grid.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "dynamic_config")
@Getter
@Setter
public class DynamicConfig extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String namespace;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "config_key", nullable = false, length = 200)
    private String configKey;

    @Column(length = 2000)
    private String value;

    @Column(length = 500)
    private String description;

    /**
     * 状态：1=启用，0=禁用（软删除）
     */
    private Integer status = 1;
}
```

- [ ] **Step 2: Create repository**

```java
package com.naon.grid.repository;

import com.naon.grid.domain.DynamicConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DynamicConfigRepository
        extends JpaRepository<DynamicConfig, Long>,
                JpaSpecificationExecutor<DynamicConfig> {

    List<DynamicConfig> findByStatus(Integer status);
}
```

- [ ] **Step 3: Compile check**

```bash
mvn compile -pl grid-tools -am -q
```
Expected: BUILD SUCCESS (无错误)

- [ ] **Step 4: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/domain/DynamicConfig.java \
       grid-tools/src/main/java/com/naon/grid/repository/DynamicConfigRepository.java
git commit -m "feat: add DynamicConfig entity and repository"
```

---

### Task 2: DTO + Request 类 + Service 接口

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/DynamicConfigDto.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/DynamicConfigQueryCriteria.java`
- Create: `grid-tools/src/main/java/com/naon/grid/rest/request/DynamicConfigCreateRequest.java`
- Create: `grid-tools/src/main/java/com/naon/grid/rest/request/DynamicConfigUpdateRequest.java`
- Create: `grid-tools/src/main/java/com/naon/grid/rest/request/DynamicConfigQueryRequest.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/DynamicConfigService.java`

**Interfaces:**
- Consumes: `DynamicConfig`, `DynamicConfigRepository`
- Produces: `DynamicConfigDto` (extends BaseDTO), `DynamicConfigQueryCriteria`, request validation DTOs
- Produces: `DynamicConfigService` interface with 7 methods

- [ ] **Step 1: Create DTO**

```java
package com.naon.grid.service.dto;

import com.naon.grid.base.BaseDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DynamicConfigDto extends BaseDTO {

    private Long id;
    private String namespace;
    private String name;
    private String configKey;
    private String value;
    private String description;
    private Integer status;
}
```

- [ ] **Step 2: Create query criteria**

```java
package com.naon.grid.service.dto;

import com.naon.grid.annotation.Query;
import lombok.Data;
import java.io.Serializable;

@Data
public class DynamicConfigQueryCriteria implements Serializable {

    @Query(type = Query.Type.INNER_LIKE)
    private String name;

    @Query(type = Query.Type.INNER_LIKE)
    private String configKey;

    @Query
    private String namespace;
}
```

- [ ] **Step 3: Create request classes**

```java
// DynamicConfigCreateRequest.java
package com.naon.grid.rest.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class DynamicConfigCreateRequest {
    @NotBlank(message = "namespace 不能为空")
    private String namespace;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "configKey 不能为空")
    private String configKey;

    private String value;
    private String description;
}
```

```java
// DynamicConfigUpdateRequest.java
package com.naon.grid.rest.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class DynamicConfigUpdateRequest {
    @NotBlank(message = "namespace 不能为空")
    private String namespace;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "configKey 不能为空")
    private String configKey;

    private String value;
    private String description;
}
```

```java
// DynamicConfigQueryRequest.java
package com.naon.grid.rest.request;

import lombok.Data;

@Data
public class DynamicConfigQueryRequest {
    private String namespace;
    private String name;
    private String configKey;
}
```

- [ ] **Step 4: Create service interface**

```java
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
```

- [ ] **Step 5: Create request directory**

```bash
mkdir -p grid-tools/src/main/java/com/naon/grid/rest/request
```

- [ ] **Step 6: Compile check**

```bash
mvn compile -pl grid-tools -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/dto/DynamicConfigDto.java \
       grid-tools/src/main/java/com/naon/grid/service/dto/DynamicConfigQueryCriteria.java \
       grid-tools/src/main/java/com/naon/grid/rest/request/DynamicConfigCreateRequest.java \
       grid-tools/src/main/java/com/naon/grid/rest/request/DynamicConfigUpdateRequest.java \
       grid-tools/src/main/java/com/naon/grid/rest/request/DynamicConfigQueryRequest.java \
       grid-tools/src/main/java/com/naon/grid/service/DynamicConfigService.java
git commit -m "feat: add DTO, request classes, and service interface for dynamic config"
```

---

### Task 3: Service 实现（缓存核心）

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/impl/DynamicConfigServiceImpl.java`

**Interfaces:**
- Consumes: `DynamicConfigRepository`, `DynamicConfigService` (implements), `DynamicConfigDto`, `DynamicConfigQueryCriteria`
- Produces: `DynamicConfigServiceImpl` — 包含 `ConcurrentHashMap` 缓存 + 7 个方法的实现

- [ ] **Step 1: Create service implementation**

```java
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
                .orElseThrow(() -> new EntityNotFoundException(DynamicConfig.class, "id", id));
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
                .orElseThrow(() -> new EntityNotFoundException(DynamicConfig.class, "id", id));

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
                .orElseThrow(() -> new EntityNotFoundException(DynamicConfig.class, "id", id));
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
```

- [ ] **Step 2: Compile check**

```bash
mvn compile -pl grid-tools -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/impl/DynamicConfigServiceImpl.java
git commit -m "feat: implement dynamic config service with ConcurrentHashMap cache"
```

---

### Task 4: Controller（管理后台接口）

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/rest/DynamicConfigController.java`

**Interfaces:**
- Consumes: `DynamicConfigService`, Request classes, `DynamicConfigDto`
- Produces: REST API 端点，映射 5 个 HTTP 方法

- [ ] **Step 1: Create controller**

```java
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
```

- [ ] **Step 2: Compile check**

```bash
mvn compile -pl grid-tools -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 完整编译+打包**

```bash
mvn clean package -DskipTests -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/rest/DynamicConfigController.java
git commit -m "feat: add dynamic config admin REST controller"
```

---

## Self-Review Checklist

- [ ] Spec 覆盖: 数据表设计 → Task 1 entity; 缓存机制 → Task 3 impl; 管理接口 → Task 4 controller; 所有 spec 需求都有对应任务
- [ ] 占位符检查: 没有 TBD、TODO、"add appropriate error handling" 等
- [ ] 类型一致性: entity 用 Long id, status 用 Integer, 所有文件之间类型一致
- [ ] 路径正确: 所有文件路径以 `grid-tools/...` 开头，包名 `com.naon.grid`
