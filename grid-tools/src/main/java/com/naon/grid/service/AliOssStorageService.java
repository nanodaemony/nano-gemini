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

import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.domain.enums.OssBusinessType;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.service.dto.AliOssStorageQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 阿里云 OSS 存储 Service 接口
 * @author Zheng Jie
 * @date 2025-05-19
 */
public interface AliOssStorageService {

    /**
     * 分页查询
     * @param criteria 查询条件
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<AliOssStorageDto> queryAll(AliOssStorageQueryCriteria criteria, Pageable pageable);

    /**
     * 查询所有数据不分页
     * @param criteria 查询条件
     * @return 数据列表
     */
    List<AliOssStorageDto> queryAll(AliOssStorageQueryCriteria criteria);

    /**
     * 根据 ID 查询
     * @param id 文件 ID
     * @return 文件信息 DTO
     */
    AliOssStorageDto findById(Long id);

    /**
     * 根据 ID 列表批量查询
     * @param ids 文件 ID 列表
     * @return 文件信息 DTO 列表（不存在的 ID 不在结果中）
     */
    List<AliOssStorageDto> findByIds(List<Long> ids);

    /**
     * 上传文件到 OSS
     * @param file 上传的文件
     * @return 保存后的存储对象（包含完整 URL）
     */
    AliOssStorage upload(MultipartFile file);

    /**
     * 上传文件到 OSS（指定业务类型）
     * @param file 上传的文件
     * @param businessType 业务类型
     * @return 保存后的存储对象（包含完整 URL）
     */
    AliOssStorage upload(MultipartFile file, OssBusinessType businessType);

    /**
     * 上传文件到 OSS（指定业务类型和自定义路径）
     * @param file 上传的文件
     * @param businessType 业务类型
     * @param customPath 自定义路径
     * @return 保存后的存储对象（包含完整 URL）
     */
    AliOssStorage upload(MultipartFile file, OssBusinessType businessType, String customPath);

    /**
     * 修改文件信息
     * @param resources 文件信息
     */
    void update(AliOssStorage resources);

    /**
     * 多选删除
     * @param ids ID 列表
     */
    void deleteAll(List<Long> ids);

    /**
     * 导出数据
     * @param all 待导出的数据
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    void download(List<AliOssStorageDto> all, HttpServletResponse response) throws IOException;
}
