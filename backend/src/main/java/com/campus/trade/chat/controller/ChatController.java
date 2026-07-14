package com.campus.trade.chat.controller;

import com.campus.trade.chat.dto.ConversationCreateDTO;
import com.campus.trade.chat.dto.ConversationQueryDTO;
import com.campus.trade.chat.dto.MessageQueryDTO;
import com.campus.trade.chat.dto.MessageSendDTO;
import com.campus.trade.chat.service.ChatService;
import com.campus.trade.chat.vo.ConversationVO;
import com.campus.trade.chat.vo.MessageVO;
import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.Result;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.ResultCode;
import com.campus.trade.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "私信")
@RestController
@RequestMapping("/api")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "会话列表")
    @GetMapping({"/conversation", "/chat/conversations"})
    public Result<PageResult<ConversationVO>> conversations(@AuthenticationPrincipal AuthenticatedUser user,
                                                             @Valid @ModelAttribute ConversationQueryDTO query) {
        return Result.success(chatService.listConversations(user.userId(), query));
    }

    @Operation(summary = "创建或获取会话")
    @PostMapping("/conversation")
    public Result<ConversationVO> create(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody ConversationCreateDTO dto) {
        return Result.success(chatService.createConversation(user.userId(), dto));
    }

    @Operation(summary = "会话详情")
    @GetMapping("/conversation/{id}")
    public Result<ConversationVO> detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return Result.success(chatService.getConversation(user.userId(), id));
    }

    @Operation(summary = "消息列表")
    @GetMapping({"/conversation/{id}/messages", "/chat/messages/{id}"})
    public Result<PageResult<MessageVO>> messages(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable Long id,
                                                   @Valid @ModelAttribute MessageQueryDTO query) {
        return Result.success(chatService.listMessages(user.userId(), id, query));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/conversation/{id}/message")
    public Result<MessageVO> send(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                  @Valid @RequestBody MessageSendDTO dto) {
        return Result.success(chatService.sendMessage(user.userId(), id, dto));
    }

    @Operation(summary = "发送消息（任务书兼容路径）")
    @PostMapping("/chat/send")
    public Result<MessageVO> sendCompatible(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody MessageSendDTO dto) {
        if (dto.conversationId() == null || dto.conversationId() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "会话ID不能为空");
        }
        return Result.success(chatService.sendMessage(user.userId(), dto.conversationId(), dto));
    }
}
