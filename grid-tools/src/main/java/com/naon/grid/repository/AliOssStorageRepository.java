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
package com.naon.grid.repository;

import com.naon.grid.domain.AliOssStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * 阿里云 OSS 存储 Repository
 * @author Zheng Jie
 * @date 2025-05-19
 */
public interface AliOssStorageRepository extends JpaRepository<AliOssStorage, Long>, JpaSpecificationExecutor<AliOssStorage> {

    /**
     * 根据 ID 查询 OSS 文件路径（这里是 fileUrl）
     * @param id 文件 ID
     * @return 文件 URL
     */
    @Query(value = "SELECT file_url FROM oss_resource_meta WHERE id = ?1", nativeQuery = true)
    String selectFileUrlById(Long id);

    /**
     * 根据 ID 查询真实文件名
     * @param id 文件 ID
     * @return 真实文件名
     */
    @Query(value = "SELECT file_real_name FROM oss_resource_meta WHERE id = ?1", nativeQuery = true)
    String selectFileRealNameById(Long id);
}
