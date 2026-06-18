# APP 部首学习接口实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 APP 端部首学习两个接口（部首列表 + 部首详情含关联汉字分页）

**Architecture:** 在 grid-system 层扩展 Repository 和 Service 方法，在 grid-app 层新建 VO 和 Controller，遵循现有 AppCharCharacterController 模式

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL, Lombok, Swagger

## Global Constraints

- VO 放在 `com.naon.grid.modules.app.rest.vo` 包下
- Controller 放在 `com.naon.grid.modules.app.rest` 包下，使用 `@Slf4j` `@RestController` `@RequiredArgsConstructor`
- APP 端只返回已发布数据（`status=1`, `publishStatus='published'`）
- APP VO 不含审计字段（createBy/updateBy/createTime/updateTime）
- 部首详情汉字分页固定每页8条，用 `hasNext` 标记告知前端

---

### Task 1: Repository 层 — 新增查询方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/charradical/CharRadicalRepository.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java`

**Interfaces:**
- Produces: `CharRadicalRepository.findByStatusAndPublishStatusOrderByIdAsc(Integer, String)` → `List<CharRadical>`
- Produces: `CharCharacterRepository.findByRadicalIdAndStatusAndPublishStatus(Long, Integer, String, Pageable)` → `Page<CharCharacter>`

- [ ] **Step 1: 给 CharRadicalRepository 添加查询所有已发布部首的方法**

在 `CharRadicalRepository` 接口中添加：

```java
List<CharRadical> findByStatusAndPublishStatusOrderByIdAsc(Integer status, String publishStatus);
```

- [ ] **Step 2: 给 CharCharacterRepository 添加按 radicalId 分页查询方法**

在 `CharCharacterRepository` 接口中添加：

```java
Page<CharCharacter> findByRadicalIdAndStatusAndPublishStatus(Long radicalId, Integer status, String publishStatus, Pageable pageable);
```

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/charradical/CharRadicalRepository.java
git add grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java
git commit -m "feat: add radical query methods to repos"
```

---

### Task 2: Service 层 — 新增业务方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/charradical/CharRadicalService.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/charradical/impl/CharRadicalServiceImpl.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

**Interfaces:**
- Consumes: `CharRadicalRepository.findByStatusAndPublishStatusOrderByIdAsc(Integer, String)`
- Consumes: `CharCharacterRepository.findByRadicalIdAndStatusAndPublishStatus(Long, Integer, String, Pageable)`
- Produces: `CharRadicalService.findAllPublished()` → `List<CharRadicalDto>`
- Produces: `CharCharacterService.findPublishedByRadicalId(Long, Pageable)` → `Page<CharCharacterDto>`

- [ ] **Step 1: CharRadicalService 接口添加 findAllPublished**

```java
List<CharRadicalDto> findAllPublished();
```

- [ ] **Step 2: CharRadicalServiceImpl 实现 findAllPublished**

```java
@Override
public List<CharRadicalDto> findAllPublished() {
    List<CharRadical> list = charRadicalRepository.findByStatusAndPublishStatusOrderByIdAsc(
            StatusEnum.ENABLED.getCode(), PublishStatusEnum.PUBLISHED.getCode());
    return list.stream().map(this::toBaseDto).collect(Collectors.toList());
}
```

- [ ] **Step 3: CharCharacterService 接口添加 findPublishedByRadicalId**

```java
Page<CharCharacterDto> findPublishedByRadicalId(Long radicalId, Pageable pageable);
```

- [ ] **Step 4: CharCharacterServiceImpl 实现 findPublishedByRadicalId**

```java
@Override
public Page<CharCharacterDto> findPublishedByRadicalId(Long radicalId, Pageable pageable) {
    Page<CharCharacter> page = charCharacterRepository.findByRadicalIdAndStatusAndPublishStatus(
            radicalId, StatusEnum.ENABLED.getCode(), PublishStatusEnum.PUBLISHED.getCode(), pageable);
    return page.map(charCharacterMapper::toDto);
}
```

确保在类顶部已经 import 了 `org.springframework.data.domain.Page`（已有的）。

- [ ] **Step 5: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/charradical/CharRadicalService.java
git add grid-system/src/main/java/com/naon/grid/backend/service/charradical/impl/CharRadicalServiceImpl.java
git add grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
git commit -m "feat: add findAllPublished and findPublishedByRadicalId to services"
```

---

### Task 3: APP VO 类 — 新建 3 个 VO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharRadicalBaseVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppRadicalCharVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharRadicalDetailVO.java`

**Interfaces:**
- Produces: `AppCharRadicalBaseVO` — 部首列表接口返回
- Produces: `AppRadicalCharVO` — 详情接口汉字条目
- Produces: `AppCharRadicalDetailVO` — 详情接口返回体

- [ ] **Step 1: 新建 AppCharRadicalBaseVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppCharRadicalBaseVO implements Serializable {

    @ApiModelProperty(value = "部首ID")
    private Long id;

    @ApiModelProperty(value = "部首字符")
    private String radical;

    @ApiModelProperty(value = "部首名称")
    private String radicalName;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "关联部首ID")
    private Long relationId;
}
```

- [ ] **Step 2: 新建 AppRadicalCharVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppRadicalCharVO implements Serializable {

    @ApiModelProperty(value = "汉字ID")
    private Integer id;

    @ApiModelProperty(value = "汉字字形")
    private String character;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "拼音")
    private String pinyin;
}
```

- [ ] **Step 3: 新建 AppCharRadicalDetailVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppCharRadicalDetailVO implements Serializable {

    // 部首基本信息
    @ApiModelProperty(value = "部首ID")
    private Long id;

    @ApiModelProperty(value = "部首字符")
    private String radical;

    @ApiModelProperty(value = "部首名称")
    private String radicalName;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @ApiModelProperty(value = "关联部首ID")
    private Long relationId;

    // 字节汉字列表
    @ApiModelProperty(value = "当前页关联汉字列表")
    private List<AppRadicalCharVO> characters;

    // 是否有下一页
    @ApiModelProperty(value = "是否还有下一页")
    private Boolean hasNext;
}
```

- [ ] **Step 4: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharRadicalBaseVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppRadicalCharVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharRadicalDetailVO.java
git commit -m "feat: add APP radical VO classes"
```

---

### Task 4: Controller — 实现 AppCharRadicalController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharRadicalController.java`

**Interfaces:**
- Consumes: `CharRadicalService.findAllPublished()` → `List<CharRadicalDto>`
- Consumes: `CharRadicalService.findById(Long)` → `CharRadicalDto`
- Consumes: `CharCharacterService.findPublishedByRadicalId(Long, Pageable)` → `Page<CharCharacterDto>`

- [ ] **Step 1: 新建 AppCharRadicalController**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCharRadicalDetailVO;
import com.naon.grid.modules.app.rest.vo.AppRadicalCharVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/char/radical")
@Api(tags = "用户：部首学习")
public class AppCharRadicalController {

    private final CharRadicalService charRadicalService;
    private final CharCharacterService charCharacterService;

    @ApiOperation("部首列表")
    @AnonymousGetMapping
    public ResponseEntity<List<AppCharRadicalBaseVO>> list() {
        List<CharRadicalDto> dtos = charRadicalService.findAllPublished();
        List<AppCharRadicalBaseVO> vos = toBaseVOList(dtos);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("部首详情（含关联汉字分页）")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppCharRadicalDetailVO> detail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        CharRadicalDto radicalDto = charRadicalService.findById(id);
        Page<CharCharacterDto> charPage = charCharacterService.findPublishedByRadicalId(
                id, PageRequest.of(page, size));

        AppCharRadicalDetailVO vo = toDetailVO(radicalDto, charPage);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    // ==================== 转换方法 ====================

    private List<AppCharRadicalBaseVO> toBaseVOList(List<CharRadicalDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private AppCharRadicalBaseVO toBaseVO(CharRadicalDto dto) {
        AppCharRadicalBaseVO vo = new AppCharRadicalBaseVO();
        vo.setId(dto.getId());
        vo.setRadical(dto.getRadical());
        vo.setRadicalName(dto.getRadicalName());
        vo.setStrokeNum(dto.getStrokeNum());
        vo.setRelationId(dto.getRelationId());
        return vo;
    }

    private AppCharRadicalDetailVO toDetailVO(CharRadicalDto radicalDto, Page<CharCharacterDto> charPage) {
        AppCharRadicalDetailVO vo = new AppCharRadicalDetailVO();
        vo.setId(radicalDto.getId());
        vo.setRadical(radicalDto.getRadical());
        vo.setRadicalName(radicalDto.getRadicalName());
        vo.setStrokeNum(radicalDto.getStrokeNum());
        vo.setEvolutionDesc(radicalDto.getEvolutionDesc());
        vo.setRelationId(radicalDto.getRelationId());

        List<AppRadicalCharVO> charVOs = charPage.getContent().stream().map(this::toCharVO).collect(Collectors.toList());
        vo.setCharacters(charVOs);
        vo.setHasNext(charPage.hasNext());

        return vo;
    }

    private AppRadicalCharVO toCharVO(CharCharacterDto dto) {
        AppRadicalCharVO vo = new AppRadicalCharVO();
        vo.setId(dto.getId());
        vo.setCharacter(dto.getCharacter());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPinyin(dto.getPinyin());
        return vo;
    }
}
```

- [ ] **Step 2: 启动验证**

```bash
cd grid-bootstrap
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后访问 Swagger: http://localhost:8000/doc.html，确认 `用户：部首学习` 分组下出现两个接口。

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharRadicalController.java
git commit -m "feat: implement AppCharRadicalController with radical list and detail APIs"
```
