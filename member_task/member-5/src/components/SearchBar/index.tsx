import { Input } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'

const { Search } = Input

interface SearchBarProps {
  placeholder?: string
  onSearch?: (value: string) => void
}

const SearchBar = ({ placeholder = '搜索商品...', onSearch }: SearchBarProps) => {
  const navigate = useNavigate()

  const handleSearch = (value: string) => {
    if (onSearch) {
      onSearch(value)
    } else {
      navigate(`/search?keyword=${encodeURIComponent(value)}`)
    }
  }

  return (
    <Search
      placeholder={placeholder}
      allowClear
      enterButton={<SearchOutlined />}
      size="large"
      onSearch={handleSearch}
      style={{ maxWidth: 600 }}
    />
  )
}

export default SearchBar