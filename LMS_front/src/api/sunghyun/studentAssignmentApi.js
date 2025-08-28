// 학생별 과제 목록 조회 API
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:19091';

export const getStudentAssignments = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/student/assignments`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    if (!response.ok) throw new Error('과제 목록을 불러오는데 실패했습니다.');
    const result = await response.json();
    return result.data; // ResponseDTO에서 data 필드 추출
  } catch (error) {
    console.error('과제 목록 조회 오류:', error);
    throw error;
  }
};

// 학생용 과제 상세 정보 조회 API
export const getStudentAssignmentDetail = async (assignmentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/student/assignments/${assignmentId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('과제를 찾을 수 없습니다.');
      } else {
        throw new Error('과제 상세 정보를 불러오는데 실패했습니다.');
      }
    }
    return await response.json();
  } catch (error) {
    console.error('과제 상세 조회 오류:', error);
    throw error;
  }
};

// 학생용 과제 루브릭 조회 API
export const getStudentAssignmentRubric = async (assignmentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/student/assignments/${assignmentId}/rubric`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    if (!response.ok) {
      if (response.status === 404) {
        return { rubricitem: [] }; // 루브릭이 없는 경우 빈 배열 반환
      } else {
        throw new Error('루브릭을 불러오는데 실패했습니다.');
      }
    }
    return await response.json();
  } catch (error) {
    console.error('루브릭 조회 오류:', error);
    // 에러가 발생해도 빈 배열 반환하여 UI가 깨지지 않도록 함
    return { rubricitem: [] };
  }
};

// assignmentsubmission 테이블에 과제 제출
export const createStudentAssignmentSubmission = async (submissionData) => {
  try {
    // submissionData가 이미 FormData인 경우 그대로 사용
    let formData;
    if (submissionData instanceof FormData) {
      formData = submissionData;
    } else {
      // 기존 방식: 객체를 FormData로 변환
      formData = new FormData();
      if (submissionData.assignmentId) formData.append('assignmentId', submissionData.assignmentId);
      if (submissionData.answerText) formData.append('answerText', submissionData.answerText);
      if (submissionData.submissionType) formData.append('submissionType', submissionData.submissionType);
      if (submissionData.file) formData.append('file', submissionData.file);
    }
    
    const response = await fetch(`${API_BASE_URL}/api/student/assignmentsubmission`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error('API 에러 응답:', errorText);
      throw new Error(`과제 제출에 실패했습니다. (${response.status}: ${errorText})`);
    }
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('과제 제출 오류:', error);
    throw error;
  }
};

// assignmentsubmission 수정
export const updateStudentAssignmentSubmission = async ({ submissionId, answerText, submissionType }) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/student/assignmentsubmission/${submissionId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ answerText, submissionType }),
    });
    if (!response.ok) throw new Error('과제 제출 수정에 실패했습니다.');
    return await response.json();
  } catch (error) {
    console.error('과제 제출 수정 오류:', error);
    throw error;
  }
};

// assignmentsubmission 삭제
export const deleteStudentAssignmentSubmission = async (submissionId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/student/assignmentsubmission/${submissionId}`, {
      method: 'DELETE',
      credentials: 'include',
    });
    if (!response.ok) throw new Error('과제 제출 삭제에 실패했습니다.');
    return await response.json();
  } catch (error) {
    console.error('과제 제출 삭제 오류:', error);
    throw error;
  }
};

// assignmentsubmission 테이블에서 내 제출 리스트 조회
export const getMyAssignmentSubmissions = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/student/assignmentsubmission`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    if (!response.ok) throw new Error('내 과제 제출 목록을 불러오는데 실패했습니다.');
    return await response.json();
  } catch (error) {
    console.error('내 과제 제출 목록 조회 오류:', error);
    throw error;
  }
};

// 특정 과제의 제출 상세 정보 조회
export const getStudentSubmissionDetail = async (assignmentId, courseId, studentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/student/assignments/${assignmentId}/submission-files?courseId=${courseId}&studentId=${studentId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    if (!response.ok) {
      if (response.status === 404) {
        return null; // 제출이 없는 경우
      }
      throw new Error('과제 제출 상세 정보를 불러오는데 실패했습니다.');
    }
    return await response.json();
  } catch (error) {
    console.error('과제 제출 상세 정보 조회 오류:', error);
    throw error;
  }
}; 