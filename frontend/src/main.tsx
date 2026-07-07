import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import { BrowserRouter } from 'react-router-dom'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    {/* 路由全局包裹，实现页面跳转 */}
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>,
)