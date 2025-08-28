// src/api/kayoung/questionBank.js
import { http, baseURL } from "../../components/auth/http"

/**
 * 현재 로그인한 사용자의 ID를 가져오는 함수
 * @returns {string|null} - 사용자 ID 또는 null
 */
const getCurrentUserId = () => {
    try {
        // localStorage에서 사용자 정보 가져오기
        const userInfo = localStorage.getItem('currentUser');
        if (userInfo) {
            const user = JSON.parse(userInfo);
            return user.memberId || null;
        } else {
            return null;
        }
    } catch (error) {
        return null;
    }
};

/**
 * 통합된 문제 목록 조회 API
 * 모든 필터링 조건을 쿼리 파라미터로 처리
 * 
 * @param {object} params - 검색 및 필터링 파라미터
 * @param {string} [params.keyword] - 검색 키워드 (문제 내용, 과목명, 강사명)
 * @param {string} [params.subject] - 과목 ID (subDetailId)
 * @param {string} [params.type] - 문제 유형 (객관식, 서술형, 코드형)
 * @param {string} [params.instructor] - 강사 ID (memberId)
 * @param {number} [params.page] - 페이지 번호 (기본값: 1)
 * @param {number} [params.limit] - 페이지당 항목 수 (기본값: 10)
 * @returns {Promise<object>} - 문제 목록 및 통계 정보
 */
export const getQuestions = async (params = {}) => {
    try {
        // 백엔드에서 확인된 엔드포인트 사용
        const response = await http.get('/api/questions/all')
        
        // 전체 응답 데이터 반환 (data 필드 포함)
        return response.data
    } catch (error) {
        
        // 문제 목록 조회 실패 시 빈 배열 반환
        return []
    }
}

/**
 *  과목 리스트 조회 (과목-세부과목 구조)
 */
export const getSubjectList = async () => {
    try {
        const response = await http.get("/api/questions/subjectList")
        
        // 새로운 응답 구조: data 안에 객체 형태로 과목들이 들어있음
        const subjectsData = response.data?.data || {}
        
        // 객체의 키들을 과목명 배열로 변환
        const subjectsArray = Object.keys(subjectsData).filter(subjectName => {
            // 빈 과목명 제외
            if (!subjectName || subjectName.trim() === '') {
                return false
            }
            return true
        })
        
        return subjectsArray
        
    } catch (error) {
        
        // 과목 목록 조회 실패 시 빈 배열 반환
        return []
    }
}

/**
 * 세부과목 목록 조회 (평면화된 구조)
 * @returns {Promise<Array<object>>} - 세부과목 목록
 */
export const getSubDetailList = async () => {
    try {
        const response = await http.get("/api/questions/subdetail")
        
        // 백엔드 응답 구조에 따라 처리
        const subDetailData = response.data?.data || []
        
        // 프론트엔드에서 사용하기 편한 형태로 변환
        const formattedSubDetails = subDetailData.map(subDetail => ({
            subDetailId: subDetail.subDetailId,
            subDetailName: subDetail.subDetailName,
            subjectName: subDetail.subjectName,
            subDetailActive: subDetail.subDetailActive
        }))
        
        return formattedSubDetails
        
    } catch (error) {
        
        // 세부과목 목록 조회 실패 시 빈 배열 반환
        return []
    }
}

/**
 * 과목-세부과목 구조 조회 (새로운 API)
 */
// export const getSubjectHierarchy = async () => {
//     try {
//         const response = await http.get("/api/subjects/hierarchy")
        
//         // 백엔드 응답 구조에 따라 처리
//         const hierarchyData = response.data?.data || response.data || {}
//         return hierarchyData
        
//     } catch (error) {
//         throw error
//     }
// }

/**
 * 현재 로그인한 사용자의 문제 목록 조회
 * @returns {Promise<object>} - 현재 사용자의 문제 목록
 */
export const getQuestionsByTeacher = async () => {
    try {
        const response = await http.get("/api/questions/my")
        
        // 전체 응답 데이터 반환 (data 필드 포함)
        return response.data
        
    } catch (error) {
        
        // 조회 실패 시 빈 배열 반환
        return []
    }
}

/**
 * 유형별 문제 조회 (getQuestions로 통합)
 * @param {string} questionType - 문제 유형
 * @returns {Promise<object>} - 해당 유형의 문제 목록
 */
// export const getQuestionsByType = async (questionType) => {
//     return getQuestions({ type: questionType })
// }

/**
 * 과목별 문제 조회 (getQuestions로 통합)
 * @param {string} subDetailId - 과목 ID
 * @returns {Promise<object>} - 해당 과목의 문제 목록
 */
// export const getQuestionsBySubject = async (subDetailId) => {
//     return getQuestions({ subject: subDetailId })
// }

/**
 * 강사 목록 조회
 * @returns {Promise<Array<object>>} - 강사 목록
 */
export const getInstructors = async () => {
    try {
        // 백엔드에서 현재 사용자의 educationId를 기반으로 강사 목록 조회
        const response = await http.get("/api/members/instructors/simple")
        
        // 응답 데이터 구조에 따라 처리
        const instructors = response.data?.data || response.data?.instructors || response.data || []
        return instructors
        
    } catch (error) {
        
        // 강사 목록 조회 실패 시 빈 배열 반환
        return []
    }
}


/**
 * 문제은행 통계 조회 (getQuestions로 통합 가능하지만 별도 엔드포인트 유지)
 * @returns {Promise<object>} - 통계 데이터
 */
export const getQuestionBankStats = async () => {
    try {
        const response = await http.get("/api/questions/stats")
        
        // 백엔드 응답 구조에 맞게 처리
        if (response.data && response.data.stats) {
            return response.data.stats
        } else if (response.data && response.data.data && response.data.data.stats) {
            return response.data.data.stats
        } else {
            return response.data
        }
    } catch (error) {
        
        // 통계 조회 실패 시 기본값 반환
        return {
            total: 0,
            objective: 0,
            subjective: 0,
            coding: 0
        }
    }
}

/**
 * 문제 생성
 * @param {object} questionData - 문제 데이터
 * @returns {Promise<object>} - 생성된 문제 정보
 */
export const createQuestion = async (questionData) => {
    try {
        // instructorId는 쿠키에서 자동으로 가져오므로 URL 파라미터에서 제거
        const response = await http.post(`/api/questions/create`, questionData)
        return response.data
    } catch (error) {
        throw error
    }
}

/**
 * 문제 수정
 * @param {string} questionId - 문제 ID
 * @param {object} questionData - 수정할 문제 데이터
 * @returns {Promise<object>} - 수정 결과
 */
export const updateQuestion = async (questionId, questionData) => {
    try {
        const response = await http.post(`/api/questions/${questionId}/update`, questionData)
        return response.data
    } catch (error) {
        throw error
    }
}


// 문제 비활성화 (논리적 삭제)
export const deactivateQuestion = async (questionId) => {
  try {
    const response = await http.patch(`/api/questions/${questionId}/status`, {
      status: "inactive",
      reason: "instructor_deactivation"
    })
    return response.data
  } catch (error) {
    throw error
  }
}


// --- 과정 관련 API 함수들 ---

/**
 * 강사가 담당하는 과정 목록 조회 (과정-과목-세부과목 구조 포함)
 * @returns {Promise<Array<object>>} - 과정 목록 (과정명, 과목, 세부과목 정보 포함)
 */
export const getMyCourses = async () => {
    try {
        const response = await http.get('/api/courses/my')
        
        // 백엔드 응답 구조에 따라 처리
        const coursesData = response.data?.data || response.data || []
        
        // 각 과정의 과목들에 대해 세부과목 정보가 없는 경우 처리
        const processedCourses = coursesData.map(course => {
            if (course.subjects && Array.isArray(course.subjects)) {
                const processedSubjects = course.subjects.map(subject => {
                    // 세부과목 정보가 없는 경우 과목 자체를 세부과목으로 사용
                    if (!subject.subDetails || !Array.isArray(subject.subDetails)) {
                        return {
                            ...subject,
                            subDetails: [{
                                subDetailId: subject.subjectId,
                                subDetailName: subject.subjectName,
                                subjectActive: subject.subjectActive || 0
                            }]
                        }
                    }
                    return subject
                })
                
                return {
                    ...course,
                    subjects: processedSubjects
                }
            }
            return course
        })
        
        return processedCourses
        
    } catch (error) {
        
        // 과정 목록 조회 실패 시 빈 배열 반환
        return []
    }
} 

// export const getMySubDetails = async () => {
//     try {
//         const response = await http.get('/api/subjects/subDetails/my')
        
//         // 백엔드 응답 구조에 따라 처리
//         const subDetailsData = response.data?.data || response.data || []
        
//         // 프론트엔드에서 사용할 형태로 변환
//         const transformedSubDetails = subDetailsData.map(subDetail => ({
//             id: subDetail.subDetailId || subDetail.id,
//             name: subDetail.subDetailName || subDetail.name,
//             subjectName: subDetail.subjectName,
//             subjectId: subDetail.subjectId,
//             educationId: subDetail.educationId,
//             active: subDetail.subDetailActive === 0, // 0이 활성, 1이 비활성
//             createdAt: subDetail.createdAt
//         }))
        
//         return transformedSubDetails
        
//     } catch (error) {
//         if (error.response) {
            
//         }
//         return []
//     }
// } 

/**
 * 활성화된 세부과목 목록 조회 (임시 - 기존 API 사용)
 * 백엔드에서 /api/subjects/subDetails/my가 구현되기 전까지 사용
 * @returns {Promise<Array<object>>} - 활성화된 세부과목 목록 (Active = 0)
 */
// export const getMySubDetailsFallback = async () => {
//     try {
        
//         // 전체 세부과목을 가져와서 필터링
//         const allSubDetails = await getSubDetailList()
        
//         // Active 값이 0인 세부과목들만 필터링 (0이 활성, 1이 비활성)
//         const activeSubDetails = allSubDetails.filter(subDetail => 
//             subDetail.subDetailActive === 0
//         )
        
//         // 프론트엔드에서 사용할 형태로 변환
//         const transformedSubDetails = activeSubDetails.map(subDetail => ({
//             id: subDetail.subDetailId || subDetail.id,
//             name: subDetail.subDetailName || subDetail.name,
//             subjectName: subDetail.subjectName,
//             active: true // 이미 필터링했으므로 항상 true
//         }))
        
//         return transformedSubDetails
        
//     } catch (error) {
//         return []
//     }
// } 