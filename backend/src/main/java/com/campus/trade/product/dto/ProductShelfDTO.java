package com.campus.trade.product.dto;

import jakarta.validation.constraints.NotNull;

public record ProductShelfDTO(@NotNull(message = "请指定上架或下架") Boolean onShelf) {
}
