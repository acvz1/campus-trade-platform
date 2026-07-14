package com.campus.trade.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasonDTO(
        @NotBlank(message = "操作原因不能为空") @Size(max = 255, message = "操作原因不能超过255字") String reason
) {
}
