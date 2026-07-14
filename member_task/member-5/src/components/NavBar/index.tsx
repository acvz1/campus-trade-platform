import { Menu } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'
import { HomeOutlined, MessageOutlined, PlusCircleOutlined, ShoppingCartOutlined, UserOutlined } from '@ant-design/icons'
import { useUserStore } from '../../stores/user'

const NavBar = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { token } = useUserStore()

  const items = [
    { key: '/', icon: <HomeOutlined />, label: '首页' },
    { key: '/chat', icon: <MessageOutlined />, label: '消息' },
    { key: '/product/publish', icon: <PlusCircleOutlined />, label: '发布' },
    { key: '/orders', icon: <ShoppingCartOutlined />, label: '订单' },
    { key: '/user/profile', icon: <UserOutlined />, label: '我的' },
  ]

  if (!token) return null

  return (
    <div style={{
      position: 'fixed',
      bottom: 0,
      left: 0,
      right: 0,
      zIndex: 1000,
      background: '#fff',
      borderTop: '1px solid #f0f0f0',
      boxShadow: '0 -2px 8px rgba(0,0,0,0.06)',
    }}>
      <Menu
        mode="horizontal"
        selectedKeys={[location.pathname]}
        items={items}
        onClick={({ key }) => navigate(key)}
        style={{
          display: 'flex',
          justifyContent: 'space-around',
          borderBottom: 'none',
          padding: '4px 0',
        }}
      />
    </div>
  )
}

export default NavBar