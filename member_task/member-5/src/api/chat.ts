import request from './request'

// 类型定义
export interface Conversation {
  id: number
  productId: number
  buyerId: number
  sellerId: number
  lastMessage: string
  lastMessageTime: string
  buyerUnreadCount: number
  sellerUnreadCount: number
  // 关联的用户信息（后端返回时带上）
  otherUserName?: string
  otherUserAvatar?: string
  productTitle?: string
}

export interface Message {
  id: number
  conversationId: number
  senderId: number
  receiverId: number
  content: string
  isRead: boolean
  createdAt: string
}

// ========== 真实接口（等后端好了用） ==========

// 获取会话列表
export const getConversations = (userId: number) => {
  return request.get<Conversation[]>(`/chat/conversations?userId=${userId}`)
}

// 获取历史消息
export const getMessages = (conversationId: number, page = 1, size = 20) => {
  return request.get<Message[]>(`/chat/messages/${conversationId}?page=${page}&size=${size}`)
}

// 发送消息
export const sendMessage = (conversationId: number, senderId: number, content: string) => {
  return request.post<Message>(`/chat/send?senderId=${senderId}`, {
    conversationId,
    content,
  })
}

// 创建会话
export const createConversation = (productId: number, buyerId: number, sellerId: number) => {
  return request.post<Conversation>(
    `/chat/conversation?productId=${productId}&buyerId=${buyerId}&sellerId=${sellerId}`
  )
}

// 标记已读
export const markAsRead = (conversationId: number, userId: number) => {
  return request.put(`/chat/read/${conversationId}?userId=${userId}`)
}

// ========== 模拟数据（开发阶段使用） ==========

// 模拟会话列表数据
export const mockConversations: Conversation[] = [
  {
    id: 1,
    productId: 1,
    buyerId: 1,
    sellerId: 2,
    lastMessage: '你好，这本书还在吗？',
    lastMessageTime: '2026-07-08 10:30:00',
    buyerUnreadCount: 0,
    sellerUnreadCount: 2,
    otherUserName: '张三',
    otherUserAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan',
    productTitle: '高等数学第七版 上下册',
  },
  {
    id: 2,
    productId: 2,
    buyerId: 1,
    sellerId: 3,
    lastMessage: '好的，下午来自提吧',
    lastMessageTime: '2026-07-07 16:20:00',
    buyerUnreadCount: 1,
    sellerUnreadCount: 0,
    otherUserName: '李四',
    otherUserAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=lisi',
    productTitle: '二手台灯 9成新',
  },
  {
    id: 3,
    productId: 3,
    buyerId: 1,
    sellerId: 4,
    lastMessage: '价格还能再便宜点吗',
    lastMessageTime: '2026-07-06 09:15:00',
    buyerUnreadCount: 3,
    sellerUnreadCount: 0,
    otherUserName: '王五',
    otherUserAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=wangwu',
    productTitle: '羽毛球拍一对',
  },
]

// 模拟获取会话列表（延迟500ms，模拟网络请求）
export const mockGetConversations = (userId: number): Promise<Conversation[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockConversations)
    }, 500)
  })
}

// ========== 模拟消息数据 ==========

// 按会话ID存储的消息
const mockMessagesMap: Record<number, Message[]> = {
  1: [
    {
      id: 1,
      conversationId: 1,
      senderId: 2,
      receiverId: 1,
      content: '你好，这本书还在吗？',
      isRead: true,
      createdAt: '2026-07-08 10:30:00',
    },
    {
      id: 2,
      conversationId: 1,
      senderId: 1,
      receiverId: 2,
      content: '在的，需要的话可以来自提',
      isRead: true,
      createdAt: '2026-07-08 10:32:00',
    },
    {
      id: 3,
      conversationId: 1,
      senderId: 2,
      receiverId: 1,
      content: '好的，下午3点方便吗？',
      isRead: false,
      createdAt: '2026-07-08 10:35:00',
    },
  ],
  2: [
    {
      id: 4,
      conversationId: 2,
      senderId: 1,
      receiverId: 3,
      content: '台灯还在吗？',
      isRead: true,
      createdAt: '2026-07-07 16:00:00',
    },
    {
      id: 5,
      conversationId: 2,
      senderId: 3,
      receiverId: 1,
      content: '在的，9成新，50块',
      isRead: true,
      createdAt: '2026-07-07 16:10:00',
    },
    {
      id: 6,
      conversationId: 2,
      senderId: 1,
      receiverId: 3,
      content: '好的，下午来自提吧',
      isRead: true,
      createdAt: '2026-07-07 16:20:00',
    },
  ],
  3: [
    {
      id: 7,
      conversationId: 3,
      senderId: 1,
      receiverId: 4,
      content: '羽毛球拍怎么卖？',
      isRead: true,
      createdAt: '2026-07-06 09:00:00',
    },
    {
      id: 8,
      conversationId: 3,
      senderId: 4,
      receiverId: 1,
      content: '一对80，送一筒球',
      isRead: true,
      createdAt: '2026-07-06 09:10:00',
    },
    {
      id: 9,
      conversationId: 3,
      senderId: 1,
      receiverId: 4,
      content: '价格还能再便宜点吗',
      isRead: true,
      createdAt: '2026-07-06 09:15:00',
    },
  ],
}

// 模拟获取历史消息
export const mockGetMessages = (conversationId: number): Promise<Message[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockMessagesMap[conversationId] || [])
    }, 300)
  })
}

// 模拟发送消息
export const mockSendMessage = (
  conversationId: number,
  senderId: number,
  content: string
): Promise<Message> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const newMessage: Message = {
        id: Date.now() + Math.random() * 1000,  // ← 这里改了！
        conversationId,
        senderId,
        receiverId: senderId === 1 ? 2 : 1,
        content,
        isRead: false,
        createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
      }
      if (!mockMessagesMap[conversationId]) {
        mockMessagesMap[conversationId] = []
      }
      mockMessagesMap[conversationId].push(newMessage)
      resolve(newMessage)
    }, 300)
  })
}