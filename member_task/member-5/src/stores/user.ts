import { create } from 'zustand'

interface UserInfo {
  id: number
  username: string
  phone: string
  schoolId: string
  avatar?: string
}

interface UserState {
  token: string | null
  userInfo: UserInfo | null
  setToken: (token: string) => void
  setUserInfo: (userInfo: UserInfo) => void
  logout: () => void
}

export const useUserStore = create<UserState>((set) => ({
  token: localStorage.getItem('token'),
  userInfo: null,
  setToken: (token) => {
    localStorage.setItem('token', token)
    set({ token })
  },
  setUserInfo: (userInfo) => set({ userInfo }),
  logout: () => {
    localStorage.removeItem('token')
    set({ token: null, userInfo: null })
    window.location.href = '/login'
  },
}))