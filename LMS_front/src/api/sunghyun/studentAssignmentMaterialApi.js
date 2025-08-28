// studentAssignmentMaterialApi.js
import { toast } from 'sonner';

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:19091';

// 파일 업로드 관련 API 엔드포인트
const API_ENDPOINTS = {
  GENERATE_PRESIGNED_URL: '/api/v2/file/upload', // Presigned URL 발급
  UPLOAD_SUCCESS: '/api/v2/file/upload/success', // 업로드 성공 처리
  UPLOAD_IMAGE: '/api/v2/file/upload-image', // 직접 이미지 업로드
  UPLOAD_FILE: '/api/v2/file/upload-file', // 직접 파일 업로드
  UPLOAD_AUDIO: '/api/v2/file/upload-audio', // 직접 오디오 업로드
  GET_IMAGE: '/api/v2/file/image', // 이미지 조회
};

// 파일 업로드 서비스 훅
export const useFileUploadService = () => {

  /**
   * Presigned URL 발급 요청
   * @param {string} fileName - 업로드할 파일 이름
   * @returns {Promise<Object>} Presigned URL 정보
   */
  const generatePresignedUrl = async (fileName) => {
    try {
      if (!fileName) {
        throw new Error('파일 이름이 없습니다.');
      }

      const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.GENERATE_PRESIGNED_URL}/${encodeURIComponent(fileName)}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
      });
      
      if (!response.ok) {
        throw new Error('Presigned URL 발급에 실패했습니다.');
      }
      
      const result = await response.json();
      return result.data || result;
    } catch (error) {
      console.error('Presigned URL 발급 오류:', error);
      toast.error(error.message || 'Presigned URL 발급에 실패했습니다.');
      throw error;
    }
  };

  /**
   * Presigned URL을 통한 파일 업로드
   * @param {File} file - 업로드할 파일
   * @param {string} presignedUrl - 발급받은 Presigned URL
   * @returns {Promise<boolean>} 업로드 성공 여부
   */
  const uploadToPresignedUrl = async (file, presignedUrl) => {
    try {
      if (!file || !presignedUrl) {
        throw new Error('파일 또는 Presigned URL이 없습니다.');
      }

      // Presigned URL에 직접 PUT 요청으로 파일 업로드
      const response = await fetch(presignedUrl, {
        method: 'PUT',
        body: file,
        headers: {
          'Content-Type': file.type,
        },
      });

      if (response.ok) {
        return true;
      }

      throw new Error(`파일 업로드에 실패했습니다. 상태 코드: ${response.status}`);
    } catch (error) {
      console.error('파일 업로드 오류:', error);
      toast.error(error.message || '파일 업로드에 실패했습니다.');
      throw error;
    }
  };

  /**
   * 업로드 성공 처리
   * @param {Object} fileInfo - 파일 정보 (key, name, type 등)
   * @param {string} ownerId - 파일 소유자 ID
   * @param {string} memberType - 소유자 타입 (USER, MEMBER 등)
   * @param {Object} assignmentInfo - 과제 정보 (assignmentId, courseId, studentId, submissionId)
   * @returns {Promise<Object>} 저장된 파일 정보
   */
  const handleUploadSuccess = async (fileInfo, ownerId, memberType = 'USER', assignmentInfo = null) => {
    try {
      if (!fileInfo || !fileInfo.key) {
        throw new Error('파일 정보가 없습니다.');
      }

      const requestData = {
        ...fileInfo,
      };

      // URL 파라미터 구성
      const params = new URLSearchParams();
      if (ownerId) params.append('ownerId', ownerId);
      if (memberType) params.append('memberType', memberType);
      
      // 과제 제출 파일인 경우 추가 파라미터 전달
      if (assignmentInfo) {
        if (assignmentInfo.assignmentId) params.append('assignmentId', assignmentInfo.assignmentId);
        if (assignmentInfo.courseId) params.append('courseId', assignmentInfo.courseId);
        if (assignmentInfo.studentId) params.append('studentId', assignmentInfo.studentId);
        if (assignmentInfo.submissionId) params.append('submissionId', assignmentInfo.submissionId);
      }

      const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.UPLOAD_SUCCESS}?${params.toString()}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(requestData),
      });
      
      if (!response.ok) {
        throw new Error('파일 업로드 완료 처리에 실패했습니다.');
      }
      
      const result = await response.json();
      return result.data || result;
    } catch (error) {
      console.error('파일 업로드 완료 처리 오류:', error);
      toast.error(error.message || '파일 업로드 완료 처리에 실패했습니다.');
      throw error;
    }
  };

  /**
   * Presigned URL 방식으로 파일 업로드 (전체 과정)
   * @param {File} file - 업로드할 파일
   * @param {Object} options - 업로드 옵션 (ownerId, memberType, assignmentInfo 등)
   * @returns {Promise<Object>} 업로드된 파일 정보
   */
  const uploadFileWithPresignedUrl = async (file, options = {}) => {
    try {
      if (!file) {
        throw new Error('파일이 없습니다.');
      }

      const { ownerId = '', memberType = 'USER', assignmentInfo = null } = options;
      
      // 1. Presigned URL 발급
      const presignedUrlInfo = await generatePresignedUrl(file.name);
      
      // 2. Presigned URL에 파일 업로드
      await uploadToPresignedUrl(file, presignedUrlInfo.url);
      
      // 파일 정보 구성
      let fileType = 'file';
      if (file.type.startsWith('image/')) {
        fileType = 'image';
      } else if (file.type.startsWith('video/')) {
        fileType = 'video';
      } else if (file.type.startsWith('audio/')) {
        fileType = 'audio';
      }
      
      // 파일 크기 및 차원 정보
      let width = 0;
      let height = 0;
      
      if (fileType === 'image') {
        // 이미지 파일의 경우 크기 정보 추출
        const imageInfo = await getImageDimensions(file);
        width = imageInfo.width;
        height = imageInfo.height;
      }
      
      // 3. 업로드 성공 처리
      const fileInfo = {
        key: presignedUrlInfo.key,
        name: file.name,
        type: fileType,
        width,
        height,
        size: file.size,
        index: 0,
        isActive: true,
        thumbnail_key: presignedUrlInfo.key, // 기본적으로 동일한 키 사용
        duration: 0
      };
      
      const result = await handleUploadSuccess(fileInfo, ownerId, memberType, assignmentInfo);
      return result;
    } catch (error) {
      console.error('파일 업로드 오류:', error);
      toast.error(error.message || '파일 업로드에 실패했습니다.');
      throw error;
    }
  };

  /**
   * 이미지 파일의 크기(width, height) 얻기
   * @param {File} imageFile - 이미지 파일
   * @returns {Promise<Object>} 이미지 크기 정보
   */
  const getImageDimensions = (imageFile) => {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => {
        resolve({ width: img.width, height: img.height });
      };
      img.onerror = () => {
        reject(new Error('이미지 크기를 가져오는데 실패했습니다.'));
      };
      img.src = URL.createObjectURL(imageFile);
    });
  };

  /**
   * 기존 방식으로 이미지 파일 업로드 (멀티파트)
   * @param {File} file - 업로드할 이미지 파일
   * @param {Object} options - 업로드 옵션
   * @returns {Promise<Object>} 업로드된 이미지 정보
   */
  const uploadImage = async (file, options = {}) => {
    try {
      if (!file) {
        throw new Error('파일이 없습니다.');
      }

      // 이미지 파일인지 확인
      if (!file.type.startsWith('image/')) {
        throw new Error('이미지 파일이 아닙니다.');
      }

      // FormData 생성
      const formData = new FormData();
      formData.append('file', file);
      
      // 기본 옵션 설정
      const { ownerId = '', memberType = 'USER' } = options;
      formData.append('ownerId', ownerId);
      formData.append('memberType', memberType);
      
      // 추가 옵션이 있는 경우 FormData에 추가
      Object.entries(options).forEach(([key, value]) => {
        if (key !== 'ownerId' && key !== 'memberType') {
          formData.append(key, value);
        }
      });

      // API 호출
      const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.UPLOAD_IMAGE}`, {
        method: 'POST',
        credentials: 'include',
        body: formData,
      });

      if (!response.ok) {
        throw new Error('이미지 업로드에 실패했습니다.');
      }
      
      const result = await response.json();
      return result.data || result;
    } catch (error) {
      console.error('이미지 업로드 오류:', error);
      toast.error(error.message || '이미지 업로드에 실패했습니다.');
      throw error;
    }
  };

  /**
   * 기존 방식으로 일반 파일 업로드 (멀티파트)
   * @param {File} file - 업로드할 파일
   * @param {Object} options - 업로드 옵션
   * @returns {Promise<Object>} 업로드된 파일 정보
   */
  const uploadFile = async (file, options = {}) => {
    try {
      if (!file) {
        throw new Error('파일이 없습니다.');
      }

      // FormData 생성
      const formData = new FormData();
      formData.append('file', file);
      
      // 기본 옵션 설정
      const { ownerId = '', memberType = 'USER' } = options;
      formData.append('ownerId', ownerId);
      formData.append('memberType', memberType);
      
      // 추가 옵션이 있는 경우 FormData에 추가
      Object.entries(options).forEach(([key, value]) => {
        if (key !== 'ownerId' && key !== 'memberType') {
          formData.append(key, value);
        }
      });

      // API 호출
      const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.UPLOAD_FILE}`, {
        method: 'POST',
        credentials: 'include',
        body: formData,
      });

      if (!response.ok) {
        throw new Error('파일 업로드에 실패했습니다.');
      }
      
      const result = await response.json();
      return result.data || result;
    } catch (error) {
      console.error('파일 업로드 오류:', error);
      toast.error(error.message || '파일 업로드에 실패했습니다.');
      throw error;
    }
  };

  /**
   * 여러 파일 업로드
   * @param {FileList|Array<File>} files - 업로드할 파일들
   * @param {Object} options - 업로드 옵션
   * @returns {Promise<Array<Object>>} 업로드된 파일들의 정보
   */
  const uploadMultipleFiles = async (files, options = {}) => {
    try {
      if (!files || files.length === 0) {
        throw new Error('업로드할 파일이 없습니다.');
      }

      const fileArray = Array.from(files);
      const uploadPromises = fileArray.map(file => {
        // Presigned URL 방식으로 업로드
        return uploadFileWithPresignedUrl(file, options)
          .then(result => ({
            ...result,
            originalFile: file,
            type: file.type.startsWith('image/') ? 'image' : 'file'
          }));
      });

      return await Promise.all(uploadPromises);
    } catch (error) {
      console.error('다중 파일 업로드 오류:', error);
      toast.error(error.message || '파일 업로드에 실패했습니다.');
      throw error;
    }
  };

  /**
   * 파일 ID로 이미지 URL 가져오기
   * @param {string} fileId - 파일 ID
   * @returns {string} 이미지 URL
   */
  const getImageUrl = (fileId) => {
    if (!fileId) return '';
    return `${API_ENDPOINTS.GET_IMAGE}/${fileId}`;
  };

  return {
    generatePresignedUrl,
    uploadToPresignedUrl,
    handleUploadSuccess,
    uploadFileWithPresignedUrl,
    uploadImage,
    uploadFile,
    uploadMultipleFiles,
    getImageUrl,
    getImageDimensions
  };
};

// 기존 API 함수들
export const getPresignedUrl = async (fileName) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/file/upload/${encodeURIComponent(fileName)}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new Error('Presigned URL 발급에 실패했습니다.');
    }
    
    const result = await response.json();
    return result.data || result;
  } catch (error) {
    console.error('Presigned URL 발급 오류:', error);
    throw error;
  }
};

export const uploadToS3 = async (presignedUrl, file) => {
  try {
    const response = await fetch(presignedUrl, {
      method: 'PUT',
      body: file,
      headers: {
        'Content-Type': file.type,
      },
    });
    return response.ok;
  } catch (error) {
    console.error('S3 업로드 오류:', error);
    throw error;
  }
};

export const completeFileUpload = async (fileData) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/file/upload/success`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(fileData),
    });
    
    if (!response.ok) {
      throw new Error('파일 업로드 완료 처리에 실패했습니다.');
    }
    
    const result = await response.json();
    return result.data || result;
  } catch (error) {
    console.error('파일 업로드 완료 처리 오류:', error);
    throw error;
  }
};

export const uploadStudentAssignmentFile = async (assignmentId, formData) => {
  try {
    console.log('📤 학생 과제 파일 업로드 시작 - assignmentId:', assignmentId);
    console.log('📤 FormData 내용:');
    for (let [key, value] of formData.entries()) {
      console.log(`📤 ${key}:`, value);
    }
    
    const response = await fetch(`${API_BASE_URL}/api/v2/student/assignment/${assignmentId}/file`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });
    
    console.log('📤 응답 상태:', response.status, response.statusText);
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error('📤 에러 응답:', errorText);
      throw new Error(`학생 과제 파일 업로드에 실패했습니다. (${response.status}: ${errorText})`);
    }
    
    const result = await response.json();
    console.log('📤 업로드 성공 응답:', result);
    
    // 다양한 응답 구조 처리
    if (result && typeof result === 'object') {
      if (result.data) {
        return result.data;
      } else if (result.result) {
        return result.result;
      } else if (result.response && result.response.data) {
        return result.response.data;
      } else {
        return result;
      }
    }
    
    return result;
  } catch (error) {
    console.error('📤 학생 과제 파일 업로드 오류:', error);
    throw error;
  }
};

export const getStudentSubmissionFiles = async (assignmentId) => {
  try {
    console.log('🔍 학생 제출 파일 목록 조회 시작 - assignmentId:', assignmentId);
    
    // 과제 자료와 비슷한 패턴으로 학생 제출 파일 조회
    const response = await fetch(`${API_BASE_URL}/api/student/assignments/${assignmentId}/submissions/files`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    console.log('🔍 응답 상태:', response.status, response.statusText);
    
    if (!response.ok) {
      if (response.status === 404) {
        console.log('🔍 제출 파일이 없습니다. 빈 배열 반환');
        return [];
      } else {
        throw new Error(`학생 제출 파일 목록 조회에 실패했습니다. (${response.status})`);
      }
    }
    
    const result = await response.json();
    console.log('🔍 원본 응답:', result);
    
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
    
    console.log('🔍 파싱된 파일 목록:', files);
    return files;
  } catch (error) {
    console.error('🔍 학생 제출 파일 목록 조회 오류:', error);
    // 에러가 발생해도 빈 배열 반환하여 UI가 깨지지 않도록 함
    return [];
  }
};

export const deleteStudentSubmissionFile = async (assignmentId, fileId) => {
  try {
    console.log('🗑️ 학생 제출 파일 삭제 시작:', { assignmentId, fileId });
    
    // FileV2Controller의 deleteFile 엔드포인트 사용
    const response = await fetch(`${API_BASE_URL}/api/v2/file/${fileId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    console.log('🗑️ 삭제 응답 상태:', response.status, response.statusText);
    
    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('삭제할 파일을 찾을 수 없습니다.');
      } else if (response.status === 403) {
        throw new Error('파일 삭제 권한이 없습니다.');
      } else {
        throw new Error(`학생 제출 파일 삭제에 실패했습니다. (${response.status})`);
      }
    }
    
    const result = await response.json();
    console.log('🗑️ 삭제 성공 응답:', result);
    return result.data || result;
  } catch (error) {
    console.error('🗑️ 학생 제출 파일 삭제 오류:', error);
    throw error;
  }
};

export const downloadStudentSubmissionFile = async (fileKey, fileName) => {
  try {
    console.log('📥 학생 제출 파일 다운로드 시작:', { fileKey, fileName });
    
    // 백엔드 API에 맞게 POST 요청으로 변경하고 RequestFileDTO 형태로 전송
    const response = await fetch(`${API_BASE_URL}/api/v2/file/download`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        key: fileKey,
        name: fileName
      }),
    });
    
    console.log('📥 다운로드 응답:', response);
    console.log('📥 Content-Type:', response.headers.get('content-type'));
    console.log('📥 파일 크기:', response.headers.get('content-length'), 'bytes');

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('파일을 찾을 수 없습니다. 파일이 삭제되었거나 경로가 잘못되었을 수 있습니다.'); 
      } else if (response.status === 403) {
        throw new Error('파일 다운로드 권한이 없습니다.');
      } else if (response.status === 401) {
        throw new Error('로그인이 필요합니다.');
      } else {
        throw new Error(`파일 다운로드에 실패했습니다: ${response.status}`);
      }
    }

    const blob = await response.blob();
    console.log('📥 Blob 생성 완료:', blob.size, 'bytes');
    
    // 실제 파일 다운로드 실행
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName || 'download';
    
    // 다운로드 링크 클릭
    document.body.appendChild(link);
    link.click();
    
    // 정리
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    
    console.log('📥 파일 다운로드 완료:', fileName);
    return blob;
  } catch (error) {
    console.error('📥 학생 제출 파일 다운로드 오류:', error);

    if (error.code === 'ERR_NETWORK') {
      throw new Error('네트워크 오류가 발생했습니다. 백엔드 서버가 실행 중인지 확인해주세요.');       
    } else {
      throw new Error(`파일 다운로드에 실패했습니다: ${error.message}`);
    }
  }
}; 

// 과제 상세 모달에서 사용할 제출 파일 조회 함수 (해당 강의의 해당 과제에 업로드된 자료만)
export const getStudentSubmissionFilesForDetail = async (assignmentId, courseId, submissionId) => {
  try {
    console.log('🔍 파일 조회 파라미터:', { assignmentId, courseId, submissionId });
    
    // submissionId를 사용해서 파일 조회
    const url = `/api/student/assignments/${assignmentId}/submission-files?submissionId=${submissionId}`;
      
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include'
    });
    
    if (!response.ok) {
      if (response.status === 404) {
        return [];
      } else {
        const errorText = await response.text();
        throw new Error(`제출 파일 조회에 실패했습니다. (${response.status})`);
      }
    }
    
    const result = await response.json();
    console.log('🔍 파일 조회 응답:', result);
    
    // 응답 구조에 따라 파일 배열 추출
    let files = [];
    if (result && result.data && Array.isArray(result.data)) {
      files = result.data;
    } else if (result && Array.isArray(result)) {
      files = result;
    }
    
    console.log('🔍 추출된 파일 목록:', files);
    return files;
  } catch (error) {
    // 네트워크 오류나 기타 오류가 발생해도 빈 배열 반환하여 UI가 깨지지 않도록 함
    console.warn('제출 파일 조회 실패:', error);
    return [];
  }
}; 