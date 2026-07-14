import { useEffect, useState } from 'react'
import { Button, Empty, Modal, Pagination, Space, Spin, Tabs, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { changeProductShelf, deleteProduct, listMyProducts, type PageResult, type Product } from '../api/product'
import ProductCard from '../components/ProductCard'

const tabs = [
  { key: '', label: '全部' }, { key: 'ON_SALE', label: '在售' }, { key: 'OFF_SHELF', label: '已下架' },
  { key: 'PENDING_REVIEW', label: '审核中' }, { key: 'REJECTED', label: '未通过' }, { key: 'SOLD', label: '已售出' },
]

export default function UserProductsPage() {
  const navigate = useNavigate()
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [result, setResult] = useState<PageResult<Product>>({ records: [], total: 0, page: 1, size: 12, pages: 0 })
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let active = true
    listMyProducts({ status: status || undefined, page, size: 12 })
      .then((data) => { if (active) setResult(data) })
      .catch((error: Error) => { if (active) message.error(error.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [page, refreshKey, status])

  const refresh = () => { setLoading(true); setRefreshKey((value) => value + 1) }

  const shelf = async (product: Product, onShelf: boolean) => {
    try { await changeProductShelf(product.id, onShelf); message.success(onShelf ? '商品已上架' : '商品已下架'); refresh() }
    catch (error) { message.error(error instanceof Error ? error.message : '操作失败') }
  }

  const remove = (product: Product) => {
    Modal.confirm({
      title: '确认删除这件商品？', content: '删除后无法恢复。', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
      onOk: async () => { await deleteProduct(product.id); message.success('商品已删除'); refresh() },
    })
  }

  const actions = (product: Product) => (
    <Space wrap onClick={(event) => event.stopPropagation()}>
      {['PENDING_REVIEW', 'ON_SALE', 'REJECTED', 'OFF_SHELF'].includes(product.status) && <Button type="link" onClick={() => navigate(`/product/edit/${product.id}`)}>编辑</Button>}
      {product.status === 'ON_SALE' && <Button type="link" onClick={() => shelf(product, false)}>下架</Button>}
      {product.status === 'OFF_SHELF' && <Button type="link" onClick={() => shelf(product, true)}>上架</Button>}
      {['PENDING_REVIEW', 'REJECTED', 'OFF_SHELF', 'SOLD'].includes(product.status) && <Button type="link" danger onClick={() => remove(product)}>删除</Button>}
    </Space>
  )

  return (
    <div className="page-wrap">
      <div className="page-heading"><div><div className="eyebrow">MY LISTINGS</div><Typography.Title level={2}>我的发布</Typography.Title></div><Button type="primary" size="large" onClick={() => navigate('/product/publish')}>发布新商品</Button></div>
      <Tabs activeKey={status} items={tabs} onChange={(value) => { setLoading(true); setStatus(value); setPage(1) }} />
      {loading ? <div className="page-loading compact"><Spin size="large" /></div> : result.records.length ? <>
        <div className="product-grid">{result.records.map((product) => <ProductCard key={product.id} product={product} footer={actions(product)} />)}</div>
        <Pagination current={page} total={result.total} pageSize={12} hideOnSinglePage onChange={(value) => { setLoading(true); setPage(value) }} className="market-pagination" />
      </> : <Empty description="当前状态下没有商品" />}
    </div>
  )
}
