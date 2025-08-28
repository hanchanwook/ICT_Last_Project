// 학생 시험 관련 API 함수들
import { http, baseURL } from "../../components/auth/http"

/**
 * 학생이 응시할 수 있는 모든 시험 목록 조회
 * @returns {Promise<object>} - 시험 목록
 */
export const getStudentExams = async () => {
    try {
        const response = await http.get('/api/student/exam/exams')
        
        return response.data
    } catch (error) {
        
        return { success: false, data: [], message: '시험 목록 조회 실패' }
    }
};

/**
 * 시험 시작 (시험 문제 가져오기) - 기존 API
 * @param {string} templateId - 시험 템플릿 ID
 * @returns {Promise<object>} - 시험 문제 정보
 */
export const startExam = async (templateId) => {
    try {
        const response = await http.get(`/api/student/exam/questions/${templateId}`)
        
        return response.data
    } catch (error) {
        throw error
    }
};

/**
 * 새로운 시험 시작 API
 * @param {string} templateId - 시험 템플릿 ID
 * @param {string} courseId - 과정 ID
 * @returns {Promise<object>} - 시험 문제 정보
 */
export const startNewExam = async (templateId, courseId) => {
    try {
        const response = await http.post(`/api/student/exam/start/${templateId}`, {
            templateId: templateId,
            courseId: courseId
        })
        
        return response.data
    } catch (error) {
        throw error
    }
};

/**
 * 시험 제출
 * @param {string} templateId - 시험 템플릿 ID
 * @param {string} courseId - 과정 ID
 * @param {object} answers - 답안 객체
 * @returns {Promise<object>} - 제출 결과
 */
export const submitExam = async (templateId, courseId, answers) => {
    try {
        const response = await http.post(`/api/student/exam/submit/${templateId}`, {
            templateId: templateId,
            courseId: courseId,
            answers: answers
        })
        
        return response.data
    } catch (error) {
        throw error
    }
};

/**
 * 시험 결과 조회
 * @param {string} templateId - 시험 템플릿 ID
 * @returns {Promise<object>} - 시험 결과
 */
// export const getExamResult = async (templateId) => {
//     try {
//         const response = await http.get(`/api/student/exam/exams/${templateId}/result`)
//         return response.data
//     } catch (error) {
//         console.error('시험 결과 조회 실패:', error)
//         return { success: false, data: {}, message: '시험 결과 조회 실패' }
//     }
// };

/**
 * 학생이 수강하는 과정 목록 조회
 * @returns {Promise<object>} - 과정 목록
 */
export const getStudentCourses = async () => {
    try {
        const response = await http.get('/api/student/exam/courses')
        return response.data
    } catch (error) {
        
        
        // 과정 목록 조회 실패 시 빈 배열 반환
        return { success: false, data: [], message: '과정 목록 조회 실패' }
    }
};

/**
 * 학생 성적 조회 API
 * @returns {Promise<object>} - 과정별 시험 성적 목록
 */
export const getStudentGrades = async () => {
    try {
        const response = await http.get('/api/student/exam/grades')
        
        return response.data
    } catch (error) {
        console.error('성적 조회 실패:', error)
        // 성적 조회 실패 시 빈 배열 반환
        return { success: false, data: [], message: '성적 조회 실패' }
    }
};

/**
 * 과정별 학생 목록 조회 (강사용) - subGroupId 사용
 * @param {string} subGroupId - 서브그룹 ID
 * @returns {Promise<object>} - 학생 목록
 */
export const getCourseStudents = async (subGroupId) => {
    try {
        const response = await http.get(`/api/instructor/subgroups/${subGroupId}/students`)
        
        return response.data
    } catch (error) {
        
        // 학생 목록 조회 실패 시 빈 배열 반환
        return { success: false, data: [], message: '학생 목록 조회 실패' }
    }
};

/**
 * 시험별 답안 제출 현황 조회 (강사용)
 * @param {string} templateId - 시험 템플릿 ID
 * @returns {Promise<object>} - 답안 제출 현황 목록
 */
export const getExamSubmissions = async (templateId) => {
    try {
        const response = await http.get(`/api/instructor/exams/${templateId}/submissions`)
        
        return response.data
    } catch (error) {
        
        // 답안 제출 현황 조회 실패 시 빈 배열 반환
        return { success: false, data: [], message: '답안 제출 현황 조회 실패' }
    }
};

/**
 * 강사가 담당하는 모든 과정 조회
 * @returns {Promise<object>} - 과정 목록
 */
export const getInstructorCourses = async () => {
    try {
        const response = await http.get('/api/instructor/courses/allcourses')
        
        return response.data
    } catch (error) {
        console.error('강사 과정 목록 조회 실패:', error)
        
        // 404 에러인 경우 기존 API로 재시도
        if (error.response?.status === 404) {
            try {
                const fallbackResponse = await http.get('/api/instructor/courses')
                return fallbackResponse.data
            } catch (fallbackError) {
                console.error('fallback API도 실패:', fallbackError)
            }
        }
        
        // 과정 목록 조회 실패 시 빈 배열 반환
        return { success: false, data: [], message: '과정 목록 조회 실패' }
    }
};

/**
 * 시험별 학생 점수 조회 (강사용)
 * @param {string} templateId - 시험 템플릿 ID
 * @returns {Promise<object>} - 학생 점수 목록
 */
export const getExamScores = async (templateId) => {
    try {
        // http 인스턴스의 인터셉터가 자동으로 userId를 추가하므로, 
        // 백엔드에서 이미 userId를 포함한 URL을 받도록 설정
        const response = await http.get(`/api/instructor/exams/${templateId}/scores`)
        
        // 응답 데이터 구조 확인 및 안전한 반환
        if (response.data && Array.isArray(response.data)) {
            return response.data
        } else if (response.data && Array.isArray(response.data.data)) {
            return response.data.data
        } else {
            return []
        }
    } catch (error) {
        console.error('시험 점수 조회 실패:', error)
        
        // 404 에러는 API가 아직 구현되지 않았음을 의미
        if (error.response?.status === 404) {
            return []
        }
        
        // 기타 에러 시에도 빈 배열 반환
        return []
    }
};

/**
 * 학생별 시험 답안 상세 조회 (강사용)
 * @param {string} templateId - 시험 템플릿 ID
 * @param {string} memberId - 학생 ID
 * @returns {Promise<object>} - 학생 답안 상세 정보
 */
export const getStudentExamAnswers = async (templateId, memberId) => {
    try {
        const response = await http.get(`/api/instructor/exams/${templateId}/students/${memberId}/answers`)
        
        return response.data
    } catch (error) {
        
        // 답안 조회 실패 시 빈 객체 반환
        return { success: false, data: {}, message: '답안 조회 실패' }
    }
};

/**
 * 학생 시험 답안 조회 (학생용)
 * @param {string} templateId - 시험 템플릿 ID
 * @returns {Promise<object>} - 학생 답안 정보
 */
export const getStudentAnswers = async (templateId) => {
    try {
        const response = await http.get(`/api/student/exam/answers/${templateId}`)
        
        return response.data
    } catch (error) {
        
        // 답안 조회 실패 시 빈 객체 반환
        return { success: false, data: {}, message: '답안 조회 실패' }
    }
};

/**
 * 시험 채점 제출 (강사용)
 * @param {object} gradingData - 채점 데이터
 * @param {string} gradingData.memberId - 학생 ID
 * @param {string} gradingData.templateId - 시험 템플릿 ID
 * @param {number} gradingData.score - 총점
 * @param {number} gradingData.isChecked - 채점 완료 여부 (1: 채점완료/학생미확인, 0: 학생확인완료)
 * @param {string} gradingData.totalComment - 전체 피드백 (백엔드 필드명)
 * @param {Array} gradingData.questionDetails - 문제별 채점 상세
 * @param {object} gradingData.questionDetails[].questionId - 문제 ID
 * @param {number} gradingData.questionDetails[].score - 문제별 점수
 * @param {string} gradingData.questionDetails[].comment - 문제별 코멘트 (선택사항)
 * @param {boolean} gradingData.questionDetails[].isCorrect - 정답 여부
 * @returns {Promise<object>} - 채점 제출 결과
 */
export const submitGrading = async (gradingData) => {
    try {
        const response = await http.post('/api/instructor/exam/grading/submit', gradingData)
        
        // 응답 상태 코드 확인
        if (response.status !== 200) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`)
        }
        
        // 응답 데이터 확인
        if (!response.data) {
            throw new Error('서버에서 응답 데이터가 없습니다.')
        }
        
        // 성공 여부 확인 (백엔드 응답 구조에 따라 조정)
        if (response.data.success === false) {
            throw new Error(response.data.message || '채점 제출에 실패했습니다.')
        }
        
        // 성공 응답이지만 success 필드가 없는 경우도 처리
        if (response.data.success === undefined && response.data.message) {
            // 메시지가 있으면 성공으로 간주
            return response.data
        }
        
        // 성공 응답
        return response.data
        
    } catch (error) {
        
        // 409 에러인 경우 특별 처리
        if (error.response?.status === 409) {
            
        }
        
        throw error
    }
}; 

/**
 * 학생 성적 확인 제출 (isChecked를 1에서 0으로 변경)
 * @param {string} templateId - 시험 템플릿 ID
 * @param {string} signature - 학생 서명
 * @param {string} courseId - 과정 ID (선택사항)
 * @param {string} memberId - 학생 ID (선택사항)
 * @returns {Promise<object>} - 성적 확인 제출 결과
 */
export const confirmGrade = async (templateId, signature, courseId = null, memberId = null) => {
    try {
        console.log('=== confirmGrade API 호출 시작 ===')
        
        // 백엔드에서 필요한 핵심 필드만 전송
        const requestData = {
            courseId: courseId,
            templateId: templateId,
            isChecked: 0
        }
        
        console.log('전송할 데이터:', requestData)
        console.log('요청 URL:', '/api/student/exam/confirm-grade')
        
        const response = await http.post('/api/student/exam/confirm-grade', requestData)
        
        console.log('=== confirmGrade API 응답 성공 ===')
        console.log('응답 데이터:', response.data)
        return response.data
    } catch (error) {
        console.error('=== confirmGrade API 오류 발생 ===')
        console.error('오류 객체:', error)
        console.error('오류 메시지:', error.message)
        console.error('오류 응답 상태:', error.response?.status)
        console.error('오류 응답 데이터:', error.response?.data)
        console.error('오류 요청 데이터:', error.config?.data)
        console.error('오류 요청 URL:', error.config?.url)
        console.error('오류 요청 메서드:', error.config?.method)
        if (error.response?.data?.message) {
            console.error('백엔드 오류 메시지:', error.response.data.message)
        }
        if (error.response?.data?.resultMessage) {
            console.error('백엔드 결과 메시지:', error.response.data.resultMessage)
        }
        throw error
    }
};