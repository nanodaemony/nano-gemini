# 阿里云 OSS 存储功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 grid-tools 模块中添加阿里云 OSS 存储功能，包含文件上传、查询、删除，文件元信息存储在 oss_resource_meta 表中，上传后返回完整文件信息。

**Architecture:** 参考现有 S3Storage 和 LocalStorage 的实现模式，保持代码风格一致，使用 JPA + MapStruct。

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, MapStruct, Aliyun OSS SDK 3.17.4

---

## 文件结构总览

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| Modify | `grid-tools/pom.xml` | 添加阿里云 OSS SDK 依赖 |
| Create | `grid-tools/src/main/java/com/naon/grid/config/AliOssConfig.java` | OSS 配置类 |
| Create | `grid-tools/src/main/java/com/naon/grid/domain/AliOssStorage.java` | 实体类 |
| Create | `grid-tools/src/main/java/com/naon/grid/repository/AliOssStorageRepository.java` | Repository |
| Create | `grid-tools/src/main/java/com/naon/grid/service/dto/AliOssStorageDto.java` | DTO |
| Create | `grid-tools/src/main/java/com/naon/grid/service/dto/AliOssStorageQueryCriteria.java` | 查询条件 |
| Create | `grid-tools/src/main/java/com/naon/grid/service/mapstruct/AliOssStorageMapper.java` | MapStruct 映射 |
| Create | `grid-tools/src/main/java/com/naon/grid/service/AliOssStorageService.java` | Service 接口 |
| Create | `grid-tools/src/main/java/com/naon/grid/service/impl/AliOssStorageServiceImpl.java` | Service 实现 |
| Create | `grid-tools/src/main/java/com/naon/grid/rest/AliOssStorageController.java` | REST 接口 |

---

### Task 1: 添加 Maven 依赖

**Files:**
- Modify: `grid-tools/pom.xml`

- [ ] **Step 1: 修改 pom.xml 添加阿里云 OSS SDK 依赖**

```xml
    <properties>
        <mail.version>1.4.7</mail.version>
        <alipay.version>4.22.57.ALL</alipay.version>
        <aliyun.oss.version>3.17.4</aliyun.oss.version>
    </properties>
```

在 dependencies 中添加：

```xml
        <!-- 阿里云 OSS SDK -->
        <dependency>
            <groupId>com.aliyun.oss</groupId>
            <artifactId>aliyun-sdk-oss</artifactId>
            <version>${aliyun.oss.version}</version>
        </dependency>
```

- [ ] **Step 2: 验证项目可以正常编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/pom.xml
git commit -m "feat: add aliyun oss sdk dependency"
```

---

### Task 2: 创建配置类 AliOssConfig

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/config/AliOssConfig.java`

- [ ] **Step 1: 编写配置类**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ali.oss")
public class AliOssConfig {

    /**
     * OSS Endpoint
     */
    private String endpoint;

    /**
     * 访问密钥 ID
     */
    private String accessKeyId;

    /**
     * 访问密钥 Secret
     */
    private String accessKeySecret;

    /**
     * 默认存储桶名称
     */
    private String bucketName;

    /**
     * OSS 访问域名
     */
    private String domain;

    /**
     * 文件存储文件夹格式
     */
    private String timeformat = "yyyy-MM";

    /**
     * 创建 OSS 客户端
     * @return OSS 客户端实例
     */
    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/config/AliOssConfig.java
git commit -m "feat: add ali oss config class"
```

---

### Task 3: 创建实体类 AliOssStorage

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/domain/AliOssStorage.java`

- [ ] **Step 1: 编写实体类**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseEntity;
import javax.persistence.*;
import java.io.Serializable;

/**
 * 阿里云 OSS 存储实体类
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Getter
@Setter
@Entity
@Table(name = "oss_resource_meta")
public class AliOssStorage extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "原始文件名")
    private String fileName;

    @ApiModelProperty(value = "OSS 上存储的文件名 (UUID)")
    private String fileRealName;

    @ApiModelProperty(value = "文件大小")
    private String fileSize;

    @ApiModelProperty(value = "文件 MIME 类型")
    private String fileMimeType;

    @ApiModelProperty(value = "文件类型")
    private String fileType;

    @ApiModelProperty(value = "OSS 完整访问 URL")
    private String fileUrl;

    @ApiModelProperty(value = "存储桶名称")
    private String bucketName;

    public void copy(AliOssStorage source) {
        BeanUtil.copyProperties(source, this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/domain/AliOssStorage.java
git commit -m "feat: add ali oss storage entity class"
```

---

### Task 4: 创建 Repository

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/repository/AliOssStorageRepository.java`

- [ ] **Step 1: 编写 Repository**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.repository;

import com.naon.grid.domain.AliOssStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * 阿里云 OSS 存储 Repository
 * @author Zheng Jie
 * @date 2025-05-19
 */
public interface AliOssStorageRepository extends JpaRepository<AliOssStorage, Long>, JpaSpecificationExecutor<AliOssStorage> {

    /**
     * 根据 ID 查询 OSS 文件路径（这里是 fileUrl）
     * @param id 文件 ID
     * @return 文件 URL
     */
    @Query(value = "SELECT file_url FROM oss_resource_meta WHERE id = ?1", nativeQuery = true)
    String selectFileUrlById(Long id);

    /**
     * 根据 ID 查询真实文件名
     * @param id 文件 ID
     * @return 真实文件名
     */
    @Query(value = "SELECT file_real_name FROM oss_resource_meta WHERE id = ?1", nativeQuery = true)
    String selectFileRealNameById(Long id);
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/repository/AliOssStorageRepository.java
git commit -m "feat: add ali oss storage repository"
```

---

### Task 5: 创建 DTO 和 QueryCriteria

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/AliOssStorageDto.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/AliOssStorageQueryCriteria.java`

- [ ] **Step 1: 编写 AliOssStorageDto**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;

/**
 * 阿里云 OSS 存储 DTO
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Getter
@Setter
public class AliOssStorageDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "原始文件名")
    private String fileName;

    @ApiModelProperty(value = "OSS 上存储的文件名")
    private String fileRealName;

    @ApiModelProperty(value = "文件大小")
    private String fileSize;

    @ApiModelProperty(value = "文件 MIME 类型")
    private String fileMimeType;

    @ApiModelProperty(value = "文件类型")
    private String fileType;

    @ApiModelProperty(value = "OSS 完整访问 URL")
    private String fileUrl;

    @ApiModelProperty(value = "存储桶名称")
    private String bucketName;
}
```

- [ ] **Step 2: 编写 AliOssStorageQueryCriteria**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.naon.grid.annotation.Query;
import java.sql.Timestamp;
import java.util.List;

/**
 * 阿里云 OSS 存储查询条件
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Data
public class AliOssStorageQueryCriteria {

    @Query(type = Query.Type.INNER_LIKE)
    @ApiModelProperty(value = "文件名称")
    private String fileName;

    @Query(type = Query.Type.BETWEEN)
    @ApiModelProperty(value = "创建时间")
    private List<Timestamp> createTime;
}
```

- [ ] **Step 3: 验证编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/dto/AliOssStorageDto.java grid-tools/src/main/java/com/naon/grid/service/dto/AliOssStorageQueryCriteria.java
git commit -m "feat: add ali oss storage dto and query criteria"
```

---

### Task 6: 创建 MapStruct Mapper

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/mapstruct/AliOssStorageMapper.java`

- [ ] **Step 1: 查看 LocalStorageMapper 作为参考**

先看现有模式。

- [ ] **Step 2: 编写 AliOssStorageMapper**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.service.mapstruct;

import com.naon.grid.base.BaseMapper;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.service.dto.AliOssStorageDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 阿里云 OSS 存储 MapStruct 映射
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AliOssStorageMapper extends BaseMapper<AliOssStorageDto, AliOssStorage> {
}
```

- [ ] **Step 3: 验证编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/mapstruct/AliOssStorageMapper.java
git commit -m "feat: add ali oss storage mapstruct mapper"
```

---

### Task 7: 创建 Service 接口

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/AliOssStorageService.java`

- [ ] **Step 1: 编写 Service 接口**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.service;

import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.service.dto.AliOssStorageQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 阿里云 OSS 存储 Service 接口
 * @author Zheng Jie
 * @date 2025-05-19
 */
public interface AliOssStorageService {

    /**
     * 分页查询
     * @param criteria 查询条件
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<AliOssStorageDto> queryAll(AliOssStorageQueryCriteria criteria, Pageable pageable);

    /**
     * 查询所有数据不分页
     * @param criteria 查询条件
     * @return 数据列表
     */
    List<AliOssStorageDto> queryAll(AliOssStorageQueryCriteria criteria);

    /**
     * 根据 ID 查询
     * @param id 文件 ID
     * @return 文件信息 DTO
     */
    AliOssStorageDto findById(Long id);

    /**
     * 上传文件到 OSS
     * @param file 上传的文件
     * @return 保存后的存储对象（包含完整 URL）
     */
    AliOssStorage upload(MultipartFile file);

    /**
     * 修改文件信息
     * @param resources 文件信息
     */
    void update(AliOssStorage resources);

    /**
     * 多选删除
     * @param ids ID 列表
     */
    void deleteAll(List<Long> ids);

    /**
     * 导出数据
     * @param all 待导出的数据
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    void download(List<AliOssStorageDto> all, HttpServletResponse response) throws IOException;
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/AliOssStorageService.java
git commit -m "feat: add ali oss storage service interface"
```

---

### Task 8: 创建 Service 实现类

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/impl/AliOssStorageServiceImpl.java`

- [ ] **Step 1: 编写 Service 实现**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.naon.grid.config.AliOssConfig;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.repository.AliOssStorageRepository;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.service.dto.AliOssStorageQueryCriteria;
import com.naon.grid.service.mapstruct.AliOssStorageMapper;
import com.naon.grid.utils.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * 阿里云 OSS 存储 Service 实现
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliOssStorageServiceImpl implements AliOssStorageService {

    private final OSS ossClient;
    private final AliOssConfig aliOssConfig;
    private final AliOssStorageRepository aliOssStorageRepository;
    private final AliOssStorageMapper aliOssStorageMapper;

    @Override
    public PageResult<AliOssStorageDto> queryAll(AliOssStorageQueryCriteria criteria, Pageable pageable) {
        Page<AliOssStorage> page = aliOssStorageRepository.findAll((root, criteriaQuery, criteriaBuilder)
                -> QueryHelp.getPredicate(root, criteria, criteriaBuilder), pageable);
        return PageUtil.toPage(page.map(aliOssStorageMapper::toDto));
    }

    @Override
    public List<AliOssStorageDto> queryAll(AliOssStorageQueryCriteria criteria) {
        return aliOssStorageMapper.toDto(aliOssStorageRepository.findAll((root, criteriaQuery, criteriaBuilder)
                -> QueryHelp.getPredicate(root, criteria, criteriaBuilder)));
    }

    @Override
    public AliOssStorageDto findById(Long id) {
        AliOssStorage storage = aliOssStorageRepository.findById(id).orElseGet(AliOssStorage::new);
        ValidationUtil.isNull(storage.getId(), "AliOssStorage", "id", id);
        return aliOssStorageMapper.toDto(storage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AliOssStorage upload(MultipartFile file) {
        String bucketName = aliOssConfig.getBucketName();
        // 检查存储桶是否存在，不存在则创建
        if (!bucketExists(bucketName)) {
            log.warn("存储桶 {} 不存在，尝试创建...", bucketName);
            createBucket(bucketName);
            log.info("存储桶 {} 创建成功。", bucketName);
        }
        // 获取原始文件名
        String originalName = file.getOriginalFilename();
        if (StringUtils.isBlank(originalName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        // 生成存储路径和文件名（UUID）
        String folder = DateUtil.format(new Date(), aliOssConfig.getTimeformat());
        String extension = FileUtil.getExtensionName(originalName);
        String fileRealName = IdUtil.simpleUUID() + "." + extension;
        String filePath = folder + "/" + fileRealName;
        // 构建访问 URL
        String fileUrl = aliOssConfig.getDomain() + "/" + filePath;

        AliOssStorage storage = new AliOssStorage();
        try {
            // 上传文件到 OSS
            ossClient.putObject(bucketName, filePath, file.getInputStream());

            // 设置存储对象属性
            storage.setFileName(originalName);
            storage.setFileRealName(fileRealName);
            storage.setFileSize(FileUtil.getSize(file.getSize()));
            storage.setFileMimeType(FileUtil.getMimeType(originalName));
            storage.setFileType(extension);
            storage.setFileUrl(fileUrl);
            storage.setBucketName(bucketName);

            // 保存到数据库
            aliOssStorageRepository.save(storage);
        } catch (IOException e) {
            log.error("上传文件到 OSS 失败: {}", e.getMessage(), e);
            throw new BadRequestException("文件上传失败: " + e.getMessage());
        }
        return storage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AliOssStorage resources) {
        AliOssStorage storage = aliOssStorageRepository.findById(resources.getId()).orElseGet(AliOssStorage::new);
        ValidationUtil.isNull(storage.getId(), "AliOssStorage", "id", resources.getId());
        storage.copy(resources);
        aliOssStorageRepository.save(storage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        String bucketName = aliOssConfig.getBucketName();
        if (!bucketExists(bucketName)) {
            throw new BadRequestException("存储桶不存在，请检查配置或权限。");
        }
        for (Long id : ids) {
            String fileRealName = aliOssStorageRepository.selectFileRealNameById(id);
            String fileUrl = aliOssStorageRepository.selectFileUrlById(id);
            if (fileUrl == null) {
                log.warn("未找到 ID 为 {} 的文件记录，跳过删除", id);
                continue;
            }
            // 从 URL 中解析出 OSS 的 object key (fileUrl 是 https://domain/folder/filename, 取后面部分)
            String objectKey = extractObjectKeyFromUrl(fileUrl, aliOssConfig.getDomain());
            try {
                // 从 OSS 删除
                ossClient.deleteObject(bucketName, objectKey);
                // 从数据库删除
                aliOssStorageRepository.deleteById(id);
            } catch (Exception e) {
                log.error("从 OSS 删除文件时出错: {}", e.getMessage(), e);
                throw new BadRequestException("删除文件失败: " + e.getMessage());
            }
        }
    }

    @Override
    public void download(List<AliOssStorageDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AliOssStorageDto storage : all) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("原始文件名", storage.getFileName());
            map.put("存储文件名", storage.getFileRealName());
            map.put("文件大小", storage.getFileSize());
            map.put("文件 MIME 类型", storage.getFileMimeType());
            map.put("文件类型", storage.getFileType());
            map.put("访问 URL", storage.getFileUrl());
            map.put("存储桶", storage.getBucketName());
            map.put("创建者", storage.getCreateBy());
            map.put("创建时间", storage.getCreateTime());
            map.put("更新者", storage.getUpdateBy());
            map.put("更新时间", storage.getUpdateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    /**
     * 检查存储桶是否存在
     */
    private boolean bucketExists(String bucketName) {
        try {
            return ossClient.doesBucketExist(bucketName);
        } catch (Exception e) {
            log.error("检查 OSS 存储桶时出错: {}", e.getMessage(), e);
            throw new BadRequestException("检查存储桶失败: " + e.getMessage());
        }
    }

    /**
     * 创建存储桶
     */
    private void createBucket(String bucketName) {
        try {
            ossClient.createBucket(bucketName);
        } catch (Exception e) {
            log.error("创建 OSS 存储桶时出错: {}", e.getMessage(), e);
            throw new BadRequestException("创建存储桶失败: " + e.getMessage());
        }
    }

    /**
     * 从完整 URL 中提取 OSS object key
     * 例如: https://domain.com/folder/filename -> folder/filename
     */
    private String extractObjectKeyFromUrl(String fileUrl, String domain) {
        if (fileUrl.startsWith(domain)) {
            return fileUrl.substring(domain.length() + 1); // +1 for the "/"
        }
        // 如果 domain 不包含协议，也要处理
        if (domain.startsWith("http://") || domain.startsWith("https://")) {
            String domainWithoutProtocol = domain.replaceFirst("https?://", "");
            if (fileUrl.contains(domainWithoutProtocol)) {
                int index = fileUrl.indexOf(domainWithoutProtocol) + domainWithoutProtocol.length();
                String keyPart = fileUrl.substring(index);
                if (keyPart.startsWith("/")) {
                    keyPart = keyPart.substring(1);
                }
                return keyPart;
            }
        }
        // 兜底：从最后一次出现的 "/" 之后开始，加上 folder 可能比较麻烦，但我们的 URL 是按规范生成的
        // 所以这个方法应该总能正确解析
        // 如果实在解析不了，返回 URL 中最后一个 "/" 之后的部分也不对
        // 因为我们的 URL 是 domain + "/" + filePath，所以直接 filePath = fileUrl.substring(domain.length() + 1)
        int protocolIndex = fileUrl.indexOf("://");
        if (protocolIndex != -1) {
            String afterProtocol = fileUrl.substring(protocolIndex + 3);
            int firstSlashIndex = afterProtocol.indexOf("/");
            if (firstSlashIndex != -1) {
                return afterProtocol.substring(firstSlashIndex + 1);
            }
        }
        // 最后兜底
        log.warn("无法从 URL {} 中提取 object key，尝试直接解析", fileUrl);
        return fileUrl;
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/impl/AliOssStorageServiceImpl.java
git commit -m "feat: add ali oss storage service implementation"
```

---

### Task 9: 创建 Controller

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/rest/AliOssStorageController.java`

- [ ] **Step 1: 编写 Controller**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.annotation.Log;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.service.dto.AliOssStorageQueryCriteria;
import com.naon.grid.utils.FileUtil;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 阿里云 OSS 存储管理
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aliOssStorage")
@Api(tags = "工具：阿里云 OSS 存储管理")
public class AliOssStorageController {

    private final AliOssStorageService aliOssStorageService;

    @ApiOperation("查询文件")
    @GetMapping
    @PreAuthorize("@el.check('editor')")
    public ResponseEntity<PageResult<AliOssStorageDto>> queryAliOssStorage(AliOssStorageQueryCriteria criteria, Pageable pageable) {
        return new ResponseEntity<>(aliOssStorageService.queryAll(criteria, pageable), HttpStatus.OK);
    }

    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('editor')")
    public void exportAliOssStorage(HttpServletResponse response, AliOssStorageQueryCriteria criteria) throws IOException {
        aliOssStorageService.download(aliOssStorageService.queryAll(criteria), response);
    }

    @Log("上传文件")
    @ApiOperation("上传文件")
    @PostMapping
    @PreAuthorize("@el.check('editor')")
    public ResponseEntity<AliOssStorage> uploadAliOssStorage(@RequestParam MultipartFile file) {
        AliOssStorage storage = aliOssStorageService.upload(file);
        return new ResponseEntity<>(storage, HttpStatus.OK);
    }

    @ApiOperation("上传图片")
    @PostMapping("/pictures")
    public ResponseEntity<AliOssStorage> uploadPicture(@RequestParam MultipartFile file) {
        String suffix = FileUtil.getExtensionName(file.getOriginalFilename());
        if (!FileUtil.IMAGE.equals(FileUtil.getFileType(suffix))) {
            throw new BadRequestException("只能上传图片");
        }
        AliOssStorage storage = aliOssStorageService.upload(file);
        return new ResponseEntity<>(storage, HttpStatus.OK);
    }

    @Log("修改文件信息")
    @ApiOperation("修改文件信息")
    @PutMapping
    @PreAuthorize("@el.check('editor')")
    public ResponseEntity<Object> updateAliOssStorage(@Validated @RequestBody AliOssStorage resources) {
        aliOssStorageService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除多个文件")
    @DeleteMapping
    @ApiOperation("删除多个文件")
    @PreAuthorize("@el.check('editor')")
    public ResponseEntity<Object> deleteAllAliOssStorage(@RequestBody List<Long> ids) {
        aliOssStorageService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/rest/AliOssStorageController.java
git commit -m "feat: add ali oss storage controller"
```

---

### Task 10: 整体编译验证

**Files:**
- None

- [ ] **Step 1: 执行完整项目编译**

Run: `cd /home/nano/nano-gemini && mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: 如果编译成功，完成最终确认**

---

## Self-Review Checklist

- [x] **Spec coverage:** 所有需求点都有对应任务：OSS 配置、上传、查询、删除、返回文件信息、UUID 文件名、公开访问、数据库表 oss_resource_meta。
- [x] **No placeholders:** 所有步骤都有完整代码，没有 TODO 或待填充内容。
- [x] **Type consistency:** 使用的类、方法名、字段名都一致，参考了现有 S3Storage 的实现模式。
