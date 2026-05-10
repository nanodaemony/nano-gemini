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

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 过滤器
 * 从 HTTP Header 提取 TraceId 并放入 MDC，用于日志追踪
 */
@Slf4j
@Component
@Order(1)
public class TraceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String traceId = httpRequest.getHeader(LogConstants.TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
            log.debug("Generated new traceId: {}", traceId);
        }

        MDC.put(LogConstants.TRACE_ID_MDC_KEY, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(LogConstants.TRACE_ID_MDC_KEY);
        }
    }

    /**
     * 生成 8 位 TraceId
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, LogConstants.TRACE_ID_LENGTH);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("TraceFilter initialized");
    }

    @Override
    public void destroy() {
        log.info("TraceFilter destroyed");
    }
}