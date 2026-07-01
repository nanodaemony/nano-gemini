package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.modules.system.service.OrganizationService;
import com.naon.grid.modules.system.service.dto.ApplicationQueryDTO;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/institution")
@Api(tags = "用户：机构注册")
public class AppInstitutionController {

    private final OrganizationService organizationService;

    @ApiOperation("机构申请入驻")
    @AnonymousPostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Validated @RequestBody InstitutionRegisterDTO dto,
            HttpServletRequest request) {
        organizationService.register(dto, request);
        return ResponseEntity.ok(Collections.singletonMap("message", "提交成功，请等待审核"));
    }

    @ApiOperation("查询申请状态")
    @AnonymousPostMapping("/application/query")
    public ResponseEntity<Map<String, Object>> queryApplication(
            @Validated @RequestBody ApplicationQueryDTO dto) {
        return ResponseEntity.ok(organizationService.queryApplication(dto));
    }

    @ApiOperation("驳回后重新提交申请")
    @AnonymousPutMapping("/application")
    public ResponseEntity<Map<String, String>> resubmit(
            @Validated @RequestBody InstitutionRegisterDTO dto,
            HttpServletRequest request) {
        organizationService.resubmit(dto, request);
        return ResponseEntity.ok(Collections.singletonMap("message", "重新提交成功，请等待审核"));
    }
}
