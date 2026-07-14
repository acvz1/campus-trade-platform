import { request } from '../utils/request'

export interface AuthResponse {
  userId: number
  phone: string
  nickname: string
  role: string
  status: string
  studentVerified: boolean
  token: string
}

export interface UserProfile {
  id: number
  phone: string
  nickname: string
  avatar?: string
  contactPhone?: string
  studentId?: string
  realName?: string
  role: string
  status: string
  studentVerified: boolean
  createdAt: string
}

export interface Address {
  id: number
  contact: string
  phone: string
  campus: string
  building: string
  room?: string
  detail: string
  isDefault: boolean
  createdAt: string
}

export interface AddressPayload {
  contactName: string
  contactPhone: string
  campus: string
  building: string
  room?: string
  detail?: string
  isDefault?: boolean
}

export interface RegisterPayload {
  phone: string
  smsCode: string
  password: string
  nickname?: string
  studentId?: string
  realName?: string
}

export const sendSmsCode = (phone: string, scene: 'REGISTER' | 'LOGIN' | 'CHANGE_PHONE') =>
  request<null>({ method: 'POST', url: '/user/sms/send', data: { phone, scene } })

export const register = (data: RegisterPayload) =>
  request<AuthResponse>({ method: 'POST', url: '/user/register', data })

export const login = (phone: string, password: string) =>
  request<AuthResponse>({ method: 'POST', url: '/user/login', data: { phone, password } })

export const getProfile = () => request<UserProfile>({ method: 'GET', url: '/user/profile' })

export const updateProfile = (data: { nickname?: string; avatar?: string; contactPhone?: string }) =>
  request<UserProfile>({ method: 'PUT', url: '/user/profile', data })

export const authenticateStudent = (studentId: string, realName: string) =>
  request<{ authStatus: string }>({ method: 'POST', url: '/user/auth/student', data: { studentId, realName } })

export const listAddresses = () => request<Address[]>({ method: 'GET', url: '/user/address' })

export const createAddress = (data: AddressPayload) =>
  request<Address>({ method: 'POST', url: '/user/address', data })

export const updateAddress = (id: number, data: AddressPayload) =>
  request<Address>({ method: 'PUT', url: `/user/address/${id}`, data })

export const deleteAddress = (id: number) =>
  request<null>({ method: 'DELETE', url: `/user/address/${id}` })

export const setDefaultAddress = (id: number) =>
  request<null>({ method: 'PUT', url: `/user/address/${id}/default` })

export const uploadAvatar = (file: File) => {
  const data = new FormData()
  data.append('file', file)
  data.append('type', 'avatar')
  return request<{ url: string }>({ method: 'POST', url: '/file/upload', data })
}
