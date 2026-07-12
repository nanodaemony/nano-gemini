package com.naon.grid.modules.billing.service.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class OrderCreateRequest {
    @NotBlank(message = "产品代码不能为空")
    private String productCode;

    @NotBlank(message = "计费周期不能为空")
    private String billingCycle;

    private String region; // 不要求前端传入，后端从 request attribute 取
    private String currency; // 结算币种，不传则按区域默认
    private Integer orgId; // 机构下单时传入
    private String couponCode; // 优惠券代码
}
