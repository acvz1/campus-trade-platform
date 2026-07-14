package com.campus.trade.order.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderVO(
        Long id,
        String orderNo,
        Long productId,
        String productTitle,
        String productImage,
        BigDecimal price,
        String tradeType,
        String status,
        OrderPartyVO buyer,
        OrderPartyVO seller,
        OrderAddressVO address,
        String buyerRemark,
        OffsetDateTime pickupTime,
        String pickupLocation,
        OffsetDateTime confirmedAt,
        OffsetDateTime completedAt,
        OffsetDateTime cancelledAt,
        String cancelReason,
        Long cancelledBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
