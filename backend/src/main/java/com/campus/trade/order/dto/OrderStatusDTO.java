package com.campus.trade.order.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrderStatusDTO(
        @Pattern(regexp = "CONFIRM_PICKUP|COMPLETE|CANCEL", message = "订单操作不正确") String action,
        @Pattern(regexp = "PENDING_PICKUP|COMPLETED|CANCELLED", message = "目标状态不正确") String status,
        String pickupTime,
        @Size(max = 255, message = "自提地点不能超过255字") String pickupLocation,
        @Size(max = 255, message = "取消原因不能超过255字") String cancelReason
) {
    @AssertTrue(message = "订单操作不能为空")
    public boolean isActionPresent() {
        return (action != null && !action.isBlank()) || (status != null && !status.isBlank());
    }
}
