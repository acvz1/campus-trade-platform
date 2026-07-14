package com.campus.trade.admin.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminProductVO(
        Long id,
        String title,
        String description,
        String mainImage,
        BigDecimal price,
        String status,
        Long categoryId,
        String categoryName,
        AdminSellerVO seller,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
