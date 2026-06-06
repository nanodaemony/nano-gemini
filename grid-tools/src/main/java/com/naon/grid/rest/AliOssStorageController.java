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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.domain.enums.OssBusinessType;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.rest.vo.AliOssStorageVO;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.service.dto.AliOssStorageQueryCriteria;
import com.naon.grid.utils.FileUtil;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 阿里云 OSS 存储管理
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aliOssStorage")
@Api(tags = "工具：阿里云 OSS 存储管理")
public class AliOssStorageController {

    private final AliOssStorageService aliOssStorageService;

    @ApiOperation("查询文件")
    @GetMapping
    @PreAuthorize("@el.check('editor')")
    public ResponseEntity<PageResult<AliOssStorageDto>> queryAliOssStorage(AliOssStorageQueryCriteria criteria, Pageable pageable) {
        return new ResponseEntity<>(aliOssStorageService.queryAll(criteria, pageable), HttpStatus.OK);
    }

    @ApiOperation("根据 ID 查询 OSS 资源信息")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AliOssStorageVO> getResource(@PathVariable Long id) {
        AliOssStorageDto dto = aliOssStorageService.findById(id);
        return new ResponseEntity<>(toVO(dto), HttpStatus.OK);
    }

    private AliOssStorageVO toVO(AliOssStorageDto dto) {
        if (dto == null) {
            return null;
        }
        AliOssStorageVO vo = new AliOssStorageVO();
        vo.setId(dto.getId());
        vo.setFileName(dto.getFileName());
        vo.setFileRealName(dto.getFileRealName());
        vo.setFileSize(dto.getFileSize());
        vo.setFileMimeType(dto.getFileMimeType());
        vo.setFileType(dto.getFileType());
        vo.setFileUrl(dto.getFileUrl());
        vo.setBucketName(dto.getBucketName());
        vo.setBusinessType(dto.getBusinessType());
        vo.setCustomPath(dto.getCustomPath());
        return vo;
    }

    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('editor')")
    public void exportAliOssStorage(HttpServletResponse response, AliOssStorageQueryCriteria criteria) throws IOException {
        aliOssStorageService.download(aliOssStorageService.queryAll(criteria), response);
    }

    @Log("上传文件")
    @ApiOperation("上传文件")
    @PostMapping
    @PreAuthorize("@el.check('editor')")
    public ResponseEntity<AliOssStorage> uploadAliOssStorage(
            @RequestParam MultipartFile file,
            @ApiParam(value = "业务类型", required = false) @RequestParam(required = false) OssBusinessType businessType,
            @ApiParam(value = "自定义路径", required = false) @RequestParam(required = false) String customPath) {
        AliOssStorage storage = aliOssStorageService.upload(file, businessType, customPath);
        return new ResponseEntity<>(storage, HttpStatus.OK);
    }

    @ApiOperation("上传图片")
    @AnonymousPostMapping("/pictures")
    public ResponseEntity<AliOssStorage> uploadPicture(
            @RequestParam MultipartFile file,
            @ApiParam(value = "业务类型", required = false) @RequestParam(required = false) OssBusinessType businessType,
            @ApiParam(value = "自定义路径", required = false) @RequestParam(required = false) String customPath) {
        String suffix = FileUtil.getExtensionName(file.getOriginalFilename());
        if (!FileUtil.IMAGE.equals(FileUtil.getFileType(suffix))) {
            throw new BadRequestException("只能上传图片");
        }
        AliOssStorage storage = aliOssStorageService.upload(file, businessType, customPath);
        return new ResponseEntity<>(storage, HttpStatus.OK);
    }

    @Log("修改文件信息")
    @ApiOperation("修改文件信息")
    @PutMapping
    @PreAuthorize("@el.check('editor')")
    public ResponseEntity<Object> updateAliOssStorage(@Validated @RequestBody AliOssStorage resources) {
        aliOssStorageService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除多个文件")
    @DeleteMapping
    @ApiOperation("删除多个文件")
    @PreAuthorize("@el.check('editor')")
    public ResponseEntity<Object> deleteAllAliOssStorage(@RequestBody List<Long> ids) {
        aliOssStorageService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
