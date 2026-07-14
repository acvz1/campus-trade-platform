import { Spin, Skeleton, Card, Row, Col } from 'antd'

interface LoadingProps {
  spinning?: boolean
  size?: 'small' | 'default' | 'large'
  tip?: string
  type?: 'spin' | 'skeleton' | 'card'
  rows?: number
  cardCount?: number
}

const Loading = ({ 
  spinning = true, 
  size = 'large', 
  tip = '加载中...',
  type = 'spin',
  rows = 3,
  cardCount = 4,
}: LoadingProps) => {
  // 卡片骨架：适合商品列表
  if (type === 'card') {
    return (
      <Row gutter={[16, 16]}>
        {Array.from({ length: cardCount }).map((_, i) => (
          <Col key={i} xs={12} sm={8} md={6} lg={4}>
            <Card>
              <Skeleton.Image active style={{ width: '100%', height: 180 }} />
              <Skeleton active paragraph={{ rows: 2 }} style={{ marginTop: 12 }} />
            </Card>
          </Col>
        ))}
      </Row>
    )
  }

  // 文本骨架：适合文章/详情页
  if (type === 'skeleton') {
    return <Skeleton active paragraph={{ rows }} />
  }

  // 默认：转圈 Spin
  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      padding: '40px 0',
      minHeight: 200 
    }}>
      <Spin size={size} tip={tip} spinning={spinning} />
    </div>
  )
}

export default Loading