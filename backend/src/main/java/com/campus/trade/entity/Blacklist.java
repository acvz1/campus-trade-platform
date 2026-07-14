package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("blacklist")
public class Blacklist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String reason;
    private Long operatorId;
    private String status;
    private String removedReason;
    private Long removedBy;
    private OffsetDateTime removedAt;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
