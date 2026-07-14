package com.campus.trade.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderQueryDTO {
    @Pattern(regexp = "buyer|seller", message = "订单角色不正确")
    private String role = "buyer";
    @Pattern(regexp = "PENDING_COMMUNICATION|PENDING_PICKUP|COMPLETED|CANCELLED", message = "订单状态不正确")
    private String status;
    @Min(value = 1, message = "页码最小为1")
    private int page = 1;
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private int size = 20;
}
