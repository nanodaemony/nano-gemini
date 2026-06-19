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
package com.naon.grid.modules.system.service.impl;

import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.system.service.VerifyService;
import com.naon.grid.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Zheng Jie
 * @date 2018-12-26
 */
@Service
@RequiredArgsConstructor
public class VerifyServiceImpl implements VerifyService {

    @Value("${code.expiration}")
    private Long expiration;
    private final RedisUtils redisUtils;

    @Override
    public void validated(String key, String code) {
        String value = redisUtils.get(key, String.class);
        if(!code.equals(value)){
            throw new BadRequestException("无效验证码");
        } else {
            redisUtils.del(key);
        }
    }
}
