package com.campus.trade.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProductStatusDTO(
        @NotBlank(message = "商品状态不能为空")
        @Pattern(regexp = "ON_SALE|OFF_SHELF", message = "仅支持上架或下架")
        String status
) {
}
