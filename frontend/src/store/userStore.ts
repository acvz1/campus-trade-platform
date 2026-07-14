import { create } from 'zustand'
import type { AuthResponse, UserProfile } from '../api/user'

interface UserState {
  token: string
  user: AuthResponse | UserProfile | null
  setSession: (session: AuthResponse) => void
  setUser: (user: UserProfile) => void
  logout: () => void
}

function readStoredUser(): AuthResponse | UserProfile | null {
  try {
    const value = localStorage.getItem('user')
    return value ? JSON.parse(value) as AuthResponse | UserProfile : null
  } catch {
    return null
  }
}

export const useUserStore = create<UserState>((set) => ({
  token: localStorage.getItem('token') ?? '',
  user: readStoredUser(),
  setSession: (session) => {
    localStorage.setItem('token', session.token)
    localStorage.setItem('user', JSON.stringify(session))
    set({ token: session.token, user: session })
  },
  setUser: (user) => {
    localStorage.setItem('user', JSON.stringify(user))
    set({ user })
  },
  logout: () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    set({ token: '', user: null })
  },
}))
