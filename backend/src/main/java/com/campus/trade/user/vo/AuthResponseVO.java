package com.campus.trade.user.vo;

public record AuthResponseVO(
        Long userId,
        String phone,
        String nickname,
        String role,
        String status,
        boolean studentVerified,
        String token
) {
}
