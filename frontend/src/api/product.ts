import { request } from '../utils/request'

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export interface Category {
  id: number
  parentId?: number
  name: string
  icon?: string
  sortOrder: number
  children: Category[]
}

export interface ProductImage {
  id?: number
  url: string
  isMain: boolean
  sort: number
}

export interface Seller {
  id: number
  nickname: string
  avatar?: string
  soldCount: number
}

export interface Product {
  id: number
  title: string
  description: string
  mainImage?: string
  price: number
  originalPrice?: number
  conditionLevel: 'NEW' | 'LIKE_NEW' | 'USED' | 'OLD'
  tradeType: 'PICKUP' | 'DELIVERY' | 'BOTH'
  tradeRemark?: string
  status: string
  viewCount: number
  favoriteCount: number
  categoryId: number
  categoryName: string
  images: ProductImage[]
  seller: Seller
  createdAt: string
  updatedAt: string
}

export interface ProductPayload {
  categoryId: number
  title: string
  description: string
  price: number
  originalPrice?: number
  conditionLevel: Product['conditionLevel']
  tradeType: Product['tradeType']
  tradeRemark?: string
  images: Array<{ url: string; isMain: boolean; sort: number }>
}

export interface ProductQuery {
  keyword?: string
  categoryId?: number
  parentCategoryId?: number
  minPrice?: number
  maxPrice?: number
  conditionLevel?: Product['conditionLevel']
  tradeType?: Product['tradeType']
  sort?: 'latest' | 'price_asc' | 'price_desc'
  status?: string
  page?: number
  size?: number
}

export const getCategories = () => request<Category[]>({ method: 'GET', url: '/category' })
export const listProducts = (params: ProductQuery = {}) => request<PageResult<Product>>({ method: 'GET', url: '/product', params })
export const getProduct = (id: number) => request<Product>({ method: 'GET', url: `/product/${id}` })
export const publishProduct = (data: ProductPayload) => request<Product>({ method: 'POST', url: '/product', data })
export const updateProduct = (id: number, data: ProductPayload) => request<Product>({ method: 'PUT', url: `/product/${id}`, data })
export const listMyProducts = (params: ProductQuery = {}) => request<PageResult<Product>>({ method: 'GET', url: '/product/my', params })
export const deleteProduct = (id: number) => request<null>({ method: 'DELETE', url: `/product/${id}` })
export const changeProductShelf = (id: number, onShelf: boolean) => request<{ id: number; status: string }>({ method: 'PUT', url: `/product/${id}/shelf`, data: { onShelf } })

export const uploadProductImage = (file: File) => {
  const data = new FormData()
  data.append('file', file)
  data.append('type', 'product')
  return request<{ url: string; originalName: string; size: number }>({ method: 'POST', url: '/file/upload', data })
}
