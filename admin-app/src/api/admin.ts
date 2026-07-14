import { request } from './request'

export interface AdminSession { userId: number; nickname: string; role: 'ADMIN' | 'SUPER_ADMIN'; token: string }
export interface Dashboard {
  totalUsers: number; totalProducts: number; totalOrders: number; completedOrders: number
  todayNewUsers: number; todayNewProducts: number; todayNewOrders: number; pendingReviewProducts: number
}
export interface ReviewProduct {
  id: number; title: string; description: string; mainImage?: string; price: number; status: string
  categoryName?: string; seller?: { id: number; nickname: string; phone: string; studentId?: string }; createdAt: string
}
export interface PageResult<T> { records: T[]; total: number; page: number; size: number; pages: number }

export function login(phone: string, password: string) {
  return request<AdminSession>({ method: 'POST', url: '/user/login', data: { phone, password } })
}
export function getDashboard() { return request<Dashboard>({ url: '/admin/dashboard/overview' }) }
export function getPendingProducts(page = 1, size = 20) {
  return request<PageResult<ReviewProduct>>({ url: '/admin/product/review', params: { status: 'PENDING_REVIEW', page, size } })
}
export function auditProduct(id: number, action: 'APPROVE' | 'REJECT', reason?: string) {
  return request<{ id: number; status: string }>({ method: 'POST', url: `/admin/product/${id}/audit`, data: { action, reason } })
}
