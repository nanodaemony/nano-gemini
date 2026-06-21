package com.naon.grid.rest.wrapper;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.naon.grid.constants.LlmChatConstants;
import com.naon.grid.enums.ChatProviderEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.rest.vo.VocabChatBaseVO;
import com.naon.grid.service.dto.ChatRequest;
import com.naon.grid.service.dto.ChatResponse;
import com.naon.grid.service.dto.PinyinRequest;
import com.naon.grid.service.dto.PinyinResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 大模型对话包装器
 */
@Slf4j
public class LlmChatWrapper {

    public static ChatRequest toChatRequest(PinyinRequest request) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setProvider(request.getProvider());
        chatRequest.setModel(request.getModel());
        chatRequest.setSystemPrompt(LlmChatConstants.PINYIN_SYSTEM_PROMPT);
        chatRequest.setUserPrompt(request.getChineseText());
        chatRequest.setTemperature(LlmChatConstants.PINYIN_DEFAULT_TEMPERATURE);
        return chatRequest;
    }

    public static PinyinResponse toPinyinResponse(ChatResponse chatResponse) {
        return PinyinResponse.builder()
                .pinyin(chatResponse.getContent())
                .build();
    }

    public static ChatRequest toChatRequest(
            String provider, String model, String systemPrompt, String userPrompt, Double temperature) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setProvider(provider != null ? ChatProviderEnum.valueOf(provider) : ChatProviderEnum.ALIYUN);
        chatRequest.setModel(model != null ? model : "qwen-plus");
        chatRequest.setSystemPrompt(systemPrompt);
        chatRequest.setUserPrompt(userPrompt);
        chatRequest.setTemperature(temperature);
        return chatRequest;
    }

    /**
     * 解析 LLM 返回的 JSON 字符串为对话列表
     */
    public static List<VocabChatBaseVO> parseDialogueResponse(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new BadRequestException("对话生成失败，模型返回为空");
        }

        try {
            String trimmed = jsonText.trim();
            // 处理 LLM 可能用 markdown 代码块包裹的情况
            if (trimmed.startsWith("```")) {
                trimmed = trimmed.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            JSONArray jsonArray = JSON.parseArray(trimmed);
            List<VocabChatBaseVO> result = new ArrayList<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                com.alibaba.fastjson2.JSONObject obj = jsonArray.getJSONObject(i);
                String role = obj.getString("role");
                String content = obj.getString("content");
                if (role != null && !role.isEmpty() && content != null && !content.isEmpty()) {
                    VocabChatBaseVO vo = new VocabChatBaseVO();
                    vo.setRole(role);
                    vo.setContent(content);
                    result.add(vo);
                } else {
                    log.warn("跳过无效对话条目（索引 {}）：role 或 content 为空", i);
                }
            }

            if (result.isEmpty()) {
                throw new BadRequestException("对话生成失败，生成的对话内容为空");
            }

            return result;
        } catch (Exception e) {
            log.error("解析对话生成响应失败: {}. 原始响应: {}", e.getMessage(), jsonText);
            throw new BadRequestException("对话生成失败，请重试");
        }
    }
}
