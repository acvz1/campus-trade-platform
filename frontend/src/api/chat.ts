import { request } from '../utils/request'

export interface ChatUser {
  id: number
  nickname: string
  avatar?: string
}

export interface Conversation {
  id: number
  productId: number
  productTitle: string
  productImage?: string
  productPrice?: number
  otherUser: ChatUser
  lastMessage?: string
  lastMessageTime?: string
  unreadCount: number
  status: 'ACTIVE' | 'CLOSED'
  createdAt: string
}

export interface ChatMessage {
  id: number
  conversationId: number
  senderId: number
  content: string
  messageType: 'TEXT' | 'SYSTEM'
  read: boolean
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export function listConversations(page = 1, size = 50) {
  return request<PageResult<Conversation>>({ url: '/conversation', params: { page, size } })
}

export function createConversation(productId: number) {
  return request<Conversation>({ method: 'POST', url: '/conversation', data: { productId } })
}

export function getConversation(id: number) {
  return request<Conversation>({ url: `/conversation/${id}` })
}

export function listMessages(id: number, page = 1, size = 50, beforeId?: number) {
  return request<PageResult<ChatMessage>>({ url: `/conversation/${id}/messages`, params: { page, size, beforeId } })
}

export function sendMessage(id: number, content: string) {
  return request<ChatMessage>({ method: 'POST', url: `/conversation/${id}/message`, data: { content } })
}
