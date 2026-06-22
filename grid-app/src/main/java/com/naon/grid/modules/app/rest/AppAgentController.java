package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.modules.app.service.AgentService;
import com.naon.grid.modules.app.service.dto.AgentRegisterDTO;
import com.naon.grid.modules.app.service.dto.TokenDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/agent")
@Api(tags = "用户：代理商注册")
public class AppAgentController {

    private final AgentService agentService;

    @ApiOperation("代理商自助注册")
    @AnonymousPostMapping("/register")
    public ResponseEntity<TokenDTO> register(@Validated @RequestBody AgentRegisterDTO dto,
                                              HttpServletRequest request) {
        TokenDTO tokenDTO = agentService.register(dto, request);
        return ResponseEntity.ok(tokenDTO);
    }
}
