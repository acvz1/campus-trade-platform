import { request } from '../utils/request'
import type { PageResult } from './product'

export type OrderStatus = 'PENDING_COMMUNICATION' | 'PENDING_PICKUP' | 'COMPLETED' | 'CANCELLED'

export interface OrderParty {
  id: number
  nickname: string
  avatar?: string
  phone: string
}

export interface OrderAddress {
  id: number
  contact: string
  phone: string
  campus: string
  building: string
  room?: string
  detail?: string
}

export interface Order {
  id: number
  orderNo: string
  productId: number
  productTitle: string
  productImage?: string
  price: number
  tradeType: string
  status: OrderStatus
  buyer: OrderParty
  seller: OrderParty
  address?: OrderAddress
  buyerRemark?: string
  pickupTime?: string
  pickupLocation?: string
  confirmedAt?: string
  completedAt?: string
  cancelledAt?: string
  cancelReason?: string
  cancelledBy?: number
  createdAt: string
  updatedAt: string
}

export const createOrder = (data: { productId: number; addressId?: number; remark?: string }) =>
  request<Order>({ method: 'POST', url: '/order', data })

export const listOrders = (params: { role: 'buyer' | 'seller'; status?: OrderStatus; page?: number; size?: number }) =>
  request<PageResult<Order>>({ method: 'GET', url: '/order', params })

export const getOrder = (id: number) => request<Order>({ method: 'GET', url: `/order/${id}` })

export const updateOrderStatus = (id: number, data: { action: 'CONFIRM_PICKUP' | 'COMPLETE'; pickupTime?: string; pickupLocation?: string }) =>
  request<Order>({ method: 'PUT', url: `/order/${id}/status`, data })

export const cancelOrder = (id: number, cancelReason?: string) =>
  request<Order>({ method: 'PUT', url: `/order/${id}/cancel`, data: { cancelReason } })
