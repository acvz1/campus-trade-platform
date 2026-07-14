import { Empty as AntEmpty } from 'antd'
import type { ReactNode } from 'react'

export default function Empty({ description, children }: { description: string; children?: ReactNode }) {
  return <div className="shared-empty"><AntEmpty description={description}>{children}</AntEmpty></div>
}
