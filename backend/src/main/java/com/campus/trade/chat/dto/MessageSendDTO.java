package com.campus.trade.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageSendDTO(
        Long conversationId,
        @NotBlank(message = "消息内容不能为空") @Size(max = 1000, message = "消息内容不能超过1000字") String content
) {
    public MessageSendDTO(String content) {
        this(null, content);
    }
}
