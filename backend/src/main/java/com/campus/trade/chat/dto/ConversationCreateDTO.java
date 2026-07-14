package com.campus.trade.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConversationCreateDTO(
        @NotNull(message = "商品ID不能为空") @Positive(message = "商品ID无效") Long productId
) {
}
