import { Button, Form, Input, Typography, message } from 'antd'
import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { login } from '../api/admin'

interface LoginValues { phone: string; password: string }

export default function AdminLoginPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  if (localStorage.getItem('admin_token')) return <Navigate to="/admin/dashboard" replace />
  const submit = async (values: LoginValues) => {
    setLoading(true)
    try {
      const session = await login(values.phone, values.password)
      if (!['ADMIN', 'SUPER_ADMIN'].includes(session.role)) throw new Error('该账号不是管理员')
      localStorage.setItem('admin_token', session.token)
      localStorage.setItem('admin_user', JSON.stringify(session))
      navigate('/admin/dashboard')
    } catch (error) {
      if (error instanceof Error) message.error(error.message)
    } finally { setLoading(false) }
  }
  return <div className="admin-login-page">
    <section className="admin-login-copy"><div className="admin-login-mark">循</div><h1>让校园交易<br />始终可信。</h1><p>商品审核 · 违规处置 · 平台数据</p></section>
    <section className="admin-login-card">
      <div className="eyebrow">ADMINISTRATOR ACCESS</div>
      <Typography.Title level={2}>管理后台登录</Typography.Title>
      <Typography.Paragraph type="secondary">仅限平台运营管理员访问</Typography.Paragraph>
      <Form<LoginValues> layout="vertical" onFinish={submit} requiredMark={false}>
        <Form.Item label="管理员手机号" name="phone" rules={[{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确手机号' }]}><Input size="large" maxLength={11} /></Form.Item>
        <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}><Input.Password size="large" /></Form.Item>
        <Button type="primary" htmlType="submit" size="large" block loading={loading}>进入管理后台</Button>
      </Form>
    </section>
  </div>
}
