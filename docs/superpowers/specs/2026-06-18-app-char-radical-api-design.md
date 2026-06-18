# APP 端部首学习接口设计

## 概述

用户端（APP）部首学习功能，提供部首列表查询和部首详情（含关联汉字分页）两个接口。

## 接口设计

### 接口 1：部首列表

```
GET /api/app/char/radical
```

返回所有已发布的部首列表（部首数量 ~200，不分页），按 `id` 升序排列。

**响应体**：`List<AppCharRadicalBaseVO>`

### 接口 2：部首详情（含关联汉字"换一批"分页）

```
GET /api/app/char/radical/{id}?page=0&size=8
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| id | Long | — | 部首ID（路径参数） |
| page | int | 0 | 页码（0-based） |
| size | int | 8 | 每页汉字数 |

**响应体**：包含部首基本信息和当前页汉字的 JSON 对象。

前端"换一批"交互：点击时 page+1，当 `hasNext = false` 时回到 page=0。

## VO 设计

### AppCharRadicalBaseVO — 列表接口返回

```java
@Getter @Setter
public class AppCharRadicalBaseVO implements Serializable {
    private Long id;
    private String radical;       // 部首字符
    private String radicalName;   // 部首名称
    private Integer strokeNum;    // 笔画数
    private Long relationId;      // 关联部首ID
}
```

### AppRadicalCharVO — 详情接口中单条汉字

```java
@Getter @Setter
public class AppRadicalCharVO implements Serializable {
    private Integer id;
    private String character;     // 汉字字形
    private String hskLevel;      // HSK等级
    private String pinyin;        // 拼音
}
```

### AppCharRadicalDetailVO — 详情接口返回体

```java
@Getter @Setter
public class AppCharRadicalDetailVO implements Serializable {
    // 部首基本信息
    private Long id;
    private String radical;
    private String radicalName;
    private Integer strokeNum;
    private String evolutionDesc;
    private Long relationId;

    // 当前页汉字列表
    private List<AppRadicalCharVO> characters;

    // 是否还有下一页（前端"换一批"用）
    private Boolean hasNext;
}
```

## Controller 结构

```
AppCharRadicalController
├── list()     → GET  /api/app/char/radical           → List<AppCharRadicalBaseVO>
└── detail()   → GET  /api/app/char/radical/{id}      → AppCharRadicalDetailVO
```

- 位置：`grid-app/src/main/java/com/naon/grid/modules/app/rest/`
- 注入：`CharRadicalService` + `CharCharacterService`
- 注解：`@Slf4j`, `@RestController`, `@RequiredArgsConstructor`, `@RequestMapping("/api/app/char/radical")`, `@Api(tags = "用户：部首学习")`

## 后端 Service / Repository 扩展

### CharCharacterRepository

```java
Page<CharCharacter> findByRadicalIdAndStatusAndPublishStatus(
    Long radicalId, Integer status, String publishStatus, Pageable pageable);
```

### CharCharacterService

```java
Page<CharCharacterDto> findPublishedByRadicalId(Long radicalId, Pageable pageable);
```

内部调用 repository 的 `findByRadicalIdAndStatusAndPublishStatus`，条件为 `status=1`、`publishStatus='published'`。

### CharRadicalService

```java
List<CharRadicalDto> findAllPublished();
```

内部调用 repository 的 `findByStatusAndPublishStatus`，条件为 `status=1`、`publishStatus='published'`，按 `id` 升序。

### CharRadicalRepository

```java
List<CharRadical> findByStatusAndPublishStatusOrderByIdAsc(
    Integer status, String publishStatus);
```

## 数据流

### 列表接口

```
Client → AppCharRadicalController.list()
  → charRadicalService.findAllPublished()
    → CharRadicalRepository.findByStatusAndPublishStatusOrderByIdAsc(1, "published")
  → 遍历 DTO → 映射为 AppCharRadicalBaseVO
  → ResponseEntity.ok(list)
```

### 详情接口

```
Client → AppCharRadicalController.detail(id, page, size)
  → charRadicalService.findById(id)
    → CharRadicalRepository.findById(id)（含草稿/发布逻辑，已有方法）
  → charCharacterService.findPublishedByRadicalId(id, PageRequest.of(page, size))
    → CharCharacterRepository.findByRadicalIdAndStatusAndPublishStatus(id, 1, "published", pageable)
  → 组装 AppCharRadicalDetailVO（部首信息 + characters + page.isLast() ? hasNext）
  → ResponseEntity.ok(vo)
```

## 文件清单

### 新增文件

| 文件 | 路径 |
|------|------|
| AppCharRadicalController.java | grid-app/.../rest/AppCharRadicalController.java |
| AppCharRadicalBaseVO.java | grid-app/.../rest/vo/AppCharRadicalBaseVO.java |
| AppCharRadicalDetailVO.java | grid-app/.../rest/vo/AppCharRadicalDetailVO.java |
| AppRadicalCharVO.java | grid-app/.../rest/vo/AppRadicalCharVO.java |

### 修改文件

| 文件 | 修改内容 |
|------|----------|
| CharRadicalRepository.java | 新增 findByStatusAndPublishStatusOrderByIdAsc |
| CharRadicalService.java | 新增 findAllPublished |
| CharRadicalServiceImpl.java | 实现 findAllPublished |
| CharCharacterRepository.java | 新增 findByRadicalIdAndStatusAndPublishStatus |
| CharCharacterService.java | 新增 findPublishedByRadicalId |
| CharCharacterServiceImpl.java | 实现 findPublishedByRadicalId |
