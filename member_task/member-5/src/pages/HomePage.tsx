import SearchBar from '../components/SearchBar'
import ProductCard from '../components/ProductCard'
import type { ProductVO } from '../components/ProductCard'
import Empty from '../components/Empty'
import Loading from '../components/Loading'
import { useState, useEffect } from 'react'

const mockProduct: ProductVO = {
  id: 1,
  title: '二手教材 - 高等数学第七版 上下册 全新未拆封',
  price: 35,
  originalPrice: 78,
  images: ['https://via.placeholder.com/300x300?text=Book'],
  condition: '9成新',
  sellerName: '张三',
  status: 'ON_SALE',
}

const HomePage = () => {
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // 模拟加载2秒
    const timer = setTimeout(() => setLoading(false), 2000)
    return () => clearTimeout(timer)
  }, [])

  return (
    <div style={{ padding: 16, paddingBottom: 80 }}>
      <h1>首页</h1>
      
      <div style={{ marginBottom: 20 }}>
        <SearchBar placeholder="搜索二手教材、数码产品..." />
      </div>

      <h3>商品列表</h3>

      {/* 测试 Loading 组件 */}
      {loading ? (
        <Loading type="card" cardCount={4} />
      ) : (
        <>
          {/* 测试商品卡片 */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16 }}>
            {[1, 2, 3, 4].map((i) => (
              <div key={i} style={{ flex: '1 1 200px', maxWidth: 280 }}>
                <ProductCard 
                  product={{
                    ...mockProduct,
                    id: i,
                    title: `${mockProduct.title} ${i}`,
                  }} 
                />
              </div>
            ))}
          </div>

          {/* 测试 Empty 组件 */}
          <div style={{ marginTop: 40 }}>
            <h4>测试空状态：</h4>
            <Empty description="暂无收藏的商品" />
          </div>
        </>
      )}
    </div>
  )
}

export default HomePage