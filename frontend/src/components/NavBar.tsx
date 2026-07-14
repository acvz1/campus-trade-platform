import { Badge } from 'antd'
import { NavLink, useLocation } from 'react-router-dom'
import { useChatStore } from '../store/chatStore'

const items = [
  { path: '/', label: '首页', icon: '⌂', exact: true },
  { path: '/chat', label: '消息', icon: '✉' },
  { path: '/product/publish', label: '发布', icon: '+', publish: true },
  { path: '/orders', label: '订单', icon: '▤' },
  { path: '/user/profile', label: '我的', icon: '○' },
]

export default function NavBar({ activeTab }: { activeTab?: string }) {
  const location = useLocation()
  const unread = useChatStore((state) => state.unreadTotal)
  return (
    <nav className="bottom-nav" aria-label="主要导航">
      {items.map((item) => {
        const active = activeTab ? activeTab === item.label : item.exact
          ? location.pathname === item.path
          : location.pathname.startsWith(item.path)
        return <NavLink key={item.path} to={item.path} className={`${active ? 'active' : ''} ${item.publish ? 'publish-tab' : ''}`}>
          <Badge count={item.path === '/chat' ? unread : 0} size="small" overflowCount={99}>
            <span className="bottom-nav-icon">{item.icon}</span>
          </Badge>
          <span>{item.label}</span>
        </NavLink>
      })}
    </nav>
  )
}
