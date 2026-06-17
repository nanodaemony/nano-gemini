# 动态配置功能设计文档

> 日期: 2026-06-17
> 状态: 已批准待实现

## 1. 概述

为 Little Grid 系统添加一个动态配置功能，允许通过后台接口对业务配置进行实时管理。配置数据持久化到 MySQL，程序运行时从内存缓存读取，修改后实时同步。

### 1.1 核心原则

- **KV 结构**: 配置以键值对形式存储
- **内存读取**: 业务代码从内存缓存读取，零 DB 开销
- **DB 持久化**: 配置持久化到数据库，重启后自动加载
- **实时更新**: 后台修改配置后立即同步到内存缓存，无轮询延迟
- **单机部署**: 当前架构为单机部署，使用进程内缓存即可

## 2. 数据表设计

### 2.1 dynamic_config

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `bigint` PK AUTO_INCREMENT | 主键 |
| `namespace` | `varchar(100)` NOT NULL | 空间/分组标签，如 "system"、"payment" |
| `name` | `varchar(200)` NOT NULL | 配置中文名称，后台展示用 |
| `config_key` | `varchar(200)` NOT NULL | 程序使用的键，如 "session.timeout" |
| `value` | `varchar(2000)` | 配置值（字符串） |
| `description` | `varchar(500)` | 备注说明 |
| `status` | `int` DEFAULT 1 | 软删除：1=可用，0=已删除 |
| `create_by` | `varchar(50)` | 创建人（审计） |
| `update_by` | `varchar(50)` | 更新人（审计） |
| `create_time` | `datetime` | 创建时间（审计） |
| `update_time` | `datetime` | 更新时间（审计） |

**索引**:
- `UNIQUE INDEX idx_ns_key (namespace, config_key)` — 同空间下键唯一

### 2.2 设计说明

- 字段名使用 `config_key` 而非 `key`，避免 MySQL 保留字
- 继承 `BaseEntity` 自动获得审计字段
- 沿用项目三状态模型中的 `status` 做软删除
- 表名 `dynamic_config` 与 grid-tools 现有实体（`chat_prompt`、`local_storage`）风格一致
- ID 使用 `Long` 类型，与 grid-tools 其他实体一致

## 3. 缓存机制

### 3.1 存储结构

```
ConcurrentHashMap<String, String> configCache

key 格式: namespace + ":" + config_key
示例 key: "system:session.timeout"
```

### 3.2 生命周期

```
启动 (@PostConstruct)
  └─→ repository.findByStatus(ENABLED)
       └─→ 逐条写入 configCache

读取 (service.get(ns, key))
  └─→ configCache.get(ns + ":" + key)  → O(1)

新增 (service.create)
  └─→ repository.save(entity)           → 写 DB（事务内）
       └─→ configCache.put(key, value)  → 更新内存

修改 (service.update)
  └─→ 获取原记录（含旧 namespace + configKey）→ 记下旧 cache key
  └─→ 更新 entity 字段                  → 写 DB（事务内）
  └─→ repository.save(entity)
  └─→ 从缓存移除旧 key（若 namespace 或 configKey 改变）
  └─→ configCache.put(newKey, value)   → 更新/添加新 key

删除 (service.delete)
  └─→ entity.setStatus(DISABLED)        → 软删除 DB（事务内）
       └─→ repository.save(entity)
       └─→ configCache.remove(key)      → 移除内存
```

### 3.3 一致性保证

- 写入操作先写 DB 后更新内存：若 DB 写入失败，内存不受影响
- 使用 `@Transactional` 保证 DB 操作的原子性
- 删/改/增 在任何异常情况下都不会导致缓存产生脏数据
- 仅通过后台接口修改配置（不开放其他写入口）

## 4. 代码结构

所有代码位于 `grid-tools` 模块，包路径 `com.naon.grid`。

```
grid-tools/src/main/java/com/naon/grid/
├── domain/
│   └── DynamicConfig.java                     ← JPA 实体
├── repository/
│   └── DynamicConfigRepository.java           ← JPA 仓库
├── service/
│   ├── DynamicConfigService.java              ← 服务接口
│   ├── dto/
│   │   ├── DynamicConfigDto.java              ← DTO
│   │   └── DynamicConfigQueryCriteria.java    ← 查询条件
│   └── impl/
│       └── DynamicConfigServiceImpl.java      ← 实现（含缓存逻辑）
├── rest/
│   ├── DynamicConfigController.java           ← 管理后台 REST 控制器
│   ├── request/
│   │   ├── DynamicConfigCreateRequest.java    ← 创建请求体
│   │   ├── DynamicConfigUpdateRequest.java    ← 更新请求体
│   │   └── DynamicConfigQueryRequest.java     ← 查询参数
│   └── vo/
│       ├── DynamicConfigVO.java               ← 详情 VO
│       └── DynamicConfigBaseVO.java           ← 列表 VO
```

### 4.1 实体类

```java
@Entity
@Table(name = "dynamic_config")
public class DynamicConfig extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String namespace;
    private String name;
    private String configKey;
    private String value;
    private String description;

    private Integer status = 1;  // StatusEnum.ENABLED
}
```

### 4.2 服务接口

```java
public interface DynamicConfigService {
    // 业务读取
    String get(String namespace, String configKey);

    // 后台管理
    PageResult<DynamicConfigDto> queryAll(DynamicConfigQueryCriteria criteria, Pageable pageable);
    DynamicConfigDto findById(Long id);
    void create(DynamicConfigDto dto);
    void update(Long id, DynamicConfigDto dto);
    void delete(Long id);
    void initCache();  // 启动初始化
}
```

### 4.3 缓存核心（ServiceImpl 关键片段）

```java
private final Map<String, String> configCache = new ConcurrentHashMap<>();

@PostConstruct
public void initCache() {
    configCache.clear();
    List<DynamicConfig> list = repository.findByStatus(StatusEnum.ENABLED.getCode());
    list.forEach(cfg -> configCache.put(
        key(cfg.getNamespace(), cfg.getConfigKey()), cfg.getValue()));
}
```

## 5. 管理后台 API

| 方法 | 路径 | 请求体/参数 | 响应 |
|------|------|------------|------|
| `GET` | `/api/dynamic-config` | Query params + Pageable | `PageResult<DynamicConfigBaseVO>` |
| `GET` | `/api/dynamic-config/{id}` | — | `DynamicConfigVO` |
| `POST` | `/api/dynamic-config` | `DynamicConfigCreateRequest` | `ResponseEntity<Void>` |
| `PUT` | `/api/dynamic-config/{id}` | `DynamicConfigUpdateRequest` | `ResponseEntity<Void>` |
| `DELETE` | `/api/dynamic-config/{id}` | — | `ResponseEntity<Void>` |

### 5.1 查询筛选

- `namespace` — 精确匹配
- `name` — 模糊搜索
- `configKey` — 模糊搜索

### 5.2 请求验证

- **创建**: namespace、name、configKey 为必填
- **更新**: namespace、name、configKey 为必填
- **删除**: 仅做软删除（status = 0）

### 5.3 权限

当前暂不加权限控制，后续可通过 `@PreAuthorize` 添加。

## 6. 模式说明

- 遵循 grid-tools 现有代码模式（实体→仓库→服务→控制器）
- 遵循项目的 RestController 命名和注释风格（`@Log`、`@ApiOperation`、`@Api`）
- 使用 Wrapper 类做 DTO/VO 转换（遵循 grid-system 的模式，不引入额外的 MapStruct）
- 沿用项目分页查询模式（`QueryHelp.getPredicate` + `JpaSpecificationExecutor`）
- 异常处理由 `GlobalExceptionHandler` 统一接管

## 7. 边界情况

| 场景 | 行为 |
|------|------|
| 启动时 DB 中无配置 | 空 Map，get() 返回 null |
| 查询不存在的 key | get() 返回 null，由业务方处理 |
| namespace 或 configKey 为 null | get() 返回 null，create/update 由 `@Valid` 拒绝 |
| 修改 config_key 后 | 旧 key 从缓存移除，新 key 加入（在 update 中处理） |
| 删除已删除的记录 | 抛出 EntityNotFoundException |
| 并发更新同一条记录 | JPA 乐观锁或事务隔离保证数据一致 |

## 8. 非功能性

- **性能**: 内存读取为 O(1)，无网络/序列化开销
- **可靠性**: DB 写入失败不污染缓存，重启自动恢复
- **可维护性**: 12 个文件，每个职责单一，总代码量约 200-300 行
- **扩展性**: 如需多实例共享缓存，将 `ConcurrentHashMap` 替换为 `RedisUtils` 调用即可
