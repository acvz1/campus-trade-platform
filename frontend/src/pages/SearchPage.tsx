import { useEffect, useState } from 'react'
import { Button, Cascader, Drawer, Empty, Form, Input, InputNumber, Pagination, Radio, Select, Skeleton, Space, Tag, Typography, message } from 'antd'
import { useSearchParams } from 'react-router-dom'
import { getCategories, type Category, type PageResult, type Product, type ProductQuery } from '../api/product'
import { getHotKeywords, searchProducts } from '../api/search'
import ProductCard from '../components/ProductCard'

interface FilterValues { categoryPath?: number[]; minPrice?: number; maxPrice?: number; conditionLevel?: Product['conditionLevel']; tradeType?: Product['tradeType'] }

function options(categories: Category[]): Array<{ value: number; label: string; children?: ReturnType<typeof options> }> {
  return categories.map((item) => ({ value: item.id, label: item.name, children: item.children.length ? options(item.children) : undefined }))
}

export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const keyword = params.get('keyword') ?? ''
  const [form] = Form.useForm<FilterValues>()
  const [categories, setCategories] = useState<Category[]>([])
  const [hot, setHot] = useState<string[]>([])
  const [filters, setFilters] = useState<FilterValues>({})
  const [sort, setSort] = useState<ProductQuery['sort']>('latest')
  const [page, setPage] = useState(1)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [result, setResult] = useState<PageResult<Product>>({ records: [], total: 0, page: 1, size: 12, pages: 0 })

  useEffect(() => {
    let active = true
    Promise.all([getCategories(), getHotKeywords()]).then(([categoryData, keywords]) => {
      if (active) { setCategories(categoryData); setHot(keywords) }
    }).catch((error: Error) => { if (active) message.error(error.message) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    const categoryPath = filters.categoryPath
    const query: ProductQuery = {
      keyword: keyword || undefined, categoryId: categoryPath?.at(-1), minPrice: filters.minPrice,
      maxPrice: filters.maxPrice, conditionLevel: filters.conditionLevel, tradeType: filters.tradeType,
      sort, page, size: 12,
    }
    searchProducts(query).then((data) => { if (active) setResult(data) })
      .catch((error: Error) => { if (active) message.error(error.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [filters, keyword, page, sort])

  const submitKeyword = (value: string) => { setLoading(true); setPage(1); setParams(value ? { keyword: value } : {}) }
  const applyFilters = async () => { const values = await form.validateFields(); setLoading(true); setFilters(values); setPage(1); setDrawerOpen(false) }
  const activeFilterCount = Object.values(filters).filter((value) => value !== undefined && (!Array.isArray(value) || value.length)).length

  return (
    <div className="page-wrap search-page">
      <div className="search-heading"><div className="eyebrow">FIND YOUR NEXT THING</div><Typography.Title level={2}>搜索校园好物</Typography.Title><Input.Search defaultValue={keyword} size="large" enterButton="搜索" placeholder="输入商品名称或描述" onSearch={submitKeyword} /></div>
      {!keyword && hot.length > 0 && <Space wrap className="hot-keywords"><span>热门：</span>{hot.map((item) => <Tag key={item} onClick={() => submitKeyword(item)}>{item}</Tag>)}</Space>}
      <div className="search-toolbar">
        <Typography.Text>{keyword ? <>“{keyword}” 共找到 <strong>{result.total}</strong> 件</> : <>共 <strong>{result.total}</strong> 件在售好物</>}</Typography.Text>
        <Space><Button onClick={() => setDrawerOpen(true)}>筛选{activeFilterCount ? ` (${activeFilterCount})` : ''}</Button><Select value={sort} style={{ width: 130 }} onChange={(value) => { setLoading(true); setSort(value); setPage(1) }} options={[{ label: '最新发布', value: 'latest' }, { label: '价格从低到高', value: 'price_asc' }, { label: '价格从高到低', value: 'price_desc' }]} /></Space>
      </div>
      {loading ? <div className="product-grid">{Array.from({ length: 8 }, (_, index) => <Skeleton.Node key={index} active className="product-skeleton" />)}</div>
        : result.records.length ? <><div className="product-grid">{result.records.map((product) => <ProductCard key={product.id} product={product} />)}</div><Pagination current={page} total={result.total} pageSize={12} hideOnSinglePage onChange={(value) => { setLoading(true); setPage(value) }} className="market-pagination" /></>
          : <Empty description="没有找到匹配的商品，换个关键词试试" />}
      <Drawer title="筛选商品" placement="right" width={380} open={drawerOpen} onClose={() => setDrawerOpen(false)} extra={<Button type="link" onClick={() => { form.resetFields(); setFilters({}) }}>清空</Button>}>
        <Form<FilterValues> form={form} layout="vertical" initialValues={filters}>
          <Form.Item label="商品类目" name="categoryPath"><Cascader options={options(categories)} changeOnSelect placeholder="全部类目" /></Form.Item>
          <Form.Item label="价格区间"><Space.Compact><Form.Item name="minPrice" noStyle><InputNumber min={0} precision={2} placeholder="最低价" /></Form.Item><Input disabled value="—" className="price-divider" /><Form.Item name="maxPrice" noStyle><InputNumber min={0} precision={2} placeholder="最高价" /></Form.Item></Space.Compact></Form.Item>
          <Form.Item label="商品成色" name="conditionLevel"><Radio.Group options={[{ label: '全新', value: 'NEW' }, { label: '九成新', value: 'LIKE_NEW' }, { label: '正常使用', value: 'USED' }, { label: '旧物', value: 'OLD' }]} /></Form.Item>
          <Form.Item label="交易方式" name="tradeType"><Radio.Group options={[{ label: '自提', value: 'PICKUP' }, { label: '送货', value: 'DELIVERY' }, { label: '均可', value: 'BOTH' }]} /></Form.Item>
          <Button type="primary" block size="large" onClick={applyFilters}>查看结果</Button>
        </Form>
      </Drawer>
    </div>
  )
}
