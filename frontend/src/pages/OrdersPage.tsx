import { useEffect, useState } from 'react'
import { Button, Empty, Pagination, Segmented, Spin, Tabs, Tag, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { listOrders, type Order, type OrderStatus } from '../api/order'
import type { PageResult } from '../api/product'

const statusTabs: Array<{ key: string; label: string }> = [
  { key: '', label: '全部' }, { key: 'PENDING_COMMUNICATION', label: '待沟通' },
  { key: 'PENDING_PICKUP', label: '待自提' }, { key: 'COMPLETED', label: '已完成' }, { key: 'CANCELLED', label: '已取消' },
]
const statusLabels: Record<OrderStatus, string> = { PENDING_COMMUNICATION: '待沟通', PENDING_PICKUP: '待自提', COMPLETED: '已完成', CANCELLED: '已取消' }
const statusColors: Record<OrderStatus, string> = { PENDING_COMMUNICATION: 'gold', PENDING_PICKUP: 'blue', COMPLETED: 'green', CANCELLED: 'default' }

export default function OrdersPage() {
  const navigate = useNavigate()
  const [role, setRole] = useState<'buyer' | 'seller'>('buyer')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [result, setResult] = useState<PageResult<Order>>({ records: [], total: 0, page: 1, size: 10, pages: 0 })

  useEffect(() => {
    let active = true
    listOrders({ role, status: statusFilter as OrderStatus || undefined, page, size: 10 })
      .then((data) => { if (active) setResult(data) })
      .catch((error: Error) => { if (active) message.error(error.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [page, role, statusFilter])

  const changeScope = (nextRole: 'buyer' | 'seller') => { setLoading(true); setRole(nextRole); setPage(1) }

  return (
    <div className="page-wrap orders-page">
      <div className="page-heading"><div><div className="eyebrow">MY TRADES</div><Typography.Title level={2}>我的订单</Typography.Title></div><Segmented value={role} options={[{ label: '我买到的', value: 'buyer' }, { label: '我卖出的', value: 'seller' }]} onChange={(value) => changeScope(value as 'buyer' | 'seller')} /></div>
      <Tabs activeKey={statusFilter} items={statusTabs} onChange={(value) => { setLoading(true); setStatusFilter(value); setPage(1) }} />
      {loading ? <div className="page-loading compact"><Spin size="large" /></div> : result.records.length ? <>
        <div className="order-list">{result.records.map((order) => (
          <article key={order.id} className="order-card">
            <div className="order-card-head"><span>订单号 {order.orderNo}</span><Tag color={statusColors[order.status]}>{statusLabels[order.status]}</Tag></div>
            <div className="order-card-body">
              <button type="button" className="order-product" onClick={() => navigate(`/order/${order.id}`)}>
                {order.productImage ? <img src={order.productImage} alt="" /> : <span className="order-image-placeholder" />}
                <div><strong>{order.productTitle}</strong><small>{role === 'buyer' ? `卖家：${order.seller.nickname}` : `买家：${order.buyer.nickname}`}</small></div>
              </button>
              <div className="order-price"><span>成交价</span><strong>¥{Number(order.price).toFixed(2)}</strong></div>
              <Button onClick={() => navigate(`/order/${order.id}`)}>查看详情</Button>
            </div>
          </article>
        ))}</div>
        <Pagination current={page} pageSize={10} total={result.total} hideOnSinglePage onChange={(value) => { setLoading(true); setPage(value) }} className="market-pagination" />
      </> : <Empty description="当前没有订单" />}
    </div>
  )
}
