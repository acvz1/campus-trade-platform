package com.campus.trade.order.dto;

import jakarta.validation.constraints.Size;

public record OrderCancelDTO(@Size(max = 255, message = "取消原因不能超过255字") String cancelReason) {
}
