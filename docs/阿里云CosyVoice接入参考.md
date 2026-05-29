# 阿里云 CosyVoice 接入参考

## 接口信息

| 项目 | 说明 |
|------|------|
| 接口路径 | `POST /api/tts/cosyvoice` |
| 鉴权 | 匿名访问（无需 Token） |
| 请求格式 | `application/json` |
| 响应格式 | `application/json` |

## 入参说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `voice` | String | 是 | - | 音色名称，见下方音色列表 |
| `text` | String | 是 | - | 待合成文本，支持 SSML（需开启 `enableSsml`） |
| `model` | String | 否 | `cosyvoice-v3-flash` | 模型：`cosyvoice-v3.5-plus`、`cosyvoice-v3.5-flash`、`cosyvoice-v3-plus`、`cosyvoice-v3-flash`、`cosyvoice-v2` |
| `format` | String | 否 | `mp3` | 音频格式：`mp3`、`pcm`、`wav`、`opus` |
| `sampleRate` | Integer | 否 | 22050 | 采样率（Hz）：8000、16000、22050、24000、44100、48000 |
| `volume` | Integer | 否 | 50 | 音量，范围 [0, 100] |
| `rate` | Float | 否 | 1.0 | 语速，范围 [0.5, 2.0]，越小越慢 |
| `pitch` | Float | 否 | 1.0 | 音调，范围 [0.5, 2.0]，越小越低 |
| `enableSsml` | Boolean | 否 | false | 是否开启 SSML，开启后 `text` 需传入 SSML 格式 |
| `wordTimestampEnabled` | Boolean | 否 | false | 是否开启字级别时间戳（仅部分音色支持） |
| `seed` | Integer | 否 | 0 | 随机数种子，范围 [0, 65535]，固定 seed 可复现相同结果 |
| `languageHints` | List\<String\> | 否 | - | 目标语言：`zh`、`en`、`fr`、`de`、`ja`、`ko`、`ru`、`pt`、`th`、`id`、`vi` |
| `instruction` | String | 否 | - | 控制指令（仅支持 Instruct 的音色可用） |
| `bitRate` | Integer | 否 | 32 | 音频码率（kbps），范围 [6, 510]，仅 `format=opus` 时生效 |
| `enableAigcTag` | Boolean | 否 | false | 是否添加 AIGC 隐性标识 |
| `aigcPropagator` | String | 否 | - | AIGC 标识 ContentPropagator 字段 |
| `aigcPropagateId` | String | 否 | - | AIGC 标识 PropagateID 字段 |
| `hotFix` | Map | 否 | - | 文本热修复配置，用于多音字注音或文本替换，详见下方多音字章节 |
| `enableMarkdownFilter` | Boolean | 否 | false | 是否启用 Markdown 过滤（仅 cosyvoice-v3-flash 复刻音色支持） |

### 推荐音色（cosyvoice-v3-flash，中文教学）

| voice | 特质 | 年龄 | 性别 | 支持 Instruct |
|-------|------|------|------|:---:|
| `longshuo_v3` | 博才干练男 | 25~30 | 男 | 否 |
| `longxiaoxia_v3` | 沉稳权威女 | 25~30 | 女 | 否 |
| `longsanshu_v3` | 沉稳质感男 | 25~45 | 男 | 否 |
| `longtian_v3` | 磁性理智男 | 30~35 | 男 | 否 |
| `longanwen_v3` | 优雅知性女 | 25~35 | 女 | 否 |
| `longanli_v3` | 利落从容女 | 25~35 | 女 | 否 |

## 汉字、词汇、例句示例配置

以下示例使用男声 `longshuo_v3`，固定 `seed=42` 保证可复现。

### 汉字（单字朗读）

语速较慢（0.7），让学生听清声母、韵母和声调。

```json
{
    "voice": "longshuo_v3",
    "text": "花",
    "model": "cosyvoice-v3-flash",
    "format": "mp3",
    "sampleRate": 22050,
    "volume": 50,
    "rate": 0.7,
    "pitch": 1.0,
    "seed": 42,
    "languageHints": ["zh"]
}
```

### 词汇（词语朗读）

语速适中（0.85），词内连贯但不过快。

```json
{
    "voice": "longshuo_v3",
    "text": "花园",
    "model": "cosyvoice-v3-flash",
    "format": "mp3",
    "sampleRate": 22050,
    "volume": 50,
    "rate": 0.85,
    "pitch": 1.0,
    "seed": 42,
    "languageHints": ["zh"]
}
```

### 例句（句子朗读）

正常语速（1.0），断句自然，适合学生跟读。

```json
{
    "voice": "longshuo_v3",
    "text": "我家的后面有一个很大的花园。",
    "model": "cosyvoice-v3-flash",
    "format": "mp3",
    "sampleRate": 22050,
    "volume": 50,
    "rate": 1.0,
    "pitch": 1.0,
    "seed": 42,
    "languageHints": ["zh"]
}
```

### 参数差异总结

| 参数 | 汉字 | 词汇 | 例句 |
|------|------|------|------|
| `rate` | 0.7 | 0.85 | 1.0 |
| `pitch` | 1.0 | 1.0 | 1.0 |
| `seed` | 42 | 42 | 42 |
| 其他 | 相同 | 相同 | 相同 |

## 多音字处理（hotFix）

`hotFix` 参数支持两种方式修正发音：

### 方式一：拼音标注（pronunciation）

指定词语的拼音标注，用于纠正多音字默认发音。

**拼音格式**：`<汉字>: "<拼音><数字声调>"`，声调 1-4 分别对应 ā、á、ǎ、à，轻声用 5 或不标。

```json
{
    "voice": "longshuo_v3",
    "text": "这个银行很长。",
    "model": "cosyvoice-v3-flash",
    "format": "mp3",
    "rate": 0.85,
    "pitch": 1.0,
    "seed": 42,
    "languageHints": ["zh"],
    "hotFix": {
        "pronunciation": [
            {"银行": "yin2 hang2"},
            {"很长": "hen3 chang2"}
        ]
    }
}
```

> 注意：`hotFix` 中的 key 必须与 `text` 中的文字完全匹配（包含上下文），才能精确替换。建议把整个词组作为 key。

### 方式二：文本替换（replace）

在合成前将指定词语替换为目标文本。

```json
{
    "voice": "longshuo_v3",
    "text": "今天天气真好。",
    "model": "cosyvoice-v3-flash",
    "format": "mp3",
    "rate": 0.85,
    "seed": 42,
    "languageHints": ["zh"],
    "hotFix": {
        "replace": [
            {"今天": "金天"}
        ]
    }
}
```

### 同时使用两种方式

```json
{
    "hotFix": {
        "pronunciation": [
            {"银行": "yin2 hang2"}
        ],
        "replace": [
            {"今天": "金天"}
        ]
    }
}
```

### 常见多音字拼音参考

| 汉字 | 常见读音 | 拼音 |
|------|----------|------|
| 行 | 银行 | yin2 hang2 |
| 行 | 行走 | xing2 zou3 |
| 长 | 很长 | hen3 chang2 |
| 长 | 长大 | zhang3 da4 |
| 乐 | 音乐 | yin1 yue4 |
| 乐 | 快乐 | kuai4 le4 |
| 重 | 重要 | zhong4 yao4 |
| 重 | 重复 | chong2 fu4 |
| 都 | 首都 | shou3 du1 |
| 都 | 都是 | dou1 shi4 |
| 了 | 好了 | hao3 le5 |
| 了 | 了解 | liao3 jie3 |
| 得 | 跑得快 | pao3 de5 kuai4 |
| 得 | 得到 | de2 dao4 |

> 拼音中的数字表示声调：1=阴平（ā），2=阳平（á），3=上声（ǎ），4=去声（à），5=轻声。

## 响应格式

```json
{
    "audioUrl": "https://your-oss-domain.com/tts/2025-05/xxx.mp3"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `audioUrl` | String | 自有 OSS 上的永久音频 URL |

## 注意事项

1. **音色后缀**：cosyvoice-v3-flash 的音色名称需要带 `_v3` 后缀，如 `longshuo_v3`，不能写成 `longshuo`。
2. **固定 seed**：建议所有音频使用相同的 `seed` 值（如 42），确保同一文本反复合成结果一致，避免重录时发音差异。
3. **语速分级**：汉字建议 0.7，词汇建议 0.85，例句建议 1.0。可根据学生水平调整。
4. **hotFix 范围**：`pronunciation` 的 key 应与 `text` 中的子串精确匹配，建议把多音字所在的整个词作为 key，而非单个字。
5. **不支持 Instruct 的音色**：`longshuo_v3`、`longxiaoxia_v3` 等大多数音色不支持 `instruction` 参数，传入也不会生效。如需通过自然语言指令控制风格，请使用 `longanyang` 或 `longanhuan`。
6. **音频缓存**：相同 `text` + `voice` + `seed` 组合合成结果一致，可自行在业务层做缓存避免重复调用 API。
