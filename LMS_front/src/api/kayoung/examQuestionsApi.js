// 시험 문제 관리 API
import { http, baseURL } from "../../components/auth/http"

/**
 * 세부과목 목록 조회
 * @returns {Promise<Array<object>>} - 세부과목 목록
 */
export const getSubDetailList = async () => {
    try {
        const response = await http.get('/api/questions/subdetail')
        
        // 백엔드 응답 구조에 따라 처리
        const subDetailData = response.data?.data || []
        
        // 프론트엔드에서 사용하기 편한 형태로 변환
        const formattedSubDetails = subDetailData.map(subDetail => ({
            subDetailId: subDetail.subDetailId,
            subDetailName: subDetail.subDetailName,
            subDetailInfo: subDetail.subDetailInfo,
            subjectName: subDetail.subjectName,
            subDetailActive: subDetail.subDetailActive,
            questionCount: subDetail.questionCount || 0,
            questions: subDetail.questions || []
        }))
        
        return formattedSubDetails
        
    } catch (error) {
        
        // 세부과목 목록 조회 실패 시 빈 배열 반환
        return []
    }
}

/**
 * 세부과목별 문제 목록 조회
 * @param {string} subDetailId - 세부과목 ID
 * @returns {Promise<Array<object>>} - 문제 목록
 */
export const getQuestionsBySubDetail = async (subDetailId) => {
    try {
        const response = await http.get(`/api/questions/subdetail/${subDetailId}/questions`)
        
        // 백엔드 응답 구조에 따라 처리
        const questionsData = response.data?.data?.questions || response.data?.questions || response.data || []
        
        // 프론트엔드에서 사용하기 편한 형태로 변환
        const formattedQuestions = questionsData.map(question => ({
            id: question.questionId || question.id,
            question: question.questionText || question.question,
            type: question.questionType || question.type,
            points: question.questionScore || question.score || 0,
            createdDate: question.createdAt || question.createdDate,
            detailSubject: question.subDetailName || question.subjectName,
            options: question.questionAnswer ? JSON.parse(question.questionAnswer) : [],
            correctAnswer: question.correctAnswer,
            explanation: question.explanation
        }))
        
        return formattedQuestions
        
    } catch (error) {
        
        // 문제 목록 조회 실패 시 빈 배열 반환
        return []
    }
}

/**
 * 문제 상세 정보 조회
 * @param {string} questionId - 문제 ID
 * @returns {Promise<object>} - 문제 상세 정보
 */
export const getQuestionDetail = async (questionId) => {
    try {
        const response = await http.get(`/api/questions/${questionId}`)
        
        // 백엔드 응답 구조에 따라 처리
        const questionData = response.data?.data || response.data
        
        // 프론트엔드에서 사용하기 편한 형태로 변환
        const formattedQuestion = {
            id: questionData.questionId || questionData.id,
            question: questionData.questionText || questionData.question,
            type: questionData.questionType || questionData.type,
            points: questionData.questionScore || questionData.score || 0,
            createdDate: questionData.createdAt || questionData.createdDate,
            detailSubject: questionData.subDetailName || questionData.subjectName,
            options: questionData.questionAnswer ? JSON.parse(questionData.questionAnswer) : [],
            correctAnswer: questionData.correctAnswer,
            explanation: questionData.explanation,
            memberId: questionData.memberId,
            subDetailId: questionData.subDetailId
        }
        
        return formattedQuestion
        
    } catch (error) {
        
        // 문제 상세 정보 조회 실패 시 null 반환
        return null
    }
}

// 직원 권한으로는 문제 생성/수정 불가
// 문제 생성과 수정은 강사 권한에서만 가능

/**
 * 문제 삭제
 * @param {string} questionId - 문제 ID
 * @returns {Promise<object>} - 삭제 결과
 */
// export const deleteQuestion = async (questionId) => {
//     try {
//         const response = await http.delete(`/api/questions/${questionId}`)
        
//         return response.data
//     } catch (error) {
//         throw error
//     }
// }

/**
 * 세부과목별 문제 통계 조회
 * @param {string} subDetailId - 세부과목 ID
 * @returns {Promise<object>} - 문제 통계 정보
 */
// export const getQuestionStats = async (subDetailId) => {
//     try {
//         const response = await http.get(`/api/questions/subdetail/${subDetailId}/stats`)
        
//         return response.data
//     } catch (error) {
        
//         // 통계 조회 실패 시 기본값 반환
//         return {
//             totalQuestions: 0,
//             objectiveQuestions: 0,
//             subjectiveQuestions: 0,
//             codingQuestions: 0
//         }
//     }
// } 