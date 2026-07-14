import { lazy, Suspense, useEffect } from 'react'
import { Badge, Button, Space, Spin } from 'antd'
import { Link, Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom'
import { useUserStore } from './store/userStore'
import { useChatStore } from './store/chatStore'
import NavBar from './components/NavBar'
import './App.css'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const HomePage = lazy(() => import('./pages/HomePage'))
const ProductDetailPage = lazy(() => import('./pages/ProductDetailPage'))
const ProductPublishPage = lazy(() => import('./pages/ProductPublishPage'))
const ProductEditPage = lazy(() => import('./pages/ProductEditPage'))
const UserProductsPage = lazy(() => import('./pages/UserProductsPage'))
const SearchPage = lazy(() => import('./pages/SearchPage'))
const OrdersPage = lazy(() => import('./pages/OrdersPage'))
const OrderDetailPage = lazy(() => import('./pages/OrderDetailPage'))
const FavoritesPage = lazy(() => import('./pages/FavoritesPage'))
const UserProfilePage = lazy(() => import('./pages/UserProfilePage'))
const UserAddressPage = lazy(() => import('./pages/UserAddressPage'))
const ChatListPage = lazy(() => import('./pages/ChatListPage'))
const ChatDetailPage = lazy(() => import('./pages/ChatDetailPage'))

function ProtectedRoute() {
  const token = useUserStore((state) => state.token)
  return token ? <Outlet /> : <Navigate to="/login" replace />
}

function SiteLayout() {
  const navigate = useNavigate()
  const token = useUserStore((state) => state.token)
  const user = useUserStore((state) => state.user)
  const logout = useUserStore((state) => state.logout)
  const unreadTotal = useChatStore((state) => state.unreadTotal)
  const loadConversations = useChatStore((state) => state.loadConversations)
  const clearChat = useChatStore((state) => state.clear)

  useEffect(() => {
    if (!token) { clearChat(); return }
    loadConversations().catch(() => undefined)
    const timer = window.setInterval(() => loadConversations().catch(() => undefined), 10_000)
    return () => window.clearInterval(timer)
  }, [clearChat, loadConversations, token])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="site-shell">
      <header className="site-header">
        <Link className="brand" to="/">
          <span className="brand-mark">循</span>
          <span>校园循物</span>
        </Link>
        <nav>
          {token ? (
            <Space>
              <span className="welcome-text">你好，{user?.nickname ?? '校园用户'}</span>
              <Link to="/">逛好物</Link>
              <Link to="/product/publish">发布</Link>
              <Link to="/user/products">我的发布</Link>
              <Link to="/orders">订单</Link>
              <Badge count={unreadTotal} size="small"><Link to="/chat">消息</Link></Badge>
              <Link to="/user/favorites">收藏</Link>
              <Link to="/user/profile">个人中心</Link>
              <Link to="/user/address">地址</Link>
              <Button type="text" onClick={handleLogout}>退出</Button>
            </Space>
          ) : (
            <Space>
              <Link to="/search">搜索</Link>
              <Link to="/login">登录</Link>
              <Button type="primary" onClick={() => navigate('/register')}>加入校园</Button>
            </Space>
          )}
        </nav>
      </header>
      <main className="site-content"><Outlet /></main>
      {token && <NavBar />}
    </div>
  )
}

function App() {
  return (
    <Suspense fallback={<div className="page-loading"><Spin size="large" /></div>}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<SiteLayout />}>
          <Route index element={<HomePage />} />
          <Route path="/product/:id" element={<ProductDetailPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/product/publish" element={<ProductPublishPage />} />
            <Route path="/product/edit/:id" element={<ProductEditPage />} />
            <Route path="/user/products" element={<UserProductsPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/order/:id" element={<OrderDetailPage />} />
            <Route path="/user/favorites" element={<FavoritesPage />} />
            <Route path="/user/profile" element={<UserProfilePage />} />
            <Route path="/user/address" element={<UserAddressPage />} />
            <Route path="/chat" element={<ChatListPage />} />
            <Route path="/chat/:id" element={<ChatDetailPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}

export default App
