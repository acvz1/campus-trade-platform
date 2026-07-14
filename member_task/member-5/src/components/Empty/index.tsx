import { Empty as AntEmpty } from 'antd'
import { FrownOutlined } from '@ant-design/icons'

interface EmptyProps {
  description?: string
  image?: React.ReactNode
  children?: React.ReactNode
}

const Empty = ({ 
  description = '暂无数据', 
  image = <FrownOutlined style={{ fontSize: 64, color: '#d9d9d9' }} />,
  children 
}: EmptyProps) => {
  return (
    <AntEmpty
      image={image}
      description={description}
      style={{ padding: '40px 0' }}
    >
      {children}
    </AntEmpty>
  )
}

export default Empty