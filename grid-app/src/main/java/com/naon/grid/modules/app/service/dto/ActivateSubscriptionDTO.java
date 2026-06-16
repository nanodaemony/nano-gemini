package com.naon.grid.modules.app.service.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 激活订阅请求 DTO（支付系统回调时调用）
 */
@Data
public class ActivateSubscriptionDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "会员级别不能为空")
    private String level;    // "VIP" / "SVIP"

    @NotNull(message = "订阅天数不能为空")
    private Integer days;    // 订阅有效天数
}
