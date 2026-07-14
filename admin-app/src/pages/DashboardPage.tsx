import { Card, Col, Row, Skeleton, Statistic, Typography, message } from 'antd'
import { useEffect, useState } from 'react'
import { getDashboard, type Dashboard } from '../api/admin'

export default function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null)
  useEffect(() => { getDashboard().then(setData).catch((error: Error) => message.error(error.message)) }, [])
  if (!data) return <Skeleton active paragraph={{ rows: 8 }} />
  const totals = [
    ['平台用户', data.totalUsers], ['全部商品', data.totalProducts], ['全部订单', data.totalOrders],
    ['已完成订单', data.completedOrders], ['待审核商品', data.pendingReviewProducts],
  ] as const
  return <div>
    <div className="page-title"><div><div className="eyebrow">OVERVIEW</div><Typography.Title level={2}>数据看板</Typography.Title></div><span>{new Date().toLocaleDateString()} 平台概览</span></div>
    <Row gutter={[18, 18]}>{totals.map(([label, value], index) => <Col xs={24} sm={12} xl={index < 4 ? 6 : 8} key={label}><Card className="stat-card"><Statistic title={label} value={value} /><span className="stat-index">0{index + 1}</span></Card></Col>)}</Row>
    <Card className="today-card" title="今日数据明细">
      <Row gutter={[24, 16]}>
        <Col xs={24} md={8}><Statistic title="今日新增用户" value={data.todayNewUsers} suffix="人" /></Col>
        <Col xs={24} md={8}><Statistic title="今日新增商品" value={data.todayNewProducts} suffix="件" /></Col>
        <Col xs={24} md={8}><Statistic title="今日新增订单" value={data.todayNewOrders} suffix="笔" /></Col>
      </Row>
    </Card>
  </div>
}
