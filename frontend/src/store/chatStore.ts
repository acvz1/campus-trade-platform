import { create } from 'zustand'
import { listConversations, type Conversation } from '../api/chat'

interface ChatState {
  conversations: Conversation[]
  unreadTotal: number
  loading: boolean
  loadConversations: () => Promise<void>
  clear: () => void
}

export const useChatStore = create<ChatState>((set) => ({
  conversations: [],
  unreadTotal: 0,
  loading: false,
  loadConversations: async () => {
    set({ loading: true })
    try {
      const result = await listConversations()
      set({
        conversations: result.records,
        unreadTotal: result.records.reduce((total, item) => total + item.unreadCount, 0),
      })
    } finally {
      set({ loading: false })
    }
  },
  clear: () => set({ conversations: [], unreadTotal: 0, loading: false }),
}))
