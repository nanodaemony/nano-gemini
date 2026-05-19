# 阿里云 OSS 存储功能设计文档

## 概述
在 grid-tools 模块中添加阿里云 OSS 存储功能，实现文件上传、查询、删除等操作，并将文件元信息存储在 MySQL 数据库中。

## 背景与目标
- 参考项目现有的 LocalStorage 和 S3Storage 实现模式
- 提供公共读取的文件访问方式
- 使用 UUID 避免文件重名
- 支持从 OSS 同步删除文件

## 数据库设计

### 表结构
表名：`oss_resource_meta`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 ID |
| file_name | VARCHAR(255) | NOT NULL | 原始文件名 |
| file_real_name | VARCHAR(255) | NOT NULL | OSS 上存储的文件名（UUID） |
| file_size | VARCHAR(255) | NOT NULL | 文件大小（格式化后的字符串） |
| file_mime_type | VARCHAR(255) | NOT NULL | MIME 类型 |
| file_type | VARCHAR(255) | NOT NULL | 文件类型分类（图片/文档/视频等） |
| file_url | VARCHAR(500) | NOT NULL | OSS 完整访问 URL |
| bucket_name | VARCHAR(255) | NOT NULL | 存储桶名称 |
| create_by | VARCHAR(255) | | 创建者（继承自 BaseEntity） |
| create_time | TIMESTAMP | | 创建时间（继承自 BaseEntity） |
| update_by | VARCHAR(255) | | 更新者（继承自 BaseEntity） |
| update_time | TIMESTAMP | | 更新时间（继承自 BaseEntity） |

## 代码结构

### 新增文件列表

1. **配置类**
   - `grid-tools/src/main/java/com/naon/grid/config/AliOssConfig.java`

2. **实体类**
   - `grid-tools/src/main/java/com/naon/grid/domain/AliOssStorage.java`

3. **Repository**
   - `grid-tools/src/main/java/com/naon/grid/repository/AliOssStorageRepository.java`

4. **Service 层**
   - `grid-tools/src/main/java/com/naon/grid/service/AliOssStorageService.java`
   - `grid-tools/src/main/java/com/naon/grid/service/impl/AliOssStorageServiceImpl.java`

5. **DTO & Mapper**
   - `grid-tools/src/main/java/com/naon/grid/service/dto/AliOssStorageDto.java`
   - `grid-tools/src/main/java/com/naon/grid/service/dto/AliOssStorageQueryCriteria.java`
   - `grid-tools/src/main/java/com/naon/grid/service/mapstruct/AliOssStorageMapper.java`

6. **Controller**
   - `grid-tools/src/main/java/com/naon/grid/rest/AliOssStorageController.java`

### 修改文件列表
- `grid-tools/pom.xml` - 添加阿里云 OSS SDK 依赖

## API 设计

| 方法 | 路径 | 说明 | 权限 | 返回值 |
|------|------|------|------|--------|
| GET | /api/aliOssStorage | 分页查询文件 | editor | PageResult&lt;AliOssStorageDto&gt; |
| GET | /api/aliOssStorage/download | 导出数据 | editor | Excel 文件 |
| POST | /api/aliOssStorage | 上传文件 | editor | AliOssStorageDto (包含文件名、URL 等完整信息) |
| POST | /api/aliOssStorage/pictures | 上传图片 | 无需认证 | AliOssStorage (包含文件名、URL 等完整信息) |
| PUT | /api/aliOssStorage | 修改文件信息 | editor | 无内容 |
| DELETE | /api/aliOssStorage | 删除文件（同时删除 OSS 上的文件） | editor | 无内容 |

## 配置项设计

在 `application.yml` 中配置：

```yaml
ali:
  oss:
    endpoint: ${ALI_OSS_ENDPOINT:}
    access-key-id: ${ALI_OSS_ACCESS_KEY_ID:}
    access-key-secret: ${ALI_OSS_ACCESS_KEY_SECRET:}
    bucket-name: ${ALI_OSS_BUCKET_NAME:}
    domain: ${ALI_OSS_DOMAIN:}
    timeformat: ${ALI_OSS_TIMEFORMAT:yyyy-MM}
```

## 实现细节

### 文件命名策略
- 使用 UUID 生成唯一文件名
- 保留原始文件后缀
- 按时间格式（yyyy-MM）组织文件夹

### 文件访问权限
- OSS Bucket 设置为公共读取
- 返回完整的 HTTP URL 供前端直接访问

### 删除策略
- 删除数据库记录前，先调用 OSS API 删除云端文件
- 使用事务保证一致性

### 上传返回值
- 上传成功后，返回完整的文件信息对象（AliOssStorage 或 AliOssStorageDto）
- 包含：id、原始文件名、存储文件名、文件大小、URL、类型等所有字段
- 前端可直接使用返回的 URL 和文件信息

## 依赖项

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>
```
