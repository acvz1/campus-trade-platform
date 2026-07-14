package com.campus.trade.chat.service;

import com.campus.trade.chat.dto.ConversationCreateDTO;
import com.campus.trade.chat.dto.ConversationQueryDTO;
import com.campus.trade.chat.dto.MessageQueryDTO;
import com.campus.trade.chat.dto.MessageSendDTO;
import com.campus.trade.chat.vo.ConversationVO;
import com.campus.trade.chat.vo.MessageVO;
import com.campus.trade.common.result.PageResult;

public interface ChatService {
    PageResult<ConversationVO> listConversations(Long userId, ConversationQueryDTO query);
    ConversationVO createConversation(Long userId, ConversationCreateDTO dto);
    ConversationVO getConversation(Long userId, Long conversationId);
    PageResult<MessageVO> listMessages(Long userId, Long conversationId, MessageQueryDTO query);
    MessageVO sendMessage(Long userId, Long conversationId, MessageSendDTO dto);
}
