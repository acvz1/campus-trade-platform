package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trade.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    @Update("""
            UPDATE conversation
            SET last_message = #{content}, last_message_at = #{sentAt}, updated_at = #{sentAt},
                buyer_unread_count = buyer_unread_count + CASE WHEN seller_id = #{senderId} THEN 1 ELSE 0 END,
                seller_unread_count = seller_unread_count + CASE WHEN buyer_id = #{senderId} THEN 1 ELSE 0 END
            WHERE id = #{conversationId}
            """)
    int updateAfterMessage(@Param("conversationId") Long conversationId,
                           @Param("senderId") Long senderId,
                           @Param("content") String content,
                           @Param("sentAt") OffsetDateTime sentAt);

    @Update("""
            UPDATE conversation
            SET buyer_unread_count = CASE WHEN buyer_id = #{userId} THEN 0 ELSE buyer_unread_count END,
                seller_unread_count = CASE WHEN seller_id = #{userId} THEN 0 ELSE seller_unread_count END
            WHERE id = #{conversationId}
            """)
    int clearUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
