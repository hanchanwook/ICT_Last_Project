// 강의 이력 관리 API
import { http, baseURL } from "../../components/auth/http"

/**
 * 강의 이력 목록 조회 API
 * @returns {Promise<object>} - 강의 이력 목록
 */
export const getLecturesHistory = async () => {
  try {
    const response = await http.get('/api/instructor/lectures/history')
    
    // 백엔드 응답 구조에 맞게 처리
    if (response.data.resultCode === 200) {
      return response.data.data
    } else {
      throw new Error(response.data.resultMessage || '강의 이력 조회 실패')
    }
  } catch (error) {
    
    // 강의 이력 조회 실패 시 빈 배열 반환
    return []
  }
}

/**
 * 필터링된 강의 목록 조회 API
 * @param {string} searchTerm - 검색 키워드
 * @param {string} status - 강의 상태
 * @returns {Promise<object>} - 필터링된 강의 목록
 */
export const getFilteredLectures = async (searchTerm, status) => {
  try {
    const params = new URLSearchParams()
    if (searchTerm) params.append('search', searchTerm)
    if (status && status !== 'all') params.append('status', status)
    
    const response = await http.get(`/api/instructor/lectures/history/filter?${params}`)
    
    // 백엔드 응답 구조에 맞게 처리
    if (response.data.resultCode === 200) {
      return response.data.data
    } else {
      throw new Error(response.data.resultMessage || '필터링된 강의 목록 조회 실패')
    }
  } catch (error) {
    
    // 필터링된 강의 목록 조회 실패 시 빈 배열 반환
    return []
  }
}

/**
 * 강의 통계 조회 API
 * @returns {Promise<object>} - 강의 통계 정보
 */
export const getLecturesStatistics = async () => {
  try {
    const response = await http.get('/api/instructor/lectures/statistics')
    
    // 백엔드 응답 구조에 맞게 처리
    if (response.data.resultCode === 200) {
      return response.data.data
    } else {
      throw new Error(response.data.resultMessage || '강의 통계 조회 실패')
    }
  } catch (error) {
    
    // 통계 조회 실패 시 기본값 반환
    return {
      totalLectures: 0,
      completedLectures: 0,
      totalStudents: 0,
      overallAverageScore: 0
    }
  }
}

/**
 * 개별 강의 상세 정보 조회 API
 * @param {string} lectureId - 강의 ID
 * @returns {Promise<object>} - 강의 상세 정보
 */
export const getLectureDetail = async (lectureId) => {
  try {
    const response = await http.get(`/api/instructor/lectures/${lectureId}/detail`)
    
    // 백엔드 응답 구조에 맞게 처리
    if (response.data.resultCode === 200) {
      return response.data.data
    } else {
      throw new Error(response.data.resultMessage || '강의 상세 정보 조회 실패')
    }
  } catch (error) {
    return null
  }
} 