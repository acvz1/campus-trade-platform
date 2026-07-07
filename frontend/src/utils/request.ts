import axios from 'axios'

// 创建请求实例
const request = axios.create({
  baseURL: 'http://后端给你的接口地址',
  timeout: 10000
})

// 请求拦截：统一携带登录token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if(token) config.headers.Authorization = token
  return config
})

// 响应拦截：简化返回数据
request.interceptors.response.use(res => {
  return res.data
}, err => {
  alert('接口请求失败')
  return Promise.reject(err)
})

export default request