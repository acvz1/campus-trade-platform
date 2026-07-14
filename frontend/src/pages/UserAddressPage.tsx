import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Checkbox, Empty, Form, Input, List, Modal, Popconfirm, Space, Tag, Typography, message } from 'antd'
import { createAddress, deleteAddress, listAddresses, setDefaultAddress, updateAddress, type Address, type AddressPayload } from '../api/user'

export default function UserAddressPage() {
  const [form] = Form.useForm<AddressPayload>()
  const [addresses, setAddresses] = useState<Address[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Address | null>(null)

  const refresh = useCallback(async () => {
    try {
      setAddresses(await listAddresses())
    } catch (error) {
      message.error(error instanceof Error ? error.message : '地址加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    listAddresses()
      .then((data) => { if (active) setAddresses(data) })
      .catch((error: Error) => { if (active) message.error(error.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ isDefault: addresses.length === 0 })
    setOpen(true)
  }

  const openEdit = (address: Address) => {
    setEditing(address)
    form.setFieldsValue({
      contactName: address.contact,
      contactPhone: address.phone,
      campus: address.campus,
      building: address.building,
      room: address.room,
      detail: address.detail,
      isDefault: address.isDefault,
    })
    setOpen(true)
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      if (editing) await updateAddress(editing.id, values)
      else await createAddress(values)
      message.success(editing ? '地址已更新' : '地址已添加')
      setOpen(false)
      await refresh()
    } catch (error) {
      if (error instanceof Error) message.error(error.message)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteAddress(id)
      message.success('地址已删除')
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : '删除失败')
    }
  }

  const handleSetDefault = async (id: number) => {
    try {
      await setDefaultAddress(id)
      message.success('默认地址已更新')
      await refresh()
    } catch (error) {
      message.error(error instanceof Error ? error.message : '设置失败')
    }
  }

  return (
    <div className="page-wrap">
      <div className="page-heading">
        <div><div className="eyebrow">CAMPUS ADDRESS BOOK</div><Typography.Title level={2}>收货地址</Typography.Title><Typography.Text type="secondary">最多保存 10 个校内地址</Typography.Text></div>
        <Button type="primary" size="large" onClick={openCreate} disabled={addresses.length >= 10}>新增地址</Button>
      </div>
      <List loading={loading} grid={{ gutter: 20, xs: 1, md: 2 }} dataSource={addresses}
        locale={{ emptyText: <Empty description="还没有收货地址" image={Empty.PRESENTED_IMAGE_SIMPLE}><Button type="primary" onClick={openCreate}>添加第一个地址</Button></Empty> }}
        renderItem={(address) => (
          <List.Item>
            <Card className={`address-card ${address.isDefault ? 'default' : ''}`} actions={[
              <Button type="link" onClick={() => openEdit(address)}>编辑</Button>,
              <Popconfirm title="确认删除这个地址？" onConfirm={() => handleDelete(address.id)} okText="删除" cancelText="取消"><Button type="link" danger>删除</Button></Popconfirm>,
              address.isDefault ? <Typography.Text type="secondary">当前默认</Typography.Text> : <Button type="link" onClick={() => handleSetDefault(address.id)}>设为默认</Button>,
            ]}>
              <Space direction="vertical" size="middle" className="address-content">
                <div className="address-title"><strong>{address.contact}</strong><span>{address.phone}</span>{address.isDefault && <Tag color="green">默认</Tag>}</div>
                <Typography.Paragraph>{address.campus} · {address.building}{address.room ? ` · ${address.room}` : ''}</Typography.Paragraph>
                {address.detail && <Typography.Text type="secondary">{address.detail}</Typography.Text>}
              </Space>
            </Card>
          </List.Item>
        )} />
      <Modal title={editing ? '编辑收货地址' : '新增收货地址'} open={open} onCancel={() => setOpen(false)} onOk={handleSave} confirmLoading={saving} okText="保存" cancelText="取消" destroyOnHidden>
        <Form<AddressPayload> form={form} layout="vertical" requiredMark={false} className="address-form">
          <div className="two-column">
            <Form.Item label="联系人" name="contactName" rules={[{ required: true, message: '请输入联系人' }]}><Input maxLength={50} /></Form.Item>
            <Form.Item label="联系电话" name="contactPhone" rules={[{ required: true, message: '请输入联系电话' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}><Input maxLength={11} /></Form.Item>
          </div>
          <div className="two-column">
            <Form.Item label="校区" name="campus" rules={[{ required: true, message: '请输入校区' }]}><Input placeholder="如：北校区" /></Form.Item>
            <Form.Item label="楼栋" name="building" rules={[{ required: true, message: '请输入楼栋' }]}><Input placeholder="如：12号宿舍楼" /></Form.Item>
          </div>
          <Form.Item label="宿舍/房间号" name="room"><Input placeholder="如：301室" /></Form.Item>
          <Form.Item label="补充说明" name="detail"><Input.TextArea rows={2} maxLength={255} showCount placeholder="可填写取件点、门牌等信息" /></Form.Item>
          <Form.Item name="isDefault" valuePropName="checked"><Checkbox>设为默认地址</Checkbox></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
