import { Navigate, useParams } from 'react-router-dom'
import ProductForm from '../components/ProductForm'

export default function ProductEditPage() {
  const id = Number(useParams().id)
  return Number.isInteger(id) && id > 0 ? <ProductForm productId={id} /> : <Navigate to="/user/products" replace />
}
