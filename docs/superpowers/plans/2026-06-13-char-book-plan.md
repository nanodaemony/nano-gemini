# 汉字书（Char Book）功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增汉字书功能，支持用户浏览汉字书列表、查看书籍下的汉字列表并跳转到详情页。

**Architecture:** 新增 `char_book` 表（无草稿工作流），在 grid-system 新增 DO/Repository/Service，在 grid-app 新增 Controller/VO。书籍下的汉字通过 `hsk_level` 或 `word_ids` 两种方式关联。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL, Fastjson2, Lombok, MapStruct, Knife4j

---

### Task 1: SQL — 追加 char_book 建表语句

**Files:**
- Modify: `sql/biz_character.sql` (append at end)

- [ ] **Step 1: 在 biz_character.sql 末尾追加 char_book 表**

```sql
-- 汉字书表
-- 注：一本书对应一条数据，汉字来源通过 hsk_level 或 word_ids 两种方式。
CREATE TABLE `char_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '汉字书ID',
  `type` varchar(32) NOT NULL COMMENT '汉字书类型, 参考枚举：CharBookTypeEnum',
  `name` varchar(32) NOT NULL COMMENT '汉字书名称',
  `sub_name` varchar(32) NOT NULL COMMENT '汉字书子名称',
  `cover_image` varchar(512) NOT NULL COMMENT '汉字书封面图',
  `desc` varchar(1024) DEFAULT NULL COMMENT '汉字书描述',
  `hsk_level` varchar(32) DEFAULT NULL COMMENT 'HSK等级(如果有等级则按照等级去检索汉字)',
  `word_ids` text DEFAULT NULL COMMENT '汉字ID列表(可能为空，此时使用hsk_level查询)',
  `order` int DEFAULT 0 COMMENT '排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字书表';
```

- [ ] **Step 2: Commit**

```bash
git add sql/biz_character.sql
git commit -m "feat: add char_book table DDL"
```

---

### Task 2: Entity — 创建 CharBook.java

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharBook.java`

- [ ] **Step 1: 创建 CharBook 实体**

```java
package com.naon.grid.backend.domain.character;

import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "char_book")
public class CharBook implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "汉字书ID", hidden = true)
    private Long id;

    @Column(name = "type", nullable = false, length = 32)
    @ApiModelProperty(value = "汉字书类型")
    private String type;

    @Column(name = "name", nullable = false, length = 32)
    @ApiModelProperty(value = "汉字书名称")
    private String name;

    @Column(name = "sub_name", nullable = false, length = 32)
    @ApiModelProperty(value = "汉字书子名称")
    private String subName;

    @Column(name = "cover_image", nullable = false, length = 512)
    @ApiModelProperty(value = "汉字书封面图")
    private String coverImage;

    @Column(name = "`desc`", length = 1024)
    @ApiModelProperty(value = "汉字书描述")
    private String desc;

    @Column(name = "hsk_level", length = 32)
    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @Column(name = "word_ids", columnDefinition = "text")
    @ApiModelProperty(value = "汉字ID列表JSON")
    private String wordIds;

    @Column(name = "`order`")
    @ApiModelProperty(value = "排序(值大的排前面)")
    private Integer order;

    @Column(name = "create_time")
    @ApiModelProperty(value = "创建时间", hidden = true)
    private Timestamp createTime;

    @Column(name = "update_time")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "有效状态：1-有效，0-无效", hidden = true)
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/character/CharBook.java
git commit -m "feat: add CharBook entity"
```

---

### Task 3: Repository — 创建 CharBookRepository.java

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharBookRepository.java`

- [ ] **Step 1: 创建 CharBookRepository**

```java
package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharBookRepository extends JpaRepository<CharBook, Long> {

    /**
     * 查询所有有效书籍，按排序字段倒序排列
     *
     * @param status 有效状态
     * @return 书籍列表
     */
    List<CharBook> findByStatusOrderByOrderDesc(Integer status);
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/character/CharBookRepository.java
git commit -m "feat: add CharBookRepository"
```

---

### Task 4: Repository — 给 CharCharacterRepository 新增两个查询方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java`

- [ ] **Step 1: 追加两个查询方法**

```java
    /**
     * 根据 HSK 等级查询已发布的汉字
     *
     * @param level         HSK等级（对应实体字段 level，数据库列 hsk_level）
     * @param status        有效状态
     * @param publishStatus 发布状态
     * @return 匹配的汉字列表
     */
    List<CharCharacter> findByLevelAndStatusAndPublishStatus(String level, Integer status, String publishStatus);

    /**
     * 根据 ID 列表批量查询汉字
     *
     * @param ids 汉字ID列表
     * @return 匹配的汉字列表（不保证顺序，需调用方自行排序）
     */
    List<CharCharacter> findByIdIn(List<Integer> ids);
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java
git commit -m "feat: add findByLevelAndStatusAndPublishStatus and findByIdIn to CharCharacterRepository"
```

---

### Task 5: Service — 创建 CharBookService 接口

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/CharBookService.java`

- [ ] **Step 1: 创建 CharBookService 接口**

```java
package com.naon.grid.backend.service.character;

import com.naon.grid.backend.domain.character.CharBook;
import com.naon.grid.backend.domain.character.CharCharacter;

import java.util.List;

public interface CharBookService {

    /**
     * 查询所有有效的汉字书（status=1），按 order 倒序排列
     */
    List<CharBook> findAvailableBooks();

    /**
     * 根据 ID 查询汉字书，不存在或已下架抛 EntityNotFoundException
     */
    CharBook findAvailableById(Long id);

    /**
     * 根据书籍配置查询对应的汉字列表
     * - 如果 book.hskLevel 不为空：按 HSK 等级从 char_character 查询已发布的汉字
     * - 如果 book.wordIds 不为空：按 ID 列表查询，并保持 word_ids 中的顺序
     * - 两者均为空：返回空列表
     */
    List<CharCharacter> findCharactersByBook(CharBook book);
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/CharBookService.java
git commit -m "feat: add CharBookService interface"
```

---

### Task 6: Service — 创建 CharBookServiceImpl 实现

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharBookServiceImpl.java`

- [ ] **Step 1: 创建 CharBookServiceImpl**

```java
package com.naon.grid.backend.service.character.impl;

import com.naon.grid.backend.domain.character.CharBook;
import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.repo.character.CharBookRepository;
import com.naon.grid.backend.repo.character.CharCharacterRepository;
import com.naon.grid.backend.service.character.CharBookService;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CharBookServiceImpl implements CharBookService {

    private final CharBookRepository charBookRepository;
    private final CharCharacterRepository charCharacterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CharBook> findAvailableBooks() {
        return charBookRepository.findByStatusOrderByOrderDesc(StatusEnum.ENABLED.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public CharBook findAvailableById(Long id) {
        CharBook book = charBookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharBook.class, "id", String.valueOf(id)));
        if (!StatusEnum.ENABLED.getCode().equals(book.getStatus())) {
            throw new EntityNotFoundException(CharBook.class, "id", String.valueOf(id));
        }
        return book;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CharCharacter> findCharactersByBook(CharBook book) {
        // 方式1：通过 HSK 等级查询
        if (book.getHskLevel() != null && !book.getHskLevel().trim().isEmpty()) {
            return charCharacterRepository.findByLevelAndStatusAndPublishStatus(
                    book.getHskLevel().trim(),
                    StatusEnum.ENABLED.getCode(),
                    PublishStatusEnum.PUBLISHED.getCode()
            );
        }

        // 方式2：通过 word_ids 查询
        if (book.getWordIds() != null && !book.getWordIds().trim().isEmpty()) {
            List<Integer> idList = parseWordIds(book.getWordIds());
            if (idList.isEmpty()) {
                return Collections.emptyList();
            }

            List<CharCharacter> characters = charCharacterRepository.findByIdIn(idList);
            // 过滤：仅保留已发布且有效的汉字
            characters.removeIf(c ->
                    !StatusEnum.ENABLED.getCode().equals(c.getStatus())
                            || !PublishStatusEnum.PUBLISHED.getCode().equals(c.getPublishStatus())
            );

            // 按 word_ids 原始顺序排序
            Map<Integer, CharCharacter> charMap = characters.stream()
                    .collect(Collectors.toMap(CharCharacter::getId, c -> c, (a, b) -> a));
            List<CharCharacter> sorted = new ArrayList<>();
            for (Integer id : idList) {
                CharCharacter c = charMap.get(id);
                if (c != null) {
                    sorted.add(c);
                }
            }
            return sorted;
        }

        return Collections.emptyList();
    }

    /**
     * 解析 word_ids JSON 字符串为 Integer 列表
     */
    private List<Integer> parseWordIds(String wordIds) {
        try {
            List<String> strList = JsonUtils.parseStringList(wordIds);
            return strList.stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BadRequestException("汉字ID列表数据解析失败");
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharBookServiceImpl.java
git commit -m "feat: add CharBookServiceImpl"
```

---

### Task 7: VO — 创建 AppCharBookListVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharBookListVO.java`

- [ ] **Step 1: 创建书籍列表响应 VO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户端汉字书列表VO
 */
@Getter
@Setter
public class AppCharBookListVO implements Serializable {

    @ApiModelProperty(value = "汉字书ID")
    private Long id;

    @ApiModelProperty(value = "汉字书类型")
    private String type;

    @ApiModelProperty(value = "汉字书名称")
    private String name;

    @ApiModelProperty(value = "汉字书子名称")
    private String subName;

    @ApiModelProperty(value = "汉字书封面图")
    private String coverImage;

    @ApiModelProperty(value = "汉字书描述")
    private String desc;
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharBookListVO.java
git commit -m "feat: add AppCharBookListVO"
```

---

### Task 8: VO — 创建 AppCharBookCharVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharBookCharVO.java`

- [ ] **Step 1: 创建书籍汉字列表响应 VO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户端汉字书下的汉字VO
 */
@Getter
@Setter
public class AppCharBookCharVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharBookCharVO.java
git commit -m "feat: add AppCharBookCharVO"
```

---

### Task 9: Controller — 创建 AppCharBookController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharBookController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.domain.character.CharBook;
import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.service.character.CharBookService;
import com.naon.grid.modules.app.rest.vo.AppCharBookCharVO;
import com.naon.grid.modules.app.rest.vo.AppCharBookListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端汉字书接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/char-book")
@Api(tags = "用户：汉字书接口")
public class AppCharBookController {

    private final CharBookService charBookService;

    @ApiOperation("查询汉字书列表")
    @AnonymousGetMapping
    public ResponseEntity<List<AppCharBookListVO>> listBooks() {
        List<CharBook> books = charBookService.findAvailableBooks();
        List<AppCharBookListVO> vos = toBookVOList(books);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("查询汉字书下的汉字列表")
    @AnonymousGetMapping("/{id}/characters")
    public ResponseEntity<List<AppCharBookCharVO>> listCharacters(@PathVariable Long id) {
        CharBook book = charBookService.findAvailableById(id);
        List<CharCharacter> characters = charBookService.findCharactersByBook(book);
        List<AppCharBookCharVO> vos = toCharVOList(characters);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    private List<AppCharBookListVO> toBookVOList(List<CharBook> books) {
        if (books == null) {
            return Collections.emptyList();
        }
        return books.stream().map(this::toBookVO).collect(Collectors.toList());
    }

    private AppCharBookListVO toBookVO(CharBook book) {
        AppCharBookListVO vo = new AppCharBookListVO();
        vo.setId(book.getId());
        vo.setType(book.getType());
        vo.setName(book.getName());
        vo.setSubName(book.getSubName());
        vo.setCoverImage(book.getCoverImage());
        vo.setDesc(book.getDesc());
        return vo;
    }

    private List<AppCharBookCharVO> toCharVOList(List<CharCharacter> characters) {
        if (characters == null) {
            return Collections.emptyList();
        }
        return characters.stream().map(this::toCharVO).collect(Collectors.toList());
    }

    private AppCharBookCharVO toCharVO(CharCharacter charEntity) {
        AppCharBookCharVO vo = new AppCharBookCharVO();
        vo.setId(charEntity.getId());
        vo.setCharacter(charEntity.getCharacter());
        vo.setPinyin(charEntity.getPinyin());
        vo.setHskLevel(charEntity.getLevel());
        return vo;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharBookController.java
git commit -m "feat: add AppCharBookController"
```

---

### Task 10: 编译验证

**Files:** (no file changes)

- [ ] **Step 1: 编译项目验证无错误**

```bash
mvn compile -DskipTests -q
```

Expected: BUILD SUCCESS（无错误）。

- [ ] **Step 2: 最后一次提交**

```bash
git add -A
git commit -m "feat: complete char book feature implementation"
```
