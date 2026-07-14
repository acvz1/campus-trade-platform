import { useEffect, useState } from 'react'
import { Button, Empty, Pagination, Spin, Tag, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { listFavorites, removeFavorite, type Favorite } from '../api/favorite'
import type { PageResult } from '../api/product'

export default function FavoritesPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [refreshKey, setRefreshKey] = useState(0)
  const [result, setResult] = useState<PageResult<Favorite>>({ records: [], total: 0, page: 1, size: 12, pages: 0 })

  useEffect(() => {
    let active = true
    listFavorites({ page, size: 12 }).then((data) => { if (active) setResult(data) })
      .catch((error: Error) => { if (active) message.error(error.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [page, refreshKey])

  const remove = async (productId: number) => {
    try { await removeFavorite(productId); message.success('已取消收藏'); setLoading(true); setRefreshKey((value) => value + 1) }
    catch (error) { message.error(error instanceof Error ? error.message : '操作失败') }
  }

  return (
    <div className="page-wrap">
      <div className="page-heading"><div><div className="eyebrow">SAVED ITEMS</div><Typography.Title level={2}>我的收藏</Typography.Title></div></div>
      {loading ? <div className="page-loading compact"><Spin size="large" /></div> : result.records.length ? <>
        <div className="favorite-grid">{result.records.map((favorite) => <article className="favorite-card" key={favorite.id}>
          <button type="button" onClick={() => navigate(`/product/${favorite.product.id}`)}>{favorite.product.mainImage ? <img src={favorite.product.mainImage} alt="" /> : <span />}</button>
          <div><Tag>{favorite.product.categoryName}</Tag><Typography.Title level={4} ellipsis>{favorite.product.title}</Typography.Title><strong>¥{Number(favorite.product.price).toFixed(2)}</strong><Button type="link" danger onClick={() => remove(favorite.product.id)}>取消收藏</Button></div>
        </article>)}</div>
        <Pagination current={page} pageSize={12} total={result.total} hideOnSinglePage onChange={(value) => { setLoading(true); setPage(value) }} className="market-pagination" />
      </> : <Empty description="还没有收藏商品"><Button type="primary" onClick={() => navigate('/')}>去逛逛</Button></Empty>}
    </div>
  )
}
