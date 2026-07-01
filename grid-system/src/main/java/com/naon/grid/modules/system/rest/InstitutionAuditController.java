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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/institutions")
@Api(tags = "系统：机构审核")
public class InstitutionAuditController {

    private final OrganizationService organizationService;
    private final GridOrganizationRepository organizationRepository;

    @ApiOperation("获取待审核机构列表")
    @GetMapping("/pending")
    public ResponseEntity<List<GridOrganization>> getPendingInstitutions() {
        return ResponseEntity.ok(organizationRepository.findByAuditStatus("PENDING"));
    }

    @ApiOperation("审核通过")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Integer id,
                                         @RequestParam String plan) {
        organizationService.approve(id, plan);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("审核驳回")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Integer id,
                                        @RequestParam String reason) {
        organizationService.reject(id, reason);
        return ResponseEntity.ok().build();
    }
}
