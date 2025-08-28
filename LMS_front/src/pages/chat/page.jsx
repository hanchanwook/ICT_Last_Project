import { useState, useRef, useEffect } from "react"
import { Search, MessageSquare, Users, Phone, Video, MoreVertical, Send, Smile, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { searchUsers, createChatRoom, getChatRooms, leaveChatRoom, getChatRoomParticipants, getChatMessages, sendMessage, getLastExitedAt, updateExitedAt, addUserToGroup } from "@/api/youngjae/chatApi"
import SockJS from "sockjs-client"
import Stomp from "stompjs"
import EmojiPicker from 'emoji-picker-react'
import { createPortal } from "react-dom"


  // 이모지창 위치 옯기고싶어
  function EmojiPickerPortal({ onEmojiClick, onClose }) {
    const panelRef = useRef(null)
  
    useEffect(() => {
      const handleOutside = (e) => {
        if (panelRef.current && !panelRef.current.contains(e.target)) {
          onClose()
        }
      }
      const handleEsc = (e) => {
        if (e.key === "Escape") onClose()
      }
      document.addEventListener("mousedown", handleOutside)
      document.addEventListener("keydown", handleEsc)
      return () => {
        document.removeEventListener("mousedown", handleOutside)
        document.removeEventListener("keydown", handleEsc)
      }
    }, [onClose])
  
    return createPortal(
      <div
        ref={panelRef}
        className="fixed bottom-10 left-5 z-50 w-[350px] h-[400px] bg-white rounded-lg shadow-xl overflow-hidden"
        style={{ border: "1px solid #e2e8f0" }}
      >
        <EmojiPicker
          onEmojiClick={(emojiObject) => onEmojiClick(emojiObject)}
          searchPlaceholder="이모지 검색..."
          width="100%"
          height="100%"
        />
      </div>,
      document.body
    )
  }

export default function ChatPage() {
  const [newMessage, setNewMessage] = useState("")
  const [activeChat, setActiveChat] = useState(null)
  const messagesEndRef = useRef(null)
  const stompClient = useRef(null)
  const connectionCheckRef = useRef(null)
  const [isConnected, setIsConnected] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [typingUsers, setTypingUsers] = useState([])
  const [onlineUsers, setOnlineUsers] = useState([])
  const [isTyping, setIsTyping] = useState(false)
  const typingTimerRef = useRef(null)
  const recentSentMessages = useRef([]) // 최근 보낸 메시지 추적

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [searchResults, setSearchResults] = useState([])
  const [searchType, setSearchType] = useState("all") // "all" for unified search
  const [selectedMembers, setSelectedMembers] = useState([])

  const [showConfirmModal, setShowConfirmModal] = useState(false)
  const [chatRoomName, setChatRoomName] = useState("")
  const [showLeaveConfirmModal, setShowLeaveConfirmModal] = useState(false)
  const [showParticipantsModal, setShowParticipantsModal] = useState(false)
  const [participants, setParticipants] = useState([])

  // 사용자 추가 관련 상태
  const [showAddUserModal, setShowAddUserModal] = useState(false)
  const [addUserSearchQuery, setAddUserSearchQuery] = useState("")
  const [addUserSearchResults, setAddUserSearchResults] = useState([])
  const [selectedUserToAdd, setSelectedUserToAdd] = useState(null)

  const [chatList, setChatList] = useState([])

  // 이모지 피커 관련 상태
  const [showEmojiPicker, setShowEmojiPicker] = useState(false)



  const [allMessages, setAllMessages] = useState({})
  const [lastExitedAt, setLastExitedAt] = useState({}) // 각 채팅방별 마지막으로 나간 시간 (DB 기반)
  const [unreadCounts, setUnreadCounts] = useState({}) // 각 채팅방별 안 읽은 메시지 개수
  const refreshIntervalRef = useRef(null)

  // messages 변수를 현재 활성 채팅의 메시지로 설정
  const currentRoom = chatList.find(room => room.name === activeChat)
  const roomId = currentRoom?.id || activeChat
  
  console.log('📊 메시지 변수 설정:', {
    activeChat,
    currentRoom: currentRoom?.name,
    roomId,
    chatListNames: chatList.map(room => room.name),
    chatListIds: chatList.map(room => room.id)
  })
  
  // roomId가 유효한 경우에만 메시지 가져오기
  const messages = activeChat && roomId && roomId !== 'null' && roomId !== 'undefined' 
    ? (allMessages[roomId] || []) 
    : []
  
  // 디버깅용 로그
  console.log('📊 메시지 상태:', {
    activeChat,
    currentRoom: currentRoom?.name,
    roomId,
    allMessagesKeys: Object.keys(allMessages),
    messagesCount: messages.length,
    messages: messages // 전체 메시지
  })
  
  // null 키 정리 (디버깅용)
  if (allMessages['null'] || allMessages['undefined']) {
    console.log('🧹 null 키 정리 필요:', {
      nullMessages: allMessages['null']?.length || 0,
      undefinedMessages: allMessages['undefined']?.length || 0
    })
    
    // null 키 정리
    setAllMessages(prev => {
      const cleaned = { ...prev }
      delete cleaned['null']
      delete cleaned['undefined']
      return cleaned
    })
  }

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  // 연결 상태 확인 함수
  const isStompConnected = () => {
    const hasStompClient = !!stompClient.current
    const isConnected = stompClient.current?.connected
    const hasWebSocket = !!stompClient.current?.ws
    const wsReadyState = stompClient.current?.ws?.readyState
    
    const isConnectedResult = hasStompClient && isConnected && hasWebSocket && wsReadyState === 1
    
    // 디버깅을 위한 상세 로그 (너무 자주 호출되지 않도록 조건부로)
    if (!isConnectedResult && hasStompClient) {
      console.log('🔍 연결 상태 상세 분석:', {
        hasStompClient,
        isConnected,
        hasWebSocket,
        wsReadyState,
        wsReadyStateText: {
          0: 'CONNECTING',
          1: 'OPEN',
          2: 'CLOSING',
          3: 'CLOSED'
        }[wsReadyState] || 'UNKNOWN',
        timestamp: new Date().toISOString()
      })
    }
    
    return isConnectedResult
  }

  // 마지막 exitedAt 시간 가져오기 (DB에서)
  const getLastExitedAtFromDB = async (roomId) => {
    console.log('🚀 getLastExitedAtFromDB 함수 호출됨:', roomId)
    try {
      console.log('🔍 exitedAt 조회 시작:', roomId)
      const response = await getLastExitedAt(roomId)
      console.log('📡 exitedAt API 응답:', response)
      
      if (response.success && response.lastExitedAt) {
        const exitedAt = new Date(response.lastExitedAt)
        console.log('✅ exitedAt 파싱 성공:', {
          original: response.lastExitedAt,
          parsed: exitedAt.toISOString(),
          isValid: !isNaN(exitedAt.getTime())
        })
        
        setLastExitedAt(prev => {
          const newState = {
            ...prev,
            [roomId]: exitedAt
          }
          console.log('🔄 lastExitedAt 상태 업데이트:', {
            roomId,
            newExitedAt: exitedAt.toISOString(),
            allRooms: Object.keys(newState)
          })
          
          // sessionStorage에 저장 (새로고침 시 유지, 탭 닫으면 삭제)
          const storedExitedAt = JSON.parse(sessionStorage.getItem('chatLastExitedAt') || '{}')
          storedExitedAt[roomId] = exitedAt.toISOString()
          sessionStorage.setItem('chatLastExitedAt', JSON.stringify(storedExitedAt))
          
          return newState
        })
        return exitedAt
      } else {
        console.log('❌ exitedAt 응답 실패:', response)
        return null
      }
    } catch (error) {
      console.error('마지막 exitedAt 조회 실패:', error)
      return null
    }
  }

  // 안 읽은 메시지 개수 계산 (백엔드 unread 필드 사용)
  const calculateUnreadCount = (roomId) => {
    // 백엔드에서 제공하는 unread 필드 사용
    const currentRoom = chatList.find(room => room.id === roomId)
    const backendUnreadCount = currentRoom?.unread || 0
    
    console.log('📊 백엔드 unread 필드 사용:', {
      roomId,
      roomName: currentRoom?.name,
      backendUnreadCount,
      hasRoom: !!currentRoom
    })
    
    return backendUnreadCount
  }

  // 안 읽은 메시지가 시작되는 인덱스 찾기 (백엔드 unread 필드 기반)
  const getFirstUnreadMessageIndex = (roomId) => {
    const messages = allMessages[roomId] || []
    const unreadCount = calculateUnreadCount(roomId)
    
    console.log('🔍 getFirstUnreadMessageIndex 디버깅 (백엔드 unread 기반):', {
      roomId,
      totalMessages: messages.length,
      unreadCount,
      hasUnreadMessages: unreadCount > 0
    })
    
    if (unreadCount <= 0) {
      console.log('❌ 안 읽은 메시지 없음, 맨 아래로 스크롤')
      return messages.length // 안 읽은 메시지가 없으면 맨 아래로
    }
    
    // 안 읽은 메시지 개수만큼 뒤에서부터 계산
    const firstUnreadIndex = Math.max(0, messages.length - unreadCount)
    
    console.log('📊 getFirstUnreadMessageIndex 결과 (백엔드 unread 기반):', {
      roomId,
      firstUnreadIndex,
      totalMessages: messages.length,
      unreadCount
    })
    
    return firstUnreadIndex
  }

  // 안 읽은 메시지 구분선 표시 여부 확인 (백엔드 unread 필드 사용)
  const shouldShowUnreadDivider = (roomId) => {
    if (!roomId) return false
    const unreadCount = calculateUnreadCount(roomId)
    return unreadCount > 0
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  // WebSocket 연결 (HTTP API와 분리)
  // 환경별 WebSocket URL 설정
  const getWebSocketUrl = () => {
    const isDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    if (isDev) {
      return 'http://localhost:19091/ws/chat'
    } else {
      // 배포 환경에서는 현재 도메인을 사용하되, WebSocket은 같은 포트로 프록시
      const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
      const host = window.location.hostname
      const port = window.location.port || (window.location.protocol === 'https:' ? '443' : '80')
      return `${protocol}//${host}:${port}/ws/chat`
    }
  }

  // 순수 WebSocket을 사용한 연결 (배포 환경용)
  const connectWebSocketDirect = (chatRoomId) => {
    console.log('순수 WebSocket 연결 시작...')
    
    const isDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    let wsUrl
    
    if (isDev) {
      wsUrl = `ws://localhost:19091/ws/chat/websocket`
    } else {
      // 배포 환경에서는 현재 도메인을 사용하되, WebSocket은 같은 포트로 프록시
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = window.location.hostname
      const port = window.location.port || (window.location.protocol === 'https:' ? '443' : '80')
      wsUrl = `${protocol}//${host}:${port}/ws/chat/websocket`
    }
    
    console.log('WebSocket URL:', wsUrl)
    
    const socket = new WebSocket(wsUrl)
    
    socket.onopen = function(event) {
      console.log('WebSocket connected')
      
      // STOMP 연결
      const stompClient = Stomp.over(socket)
      stompClient.connect({}, 
        function (frame) {
          console.log('STOMP connected: ' + frame)
          
          // 채팅방 구독
          stompClient.subscribe(`/topic/chat/${chatRoomId}`, function (message) {
            const chatMessage = JSON.parse(message.body)
            console.log('Received message:', chatMessage)
            // 메시지 처리 로직
          })
        },
        function (error) {
          console.error('STOMP connection error:', error)
        }
      )
    }
    
    socket.onerror = function(error) {
      console.error('WebSocket error:', error)
    }
    
    socket.onclose = function(event) {
      console.log('WebSocket closed:', event.code, event.reason)
    }
    
    return socket
  }

  // SockJS를 사용한 WebSocket 연결 (쿠키 자동 전달) - HttpOnly 쿠키 지원
  const connectWebSocketWithSockJS = (chatRoomId) => {
    console.log('SockJS를 사용한 WebSocket 연결 시작...')
    
    const wsUrl = getWebSocketUrl()
    console.log('WebSocket URL:', wsUrl)
    
    const socket = new SockJS(wsUrl)
    const stompClient = Stomp.over(socket)
    
    // 연결 시 쿠키가 자동으로 전달됨 (HttpOnly 쿠키 포함)
    stompClient.connect({}, 
      function (frame) {
        console.log('Connected to WebSocket: ' + frame)
        
        // 채팅방 구독
        stompClient.subscribe(`/topic/chat/${chatRoomId}`, function (message) {
          const chatMessage = JSON.parse(message.body)
          console.log('Received message:', chatMessage)
          // 메시지 처리 로직
        })
        
        // 개인 메시지 구독
        stompClient.subscribe('/user/queue/messages', function (message) {
          const privateMessage = JSON.parse(message.body)
          console.log('Received private message:', privateMessage)
          // 개인 메시지 처리 로직
        })
      },
      function (error) {
        console.error('WebSocket connection error:', error)
      }
    )
    
    return stompClient
  }

  // Authorization 헤더를 사용한 연결 (HttpOnly 쿠키 대안)
  const connectWebSocketWithAuthHeader = (chatRoomId) => {
    console.log('Authorization 헤더를 사용한 WebSocket 연결 시작...')
    
    const isDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    
    // HttpOnly 쿠키는 JavaScript에서 접근할 수 없으므로
    // 서버에서 별도로 토큰을 받아와야 함
    const apiUrl = isDev ? 'http://localhost:19091/api/auth/token' : '/api/auth/token'
    
    fetch(apiUrl, {
      method: 'GET',
      credentials: 'include' // 쿠키 포함
    })
    .then(response => response.json())
    .then(data => {
      if (data.token) {
        // SockJS 연결 시 헤더 추가
        const wsUrl = getWebSocketUrl()
        const socket = new SockJS(wsUrl)
        const stompClient = Stomp.over(socket)
        
        // 연결 시 Authorization 헤더 추가
        const connectHeaders = {
          'Authorization': `Bearer ${data.token}`
        }
        
        stompClient.connect(connectHeaders, 
          function (frame) {
            console.log('Connected to WebSocket with auth header: ' + frame)
            
            // 채팅방 구독
            stompClient.subscribe(`/topic/chat/${chatRoomId}`, function (message) {
              const chatMessage = JSON.parse(message.body)
              console.log('Received message:', chatMessage)
              // 메시지 처리 로직
            })
          },
          function (error) {
            console.error('WebSocket connection error:', error)
          }
        )
      } else {
        console.error('토큰을 받아올 수 없습니다.')
      }
    })
    .catch(error => {
      console.error('토큰 요청 실패:', error)
    })
  }

  // 쿠키 상태 확인 함수
  const checkCookieStatus = () => {
    console.log('=== 쿠키 상태 확인 ===')
    console.log('document.cookie:', document.cookie)
    console.log('현재 도메인:', window.location.hostname)
    console.log('현재 프로토콜:', window.location.protocol)
    
    const isDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    
    // HttpOnly 쿠키는 JavaScript에서 접근할 수 없으므로
    // 서버에서 쿠키 상태를 확인하는 API 호출
    const apiUrl = isDev ? 'http://localhost:19091/api/auth/cookie-status' : '/api/auth/cookie-status'
    
    fetch(apiUrl, {
      method: 'GET',
      credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
      console.log('서버에서 확인한 쿠키 상태:', data)
    })
    .catch(error => {
      console.error('쿠키 상태 확인 실패:', error)
    })
  }

  // 연결 테스트 함수
  const testWebSocketConnection = () => {
    console.log('=== WebSocket 연결 테스트 ===')
    const isDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    console.log('현재 환경:', isDev ? '개발' : '배포')
    console.log('WebSocket URL:', getWebSocketUrl())
    
    // 간단한 연결 테스트
    try {
      const stompClient = connectWebSocketWithSockJS('test')
      setTimeout(() => {
        if (stompClient && stompClient.connected) {
          console.log('✅ WebSocket 연결 성공!')
          stompClient.disconnect()
        } else {
          console.log('❌ WebSocket 연결 실패')
        }
      }, 3000)
    } catch (error) {
      console.error('WebSocket 연결 테스트 중 오류:', error)
    }
  }

  const connectToChatRoom = (roomId) => {
    try {
      console.log('🔄 WebSocket 연결 시도:', {
        roomId,
        timestamp: new Date().toISOString(),
        existingConnection: !!(stompClient.current && stompClient.current.connected),
        wsBaseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:19091',
        location: window.location.href,
        protocol: window.location.protocol,
        host: window.location.host
      })
      
      if (stompClient.current) {
        // 기존 연결 해제
        console.log('🔌 기존 WebSocket 연결 해제')
        try {
          if (stompClient.current.connected) {
            stompClient.current.disconnect()
          }
        } catch (error) {
          console.error('❌ 기존 WebSocket 연결 해제 중 오류:', error)
        } finally {
          // 기존 인터벌도 정리
          if (connectionCheckRef.current?.intervalId) {
            clearInterval(connectionCheckRef.current.intervalId)
            connectionCheckRef.current = null
          }
          // stompClient 초기화
          stompClient.current = null
        }
      }

      // 새로운 WebSocket URL 생성 함수 사용
      const wsUrl = getWebSocketUrl()
      console.log('🔗 WebSocket URL:', wsUrl)
      
      // 네트워크 상태 확인
      if (!navigator.onLine) {
        console.error('❌ 네트워크 연결이 없습니다.')
        setIsConnected(false)
        setIsLoading(false)
        return
      }
      
      // 연결 전략: SockJS 우선 시도, 실패 시 순수 WebSocket 시도
      let connectionAttempted = false
      
      // 1. SockJS 연결 시도
      try {
        console.log('🔗 SockJS 연결 시도...')
        const socket = new SockJS(wsUrl, null, { 
          withCredentials: true,
          transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
          timeout: 15000, // 15초 타임아웃
          heartbeat: {
            interval: 25000, // 25초마다 heartbeat
            timeout: 60000   // 60초 타임아웃
          }
        })
        
        stompClient.current = Stomp.over(socket)
        
        // STOMP 디버그 모드 비활성화
        stompClient.current.debug = null
        
        // SockJS 이벤트 리스너 추가
        socket.onopen = function() {
          console.log('✅ SockJS 연결 성공')
        }
        
        socket.onclose = function(event) {
          console.log('❌ SockJS 연결 종료:', event)
          setIsConnected(false)
          
          // SockJS 실패 시 순수 WebSocket 시도
          if (!connectionAttempted) {
            connectionAttempted = true
            console.log('🔄 SockJS 실패, 순수 WebSocket 시도...')
            connectWebSocketDirect(roomId)
          }
        }
        
        socket.onerror = function(error) {
          console.error('❌ SockJS 연결 오류:', error)
          setIsConnected(false)
          
          // SockJS 실패 시 순수 WebSocket 시도
          if (!connectionAttempted) {
            connectionAttempted = true
            console.log('🔄 SockJS 실패, 순수 WebSocket 시도...')
            connectWebSocketDirect(roomId)
          }
        }

        // 쿠키 가져오기
        const cookies = document.cookie
        const refreshToken = cookies.split(';')
          .find(cookie => cookie.trim().startsWith('refresh='))
          ?.split('=')[1]
        
        const connectHeaders = refreshToken ? {
          'Cookie': `refresh=${refreshToken}`
        } : {}

        stompClient.current.connect(connectHeaders, function(frame) {
          console.log('✅ STOMP 연결 성공:', frame)
          console.log('🔗 연결된 클라이언트 정보:', {
            roomId,
            timestamp: new Date().toISOString(),
            userAgent: navigator.userAgent,
            frame: frame
          })
          setIsConnected(true)
          setIsLoading(false)
          connectionAttempted = true 
          
          // 연결이 완전히 확립된 후 작업 수행 (약간의 지연 추가)
          setTimeout(() => {
            // 채팅방 입장
            console.log('🔑 채팅방 입장:', roomId)
            try {
              stompClient.current.send("/app/chat.addUser", {}, roomId)
              console.log('✅ 채팅방 입장 완료')
            } catch (error) {
              console.error('❌ 채팅방 입장 실패:', error)
            }
            
            // 메시지 구독
            const subscribePath = "/topic/room/" + roomId
            console.log('🔔 메시지 구독 시작:', subscribePath)
            try {
              stompClient.current.subscribe(subscribePath, function(message) {
                console.log('📨 WebSocket 메시지 수신:', message.body)
                try {
                  const chatMessage = JSON.parse(message.body)
                  displayMessage(chatMessage)
                } catch (error) {
                  console.error('❌ 메시지 파싱 실패:', error, message.body)
                }
              })
              console.log('✅ 메시지 구독 완료')
            } catch (error) {
              console.error('❌ 메시지 구독 실패:', error)
            }
            
            // 상태 구독
            try {
              stompClient.current.subscribe("/topic/room/" + roomId + "/status", function(message) {
                const statusMessage = JSON.parse(message.body)
                handleUserStatus(statusMessage)
              })
              console.log('✅ 상태 구독 완료')
            } catch (error) {
              console.error('❌ 상태 구독 실패:', error)
            }
            
            // 타이핑 구독
            try {
              stompClient.current.subscribe("/topic/room/" + roomId + "/typing", function(message) {
                const typingMessage = JSON.parse(message.body)
                handleTypingStatus(typingMessage)
              })
              console.log('✅ 타이핑 구독 완료')
            } catch (error) {
              console.error('❌ 타이핑 구독 실패:', error)
            }
          }, 100) // 100ms 지연으로 연결 안정화
        }, function(error) {
          console.log('❌ WebSocket 연결 실패:', error)
          console.log('🔍 연결 실패 상세 정보:', {
            command: error.command,
            body: error.body,
            headers: error.headers,
            timestamp: new Date().toISOString(),
            userAgent: navigator.userAgent,
            url: wsUrl
          })
          
          // 서버 상태 확인
          const healthUrl = isDev ? 'http://localhost:19091/api/health' : '/api/health'
          fetch(healthUrl, { 
            method: 'GET',
            credentials: 'include'
          })
          .then(response => {
            console.log('🏥 서버 상태 확인:', response.status, response.statusText)
          })
          .catch(healthError => {
            console.error('🏥 서버 상태 확인 실패:', healthError)
          })
          
          if (error.command === 'ERROR') {
            if (error.body && error.body.includes('JWT 인증 실패')) {
              alert('인증 실패: 로그인이 필요합니다.')
            } else {
              alert('WebSocket 연결 실패: ' + error.body)
            }
          } else {
            alert('WebSocket 연결 실패: ' + (error.message || '알 수 없는 오류'))
          }
          
          setIsConnected(false)
          
          // SockJS 실패 시 순수 WebSocket 시도
          if (!connectionAttempted) {
            connectionAttempted = true
            console.log('🔄 SockJS 실패, 순수 WebSocket 시도...')
            connectWebSocketDirect(roomId)
          }
        })
        
      } catch (error) {
        console.error('SockJS 초기화 실패:', error)
        
        // SockJS 실패 시 순수 WebSocket 시도
        if (!connectionAttempted) {
          connectionAttempted = true
          console.log('🔄 SockJS 초기화 실패, 순수 WebSocket 시도...')
          connectWebSocketDirect(roomId)
        }
      }
      
      // 연결 상태 모니터링 및 자동 재연결
      if (connectionCheckRef.current?.intervalId) {
        clearInterval(connectionCheckRef.current.intervalId)
      }
      
      // connectionCheckRef를 객체로 초기화
      connectionCheckRef.current = {
        intervalId: null,
        reconnectCount: 0
      }
      
      connectionCheckRef.current.intervalId = setInterval(() => {
        const isConnectedNow = isStompConnected()
        if (isConnectedNow) {
          setIsConnected(true)
        } else {
          setIsConnected(false)
          console.log('⚠️ SockJS 연결 끊어짐 감지:', {
            timestamp: new Date().toISOString(),
            stompClient: !!stompClient.current,
            connected: stompClient.current?.connected,
            wsReadyState: stompClient.current?.ws?.readyState,
            socketReadyState: stompClient.current?.ws?.readyState,
            activeChat,
            roomId,
            networkOnline: navigator.onLine,
            reconnectAttempts: connectionCheckRef.current?.reconnectCount || 0
          })
          
          // 재연결 시도 횟수 추적
          connectionCheckRef.current.reconnectCount++
          
          // 자동 재연결 시도 (최대 3회)
          if (activeChat && roomId && connectionCheckRef.current.reconnectCount <= 3) {
            console.log(`🔄 SockJS 자동 재연결 시도 (${connectionCheckRef.current.reconnectCount}/3)`)
            setTimeout(() => {
              if (!isConnected) {
                connectToChatRoom(roomId)
              }
            }, 5000)
          } else if (connectionCheckRef.current.reconnectCount > 3) {
            console.log('❌ 최대 재연결 시도 횟수 초과, HTTP 폴링 모드로 전환')
            // HTTP 폴링 모드로 전환하는 로직은 이미 구현되어 있음
          }
        }
      }, 2000) // 2초마다 체크
    } catch (error) {
      console.error('WebSocket 초기화 실패:', error)
      setIsConnected(false)
      setIsLoading(false)
    }
  }

  // 안전한 날짜 파싱 함수
  const parseSafeDate = (dateString) => {
    if (!dateString) return new Date()
    
    try {
      const date = new Date(dateString)
      // Invalid Date 체크
      if (isNaN(date.getTime())) {
        console.warn('Invalid date string:', dateString)
        return new Date()
      }
      return date
    } catch (error) {
      console.error('Date parsing error:', error, 'for string:', dateString)
      return new Date()
    }
  }

  // 이전 메시지들 불러오기
  const fetchPreviousMessages = async (roomId) => {
    try {
      // console.log('이전 메시지 불러오기 시작:', roomId)
      const response = await getChatMessages(roomId)
      
      if (response.success && response.messages) {
        // console.log('이전 메시지 응답:', response.messages)
        
        // 현재 사용자 정보 가져오기
        const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')
        const currentUserName = currentUser.name || currentUser.username || "나"
        
        const formattedMessages = response.messages.map(msg => {
          // 백엔드 메시지 구조에 맞게 파싱
          let messageContent = ""
          let senderName = ""
          let messageTime = ""
          let senderId = ""
          
          if (msg.message && msg.message.content) {
            messageContent = msg.message.content
            senderName = msg.message.sender?.name || "알 수 없음"
            senderId = msg.message.sender?.id || ""
            messageTime = msg.message.time || msg.time || new Date().toISOString()
          } else if (msg.content) {
            messageContent = msg.content
            senderName = msg.sender?.name || msg.sender || "알 수 없음"
            senderId = msg.sender?.id || msg.senderId || ""
            messageTime = msg.time || new Date().toISOString()
          }
          
          // 빈 메시지는 제외
          if (!messageContent || !messageContent.trim()) {
            return null
          }
          
          const parsedTime = parseSafeDate(messageTime)
          
          // 내가 쓴 메시지인지 확인 (이름 또는 ID로 비교)
          const isMyMessage = senderName === currentUserName || 
                             senderId === currentUser.id ||
                             senderName === "나" // 기존 호환성 유지
          
          return {
            id: msg.id || msg.message?.id || Date.now(),
            sender: senderName,
            content: messageContent.trim(),
            time: parsedTime.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }),
            isMe: isMyMessage,
            timestamp: parsedTime.getTime() // 정렬을 위한 타임스탬프 추가
          }
        }).filter(msg => msg !== null) // null 메시지 제거
        
        // 시간순으로 정렬 (오래된 메시지가 위로, 최신 메시지가 아래로)
        const sortedMessages = formattedMessages.sort((a, b) => a.timestamp - b.timestamp)
        
        // console.log('포맷된 이전 메시지:', formattedMessages)
        
        // 기존 메시지와 병합 (중복 방지)
        setAllMessages(prev => ({
          ...prev,
          [roomId]: sortedMessages
        }))
        
        // 스크롤을 맨 아래로
        setTimeout(() => {
          scrollToBottom()
        }, 100)
      } else {
        // console.log('이전 메시지가 없거나 응답 구조가 다름:', response)
        setAllMessages(prev => ({
          ...prev,
          [roomId]: []
        }))
      }
    } catch (error) {
      console.error('이전 메시지 불러오기 중 오류:', error)
      setAllMessages(prev => ({
        ...prev,
        [roomId]: []
      }))
    }
  }

  // 실시간 메시지 수신 (WebSocket)
  const displayMessage = (chatMessage) => {
    console.log('📨 실시간 메시지 수신:', chatMessage)
    
    // 백엔드 메시지 구조에 맞게 파싱
    let messageContent = ""
    let messageSender = ""
    let messageTime = ""
    let messageId = ""
    let messageSenderId = ""
    
    // message 객체가 있는 경우 (백엔드 구조)
    if (chatMessage.message) {
      messageContent = chatMessage.message.content || ""
      messageSender = chatMessage.message.sender?.name || "알 수 없음"
      messageSenderId = chatMessage.message.sender?.id || ""
      messageTime = chatMessage.message.time || ""
      messageId = chatMessage.message.id || chatMessage.id || ""
    } else {
      // 직접 content가 있는 경우 (기존 구조)
      messageContent = chatMessage.content || ""
      messageSender = chatMessage.sender || "알 수 없음"
      messageSenderId = chatMessage.senderId || ""
      messageTime = chatMessage.timestamp || ""
      messageId = chatMessage.id || ""
    }
    
    console.log('📝 파싱된 메시지:', { messageContent, messageSender, messageId })
    
    // content가 비어있는 메시지는 제외
    if (!messageContent || !messageContent.trim()) {
      console.log('❌ 빈 메시지 무시')
      return
    }
    
    // 현재 사용자 정보 가져오기
    const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')
    const currentUserName = currentUser.name || currentUser.username || "나"
    
    // 내가 쓴 메시지인지 확인 (이름 또는 ID로 비교)
    const isMyMessage = messageSender === currentUserName || 
                       messageSenderId === currentUser.id ||
                       messageSender === "나" // 기존 호환성 유지
    
    // 내가 보낸 메시지는 중복 체크에서 제외 (다른 사용자로부터 온 메시지만 중복 체크)
    if (!isMyMessage) {
      // 최근에 보낸 메시지와 내용이 같으면 무시 (중복 방지) - 완화
      const isRecentSentMessage = recentSentMessages.current.some(
        sentMsg => sentMsg.content === messageContent.trim() && 
                   Date.now() - sentMsg.timestamp < 3000 // 3초로 단축
      )
      
      if (isRecentSentMessage) {
        console.log('🔄 중복 메시지 무시 (최근에 보낸 메시지):', messageContent)
        return
      }
    }
    
    console.log('👤 현재 사용자 정보:', {
      currentUser,
      currentUserName,
      messageSender,
      messageSenderId,
      isMyMessage
    })
    
    // 고유한 메시지 ID 생성 (내용 + 시간 + 발신자 기반)
    const uniqueId = `${messageSender}_${messageContent.trim()}_${parseSafeDate(messageTime).getTime()}`
    
    const newMsg = {
      id: messageId || uniqueId,
      sender: messageSender,
      content: messageContent.trim(),
      time: parseSafeDate(messageTime).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }),
      isMe: isMyMessage,
      timestamp: parseSafeDate(messageTime).getTime(), // 정렬을 위한 타임스탬프 추가
      uniqueId: uniqueId // 중복 체크용 고유 ID
    }

    // 현재 활성 채팅방의 ID 찾기
    const currentRoom = chatList.find(room => room.name === activeChat)
    const roomId = currentRoom?.id || activeChat
    
    console.log('🔍 채팅방 찾기:', {
      activeChat,
      chatListNames: chatList.map(room => room.name),
      currentRoom: currentRoom?.name,
      roomId,
      chatListIds: chatList.map(room => room.id)
    })
    
    // roomId가 유효하지 않으면 메시지 추가하지 않음
    if (!roomId || roomId === 'null' || roomId === 'undefined') {
      console.log('❌ 유효하지 않은 roomId:', roomId)
      
      // 채팅방이 선택되지 않은 상태에서 메시지가 오면 해당 채팅방을 자동 선택
      if (!activeChat && chatMessage.roomId) {
        const targetRoom = chatList.find(room => room.id === chatMessage.roomId)
        if (targetRoom) {
          console.log('🔄 채팅방 자동 선택:', targetRoom.name)
          setActiveChat(targetRoom.name)
          // 잠시 후 메시지 다시 처리
          setTimeout(() => {
            displayMessage(chatMessage)
          }, 100)
        } else {
          // 채팅방을 찾을 수 없으면 메시지를 해당 roomId로 저장
          console.log('💾 메시지 임시 저장:', chatMessage.roomId)
          setAllMessages(prev => ({
            ...prev,
            [chatMessage.roomId]: [...(prev[chatMessage.roomId] || []), newMsg],
          }))
        }
      }
      return
    }
    
    // 강화된 중복 체크 (ID, 내용, 시간, 발신자 모두 확인)
    setAllMessages((prev) => {
      const currentMessages = prev[roomId] || []
      
      // 여러 조건으로 중복 체크
      const existsById = currentMessages.some(msg => msg.id === newMsg.id)
      const existsByContent = currentMessages.some(msg => 
        msg.content === newMsg.content && 
        msg.sender === newMsg.sender && 
        Math.abs(msg.timestamp - newMsg.timestamp) < 5000 // 5초 이내
      )
      const existsByUniqueId = currentMessages.some(msg => msg.uniqueId === newMsg.uniqueId)
      
      const isDuplicate = existsById || existsByContent || existsByUniqueId
      
      console.log('🔍 강화된 메시지 중복 체크:', { 
        messageId: newMsg.id,
        uniqueId: newMsg.uniqueId,
        content: newMsg.content,
        sender: newMsg.sender,
        timestamp: newMsg.timestamp,
        existsById,
        existsByContent,
        existsByUniqueId,
        isDuplicate,
        currentMessagesCount: currentMessages.length,
        roomId
      })
      
      if (!isDuplicate) {
        console.log('✅ 새 메시지 추가:', newMsg)
        const newState = {
          ...prev,
          [roomId]: [...currentMessages, newMsg],
        }
        console.log('🔄 상태 업데이트 후:', {
          newRoomMessagesCount: newState[roomId]?.length,
          allMessagesKeys: Object.keys(newState)
        })
        return newState
      } else {
        console.log('❌ 중복 메시지 무시:', {
          reason: existsById ? 'ID 중복' : existsByContent ? '내용 중복' : '고유ID 중복',
          messageId: newMsg.id
        })
      }
      return prev
    })

    // 다른 사용자의 메시지인 경우 채팅방 목록의 lastMessage 업데이트
    if (!newMsg.isMe) {
      setChatList(prev => prev.map(room => 
        room.name === activeChat 
          ? { ...room, lastMessage: messageContent.trim(), time: newMsg.time }
          : room
      ))
    }
  }

  // 사용자 상태 처리
  const handleUserStatus = (statusMessage) => {
    if (statusMessage.type === 'JOIN') {
      setOnlineUsers(prev => [...prev, statusMessage.sender])
    } else if (statusMessage.type === 'LEAVE') {
      setOnlineUsers(prev => prev.filter(user => user !== statusMessage.sender))
    }
  }

  // 타이핑 상태 처리
  const handleTypingStatus = (typingMessage) => {
    if (typingMessage.type === 'TYPING') {
      setTypingUsers(prev => [...prev.filter(user => user !== typingMessage.sender), typingMessage.sender])
    } else if (typingMessage.type === 'STOP_TYPING') {
      setTypingUsers(prev => prev.filter(user => user !== typingMessage.sender))
    }
  }

  // 타이핑 상태 전송 (개선된 로직)
  const sendTypingStatus = (isTyping) => {
    if (stompClient.current && stompClient.current.connected && activeChat) {
      const currentRoom = chatList.find(room => room.name === activeChat)
      if (currentRoom) {
        // 현재 사용자 정보 가져오기
        const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')
        const currentUserName = currentUser.name || currentUser.username || "나"
        
        const typingMessage = {
          type: isTyping ? 'TYPING' : 'STOP_TYPING',
          sender: currentUserName,
          roomId: currentRoom.id
        }
        console.log('타이핑 상태 전송:', isTyping ? 'TYPING' : 'STOP_TYPING')
        stompClient.current.send("/app/chat.typing", {}, JSON.stringify(typingMessage))
      }
    }
  }

  // 타이핑 시작 처리
  const handleTypingStart = () => {
    if (!isTyping) {
      setIsTyping(true)
      sendTypingStatus(true)
    }
    
    // 기존 타이머 정리
    if (typingTimerRef.current) {
      clearTimeout(typingTimerRef.current)
    }
    
    // 3초 후 타이핑 중지
    typingTimerRef.current = setTimeout(() => {
      handleTypingStop()
    }, 3000)
  }

  // 타이핑 중지 처리
  const handleTypingStop = () => {
    if (isTyping) {
      setIsTyping(false)
      sendTypingStatus(false)
    }
    
    if (typingTimerRef.current) {
      clearTimeout(typingTimerRef.current)
      typingTimerRef.current = null
    }
  }

  // 컴포넌트 마운트 시 채팅방 목록 가져오기 및 sessionStorage에서 exitedAt 로드
  useEffect(() => {
    // sessionStorage에서 저장된 exitedAt 정보 로드
    const storedExitedAt = JSON.parse(sessionStorage.getItem('chatLastExitedAt') || '{}')
    const loadedExitedAt = {}
    
    Object.keys(storedExitedAt).forEach(roomId => {
      const exitedAtString = storedExitedAt[roomId]
      if (exitedAtString) {
        const exitedAt = new Date(exitedAtString)
        if (!isNaN(exitedAt.getTime())) {
          loadedExitedAt[roomId] = exitedAt
        }
      }
    })
    
    if (Object.keys(loadedExitedAt).length > 0) {
      console.log('📱 sessionStorage에서 exitedAt 로드:', loadedExitedAt)
      setLastExitedAt(loadedExitedAt)
    }
    
    const fetchChatRooms = async () => {
      try {
        // console.log("채팅방 목록 조회 시작")
        const response = await getChatRooms()
        // console.log("채팅방 목록 응답:", response)
        
        if (response.success && response.rooms) {
          const formattedRooms = response.rooms.map(room => {
            // lastMessage 검증 (빈 메시지만 필터링)
            let safeLastMessage = "새로운 채팅방입니다."
            if (room.lastMessage && room.lastMessage.trim() && room.lastMessage.trim().length > 0) {
              safeLastMessage = room.lastMessage.trim()
            }
            
            return {
              id: room.id,
              name: room.name,
              lastMessage: safeLastMessage,
              time: room.lastMessageTimestamp ? parseSafeDate(room.lastMessageTimestamp).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }) : null,
              unread: room.unread || 0,
              // type 필드 제거 (사용하지 않음)
            }
          })
          // console.log("포맷된 채팅방 목록:", formattedRooms)
          setChatList(formattedRooms)
        } else {
          // console.log("채팅방 목록이 비어있거나 응답 구조가 다름:", response)
          setChatList([])
        }
      } catch (error) {
        console.error("채팅방 목록 조회 중 오류:", error)
        // console.log("에러 상세:", error.response?.data || error.message)
        setChatList([])
      }
    }

    fetchChatRooms()
  }, [])

  // 채팅방 변경 시 WebSocket 연결 및 exitedAt 정보 로드
  useEffect(() => {
    console.log('🔄 useEffect 실행:', { activeChat, chatListLength: chatList.length })
    
    if (activeChat) {
      const currentRoom = chatList.find(room => room.name === activeChat)
      console.log('🔍 현재 채팅방 찾기:', { activeChat, currentRoom })
      
      if (currentRoom) {
        console.log('🔄 채팅방 변경 감지:', {
          activeChat,
          roomId: currentRoom.id,
          chatListLength: chatList.length
        })
        
        try {
          // 1. DB에서 마지막 exitedAt 정보 로드
          console.log('🔄 exitedAt 정보 로드 시작:', currentRoom.id)
          getLastExitedAtFromDB(currentRoom.id).then((exitedAt) => {
            console.log('✅ exitedAt 정보 로드 완료:', exitedAt?.toISOString())
          }).catch((error) => {
            console.error('❌ exitedAt 정보 로드 실패:', error)
          })
          
          // 2. 이전 메시지 로드
          fetchPreviousMessages(currentRoom.id)
          
          // 3. WebSocket 연결
          connectToChatRoom(currentRoom.id)
          
          // 주기적 새로고침 시작 (WebSocket 연결이 안 될 경우를 대비)
          if (refreshIntervalRef.current) {
            clearInterval(refreshIntervalRef.current)
          }
          refreshIntervalRef.current = setInterval(async () => {
            if (!isConnected) {
              console.log('🔄 WebSocket 연결 안됨, HTTP로 메시지 새로고침')
              await fetchPreviousMessages(currentRoom.id)
            }
          }, 3000) // 3초마다 새로고침
          
        } catch (error) {
          console.error("WebSocket 연결 실패:", error)
          setIsConnected(false)
          setIsLoading(false)
        }
      } else {
        console.log('❌ 채팅방을 찾을 수 없음:', activeChat)
        setIsConnected(false)
        setIsLoading(false)
      }
    } else {
      // 채팅방이 선택되지 않으면 연결 상태 초기화
      console.log('🔌 채팅방 선택 해제, 연결 해제')
      setIsConnected(false)
      setIsLoading(false)
      
      // WebSocket 연결 해제
      if (stompClient.current) {
        try {
          if (stompClient.current.connected) {
            stompClient.current.disconnect()
          }
        } catch (error) {
          console.error('❌ WebSocket 연결 해제 중 오류:', error)
        } finally {
          stompClient.current = null
        }
      }
      
      // 새로고침 인터벌 정리
      if (refreshIntervalRef.current) {
        clearInterval(refreshIntervalRef.current)
        refreshIntervalRef.current = null
      }
    }
  }, [activeChat, chatList, isConnected])

  // 메시지가 업데이트될 때 안 읽은 메시지부터 보여주기
  useEffect(() => {
    if (activeChat && messages.length > 0) {
      const currentRoom = chatList.find(room => room.name === activeChat)
      if (currentRoom) {
        const firstUnreadIndex = getFirstUnreadMessageIndex(currentRoom.id)
        const unreadCount = calculateUnreadCount(currentRoom.id)
        
        console.log('📖 안 읽은 메시지 정보:', {
          roomId: currentRoom.id,
          firstUnreadIndex,
          unreadCount,
          totalMessages: messages.length
        })
        
        // 안 읽은 메시지가 있으면 해당 위치로, 없으면 맨 아래로 스크롤
        setTimeout(() => {
          if (unreadCount > 0 && firstUnreadIndex < messages.length) {
            // 안 읽은 메시지가 시작되는 위치로 스크롤
            const messageElements = document.querySelectorAll('[data-message-index]')
            const targetElement = messageElements[firstUnreadIndex]
            if (targetElement) {
              targetElement.scrollIntoView({ behavior: 'smooth', block: 'start' })
              console.log('📍 안 읽은 메시지 위치로 스크롤 완료')
            }
          } else {
            // 안 읽은 메시지가 없으면 맨 아래로 스크롤
            scrollToBottom()
          }
        }, 500)
      }
    }
  }, [messages, activeChat])

  // 컴포넌트 언마운트 시 연결 해제 및 exitedAt 업데이트
  useEffect(() => {
    return () => {
      // 현재 활성 채팅방이 있으면 exitedAt 업데이트
      if (activeChat) {
        const currentRoom = chatList.find(room => room.name === activeChat)
        if (currentRoom) {
          console.log('🔄 컴포넌트 언마운트 시 exitedAt 업데이트:', currentRoom.id)
          updateExitedAt(currentRoom.id).catch(error => {
            console.error('❌ 컴포넌트 언마운트 시 exitedAt 업데이트 실패:', error)
          })
        }
      }
      
      // 인터벌 정리
      if (connectionCheckRef.current?.intervalId) {
        clearInterval(connectionCheckRef.current.intervalId)
      }
      if (typingTimerRef.current) {
        clearTimeout(typingTimerRef.current)
      }
      if (refreshIntervalRef.current) {
        clearInterval(refreshIntervalRef.current)
      }
      
      // WebSocket 연결 안전하게 해제
      if (stompClient.current) {
        try {
          if (stompClient.current.connected) {
            stompClient.current.disconnect()
          }
        } catch (error) {
          console.error('❌ WebSocket 연결 해제 중 오류:', error)
        } finally {
          stompClient.current = null
          setIsConnected(false)
          setIsLoading(false)
        }
      }
    }
  }, [activeChat, chatList])

  const handleSearch = async () => {
    // console.log("검색 시작:", { searchQuery })
    
    if (searchQuery.trim()) {
      try {
        // console.log("통합 검색 요청:", searchQuery)
        const data = await searchUsers(searchQuery)
        
        // console.log("검색 결과:", data)
        
        // 백엔드 응답 구조에 맞게 데이터 설정 및 중복 제거
        const results = data.success ? data.data
          .filter((user, index, self) => 
            // 같은 id를 가진 첫 번째 항목만 유지
            index === self.findIndex(u => u.id === user.id)
          ) : []
        // console.log("설정할 검색 결과:", results)
        setSearchResults(results)
      } catch (error) {
        console.error("검색 중 오류 발생:", error)
        // console.log("백엔드 연결 실패, 더미 데이터로 fallback")
        
        // console.log("백엔드 연결 실패")
        setSearchResults([])
      }
    } else {
      // console.log("검색어가 비어있음")
      setSearchResults([])
    }
  }

  const handleAddMember = (user) => {
    // ID로 중복 체크
    if (!selectedMembers.find((member) => member.id === user.id)) {
      setSelectedMembers([...selectedMembers, user])
    }
  }

  const handleRemoveMember = (userId) => {
    setSelectedMembers(selectedMembers.filter((member) => member.id !== userId))
  }

  const handleCreateChatRoom = () => {
    if (selectedMembers.length > 0) {
      setShowConfirmModal(true)
      // 기본 채팅방 이름 설정
      const memberNames = selectedMembers.map((member) => member.name).join(", ")
      setChatRoomName(memberNames.length > 30 ? `${memberNames.substring(0, 30)}...` : memberNames)
    }
  }

  const handleFinalCreateChatRoom = async () => {
    try {
      // 백엔드로 채팅방 생성 요청
      const chatRoomData = {
        name: chatRoomName,
        participantIds: selectedMembers.map(member => member.id)
      }
      
      console.log('채팅방 생성 요청:', chatRoomData)
      const response = await createChatRoom(chatRoomData)
      
      // 백엔드 응답에서 채팅방 정보 추출
      const roomData = response.success ? response.room : null
      
      if (roomData) {
        // 새 채팅방을 chatList에 추가
        const newChatRoom = {
          id: roomData.id,
          name: roomData.name,
          lastMessage: "채팅방이 생성되었습니다.",
          time: new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }),
          unread: 0,
          members: roomData.participants,
        }

              setChatList((prev) => [newChatRoom, ...prev])

        // 새로 생성된 채팅방을 활성화
        setActiveChat(roomData.name)

        console.log("최종 채팅방 생성:", {
          roomData: roomData,
          response: response
        })

        // 모든 상태 초기화
        resetModalStates()
      } else {
        console.error("채팅방 생성 실패: 응답 데이터 없음")
        alert("채팅방 생성에 실패했습니다.")
      }
    } catch (error) {
      console.error("채팅방 생성 중 오류 발생:", error)
      alert("채팅방 생성에 실패했습니다.")
    }
  }

  const resetModalStates = () => {
    setIsModalOpen(false)
    setShowConfirmModal(false)
    setSearchQuery("")
    setSearchResults([])
    setSelectedMembers([])
    setChatRoomName("")
    setSearchType("all")
  }

  // 새 메시지 전송 (WebSocket과 HTTP API 모두 사용)
  const handleSendMessage = async (e) => {
    e.preventDefault()
    if (newMessage.trim()) {
      const currentRoom = chatList.find(room => room.name === activeChat)
      if (!currentRoom) {
        console.log('❌ 현재 채팅방을 찾을 수 없음:', activeChat)
        return
      }

      // 현재 사용자 정보 가져오기
      const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')
      const currentUserName = currentUser.name || currentUser.username || "나"

      console.log('📝 메시지 전송 시작:', {
        message: newMessage,
        currentUser,
        currentUserName,
        currentRoom,
        activeChat
      })

      // 고유한 메시지 ID 생성
      const messageTimestamp = Date.now()
      const uniqueId = `${currentUserName}_${newMessage}_${messageTimestamp}`
      
      // 로컬에 메시지 추가 (즉시 표시)
      const newMsg = {
        id: uniqueId,
        sender: currentUserName,
        content: newMessage,
        time: new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }),
        isMe: true,
        timestamp: messageTimestamp, // 정렬을 위한 타임스탬프 추가
        uniqueId: uniqueId // 중복 체크용 고유 ID
      }
      
      console.log('💾 로컬 메시지 추가:', newMsg)
      
      // 최근 보낸 메시지 추적 (중복 방지용)
      recentSentMessages.current.push({
        content: newMessage,
        timestamp: messageTimestamp,
        uniqueId: uniqueId
      })
      
      // 10초 이전 메시지는 제거
      recentSentMessages.current = recentSentMessages.current.filter(
        msg => Date.now() - msg.timestamp < 10000
      )
      
      // 메시지 상태 업데이트
      setAllMessages((prev) => {
        const currentMessages = prev[currentRoom.id] || []
        const updatedMessages = [...currentMessages, newMsg]
        
        console.log('🔄 메시지 상태 업데이트:', {
          roomId: currentRoom.id,
          previousCount: currentMessages.length,
          newCount: updatedMessages.length,
          newMessage: newMsg
        })
        
        return {
          ...prev,
          [currentRoom.id]: updatedMessages,
        }
      })

      // 채팅방 목록의 lastMessage 업데이트
      setChatList(prev => prev.map(room => 
        room.name === activeChat 
          ? { ...room, lastMessage: newMessage, time: new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }) }
          : room
      ))

      // WebSocket과 HTTP API 모두 사용 (안정성 향상)
      const chatMessage = {
        content: newMessage,
        sender: currentUserName,
        senderId: currentUser.id || currentUserName,
        roomId: currentRoom.id,
        timestamp: new Date().toISOString()
      }

      console.log('📤 메시지 전송 시작:', chatMessage)
      
      // 1. WebSocket으로 전송 (실시간)
      if (isStompConnected()) {
        console.log('🔗 SockJS 연결 상태 확인:', {
          stompClient: !!stompClient.current,
          connected: stompClient.current?.connected,
          wsReadyState: stompClient.current?.ws?.readyState,
          roomId: currentRoom.id
        })
        
        try {
          stompClient.current.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage))
          console.log('✅ SockJS 메시지 전송 완료')
        } catch (error) {
          console.error('❌ SockJS 메시지 전송 실패:', error)
        }
      } else {
        console.log('❌ SockJS 연결 안됨')
        console.log('연결 상태:', { 
          stompClient: !!stompClient.current, 
          connected: stompClient.current?.connected,
          wsReadyState: stompClient.current?.ws?.readyState,
          activeChat,
          currentRoom: currentRoom?.id
        })
      }
      
      // 2. HTTP API로도 전송 (백업)
      try {
        console.log('📡 HTTP API로 메시지 전송 시도')
        const response = await sendMessage(currentRoom.id, {
          content: newMessage,
          sender: currentUserName,
          senderId: currentUser.id || currentUserName
        })
        console.log('✅ HTTP API 메시지 전송 완료:', response)
        
        // HTTP API로 전송 성공 시에는 새로고침하지 않음 (중복 방지)
        if (response.success) {
          console.log('✅ HTTP API 전송 성공, 새로고침 생략 (중복 방지)')
        }
      } catch (error) {
        console.error('❌ HTTP API 메시지 전송 실패:', error)
        alert('메시지 전송에 실패했습니다. 다시 시도해주세요.')
      }
      
      // 타이핑 상태 중지
      handleTypingStop()
      
      setNewMessage("")
    }
  }

  // 메시지 입력 시 타이핑 상태 전송 (개선된 로직)
  const handleMessageInput = (e) => {
    setNewMessage(e.target.value)
    handleTypingStart()
  }

  // 이모지 선택 처리
  const handleEmojiClick = (emojiObject) => {
    if (!emojiObject?.emoji) return; // 보호막
    setNewMessage(prev => prev + emojiObject.emoji)
    setShowEmojiPicker(false)
  }

  // 이모지 피커 토글
  const toggleEmojiPicker = () => {
    setShowEmojiPicker(prev => !prev)
  }


  const handleLeaveChatRoom = async () => {
    try {
      // 현재 활성 채팅방의 ID 찾기
      const currentRoom = chatList.find(room => room.name === activeChat)
      if (!currentRoom) {
        console.error("현재 채팅방을 찾을 수 없습니다.")
        return
      }

      // 1. exitedAt 업데이트 (채팅방을 나간 시간 기록)
      try {
        console.log('📝 exitedAt 업데이트 시작:', currentRoom.id)
        await updateExitedAt(currentRoom.id)
        console.log('✅ exitedAt 업데이트 완료')
      } catch (error) {
        console.error('❌ exitedAt 업데이트 실패:', error)
        // exitedAt 업데이트 실패해도 채팅방 나가기는 계속 진행
      }

      // 2. 채팅방 나가기
      await leaveChatRoom(currentRoom.id)
      
      // 채팅방 목록에서 제거
      setChatList(prev => prev.filter(room => room.id !== currentRoom.id))
      
      // 활성 채팅방 초기화
      setActiveChat(null)
      
      // 모달 닫기
      setShowLeaveConfirmModal(false)
      
      console.log("채팅방을 나갔습니다:", currentRoom.name)
    } catch (error) {
      console.error("채팅방 나가기 중 오류 발생:", error)
      alert("채팅방 나가기에 실패했습니다.")
    }
  }

  const handleShowParticipants = async () => {
    try {
      // 현재 활성 채팅방의 ID 찾기
      const currentRoom = chatList.find(room => room.name === activeChat)
      if (!currentRoom) {
        console.error("현재 채팅방을 찾을 수 없습니다.")
        return
      }

      const response = await getChatRoomParticipants(currentRoom.id)
      
      if (response.success && response.participants) {
        setParticipants(response.participants)
        setShowParticipantsModal(true)
      } else {
        console.error("참가자 목록을 가져올 수 없습니다.")
      }
    } catch (error) {
      console.error("참가자 목록 조회 중 오류 발생:", error)
      alert("참가자 목록을 불러오는데 실패했습니다.")
    }
  }

  // 사용자 추가 검색
  const handleAddUserSearch = async () => {
    if (addUserSearchQuery.trim()) {
      try {
        const data = await searchUsers(addUserSearchQuery)
        const results = data.success ? data.data.filter((user, index, self) => 
          index === self.findIndex(u => u.id === user.id)
        ) : []
        setAddUserSearchResults(results)
      } catch (error) {
        console.error("사용자 검색 중 오류 발생:", error)
        setAddUserSearchResults([])
      }
    } else {
      setAddUserSearchResults([])
    }
  }

  // 사용자 추가
  const handleAddUserToGroup = async () => {
    if (!selectedUserToAdd) {
      alert("추가할 사용자를 선택해주세요.")
      return
    }

    try {
      const currentRoom = chatList.find(room => room.name === activeChat)
      if (!currentRoom) {
        console.error("현재 채팅방을 찾을 수 없습니다.")
        return
      }

      const response = await addUserToGroup(currentRoom.id, selectedUserToAdd.id)
      
      if (response.success) {
        alert(`${selectedUserToAdd.name}님이 그룹에 추가되었습니다.`)
        
        // 참가자 목록 새로고침
        const participantsResponse = await getChatRoomParticipants(currentRoom.id)
        if (participantsResponse.success && participantsResponse.participants) {
          setParticipants(participantsResponse.participants)
        }
        
        // 모달 상태 초기화
        setShowAddUserModal(false)
        setAddUserSearchQuery("")
        setAddUserSearchResults([])
        setSelectedUserToAdd(null)
      } else {
        alert("사용자 추가에 실패했습니다: " + (response.message || "알 수 없는 오류"))
      }
    } catch (error) {
      console.error("사용자 추가 중 오류 발생:", error)
      alert("사용자 추가에 실패했습니다.")
    }
  }



  return (
    <PageLayout currentPage="chat" userRole="admin">
      <main className="p-4 pb-8 h-[calc(100vh-80px)]">
        <div className="max-w-7xl mx-auto h-full">
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-4 h-full">
            {/* 채팅 목록 */}
            <Card className="lg:col-span-1">
      {showEmojiPicker && (
        <EmojiPickerPortal
          onEmojiClick={handleEmojiClick}
          onClose={() => setShowEmojiPicker(false)}
        />
      )}
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle style={{ color: "#2C3E50" }}>메시지</CardTitle>
                  <Button
                    size="sm"
                    className="text-white"
                    style={{ backgroundColor: "#1ABC9C" }}
                    onClick={() => setIsModalOpen(true)}
                  >
                    <Plus className="w-4 h-4" />
                    채팅방 만들기
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="p-0">
                <div className="max-h-[calc(100vh-200px)] overflow-y-auto space-y-1">
                  {chatList.map((chat, index) => (
                    <div
                      key={index}
                      className={`p-3 cursor-pointer hover:bg-gray-50 border-b ${
                        activeChat === chat.name ? "bg-gray-100" : ""
                      }`}
                      onClick={async () => {
                        console.log('🔄 채팅방 클릭:', chat.name, chat.id)
                        
                        // 이미 선택된 채팅방이면 중복 처리 방지
                        if (activeChat === chat.name) {
                          console.log('🔄 이미 선택된 채팅방')
                          return
                        }
                        
                        setActiveChat(chat.name)
                        setIsLoading(true)
                        
                        // 채팅방 입장 시 안 읽은 메시지 카운트 초기화
                        setChatList(prev => prev.map(room => 
                          room.id === chat.id 
                            ? { ...room, unread: 0 }
                            : room
                        ))
                        
                        try {
                          // 1. HTTP API로 이전 메시지 로드
                          console.log('📚 이전 메시지 로드 시작')
                          await fetchPreviousMessages(chat.id)
                          console.log('✅ 이전 메시지 로드 완료')
                          
                          // 2. WebSocket 연결 (실시간 메시지 수신용)
                          console.log('🔌 WebSocket 연결 시작')
                          connectToChatRoom(chat.id)
                          console.log('✅ WebSocket 연결 요청 완료')
                        } catch (error) {
                          console.error('❌ 채팅방 입장 실패:', error)
                        } finally {
                          setIsLoading(false)
                        }
                      }}
                    >
                      <div className="flex justify-between items-start">
                        <div className="flex-1">
                          <div className="flex justify-between items-center mb-1">
                            <h4 className="font-medium text-sm" style={{ color: "#2C3E50" }}>
                              {chat.name}
                            </h4>
                            {chat.time && chat.time !== 'Invalid Date' && chat.time !== '오전 0:00' && chat.time !== '오후 0:00' ? (
                              <span className="text-xs" style={{ color: "#95A5A6" }}>
                                {chat.time}
                              </span>
                            ) : null}
                          </div>
                          <p className="text-xs truncate" style={{ color: "#95A5A6" }}>
                            {chat.lastMessage}
                          </p>
                        </div>
                        {(() => {
                          const unreadCount = calculateUnreadCount(chat.id)
                          return unreadCount > 0 ? (
                            <span
                              className="ml-2 px-2 py-1 rounded-full text-xs text-white"
                              style={{ backgroundColor: "#1ABC9C" }}
                            >
                              {unreadCount}
                            </span>
                          ) : null
                        })()}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* 채팅 화면 */}
            <Card className="lg:col-span-3 flex flex-col">
              {activeChat ? (
                <>
                  <CardHeader className="border-b">
                    <div className="flex items-center justify-between">
                      <CardTitle style={{ color: "#2C3E50" }}>{activeChat}</CardTitle>
                      <div className="flex items-center space-x-2">
                        <div className={`w-2 h-2 rounded-full ${isConnected ? "bg-green-500" : "bg-red-500"}`}></div>
                        <span className="text-sm" style={{ color: "#95A5A6" }}>
                          {isConnected ? "온라인" : "오프라인"}
                        </span>
                        {!isConnected && (
                          <span className="text-xs text-orange-500">
                            (HTTP 폴링 사용)
                          </span>
                        )}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={handleShowParticipants}
                          style={{ color: "#1ABC9C" }}
                          className="hover:bg-green-50"
                        >
                          <Users className="w-4 h-4 mr-1" />
                          참가자
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setShowLeaveConfirmModal(true)}
                          style={{ color: "#E74C3C" }}
                          className="hover:bg-red-50"
                        >
                          나가기
                        </Button>
                      </div>
                    </div>
                  </CardHeader>

                  {/* 로딩 화면 */}
                  {isLoading ? (
                    <div className="flex-1 flex items-center justify-center">
                      <div className="text-center">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500 mx-auto mb-4"></div>
                        <h3 className="text-lg font-medium mb-2" style={{ color: "#2C3E50" }}>
                          채팅방에 연결 중...
                        </h3>
                        <p className="text-sm" style={{ color: "#95A5A6" }}>
                          잠시만 기다려주세요
                        </p>
                      </div>
                    </div>
                  ) : (
                    <>
                      {/* 메시지 영역 */}
                      <CardContent 
                        className="flex-1 overflow-y-auto p-4 max-h-[calc(100vh-500px)]"
                        onScroll={(e) => {
                          const currentRoom = chatList.find(room => room.name === activeChat)
                          if (currentRoom) {
                            const container = e.target
                            const scrollTop = container.scrollTop
                            const scrollHeight = container.scrollHeight
                            const clientHeight = container.clientHeight
                            
                            // 스크롤 위치에 따라 현재 읽고 있는 메시지 인덱스 계산
                            const messageElements = container.querySelectorAll('[data-message-index]')
                            let currentReadIndex = 0
                            
                            messageElements.forEach((element, index) => {
                              const rect = element.getBoundingClientRect()
                              const containerRect = container.getBoundingClientRect()
                              
                              // 메시지가 화면 중앙에 가까우면 해당 인덱스로 설정
                              if (rect.top <= containerRect.top + containerRect.height / 2) {
                                currentReadIndex = index
                              }
                            })
                            
                            // 현재 읽고 있는 위치 로그 (디버깅용)
                            console.log('📖 현재 읽고 있는 메시지 인덱스:', currentReadIndex)
                          }
                        }}
                      >
                        <div className="space-y-4">
                          {(() => {
                            // 중복 제거를 위한 필터링
                            const uniqueMessages = messages.reduce((acc, message) => {
                              const key = `${message.sender}_${message.content}_${message.timestamp}`
                              if (!acc.some(existing => 
                                existing.sender === message.sender && 
                                existing.content === message.content && 
                                Math.abs(existing.timestamp - message.timestamp) < 5000
                              )) {
                                acc.push(message)
                              }
                              return acc
                            }, [])
                            
                            const filteredMessages = uniqueMessages.filter(message => 
                              message.content && 
                              message.content.trim() && 
                              message.content.trim().length > 0
                            )
                            
                            console.log('🎨 메시지 렌더링:', {
                              totalMessages: messages.length,
                              uniqueMessages: uniqueMessages.length,
                              filteredMessages: filteredMessages.length,
                              roomId: roomId,
                              activeChat: activeChat,
                              allMessagesKeys: Object.keys(allMessages)
                            })
                            
                            if (filteredMessages.length === 0) {
                              console.log('⚠️ 필터링된 메시지가 없음')
                            }
                            
                            return filteredMessages.map((message, index) => {
                              const currentRoom = chatList.find(room => room.name === activeChat)
                              
                              // filteredMessages 기준으로 안 읽은 메시지 인덱스 계산
                              const lastExitedTime = currentRoom ? lastExitedAt[currentRoom.id] : null
                              const firstUnreadIndex = lastExitedTime ? 
                                filteredMessages.findIndex(msg => {
                                  const messageTime = new Date(msg.timestamp || msg.time)
                                  return messageTime > lastExitedTime
                                }) : -1
                              
                              const showDivider = index === firstUnreadIndex && shouldShowUnreadDivider(currentRoom?.id)
                              
                              // 디버깅: 구분선 표시 조건 확인
                              if (index < 5) {
                                console.log('🔍 구분선 디버깅:', {
                                  messageIndex: index,
                                  firstUnreadIndex,
                                  showDivider,
                                  currentRoomId: currentRoom?.id,
                                  shouldShowDivider: shouldShowUnreadDivider(currentRoom?.id),
                                  unreadCount: calculateUnreadCount(currentRoom?.id),
                                  lastExitedTime: lastExitedTime?.toISOString(),
                                  filteredMessagesCount: filteredMessages.length
                                })
                              }
                              
                              return (
                                <div key={message.id}>
                                  {/* 안 읽은 메시지 구분선 */}
                                  {showDivider && (
                                    <div className="flex items-center justify-center my-4">
                                      <div className="flex-1 h-px bg-gray-300"></div>
                                      <div className="px-3 py-1 mx-2 bg-red-100 text-red-600 text-xs font-medium rounded-full">
                                        📖 안 읽은 메시지
                                      </div>
                                      <div className="flex-1 h-px bg-gray-300"></div>
                                    </div>
                                  )}
                                  
                                  <div 
                                    className={`flex ${message.isMe ? "justify-end" : "justify-start"}`}
                                    data-message-index={index}
                                  >
                                <div className={`max-w-xs lg:max-w-md ${message.isMe ? "order-2" : "order-1"}`}>
                                  {!message.isMe && (
                                    <p className="text-xs mb-1" style={{ color: "#95A5A6" }}>
                                      {message.sender}
                                    </p>
                                  )}
                                  <div
                                    className={`px-4 py-2 rounded-lg ${message.isMe ? "text-white" : "bg-gray-100"}`}
                                    style={{
                                      backgroundColor: message.isMe ? "#1ABC9C" : "#f1f2f6",
                                      color: message.isMe ? "white" : "#2C3E50",
                                    }}
                                  >
                                    <p className="text-sm">{message.content}</p>
                                  </div>
                                  <p
                                    className={`text-xs mt-1 ${message.isMe ? "text-right" : "text-left"}`}
                                    style={{ color: "#95A5A6" }}
                                  >
                                    {message.time && message.time !== 'Invalid Date' ? message.time : new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
                                  </p>
                                </div>
                              </div>
                                </div>
                              )
                            })
                          })()}
                          <div ref={messagesEndRef} />
                        </div>
                        
                        {/* 타이핑 상태 표시 */}
                        {typingUsers.length > 0 && (
                          <div className="flex items-center space-x-2 text-sm" style={{ color: "#95A5A6" }}>
                            <div className="flex space-x-1">
                              <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                              <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
                              <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                            </div>
                            <span>{typingUsers.join(', ')}님이 입력 중...</span>
                          </div>
                        )}
                      </CardContent>

                      {/* 안 읽은 메시지 버튼 */}
                      {(() => {
                        const currentRoom = chatList.find(room => room.name === activeChat)
                        const unreadCount = currentRoom ? calculateUnreadCount(currentRoom.id) : 0
                        
                        if (unreadCount > 0) {
                          return (
                            <div className="border-t p-4 pb-6 bg-gray-50">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => {
                                  if (currentRoom) {
                                    const firstUnreadIndex = getFirstUnreadMessageIndex(currentRoom.id)
                                    console.log('📖 안 읽은 메시지로 이동:', { firstUnreadIndex, unreadCount })
                                    
                                    if (firstUnreadIndex < messages.length) {
                                      const messageElements = document.querySelectorAll('[data-message-index]')
                                      const targetElement = messageElements[firstUnreadIndex]
                                      if (targetElement) {
                                        targetElement.scrollIntoView({ behavior: 'smooth', block: 'start' })
                                      }
                                    } else {
                                      scrollToBottom()
                                    }
                                  }
                                }}
                                className="w-full text-sm"
                                style={{ borderColor: "#1ABC9C", color: "#1ABC9C" }}
                              >
                                📖 안 읽은 메시지 {unreadCount}개 보기
                              </Button>
                            </div>
                          )
                        }
                        return null
                      })()}

                      {/* 메시지 입력 영역 */}
                      <div className="border-t p-6 pb-8 relative">
                        <form onSubmit={handleSendMessage} className="flex items-center space-x-2">
                          <Button 
                            type="button" 
                            variant="ghost" 
                            size="sm"
                            onClick={toggleEmojiPicker}
                          >
                            <Smile className="w-4 h-4" style={{ color: "#95A5A6" }} />
                          </Button>
                          <Input
                            placeholder="메시지를 입력하세요..."
                            value={newMessage}
                            onChange={handleMessageInput}
                            className="flex-1 selection:bg-blue-200 selection:text-blue-900"
                          />
                          <Button 
                            type="submit" 
                            size="sm" 
                            className="text-white" 
                            style={{ backgroundColor: "#1ABC9C" }}
                            disabled={!newMessage.trim()}
                          >
                            <Send className="w-4 h-4" />
                          </Button>
                        </form>
                      </div>
                    </>
                  )}
                </>
              ) : (
                /* 채팅방을 선택하지 않았을 때 표시할 안내 화면 */
                <div className="flex-1 flex items-center justify-center">
                  <div className="text-center">
                    <MessageSquare className="w-16 h-16 mx-auto mb-4" style={{ color: "#95A5A6" }} />
                    <h3 className="text-lg font-medium mb-2" style={{ color: "#2C3E50" }}>
                      채팅방을 선택해주세요
                    </h3>
                    <p className="text-sm" style={{ color: "#95A5A6" }}>
                      왼쪽에서 채팅방을 선택하거나 새로운 채팅방을 만들어보세요
                    </p>
                  </div>
                </div>
              )}
            </Card>
          </div>
        </div>

        {/* 채팅방 만들기 모달 */}
        {isModalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <Card className="w-full max-w-md mx-4">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle style={{ color: "#2C3E50" }}>새 채팅방 만들기</CardTitle>
                  <Button variant="ghost" size="sm" onClick={resetModalStates} style={{ color: "#95A5A6" }}>
                    ✕
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                    사용자 검색
                  </label>
                  
                  <div className="flex space-x-2">
                    <Input
                      placeholder="이름 또는 ID로 검색"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      onKeyPress={(e) => e.key === "Enter" && handleSearch()}
                      className="flex-1 selection:bg-blue-200 selection:text-blue-900 mt-2"
                    />
                    <Button
                      onClick={handleSearch}
                      size="sm"
                      className="mt-2 text-white bg-[#1abc9c] transition-all duration-200 active:scale-95 hover:bg-[rgb(10,150,120)]"
                    >
                      검색
                    </Button>
                  </div>
                </div>

                {/* 검색 결과 */}
                {searchResults.length > 0 && (
                  <div className="space-y-2">
                    <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                      검색 결과
                    </label>
                    <div className="max-h-48 overflow-y-auto space-y-1 mt-2">
                      {searchResults.map((user) => (
                        <div
                          key={user.id}
                          className="flex items-center justify-between p-2 border rounded hover:bg-gray-50"
                        >
                          <div>
                            <p className="font-medium text-sm" style={{ color: "#2C3E50" }}>
                              {user.name}
                            </p>
                            <p className="text-xs" style={{ color: "#95A5A6" }}>
                              {user.email || user.id}
                            </p>
                          </div>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-[#1abc9c] bg-white border border-[#1abc9c]
                            hover:text-white hover:bg-[#1abc9c] "
                            onClick={() => handleAddMember(user)}
                          >
                            추가
                          </Button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {searchResults.length === 0 && (
                  <div className="text-center py-4">
                    <p className="text-sm" style={{ color: "#95A5A6" }}>
                      검색 결과가 없습니다.
                    </p>
                  </div>
                )}

                {/* 추가된 멤버 섹션 */}
                {selectedMembers.length > 0 && (
                  <div className="space-y-2">
                    <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                      추가된 멤버 ({selectedMembers.length}명)
                    </label>
                    <div className="max-h-32 overflow-y-auto space-y-1">
                      {selectedMembers.map((member) => (
                        <div
                          key={member.id}
                          className="flex items-center justify-between p-2 border rounded bg-gray-50"
                        >
                          <div>
                            <p className="font-medium text-sm" style={{ color: "#2C3E50" }}>
                              {member.name}
                            </p>
                            <p className="text-xs" style={{ color: "#95A5A6" }}>
                              {member.email || member.id}
                            </p>
                          </div>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-[#95A5A6] bg-white border border-[#95A5A6] hover:bg-gray-100"
                            onClick={() => handleRemoveMember(member.id)}
                          >
                            제거
                          </Button>
                        </div>
                      ))}
                    </div>
                    <Button
                      className="w-full text-white font-medium bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                      onClick={handleCreateChatRoom}
                    >
                      채팅방 만들기 ({selectedMembers.length}명)
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        )}

        {/* 채팅방 생성 확인 모달 */}
        {showConfirmModal && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <Card className="w-full max-w-md mx-4">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle style={{ color: "#2C3E50" }}>채팅방 생성 확인</CardTitle>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={resetModalStates}
                    style={{ color: "#95A5A6" }}
                  >
                    ✕
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                    채팅방 이름
                  </label>
                  <Input
                    placeholder="채팅방 이름을 입력하세요"
                    value={chatRoomName}
                    onChange={(e) => setChatRoomName(e.target.value)}
                    className="w-full selection:bg-blue-200 selection:text-blue-900 mt-2"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                    참여 멤버 ({selectedMembers.length}명)
                  </label>
                  <div className="max-h-40 overflow-y-auto space-y-1 p-2 border rounded bg-gray-50 mt-2">
                    {selectedMembers.map((member) => (
                      <div key={member.id} className="flex items-center space-x-2 p-1">
                        <div
                          className="w-8 h-8 rounded-full flex items-center justify-center text-xs text-white font-medium"
                          style={{ backgroundColor: "#1ABC9C" }}
                        >
                          {member.name.charAt(0)}
                        </div>
                        <div>
                          <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            {member.name}
                          </p>
                          <p className="text-xs" style={{ color: "#95A5A6" }}>
                            {member.email || member.id} • {member.role}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="flex space-x-2 pt-4">
                  <Button
                    variant="outline"
                    className="flex-1 bg-transparent
                    text-[#95A5A6] bg-white border border-[#95A5A6] hover:bg-gray-100"
                    onClick={resetModalStates}
                  >
                    취소
                  </Button>
                  <Button
                    className="flex-1 text-white font-medium
                    text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                    onClick={handleFinalCreateChatRoom}
                    disabled={!chatRoomName.trim()}
                  >
                    채팅방 생성
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        )}

        {/* 참가자 목록 모달 */}
        {showParticipantsModal && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <Card className="w-full max-w-md mx-4">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle style={{ color: "#2C3E50" }}>채팅방 참가자</CardTitle>
                  <div className="flex items-center space-x-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setShowAddUserModal(true)}
                      style={{ borderColor: "#1ABC9C", color: "#1ABC9C" }}
                    >
                      + 사용자 추가
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setShowParticipantsModal(false)}
                      style={{ color: "#95A5A6" }}
                    >
                      ✕
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="max-h-96 overflow-y-auto space-y-2">
                  {participants.length > 0 ? (
                    participants.map((participant) => (
                      <div
                        key={participant.id}
                        className="flex items-center space-x-3 p-3 border rounded-lg hover:bg-gray-50"
                      >
                        <div
                          className="w-10 h-10 rounded-full flex items-center justify-center text-sm font-medium text-white"
                          style={{ backgroundColor: "#1ABC9C" }}
                        >
                          {participant.name.charAt(0)}
                        </div>
                        <div className="flex-1">
                          <p className="font-medium text-sm" style={{ color: "#2C3E50" }}>
                            {participant.name}
                          </p>
                          <p className="text-xs" style={{ color: "#95A5A6" }}>
                            {participant.email || participant.id}
                          </p>
                        </div>
                        {participant.isOnline && (
                          <div className="w-2 h-2 rounded-full" style={{ backgroundColor: "#1ABC9C" }}></div>
                        )}
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-8">
                      <p className="text-sm" style={{ color: "#95A5A6" }}>
                        참가자가 없습니다.
                      </p>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        )}

        {/* 사용자 추가 모달 */}
        {showAddUserModal && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <Card className="w-full max-w-md mx-4">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle style={{ color: "#2C3E50" }}>사용자 추가</CardTitle>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setShowAddUserModal(false)
                      setAddUserSearchQuery("")
                      setAddUserSearchResults([])
                      setSelectedUserToAdd(null)
                    }}
                    style={{ color: "#95A5A6" }}
                  >
                    ✕
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                    사용자 검색
                  </label>
                  
                  <div className="flex space-x-2">
                    <Input
                      placeholder="이름 또는 ID로 검색"
                      value={addUserSearchQuery}
                      onChange={(e) => setAddUserSearchQuery(e.target.value)}
                      onKeyPress={(e) => e.key === "Enter" && handleAddUserSearch()}
                      className="flex-1 selection:bg-blue-200 selection:text-blue-900"
                    />
                    <Button
                      onClick={handleAddUserSearch}
                      size="sm"
                      className="text-white transition-all duration-200 hover:scale-105 active:scale-95 hover:shadow-lg"
                      style={{ backgroundColor: "#1ABC9C" }}
                    >
                      검색
                    </Button>
                  </div>
                </div>

                {/* 검색 결과 */}
                {addUserSearchResults.length > 0 && (
                  <div className="space-y-2">
                    <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                      검색 결과
                    </label>
                    <div className="max-h-48 overflow-y-auto space-y-1">
                      {addUserSearchResults.map((user) => (
                        <div
                          key={user.id}
                          className={`flex items-center justify-between p-2 border rounded cursor-pointer ${
                            selectedUserToAdd?.id === user.id ? 'bg-green-50 border-green-300' : 'hover:bg-gray-50'
                          }`}
                          onClick={() => setSelectedUserToAdd(user)}
                        >
                          <div>
                            <p className="font-medium text-sm" style={{ color: "#2C3E50" }}>
                              {user.name}
                            </p>
                            <p className="text-xs" style={{ color: "#95A5A6" }}>
                              {user.email || user.id}
                            </p>
                          </div>
                          {selectedUserToAdd?.id === user.id && (
                            <div className="text-green-500">✓</div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {addUserSearchResults.length === 0 && addUserSearchQuery && (
                  <div className="text-center py-4">
                    <p className="text-sm" style={{ color: "#95A5A6" }}>
                      검색 결과가 없습니다.
                    </p>
                  </div>
                )}

                {/* 선택된 사용자 표시 */}
                {selectedUserToAdd && (
                  <div className="space-y-2">
                    <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                      추가할 사용자
                    </label>
                    <div className="p-3 border rounded bg-green-50">
                      <p className="font-medium text-sm" style={{ color: "#2C3E50" }}>
                        {selectedUserToAdd.name}
                      </p>
                      <p className="text-xs" style={{ color: "#95A5A6" }}>
                        {selectedUserToAdd.email || selectedUserToAdd.id}
                      </p>
                    </div>
                  </div>
                )}

                <div className="flex space-x-2 pt-4">
                  <Button
                    variant="outline"
                    className="flex-1 bg-transparent"
                    onClick={() => {
                      setShowAddUserModal(false)
                      setAddUserSearchQuery("")
                      setAddUserSearchResults([])
                      setSelectedUserToAdd(null)
                    }}
                    style={{ borderColor: "#95A5A6", color: "#95A5A6" }}
                  >
                    취소
                  </Button>
                  <Button
                    className="flex-1 text-white font-medium"
                    style={{ backgroundColor: "#1ABC9C" }}
                    onClick={handleAddUserToGroup}
                    disabled={!selectedUserToAdd}
                  >
                    추가
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        )}

        {/* 채팅방 나가기 확인 모달 */}
        {showLeaveConfirmModal && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <Card className="w-full max-w-md mx-4">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle style={{ color: "#2C3E50" }}>채팅방 나가기</CardTitle>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setShowLeaveConfirmModal(false)}
                    style={{ color: "#95A5A6" }}
                  >
                    ✕
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="text-center">
                  <p className="text-sm" style={{ color: "#2C3E50" }}>
                    <strong>{activeChat}</strong> 채팅방을 나가시겠습니까?
                  </p>
                  <p className="text-xs mt-2" style={{ color: "#95A5A6" }}>
                    채팅방을 나가면 다시 들어올 수 없습니다.
                  </p>
                </div>

                <div className="flex space-x-2 pt-4">
                  <Button
                    variant="outline"
                    className="flex-1 bg-transparent
                    text-[#95A5A6] bg-white border border-[#95A5A6] hover:bg-gray-100"
                    onClick={() => setShowLeaveConfirmModal(false)}
                  >
                    취소
                  </Button>
                  <Button
                    className="flex-1 text-white font-medium
                    bg-[#E74C3C] hover:bg-red-600"
                    onClick={handleLeaveChatRoom}
                  >
                    나가기
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        )}
      </main>
    </PageLayout>
  )
}
