import { Button, Layout, Menu, Typography } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'

const { Header, Sider, Content } = Layout

export default function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const user = (() => { try { return JSON.parse(localStorage.getItem('admin_user') ?? '{}') } catch { return {} } })()
  const logout = () => {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
    navigate('/admin/login')
  }
  return <Layout className="admin-shell">
    <Sider width={230} className="admin-sider">
      <div className="admin-brand"><span>循</span><div><strong>校园循物</strong><small>ADMIN CONSOLE</small></div></div>
      <Menu theme="dark" mode="inline" selectedKeys={[location.pathname]} onClick={({ key }) => navigate(key)} items={[
        { key: '/admin/dashboard', label: '数据看板' },
        { key: '/admin/audit', label: '商品审核' },
      ]} />
    </Sider>
    <Layout>
      <Header className="admin-header">
        <Typography.Text>校园交易平台运营中心</Typography.Text>
        <div><span>{user.nickname ?? '管理员'}</span><Button type="text" onClick={logout}>退出</Button></div>
      </Header>
      <Content className="admin-content"><Outlet /></Content>
    </Layout>
  </Layout>
}
