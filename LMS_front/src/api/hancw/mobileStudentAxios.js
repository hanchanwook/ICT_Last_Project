import { http } from '@/components/auth/http'

// ===== 모바일 학생 관련 API (통합) =====

// 1. 모바일 학생 로그인
export const mobileStudentLogin = async (loginData) => {
  try {
    console.log('�� 모바일 학생 로그인 요청:', { 
      username: loginData.username,
      hasAttendanceHistory: !!loginData.attendanceHistory,
      hasCheckoutHistory: !!loginData.checkoutHistory,
      sessionId: loginData.sessionId
    })
    
    const response = await http.post('/api/auth/mobile/login', {
      username: loginData.username,
      password: loginData.password,
      // 출석 이력 데이터 추가
      attendanceHistory: loginData.attendanceHistory,
      checkoutHistory: loginData.checkoutHistory,
      sessionId: loginData.sessionId
    })
    
    console.log('📱 모바일 학생 로그인 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('📱 모바일 학생 로그인 실패:', error)
    
    // 에러 타입별 처리
    if (error.response?.status === 401) {
      throw new Error('아이디 또는 비밀번호가 올바르지 않습니다.')
    } else if (error.response?.status === 403) {
      throw new Error('학생 계정만 로그인 가능합니다.')
    } else if (error.response?.status === 404) {
      throw new Error('존재하지 않는 계정입니다.')
    } else if (error.response?.status >= 500) {
      throw new Error('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
    } else if (!error.response) {
      throw new Error('네트워크 연결을 확인해주세요.')
    } else {
      throw new Error('로그인 중 오류가 발생했습니다.')
    }
  }
}

// 2. 모바일 로그아웃
export const mobileLogout = async () => {
  try {
    const response = await http.post('/api/auth/mobile/logout')
    console.log('📱 모바일 로그아웃 성공')
    return response.data
  } catch (error) {
    console.error('📱 모바일 로그아웃 실패:', error)
    throw error
  }
}

// 3. 모바일 로그인 상태 확인
export const checkMobileLoginStatus = async () => {
  try {
    const response = await http.get('/api/auth/mobile/status')
    return response.data
  } catch (error) {
    console.error('📱 모바일 로그인 상태 확인 실패:', error)
    throw error
  }
}

// 4. 학생 정보 조회
export const getStudentInfo = async (memberId) => {
  try {
    const endpoint = memberId ? `/api/student/info/${memberId}` : '/api/student/info'
    const response = await http.get(endpoint)
    return response.data
  } catch (error) {
    console.error('📱 학생 정보 조회 실패:', error)
    throw error
  }
}

// 5. QR 코드 데이터 검증
export const validateQRData = async (qrData) => {
  try {
    console.log('📱 QR 데이터 검증 요청:', qrData)
    const response = await http.post('/api/student/qr/validate', { qrData })
    console.log('📱 QR 데이터 검증 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('QR 데이터 검증 실패:', error)
    throw error
  }
}

// 6. 출석 처리
export const submitAttendance = async (attendanceData) => {
  try {
    console.log('📱 출석 처리 요청:', attendanceData)
    const response = await http.post('/api/student/qr/check-in', attendanceData)
    console.log('📱 출석 처리 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('출석 처리 실패:', error)
    throw error
  }
}

// 7. QR 세션 상태 확인
export const checkQRSessionStatus = async (sessionId) => {
  try {
    const response = await http.get(`/api/student/qr/session/${sessionId}/status`)
    return response.data
  } catch (error) {
    console.error('QR 세션 상태 확인 실패:', error)
    throw error
  }
} 

// 8. 학생별 오늘 출석 상태 조회
export const getTodayAttendanceStatus = async (userId) => {
  try {
    console.log('📱 오늘 출석 상태 조회 요청:', userId)
    const response = await http.get(`/api/student/qr/status/${userId}`)
    console.log('📱 오늘 출석 상태 조회 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('📱 오늘 출석 상태 조회 실패:', error)
    
    // 에러 타입별 처리
    if (error.response?.status === 404) {
      throw new Error('출석 기록을 찾을 수 없습니다.')
    } else if (error.response?.status >= 500) {
      throw new Error('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
    } else if (!error.response) {
      throw new Error('네트워크 연결을 확인해주세요.')
    } else {
      throw new Error('출석 상태 조회 중 오류가 발생했습니다.')
    }
  }
}

// 9. QR 퇴실 처리
export const submitCheckOut = async (checkoutData) => {
  try {
    console.log('📱 QR 퇴실 처리 요청:', checkoutData)
    const response = await http.post('/api/student/qr/checkout', checkoutData)
    console.log('📱 QR 퇴실 처리 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('📱 QR 퇴실 처리 실패:', error)
    
    // 에러 타입별 처리
    if (error.response?.status === 400) {
      const errorMessage = error.response.data?.message || '퇴실 처리에 실패했습니다.'
      throw new Error(errorMessage)
    } else if (error.response?.status === 404) {
      throw new Error('QR 세션을 찾을 수 없습니다.')
    } else if (error.response?.status >= 500) {
      throw new Error('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
    } else if (!error.response) {
      throw new Error('네트워크 연결을 확인해주세요.')
    } else {
      throw new Error('퇴실 처리 중 오류가 발생했습니다.')
    }
  }
} 