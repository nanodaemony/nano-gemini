package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.modules.system.service.OrganizationService;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
import com.naon.grid.modules.system.service.dto.TokenDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/institution")
@Api(tags = "用户：机构注册")
public class AppInstitutionController {

    private final OrganizationService organizationService;

    @ApiOperation("机构自助注册")
    @AnonymousPostMapping("/register")
    public ResponseEntity<TokenDTO> register(@Validated @RequestBody InstitutionRegisterDTO dto,
                                              HttpServletRequest request) {
        TokenDTO tokenDTO = organizationService.register(dto, request);
        return ResponseEntity.ok(tokenDTO);
    }
}
