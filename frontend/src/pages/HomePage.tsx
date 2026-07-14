import { useEffect, useState } from 'react'
import { Button, Empty, Pagination, Skeleton, Tabs, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { getCategories, listProducts, type Category, type PageResult, type Product } from '../api/product'
import ProductCard from '../components/ProductCard'
import SearchBar from '../components/SearchBar'

export default function HomePage() {
  const navigate = useNavigate()
  const [categories, setCategories] = useState<Category[]>([])
  const [result, setResult] = useState<PageResult<Product>>({ records: [], total: 0, page: 1, size: 12, pages: 0 })
  const [categoryId, setCategoryId] = useState<number | undefined>()
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    getCategories().then((data) => { if (active) setCategories(data) })
      .catch((error: Error) => { if (active) message.error(error.message) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    listProducts({ parentCategoryId: categoryId, page, size: 12 })
      .then((data) => { if (active) setResult(data) })
      .catch((error: Error) => { if (active) message.error(error.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [categoryId, page])

  const switchCategory = (key: string) => {
    setLoading(true)
    setPage(1)
    setCategoryId(key === 'all' ? undefined : Number(key))
  }

  return (
    <>
      <section className="market-hero">
        <div>
          <div className="eyebrow light">CAMPUS CIRCULAR MARKET</div>
          <h1>让每件好物，<br />在校园继续流转。</h1>
          <p>本校实名社区 · 面对面交易 · 让闲置找到新主人</p>
          <SearchBar placeholder="搜教材、数码、宿舍好物" onSearch={(keyword) => navigate(`/search?keyword=${encodeURIComponent(keyword)}`)} />
        </div>
        <div className="market-stat"><strong>校园专属</strong><span>只和同校同学交易</span></div>
      </section>
      <section className="market-section">
        <div className="section-heading">
          <div><div className="eyebrow">FRESH FINDS</div><Typography.Title level={2}>刚刚上架</Typography.Title></div>
          <Button type="primary" onClick={() => navigate('/product/publish')}>发布闲置</Button>
        </div>
        <Tabs activeKey={categoryId?.toString() ?? 'all'} onChange={switchCategory}
          items={[{ key: 'all', label: '全部' }, ...categories.map((item) => ({ key: String(item.id), label: item.name }))]} />
        {loading ? <div className="product-grid">{Array.from({ length: 8 }, (_, index) => <Skeleton.Node key={index} active className="product-skeleton" />)}</div>
          : result.records.length > 0 ? <>
            <div className="product-grid">{result.records.map((product) => <ProductCard key={product.id} product={product} />)}</div>
            <Pagination current={page} total={result.total} pageSize={12} hideOnSinglePage onChange={(value) => { setLoading(true); setPage(value) }} className="market-pagination" />
          </> : <Empty description="这个分类还没有在售商品"><Button type="primary" onClick={() => navigate('/product/publish')}>发布第一件</Button></Empty>}
      </section>
    </>
  )
}
