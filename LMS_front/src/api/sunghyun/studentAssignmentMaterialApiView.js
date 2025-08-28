import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api/instructor';

// 과제 자료 목록 조회
export const getAssignmentMaterials = async (assignmentId, userId = null) => {
  try {
    const params = userId ? { userId } : {};
    const response = await axios.get(`${BASE_URL}/assignments/${assignmentId}/materials`, { params });
    return response.data;
  } catch (error) {
    console.error('과제 자료 목록 조회 실패:', error);
    throw error;
  }
};

// 과제 자료 다운로드 (materialId 사용)
export const downloadAssignmentMaterial = async (materialId, userId = null) => {
  try {
    console.log('🔍 downloadAssignmentMaterial 시작:', { materialId, userId });
    console.log('🔍 BASE_URL:', BASE_URL);
    
    const params = userId ? { userId } : {};
    console.log('🔍 요청 파라미터:', params);
    
    const url = `${BASE_URL}/assignments/materials/${materialId}/download`;
    console.log('🔍 요청 URL:', url);
    
    const response = await axios.get(url, { params });
    console.log('🔍 응답 성공:', response.status, response.statusText);
    console.log('🔍 응답 데이터:', response.data);
    
    return response.data;
  } catch (error) {
    console.error('❌ 과제 자료 다운로드 실패:', error);
    console.error('❌ 에러 상세:', {
      message: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      config: {
        url: error.config?.url,
        method: error.config?.method,
        params: error.config?.params
      }
    });
    throw error;
  }
};

// 과제 자료 다운로드 (fileKey 사용)
export const downloadAssignmentFile = async (fileKey, userId = null) => {
  try {
    const params = userId ? { userId } : {};
    const response = await axios.get(`${BASE_URL}/assignment/file/download/${fileKey}`, { params });
    return response.data;
  } catch (error) {
    console.error('과제 파일 다운로드 실패:', error);
    throw error;
  }
};

// 과제 자료 다운로드 URL 생성
export const generateAssignmentFileDownloadUrl = async (materialId) => {
  try {
    const response = await axios.get(`${BASE_URL}/assignment/file/download/url/${materialId}`);
    return response.data;
  } catch (error) {
    console.error('과제 파일 다운로드 URL 생성 실패:', error);
    throw error;
  }
};

// 과제 자료 다운로드 (materialId로 직접 다운로드)
export const downloadAssignmentFileByMaterialId = async (materialId, userId = null) => {
  try {
    const params = userId ? { userId } : {};
    const response = await axios.get(`${BASE_URL}/assignment/file/download/material/${materialId}`, { params });
    return response.data;
  } catch (error) {
    console.error('materialId로 과제 파일 다운로드 실패:', error);
    throw error;
  }
};

// 과제 제출 파일 관련 API (AssignmentSubmissionFileDTO 기반)
export const getAssignmentSubmissionFiles = async (submissionId) => {
  try {
    const response = await axios.get(`${BASE_URL}/assignments/submissions/${submissionId}/files`);
    return response.data;
  } catch (error) {
    console.error('과제 제출 파일 목록 조회 실패:', error);
    throw error;
  }
};

// 과제 제출 파일 업로드
export const uploadAssignmentSubmissionFile = async (submissionId, fileData) => {
  try {
    const response = await axios.post(`${BASE_URL}/assignments/submissions/${submissionId}/files`, fileData);
    return response.data;
  } catch (error) {
    console.error('과제 제출 파일 업로드 실패:', error);
    throw error;
  }
};

// 과제 제출 파일 삭제
export const deleteAssignmentSubmissionFile = async (submissionId, fileId) => {
  try {
    const response = await axios.delete(`${BASE_URL}/assignments/submissions/${submissionId}/files/${fileId}`);
    return response.data;
  } catch (error) {
    console.error('과제 제출 파일 삭제 실패:', error);
    throw error;
  }
};

// 과제 제출 상태 조회
export const getAssignmentSubmissionStatus = async (assignmentId, studentId) => {
  try {
    const response = await axios.get(`${BASE_URL}/assignments/${assignmentId}/submissions/${studentId}/status`);
    return response.data;
  } catch (error) {
    console.error('과제 제출 상태 조회 실패:', error);
    throw error;
  }
};

// 과제 제출 정보 조회
export const getAssignmentSubmission = async (submissionId) => {
  try {
    const response = await axios.get(`${BASE_URL}/assignments/submissions/${submissionId}`);
    return response.data;
  } catch (error) {
    console.error('과제 제출 정보 조회 실패:', error);
    throw error;
  }
};

// 과제 제출 생성/수정
export const submitAssignment = async (assignmentId, submissionData) => {
  try {
    const response = await axios.post(`${BASE_URL}/assignments/${assignmentId}/submissions`, submissionData);
    return response.data;
  } catch (error) {
    console.error('과제 제출 실패:', error);
    throw error;
  }
};

// 과제 제출 수정
export const updateAssignmentSubmission = async (submissionId, submissionData) => {
  try {
    const response = await axios.put(`${BASE_URL}/assignments/submissions/${submissionId}`, submissionData);
    return response.data;
  } catch (error) {
    console.error('과제 제출 수정 실패:', error);
    throw error;
  }
};

// 업로드된 파일들을 assignment_submission_file 테이블에 연결
export const connectSubmissionFiles = async (files) => {
  try {
    const response = await axios.post(`${BASE_URL}/connect-files`, files);
    return response.data;
  } catch (error) {
    console.error('파일 연결 실패:', error);
    throw error;
  }
};

// 파일들의 submission_id를 업데이트
export const updateSubmissionFileIds = async (fileIds, submissionId) => {
  try {
    const response = await axios.put(`${BASE_URL}/update-submission-file-ids`, {
      fileIds: fileIds,
      submissionId: submissionId
    });
    return response.data;
  } catch (error) {
    console.error('파일 submission_id 업데이트 실패:', error);
    throw error;
  }
};

// 파일 다운로드 헬퍼 함수
export const downloadFile = async (url, fileName) => {
  try {
    console.log('🔍 downloadFile 시작:', { url, fileName });
    
    const response = await axios.get(url, {
      responseType: 'blob'
    });
    
    console.log('🔍 blob 응답 성공:', response.status, response.statusText);
    console.log('🔍 blob 크기:', response.data.size, 'bytes');
    
    const blob = new Blob([response.data]);
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
    
    console.log('✅ downloadFile 완료:', fileName);
  } catch (error) {
    console.error('❌ 파일 다운로드 실패:', error);
    console.error('❌ 에러 상세:', {
      message: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      url: url,
      fileName: fileName
    });
    throw error;
  }
};

// API 응답 처리를 위한 헬퍼 함수
export const handleApiResponse = (response) => {
  if (response.resultCode === 200) {
    return response.data;
  } else {
    throw new Error(response.resultMessage || 'API 호출 실패');
  }
};

// 에러 처리를 위한 헬퍼 함수
export const handleApiError = (error) => {
  if (error.response) {
    // 서버에서 응답이 왔지만 에러 상태인 경우
    const errorMessage = error.response.data?.resultMessage || error.response.data?.message || '서버 오류가 발생했습니다.';
    throw new Error(errorMessage);
  } else if (error.request) {
    // 요청은 보냈지만 응답을 받지 못한 경우
    throw new Error('서버에 연결할 수 없습니다.');
  } else {
    // 요청 자체를 보내지 못한 경우
    throw new Error('네트워크 오류가 발생했습니다.');
  }
};
