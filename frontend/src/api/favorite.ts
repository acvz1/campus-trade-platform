import { request } from '../utils/request'
import type { PageResult } from './product'

export interface FavoriteProduct {
  id: number
  title: string
  mainImage?: string
  price: number
  status: string
  categoryId: number
  categoryName: string
  createdAt: string
}

export interface Favorite {
  id: number
  product: FavoriteProduct
  createdAt: string
}

export const toggleFavorite = (productId: number) =>
  request<{ favorited: boolean }>({ method: 'POST', url: `/favorite/${productId}` })

export const removeFavorite = (productId: number) =>
  request<null>({ method: 'DELETE', url: `/favorite/${productId}` })

export const checkFavorite = (productId: number) =>
  request<{ favorited: boolean }>({ method: 'GET', url: `/favorite/check/${productId}` })

export const listFavorites = (params: { categoryId?: number; page?: number; size?: number } = {}) =>
  request<PageResult<Favorite>>({ method: 'GET', url: '/favorite', params })
