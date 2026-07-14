import { useEffect, useState, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Input, Button, Avatar, Spin, Typography, message as antMessage } from 'antd'
import { SendOutlined, ArrowLeftOutlined } from '@ant-design/icons'
import { useChatStore } from '../stores/chat'
import { getMessages, sendMessage } from '../api/chat'
import type { Message } from '../api/chat'

const { Text } = Typography
const { TextArea } = Input

const ChatDetailPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const conversationId = Number(id)

  // 从 store 读取 messages，不再用本地 state
  const { conversations, messages, setMessages, addMessage, clearUnread } = useChatStore()
  
  const [loading, setLoading] = useState(true)
  const [inputValue, setInputValue] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const sendingRef = useRef(false)

  // 当前用户ID（实际应从 userStore 获取）
  const currentUserId = 1

  // 获取当前会话信息
  const conversation = conversations.find((c) => c.id === conversationId)
  const otherUser = conversation
    ? {
        name: conversation.otherUserName || '用户',
        avatar: conversation.otherUserAvatar || 'https://api.dicebear.com/7.x/avataaars/svg',
      }
    : { name: '用户', avatar: '' }

  // 加载历史消息
  useEffect(() => {
    const loadMessages = async () => {
      setLoading(true)
      try {
        const response = await getMessages(conversationId)
	console.log('消息数据:', response.data)
	setMessages(response.data)
        clearUnread(conversationId)
      } catch (error) {
        console.error('加载消息失败:', error)
      } finally {
        setLoading(false)
      }
    }
    loadMessages()
  }, [conversationId, clearUnread, setMessages])

  // 滚动到底部
  const scrollToBottom = useCallback(() => {
    setTimeout(() => {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, 100)
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages, scrollToBottom])

  // 发送消息
  const handleSend = async () => {
    console.log('=== handleSend 被调用了 ===', new Date().toISOString())
    const content = inputValue.trim()
    if (sendingRef.current || !content) return

    sendingRef.current = true

    try {
    const newMessage = await sendMessage(conversationId, currentUserId, content)
     addMessage(newMessage.data)
      setInputValue('')
      scrollToBottom()
    } catch (error) {
      antMessage.error('发送失败，请重试')
    } finally {
      sendingRef.current = false
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const formatTime = (time: string) => {
    const date = new Date(time)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  const isOwnMessage = (message: Message) => message.senderId === currentUserId

  return (
    <div
      style={{
        height: '100vh',
        display: 'flex',
        flexDirection: 'column',
        background: '#f5f5f5',
      }}
    >
      {/* 顶部导航 */}
      <div
        style={{
          padding: '12px 16px',
          background: '#fff',
          borderBottom: '1px solid #f0f0f0',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          position: 'sticky',
          top: 0,
          zIndex: 10,
        }}
      >
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/chat')}
        />
        <Avatar src={otherUser.avatar} size={36} />
        <div>
          <Text strong>{otherUser.name}</Text>
          {conversation?.productTitle && (
            <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
              商品：{conversation.productTitle}
            </Text>
          )}
        </div>
      </div>

      {/* 消息列表 */}
      <div
        style={{
          flex: 1,
          overflow: 'auto',
          padding: '16px 12px',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {loading ? (
          <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 60 }}>
            <Spin size="large" tip="加载中..." />
          </div>
        ) : messages.length === 0 ? (
          <div style={{ textAlign: 'center', color: '#999', paddingTop: 60 }}>
            暂无消息，开始聊天吧~
          </div>
        ) : (
          <>
            {messages.map((msg) => (
              <div
                key={msg.id}
                style={{
                  display: 'flex',
                  justifyContent: isOwnMessage(msg) ? 'flex-end' : 'flex-start',
                  marginBottom: 12,
                }}
              >
                {!isOwnMessage(msg) && (
                  <Avatar
                    size={32}
                    src={otherUser.avatar}
                    style={{ marginRight: 8, flexShrink: 0 }}
                  />
                )}
                <div
                  style={{
                    maxWidth: '70%',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: isOwnMessage(msg) ? 'flex-end' : 'flex-start',
                  }}
                >
                  <div
                    style={{
                      padding: '10px 14px',
                      borderRadius: 12,
                      background: isOwnMessage(msg) ? '#1677ff' : '#fff',
                      color: isOwnMessage(msg) ? '#fff' : '#333',
                      wordBreak: 'break-word',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.06)',
                    }}
                  >
                    {msg.content}
                  </div>
                  <Text
                    type="secondary"
                    style={{
                      fontSize: 11,
                      marginTop: 4,
                      color: '#999',
                    }}
                  >
                    {formatTime(msg.createdAt)}
                  </Text>
                </div>
                {isOwnMessage(msg) && (
                  <Avatar
                    size={32}
                    src="https://api.dicebear.com/7.x/avataaars/svg?seed=me"
                    style={{ marginLeft: 8, flexShrink: 0 }}
                  />
                )}
              </div>
            ))}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* 底部输入框 */}
      <div
        style={{
          padding: '12px 16px',
          background: '#fff',
          borderTop: '1px solid #f0f0f0',
          display: 'flex',
          gap: 8,
          alignItems: 'flex-end',
          position: 'sticky',
          bottom: 0,
        }}
      >
        <TextArea
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入消息..."
          autoSize={{ minRows: 1, maxRows: 4 }}
          style={{ flex: 1, resize: 'none' }}
          disabled={sendingRef.current}
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          loading={sendingRef.current}
          disabled={!inputValue.trim() || sendingRef.current}
        >
          发送
        </Button>
      </div>
    </div>
  )
}

export default ChatDetailPage