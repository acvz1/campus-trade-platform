import { Avatar, Badge, Typography, message } from 'antd'
import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Empty from '../components/Empty'
import Loading from '../components/Loading'
import { useChatStore } from '../store/chatStore'

function formatTime(value?: string) {
  if (!value) return '尚未开始聊天'
  const date = new Date(value)
  return date.toDateString() === new Date().toDateString()
    ? date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : date.toLocaleDateString()
}

export default function ChatListPage() {
  const navigate = useNavigate()
  const conversations = useChatStore((state) => state.conversations)
  const loading = useChatStore((state) => state.loading)
  const load = useChatStore((state) => state.loadConversations)

  useEffect(() => {
    load().catch((error: Error) => message.error(error.message))
  }, [load])

  return (
    <div className="page-wrap chat-list-page">
      <div className="eyebrow">CAMPUS MESSAGES</div>
      <Typography.Title level={1}>消息</Typography.Title>
      <Typography.Paragraph type="secondary">围绕商品沟通时间、地点和细节，消息每 10 秒自动刷新。</Typography.Paragraph>
      {loading && conversations.length === 0 ? <Loading spinning /> : conversations.length === 0
        ? <Empty description="还没有会话，去商品详情联系卖家吧" />
        : <div className="conversation-list">
          {conversations.map((item) => <button type="button" className="conversation-row" key={item.id} onClick={() => navigate(`/chat/${item.id}`)}>
            <Badge count={item.unreadCount} overflowCount={99}>
              <Avatar size={52} src={item.otherUser?.avatar}>{item.otherUser?.nickname?.slice(0, 1) ?? '校'}</Avatar>
            </Badge>
            <div className="conversation-main">
              <div><strong>{item.otherUser?.nickname ?? '校园用户'}</strong><time>{formatTime(item.lastMessageTime)}</time></div>
              <span className="conversation-product">关于「{item.productTitle}」</span>
              <p>{item.lastMessage || '会话已创建，发条消息打个招呼吧'}</p>
            </div>
            {item.productImage && <img className="conversation-product-image" src={item.productImage} alt="" />}
          </button>)}
        </div>}
    </div>
  )
}
