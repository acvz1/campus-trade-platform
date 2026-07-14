package com.campus.trade.user.vo;

import java.time.OffsetDateTime;

public record UserVO(
        Long id,
        String phone,
        String nickname,
        String avatar,
        String contactPhone,
        String studentId,
        String realName,
        String role,
        String status,
        boolean studentVerified,
        OffsetDateTime createdAt
) {
}
