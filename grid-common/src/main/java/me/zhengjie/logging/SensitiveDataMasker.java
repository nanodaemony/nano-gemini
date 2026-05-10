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
package me.zhengjie.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

/**
 * 敏感数据脱敏工具
 */
@Slf4j
public final class SensitiveDataMasker {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MASK_VALUE = "******";

    /**
     * 对 JSON 字符串中的敏感字段进行脱敏
     * @param jsonStr JSON 字符串
     * @return 脱敏后的 JSON 字符串
     */
    public static String mask(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return jsonStr;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonStr);
            if (root.isObject()) {
                maskObjectNode((ObjectNode) root);
                return objectMapper.writeValueAsString(root);
            }
            return jsonStr;
        } catch (Exception e) {
            log.warn("Failed to mask sensitive data: {}", e.getMessage());
            return jsonStr;
        }
    }

    /**
     * 递归脱敏 ObjectNode
     */
    private static void maskObjectNode(ObjectNode node) {
        node.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();

            if (LogConstants.SENSITIVE_FIELDS.contains(fieldName)) {
                node.put(fieldName, MASK_VALUE);
            } else if (fieldValue.isObject()) {
                maskObjectNode((ObjectNode) fieldValue);
            }
        });
    }

    /**
     * 手机号脱敏: 13812345678 -> 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private SensitiveDataMasker() {}
}