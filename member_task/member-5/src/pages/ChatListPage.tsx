import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { List, Spin, Typography, Empty as AntEmpty } from 'antd'
import { MessageOutlined } from '@ant-design/icons'
import ChatListItem from '../components/ChatListItem'
import { useChatStore } from '../stores/chat'
import { getConversations } from '../api/chat'
import type { Conversation } from '../api/chat'

const { Title } = Typography

const ChatListPage = () => {
  const navigate = useNavigate()
  const { conversations, setConversations } = useChatStore()
  const [loading, setLoadingState] = useState(true)

  // 假设当前用户ID为 1（实际应从 userStore 获取）
  const currentUserId = 1

  useEffect(() => {
    const loadConversations = async () => {
      setLoadingState(true)
      try {
        const response = await getConversations(currentUserId)
	setConversations(response.data)  
      } catch (error) {
        console.error('加载会话列表失败:', error)
      } finally {
        setLoadingState(false)
      }
    }

    loadConversations()
  }, [])

  const handleConversationClick = (conversationId: number) => {
    navigate(`/chat/${conversationId}`)
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 60 }}>
        <Spin size="large" tip="加载中..." />
      </div>
    )
  }

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <div
        style={{
          padding: '16px 16px 8px',
          borderBottom: '1px solid #f0f0f0',
          background: '#fff',
          position: 'sticky',
          top: 0,
          zIndex: 10,
        }}
      >
        <Title level={4} style={{ margin: 0 }}>
          消息
        </Title>
      </div>

      <div style={{ flex: 1, overflow: 'auto' }}>
        {conversations.length === 0 ? (
          <AntEmpty
            image={<MessageOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />}
            description="暂无消息"
            style={{ paddingTop: 60 }}
          />
        ) : (
          <List
            dataSource={conversations}
            renderItem={(item: Conversation) => (
              <ChatListItem
                conversation={item}
                userId={currentUserId}
                onClick={handleConversationClick}
              />
            )}
            style={{ padding: 0 }}
          />
        )}
      </div>
    </div>
  )
}

export default ChatListPage