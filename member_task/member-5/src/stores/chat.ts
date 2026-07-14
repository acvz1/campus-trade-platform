import { create } from 'zustand'
import type { Conversation, Message } from '../api/chat'

interface ChatState {
  conversations: Conversation[]
  currentConversationId: number | null
  messages: Message[]
  unreadCount: number
  loading: boolean

  setConversations: (conversations: Conversation[]) => void
  setCurrentConversation: (id: number) => void
  setMessages: (messages: Message[]) => void
  addMessage: (message: Message) => void
  updateUnreadCount: () => void
  clearUnread: (conversationId: number) => void
}

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  currentConversationId: null,
  messages: [],
  unreadCount: 0,
  loading: false,

  setConversations: (conversations) => {
    const unreadCount = conversations.reduce((sum, conv) => {
      return sum + (conv.buyerUnreadCount || 0)
    }, 0)
    set({ conversations, unreadCount })
  },

  setCurrentConversation: (id) => set({ currentConversationId: id }),

  setMessages: (messages) => set({ messages }),

  addMessage: (message) => {
    set((state) => {
      // 检查是否已存在相同 id 的消息（防止重复添加）
      const exists = state.messages.some((m) => m.id === message.id)
      if (exists) return state
      return {
        messages: [...state.messages, message],
      }
    })
  },

  updateUnreadCount: () => {
    const { conversations } = get()
    const unreadCount = conversations.reduce((sum, conv) => {
      return sum + (conv.buyerUnreadCount || 0)
    }, 0)
    set({ unreadCount })
  },

  clearUnread: (conversationId) => {
    set((state) => ({
      conversations: state.conversations.map((conv) =>
        conv.id === conversationId
          ? { ...conv, buyerUnreadCount: 0 }
          : conv
      ),
    }))
    get().updateUnreadCount()
  },
}))