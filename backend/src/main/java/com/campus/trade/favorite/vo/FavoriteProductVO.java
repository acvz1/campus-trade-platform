package com.campus.trade.favorite.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FavoriteProductVO(
        Long id,
        String title,
        String mainImage,
        BigDecimal price,
        String status,
        Long categoryId,
        String categoryName,
        OffsetDateTime createdAt
) {
}
