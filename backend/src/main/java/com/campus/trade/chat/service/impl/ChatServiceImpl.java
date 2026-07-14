package com.campus.trade.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trade.chat.dto.ConversationCreateDTO;
import com.campus.trade.chat.dto.ConversationQueryDTO;
import com.campus.trade.chat.dto.MessageQueryDTO;
import com.campus.trade.chat.dto.MessageSendDTO;
import com.campus.trade.chat.service.ChatService;
import com.campus.trade.chat.vo.ChatUserVO;
import com.campus.trade.chat.vo.ConversationVO;
import com.campus.trade.chat.vo.MessageVO;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.ResultCode;
import com.campus.trade.entity.Conversation;
import com.campus.trade.entity.Message;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductImage;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.ConversationMapper;
import com.campus.trade.mapper.MessageMapper;
import com.campus.trade.mapper.ProductImageMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final UserMapper userMapper;

    public ChatServiceImpl(ConversationMapper conversationMapper, MessageMapper messageMapper,
                           ProductMapper productMapper, ProductImageMapper productImageMapper,
                           UserMapper userMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<ConversationVO> listConversations(Long userId, ConversationQueryDTO query) {
        requireActiveUser(userId);
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .and(item -> item.eq(Conversation::getBuyerId, userId).or().eq(Conversation::getSellerId, userId))
                .orderByDesc(Conversation::getLastMessageAt)
                .orderByDesc(Conversation::getUpdatedAt);
        Page<Conversation> page = conversationMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<>(enrich(page.getRecords(), userId), page.getTotal(), page.getCurrent(),
                page.getSize(), page.getPages());
    }

    @Override
    public ConversationVO createConversation(Long userId, ConversationCreateDTO dto) {
        requireActiveUser(userId);
        Product product = productMapper.selectById(dto.productId());
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (product.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.CONFLICT, "不能与自己发起会话");
        }
        Conversation existing = findByParticipants(dto.productId(), userId, product.getSellerId());
        if (existing != null) return enrich(List.of(existing), userId).get(0);

        Conversation conversation = new Conversation();
        conversation.setProductId(product.getId());
        conversation.setBuyerId(userId);
        conversation.setSellerId(product.getSellerId());
        conversation.setBuyerUnreadCount(0);
        conversation.setSellerUnreadCount(0);
        conversation.setStatus("ACTIVE");
        try {
            conversationMapper.insert(conversation);
        } catch (DataIntegrityViolationException conflict) {
            Conversation concurrent = findByParticipants(dto.productId(), userId, product.getSellerId());
            if (concurrent != null) return enrich(List.of(concurrent), userId).get(0);
            throw conflict;
        }
        return enrich(List.of(conversation), userId).get(0);
    }

    @Override
    public ConversationVO getConversation(Long userId, Long conversationId) {
        Conversation conversation = requireParticipant(userId, conversationId);
        return enrich(List.of(conversation), userId).get(0);
    }

    @Override
    @Transactional
    public PageResult<MessageVO> listMessages(Long userId, Long conversationId, MessageQueryDTO query) {
        requireParticipant(userId, conversationId);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId);
        if (query.getBeforeId() != null) wrapper.lt(Message::getId, query.getBeforeId());
        wrapper.orderByDesc(Message::getCreatedAt).orderByDesc(Message::getId);
        Page<Message> page = messageMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        List<Message> chronological = new ArrayList<>(page.getRecords());
        Collections.reverse(chronological);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        messageMapper.markReceivedAsRead(conversationId, userId, now);
        conversationMapper.clearUnread(conversationId, userId);
        return new PageResult<>(chronological.stream().map(this::toMessageVO).toList(), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    @Transactional
    public MessageVO sendMessage(Long userId, Long conversationId, MessageSendDTO dto) {
        Conversation conversation = requireParticipant(userId, conversationId);
        if (!"ACTIVE".equals(conversation.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "会话已关闭");
        }
        if (dto.conversationId() != null && !conversationId.equals(dto.conversationId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "会话ID不一致");
        }
        String content = dto.content().trim();
        if (content.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能为空");
        }
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setContent(content);
        message.setMessageType("TEXT");
        message.setIsRead(false);
        messageMapper.insert(message);
        String summary = content.length() > 500 ? content.substring(0, 500) : content;
        conversationMapper.updateAfterMessage(conversationId, userId, summary, message.getCreatedAt());
        return toMessageVO(message);
    }

    private List<ConversationVO> enrich(List<Conversation> conversations, Long viewerId) {
        if (conversations.isEmpty()) return List.of();
        Set<Long> productIds = conversations.stream().map(Conversation::getProductId).collect(Collectors.toSet());
        Set<Long> otherIds = conversations.stream()
                .map(item -> item.getBuyerId().equals(viewerId) ? item.getSellerId() : item.getBuyerId())
                .collect(Collectors.toSet());
        Map<Long, Product> products = productMapper.selectByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, User> users = userMapper.selectByIds(otherIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, String> images = new HashMap<>();
        productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, productIds)
                        .orderByDesc(ProductImage::getIsMain)
                        .orderByAsc(ProductImage::getSortOrder))
                .forEach(image -> images.putIfAbsent(image.getProductId(), image.getImageUrl()));
        return conversations.stream().map(item -> {
            Long otherId = item.getBuyerId().equals(viewerId) ? item.getSellerId() : item.getBuyerId();
            return toConversationVO(item, viewerId, products.get(item.getProductId()), users.get(otherId),
                    images.get(item.getProductId()));
        }).toList();
    }

    private ConversationVO toConversationVO(Conversation conversation, Long viewerId, Product product,
                                              User other, String image) {
        int unread = conversation.getBuyerId().equals(viewerId)
                ? safeCount(conversation.getBuyerUnreadCount()) : safeCount(conversation.getSellerUnreadCount());
        ChatUserVO otherVO = other == null ? null : new ChatUserVO(other.getId(), other.getNickname(), other.getAvatarUrl());
        return new ConversationVO(conversation.getId(), conversation.getProductId(),
                product == null ? "商品已删除" : product.getTitle(), image,
                product == null ? null : product.getPrice(), otherVO, conversation.getLastMessage(),
                conversation.getLastMessageAt(), unread, conversation.getStatus(), conversation.getCreatedAt());
    }

    private MessageVO toMessageVO(Message message) {
        return new MessageVO(message.getId(), message.getConversationId(), message.getSenderId(),
                message.getContent(), message.getMessageType(), Boolean.TRUE.equals(message.getIsRead()),
                message.getCreatedAt());
    }

    private Conversation findByParticipants(Long productId, Long buyerId, Long sellerId) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getProductId, productId)
                .eq(Conversation::getBuyerId, buyerId)
                .eq(Conversation::getSellerId, sellerId));
    }

    private Conversation requireParticipant(Long userId, Long conversationId) {
        requireActiveUser(userId);
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能访问自己参与的会话");
        }
        return conversation;
    }

    private User requireActiveUser(Long userId) {
        User user = requireUser(userId);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账号不可使用私信");
        }
        return user;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        return user;
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }
}
