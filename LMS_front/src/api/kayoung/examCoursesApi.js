// 직원용 시험 과정 관리 API 함수들
import { http, baseURL } from "../../components/auth/http"

/**
 * 강사가 담당하는 과정 목록 조회 (강사용)
 * @returns {Promise<object>} - 과정 목록
 */
// export const getInstructorExamCourses = async () => {
//     try {
//         const response = await http.get('/api/instructor/exam/courses')
        
//         return response.data
//     } catch (error) {
        
//         // 과정 목록 조회 실패 시 빈 배열 반환
//         return { success: false, data: [], message: '과정 목록 조회 실패' }
//     }
// };

/**
 * 강사가 담당하는 특정 과정의 시험 목록 조회
 * @param {string} courseId - 과정 ID
 * @returns {Promise<object>} - 시험 목록
 */
// export const getInstructorCourseExams = async (courseId) => {
//     try {
//         const response = await http.get(`/api/instructor/exam/courses/${courseId}/exams`)
        
//         return response.data
//     } catch (error) {
        
//         return { success: false, data: [], message: '시험 목록 조회 실패' }
//     }
// };

/**
 * 강사가 담당하는 특정 과정의 성적 통계 조회
 * @param {string} courseId - 과정 ID
 * @returns {Promise<object>} - 성적 통계
 */
// export const getInstructorCourseGrades = async (courseId) => {
//     try {
//         const response = await http.get(`/api/instructor/exam/courses/${courseId}/grades`)
        
//         return response.data
//     } catch (error) {
        
//         return { 
//             success: false, 
//             data: {
//                 avgScore: 0,
//                 passRate: 0,
//                 examCount: 0,
//                 studentCount: 0
//             }, 
//             message: '성적 통계 조회 실패' 
//         }
//     }
// };

/**
 * 직원이 관리할 수 있는 모든 과정 목록 조회 (해당 학원의 educationId 기준)
 * @returns {Promise<object>} - 과정 목록
 */
export const getStaffCourses = async () => {
    try {
        const response = await http.get('/api/staff/exam/courses')
        
        return response.data
    } catch (error) {
        
        // 과정 목록 조회 실패 시 빈 배열 반환
        return { success: false, data: [], message: '과정 목록 조회 실패' }
    }
};

/**
 * 특정 과정의 시험 목록 조회
 * @param {string} courseId - 과정 ID
 * @returns {Promise<object>} - 시험 목록
 */
export const getCourseExams = async (courseId) => {
    try {
        const response = await http.get(`/api/staff/exam/courses/${courseId}/exams`)
        
        return response.data
    } catch (error) {
        
        return { success: false, data: [], message: '시험 목록 조회 실패' }
    }
};

/**
 * 특정 과정의 성적 통계 조회
 * @param {string} courseId - 과정 ID
 * @returns {Promise<object>} - 성적 통계
 */
export const getCourseGrades = async (courseId) => {
    try {
        const response = await http.get(`/api/staff/exam/courses/${courseId}/grades`)
        
        return response.data
    } catch (error) {
        
        return { 
            success: false, 
            data: {
                avgScore: 0,
                passRate: 0,
                examCount: 0,
                studentCount: 0,
                gradeDistribution: {
                    A: 0, B: 0, C: 0, D: 0, F: 0
                }
            }, 
            message: '성적 통계 조회 실패' 
        }
    }
}; 