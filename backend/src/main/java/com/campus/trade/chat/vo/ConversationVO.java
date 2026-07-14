package com.campus.trade.chat.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ConversationVO(
        Long id,
        Long productId,
        String productTitle,
        String productImage,
        BigDecimal productPrice,
        ChatUserVO otherUser,
        String lastMessage,
        OffsetDateTime lastMessageTime,
        int unreadCount,
        String status,
        OffsetDateTime createdAt
) {
}
