package com.campus.trade.product.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ProductVO(
        Long id,
        String title,
        String description,
        String mainImage,
        BigDecimal price,
        BigDecimal originalPrice,
        String conditionLevel,
        String tradeType,
        String tradeRemark,
        String status,
        Integer viewCount,
        Integer favoriteCount,
        Long categoryId,
        String categoryName,
        List<ProductImageVO> images,
        SellerVO seller,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
