package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trade.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    @Update("""
            UPDATE message SET is_read = TRUE, read_at = #{readAt}
            WHERE conversation_id = #{conversationId} AND sender_id <> #{userId} AND is_read = FALSE
            """)
    int markReceivedAsRead(@Param("conversationId") Long conversationId,
                           @Param("userId") Long userId,
                           @Param("readAt") OffsetDateTime readAt);
}
