import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ConfigProvider } from 'antd'
import { BrowserRouter } from 'react-router-dom'
import AdminRouter from './router'
import './styles.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode><ConfigProvider theme={{ token: { colorPrimary: '#12664f', borderRadius: 10, fontFamily: 'Inter, "PingFang SC", sans-serif' } }}><BrowserRouter><AdminRouter /></BrowserRouter></ConfigProvider></StrictMode>,
)
