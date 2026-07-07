import { create } from 'zustand'

interface UserInfo {
  token: string
  username: string
  setToken: (val:string)=>void
  setName: (val:string)=>void
}

export const useUserStore = create<UserInfo>((set) => ({
  token: '',
  username: '',
  setToken: (val) => set({token:val}),
  setName: (val) => set({username:val})
}))