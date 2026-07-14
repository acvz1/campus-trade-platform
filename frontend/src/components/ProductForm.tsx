import { useEffect, useState } from 'react'
import { Button, Cascader, Form, Image, Input, InputNumber, Radio, Space, Spin, Typography, Upload, message, type UploadProps } from 'antd'
import { useNavigate } from 'react-router-dom'
import { getCategories, getProduct, publishProduct, updateProduct, uploadProductImage, type Category, type ProductPayload } from '../api/product'

interface ProductFormValues {
  categoryPath: number[]
  title: string
  description: string
  price: number
  originalPrice?: number
  conditionLevel: ProductPayload['conditionLevel']
  tradeType: ProductPayload['tradeType']
  tradeRemark?: string
}

interface UploadedImage { url: string; name: string }

function categoryOptions(categories: Category[]): Array<{ value: number; label: string; children?: ReturnType<typeof categoryOptions> }> {
  return categories.map((item) => ({
    value: item.id,
    label: item.name,
    children: item.children.length ? categoryOptions(item.children) : undefined,
  }))
}

export default function ProductForm({ productId }: { productId?: number }) {
  const navigate = useNavigate()
  const [form] = Form.useForm<ProductFormValues>()
  const [categories, setCategories] = useState<Category[]>([])
  const [images, setImages] = useState<UploadedImage[]>([])
  const [loading, setLoading] = useState(Boolean(productId))
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [dragIndex, setDragIndex] = useState<number | null>(null)

  useEffect(() => {
    let active = true
    getCategories().then((data) => { if (active) setCategories(data) })
      .catch((error: Error) => { if (active) message.error(error.message) })
    if (productId) {
      getProduct(productId).then((product) => {
        if (!active) return
        form.setFieldsValue({
          categoryPath: [product.categoryId], title: product.title, description: product.description,
          price: Number(product.price), originalPrice: product.originalPrice ? Number(product.originalPrice) : undefined,
          conditionLevel: product.conditionLevel, tradeType: product.tradeType, tradeRemark: product.tradeRemark,
        })
        setImages(product.images.map((item) => ({ url: item.url, name: item.url.split('/').pop() ?? '商品图片' })))
      }).catch((error: Error) => message.error(error.message)).finally(() => { if (active) setLoading(false) })
    }
    return () => { active = false }
  }, [form, productId])

  const handleUpload: UploadProps['beforeUpload'] = async (file) => {
    if (images.length >= 9) {
      message.warning('最多上传9张图片')
      return false
    }
    try {
      setUploading(true)
      const uploaded = await uploadProductImage(file as File)
      setImages((current) => [...current, { url: uploaded.url, name: file.name }])
    } catch (error) {
      message.error(error instanceof Error ? error.message : '图片上传失败')
    } finally {
      setUploading(false)
    }
    return false
  }

  const dropImage = (targetIndex: number) => {
    if (dragIndex === null || dragIndex === targetIndex) return
    setImages((current) => {
      const reordered = [...current]
      const [dragged] = reordered.splice(dragIndex, 1)
      reordered.splice(targetIndex, 0, dragged)
      return reordered
    })
    setDragIndex(null)
  }

  const handleSubmit = async (values: ProductFormValues) => {
    if (images.length === 0) {
      message.error('请至少上传一张商品图片')
      return
    }
    const categoryId = values.categoryPath.at(-1)
    if (!categoryId) return
    const payload: ProductPayload = {
      ...values,
      categoryId,
      images: images.map((image, index) => ({ url: image.url, isMain: index === 0, sort: index + 1 })),
    }
    try {
      setSaving(true)
      const product = productId ? await updateProduct(productId, payload) : await publishProduct(payload)
      message.success(productId ? '商品已更新' : '商品已提交审核')
      navigate(productId ? '/user/products' : `/product/${product.id}`)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="page-loading"><Spin size="large" /></div>

  return (
    <div className="page-wrap product-form-page">
      <div className="page-heading"><div><div className="eyebrow">LIST AN ITEM</div><Typography.Title level={2}>{productId ? '编辑商品' : '发布闲置'}</Typography.Title><Typography.Text type="secondary">真实描述，让交易从信任开始</Typography.Text></div></div>
      <Form<ProductFormValues> form={form} layout="vertical" size="large" requiredMark={false} onFinish={handleSubmit}
        initialValues={{ conditionLevel: 'LIKE_NEW', tradeType: 'PICKUP' }}>
        <section className="form-section">
          <div className="form-section-title"><span>01</span><div><strong>商品图片</strong><small>第一张作为封面，拖拽可调整顺序（1–9张）</small></div></div>
          <div className="image-sort-list">
            {images.map((image, index) => (
              <div key={`${image.url}-${index}`} className="image-sort-item" draggable onDragStart={() => setDragIndex(index)} onDragOver={(event) => event.preventDefault()} onDrop={() => dropImage(index)}>
                <Image src={image.url} width="100%" height={132} preview />
                <div><span>{index === 0 ? '封面' : `图片 ${index + 1}`}</span><Button type="link" danger size="small" onClick={() => setImages((current) => current.filter((_, itemIndex) => itemIndex !== index))}>删除</Button></div>
              </div>
            ))}
            {images.length < 9 && <Upload accept="image/png,image/jpeg,image/webp" showUploadList={false} beforeUpload={handleUpload}><button type="button" className="image-upload-tile" disabled={uploading}>{uploading ? '上传中…' : <>＋<span>添加图片</span></>}</button></Upload>}
          </div>
        </section>
        <section className="form-section">
          <div className="form-section-title"><span>02</span><div><strong>基础信息</strong><small>简洁准确的信息更容易被发现</small></div></div>
          <Form.Item label="商品标题" name="title" rules={[{ required: true }, { min: 2, max: 50, message: '标题长度为2-50字' }]}><Input showCount maxLength={50} placeholder="品牌 + 品名 + 关键特点" /></Form.Item>
          <Form.Item label="商品描述" name="description" rules={[{ required: true }, { min: 10, max: 1000, message: '描述长度为10-1000字' }]}><Input.TextArea rows={6} showCount maxLength={1000} placeholder="描述购买时间、使用情况、瑕疵和配件等" /></Form.Item>
          <Form.Item label="商品类目" name="categoryPath" rules={[{ required: true, message: '请选择类目' }]}><Cascader options={categoryOptions(categories)} changeOnSelect placeholder="请选择商品类目" /></Form.Item>
        </section>
        <section className="form-section">
          <div className="form-section-title"><span>03</span><div><strong>价格与交易</strong><small>设置合理价格和交付方式</small></div></div>
          <div className="two-column">
            <Form.Item label="售价" name="price" rules={[{ required: true }]}><InputNumber min={0.01} max={99999.99} precision={2} prefix="¥" style={{ width: '100%' }} /></Form.Item>
            <Form.Item label="购入原价" name="originalPrice" dependencies={['price']} rules={[
              ({ getFieldValue }) => ({ validator(_, value) { return !value || !getFieldValue('price') || value >= getFieldValue('price') ? Promise.resolve() : Promise.reject(new Error('原价不能低于售价')) } }),
            ]}><InputNumber min={0.01} max={99999.99} precision={2} prefix="¥" style={{ width: '100%' }} /></Form.Item>
          </div>
          <Form.Item label="商品成色" name="conditionLevel"><Radio.Group optionType="button" buttonStyle="solid" options={[{ label: '全新', value: 'NEW' }, { label: '九成新', value: 'LIKE_NEW' }, { label: '正常使用', value: 'USED' }, { label: '岁月痕迹', value: 'OLD' }]} /></Form.Item>
          <Form.Item label="交易方式" name="tradeType"><Radio.Group options={[{ label: '校内自提', value: 'PICKUP' }, { label: '送货', value: 'DELIVERY' }, { label: '均可', value: 'BOTH' }]} /></Form.Item>
          <Form.Item label="交易备注" name="tradeRemark"><Input maxLength={255} placeholder="如：北校区图书馆门口自提" /></Form.Item>
        </section>
        <Space className="product-form-actions"><Button onClick={() => navigate(-1)}>取消</Button><Button type="primary" htmlType="submit" loading={saving}>{productId ? '保存修改' : '提交审核'}</Button></Space>
      </Form>
    </div>
  )
}
