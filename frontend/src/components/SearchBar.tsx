import { Input } from 'antd'

export default function SearchBar({ placeholder = '搜索校园好物', onSearch }: { placeholder?: string; onSearch: (value: string) => void }) {
  return <Input.Search allowClear size="large" placeholder={placeholder} enterButton="搜索" onSearch={(value) => onSearch(value.trim())} />
}
