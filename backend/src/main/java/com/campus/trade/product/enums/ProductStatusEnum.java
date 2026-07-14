package com.campus.trade.product.enums;

import java.util.Arrays;

public enum ProductStatusEnum {
    PENDING_REVIEW,
    ON_SALE,
    REJECTED,
    OFF_SHELF,
    SOLD,
    VIOLATION_DELISTED,
    DELETED;

    public static boolean supports(String value) {
        return value != null && Arrays.stream(values()).anyMatch(item -> item.name().equals(value));
    }
}
