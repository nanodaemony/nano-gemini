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

import com.naon.grid.modules.system.domain.GridAgent;
import com.naon.grid.modules.system.repository.GridAgentRepository;
import com.naon.grid.modules.system.service.AgentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agents")
@Api(tags = "系统：代理商审核")
public class AgentAuditController {

    private final AgentService agentService;
    private final GridAgentRepository agentRepository;

    @ApiOperation("获取待审核代理商列表")
    @GetMapping("/pending")
    public ResponseEntity<List<GridAgent>> getPendingAgents() {
        return ResponseEntity.ok(agentRepository.findByAuditStatus("PENDING"));
    }

    @ApiOperation("审核通过")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Integer id) {
        agentService.approve(id);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("审核驳回")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Integer id,
                                        @RequestParam String reason) {
        agentService.reject(id, reason);
        return ResponseEntity.ok().build();
    }
}
