import { Card, Tag, Typography, Avatar } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'

const { Text, Paragraph } = Typography

export interface ProductVO {
  id: number
  title: string
  price: number
  originalPrice?: number
  images: string[]
  condition: string // 9成新、全新等
  sellerName: string
  sellerAvatar?: string
  status: 'ON_SALE' | 'SOLD' | 'OFF_SHELF'
}

interface ProductCardProps {
  product: ProductVO
  onClick?: (productId: number) => void
  showStatus?: boolean
}

const ProductCard = ({ product, onClick, showStatus = true }: ProductCardProps) => {
  const navigate = useNavigate()

  const handleClick = () => {
    if (onClick) {
      onClick(product.id)
    } else {
      navigate(`/product/${product.id}`)
    }
  }

  // 状态标签颜色
  const statusMap = {
    ON_SALE: { color: 'green', text: '在售' },
    SOLD: { color: 'red', text: '已售' },
    OFF_SHELF: { color: 'default', text: '已下架' },
  }

  return (
    <Card
      hoverable
      cover={
        <img
          alt={product.title}
          src={product.images?.[0] || 'https://via.placeholder.com/300x300?text=No+Image'}
          style={{ height: 200, objectFit: 'cover' }}
        />
      }
      onClick={handleClick}
      style={{ width: '100%', maxWidth: 280 }}
      actions={[
        <span key="seller">
          <Avatar size="small" icon={<UserOutlined />} src={product.sellerAvatar} />
          {' '}{product.sellerName}
        </span>,
      ]}
    >
      <Card.Meta
        title={
          <Paragraph ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>
            {product.title}
          </Paragraph>
        }
        description={
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
              <Text strong style={{ fontSize: 18, color: '#ff4d4f' }}>
                ¥{product.price}
              </Text>
              {product.originalPrice && (
                <Text delete type="secondary" style={{ fontSize: 14 }}>
                  ¥{product.originalPrice}
                </Text>
              )}
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
              <Tag>{product.condition}</Tag>
              {showStatus && product.status !== 'ON_SALE' && (
                <Tag color={statusMap[product.status].color}>
                  {statusMap[product.status].text}
                </Tag>
              )}
            </div>
          </div>
        }
      />
    </Card>
  )
}

export default ProductCard