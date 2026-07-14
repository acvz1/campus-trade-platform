import { useEffect, useState } from 'react'
import { Avatar, Button, Carousel, Descriptions, Image, Space, Spin, Tag, Typography, message } from 'antd'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { getProduct, type Product } from '../api/product'
import { useUserStore } from '../store/userStore'
import FavoriteButton from '../components/FavoriteButton'
import OrderConfirmModal from '../components/OrderConfirmModal'
import { createConversation } from '../api/chat'

const conditionLabels = { NEW: '全新', LIKE_NEW: '九成新', USED: '正常使用', OLD: '岁月痕迹' }
const tradeLabels = { PICKUP: '校内自提', DELIVERY: '送货', BOTH: '自提或送货' }
const statusLabels: Record<string, string> = { PENDING_REVIEW: '审核中', ON_SALE: '在售', REJECTED: '审核未通过', OFF_SHELF: '已下架', SOLD: '已售出' }

export default function ProductDetailPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const token = useUserStore((state) => state.token)
  const storedUser = useUserStore((state) => state.user)
  const [product, setProduct] = useState<Product | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [orderOpen, setOrderOpen] = useState(false)
  const [contacting, setContacting] = useState(false)

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) return
    let active = true
    getProduct(id).then((data) => { if (active) setProduct(data) })
      .catch((error: Error) => { if (active) { setNotFound(true); message.error(error.message) } })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])

  if (!Number.isInteger(id) || id <= 0 || notFound) return <Navigate to="/" replace />
  if (loading || !product) return <div className="page-loading"><Spin size="large" /></div>

  const requireLogin = (next: () => void) => token ? next() : navigate('/login', { state: { from: `/product/${id}` } })
  const currentUserId = storedUser && 'id' in storedUser ? storedUser.id : storedUser?.userId
  const isOwnProduct = currentUserId === product.seller.id

  const contactSeller = async () => {
    setContacting(true)
    try {
      const conversation = await createConversation(product.id)
      navigate(`/chat/${conversation.id}`)
    } catch (error) {
      if (error instanceof Error) message.error(error.message)
    } finally {
      setContacting(false)
    }
  }

  return (
    <div className="page-wrap product-detail-page">
      <div className="product-detail-grid">
        <div className="product-gallery">
          <Image.PreviewGroup>
            <Carousel arrows dots={{ className: 'gallery-dots' }}>
              {product.images.map((image) => <div key={image.id ?? image.url}><div className="gallery-slide"><Image src={image.url} alt={product.title} /></div></div>)}
            </Carousel>
          </Image.PreviewGroup>
        </div>
        <aside className="product-buy-panel">
          <Space><Tag color="green">{product.categoryName}</Tag><Tag>{statusLabels[product.status] ?? product.status}</Tag></Space>
          <Typography.Title level={1}>{product.title}</Typography.Title>
          <div className="detail-price"><strong>¥{Number(product.price).toFixed(2)}</strong>{product.originalPrice && <del>¥{Number(product.originalPrice).toFixed(2)}</del>}</div>
          <Descriptions column={2} size="small" items={[
            { key: 'condition', label: '成色', children: conditionLabels[product.conditionLevel] },
            { key: 'trade', label: '交易', children: tradeLabels[product.tradeType] },
            { key: 'views', label: '浏览', children: `${product.viewCount} 次` },
            { key: 'time', label: '发布', children: new Date(product.createdAt).toLocaleDateString() },
          ]} />
          {product.tradeRemark && <div className="trade-note">交易说明：{product.tradeRemark}</div>}
          <div className="seller-panel">
            <Avatar size={48} src={product.seller.avatar}>{product.seller.nickname.slice(0, 1)}</Avatar>
            <div><strong>{product.seller.nickname}</strong><span>已完成 {product.seller.soldCount} 笔交易</span></div>
          </div>
          <Space direction="vertical" className="buy-actions">
            <Button type="primary" size="large" block disabled={product.status !== 'ON_SALE' || isOwnProduct} onClick={() => requireLogin(() => setOrderOpen(true))}>{isOwnProduct ? '这是你发布的商品' : '立即下单'}</Button>
            <Space.Compact block>
              <Button size="large" block loading={contacting} disabled={isOwnProduct} onClick={() => requireLogin(contactSeller)}>联系卖家</Button>
              <FavoriteButton productId={product.id} disabled={product.status !== 'ON_SALE' || isOwnProduct} />
            </Space.Compact>
          </Space>
        </aside>
      </div>
      <section className="product-description">
        <div className="eyebrow">ITEM STORY</div><Typography.Title level={2}>商品详情</Typography.Title>
        <Typography.Paragraph>{product.description}</Typography.Paragraph>
      </section>
      <OrderConfirmModal product={product} open={orderOpen} onClose={() => setOrderOpen(false)} />
    </div>
  )
}
