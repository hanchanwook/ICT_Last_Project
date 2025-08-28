/**
 * 강사용 과제 제출 파일 관리 API
 * 
 * 백엔드에서 새로운 /api/professor/assignments/** 엔드포인트가 아직 구현되지 않았을 가능성이 있으므로,
 * 임시로 기존 엔드포인트를 사용합니다.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:19091';

/**
 * JWT 토큰 가져오기
 */
const getAuthToken = () => {
  return localStorage.getItem('token') || localStorage.getItem('accessToken');
};

/**
 * 강사용 - 과제의 모든 학생 제출 파일 목록 조회
 * @param {string} assignmentId - 과제 ID
 * @returns {Promise<Array>} 파일 목록
 */
export const getProfessorAssignmentSubmissionFiles = async (assignmentId) => {
  try {
    console.log('🔍 강사용 과제 제출 파일 목록 조회 시작:', { assignmentId })
    
    // 임시로 기존 학생용 엔드포인트 사용
    const response = await fetch(`${API_BASE_URL}/api/student/assignments/${assignmentId}/submissions/files`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    })

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
      } else if (response.status === 403) {
        throw new Error('접근 권한이 없습니다.');
      } else if (response.status === 404) {
        console.log('🔍 제출 파일이 없습니다. 빈 배열 반환');
        return [];
      } else {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
    }

    const result = await response.json()
    console.log('✅ 강사용 과제 제출 파일 목록 조회 성공:', result)
    
    // 다양한 응답 구조 처리
    let files = [];
    if (result && typeof result === 'object') {
      if (Array.isArray(result)) {
        files = result;
      } else if (result.data && Array.isArray(result.data)) {
        files = result.data;
      } else if (result.result && Array.isArray(result.result)) {
        files = result.result;
      } else if (result.response && Array.isArray(result.response.data)) {
        files = result.response.data;
      } else if (result.files && Array.isArray(result.files)) {
        files = result.files;
      }
    }
    
    return files;
  } catch (error) {
    console.error('❌ 강사용 과제 제출 파일 목록 조회 실패:', error)
    return [];
  }
}

/**
 * 강사용 - 특정 학생의 과제 제출 파일 목록 조회
 * @param {string} assignmentId - 과제 ID
 * @param {string} studentId - 학생 ID
 * @returns {Promise<Array>} 파일 목록
 */
export const getProfessorStudentSubmissionFiles = async (assignmentId, studentId) => {
  try {
    console.log('🔍 강사용 학생별 과제 제출 파일 목록 조회 시작:', { assignmentId, studentId })
    
    // 임시로 기존 학생용 엔드포인트 사용 (학생 ID로 필터링은 백엔드에서 처리)
    const response = await fetch(`${API_BASE_URL}/api/student/assignments/${assignmentId}/submissions/files?studentId=${studentId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    })

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
      } else if (response.status === 403) {
        throw new Error('접근 권한이 없습니다.');
      } else if (response.status === 404) {
        console.log('🔍 해당 학생의 제출 파일이 없습니다. 빈 배열 반환');
        return [];
      } else {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
    }

    const result = await response.json()
    console.log('✅ 강사용 학생별 과제 제출 파일 목록 조회 성공:', result)
    
    // 다양한 응답 구조 처리
    let files = [];
    if (result && typeof result === 'object') {
      if (Array.isArray(result)) {
        files = result;
      } else if (result.data && Array.isArray(result.data)) {
        files = result.data;
      } else if (result.result && Array.isArray(result.result)) {
        files = result.result;
      } else if (result.response && Array.isArray(result.response.data)) {
        files = result.response.data;
      } else if (result.files && Array.isArray(result.files)) {
        files = result.files;
      }
    }
    
    // 학생 ID로 필터링 (백엔드에서 필터링하지 않는 경우를 대비)
    if (studentId && files.length > 0) {
      files = files.filter(file => file.studentId === studentId || file.memberId === studentId);
    }
    
    return files;
  } catch (error) {
    console.error('❌ 강사용 학생별 과제 제출 파일 목록 조회 실패:', error)
    return [];
  }
}

/**
 * 강사용 - 과제 제출 파일 다운로드
 * @param {string} fileId - 파일 ID
 * @param {string} fileName - 파일명 (다운로드 시 사용)
 * @returns {Promise<void>}
 */
export const downloadProfessorSubmissionFile = async (fileId, fileName = 'submission_file') => {
  try {
    console.log('🔍 강사용 과제 제출 파일 다운로드 시작:', { fileId, fileName })
    
    // 임시로 기존 파일 다운로드 엔드포인트 사용
    const response = await fetch(`${API_BASE_URL}/api/v2/file/download`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        fileId: fileId,
        fileName: fileName
      })
    })

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
      } else if (response.status === 403) {
        throw new Error('접근 권한이 없습니다.');
      } else if (response.status === 404) {
        throw new Error('파일을 찾을 수 없습니다.');
      } else {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    console.log('✅ 강사용 과제 제출 파일 다운로드 성공:', fileName)
  } catch (error) {
    console.error('❌ 강사용 과제 제출 파일 다운로드 실패:', error)
    throw error
  }
} 