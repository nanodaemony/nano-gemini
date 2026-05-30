# 用户端汉字 API 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为普通用户提供汉字搜索和详情查询接口，复用后台 Service，返回数据不包含审计字段。

**Architecture:** 在 grid-app 模块中创建 Controller、Request 和 VO 类，复用 grid-system 模块中的 CharCharacterService，扩展 Service 以支持仅匹配汉字的搜索功能。

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, Lombok, Swagger

---

## 任务概览

### 阶段 1：扩展 grid-system 的查询功能
- 任务 1：修改 CharCharacterQueryCriteria，新增字段控制查询行为
- 任务 2：在 CharCharacterService 中新增搜索方法
- 任务 3：在 CharCharacterServiceImpl 中实现搜索方法

### 阶段 2：创建 grid-app 的 VO 和 Request 类
- 任务 4：创建 AppCharCharacterBaseVO
- 任务 5：创建 AppCharCharacterDetailVO（含内部类）
- 任务 6：创建 AppCharCharacterSearchRequest

### 阶段 3：创建 Controller
- 任务 7：创建 AppCharCharacterController

### 阶段 4：验证
- 任务 8：编译验证
- 任务 9：提交代码

---

## 详细任务

### 任务 1：修改 CharCharacterQueryCriteria

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterQueryCriteria.java`

- [ ] **Step 1: 修改 CharCharacterQueryCriteria，新增 searchCharacterOnly 字段**

```java
package com.naon.grid.backend.service.character.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharCharacterQueryCriteria implements Serializable {

    @ApiModelProperty(value = "汉字或拼音模糊查询")
    @Query(blurry = "character,pinyin")
    private String blurry;

    @ApiModelProperty(value = "是否仅搜索汉字字段（true=仅匹配character，false=匹配character和pinyin）")
    private Boolean searchCharacterOnly = false;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterQueryCriteria.java
git commit -m "feat: add searchCharacterOnly flag to CharCharacterQueryCriteria"
```

---

### 任务 2：在 CharCharacterService 中新增搜索方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java`

- [ ] **Step 1: 新增 searchByCharacter 方法接口**

```java
package com.naon.grid.backend.service.character;

import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CharCharacterService {

    PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable);

    CharCharacterDto findById(Integer id);

    Integer create(CharCharacterDto resources);

    void update(Integer id, CharCharacterDto resources);

    void delete(Integer id);

    /**
     * 根据汉字模糊搜索（仅匹配character字段）
     * @param blurry 搜索关键词
     * @return 匹配的汉字列表
     */
    List<CharCharacterDto> searchByCharacter(String blurry);
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java
git commit -m "feat: add searchByCharacter method to CharCharacterService"
```

---

### 任务 3：在 CharCharacterServiceImpl 中实现搜索方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: 实现 searchByCharacter 方法**

在 `CharCharacterServiceImpl` 类的末尾添加：

```java
    @Override
    public List<CharCharacterDto> searchByCharacter(String blurry) {
        List<CharCharacter> characters = charCharacterRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 状态必须为启用
            predicates.add(criteriaBuilder.equal(root.get("status"), com.naon.grid.enums.StatusEnum.ENABLED.getCode()));
            
            // 模糊匹配汉字
            if (blurry != null && !blurry.isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("character"), "%" + blurry + "%"));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        
        return characters.stream().map(charCharacterMapper::toDto).collect(java.util.stream.Collectors.toList());
    }
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
git commit -m "feat: implement searchByCharacter method"
```

---

### 任务 4：创建 AppCharCharacterBaseVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterBaseVO.java`

- [ ] **Step 1: 创建 AppCharCharacterBaseVO 类**

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 用户端汉字基础VO（不包含审计字段）
 */
@Getter
@Setter
public class AppCharCharacterBaseVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private List<TextTranslationVO> descTranslations;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-app`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterBaseVO.java
git commit -m "feat: add AppCharCharacterBaseVO"
```

---

### 任务 5：创建 AppCharCharacterDetailVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterDetailVO.java`

- [ ] **Step 1: 创建 AppCharCharacterDetailVO 类（包含内部类）**

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 用户端汉字详情VO（不包含审计字段）
 */
@Getter
@Setter
public class AppCharCharacterDetailVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private List<TextTranslationVO> descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationVO> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordVO> words;

    @Getter
    @Setter
    public static class CharDiscriminationVO implements Serializable {

        @ApiModelProperty(value = "辨析唯一ID")
        private Integer id;

        @ApiModelProperty(value = "汉字ID")
        private Integer charId;

        @ApiModelProperty(value = "辨析汉字")
        private String discrimChar;

        @ApiModelProperty(value = "辨析拼音")
        private String discrimPinyin;

        @ApiModelProperty(value = "辨析汉字翻译")
        private List<TextTranslationVO> discrimCharTranslations;

        @ApiModelProperty(value = "对比翻译")
        private List<TextTranslationVO> comparisonTranslations;
    }

    @Getter
    @Setter
    public static class CharWordVO implements Serializable {

        @ApiModelProperty(value = "组词唯一ID")
        private Integer id;

        @ApiModelProperty(value = "汉字ID")
        private Integer charId;

        @ApiModelProperty(value = "组词")
        private String wordItem;

        @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
        private String level;

        @ApiModelProperty(value = "拼音")
        private String pinyin;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "组词翻译")
        private List<TextTranslationVO> wordItemTranslations;

        @ApiModelProperty(value = "例句")
        private String exampleSentence;

        @ApiModelProperty(value = "例句拼音")
        private String examplePinyin;

        @ApiModelProperty(value = "例句翻译")
        private List<TextTranslationVO> exampleTranslations;

        @ApiModelProperty(value = "例句图片")
        private String exampleImage;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-app`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterDetailVO.java
git commit -m "feat: add AppCharCharacterDetailVO"
```

---

### 任务 6：创建 AppCharCharacterSearchRequest

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppCharCharacterSearchRequest.java`

- [ ] **Step 1: 创建 AppCharCharacterSearchRequest 类**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户端汉字搜索请求
 */
@Data
public class AppCharCharacterSearchRequest implements Serializable {

    @ApiModelProperty(value = "汉字模糊查询关键词")
    private String blurry;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-app`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppCharCharacterSearchRequest.java
git commit -m "feat: add AppCharCharacterSearchRequest"
```

---

### 任务 7：创建 AppCharCharacterController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java`

- [ ] **Step 1: 创建 AppCharCharacterController 类**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharDiscriminationDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.modules.app.rest.request.AppCharCharacterSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端汉字接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/character")
@Api(tags = "应用：汉字查询接口")
public class AppCharCharacterController {

    private final CharCharacterService charCharacterService;

    @ApiOperation("搜索汉字（仅匹配汉字字段）")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppCharCharacterBaseVO>> search(AppCharCharacterSearchRequest request) {
        List<CharCharacterDto> dtos = charCharacterService.searchByCharacter(request.getBlurry());
        List<AppCharCharacterBaseVO> vos = toBaseVOList(dtos);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("根据ID查询汉字详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppCharCharacterDetailVO> getDetail(@PathVariable Integer id) {
        CharCharacterDto dto = charCharacterService.findById(id);
        AppCharCharacterDetailVO vo = toDetailVO(dto);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    private List<AppCharCharacterBaseVO> toBaseVOList(List<CharCharacterDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private AppCharCharacterBaseVO toBaseVO(CharCharacterDto dto) {
        AppCharCharacterBaseVO vo = new AppCharCharacterBaseVO();
        vo.setId(dto.getId());
        vo.setSequenceNo(dto.getSequenceNo());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setStroke(dto.getStroke());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
        return vo;
    }

    private AppCharCharacterDetailVO toDetailVO(CharCharacterDto dto) {
        AppCharCharacterDetailVO vo = new AppCharCharacterDetailVO();
        vo.setId(dto.getId());
        vo.setSequenceNo(dto.getSequenceNo());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setStroke(dto.getStroke());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
        vo.setDiscriminations(toDiscriminationVOList(dto.getDiscriminations()));
        vo.setWords(toWordVOList(dto.getWords()));
        return vo;
    }

    private List<AppCharCharacterDetailVO.CharDiscriminationVO> toDiscriminationVOList(List<CharDiscriminationDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toDiscriminationVO).collect(Collectors.toList());
    }

    private AppCharCharacterDetailVO.CharDiscriminationVO toDiscriminationVO(CharDiscriminationDto dto) {
        AppCharCharacterDetailVO.CharDiscriminationVO vo = new AppCharCharacterDetailVO.CharDiscriminationVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setDiscrimChar(dto.getDiscrimChar());
        vo.setDiscrimPinyin(dto.getDiscrimPinyin());
        vo.setDiscrimCharTranslations(toTextTranslationVOList(dto.getDiscrimCharTranslations()));
        vo.setComparisonTranslations(toTextTranslationVOList(dto.getComparisonTranslations()));
        return vo;
    }

    private List<AppCharCharacterDetailVO.CharWordVO> toWordVOList(List<CharWordDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toWordVO).collect(Collectors.toList());
    }

    private AppCharCharacterDetailVO.CharWordVO toWordVO(CharWordDto dto) {
        AppCharCharacterDetailVO.CharWordVO vo = new AppCharCharacterDetailVO.CharWordVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setWordItem(dto.getWordItem());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setWordItemTranslations(toTextTranslationVOList(dto.getWordItemTranslations()));
        vo.setExampleSentence(dto.getExampleSentence());
        vo.setExamplePinyin(dto.getExamplePinyin());
        vo.setExampleTranslations(toTextTranslationVOList(dto.getExampleTranslations()));
        vo.setExampleImage(dto.getExampleImage());
        return vo;
    }

    private List<TextTranslationVO> toTextTranslationVOList(List<com.naon.grid.domain.common.TextTranslation> translations) {
        if (translations == null) {
            return Collections.emptyList();
        }
        return translations.stream().map(this::toTextTranslationVO).collect(Collectors.toList());
    }

    private TextTranslationVO toTextTranslationVO(com.naon.grid.domain.common.TextTranslation translation) {
        if (translation == null) {
            return null;
        }
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-app`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java
git commit -m "feat: add AppCharCharacterController"
```

---

### 任务 8：整体编译验证

**Files:**
- N/A

- [ ] **Step 1: 完整编译项目**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: 验证模块依赖关系**

确认 grid-app 能正确依赖 grid-system 的类，无编译错误。

---

### 任务 9：最终提交

**Files:**
- N/A

- [ ] **Step 1: 查看 git 状态**

Run: `git status`
Expected: 所有更改已提交

- [ ] **Step 2: 最终验证（可选）**

启动应用并访问 Swagger UI (`/doc.html`) 验证新接口是否出现在「应用：汉字查询接口」分组下。

---

## 验收标准

1. 两个接口可用：
   - `GET /api/app/character/search?blurry=你` - 返回汉字列表（不含时间字段）
   - `GET /api/app/character/{id}` - 返回汉字详情（不含时间字段）

2. 搜索仅匹配 `character` 字段，不匹配 `pinyin`

3. 所有返回数据不含 `createTime`、`updateTime`、`createBy`、`updateBy` 等审计字段

4. 所有代码编译通过，无错误
