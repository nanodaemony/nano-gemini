# 火山引擎 TTS 语音合成集成设计文档

> 日期：2026-05-20
> 模块：grid-tools

## 一、概述

在现有阿里云 TTS 基础上，新增火山引擎（豆包）大模型语音合成支持，方便对比效果。两个 TTS 服务使用独立接口，但共享相同的数据库表（通过 vendor 字段区分）。

## 二、模块结构

### 新增/修改文件

```
grid-tools/src/main/java/com/naon/grid/
├── config/
│   └── VolcengineTtsConfig.java          # 火山引擎配置类（新增）
├── domain/
│   └── TtsRecord.java                    # TTS 记录实体类（添加 vendor 字段）
├── repository/
│   └── TtsRecordRepository.java          # 数据访问层（不变）
├── service/
│   ├── TtsService.java                   # 阿里云 TTS 接口（不变）
│   ├── VolcengineTtsService.java         # 火山引擎 TTS 接口（新增）
│   ├── dto/
│   │   ├── TtsRequest.java               # 阿里云请求 DTO（不变）
│   │   ├── TtsResponse.java              # 响应 DTO（不变）
│   │   ├── VolcengineTtsRequest.java     # 火山引擎请求 DTO（新增）
│   │   └── VolcengineTtsResponse.java    # 火山引擎响应 DTO（新增）
│   └── impl/
│       ├── TtsServiceImpl.java           # 阿里云实现（不变）
│       └── VolcengineTtsServiceImpl.java # 火山引擎实现（新增）
└── rest/
    ├── TtsController.java                # 阿里云接口（不变）
    └── VolcengineTtsController.java      # 火山引擎接口（新增）
```

## 三、配置管理

### 3.1 .env 配置

在 `.env` 和 `.env.example` 中添加：

```bash
# 火山引擎 TTS 配置
VOLCENGINE_API_KEY=your-api-key
```

### 3.2 application.yml 配置

在 `grid-system/src/main/resources/config/application.yml` 中添加：

```yaml
volcengine:
  tts:
    api-key: ${VOLCENGINE_API_KEY:}
    api-resource-id: seed-tts-2.0
    base-url: https://openspeech.bytedance.com/api/v3/tts/unidirectional
```

## 四、数据库设计

### 4.1 表结构变更

在 `tts_record` 表中新增字段：

| 字段 | 类型 | 说明 | 约束 |
|---|---|---|---|
| vendor | VARCHAR(50) | TTS 厂商 | NOT NULL, DEFAULT 'aliyun' |

vendor 可选值：
- `aliyun` - 阿里云百炼
- `volcengine` - 火山引擎

## 五、API 设计

### 5.1 阿里云 TTS（保持不变）

**URL**: `POST /api/tts/generate`

### 5.2 火山引擎 TTS（新增）

**URL**: `POST /api/tts/volcengine/generate`

**请求参数**:
```json
{
  "text": "需要合成的文本",
  "speaker": "zh_female_shuangkuaisisi_moon_bigtts",
  "apiResourceId": "seed-tts-2.0",
  "model": "seed-tts-2.0-standard",
  "audioParams": {
    "format": "mp3",
    "sampleRate": 24000,
    "speechRate": 0,
    "loudnessRate": 0
  },
  "contextTexts": ["用标准的语气"]
}
```

**响应参数**:
```json
{
  "audioUrl": "https://your-oss-domain.com/tts/2026-05/xxx.mp3"
}
```

## 六、核心流程（火山引擎）

1. **接收请求参数**：验证必选参数（text, speaker, apiResourceId）
2. **构建火山引擎请求**：
   - 请求 Header: `X-Api-Key`, `X-Api-Resource-Id`, `X-Api-Request-Id`
   - 请求 Body: 按照火山引擎格式构建 user, req_params
3. **调用火山引擎 HTTP Chunked 接口**：
   - 使用 Java HttpClient 发送 POST 请求
   - 流式读取响应
4. **解析响应并拼接音频**：
   - 读取每个 chunk 的 JSON
   - 提取 base64 编码的 data 字段
   - 拼接所有音频数据
5. **上传到自己的 OSS**：
   - 调用 `AliOssStorageService` 上传音频
   - 指定业务类型为 `TTS`
6. **保存记录**：
   - vendor = `volcengine`
7. **返回结果**：
   - 返回 OSS 上的最终音频 URL

## 七、依赖管理

无需新增依赖，使用现有的 HttpClient 和 fastjson2 即可。

## 八、注意事项

1. **音频格式**：默认使用 mp3，也支持 wav/ogg_opus/pcm
2. **流式响应**：火山引擎返回多个 JSON chunk，需要全部拼接
3. **Base64 解码**：每个 chunk 的 data 字段是 base64 编码的音频数据
4. **音色列表**：参考火山引擎文档选择合适的 speaker
