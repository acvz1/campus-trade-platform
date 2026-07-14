import { useEffect, useState } from 'react'
import { Button, Form, Input, Result, Space, Steps, Typography, message } from 'antd'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { register, sendSmsCode, type RegisterPayload } from '../api/user'
import { useUserStore } from '../store/userStore'

interface RegisterValues extends RegisterPayload {
  confirmPassword: string
}

export default function RegisterPage() {
  const navigate = useNavigate()
  const token = useUserStore((state) => state.token)
  const [form] = Form.useForm<RegisterValues>()
  const [step, setStep] = useState(0)
  const [countdown, setCountdown] = useState(0)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (countdown <= 0) return
    const timer = window.setTimeout(() => setCountdown((value) => value - 1), 1000)
    return () => window.clearTimeout(timer)
  }, [countdown])

  if (token) {
    return <Navigate to="/" replace />
  }

  const handleSendCode = async () => {
    try {
      const phone = form.getFieldValue('phone') as string
      await form.validateFields(['phone'])
      await sendSmsCode(phone, 'REGISTER')
      setCountdown(60)
      message.success('验证码已发送，MVP 验证码为 888888')
    } catch (error) {
      if (error instanceof Error) message.error(error.message)
    }
  }

  const nextStep = async () => {
    await form.validateFields(['phone', 'smsCode'])
    setStep(1)
  }

  const handleRegister = async () => {
    try {
      const values = await form.validateFields()
      setSubmitting(true)
      await register({
        phone: values.phone,
        smsCode: values.smsCode,
        password: values.password,
        nickname: values.nickname,
        studentId: values.studentId,
        realName: values.realName,
      })
      setStep(2)
    } catch (error) {
      if (error instanceof Error) message.error(error.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="register-page">
      <header className="register-header">
        <Link className="brand" to="/"><span className="brand-mark">循</span><span>校园循物</span></Link>
        <span>已有账号？ <Link to="/login">直接登录</Link></span>
      </header>
      <main className="register-card">
        <div className="eyebrow">JOIN THE COMMUNITY</div>
        <Typography.Title level={2}>创建校园账号</Typography.Title>
        <Steps current={step} items={[{ title: '验证手机' }, { title: '校园信息' }, { title: '注册成功' }]} />
        {step < 2 ? (
          <Form<RegisterValues> form={form} layout="vertical" size="large" requiredMark={false} className="register-form">
            {step === 0 && (
              <>
                <Form.Item label="手机号" name="phone" rules={[
                  { required: true, message: '请输入手机号' },
                  { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
                ]}>
                  <Input maxLength={11} inputMode="tel" placeholder="仅用于登录和身份验证" />
                </Form.Item>
                <Form.Item label="短信验证码" required>
                  <Space.Compact block>
                    <Form.Item name="smsCode" noStyle rules={[
                      { required: true, message: '请输入验证码' },
                      { pattern: /^\d{6}$/, message: '验证码为6位数字' },
                    ]}>
                      <Input maxLength={6} placeholder="请输入 888888" />
                    </Form.Item>
                    <Button disabled={countdown > 0} onClick={handleSendCode}>
                      {countdown > 0 ? `${countdown}s` : '获取验证码'}
                    </Button>
                  </Space.Compact>
                </Form.Item>
                <Button type="primary" block onClick={nextStep}>下一步</Button>
              </>
            )}
            {step === 1 && (
              <>
                <div className="form-hint">学号和姓名用于确认本校身份；完成后即可发布与交易。</div>
                <Form.Item label="昵称" name="nickname" rules={[{ max: 50, message: '昵称不能超过50个字符' }]}>
                  <Input placeholder="你希望大家怎么称呼你" />
                </Form.Item>
                <div className="two-column">
                  <Form.Item label="学号" name="studentId" rules={[
                    { required: true, message: '请输入学号' },
                    { pattern: /^[A-Za-z0-9]{6,20}$/, message: '学号应为6-20位字母或数字' },
                  ]}><Input placeholder="请输入学号" /></Form.Item>
                  <Form.Item label="真实姓名" name="realName" rules={[{ required: true, message: '请输入真实姓名' }]}>
                    <Input placeholder="仅用于校园认证" />
                  </Form.Item>
                </div>
                <Form.Item label="设置密码" name="password" rules={[
                  { required: true, message: '请设置密码' },
                  { min: 6, max: 20, message: '密码长度为6-20位' },
                ]}><Input.Password placeholder="6-20位密码" /></Form.Item>
                <Form.Item label="确认密码" name="confirmPassword" dependencies={['password']} rules={[
                  { required: true, message: '请再次输入密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      return !value || getFieldValue('password') === value
                        ? Promise.resolve() : Promise.reject(new Error('两次输入的密码不一致'))
                    },
                  }),
                ]}><Input.Password placeholder="再次输入密码" /></Form.Item>
                <Space className="form-actions">
                  <Button onClick={() => setStep(0)}>上一步</Button>
                  <Button type="primary" loading={submitting} onClick={handleRegister}>完成注册</Button>
                </Space>
              </>
            )}
          </Form>
        ) : (
          <Result status="success" title="欢迎加入校园循物" subTitle="账号与校园身份已验证，可以使用手机号和密码登录。"
            extra={<Button type="primary" onClick={() => navigate('/login')}>去登录</Button>} />
        )}
      </main>
    </div>
  )
}
