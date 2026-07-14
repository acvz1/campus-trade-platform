import { useEffect, useState } from 'react'
import { Avatar, Button, DatePicker, Descriptions, Form, Input, Modal, Spin, Steps, Tag, Typography, message } from 'antd'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { cancelOrder, getOrder, updateOrderStatus, type Order } from '../api/order'
import { useUserStore } from '../store/userStore'

interface PickupValues { pickupTime: { format: (pattern: string) => string }; pickupLocation: string }
interface CancelValues { cancelReason?: string }

const statusLabels = { PENDING_COMMUNICATION: '待沟通', PENDING_PICKUP: '待自提', COMPLETED: '已完成', CANCELLED: '已取消' }

export default function OrderDetailPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const storedUser = useUserStore((state) => state.user)
  const currentUserId = storedUser && 'id' in storedUser ? storedUser.id : storedUser?.userId
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [pickupOpen, setPickupOpen] = useState(false)
  const [cancelOpen, setCancelOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [pickupForm] = Form.useForm<PickupValues>()
  const [cancelForm] = Form.useForm<CancelValues>()

  const reload = () => getOrder(id).then(setOrder)

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) return
    let active = true
    getOrder(id).then((data) => { if (active) setOrder(data) })
      .catch((error: Error) => { if (active) message.error(error.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])

  if (!Number.isInteger(id) || id <= 0) return <Navigate to="/orders" replace />
  if (loading || !order) return <div className="page-loading"><Spin size="large" /></div>

  const submitPickup = async () => {
    try {
      const values = await pickupForm.validateFields()
      setSubmitting(true)
      await updateOrderStatus(id, { action: 'CONFIRM_PICKUP', pickupTime: values.pickupTime.format('YYYY-MM-DD HH:mm'), pickupLocation: values.pickupLocation })
      setPickupOpen(false); await reload(); message.success('已进入待自提状态')
    } catch (error) { if (error instanceof Error) message.error(error.message) }
    finally { setSubmitting(false) }
  }

  const complete = async () => {
    try { setSubmitting(true); await updateOrderStatus(id, { action: 'COMPLETE' }); await reload(); message.success('交易已完成') }
    catch (error) { message.error(error instanceof Error ? error.message : '操作失败') }
    finally { setSubmitting(false) }
  }

  const submitCancel = async () => {
    try { const values = await cancelForm.validateFields(); setSubmitting(true); await cancelOrder(id, values.cancelReason); setCancelOpen(false); await reload(); message.success('订单已取消') }
    catch (error) { if (error instanceof Error) message.error(error.message) }
    finally { setSubmitting(false) }
  }

  const currentStep = order.status === 'PENDING_COMMUNICATION' ? 0 : order.status === 'PENDING_PICKUP' ? 1 : 2
  const parties = [{ label: '买家', party: order.buyer }, { label: '卖家', party: order.seller }]
  return (
    <div className="page-wrap order-detail-page">
      <Button type="link" onClick={() => navigate('/orders')}>← 返回订单列表</Button>
      <div className="order-detail-head"><div><div className="eyebrow">ORDER {order.orderNo}</div><Typography.Title level={2}>{statusLabels[order.status]}</Typography.Title></div><Tag>{order.status}</Tag></div>
      {order.status !== 'CANCELLED' ? <Steps current={currentStep} items={[{ title: '待沟通', description: '确认时间地点' }, { title: '待自提', description: order.pickupLocation ?? '等待履约' }, { title: '已完成', description: '交易完成' }]} className="order-steps" />
        : <div className="cancel-banner"><strong>订单已取消</strong><span>{order.cancelReason}</span></div>}
      <div className="order-detail-grid">
        <section className="order-detail-card">
          <Typography.Title level={4}>商品信息</Typography.Title>
          <div className="detail-order-product">{order.productImage && <img src={order.productImage} alt="" />}<div><strong>{order.productTitle}</strong><span>¥{Number(order.price).toFixed(2)}</span></div></div>
          <Descriptions column={1} items={[
            { key: 'trade', label: '交易方式', children: order.tradeType },
            { key: 'remark', label: '买家备注', children: order.buyerRemark ?? '无' },
            { key: 'pickup', label: '约定地点', children: order.pickupLocation ?? '尚未约定' },
            { key: 'time', label: '约定时间', children: order.pickupTime ? new Date(order.pickupTime).toLocaleString() : '尚未约定' },
            { key: 'created', label: '下单时间', children: new Date(order.createdAt).toLocaleString() },
          ]} />
        </section>
        <section className="order-detail-card">
          <Typography.Title level={4}>交易双方</Typography.Title>
          {parties.map(({ label, party }) => <div className="order-party" key={label}><Avatar src={party.avatar}>{party.nickname.slice(0, 1)}</Avatar><div><small>{label}</small><strong>{party.nickname}</strong><span>{party.phone}</span></div></div>)}
          {order.address && <><Typography.Title level={5}>收货地址</Typography.Title><Typography.Paragraph>{order.address.contact} · {order.address.phone}<br />{order.address.campus} {order.address.building} {order.address.room}</Typography.Paragraph></>}
        </section>
      </div>
      {['PENDING_COMMUNICATION', 'PENDING_PICKUP'].includes(order.status) && <div className="order-action-bar">
        {order.status === 'PENDING_COMMUNICATION' && <Button type="primary" size="large" onClick={() => setPickupOpen(true)}>确认进入待自提</Button>}
        {order.status === 'PENDING_PICKUP' && currentUserId === order.buyer.id && <Button type="primary" size="large" loading={submitting} onClick={complete}>确认交易完成</Button>}
        <Button danger size="large" onClick={() => setCancelOpen(true)}>取消订单</Button>
      </div>}
      <Modal title="约定自提时间与地点" open={pickupOpen} onCancel={() => setPickupOpen(false)} onOk={submitPickup} confirmLoading={submitting} okText="确认" cancelText="取消" destroyOnHidden>
        <Form<PickupValues> form={pickupForm} layout="vertical"><Form.Item label="约定时间" name="pickupTime" rules={[{ required: true, message: '请选择时间' }]}><DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} /></Form.Item><Form.Item label="约定地点" name="pickupLocation" rules={[{ required: true, message: '请输入地点' }, { max: 255 }]}><Input placeholder="如：北校区图书馆门口" /></Form.Item></Form>
      </Modal>
      <Modal title="取消订单" open={cancelOpen} onCancel={() => setCancelOpen(false)} onOk={submitCancel} confirmLoading={submitting} okText="确认取消" okButtonProps={{ danger: true }} cancelText="返回" destroyOnHidden>
        <Form<CancelValues> form={cancelForm} layout="vertical"><Form.Item label="取消原因" name="cancelReason" rules={[{ max: 255 }]}><Input.TextArea rows={3} placeholder="请简单说明取消原因" /></Form.Item></Form>
      </Modal>
    </div>
  )
}
