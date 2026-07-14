import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import NavBar from './components/NavBar'
import { useChatStore } from './stores/chat'
import { getConversations } from './api/chat'   

function App() {
  const location = useLocation()
  const { setConversations } = useChatStore()
  
  // 登录页和注册页不显示底部导航
  const hideNavBar = ['/login', '/register'].includes(location.pathname)

  // 当前用户ID（实际应从 userStore 获取）
  const currentUserId = 1

  // 消息轮询：每10秒刷新会话列表（更新未读数）
  useEffect(() => {
    const fetchConversations = async () => {
  try {
    const response = await getConversations(currentUserId)
    console.log('后端返回:', response)
    setConversations(response.data)
  } catch (error) {
    console.error('轮询刷新会话失败:', error)
  }
}

    // 首次加载
    fetchConversations()

    // 每10秒轮询
    const interval = setInterval(fetchConversations, 10000)

    return () => clearInterval(interval)
  }, [currentUserId, setConversations])

  return (
    <div style={{ paddingBottom: hideNavBar ? 0 : 64 }}>
      <Outlet />
      {!hideNavBar && <NavBar />}
    </div>
  )
}

export default App