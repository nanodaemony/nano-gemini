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

import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.RegionPricing;
import com.naon.grid.modules.billing.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Api(tags = "系统：产品管理")
public class ProductController {

    private final ProductService productService;

    @ApiOperation("获取所有产品列表")
    @GetMapping
    public ResponseEntity<List<GridProduct>> getAllProducts() {
        return ResponseEntity.ok(productService.findAllActive());
    }

    @ApiOperation("获取产品的区域定价")
    @GetMapping("/{code}/pricing")
    public ResponseEntity<?> getProductPricing(@PathVariable String code,
                                                @RequestParam(defaultValue = "C") String region) {
        return productService.findByCode(code)
                .map(product -> ResponseEntity.ok(
                        productService.getPricingByProductAndRegion(product.getId(), region)))
                .orElse(ResponseEntity.notFound().build());
    }
}
