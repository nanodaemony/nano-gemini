package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.modules.system.service.OrganizationService;
import com.naon.grid.modules.system.service.dto.ApplicationQueryDTO;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/institution")
@Api(tags = "用户：机构注册")
public class AppInstitutionController {

    private final OrganizationService organizationService;

    @ApiOperation("机构自助注册")
    @AnonymousPostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Validated @RequestBody InstitutionRegisterDTO dto,
                                              HttpServletRequest request) {
        organizationService.register(dto, request);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "申请已提交，请等待审核");
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
