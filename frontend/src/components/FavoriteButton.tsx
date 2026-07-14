import { useEffect, useState } from 'react'
import { Button, message } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import { checkFavorite, toggleFavorite } from '../api/favorite'
import { useUserStore } from '../store/userStore'

export default function FavoriteButton({ productId, disabled = false }: { productId: number; disabled?: boolean }) {
  const navigate = useNavigate()
  const location = useLocation()
  const token = useUserStore((state) => state.token)
  const [favorited, setFavorited] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!token) return
    let active = true
    checkFavorite(productId).then((result) => { if (active) setFavorited(result.favorited) })
      .catch(() => undefined)
    return () => { active = false }
  }, [productId, token])

  const toggle = async () => {
    if (!token) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }
    try {
      setLoading(true)
      const result = await toggleFavorite(productId)
      setFavorited(result.favorited)
      message.success(result.favorited ? '已加入收藏' : '已取消收藏')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '收藏操作失败')
    } finally {
      setLoading(false)
    }
  }

  return <Button size="large" block loading={loading} disabled={disabled} className={favorited ? 'favorite-button active' : 'favorite-button'} onClick={toggle}>{favorited ? '♥ 已收藏' : '♡ 收藏'}</Button>
}
