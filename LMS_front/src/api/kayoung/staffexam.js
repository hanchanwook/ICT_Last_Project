// 직원용 시험 관련 API 함수들
import { http, baseURL } from "../../components/auth/http"

/**
 * 특정 시험의 문제별 통계 조회
 * @param {string} examId - 시험 ID
 * @returns {Promise<object>} - 문제별 통계
 */
export const getExamQuestionStats = async (examId) => {
    try {
        const response = await http.get(`/api/staff/exam/${examId}/question-stats`)
        
        return response.data
    } catch (error) {
        
        return { 
            success: false, 
            data: {
                examId: examId,
                examName: '',
                questionStats: [],
                totalQuestions: 0,
                avgCorrectRate: 0
            }, 
            message: '문제별 통계 조회 실패' 
        }
    }
};

/**
 * 특정 시험의 학생별 답안 상세 조회
 * @param {string} examId - 시험 ID
 * @returns {Promise<object>} - 학생별 답안 상세
 */
// export const getExamStudentAnswers = async (examId) => {
//     try {
//         const response = await http.get(`/api/staff/exam/${examId}/student-answers`)
        
//         return response.data
//     } catch (error) {
        
//         return { 
//             success: false, 
//             data: {
//                 examId: examId,
//                 examName: '',
//                 studentAnswers: [],
//                 totalStudents: 0
//             }, 
//             message: '학생 답안 상세 조회 실패' 
//         }
//     }
// };

/**
 * 특정 시험의 문제별 정답률 분석
 * @param {string} examId - 시험 ID
 * @returns {Promise<object>} - 문제별 정답률 분석
 */
// export const getExamQuestionAnalysis = async (examId) => {
//     try {
//         const response = await http.get(`/api/staff/exam/${examId}/question-analysis`)
        
//         return response.data
//     } catch (error) {
        
//         return { 
//             success: false, 
//             data: {
//                 examId: examId,
//                 examName: '',
//                 questionAnalysis: [],
//                 totalQuestions: 0
//             }, 
//             message: '문제별 정답률 분석 조회 실패' 
//         }
//     }
// };
