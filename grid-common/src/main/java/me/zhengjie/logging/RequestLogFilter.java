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
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

/**
 * 请求/响应日志过滤器
 * 打印完整的请求和响应信息，便于调试
 */
@Slf4j
@Component
@Order(2)
public class RequestLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 跳过不需要记录的路径
        if (shouldSkip(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        // 包装请求和响应，以便多次读取内容
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();
        String traceId = MDC.get(LogConstants.TRACE_ID_MDC_KEY);

        try {
            // 先执行请求，让内容被缓存
            chain.doFilter(wrappedRequest, wrappedResponse);

            long duration = System.currentTimeMillis() - startTime;

            // 打印请求日志
            logRequest(wrappedRequest, traceId);

            // 打印响应日志
            logResponse(wrappedResponse, traceId, duration);
        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage());
            throw e;
        } finally {
            // 将缓存的内容写回原始响应 - 必须在 finally 中确保执行
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * 打印请求日志
     */
    private void logRequest(ContentCachingRequestWrapper request, String traceId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String headers = extractHeaders(request);
        String body = getRequestBody(request);

        log.info("=== 请求开始 [traceId: {}] ===\n{} {}\nHeaders: {}\nBody: {}",
            traceId, method, uri, headers, body);
    }

    /**
     * 打印响应日志
     */
    private void logResponse(ContentCachingResponseWrapper response, String traceId, long duration) {
        int status = response.getStatus();
        String body = getResponseBody(response);

        log.info("=== 响应 [traceId: {}] ===\nStatus: {}\nBody: {}\n耗时: {}ms\n=== 请求结束 ===",
            traceId, status, body, duration);
    }

    /**
     * 提取请求头
     */
    private String extractHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        return Collections.list(headerNames).stream()
            .map(name -> name + "=" + request.getHeader(name))
            .filter(header -> !header.toLowerCase().contains("authorization"))
            .collect(Collectors.joining(", "));
    }

    /**
     * 获取请求体（脱敏）
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        String body = new String(content, StandardCharsets.UTF_8);
        return SensitiveDataMasker.mask(body);
    }

    /**
     * 获取响应体（脱敏）
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        String body = new String(content, StandardCharsets.UTF_8);
        return SensitiveDataMasker.mask(body);
    }

    /**
     * 判断是否跳过日志记录
     */
    private boolean shouldSkip(String uri) {
        return LogConstants.SKIP_PATH_PREFIXES.stream()
            .anyMatch(uri::startsWith);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("RequestLogFilter initialized");
    }

    @Override
    public void destroy() {
        log.info("RequestLogFilter destroyed");
    }
}