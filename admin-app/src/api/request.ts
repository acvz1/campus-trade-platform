import axios, { type AxiosRequestConfig } from 'axios'

interface ApiResponse<T> { code: number; message: string; data: T }

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api', timeout: 10_000 })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use((response) => response, (error) => {
  const errorMessage = error.response?.data?.message ?? error.message ?? '网络请求失败'
  if (error.response?.status === 401 || error.response?.status === 403) {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
    if (window.location.pathname !== '/admin/login') window.location.assign('/admin/login')
  }
  return Promise.reject(new Error(errorMessage))
})

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResponse<T>>(config)
  if (response.data.code !== 200) throw new Error(response.data.message || '请求失败')
  return response.data.data
}
