import { http } from '@/components/auth/http'

// 직원 - 1 - 전체 회원 목록 조회
export const getMemberList = async (educationId = null, params = {}) => {
  try {
    // educationId를 항상 params에 추가
    const requestParams = { ...params }
    if (educationId) {
      requestParams.educationId = educationId
    }
    console.log('회원 목록 조회 요청 파라미터:', requestParams)
    
    const response = await http.get("/api/members", { params: requestParams })
    console.log('회원 목록 조회 응답:', response.data)
    return response.data
  } catch (error) {
    console.error("회원 목록 조회 실패:", error)
    throw error
  }
}

// 직원 - 2 - 특정 회원 상세 정보 조회
export const getMemberDetail = async (userId, currentUserId, educationId = null) => {
  try {
    console.log('회원 상세 정보 조회 - userId:', userId, 'currentUserId:', currentUserId, 'educationId:', educationId)
    
    // educationId를 항상 params에 추가
    const params = { userId: currentUserId }
    if (educationId) {
      params.educationId = educationId
    }
    
    const response = await http.get(`/api/members/detail/${userId}`, { params })
    console.log('회원 상세 정보 응답:', response.data)
    return response.data
  } catch (error) {
    console.error("회원 상세 정보 조회 실패:", error)
    throw error
  }
}

// 직원 - 3 - 회원 정보 수정
export const updateMember = async (userId, memberData) => {
  try {
    console.log('회원 정보 수정 - userId:', userId)
    const response = await http.put(`/api/members/update/${userId}`, memberData)
    return response.data
  } catch (error) {
    console.error("회원 정보 수정 실패:", error)
    throw error
  }
}

// 4. 학생별 출석 기록 조회
export const getStudentAttendance = async (userId, params = {}) => {
  try {
    console.log('학생 출석 기록 조회 - userId:', userId)
    const response = await http.get(`/api/attendance/students/${userId}`, { params })
    return response.data
  } catch (error) {
    console.error("학생 출석 기록 조회 실패:", error)
    throw error
  }
}

// 5. 전체 학생 출석 기록 조회
export const getAllAttendances = async () => {
  try {
    console.log('전체 학생 출석 기록 조회 시작')
    const response = await http.get("/api/attendance/management/all")
    console.log('전체 학생 출석 기록 조회 응답:', response.data)
    return response.data
  } catch (error) {
    console.error("전체 학생 출석 기록 조회 실패:", error)
    throw error
  }
}

// 6. 출석 통계 조회
export const getAttendanceStatistics = async (params = {}) => {
  try {
    const response = await http.get("/api/attendance/statistics", { params })
    return response.data
  } catch (error) {
    console.error("출석 통계 조회 실패:", error)
    throw error
  }
}

// 7. 출석 기록 수정
export const updateAttendance = async (attendanceId, attendanceData) => {
  try {
    const response = await http.put(`/api/attendance/management/${attendanceId}`, attendanceData)
    return response.data
  } catch (error) {
    console.error("출석 기록 수정 실패:", error)
    throw error
  }
}

// 직원 - 4 - 학생의 추가 수강 신청 가능 과정 목록 조회
export const getAvailableCourses = async (userId) => {
  try {
    console.log('수강 신청 가능 과정 목록 조회 - userId:', userId)
    const response = await http.get('/api/members/checkcourse', {
      params: { userId }
    })
    console.log('수강 신청 가능 과정 목록 응답:', response.data)
    return response.data
  } catch (error) {
    console.error("수강 신청 가능 과정 목록 조회 실패:", error)
    throw error
  }
}

// 직원 - 5 -학생의 추가 과정 신청 API
export const registerCourse = async (courseData) => {
  try {
    console.log('과정 신청 요청:', courseData)
    const response = await http.post('/api/members/addcourse', courseData)
    console.log('과정 신청 응답:', response.data)
    return response.data
  } catch (error) {
    console.error("과정 신청 실패:", error)
    throw error
  }
}





