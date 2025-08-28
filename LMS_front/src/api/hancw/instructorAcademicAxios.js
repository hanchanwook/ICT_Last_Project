import { http } from '@/components/auth/http'

// ===== 강사 담당 학생 관리 =====

// 1. 강사가 담당하는 학생 목록 조회
export const getInstructorStudents = async (userId, params = {}) => {
  try {
    // http.jsx에서 자동으로 userId를 추가하므로 instructorId 제거
    const response = await http.get('/api/instructor/students', { 
      params: { ...params }
    })
    return response.data
  } catch (error) {
    console.error('담당 학생 목록 조회 실패:', error)
    throw error
  }
}

// 2. 학생 상세 정보 조회
export const getStudentDetail = async (userId, instructorId) => {
  try {
    console.log('강사 학생 상세 정보 조회 - userId:', userId, 'instructorId:', instructorId)
    // 현재 로그인한 사용자 정보 가져오기
    const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')
    const memberId = currentUser.memberId
    
    const response = await http.get(`/api/instructor/students/${userId}`, {
      params: { instructorId: memberId }
    })
    console.log('강사 학생 상세 정보 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('학생 상세 정보 조회 실패:', error)
    throw error
  }
}

// 3. 특정 학생 성적 조회
export const getStudentGrades = async (userId, params = {}) => {
  try {
    console.log('강사 학생 성적 조회 - userId:', userId, 'params:', params)
    const response = await http.get(`/api/instructor/students/grades`, { 
      params: { userId, ...params }
    })
    console.log('강사 학생 성적 조회 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('학생 성적 조회 실패:', error)
    throw error
  }
}



// ===== 출석 현황 관리 =====

// 4. 강사 담당 과정별 출석 현황 조회
export const getAttendanceStatus = async (userId, params = {}) => {
  try {
    console.log('📤 출석 현황 조회 요청 - userId:', userId, 'params:', params)
    const response = await http.get(`/api/instructor/attendance/status?userId=${userId}`, { 
      params: { ...params }
    })
    console.log('📥 출석 현황 조회 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('출석 현황 조회 실패:', error)
    throw error
  }
}

// 6. 출석 데이터 엑셀 내보내기
export const exportAttendanceData = async (params = {}) => {
  try {
    const response = await http.get('/api/instructor/attendance/export', { 
      params,
      responseType: 'blob'
    })
    return response.data
  } catch (error) {
    console.error('출석 데이터 내보내기 실패:', error)
    throw error
  }
}

// ===== QR 출석 관리 =====

// 7. QR 출석 세션 생성
export const createQRSession = async (sessionData) => {
  try {
    console.log('📤 QR 세션 생성 요청:', sessionData)
    const response = await http.post('/api/instructor/qr/session', sessionData)
    console.log('📥 QR 세션 생성 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('QR 세션 생성 실패:', error)
    throw error
  }
}
/*
// 8. QR 출석 세션 조회
export const getQRSession = async (sessionId) => {
  try {
    console.log('🔍 QR 세션 조회:', sessionId)
    const response = await http.get(`/api/instructor/qr/session/${sessionId}`)
    console.log('📋 QR 세션 정보:', response.data)
    return response.data
  } catch (error) {
    console.error('QR 세션 조회 실패:', error)
    throw error
  }
}

// 9. QR 출석 세션 종료
export const endQRSession = async (sessionId) => {
  try {
    const response = await http.put(`/api/instructor/qr/session/${sessionId}/end`)
    return response.data
  } catch (error) {
    console.error('QR 세션 종료 실패:', error)
    throw error
  }
}
*/
// 10. 특정 강의실의 QR 출석 현황 조회 (classroomId 기반)
export const getClassroomQRAttendance = async (classroomId, params = {}) => {
  try {
    const response = await http.get(`/api/instructor/qr/classroom/${classroomId}/attendance`, { params })
    return response.data
  } catch (error) {
    console.error('강의실 QR 출석 현황 조회 실패:', error)
    throw error
  }
}

// ===== 통계 및 대시보드 =====

// 11. 강사 대시보드 통계 조회
export const getInstructorDashboard = async (userId) => {
  try {
    const response = await http.get('/api/instructor/dashboard/academic')
    return response.data
  } catch (error) {
    console.error('강사 대시보드 조회 실패:', error)
    throw error
  }
}


// 12. 강의실별 통계 조회
export const getClassroomStatistics = async (classroomId) => {
  try {
    const response = await http.get(`/api/instructor/classrooms/${classroomId}/statistics`)
    return response.data
  } catch (error) {
    console.error('강의실 통계 조회 실패:', error)
    throw error
  }
}



// 13. 특정 학생의 출석 이력 조회
export const getStudentAttendanceHistory = async (userId, params = {}) => {
  try {
    console.log('강사 학생 출석 이력 조회 - userId:', userId)
    const response = await http.get(`/api/instructor/attendance/students/${userId}/attendance`, { params })
    return response.data
  } catch (error) {
    console.error('학생 출석 이력 조회 실패:', error)
    throw error
  }
}

// 14. 강사별 담당 과정의 강의실 목록 조회
export const getInstructorClassrooms = async (userId) => {
  try {
    console.log('📤 강사별 강의실 조회 요청, userId:', userId)
    const response = await http.get(`/api/instructor/qr/classrooms/${userId}`)
    console.log('📥 강사별 강의실 조회 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('강사별 강의실 조회 실패:', error)
    throw error
  }
}

// 15. 모든 과정 정보 조회
export const getAllCourses = async () => {
  try {
    console.log('📤 모든 과정 정보 조회 요청')
    const response = await http.get('/api/instructor/qr/courses')
    console.log('📥 모든 과정 정보 조회 응답:', response.data)
    return response.data
  } catch (error) {
    console.error('모든 과정 정보 조회 실패:', error)
    throw error
  }
}
