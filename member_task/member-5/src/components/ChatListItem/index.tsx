import { Avatar, Badge, Typography, List } from 'antd'
import type { Conversation } from '../../api/chat'

const { Text } = Typography

interface ChatListItemProps {
  conversation: Conversation
  userId: number // 当前登录用户ID
  onClick?: (conversationId: number) => void
}

const ChatListItem = ({ conversation, userId, onClick }: ChatListItemProps) => {
  // 判断当前用户是买家还是卖家，获取对方的未读数
  const isBuyer = conversation.buyerId === userId
  const unreadCount = isBuyer ? conversation.buyerUnreadCount : conversation.sellerUnreadCount

  // 时间格式化
  const formatTime = (time: string) => {
    const date = new Date(time)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    
    if (diff < 24 * 60 * 60 * 1000) {
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    } else if (diff < 7 * 24 * 60 * 60 * 1000) {
      return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
    } else {
      return `${date.getMonth() + 1}/${date.getDate()}`
    }
  }

  return (
    <List.Item
      onClick={() => onClick?.(conversation.id)}
      style={{ cursor: 'pointer', padding: '12px 16px' }}
      extra={
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {conversation.lastMessageTime ? formatTime(conversation.lastMessageTime) : ''}
          </Text>
          {unreadCount && unreadCount > 0 && (
            <Badge count={unreadCount} style={{ marginTop: 4 }} />
          )}
        </div>
      }
    >
      <List.Item.Meta
        avatar={
          <Avatar
            size={48}
            src={conversation.otherUserAvatar || 'https://api.dicebear.com/7.x/avataaars/svg'}
          />
        }
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <Text strong>{conversation.otherUserName || '用户'}</Text>
          </div>
        }
        description={
          <div>
            <Text
              type="secondary"
              ellipsis
              style={{ fontSize: 14, maxWidth: 200 }}
            >
              {conversation.lastMessage || '暂无消息'}
            </Text>
            {conversation.productTitle && (
              <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                商品：{conversation.productTitle}
              </Text>
            )}
          </div>
        }
      />
    </List.Item>
  )
}

export default ChatListItem