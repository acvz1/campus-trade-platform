package com.campus.trade.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderCreateDTO(
        @NotNull(message = "商品ID不能为空") Long productId,
        Long addressId,
        @JsonAlias("buyerRemark")
        @Size(max = 200, message = "买家备注不能超过200字")
        String remark
) {
}
