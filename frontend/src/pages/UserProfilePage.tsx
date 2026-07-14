import { useEffect, useState } from 'react'
import { Avatar, Button, Card, Descriptions, Divider, Form, Input, Space, Spin, Tag, Typography, Upload, message, type UploadProps } from 'antd'
import { authenticateStudent, getProfile, updateProfile, uploadAvatar, type UserProfile } from '../api/user'
import { useUserStore } from '../store/userStore'

interface ProfileValues {
  nickname: string
  avatar?: string
  contactPhone?: string
}

interface StudentValues {
  studentId: string
  realName: string
}

export default function UserProfilePage() {
  const [form] = Form.useForm<ProfileValues>()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const setStoredUser = useUserStore((state) => state.setUser)

  useEffect(() => {
    getProfile()
      .then((data) => {
        setProfile(data)
        setStoredUser(data)
        form.setFieldsValue({ nickname: data.nickname, avatar: data.avatar, contactPhone: data.contactPhone })
      })
      .catch((error: Error) => message.error(error.message))
      .finally(() => setLoading(false))
  }, [form, setStoredUser])

  const handleSave = async (values: ProfileValues) => {
    try {
      setSaving(true)
      const updated = await updateProfile(values)
      setProfile(updated)
      setStoredUser(updated)
      message.success('个人资料已保存')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const handleAvatarUpload: UploadProps['beforeUpload'] = async (file) => {
    try {
      const result = await uploadAvatar(file as File)
      form.setFieldValue('avatar', result.url)
      message.success('头像上传成功，保存资料后生效')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '头像上传失败')
    }
    return false
  }

  const handleStudentAuth = async (values: StudentValues) => {
    try {
      await authenticateStudent(values.studentId, values.realName)
      const updated = await getProfile()
      setProfile(updated)
      setStoredUser(updated)
      message.success('校园身份认证成功')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '认证失败')
    }
  }

  if (loading || !profile) {
    return <div className="page-loading"><Spin size="large" /></div>
  }

  return (
    <div className="page-wrap">
      <div className="page-heading">
        <div><div className="eyebrow">MY CAMPUS ID</div><Typography.Title level={2}>个人中心</Typography.Title></div>
        <Tag color={profile.studentVerified ? 'green' : 'gold'}>{profile.studentVerified ? '校园身份已认证' : '待完成校园认证'}</Tag>
      </div>
      <div className="profile-grid">
        <Card className="profile-summary">
          <Avatar size={92} src={profile.avatar}>{profile.nickname.slice(0, 1)}</Avatar>
          <Typography.Title level={3}>{profile.nickname}</Typography.Title>
          <Typography.Text type="secondary">{profile.phone}</Typography.Text>
          <Divider />
          <Descriptions column={1} size="small" items={[
            { key: 'role', label: '账号角色', children: profile.role },
            { key: 'student', label: '学号', children: profile.studentId ?? '尚未认证' },
            { key: 'name', label: '真实姓名', children: profile.realName ?? '尚未认证' },
            { key: 'status', label: '账号状态', children: profile.status },
          ]} />
        </Card>
        <Space direction="vertical" size="large" className="profile-forms">
          <Card title="编辑公开资料">
            <Form<ProfileValues> form={form} layout="vertical" onFinish={handleSave} requiredMark={false}>
              <Form.Item label="头像" name="avatar">
                <Input placeholder="头像地址" addonAfter={<Upload showUploadList={false} accept="image/png,image/jpeg,image/webp" beforeUpload={handleAvatarUpload}><Button type="link" size="small">上传</Button></Upload>} />
              </Form.Item>
              <Form.Item label="昵称" name="nickname" rules={[{ required: true, message: '请输入昵称' }, { max: 50 }]}>
                <Input maxLength={50} />
              </Form.Item>
              <Form.Item label="联系方式" name="contactPhone" rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}>
                <Input maxLength={11} placeholder="供交易双方联系，可选" />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={saving}>保存资料</Button>
            </Form>
          </Card>
          {!profile.studentVerified && (
            <Card title="完成校园实名认证" className="verification-card">
              <Typography.Paragraph type="secondary">认证后才可发布商品、下单和发起交易。</Typography.Paragraph>
              <Form<StudentValues> layout="inline" onFinish={handleStudentAuth}>
                <Form.Item name="studentId" rules={[{ required: true, message: '请输入学号' }, { pattern: /^[A-Za-z0-9]{6,20}$/, message: '学号格式不正确' }]}>
                  <Input placeholder="学号" />
                </Form.Item>
                <Form.Item name="realName" rules={[{ required: true, message: '请输入姓名' }]}>
                  <Input placeholder="真实姓名" />
                </Form.Item>
                <Button type="primary" htmlType="submit">提交认证</Button>
              </Form>
            </Card>
          )}
        </Space>
      </div>
    </div>
  )
}
