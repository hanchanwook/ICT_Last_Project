import { http } from '@/components/auth/http'

// 통합 검색 API
export const searchUsers = async (query) => {
  try {
    const response = await http.get(`/api/chat/users/search?keyword=${query}`)
    return response.data
  } catch (error) {
    throw error
  }
}

// 채팅방 생성 API
export const createChatRoom = async (chatRoomData) => {
  try {
    const response = await http.post('/api/chat/rooms', {
      name: chatRoomData.name,
      participantIds: chatRoomData.participantIds
    })
    return response.data
  } catch (error) {
    throw error
  }
}

// 채팅방 목록 조회 API
export const getChatRooms = async () => {
  try {
    const response = await http.get('/api/chat/rooms')
    return response.data
  } catch (error) {
    throw error
  }
}

// 메시지 전송 API
export const sendMessage = async (roomId, messageData) => {
  try {
    const response = await http.post(`/api/chat/rooms/${roomId}/messages`, messageData)
    return response.data
  } catch (error) {
    throw error
  }
}

// 채팅방 메시지 조회 API
export const getChatMessages = async (roomId) => {
  try {
    const response = await http.get(`/api/chat/rooms/${roomId}/messages`)
    return response.data
  } catch (error) {
    throw error
  }
}

// 채팅방 나가기 API
export const leaveChatRoom = async (roomId) => {
  try {
    const response = await http.post(`/api/chat/rooms/${roomId}/leave`)
    return response.data
  } catch (error) {
    throw error
  }
}

// 채팅방 참가자 목록 조회 API
export const getChatRoomParticipants = async (roomId) => {
  try {
    const response = await http.get(`/api/chat/rooms/${roomId}/participants`)
    return response.data
  } catch (error) {
    throw error
  }
}

// 사용자의 마지막 exitedAt 시간 조회 API
export const getLastExitedAt = async (roomId) => {
  try {
    const response = await http.get(`/api/chat/rooms/${roomId}/last-exited-at`)
    return response.data
  } catch (error) {
    throw error
  }
}

// 사용자의 exitedAt 시간 업데이트 API
export const updateExitedAt = async (roomId) => {
  try {
    const response = await http.post(`/api/chat/rooms/${roomId}/update-exited-at`)
    return response.data
  } catch (error) {
    throw error
  }
}

// 그룹에 사용자 추가 API
export const addUserToGroup = async (roomId, userId) => {
  try {
    const response = await http.post(`/api/chat/rooms/${roomId}/add-user`, {
      userId: userId
    })
    return response.data
  } catch (error) {
    throw error
  }
}





