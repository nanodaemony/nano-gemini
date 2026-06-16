package com.naon.grid.modules.app.service.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建订阅订单请求 DTO
 */
@Data
public class CreateOrderDTO {
    @NotBlank(message = "会员级别不能为空")
    private String level;    // "VIP" / "SVIP"

    @NotNull(message = "订阅时长不能为空")
    private Integer periodMonths;  // 订阅月数
}
