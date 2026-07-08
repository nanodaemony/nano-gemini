# Radical Practice API Design

## 需求概述

为 App 端开发部首练习接口。用户传入目标部首 ID，接口返回 3 个部首（目标部首 + 2 个随机部首），每个部首附带最多 10 个随机汉字。前端将汉字打乱后展示，由用户将汉字归位到正确的部首下。

## 接口定义

### 路径与方法

```
GET /api/app/char/radical/{id}/practice
```

### 请求参数

| 参数 | 类型 | 位置 | 必填 | 说明 |
|---|---|---|---|---|
| id | Long | Path | 是 | 目标部首 ID |

### 响应结构

```json
{
  "radicals": [
    {
      "radicalId": 1,
      "radical": "口",
      "radicalName": "口字旁",
      "characters": [
        { "id": 101, "character": "吃" },
        { "id": 102, "character": "喝" }
      ]
    },
    {
      "radicalId": 15,
      "radical": "亻",
      "radicalName": "单人旁",
      "characters": [...]
    },
    {
      "radicalId": 42,
      "radical": "扌",
      "radicalName": "提手旁",
      "characters": [...]
    }
  ]
}
```

- `radicals` 数组固定 3 个元素：第一个为目标部首，后两个为随机部首
- 每个部首的 `characters` 为从该部首关联的已发布汉字中随机选取的最多 10 个（不足则全部返回）

## 核心逻辑

### 处理流程

```
1. 根据 id 查询部首信息（charRadicalService.findPublishedById）
   → 如部首不存在或未发布，抛出异常（复用现有 404 逻辑）

2. 查询该部首关联的已发布汉字全部列表
   → 随机选取 min(总数, 10) 个

3. 获取所有已发布部首列表（charRadicalService.findAllPublished）
   → 排除当前部首 id
   → 完全随机选取 2 个

4. 对选中的 2 个随机部首，分别查询其关联的已发布汉字
   → 各随机选取 min(总数, 10) 个

5. 组装响应：目标部首排在第一个，随机部首随后
```

### 随机选取策略

- 使用 `Collections.shuffle()` 对汉字列表随机打乱后取前 N 个
- 从所有已发布部首中排除当前部首后随机选取 2 个（不要求有汉字关联，如果某个随机部首没有已发布汉字，该部首的 characters 为空数组）
- 不保证每次请求返回不同的部首/汉字组合（由前端决定是否缓存结果）

## 涉及文件

### grid-system 模块（3 个文件修改）

#### 1. CharCharacterRepository.java — 新增查询方法

新增非分页版本的查询方法，返回 `List<CharCharacter>`：

```java
/**
 * 根据 radicalId 查询所有已发布的汉字（不分页）
 */
List<CharCharacter> findByRadicalIdAndStatusAndPublishStatus(
    Long radicalId, Integer status, String publishStatus);
```

Spring Data JPA 会根据方法名自动生成查询，不需要额外写 SQL。

#### 2. CharCharacterService.java — 新增接口方法

```java
List<CharCharacterDto> findPublishedListByRadicalId(Long radicalId);
```

#### 3. CharCharacterServiceImpl.java — 实现方法

```java
@Override
public List<CharCharacterDto> findPublishedListByRadicalId(Long radicalId) {
    List<CharCharacter> list = charCharacterRepository
        .findByRadicalIdAndStatusAndPublishStatus(
            radicalId, StatusEnum.ENABLED.getCode(), PublishStatusEnum.PUBLISHED.getCode());
    return list.stream().map(charCharacterMapper::toDto).collect(Collectors.toList());
}
```

### grid-app 模块（4 个文件）

#### 4. AppCharRadicalController.java — 新增端点

在已有 Controller 中新增 `practice` 方法。Controller 只负责流程编排，转换逻辑委托给 Wrapper：

```java
@ApiOperation("部首练习：获取目标部首+2个随机部首，各附带最多10个随机汉字")
@AnonymousGetMapping("/{id}/practice")
public ResponseEntity<AppRadicalPracticeVO> practice(@PathVariable Long id) {
    // 1. 查询目标部首
    CharRadicalDto targetRadical = charRadicalService.findPublishedById(id);

    // 2. 获取目标部首的随机汉字
    List<CharCharacterDto> targetChars = charCharacterService.findPublishedListByRadicalId(id);
    List<CharCharacterDto> randomTargetChars = pickRandom(targetChars, 10);

    // 3. 随机选2个其他部首
    List<CharRadicalDto> allRadicals = charRadicalService.findAllPublished();
    List<CharRadicalDto> others = allRadicals.stream()
        .filter(r -> !r.getId().equals(id))
        .collect(Collectors.toList());
    Collections.shuffle(others);
    List<CharRadicalDto> randomRadicals = others.stream().limit(2).collect(Collectors.toList());

    // 4. 获取随机部首的随机汉字
    List<AppRadicalPracticeVO.RadicalGroup> radicalGroups = new ArrayList<>();
    radicalGroups.add(AppCharRadicalWrapper.toGroup(targetRadical, randomTargetChars));
    for (CharRadicalDto radical : randomRadicals) {
        List<CharCharacterDto> chars = charCharacterService.findPublishedListByRadicalId(radical.getId());
        radicalGroups.add(AppCharRadicalWrapper.toGroup(radical, pickRandom(chars, 10)));
    }

    return new ResponseEntity<>(AppRadicalPracticeVO.withRadicals(radicalGroups), HttpStatus.OK);
}

/** 从列表中随机取最多 limit 个元素 */
private static <T> List<T> pickRandom(List<T> list, int limit) {
    if (list == null || list.isEmpty()) return Collections.emptyList();
    List<T> copy = new ArrayList<>(list);
    Collections.shuffle(copy);
    return copy.subList(0, Math.min(limit, copy.size()));
}
```

#### 5. AppRadicalPracticeVO.java — 新建响应 VO

```java
@Getter
@Setter
public class AppRadicalPracticeVO implements Serializable {
    @ApiModelProperty(value = "部首分组列表（3个）")
    private List<RadicalGroup> radicals;

    @Getter
    @Setter
    public static class RadicalGroup implements Serializable {
        @ApiModelProperty(value = "部首ID")
        private Long radicalId;
        @ApiModelProperty(value = "部首字符")
        private String radical;
        @ApiModelProperty(value = "部首名称")
        private String radicalName;
        @ApiModelProperty(value = "该部首下的汉字列表（最多10个）")
        private List<AppPracticeCharVO> characters;
    }

    public static AppRadicalPracticeVO withRadicals(List<RadicalGroup> radicals) {
        AppRadicalPracticeVO vo = new AppRadicalPracticeVO();
        vo.setRadicals(radicals);
        return vo;
    }
}
```

#### 6. AppPracticeCharVO.java — 新建汉字子 VO

```java
@Getter
@Setter
public class AppPracticeCharVO implements Serializable {
    @ApiModelProperty(value = "汉字ID")
    private Integer id;
    @ApiModelProperty(value = "汉字字形")
    private String character;
}
```

> 注：不需要复用已有的 `AppRadicalCharVO`（含 hskLevel、pinyin），因为用户明确只需要最小信息。

#### 7. AppCharRadicalWrapper.java — 新增转换方法

在已有 Wrapper 中新增 `toGroup` 方法，负责 `Dto → VO` 转换：

```java
public static AppRadicalPracticeVO.RadicalGroup toGroup(
        CharRadicalDto radicalDto, List<CharCharacterDto> charDtos) {
    AppRadicalPracticeVO.RadicalGroup group = new AppRadicalPracticeVO.RadicalGroup();
    group.setRadicalId(radicalDto.getId());
    group.setRadical(radicalDto.getRadical());
    group.setRadicalName(radicalDto.getRadicalName());
    List<AppPracticeCharVO> chars = charDtos.stream().map(dto -> {
        AppPracticeCharVO vo = new AppPracticeCharVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        return vo;
    }).collect(Collectors.toList());
    group.setCharacters(chars);
    return group;
}
```

> 遵循项目 Wrapper 模式：Controller 中不得包含 Dto→VO 转换逻辑，`toGroup` 为静态纯映射方法。Controller 保留 `pickRandom` 私有方法（业务逻辑，非转换逻辑）。

## 数据流

```
Client → GET /api/app/char/radical/{id}/practice
  → charRadicalService.findPublishedById(id)
    → CharRadicalRepository.findById(id) → 已有逻辑，含 404 处理
  → charCharacterService.findPublishedListByRadicalId(id)
    → CharCharacterRepository.findByRadicalIdAndStatusAndPublishStatus(id, 1, "published") → List
  → charRadicalService.findAllPublished()
    → CharRadicalRepository.findByStatusAndPublishStatusOrderByIdAsc(1, "published") → List
  → 随机算法 → charCharacterService.findPublishedListByRadicalId(x2)
  → 组装 AppRadicalPracticeVO → ResponseEntity.ok(vo)
```

## 异常处理

- **部首不存在/未发布**: `findPublishedById` 已有校验逻辑，自动抛出 `BadRequestException("部首不存在")`
- **随机部首没有汉字**: 返回空 `characters` 数组，前端正常展示（空列表的部首可显示为灰色或提示无可用汉字）
- **总部首数不足 3 个**: 有多少返回多少，`radicals` 数组长度可能少于 3

## 注意事项

- 不涉及用户状态，无需登录，使用 `@AnonymousGetMapping`
- 复用已有 Service 层，不加 App 中间 Service
- 随机算法使用标准 `Collections.shuffle()`，不引入额外依赖
- 遵循项目现有命名规范：VO 以 `App` 前缀开头，置于 `grid-app/.../rest/vo/` 包下
