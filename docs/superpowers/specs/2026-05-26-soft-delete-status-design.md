# 软删除状态字段设计方案

**日期**: 2026-05-26
**功能**: 为汉字和词汇相关表添加 status 字段实现软删除

---

## 1. 概述

为所有资源相关表添加 `status` 字段，表示资源是否可用。删除操作不再物理删除数据，而是更新 status 为 0 实现软删除。所有查询（包括后台和用户侧）只返回 status=1 的数据。

## 2. status 字段定义

| 属性 | 值 |
|------|-----|
| 类型 | tinyint (Integer) |
| 可用 | 1 |
| 不可用 | 0 |
| 默认值 | 1 |

## 3. 涉及的实体表

### 3.1 汉字模块

| 实体类 | 表名 | 说明 |
|--------|------|------|
| CharCharacter | char_character | 汉字主表 |
| CharDiscrimination | char_discrimination | 汉字辨析 |
| CharWord | char_word | 汉字组词 |

### 3.2 词汇模块

| 实体类 | 表名 | 说明 |
|--------|------|------|
| VocabWord | vocab_word | 词汇主表 |
| VocabSense | vocab_sense | 词汇义项 |
| VocabStructure | vocab_structure | 词汇搭配 |
| VocabExample | vocab_example | 词汇例句 |
| VocabExercise | vocab_exercise | 词汇练习题 |

## 4. 文件修改清单

### 4.1 Entity 类

为以下实体添加 status 字段：
- `CharCharacter`
- `CharDiscrimination`
- `CharWord`
- `VocabWord`
- `VocabSense`
- `VocabStructure`
- `VocabExample`
- `VocabExercise`

### 4.2 DTO 类

为以下 DTO 添加 status 字段：
- `CharCharacterDto`
- `VocabWordDto`
- 子实体 DTO（用于返回数据时包含状态）

### 4.3 Repository 接口

为以下 Repository 添加 status 过滤查询方法：
- `CharCharacterRepository`
- `CharDiscriminationRepository` - 添加 `findByCharIdAndStatus`
- `CharWordRepository` - 添加 `findByCharIdAndStatus`
- `VocabWordRepository`
- `VocabSenseRepository` - 添加 `findByWordIdAndStatus`
- `VocabStructureRepository` - 添加 `findBySenseIdAndStatus`
- `VocabExampleRepository` - 添加 `findByStructureIdAndStatus`
- `VocabExerciseRepository` - 添加 `findByWordIdAndStatus`

### 4.4 Service 实现

修改以下 Service 的查询和删除逻辑：
- `CharCharacterServiceImpl`
- `VocabWordServiceImpl`

**查询逻辑修改：**
- `queryAll`: 在 Specification 中添加 `status = 1` 条件
- `findById`: 查找时检查 status=1，找不到抛 EntityNotFoundException
- 子实体查询：只返回 status=1 的数据

**删除逻辑修改：**
- `delete`: 不再调用 repository.delete()，改为设置 status=0 并保存
- 子实体删除：同步逻辑中删除子实体也是设置 status=0

## 5. 具体实现要点

### 5.1 Entity 字段添加

```java
@Column(name = "status")
@ApiModelProperty(value = "状态: 1=可用, 0=不可用")
private Integer status = 1;
```

### 5.2 Repository 添加查询方法

以 CharDiscriminationRepository 为例：
```java
List<CharDiscrimination> findByCharIdAndStatus(Integer charId, Integer status);
```

### 5.3 Service 查询过滤

在 queryAll 中添加 status 条件：
```java
public PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable) {
    Page<CharCharacter> page = charCharacterRepository.findAll(
        (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), 1));
            // 其他查询条件...
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    return PageUtil.toPage(page.map(charCharacterMapper::toDto));
}
```

或者使用现有的 QueryHelp 机制，在 QueryCriteria 中添加 status 条件。

### 5.4 软删除实现

```java
@Transactional(rollbackFor = Exception.class)
public void delete(Integer id) {
    CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
    if (charCharacter.getId() == null) {
        throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
    }
    // 软删除子实体
    deleteChildren(id);
    // 软删除主实体
    charCharacter.setStatus(0);
    charCharacterRepository.save(charCharacter);
}

private void deleteChildren(Integer charId) {
    List<CharDiscrimination> discriminations = charDiscriminationRepository.findByCharIdAndStatus(charId, 1);
    for (CharDiscrimination d : discriminations) {
        d.setStatus(0);
        charDiscriminationRepository.save(d);
    }
    List<CharWord> words = charWordRepository.findByCharIdAndStatus(charId, 1);
    for (CharWord w : words) {
        w.setStatus(0);
        charWordRepository.save(w);
    }
}
```

### 5.5 子实体同步逻辑修改

在 syncDiscriminations / syncWords 等方法中，删除操作改为设置 status=0 而不是物理删除。

## 6. API 变化

- Controller 接口保持不变
- 删除接口行为从物理删除变为软删除
- 所有查询结果自动过滤掉 status=0 的数据

## 7. 数据库变更

需要为所有涉及的表添加 status 列，默认值为 1：

```sql
-- 汉字模块
ALTER TABLE char_character ADD COLUMN status TINYINT DEFAULT 1;
ALTER TABLE char_discrimination ADD COLUMN status TINYINT DEFAULT 1;
ALTER TABLE char_word ADD COLUMN status TINYINT DEFAULT 1;

-- 词汇模块
ALTER TABLE vocab_word ADD COLUMN status TINYINT DEFAULT 1;
ALTER TABLE vocab_sense ADD COLUMN status TINYINT DEFAULT 1;
ALTER TABLE vocab_structure ADD COLUMN status TINYINT DEFAULT 1;
ALTER TABLE vocab_example ADD COLUMN status TINYINT DEFAULT 1;
ALTER TABLE vocab_exercise ADD COLUMN status TINYINT DEFAULT 1;
```

（注：根据用户要求，不需要考虑数据迁移，默认都是上线状态）
