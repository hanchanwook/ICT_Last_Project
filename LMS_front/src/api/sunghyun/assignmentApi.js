// 과제 관련 API 함수들

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:19091';

// 사용자 정보에서 과제 ID 조회 (기존 과제 목록에서 해당 과정의 과제 찾기)
export const getAssignmentIdByUserInfo = async (courseId) => {
  try {
    // 기존 과제 목록 조회 API 사용
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
      } else if (response.status === 403) {
        throw new Error('접근 권한이 없습니다.');
      } else {
        throw new Error('과제 목록 조회에 실패했습니다.');
      }
    }
    
    const assignments = await response.json();
    const assignmentData = assignments.data || assignments || [];
    
    // 해당 과정의 과제 찾기
    const targetAssignment = assignmentData.find(assignment => assignment.courseId === courseId);
    
    if (!targetAssignment) {
      throw new Error('해당 과정의 과제를 찾을 수 없습니다. 과제를 먼저 등록해주세요.');
    }
    
    return {
      data: {
        assignmentId: targetAssignment.assignmentId
      }
    };
  } catch (error) {
    console.error('과제 ID 조회 오류:', error);
    throw error;
  }
};

// 강사가 담당하는 과제 목록 조회
export const getInstructorAssignments = async (memberId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments?memberId=${memberId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
      } else if (response.status === 403) {
        throw new Error('접근 권한이 없습니다.');
      } else {
        throw new Error('과제 목록을 불러오는데 실패했습니다.');
      }
    }
    
    return await response.json();
  } catch (error) {
    console.error('과제 목록 조회 오류:', error);
    throw error;
  }
};

// 과제 등록 (파일 포함)
export const createAssignment = async (payload) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error('과제 등록 서버 응답:', errorText);
      
      if (response.status === 500) {
        throw new Error(`서버 내부 오류: ${errorText}`);
      } else if (response.status === 400) {
        throw new Error(`잘못된 요청: ${errorText}`);
      } else if (response.status === 401) {
        throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
      } else if (response.status === 403) {
        throw new Error('접근 권한이 없습니다.');
      } else {
        throw new Error(`과제 등록에 실패했습니다. (${response.status}): ${errorText}`);
      }
    }
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('과제 등록 오류:', error);
    throw error;
  }
};

// 과제 수정
export const updateAssignment = async (assignmentId, payload) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      throw new Error('과제 수정에 실패했습니다.');
    }
    return await response.json();
  } catch (error) {
    console.error('과제 수정 오류:', error);
    throw error;
  }
};

// 과제 상세 정보 조회
export const getAssignmentDetail = async (assignmentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}`, {
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
    if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
      throw new Error('서버에 연결할 수 없습니다. 백엔드 서버가 실행 중인지 확인해주세요.');
    }
    throw error;
  }
};

// courseId를 기반으로 ROLE_STUDENT인 학생 목록 조회
export const getCourseStudents = async (courseId) => {
  try {
    // 먼저 기존 엔드포인트 시도
    console.log('🔄 강의 학생 목록 조회 시도 1: /api/instructor/courses/${courseId}/students')
    const response = await fetch(`${API_BASE_URL}/api/instructor/courses/${courseId}/students`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (response.ok) {
      const result = await response.json();
      console.log('✅ 강의 학생 목록 조회 성공 (기존 엔드포인트)')
      return { data: result.data || result };
    }
    
    // 기존 엔드포인트가 실패하면 대체 엔드포인트 시도
    console.log('🔄 강의 학생 목록 조회 시도 2: /api/course/students/${courseId}')
    const alternativeResponse = await fetch(`${API_BASE_URL}/api/course/students/${courseId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (alternativeResponse.ok) {
      const result = await alternativeResponse.json();
      console.log('✅ 강의 학생 목록 조회 성공 (대체 엔드포인트)')
      const studentData = result.data || result;
      console.log('대체 엔드포인트에서 받은 학생 데이터:', studentData);
      return { data: studentData };
    }
    
    // 세 번째 엔드포인트 시도: 강사 담당 학생 목록
    console.log('🔄 강의 학생 목록 조회 시도 3: /api/instructor/students')
    const instructorStudentsResponse = await fetch(`${API_BASE_URL}/api/instructor/students`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (instructorStudentsResponse.ok) {
      const result = await instructorStudentsResponse.json();
      console.log('✅ 강의 학생 목록 조회 성공 (강사 담당 학생 엔드포인트)')
      // 강사 담당 학생 목록에서 해당 강의의 학생들만 필터링
      const courseStudents = result.data?.filter(student => 
        student.courseId === courseId || 
        student.lectureId === courseId ||
        student.course?.courseId === courseId
      ) || [];
      return { data: courseStudents };
    }
    
    // 모든 엔드포인트 실패한 경우
    console.warn('❌ 모든 강의 학생 목록 엔드포인트 실패')
    console.warn('기존 엔드포인트 상태:', response.status)
    console.warn('대체 엔드포인트 상태:', alternativeResponse.status)
    console.warn('강사 담당 학생 엔드포인트 상태:', instructorStudentsResponse.status)
    return { data: [] };
    
  } catch (error) {
    console.error('학생 목록 조회 오류:', error);
    if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
      throw new Error('서버에 연결할 수 없습니다. 백엔드 서버가 실행 중인지 확인해주세요.');
    }
    // 기타 오류 시에도 빈 배열 반환
    return { data: [] };
  }
};

// 과제 삭제
export const deleteAssignment = async (assignmentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new Error('과제 삭제에 실패했습니다.');
    }
    
    // 응답이 JSON인지 텍스트인지 확인
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    } else {
      // 텍스트 응답인 경우
      const textResponse = await response.text();
      return { message: textResponse, success: true };
    }
  } catch (error) {
    console.error('과제 삭제 오류:', error);
    throw error;
  }
};

// 과제별 제출 현황 조회
export const getAssignmentSubmissions = async (assignmentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}/submissions`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new Error('제출 현황을 불러오는데 실패했습니다.');
    }
    
    return await response.json();
  } catch (error) {
    console.error('제출 현황 조회 오류:', error);
    
    // 연결 실패 시 더 구체적인 에러 메시지 제공
    if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
      throw new Error('서버에 연결할 수 없습니다. 백엔드 서버가 실행 중인지 확인해주세요.');
    }
    
    throw error;
  }
};

// 과제 채점 제출
export const submitAssignmentGrading = async (submissionId, gradingData) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignmentsubmission/${submissionId}`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(gradingData),
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      console.error('채점 제출 오류 응답:', errorData)
      
      if (response.status === 400) {
        throw new Error(errorData.resultMessage || errorData.message || '잘못된 요청입니다. 입력 데이터를 확인해주세요.');
      } else if (response.status === 404) {
        throw new Error('제출 내역을 찾을 수 없습니다.');
      } else if (response.status === 403) {
        throw new Error('채점 권한이 없습니다.');
      } else {
        throw new Error(errorData.resultMessage || errorData.message || '채점 제출에 실패했습니다.');
      }
    }
    
    const result = await response.json()
    return result;
  } catch (error) {
    console.error('채점 제출 오류:', error);
    if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
      throw new Error('서버에 연결할 수 없습니다. 백엔드 서버가 실행 중인지 확인해주세요.');
    }
    throw error;
  }
};

// 과제 통계 정보 조회
export const getAssignmentStats = async (memberId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/stats?memberId=${memberId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new Error('과제 통계를 불러오는데 실패했습니다.');
    }
    
    return await response.json();
  } catch (error) {
    console.error('과제 통계 조회 오류:', error);
    throw error;
  }
};

// 과제 파일 업로드
export const uploadAssignmentFile = async (assignmentId, file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('assignmentId', assignmentId);

    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}/files`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });
    
    if (!response.ok) {
      throw new Error('파일 업로드에 실패했습니다.');
    }
    
    return await response.json();
  } catch (error) {
    console.error('파일 업로드 오류:', error);
    throw error;
  }
};

// 과제 파일 삭제
export const deleteAssignmentFile = async (assignmentId, fileId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}/files/${fileId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new Error('파일 삭제에 실패했습니다.');
    }
    
    return await response.json();
  } catch (error) {
    console.error('파일 삭제 오류:', error);
    throw error;
  }
};

// ===== 루브릭 관련 API 함수들 =====

// 과제별 루브릭 조회
export const getAssignmentRubric = async (assignmentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}/rubric`, {
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
    
    // 연결 실패 시 더 구체적인 에러 메시지 제공
    if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
      throw new Error('서버에 연결할 수 없습니다. 백엔드 서버가 실행 중인지 확인해주세요.');
    }
    
    throw error;
  }
};

// 과제별 루브릭 생성/수정
export const saveAssignmentRubric = async (assignmentId, rubricitem) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}/rubric`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ rubricitem }),
    });
    
    if (!response.ok) {
      throw new Error('루브릭 저장에 실패했습니다.');
    }
    
    return await response.json();
  } catch (error) {
    console.error('루브릭 저장 오류:', error);
    throw error;
  }
};

// 과제별 루브릭 삭제
export const deleteAssignmentRubric = async (assignmentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}/rubric`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new Error('루브릭 삭제에 실패했습니다.');
    }
    
    return await response.json();
  } catch (error) {
    console.error('루브릭 삭제 오류:', error);
    throw error;
  }
};

// 루브릭 템플릿 목록 조회 (재사용 가능한 템플릿)
export const getRubricTemplates = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/rubric-templates`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new Error('루브릭 템플릿을 불러오는데 실패했습니다.');
    }
    
    return await response.json();
  } catch (error) {
    console.error('루브릭 템플릿 조회 오류:', error);
    throw error;
  }
};

// 루브릭 템플릿 저장
export const saveRubricTemplate = async (templateData) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/instructor/rubric-templates`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(templateData),
    });
    
    if (!response.ok) {
      throw new Error('루브릭 템플릿 저장에 실패했습니다.');
    }
    
    return await response.json();
  } catch (error) {
    console.error('루브릭 템플릿 저장 오류:', error);
    throw error;
  }
}; 

// 강사용: 과제 제출 파일 조회 (학생 제출 파일을 강사가 조회)
// 임시로 다시 활성화하여 사용
export const getInstructorSubmissionFiles = async (assignmentId, courseId, submissionId, studentId) => {
  try {
    console.log('🔍 강사용 과제 제출 파일 조회 시작:', { assignmentId, courseId, submissionId, studentId });
    
    // 기존 강사용 엔드포인트 사용
    const response = await fetch(`${API_BASE_URL}/api/instructor/assignments/${assignmentId}/submission-files`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      if (response.status === 404) {
        console.log('🔍 제출 파일이 없습니다. 빈 배열 반환');
        return [];
      } else {
        throw new Error(`강사용 과제 제출 파일 조회에 실패했습니다. (${response.status})`);
      }
    }
    
    const result = await response.json();
    console.log('🔍 강사용 과제 제출 파일 조회 결과:', result);
    
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
    
    console.log('🔍 처리된 파일 배열 (필터링 전):', files);
    console.log('🔍 필터링할 studentId:', studentId);
    
    // 임시로 필터링 제거 - 모든 파일 반환
    console.log('🔍 필터링 제거됨 - 모든 파일 반환');
    
    console.log('🔍 최종 반환할 파일 배열:', files);
    return files;
  } catch (error) {
    console.error('🔍 강사용 과제 제출 파일 조회 오류:', error);
    return [];
  }
}; 