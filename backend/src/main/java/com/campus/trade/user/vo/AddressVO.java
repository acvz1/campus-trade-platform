package com.campus.trade.user.vo;

import java.time.OffsetDateTime;

public record AddressVO(
        Long id,
        String contact,
        String phone,
        String campus,
        String building,
        String room,
        String detail,
        boolean isDefault,
        OffsetDateTime createdAt
) {
}
