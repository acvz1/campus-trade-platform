import { createBrowserRouter } from 'react-router-dom'
import AuthGuard from '../components/AuthGuard'
import App from '../App'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import HomePage from '../pages/HomePage'
import ChatListPage from '../pages/ChatListPage'
import ChatDetailPage from '../pages/ChatDetailPage'  

const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    path: '/',
    element: (
      <AuthGuard>
        <App />
      </AuthGuard>
    ),
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: '/chat',
        element: <ChatListPage />,
      },
      {
        path: '/chat/:id',           
        element: <ChatDetailPage />,
      },
      // 后续成员2/3/4 在这里添加更多子路由
    ],
  },
])

export default router