import { Button, Form, Input, Typography, message } from 'antd'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { login } from '../api/user'
import { useUserStore } from '../store/userStore'

interface LoginValues {
  phone: string
  password: string
}

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const token = useUserStore((state) => state.token)
  const setSession = useUserStore((state) => state.setSession)

  if (token) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = async (values: LoginValues) => {
    try {
      const session = await login(values.phone, values.password)
      setSession(session)
      message.success('欢迎回来')
      const destination = (location.state as { from?: string } | null)?.from ?? '/'
      navigate(destination, { replace: true })
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败')
    }
  }

  return (
    <div className="auth-page">
      <section className="auth-story">
        <Link className="brand brand-light" to="/">
          <span className="brand-mark">循</span><span>校园循物</span>
        </Link>
        <div>
          <div className="eyebrow light">TRUSTED CAMPUS ONLY</div>
          <h1>一件闲置，<br />一次新的相遇。</h1>
          <p>只面向本校师生的可信交易社区。</p>
        </div>
        <small>手机号 + 学号双重认证 · 校内当面交易</small>
      </section>
      <section className="auth-panel">
        <div className="auth-card">
          <Typography.Title level={2}>欢迎回来</Typography.Title>
          <Typography.Paragraph type="secondary">登录后继续发现身边的校园好物</Typography.Paragraph>
          <Form<LoginValues> layout="vertical" size="large" onFinish={handleSubmit} requiredMark={false}>
            <Form.Item label="手机号" name="phone" rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号' },
            ]}>
              <Input inputMode="tel" maxLength={11} placeholder="请输入11位手机号" />
            </Form.Item>
            <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password placeholder="请输入密码" />
            </Form.Item>
            <Button type="primary" htmlType="submit" size="large" block>登录</Button>
          </Form>
          <div className="auth-switch">还没有账号？<Link to="/register">立即注册</Link></div>
        </div>
      </section>
    </div>
  )
}
