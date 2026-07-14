import { Avatar, Button, Input, Spin, Typography, message } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { getConversation, listMessages, sendMessage, type ChatMessage, type Conversation } from '../api/chat'
import Empty from '../components/Empty'
import { useChatStore } from '../store/chatStore'
import { useUserStore } from '../store/userStore'

export default function ChatDetailPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const user = useUserStore((state) => state.user)
  const refreshConversations = useChatStore((state) => state.loadConversations)
  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)
  const currentUserId = user && 'id' in user ? user.id : user?.userId

  const reload = useCallback(async (quiet = false) => {
    try {
      const [detail, result] = await Promise.all([getConversation(id), listMessages(id)])
      setConversation(detail)
      setMessages(result.records)
      refreshConversations().catch(() => undefined)
    } catch (error) {
      if (!quiet && error instanceof Error) message.error(error.message)
    } finally {
      if (!quiet) setLoading(false)
    }
  }, [id, refreshConversations])

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) return
    const initial = window.setTimeout(() => reload(), 0)
    const timer = window.setInterval(() => reload(true), 10_000)
    return () => { window.clearTimeout(initial); window.clearInterval(timer) }
  }, [id, reload])

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages])

  const submit = async () => {
    const value = content.trim()
    if (!value || sending) return
    setSending(true)
    try {
      const sent = await sendMessage(id, value)
      setMessages((current) => [...current, sent])
      setContent('')
      refreshConversations().catch(() => undefined)
    } catch (error) {
      if (error instanceof Error) message.error(error.message)
    } finally {
      setSending(false)
    }
  }

  if (!Number.isInteger(id) || id <= 0) return <Navigate to="/chat" replace />
  if (loading) return <div className="page-loading"><Spin size="large" /></div>
  if (!conversation) return <Navigate to="/chat" replace />

  return (
    <div className="chat-detail-page">
      <header className="chat-detail-header">
        <Button type="text" onClick={() => navigate('/chat')}>← 返回</Button>
        <Avatar src={conversation.otherUser?.avatar}>{conversation.otherUser?.nickname?.slice(0, 1)}</Avatar>
        <div><strong>{conversation.otherUser?.nickname}</strong><span>关于「{conversation.productTitle}」</span></div>
        <Button type="link" onClick={() => navigate(`/product/${conversation.productId}`)}>查看商品</Button>
      </header>
      <div className="message-scroll">
        {messages.length === 0 ? <Empty description="还没有消息，先打个招呼吧" /> : messages.map((item) => {
          const mine = item.senderId === currentUserId
          return <div key={item.id} className={`message-line ${mine ? 'mine' : ''}`}>
            {!mine && <Avatar size={34} src={conversation.otherUser?.avatar}>{conversation.otherUser?.nickname?.slice(0, 1)}</Avatar>}
            <div><div className="message-bubble">{item.content}</div><time>{new Date(item.createdAt).toLocaleString()}</time></div>
          </div>
        })}
        <div ref={bottomRef} />
      </div>
      <div className="message-composer">
        <Input.TextArea value={content} maxLength={1000} autoSize={{ minRows: 1, maxRows: 4 }} placeholder={conversation.status === 'ACTIVE' ? '输入消息，Enter 发送' : '会话已关闭'} disabled={conversation.status !== 'ACTIVE'} onChange={(event) => setContent(event.target.value)} onPressEnter={(event) => { if (!event.shiftKey) { event.preventDefault(); submit() } }} />
        <Button type="primary" loading={sending} disabled={!content.trim() || conversation.status !== 'ACTIVE'} onClick={submit}>发送</Button>
      </div>
      <Typography.Text type="secondary" className="chat-refresh-tip">HTTP 消息模式 · 每 10 秒自动刷新</Typography.Text>
    </div>
  )
}
