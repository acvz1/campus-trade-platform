package com.campus.trade.admin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AuditDTO(
        Long productId,
        @NotBlank(message = "审核动作不能为空") String action,
        @Size(max = 255, message = "审核原因不能超过255字") String reason
) {
    private static final Set<String> ACTIONS = Set.of("APPROVE", "REJECT");

    @AssertTrue(message = "审核动作无效，驳回时必须填写原因")
    public boolean isValid() {
        if (action == null || !ACTIONS.contains(action.toUpperCase())) return false;
        return !"REJECT".equalsIgnoreCase(action) || (reason != null && !reason.isBlank());
    }
}
