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

    @ApiOperation("推荐系统状态")
    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok("Referral system active");
    }
}
