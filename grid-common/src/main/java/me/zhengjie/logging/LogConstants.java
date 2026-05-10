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

import java.util.Set;

/**
 * 日志常量定义
 */
public final class LogConstants {

    /** TraceId HTTP Header 名称 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** MDC 中 TraceId 的 key */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /** TraceId 长度 */
    public static final int TRACE_ID_LENGTH = 8;

    /** 需要脱敏的字段名 */
    public static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "pwd", "token", "accessToken", "refreshToken",
        "secret", "apiKey", "creditCard", "Authorization"
    );

    /** 需要打印详细日志的路径前缀 */
    public static final Set<String> LOG_PATH_PREFIXES = Set.of(
        "/api/app/auth",
        "/api/maint"
    );

    /** 需要跳过日志的路径前缀 */
    public static final Set<String> SKIP_PATH_PREFIXES = Set.of(
        "/actuator",
        "/static",
        "/favicon.ico",
        "/swagger-resources",
        "/v2/api-docs",
        "/webjars"
    );

    /** 日志保留天数 */
    public static final int LOG_MAX_HISTORY = 7;

    private LogConstants() {}
}