import { Skeleton, Spin } from 'antd'

export default function Loading({ spinning, rows = 4 }: { spinning: boolean; rows?: number }) {
  if (!spinning) return null
  return <div className="shared-loading"><Spin size="large" /><Skeleton active paragraph={{ rows }} /></div>
}
