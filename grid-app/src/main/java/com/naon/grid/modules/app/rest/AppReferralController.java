package com.naon.grid.modules.app.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/referral")
@Api(tags = "用户：推荐接口")
public class AppReferralController {

    // Phase 1: simple health check, full referral API in Phase 2
    @ApiOperation("推荐信息")
    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok("Referral system active");
    }
}
