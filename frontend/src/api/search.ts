import { request } from '../utils/request'
import type { PageResult, Product, ProductQuery } from './product'

export const searchProducts = (params: ProductQuery) =>
  request<PageResult<Product>>({ method: 'GET', url: '/search', params })

export const getHotKeywords = () => request<string[]>({ method: 'GET', url: '/search/hot' })
