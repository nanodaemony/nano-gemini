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
package com.naon.grid.modules.system.rest;

import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.repository.GridOrganizationRepository;
import com.naon.grid.modules.system.service.OrganizationService;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.backend.service.dto.OrganizationQueryCriteria;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/institutions")
@Api(tags = "系统：机构审核")
public class InstitutionAuditController {

    private final OrganizationService organizationService;
    private final GridOrganizationRepository organizationRepository;

    @ApiOperation("分页查询机构列表")
    @GetMapping
    @PreAuthorize("@el.check('institution:list')")
    public ResponseEntity<PageResult<GridOrganization>> queryAll(
            OrganizationQueryCriteria criteria, Pageable pageable) {
        Page<GridOrganization> page = organizationRepository.findAll(
                (root, query, cb) -> QueryHelp.getPredicate(root, criteria, cb),
                pageable);
        return ResponseEntity.ok(PageUtil.toPage(page));
    }

    @ApiOperation("获取机构详情")
    @GetMapping("/{id}")
    @PreAuthorize("@el.check('institution:list')")
    public ResponseEntity<GridOrganization> getDetail(@PathVariable Integer id) {
        GridOrganization org = organizationService.findById(id);
        org.setAdminPassword(null); // 不返回密码
        return ResponseEntity.ok(org);
    }

    @ApiOperation("审核通过")
    @PostMapping("/{id}/approve")
    @PreAuthorize("@el.check('institution:approve')")
    public ResponseEntity<Void> approve(@PathVariable Integer id,
                                         @RequestParam String plan) {
        organizationService.approve(id, plan);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("审核驳回")
    @PostMapping("/{id}/reject")
    @PreAuthorize("@el.check('institution:reject')")
    public ResponseEntity<Void> reject(@PathVariable Integer id,
                                        @RequestParam String reason) {
        organizationService.reject(id, reason);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("变更机构角色（普通机构 ↔ 代理机构）")
    @PutMapping("/{id}/role")
    @PreAuthorize("@el.check('institution:edit')")
    public ResponseEntity<Void> updateRole(@PathVariable Integer id,
                                           @RequestParam String orgRole) {
        organizationService.updateRole(id, orgRole);
        return ResponseEntity.ok().build();
    }
}
