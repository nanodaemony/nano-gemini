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

import com.naon.grid.service.dto.VolcengineTtsRequest;
import com.naon.grid.service.dto.VolcengineTtsResponse;

/**
 * 火山引擎 TTS 语音合成 Service 接口
 * @author nano
 * @date 2026-05-20
 */
public interface VolcengineTtsService {

    /**
     * 语音合成
     * @param request 请求参数
     * @return 响应参数（包含最终 OSS 音频 URL）
     */
    VolcengineTtsResponse generate(VolcengineTtsRequest request);
}
