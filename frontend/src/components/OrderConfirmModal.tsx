import { useEffect, useState } from 'react'
import { Alert, Button, Form, Input, Modal, Select, Space, Typography, message } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import { createOrder } from '../api/order'
import type { Product } from '../api/product'
import { listAddresses, type Address } from '../api/user'

interface OrderValues { addressId?: number; remark?: string }

export default function OrderConfirmModal({ product, open, onClose }: { product: Product; open: boolean; onClose: () => void }) {
  const navigate = useNavigate()
  const [form] = Form.useForm<OrderValues>()
  const [addresses, setAddresses] = useState<Address[]>([])
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!open) return
    form.resetFields()
    let active = true
    listAddresses().then((data) => {
      if (!active) return
      setAddresses(data)
      const defaultAddress = data.find((item) => item.isDefault)
      if (defaultAddress) form.setFieldValue('addressId', defaultAddress.id)
    }).catch((error: Error) => { if (active) message.error(error.message) })
    return () => { active = false }
  }, [form, open])

  const submit = async () => {
    try {
      const values = await form.validateFields()
      setSubmitting(true)
      const order = await createOrder({ productId: product.id, ...values })
      message.success('下单成功，请与卖家确认交易时间')
      onClose()
      navigate(`/order/${order.id}`)
    } catch (error) {
      if (error instanceof Error) message.error(error.message)
    } finally {
      setSubmitting(false)
    }
  }

  const requiresAddress = product.tradeType === 'DELIVERY'
  return (
    <Modal title="确认下单" open={open} onCancel={onClose} onOk={submit} confirmLoading={submitting} okText="确认下单" cancelText="再想想" destroyOnHidden>
      <div className="order-confirm-product">
        {product.mainImage && <img src={product.mainImage} alt="" />}
        <div><Typography.Text strong>{product.title}</Typography.Text><strong>¥{Number(product.price).toFixed(2)}</strong></div>
      </div>
      <Alert type="info" showIcon message="订单创建后，商品将为你保留，请及时与卖家沟通。" />
      <Form<OrderValues> form={form} layout="vertical" className="order-confirm-form">
        {(requiresAddress || product.tradeType === 'BOTH') && <Form.Item label="收货地址" name="addressId" rules={requiresAddress ? [{ required: true, message: '送货交易必须选择地址' }] : []}>
          <Select placeholder={addresses.length ? '请选择收货地址' : '暂无地址'} options={addresses.map((item) => ({ value: item.id, label: `${item.contact} · ${item.campus} ${item.building} ${item.room ?? ''}` }))} />
        </Form.Item>}
        {requiresAddress && addresses.length === 0 && <Space><Typography.Text type="secondary">尚未添加地址</Typography.Text><Link to="/user/address">去添加</Link></Space>}
        <Form.Item label="给卖家的备注" name="remark" rules={[{ max: 200, message: '备注不能超过200字' }]}><Input.TextArea rows={3} maxLength={200} showCount placeholder="如：可以周末取货吗？" /></Form.Item>
      </Form>
      <Button type="link" onClick={() => navigate(`/product/${product.id}`)}>查看商品详情</Button>
    </Modal>
  )
}
