// src/api/kayoung/templateApi.js
import { http, baseURL } from "../../components/auth/http"

// 시험 템플릿 목록 조회 (현재 로그인한 강사의 시험 템플릿 목록)
// export const getExamTemplates = async () => {
//   try {
//     const response = await http.get('/api/templates')
//     return response.data
//   } catch (error) {
//     throw error
//   }
// }

// 내 시험 목록 조회
export const getMyExamTemplates = async () => {
  try {
    const response = await http.get('/api/templates/my')
    return response.data
  } catch (error) {
    throw error
  }
}

// 시험 템플릿 상세 정보 조회
export const getExamTemplateDetail = async (templateId) => {
  try {
    const response = await http.get(`/api/templates/${templateId}`)
    return response.data
  } catch (error) {
    throw error
  }
}

// 시험 템플릿 생성
// export const createExamTemplate = async (templateData) => {
//   try {
//     const response = await http.post('/api/templates', templateData)
//     return response.data
//   } catch (error) {
//     throw error
//   }
// }

// 통합 시험 생성 (새 문제 생성 + 시험 템플릿 생성 + 시험-문제 연결)
export const createCompleteExam = async (completeExamData) => {
  try {
    const response = await http.post('/api/templates/create-complete', completeExamData)
    return response.data
  } catch (error) {
    if (error.response) {
      
    }
    throw error
  }
}

// 시험 템플릿 수정
export const updateExamTemplate = async (templateId, templateData) => {
  try {
    const response = await http.put(`/api/templates/${templateId}`, templateData)
    return response.data
  } catch (error) {
    throw error
  }
}

// 시험-문제 연결 수정
// export const updateTemplateQuestions = async (templateId, questionsData) => {
//   try {
//     const response = await http.put(`/api/templates/${templateId}/questions`, questionsData)
//     return response.data
//   } catch (error) {
//     throw error
//   }
// }

// 시험 템플릿 비활성화 (논리적 삭제)
export const deactivateExamTemplate = async (templateId) => {
  try {
    const response = await http.patch(`/api/templates/${templateId}/status`, {
      status: "inactive",
      reason: "instructor_deactivation"
    })
    return response.data
  } catch (error) {
    throw error
  }
}

// 시험 열기 (오픈 날짜 설정)
export const openExam = async (templateId) => {
  try {
    const currentDate = new Date().toISOString()
    const response = await http.patch(`/api/templates/${templateId}`, {
      templateOpen: currentDate
    })
    return response.data
  } catch (error) {
    throw error
  }
}

// 시험 종료 시 미제출 학생들 자동 처리
export const closeExamWithAutoSubmission = async (templateId, studentMemberIds) => {
  try {
    const currentDate = new Date().toISOString()
    const response = await http.post(`/api/templates/${templateId}/close-with-auto-submission`, {
      templateId: templateId,
      templateClose: currentDate,
      studentMemberIds: studentMemberIds
    })
    
    return response.data
  } catch (error) {
    throw error
  }
}

// 문제 상세 정보 조회
// export const getQuestionDetail = async (questionId) => {
//   try {
//     const response = await http.get(`/api/questions/${questionId}`)
//     return response.data
//   } catch (error) {
//     throw error
//   }
// }

// 과정 정보 조회

// export const getCourseInfo = async (subGroupId) => {
//   try {
//     const response = await http.get(`/api/courses/${subGroupId}`)
//     return response.data
//   } catch (error) {
//     // 404 에러 등은 정상적인 상황이므로 에러를 던지지 않고 기본값 반환
//     if (error.response?.status === 404) {
//       return {
//         resultCode: "SUCCESS",
//         data: {
//           courseName: "과정명",
//           courseCode: "COURSE001"
//         }
//       }
//     }
//     throw error
//   }
// }

// 시험 템플릿 응시 학생 목록 조회
// export const getTemplateStudents = async (templateId) => {
//   try {
//     const response = await http.get(`/api/templates/${templateId}/students`)
//     return response.data
//   } catch (error) {
//     throw error
//   }
// }

// 시험-문제 연결
// export const createTemplateQuestions = async (templateQuestionData) => {
//   try {
//     const response = await http.post('/api/templates/templateQuestions/create', templateQuestionData)
//     return response.data
//   } catch (error) {
//     throw error
//   }
// }

/**
 * 새로운 문제 생성 API
 * @param {object} questionData - 문제 데이터
 * @returns {Promise<object>} - 생성된 문제 정보
 */
// export const createNewQuestion = async (questionData) => {
//     try {
//         const response = await http.post('/api/questions/create', questionData)
//         return response.data
//     } catch (error) {
//         throw error
//     }
// }

/**
 * 시험과 문제 연결 API
 * @param {string} templateId - 시험 ID
 * @param {Array} questionMappings - 문제 매핑 배열 [{questionId, score}]
 * @returns {Promise<object>} - 연결 결과
 */
// export const linkExamWithQuestions = async (templateId, questionMappings) => {
//     try {
//         const response = await http.post('/api/templateQuestions/create', {
//             templateId,
//             questionMappings
//         })
//         return response.data
//     } catch (error) {
//         throw error
//     }
// }

/**
 * 시험 목록 조회 API (기존 함수명 유지)
 * @returns {Promise<Array>} - 시험 목록
 */
// export const getExamList = async () => {
//     try {
//         const response = await http.get('/api/templates/my')
//         return response.data
//     } catch (error) {
//         throw error
//     }
// }

/**
 * 시험 상세 정보 조회 API (기존 함수명 유지)
 * @param {string} templateId - 시험 템플릿 ID
 * @returns {Promise<object>} - 시험 상세 정보
 */
// export const getExamDetail = async (templateId) => {
//     try {
//         const response = await http.get(`/api/templates/${templateId}`)
//         return response.data
//     } catch (error) {
//         throw error
//     }
// }

// 시험 응시 학생 목록 조회 (기존 함수명 유지)
// export const getExamStudents = async (templateId) => {
//   try {
//     const response = await http.get(`/api/templates/${templateId}/students`)
//     return response.data
//   } catch (error) {
//     throw error
//   }
// }

// 시험 삭제 (비활성화) - 기존 함수명 유지
// export const deactivateExam = async (templateId) => {
//   try {
//     const response = await http.patch(`/api/templates/${templateId}/status`, {
//       status: "inactive",
//       reason: "instructor_deactivation"
//     })
//     return response.data
//   } catch (error) {
//     throw error
//   }
// }







