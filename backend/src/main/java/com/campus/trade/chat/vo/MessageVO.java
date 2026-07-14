package com.campus.trade.chat.vo;

import java.time.OffsetDateTime;

public record MessageVO(
        Long id,
        Long conversationId,
        Long senderId,
        String content,
        String messageType,
        boolean read,
        OffsetDateTime createdAt
) {
}
