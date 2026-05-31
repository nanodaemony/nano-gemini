# 纲外词记录功能设计文档

## 概述

实现纲外词（用户搜索但未找到的词汇）的记录和管理功能，帮助团队了解用户需求，补充词汇库。

## 背景

用户通过 `AppVocabWordController.search` 接口搜索词汇时，如果搜索不到结果，说明我们的词汇库中缺少该词汇。需要将这些词汇记录下来，统计搜索次数，并提供后台管理接口进行处理。

## 需求

### 功能需求

1. **纲外词记录**：用户搜索无结果时，自动记录搜索词（仅记录符合条件的词）
2. **搜索次数统计**：同一词汇多次搜索时，累加搜索次数
3. **后台分页查询**：支持按处理状态筛选，按搜索次数降序排列
4. **标记已处理**：后台可以将纲外词标记为已处理状态

### 非功能需求

1. **搜索词过滤**：仅记录"全中文+中文标点"的搜索词
2. **去空格处理**：记录前去除搜索词首尾及中间的空格
3. **性能优化**：使用唯一索引防止重复记录，使用 upsert 逻辑

## 设计方案

### 架构

- 在 `grid-system` 模块新增纲外词相关的 Entity、Repository、Service、DTO、VO
- 修改 `grid-app` 模块的 `AppVocabWordController.search` 方法，集成纲外词记录逻辑
- 在 `VocabWordController` 新增后台管理接口

### 数据库设计

#### 表 `vocab_outline_record`

```sql
CREATE TABLE `vocab_outline_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `word` varchar(50) NOT NULL COMMENT '词汇文本（去空格后）',
  `search_count` int(11) NOT NULL DEFAULT '1' COMMENT '未搜到次数',
  `status` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '处理状态, 0:未处理 1:已处理',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`),
  KEY `idx_status` (`status`),
  KEY `idx_search_count` (`search_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='纲外词记录表';
```

**索引说明**：
- `uk_word`：防止同一词重复记录
- `idx_status`：加速按状态筛选
- `idx_search_count`：加速按次数排序

### 接口设计

#### 1. 后台分页查询纲外词

- **路径**：`GET /api/vocabulary/outline`
- **请求参数**：
  ```
  page: Integer          // 页码
  size: Integer          // 每页大小
  status: Integer?       // 处理状态（可选），0=未处理，1=已处理
  ```
- **响应**：`PageResult<VocabOutlineRecordVO>`

#### 2. 标记已处理

- **路径**：`PUT /api/vocabulary/outline/{id}/complete`
- **路径参数**：
  ```
  id: Integer            // 纲外词记录ID
  ```
- **响应**：`204 No Content`

### 文件结构

#### grid-system 模块新增

```
grid-system/src/main/java/com/naon/grid/backend/
├── domain/vocabulary/
│   └── VocabOutlineRecord.java              // 实体类
├── repo/vocabulary/
│   └── VocabOutlineRecordRepository.java     // Repository
├── service/vocabulary/
│   ├── VocabOutlineRecordService.java        // Service接口
│   ├── dto/
│   │   └── VocabOutlineRecordDto.java        // DTO
│   │   └── VocabOutlineRecordQueryCriteria.java // 查询条件
│   ├── impl/
│   │   └── VocabOutlineRecordServiceImpl.java // Service实现
│   └── mapstruct/
│       └── VocabOutlineRecordMapper.java     // MapStruct Mapper
└── rest/
    └── vo/
        └── VocabOutlineRecordVO.java         // VO
```

#### grid-system 模块修改

```
grid-system/src/main/java/com/naon/grid/backend/rest/
└── VocabWordController.java  // 新增后台管理接口
```

#### grid-app 模块修改

```
grid-app/src/main/java/com/naon/grid/modules/app/rest/
└── AppVocabWordController.java  // 修改search方法
```

### 详细设计

#### 1. 实体类 VocabOutlineRecord

```java
@Entity
@Getter
@Setter
@Table(name = "vocab_outline_record")
public class VocabOutlineRecord implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "主键ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "word", nullable = false, length = 50)
    @ApiModelProperty(value = "词汇文本")
    private String word;

    @Column(name = "search_count", nullable = false)
    @ApiModelProperty(value = "未搜到次数")
    private Integer searchCount = 1;

    @Column(name = "status")
    @ApiModelProperty(value = "处理状态, 0:未处理 1:已处理")
    private Integer status = StatusEnum.DISABLED.getCode(); // 0=未处理

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
```

**注意**：复用 `StatusEnum`，0=未处理，1=已处理。

#### 2. Repository VocabOutlineRecordRepository

```java
@Repository
public interface VocabOutlineRecordRepository extends JpaRepository<VocabOutlineRecord, Integer>, JpaSpecificationExecutor<VocabOutlineRecord> {

    Optional<VocabOutlineRecord> findByWord(String word);

    @Modifying
    @Query("UPDATE VocabOutlineRecord r SET r.searchCount = r.searchCount + 1, r.updateTime = CURRENT_TIMESTAMP WHERE r.word = :word")
    int incrementSearchCount(@Param("word") String word);
}
```

#### 3. Service VocabOutlineRecordService

```java
public interface VocabOutlineRecordService {

    /**
     * 记录纲外词（如果符合条件）
     * @param searchWord 用户原始搜索词
     */
    void recordIfNeeded(String searchWord);

    /**
     * 分页查询纲外词
     */
    PageResult<VocabOutlineRecordDto> queryAll(VocabOutlineRecordQueryCriteria criteria, Pageable pageable);

    /**
     * 标记为已处理
     */
    void markAsCompleted(Integer id);
}
```

#### 4. 查询条件 VocabOutlineRecordQueryCriteria

```java
@Data
public class VocabOutlineRecordQueryCriteria implements Serializable {

    @ApiModelProperty(value = "处理状态")
    @Query
    private Integer status;
}
```

#### 5. DTO 和 VO

**VocabOutlineRecordDto**：
```java
@Data
public class VocabOutlineRecordDto implements Serializable {
    private Integer id;
    private String word;
    private Integer searchCount;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
```

**VocabOutlineRecordVO**：
```java
@Data
public class VocabOutlineRecordVO implements Serializable {
    private Integer id;
    private String word;
    private Integer searchCount;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
```

#### 6. 搜索词过滤逻辑

```java
private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fff\\u3000-\\u303f\\uff00-\\uffef]+$");

/**
 * 预处理并验证搜索词
 * @param searchWord 原始搜索词
 * @return 处理后的搜索词，如果不符合条件返回null
 */
private String preprocessSearchWord(String searchWord) {
    if (searchWord == null || searchWord.trim().isEmpty()) {
        return null;
    }

    // 去掉所有空格（包括首尾和中间）
    String processed = searchWord.replaceAll("\\s+", "");

    // 检查长度（不超过50字符）
    if (processed.length() > 50) {
        return null;
    }

    // 检查是否为全中文+中文标点
    if (!CHINESE_PATTERN.matcher(processed).matches()) {
        return null;
    }

    return processed;
}
```

**正则说明**：
- `一-鿿`：中日韩统一表意文字（基本汉字）
- `　-〿`：CJK 符号和标点
- `＀-￯`：半角及全角形式

#### 7. 记录纲外词逻辑

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void recordIfNeeded(String searchWord) {
    String processedWord = preprocessSearchWord(searchWord);
    if (processedWord == null) {
        return;
    }

    // 尝试直接增加计数（避免先查询再插入的竞态条件）
    int updated = vocabOutlineRecordRepository.incrementSearchCount(processedWord);
    if (updated > 0) {
        return;
    }

    // 没有更新到记录，说明是新词，尝试插入
    try {
        VocabOutlineRecord record = new VocabOutlineRecord();
        record.setWord(processedWord);
        record.setSearchCount(1);
        record.setStatus(StatusEnum.DISABLED.getCode()); // 0=未处理
        vocabOutlineRecordRepository.save(record);
    } catch (DataIntegrityViolationException e) {
        // 并发情况下，另一个线程已经插入了，再次尝试增加计数
        vocabOutlineRecordRepository.incrementSearchCount(processedWord);
    }
}
```

#### 8. AppVocabWordController.search 修改

```java
@AnonymousGetMapping("/search")
public ResponseEntity<List<AppVocabWordBaseVO>> search(AppVocabWordSearchRequest request) {
    VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
    criteria.setBlurry(request.getBlurry());
    Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"));
    List<VocabWordDto> dtos = vocabWordService.queryAll(criteria, pageable).getContent();
    List<AppVocabWordBaseVO> vos = toBaseVOList(dtos);

    // 如果搜索结果为空，记录纲外词
    if (vos.isEmpty()) {
        vocabOutlineRecordService.recordIfNeeded(request.getBlurry());
    }

    return new ResponseEntity<>(vos, HttpStatus.OK);
}
```

**注意**：需要注入 `VocabOutlineRecordService`。

#### 9. 后台查询接口实现

```java
@Log("查询纲外词列表")
@ApiOperation("分页查询纲外词列表")
@AnonymousGetMapping("/outline")
public ResponseEntity<PageResult<VocabOutlineRecordVO>> queryOutline(
        VocabOutlineRecordQueryCriteria criteria,
        Pageable pageable) {
    // 默认按搜索次数降序、创建时间降序
    if (pageable.getSort().isEmpty()) {
        pageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "searchCount")
                        .and(Sort.by(Sort.Direction.DESC, "createTime"))
        );
    }
    PageResult<VocabOutlineRecordDto> pageResult = vocabOutlineRecordService.queryAll(criteria, pageable);
    List<VocabOutlineRecordVO> vos = pageResult.getContent().stream()
            .map(vocabOutlineRecordMapper::toVo)
            .collect(Collectors.toList());
    return new ResponseEntity<>(new PageResult<>(vos, pageResult.getTotalElements()), HttpStatus.OK);
}
```

#### 10. 标记已处理接口实现

```java
@Log("标记纲外词已处理")
@ApiOperation("标记纲外词为已处理")
@AnonymousPutMapping("/outline/{id}/complete")
public ResponseEntity<Object> completeOutline(@PathVariable Integer id) {
    vocabOutlineRecordService.markAsCompleted(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}
```

#### 11. Service 实现

```java
@Override
public PageResult<VocabOutlineRecordDto> queryAll(VocabOutlineRecordQueryCriteria criteria, Pageable pageable) {
    Page<VocabOutlineRecord> page = vocabOutlineRecordRepository.findAll(
            (root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder),
            pageable
    );
    return PageUtil.toPage(page.map(vocabOutlineRecordMapper::toDto));
}

@Override
@Transactional(rollbackFor = Exception.class)
public void markAsCompleted(Integer id) {
    VocabOutlineRecord record = vocabOutlineRecordRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(VocabOutlineRecord.class, "id", String.valueOf(id)));
    record.setStatus(StatusEnum.ENABLED.getCode()); // 1=已处理
    vocabOutlineRecordRepository.save(record);
}
```

## 风险与注意事项

1. **并发安全**：使用"先 update，失败再 insert"的策略处理并发，配合唯一索引保证数据一致性
2. **搜索词长度限制**：数据库字段限制为 50 字符，预处理时需要检查
3. **状态枚举复用**：使用 `StatusEnum`，0=未处理，1=已处理，语义可能不够清晰，但保持与现有代码一致
4. **性能影响**：在搜索接口中添加了写操作，注意监控数据库性能
