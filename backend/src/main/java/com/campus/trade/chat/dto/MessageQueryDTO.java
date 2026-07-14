package com.campus.trade.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MessageQueryDTO {
    @Min(value = 1, message = "页码必须大于0")
    private long page = 1;
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页最多100条")
    private long size = 50;
    @Positive(message = "消息游标无效")
    private Long beforeId;
}
