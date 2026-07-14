import { Button, Image, Input, Modal, Space, Table, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import { auditProduct, getPendingProducts, type ReviewProduct } from '../api/admin'

export default function AuditPage() {
  const [records, setRecords] = useState<ReviewProduct[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [rejecting, setRejecting] = useState<ReviewProduct | null>(null)
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    getPendingProducts(page).then((data) => { setRecords(data.records); setTotal(data.total) })
      .catch((error: Error) => message.error(error.message)).finally(() => setLoading(false))
  }, [page])
  useEffect(() => {
    const initial = window.setTimeout(load, 0)
    return () => window.clearTimeout(initial)
  }, [load])

  const approve = (product: ReviewProduct) => Modal.confirm({
    title: '确认通过审核？', content: `商品「${product.title}」将立即上架。`, okText: '通过并上架',
    onOk: async () => { await auditProduct(product.id, 'APPROVE'); message.success('审核通过'); load() },
  })
  const reject = async () => {
    if (!rejecting || !reason.trim()) { message.warning('请填写驳回原因'); return }
    setSubmitting(true)
    try { await auditProduct(rejecting.id, 'REJECT', reason.trim()); message.success('已驳回'); setRejecting(null); setReason(''); load() }
    catch (error) { if (error instanceof Error) message.error(error.message) }
    finally { setSubmitting(false) }
  }
  const columns: ColumnsType<ReviewProduct> = [
    { title: '商品', dataIndex: 'title', width: 300, render: (_, item) => <div className="audit-product">{item.mainImage ? <Image width={58} height={58} src={item.mainImage} /> : <span className="image-empty">无图</span>}<div><strong>{item.title}</strong><span>{item.description}</span></div></div> },
    { title: '类目', dataIndex: 'categoryName', width: 110, render: (value) => <Tag>{value ?? '未分类'}</Tag> },
    { title: '价格', dataIndex: 'price', width: 100, render: (value) => <strong>¥{Number(value).toFixed(2)}</strong> },
    { title: '发布者', width: 150, render: (_, item) => <div><strong>{item.seller?.nickname}</strong><br /><Typography.Text type="secondary">{item.seller?.studentId ?? item.seller?.phone}</Typography.Text></div> },
    { title: '发布时间', dataIndex: 'createdAt', width: 165, render: (value) => new Date(value).toLocaleString() },
    { title: '操作', fixed: 'right', width: 150, render: (_, item) => <Space><Button type="primary" size="small" onClick={() => approve(item)}>通过</Button><Button danger size="small" onClick={() => setRejecting(item)}>驳回</Button></Space> },
  ]
  return <div>
    <div className="page-title"><div><div className="eyebrow">CONTENT REVIEW</div><Typography.Title level={2}>商品审核</Typography.Title></div><Tag color="gold">{total} 件待处理</Tag></div>
    <Table rowKey="id" columns={columns} dataSource={records} loading={loading} scroll={{ x: 980 }} pagination={{ current: page, total, pageSize: 20, hideOnSinglePage: true, onChange: setPage }} />
    <Modal title="驳回商品" open={Boolean(rejecting)} onCancel={() => { setRejecting(null); setReason('') }} onOk={reject} confirmLoading={submitting} okButtonProps={{ danger: true }} okText="确认驳回">
      <Typography.Paragraph>请说明「{rejecting?.title}」未通过审核的原因，卖家可据此修改。</Typography.Paragraph>
      <Input.TextArea rows={4} maxLength={255} showCount value={reason} onChange={(event) => setReason(event.target.value)} placeholder="例如：商品图片模糊，无法确认实际状态" />
    </Modal>
  </div>
}
