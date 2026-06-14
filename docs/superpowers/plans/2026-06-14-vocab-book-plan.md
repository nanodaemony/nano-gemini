# 词汇书用户侧接口实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现词汇书（vocab_book）的用户侧 API，包含书籍列表查询和书籍下词汇列表查询。

**Architecture:** 参照已有的汉字书模式（CharBook → CharBookRepository → CharBookService → AppCharBookController），实现词汇书对应的全链路。词汇来源支持两种模式：按 HSK 等级查询词汇表，或按 word_ids JSON 列表查询。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL, Fastjson2, Lombok, Knife4j(Swagger)

---

### Task 1: 新增 VocabWordRepository 查询方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabWordRepository.java`

- [ ] **Step 1: 在 VocabWordRepository 中添加两个查询方法**

在当前接口中追加以下两个方法，用于支持词汇书的两种词汇查询模式：

```java
package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabWordRepository extends JpaRepository<VocabWord, Integer>, JpaSpecificationExecutor<VocabWord> {

    List<VocabWord> findByWordAndStatus(String word, Integer status);

    // ========== 以下为新增方法 ==========

    /**
     * 根据 HSK 等级查询已发布的词汇
     *
     * @param hskLevel      HSK等级
     * @param status        有效状态
     * @param publishStatus 发布状态
     * @return 匹配的词汇列表
     */
    List<VocabWord> findByHskLevelAndStatusAndPublishStatus(String hskLevel, Integer status, String publishStatus);

    /**
     * 根据 ID 列表批量查询词汇（结果不保证顺序，需调用方自行排序）
     *
     * @param ids 词汇ID列表
     * @return 匹配的词汇列表
     */
    List<VocabWord> findByIdIn(List<Integer> ids);
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system -am -q`
Expected: BUILD SUCCESS（新方法名与 VocabWord 实体字段名一致，Spring Data JPA 能自动解析）

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabWordRepository.java
git commit -m "feat: add findByHskLevelAndStatusAndPublishStatus and findByIdIn to VocabWordRepository"
```

---

### Task 2: 创建 VocabBook DO 实体

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabBook.java`

- [ ] **Step 1: 创建 VocabBook.java**

与 `CharBook.java` 相同的模式，映射 `vocab_book` 表：

```java
package com.naon.grid.backend.domain.vocabulary;

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
@Table(name = "vocab_book")
public class VocabBook implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "词汇书ID", hidden = true)
    private Long id;

    @Column(name = "type", nullable = false, length = 32)
    @ApiModelProperty(value = "词汇书类型")
    private String type;

    @Column(name = "name", nullable = false, length = 32)
    @ApiModelProperty(value = "词汇书名称")
    private String name;

    @Column(name = "sub_name", nullable = false, length = 32)
    @ApiModelProperty(value = "词汇书子名称")
    private String subName;

    @Column(name = "cover_image", nullable = false, length = 512)
    @ApiModelProperty(value = "词汇书封面图")
    private String coverImage;

    @Column(name = "`desc`", length = 1024)
    @ApiModelProperty(value = "词汇书描述")
    private String desc;

    @Column(name = "hsk_level", length = 32)
    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @Column(name = "word_ids", columnDefinition = "text")
    @ApiModelProperty(value = "词汇ID列表JSON")
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

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabBook.java
git commit -m "feat: add VocabBook entity for vocab_book table"
```

---

### Task 3: 创建 VocabBookRepository

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabBookRepository.java`

- [ ] **Step 1: 创建 VocabBookRepository.java**

```java
package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabBookRepository extends JpaRepository<VocabBook, Long> {

    /**
     * 查询所有有效书籍，按排序字段倒序排列
     *
     * @param status 有效状态
     * @return 书籍列表
     */
    List<VocabBook> findByStatusOrderByOrderDesc(Integer status);
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabBookRepository.java
git commit -m "feat: add VocabBookRepository"
```

---

### Task 4: 创建 VocabBookService 接口和实现

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabBookService.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabBookServiceImpl.java`

- [ ] **Step 1: 创建 VocabBookService 接口**

```java
package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabBook;
import com.naon.grid.backend.domain.vocabulary.VocabWord;

import java.util.List;

public interface VocabBookService {

    /**
     * 查询所有有效的词汇书（status=1），按 order 倒序排列
     */
    List<VocabBook> findAvailableBooks();

    /**
     * 根据 ID 查询词汇书，不存在或已下架抛 EntityNotFoundException
     */
    VocabBook findAvailableById(Long id);

    /**
     * 根据书籍配置查询对应的词汇列表
     * - 如果 book.hskLevel 不为空：按 HSK 等级从 vocab_word 查询已发布的词汇
     * - 如果 book.wordIds 不为空：按 ID 列表查询，并保持 word_ids 中的顺序
     * - 两者均为空：返回空列表
     */
    List<VocabWord> findWordsByBook(VocabBook book);
}
```

- [ ] **Step 2: 创建 VocabBookServiceImpl 实现类**

```java
package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.VocabBook;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.backend.repo.vocabulary.VocabBookRepository;
import com.naon.grid.backend.repo.vocabulary.VocabWordRepository;
import com.naon.grid.backend.service.vocabulary.VocabBookService;
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
public class VocabBookServiceImpl implements VocabBookService {

    private final VocabBookRepository vocabBookRepository;
    private final VocabWordRepository vocabWordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VocabBook> findAvailableBooks() {
        return vocabBookRepository.findByStatusOrderByOrderDesc(StatusEnum.ENABLED.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public VocabBook findAvailableById(Long id) {
        VocabBook book = vocabBookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabBook.class, "id", String.valueOf(id)));
        if (!StatusEnum.ENABLED.getCode().equals(book.getStatus())) {
            throw new EntityNotFoundException(VocabBook.class, "id", String.valueOf(id));
        }
        return book;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabWord> findWordsByBook(VocabBook book) {
        // 方式1：通过 HSK 等级查询
        if (book.getHskLevel() != null && !book.getHskLevel().trim().isEmpty()) {
            return vocabWordRepository.findByHskLevelAndStatusAndPublishStatus(
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

            List<VocabWord> words = vocabWordRepository.findByIdIn(idList);
            // 过滤：仅保留已发布且有效的词汇
            words.removeIf(w ->
                    !StatusEnum.ENABLED.getCode().equals(w.getStatus())
                            || !PublishStatusEnum.PUBLISHED.getCode().equals(w.getPublishStatus())
            );

            // 按 word_ids 原始顺序排序
            Map<Integer, VocabWord> wordMap = words.stream()
                    .collect(Collectors.toMap(VocabWord::getId, w -> w, (a, b) -> a));
            List<VocabWord> sorted = new ArrayList<>();
            for (Integer id : idList) {
                VocabWord w = wordMap.get(id);
                if (w != null) {
                    sorted.add(w);
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
            throw new BadRequestException("词汇ID列表数据解析失败");
        }
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl grid-system -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabBookService.java
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabBookServiceImpl.java
git commit -m "feat: add VocabBookService interface and implementation"
```

---

### Task 5: 创建用户端 VO 类

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabBookListVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabBookWordVO.java`

- [ ] **Step 1: 创建 AppVocabBookListVO.java**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户端词汇书列表VO
 */
@Getter
@Setter
public class AppVocabBookListVO implements Serializable {

    @ApiModelProperty(value = "词汇书ID")
    private Long id;

    @ApiModelProperty(value = "词汇书类型")
    private String type;

    @ApiModelProperty(value = "词汇书名称")
    private String name;

    @ApiModelProperty(value = "词汇书子名称")
    private String subName;

    @ApiModelProperty(value = "词汇书封面图")
    private String coverImage;

    @ApiModelProperty(value = "词汇书描述")
    private String desc;
}
```

- [ ] **Step 2: 创建 AppVocabBookWordVO.java**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户端词汇书下的词汇VO
 */
@Getter
@Setter
public class AppVocabBookWordVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl grid-app -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabBookListVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabBookWordVO.java
git commit -m "feat: add AppVocabBookListVO and AppVocabBookWordVO"
```

---

### Task 6: 创建 AppVocabBookController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabBookController.java`

- [ ] **Step 1: 创建 AppVocabBookController.java**

参照 `AppCharBookController`，提供两个匿名接口：

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.domain.vocabulary.VocabBook;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.backend.service.vocabulary.VocabBookService;
import com.naon.grid.modules.app.rest.vo.AppVocabBookListVO;
import com.naon.grid.modules.app.rest.vo.AppVocabBookWordVO;
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
 * 用户端词汇书接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/vocab-book")
@Api(tags = "用户：词汇书接口")
public class AppVocabBookController {

    private final VocabBookService vocabBookService;

    @ApiOperation("查询词汇书列表")
    @AnonymousGetMapping
    public ResponseEntity<List<AppVocabBookListVO>> listBooks() {
        List<VocabBook> books = vocabBookService.findAvailableBooks();
        List<AppVocabBookListVO> vos = toBookVOList(books);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("查询词汇书下的词汇列表")
    @AnonymousGetMapping("/{id}/words")
    public ResponseEntity<List<AppVocabBookWordVO>> listWords(@PathVariable Long id) {
        VocabBook book = vocabBookService.findAvailableById(id);
        List<VocabWord> words = vocabBookService.findWordsByBook(book);
        List<AppVocabBookWordVO> vos = toWordVOList(words);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    private List<AppVocabBookListVO> toBookVOList(List<VocabBook> books) {
        if (books == null) {
            return Collections.emptyList();
        }
        return books.stream().map(this::toBookVO).collect(Collectors.toList());
    }

    private AppVocabBookListVO toBookVO(VocabBook book) {
        AppVocabBookListVO vo = new AppVocabBookListVO();
        vo.setId(book.getId());
        vo.setType(book.getType());
        vo.setName(book.getName());
        vo.setSubName(book.getSubName());
        vo.setCoverImage(book.getCoverImage());
        vo.setDesc(book.getDesc());
        return vo;
    }

    private List<AppVocabBookWordVO> toWordVOList(List<VocabWord> words) {
        if (words == null) {
            return Collections.emptyList();
        }
        return words.stream().map(this::toWordVO).collect(Collectors.toList());
    }

    private AppVocabBookWordVO toWordVO(VocabWord word) {
        AppVocabBookWordVO vo = new AppVocabBookWordVO();
        vo.setId(word.getId());
        vo.setWord(word.getWord());
        vo.setPinyin(word.getPinyin());
        vo.setHskLevel(word.getHskLevel());
        return vo;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-app -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabBookController.java
git commit -m "feat: add AppVocabBookController with list books and list words endpoints"
```

---

### Task 7: 全局编译验证

- [ ] **Step 1: 完整编译**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 整体提交确认**

```bash
git status
# 确认工作区干净
```
