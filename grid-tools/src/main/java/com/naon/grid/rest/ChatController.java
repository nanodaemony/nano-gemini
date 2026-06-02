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

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.service.ChatService;
import com.naon.grid.service.dto.ChatRequest;
import com.naon.grid.service.dto.ChatResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 大模型对话控制器
 * @author nano
 * @date 2026-06-02
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Api(tags = "工具：大模型对话")
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @Log("大模型对话")
    @ApiOperation("大模型对话")
    @AnonymousPostMapping("/completions")
    public ResponseEntity<ChatResponse> chat(@Validated @RequestBody ChatRequest request) {
        return new ResponseEntity<>(chatService.chat(request), HttpStatus.OK);
    }
}
