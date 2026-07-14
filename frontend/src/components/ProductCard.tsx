import { Card, Tag, Typography } from 'antd'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Product } from '../api/product'

const conditionLabels: Record<Product['conditionLevel'], string> = {
  NEW: '全新', LIKE_NEW: '九成新', USED: '正常使用', OLD: '岁月痕迹',
}

export default function ProductCard({ product, footer, onClick }: { product: Product; footer?: ReactNode; onClick?: (product: Product) => void }) {
  const navigate = useNavigate()
  const open = () => onClick ? onClick(product) : navigate(`/product/${product.id}`)
  return (
    <Card hoverable className="product-card" cover={
      <button className="product-cover" type="button" onClick={open}>
        {product.mainImage ? <img src={product.mainImage} alt={product.title} /> : <span>暂无图片</span>}
        <Tag>{conditionLabels[product.conditionLevel]}</Tag>
      </button>
    } actions={footer ? [footer] : undefined}>
      <button type="button" className="product-card-body" onClick={open}>
        <Typography.Title level={4} ellipsis={{ rows: 2 }}>{product.title}</Typography.Title>
        <div className="product-meta"><strong>¥{Number(product.price).toFixed(2)}</strong>{product.originalPrice && <del>¥{Number(product.originalPrice).toFixed(2)}</del>}<span>{product.categoryName}</span></div>
        <div className="seller-line"><span>{product.seller?.nickname ?? '校园用户'}</span><span>{new Date(product.createdAt).toLocaleDateString()}</span></div>
      </button>
    </Card>
  )
}
