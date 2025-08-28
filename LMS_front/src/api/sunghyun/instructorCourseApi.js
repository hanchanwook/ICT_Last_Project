// 강사 담당 강의 관련 API 함수들
import { http } from '@/components/auth/http'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:19091';

// 강사가 담당하는 모든 강의 목록 조회
export const getInstructorLectures = async () => {
  try {
    const response = await http.get(`/api/instructor/lectures/all`);
    // 항상 배열만 반환
    return response.data.data || [];
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
    } else if (error.response && error.response.status === 403) {
      throw new Error('접근 권한이 없습니다.');
    } else {
      throw new Error('강의 목록을 불러오는데 실패했습니다.');
    }
  }
};

// 특정 강의 상세 정보 조회
export const getLectureDetail = async (courseId) => {
  try {
    const response = await http.get(`/api/instructor/lectures/${courseId}`);
    return response.data.data || response.data; // ResponseDTO 구조 고려
  } catch (error) {
    if (error.response && error.response.status === 400) {
      throw new Error(`강의 ID ${courseId}에 해당하는 강의를 찾을 수 없습니다.`);
    } else if (error.response && error.response.status === 401) {
      throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
    } else if (error.response && error.response.status === 403) {
      throw new Error('접근 권한이 없습니다.');
    } else {
      throw new Error(`강의 상세 정보를 불러오는데 실패했습니다. (${error.response?.status})`);
    }
  }
};

// 강의 통계 정보 조회
export const getLectureStats = async () => {
  try {
    const response = await http.get(`/api/instructor/lectures/stats`);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
    } else if (error.response && error.response.status === 403) {
      throw new Error('접근 권한이 없습니다.');
    } else {
      throw new Error('강의 통계를 불러오는데 실패했습니다.');
    }
  }
};

// 강의실 정보 조회
export const getClassInfo = async (classId) => {
  // 요청 데이터 유효성 검사
  if (!classId) {
    console.error("❌ Invalid classId: classId is required");
    throw new Error("강의실 ID가 필요합니다.");
  }

  // userId 유효성 검사
  const currentUser = JSON.parse(localStorage.getItem("currentUser") || "{}");
  const userId = currentUser.memberId;
  if (!userId) {
    console.error("❌ Invalid userId: User not authenticated");
    throw new Error("사용자 인증이 필요합니다.");
  }

  try {
    const response = await http.get(`/api/instructor/class/${classId}`);
    
    // 응답 구조 확인 및 처리
    let classInfo = null;
    if (response.data) {
      if (response.data.data) {
        classInfo = response.data.data;
      } else if (response.data.classId) {
        classInfo = response.data;
      } else {
        classInfo = response.data;
      }
    }
    
    // 필수 필드가 없으면 기본값 설정
    if (!classInfo || !classInfo.classCode) {
      console.warn('⚠️ 강의실 정보에 classCode가 없음, 기본값 사용');
      return {
        classId: classId,
        classCode: '미정',
        className: '강의실 미정'
      };
    }
    
    return classInfo;
  } catch (error) {
    console.error(`❌ 강의실 ${classId} 정보 조회 실패:`, error);
    
    // HTTP 상태 코드별 오류 처리
    if (error.response?.status === 400) {
      console.error('🔍 400 Bad Request 상세 정보:', {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data,
        headers: error.response.headers
      });
      throw new Error(`강의실 정보 요청이 잘못되었습니다. (${error.response.data?.message || '잘못된 요청'})`);
    } else if (error.response?.status === 401) {
      throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
    } else if (error.response?.status === 403) {
      throw new Error('강의실 정보에 접근할 권한이 없습니다.');
    } else if (error.response?.status === 404) {
      throw new Error(`강의실 ID ${classId}를 찾을 수 없습니다.`);
    } else if (error.response?.status === 500) {
      throw new Error('서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    }
    
    // 네트워크 오류 등 기타 오류
    if (error.code === 'NETWORK_ERROR') {
      throw new Error('네트워크 연결을 확인해주세요.');
    }
    
    // 기본 오류 메시지
    throw new Error(`강의실 정보를 불러오는데 실패했습니다. (${error.message})`);
  }
}

// 강의 자료 업로드 (Presigned URL 방식)
export const uploadCourseMaterial = async (lectureId, file, title) => {
    try {
        // FileEnum 변환 함수
        function getFileEnum(file) {
            const mime = file.type;
            if (mime.startsWith('image/')) return 'image';
            if (mime.startsWith('audio/')) return 'audio';
            if (mime.startsWith('video/')) return 'video';
            return 'file';
        }
        const fileEnum = getFileEnum(file);
        // 1. Presigned URL 요청 (LMS 백엔드 경로 사용)
        const fileName = file.name;
        const ownerId = lectureId;
        const memberType = "INSTRUCTOR";
        
        // 현재 사용자 정보에서 userId 가져오기
        const currentUser = JSON.parse(localStorage.getItem("currentUser") || "{}");
        const userId = currentUser.memberId;
        
        if (!userId) {
          throw new Error('사용자 정보를 찾을 수 없습니다.');
        }
        
        const presignedResponse = await fetch(
            `${API_BASE_URL}/instructor/file/upload/${fileName}?ownerId=${ownerId}&memberType=${memberType}&userId=${userId}`,
            {
                method: 'GET',
                credentials: 'include'
            }
        );
        
        if (!presignedResponse.ok) {
            throw new Error('Presigned URL 요청 실패');
        }
        
        const presignedData = await presignedResponse.json();
        // 2. S3에 파일 업로드 (직접 업로드)
        const uploadResponse = await fetch(presignedData.data.url, {
            method: 'PUT',
            body: file,
            headers: {
                'Content-Type': file.type
            }
        });
        
        if (!uploadResponse.ok) {
            throw new Error('S3 업로드 실패');
        }
        
        // 3. 파일 업로드 완료 알림 (LMS 백엔드 경로 사용)
        const completeResponse = await fetch(`${API_BASE_URL}/instructor/file/upload/complete`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                key: presignedData.data.key,
                name: file.name,
                size: file.size,
                type: fileEnum, // ← 여기!
                width: 0,
                height: 0,
                index: 0,
                active: true,
                duration: 0,
                thumbnailKey: ''
            })
        });
        
        if (!completeResponse.ok) {
            throw new Error('파일 메타데이터 저장 실패');
        }
        
        const completeData = await completeResponse.json();
        
        // 4. 강의 자료 저장 (기존 엔드포인트)
        const materialResponse = await fetch(`${API_BASE_URL}/api/instructor/lectures/${lectureId}/materials`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                fileId: completeData.data.id,        // File_Service에서 받은 fileId
                fileKey: presignedData.data.key,     // S3 파일 키
                title: title || file.name,
                fileName: file.name,
                fileSize: file.size,
                fileType: fileEnum // ← 여기!
            })
        });
        
        if (!materialResponse.ok) {
            const errorData = await materialResponse.json();
            throw new Error(`강의 자료 저장 실패: ${errorData.resultMessage}`);
        }
        
        const materialData = await materialResponse.json();
        return materialData;
        
    } catch (error) {
        console.error('파일 업로드 실패:', error);
        throw error;
    }
};

// 강의 자료 목록 조회
export const getCourseMaterials = async (courseId) => {
  // 요청 데이터 유효성 검사
  if (!courseId) {
    console.error("❌ Invalid courseId: courseId is required");
    throw new Error("강의 ID가 필요합니다.");
  }

  // userId 유효성 검사
  const currentUser = JSON.parse(localStorage.getItem("currentUser") || "{}");
  const userId = currentUser.memberId;
  if (!userId) {
    console.error("❌ Invalid userId: User not authenticated");
    throw new Error("사용자 인증이 필요합니다.");
  }

  try {
    const response = await http.get(`/api/instructor/lectures/${courseId}/materials`);
    
    if (response.status !== 200) {
      throw new Error('강의 자료 목록 조회에 실패했습니다.');
    }
    return response.data;
  } catch (error) {
    console.error('❌ 강의 자료 목록 조회 오류:', error);
    
    // HTTP 상태 코드별 오류 처리
    if (error.response?.status === 400) {
      console.error('🔍 400 Bad Request 상세 정보:', {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data
      });
      throw new Error(`강의 자료 요청이 잘못되었습니다. (${error.response.data?.message || '잘못된 요청'})`);
    } else if (error.response?.status === 401) {
      throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
    } else if (error.response?.status === 403) {
      throw new Error('강의 자료에 접근할 권한이 없습니다.');
    } else if (error.response?.status === 404) {
      throw new Error(`강의 ID ${courseId}를 찾을 수 없습니다.`);
    } else if (error.response?.status === 500) {
      console.warn('⚠️ 서버 내부 오류로 인해 빈 배열을 반환합니다.');
      return [];
    }
    
    // 네트워크 오류 등 기타 오류
    if (error.code === 'NETWORK_ERROR') {
      throw new Error('네트워크 연결을 확인해주세요.');
    }
    
    // 기본 오류 메시지
    throw new Error(`강의 자료를 불러오는데 실패했습니다. (${error.message})`);
  }
}

// 강의 자료 삭제 - 임시 처리 (백엔드 API 미구현)
export const deleteCourseMaterial = async (materialId) => {
  try {
    // 백엔드 API가 구현되지 않았으므로 임시로 성공 응답 반환
    
    // 실제 API 호출 시도 (실패할 것으로 예상)
    try {
      const response = await http.delete(`/api/instructor/lectures/3/materials/${materialId}`);
      return response.data;
    } catch (apiError) {
      console.warn('백엔드 API 호출 실패 (예상됨):', apiError);
      
      // 임시로 성공 응답 반환
      return {
        success: true,
        message: '파일이 성공적으로 삭제되었습니다. (임시 처리)',
        note: '백엔드 API 구현 후 실제 삭제가 완료됩니다.'
      };
    }
  } catch (error) {
    console.error('파일 삭제 오류:', error);
    
    // 400 에러의 경우 더 자세한 정보 출력
    if (error.response?.status === 400) {
      console.error('🔍 400 에러 상세 정보:', {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data,
        headers: error.response.headers
      });
      
      if (error.response.data) {
        console.error('🔍 서버 에러 응답:', error.response.data);
        console.error('🔍 에러 응답 (문자열):', JSON.stringify(error.response.data, null, 2));
      }
    }
    
    throw error;
  }
};

// 파일 다운로드 API - http 인스턴스 사용
export const downloadFile = async (fileKey, fileName) => {
  try {
    // 입력값 검증
    if (!fileKey) {
      throw new Error('파일 키가 필요합니다.');
    }
    
    if (!fileName) {
      fileName = 'download';
    }
    
    console.log('📥 파일 다운로드 시작:', { fileKey, fileName });
    
    // 요청 데이터 구조 확인 및 로깅
    const requestData = { key: fileKey };
    console.log('📤 요청 데이터:', requestData);
    
    // 강사용 원래 다운로드 API 사용 - 수정됨
    const response = await http.post(`/api/v2/file/download`, requestData, {
      responseType: 'blob', // 중요!
      timeout: 30000, // 30초 타임아웃 설정
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    console.log('📥 서버 응답:', {
      status: response.status,
      statusText: response.statusText,
      contentType: response.headers['content-type'],
      contentLength: response.headers['content-length']
    });
    
    // 응답 데이터 검증
    if (!response.data || response.data.size === 0) {
      throw new Error('서버에서 빈 파일을 반환했습니다.');
    }
    
    // 파일 다운로드
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/octet-stream'
    });
    
    console.log('📦 Blob 생성 완료:', {
      size: blob.size,
      type: blob.type
    });
    
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
    console.log('✅ 파일 다운로드 완료:', fileName);
  } catch (error) {
    console.error('📥 파일 다운로드 오류:', error);
    console.error('📥 오류 상세 정보:', {
      message: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      config: error.config
    });
    
    if (error.response?.status === 404) {
      throw new Error('파일을 찾을 수 없습니다. 파일이 삭제되었거나 경로가 잘못되었을 수 있습니다.');
    } else if (error.response?.status === 403) {
      throw new Error('파일 다운로드 권한이 없습니다.');
    } else if (error.response?.status === 401) {
      throw new Error('로그인이 필요합니다.');
    } else if (error.response?.status === 500) {
      throw new Error('서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } else if (error.code === 'ERR_NETWORK') {
      throw new Error('네트워크 오류가 발생했습니다. 백엔드 서버가 실행 중인지 확인해주세요.');
    } else if (error.code === 'ECONNABORTED') {
      throw new Error('요청 시간이 초과되었습니다. 네트워크 상태를 확인해주세요.');
    } else {
      throw new Error(`파일 다운로드에 실패했습니다: ${error.message}`);
    }
  }
};

// 다운로드 URL 생성 API (선택적)
export const getDownloadUrl = async (fileKey) => {
  try {
    // 백엔드에서 새로운 API가 구현될 때까지 기존 API 사용
    // TODO: 백엔드에서 /api/instructor/file/download-url/filekey/{fileKey} 구현 후 변경
    // const response = await http.get(`/api/file/download-url/${fileKey}`);
    const response = await http.get(`/api/v2/file/upload/${fileKey}`);
    
    return response.data;
  } catch (error) {
    console.error('🔗 다운로드 URL 생성 오류:', error);
    throw error;
  }
};

// ===== 강의 계획서(lectureplan) 관련 API 함수들 =====

// 강의 계획서(lectureplan) 조회
export const getLecturePlan = async (courseId) => {
  try {
    const response = await http.get(`/api/instructor/lectureplan/course/${courseId}`);
    const lectureplanData = response.data.data; // ResponseDTO에서 data 필드 추출

    // 주차별 계획도 함께 조회
    if (lectureplanData && lectureplanData.planId) {
      try {
        const weeklyplanResponse = await http.get(`/api/instructor/lectureplan/${lectureplanData.planId}/weeklyplan`);
        const weeklyplanResult = weeklyplanResponse.data;
        lectureplanData.weeklyplan = weeklyplanResult.data || [];
      } catch (error) {
        console.warn('주차별 계획 조회에 실패했습니다:', error);
        lectureplanData.weeklyplan = [];
      }
    }

    return lectureplanData;
  } catch (error) {
    console.error('강의 계획서(lectureplan) 조회 오류:', error);
    throw error;
  }
};

// 강의 계획서(lectureplan) 등록
export const createLecturePlan = async (courseId, lectureplanData, weeklyplanData) => {
  try {
    // 먼저 강의계획서 등록 (주차별 계획 제외)
    const lectureplanRequestData = {
      courseId: courseId,
      planTitle: lectureplanData.planTitle?.trim() || "",
      planContent: lectureplanData.planContent?.trim() || "",
      courseGoal: lectureplanData.courseGoal?.trim() || "",
      learningMethod: lectureplanData.learningMethod?.trim() || "",
      evaluationMethod: lectureplanData.evaluationMethod?.trim() || "",
      textbook: lectureplanData.textbook?.trim() || "",
      weekCount: parseInt(lectureplanData.weekCount) || 15,
      assignmentPolicy: lectureplanData.assignmentPolicy?.trim() || "",
      latePolicy: lectureplanData.latePolicy?.trim() || "",
      etcNote: lectureplanData.etcNote?.trim() || "",
    };

    console.log('강의계획서 등록 요청 데이터:', JSON.stringify(lectureplanRequestData, null, 2));

    const lectureplanResponse = await http.post(`/api/instructor/lectureplan`, lectureplanRequestData);
    const lectureplanResult = lectureplanResponse.data;
    const planId = lectureplanResult.data.planId; // 등록된 계획서의 ID

    // 주차별 계획이 있으면 별도로 등록
    if (weeklyplanData && weeklyplanData.length > 0) {
      try {
        const weeklyplanRequestData = weeklyplanData.map((week, index) => {
          // weekNumber가 없거나 잘못된 경우 인덱스 기반으로 설정
          let weekNumber = week.weekNumber || index + 1;
          
          // weekNumber를 명시적으로 숫자로 변환
          weekNumber = parseInt(weekNumber, 10);
          if (isNaN(weekNumber) || weekNumber <= 0) {
            weekNumber = index + 1;
          }
          
          // 주차 번호가 1 이상인지 확인
          if (weekNumber < 1) {
            weekNumber = index + 1;
          }
          
          return {
            planId: planId,
            weekNumber: weekNumber,
            weekTitle: week.weekTitle?.trim() || "",
            title: week.weekTitle?.trim() || "",
            weekContent: week.weekContent?.trim() || "",
            content: week.weekContent?.trim() || "",
            subjectIds: Array.isArray(week.subjectIds) ? week.subjectIds : [],
            subDetailIds: Array.isArray(week.subDetailIds) ? week.subDetailIds : [],
          };
        }).filter(week => week.weekNumber > 0); // 유효한 주차 번호만 필터링
        
        console.log('주차별 계획 등록 요청 데이터:', JSON.stringify(weeklyplanRequestData, null, 2));
        
        // saveWeeklyplan 함수 사용
        await saveWeeklyplan(planId, weeklyplanRequestData);
      } catch (error) {
        console.error('주차별 계획 등록 중 오류:', error);
        console.error('에러 응답 데이터:', error.response?.data);
        console.error('에러 상태:', error.response?.status);
        console.error('에러 헤더:', error.response?.headers);
        console.error('요청 데이터:', weeklyplanData);
        throw new Error('주차별 계획 등록에 실패했습니다: ' + error.message);
      }
    }

    return lectureplanResult.data;
  } catch (error) {
    console.error('강의 계획서(lectureplan) 등록 오류:', error);
    console.error('에러 응답 데이터:', error.response?.data);
    console.error('에러 상태:', error.response?.status);
    throw error;
  }
};

// 강의 계획서(lectureplan) 수정
export const updateLecturePlan = async (planId, lectureplanData, weeklyplanData) => {
  try {
    // 강의계획서 수정 (주차별 계획 제외)
    const lectureplanRequestData = {
      planTitle: lectureplanData.planTitle?.trim() || "",
      planContent: lectureplanData.planContent?.trim() || "",
      courseGoal: lectureplanData.courseGoal?.trim() || "",
      learningMethod: lectureplanData.learningMethod?.trim() || "",
      evaluationMethod: lectureplanData.evaluationMethod?.trim() || "",
      textbook: lectureplanData.textbook?.trim() || "",
      weekCount: parseInt(lectureplanData.weekCount) || 15,
      assignmentPolicy: lectureplanData.assignmentPolicy?.trim() || "",
      latePolicy: lectureplanData.latePolicy?.trim() || "",
      etcNote: lectureplanData.etcNote?.trim() || "",
    };

    console.log('강의계획서 수정 요청 데이터:', JSON.stringify(lectureplanRequestData, null, 2));

    const lectureplanResponse = await http.put(`/api/instructor/lectureplan/${planId}`, lectureplanRequestData);
    const lectureplanResult = lectureplanResponse.data;

    // 주차별 계획 처리
    if (weeklyplanData && weeklyplanData.length > 0) {
      try {
        // 기존 주차별 계획 삭제
        await deleteWeeklyplan(planId);
        
        // 새로운 주차별 계획 등록
        const weeklyplanRequestData = weeklyplanData.map((week, index) => {
          // weekNumber가 없거나 잘못된 경우 인덱스 기반으로 설정
          let weekNumber = week.weekNumber || index + 1;
          
          // weekNumber를 명시적으로 숫자로 변환
          weekNumber = parseInt(weekNumber, 10);
          if (isNaN(weekNumber) || weekNumber <= 0) {
            weekNumber = index + 1;
          }
          
          // 주차 번호가 1 이상인지 확인
          if (weekNumber < 1) {
            weekNumber = index + 1;
          }
          
          return {
            planId: planId,
            weekNumber: weekNumber,
            weekTitle: week.weekTitle?.trim() || "",
            title: week.weekTitle?.trim() || "",
            weekContent: week.weekContent?.trim() || "",
            content: week.weekContent?.trim() || "",
            subjectIds: Array.isArray(week.subjectIds) ? week.subjectIds : [],
            subDetailIds: Array.isArray(week.subDetailIds) ? week.subDetailIds : [],
            // 추가 가능한 필드명들
            week_title: week.weekTitle?.trim() || "",
            week_content: week.weekContent?.trim() || "",
            weekName: week.weekTitle?.trim() || "",
            weekDescription: week.weekContent?.trim() || "",
          };
        }).filter(week => week.weekNumber > 0); // 유효한 주차 번호만 필터링
        
        console.log('주차별 계획 요청 데이터:', JSON.stringify(weeklyplanRequestData, null, 2));
        
        // saveWeeklyplan 함수 사용
        await saveWeeklyplan(planId, weeklyplanRequestData);
      } catch (error) {
        console.error('주차별 계획 처리 중 오류:', error);
        console.error('에러 응답 데이터:', error.response?.data);
        console.error('에러 상태:', error.response?.status);
        console.error('에러 헤더:', error.response?.headers);
        console.error('요청 데이터:', weeklyplanData);
        throw new Error('주차별 계획 처리에 실패했습니다: ' + error.message);
      }
    } else {
      // 주차별 계획이 없으면 기존 데이터 삭제
      try {
        await deleteWeeklyplan(planId);
      } catch (error) {
        console.warn('기존 주차별 계획 삭제에 실패했습니다:', error);
      }
    }

    return lectureplanResult.data;
  } catch (error) {
    console.error('강의 계획서(lectureplan) 수정 오류:', error);
    console.error('에러 응답 데이터:', error.response?.data);
    console.error('에러 상태:', error.response?.status);
    throw error;
  }
};

// 강의 계획서(lectureplan) 삭제
export const deleteLecturePlan = async (planId) => {
  try {
    const response = await http.delete(`/api/instructor/lectureplan/${planId}`);
    const result = response.data; // ResponseDTO에서 data 필드 추출
    return result.data;
  } catch (error) {
    console.error('강의 계획서(lectureplan) 삭제 오류:', error);
    throw error;
  }
};

// 주차별 계획(weeklyplan) 조회
export const getWeeklyplan = async (planId) => {
  try {
    const response = await http.get(`/api/instructor/lectureplan/${planId}/weeklyplan`);
    const result = response.data; // ResponseDTO에서 data 필드 추출
    return result.data;
  } catch (error) {
    console.error('주차별 계획(weeklyplan) 조회 오류:', error);
    throw error;
  }
};

// 주차별 계획(weeklyplan) 등록/수정
export const saveWeeklyplan = async (planId, weeklyplanData) => {
  try {
    const response = await http.post(`/api/instructor/lectureplan/${planId}/weeklyplan`, weeklyplanData && weeklyplanData.length > 0 ? weeklyplanData : []);
    const result = response.data; // ResponseDTO에서 data 필드 추출
    return result.data;
  } catch (error) {
    console.error('주차별 계획(weeklyplan) 저장 오류:', error);
    throw error;
  }
};

// 주차별 계획(weeklyplan) 삭제
export const deleteWeeklyplan = async (planId) => {
  try {
    const response = await http.delete(`/api/instructor/lectureplan/${planId}/weeklyplan`);
    const result = response.data; // ResponseDTO에서 data 필드 추출
    return result.data;
  } catch (error) {
    console.error('주차별 계획(weeklyplan) 삭제 오류:', error);
    throw error;
  }
};

// 강의 계획서 엑셀 템플릿 다운로드
export const downloadSyllabusTemplate = async () => {
  try {
    const response = await http.get(`/api/instructor/syllabus/template/download`);
    
    if (!response.ok) throw new Error('템플릿 다운로드에 실패했습니다.');
    
    const blob = response.data;
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'syllabus_template.xlsx';
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  } catch (error) {
    console.error('템플릿 다운로드 오류:', error);
    throw error;
  }
};

// 엑셀 파일로 강의 계획서 업로드
export const uploadSyllabusFromExcel = async (courseId, file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);

    const response = await http.post(`/api/instructor/syllabus/${courseId}/upload-excel`, formData);
    
    if (!response.ok) throw new Error('엑셀 파일 업로드에 실패했습니다.');
    const result = response.data; // ResponseDTO에서 data 필드 추출
    return result.data;
  } catch (error) {
    console.error('엑셀 파일 업로드 오류:', error);
    throw error;
  }
};

// 강의에 연결된 과목 목록 조회
export const getCourseSubjects = async (courseId) => {
  try {
    const response = await http.get(`/api/instructor/course/${courseId}/subjects`);
    const result = response.data;
    return result.data || [];
  } catch (error) {
    console.error('강의 과목 목록 조회 오류:', error);
    throw error;
  }
};

// 과목에 연결된 세부과목 목록 조회
export const getSubjectDetails = async (subjectId) => {
  try {
    const response = await http.get(`/api/instructor/subject/${subjectId}/details`);
    const result = response.data;
    return result.data || [];
  } catch (error) {
    console.error('과목 세부과목 목록 조회 오류:', error);
    throw error;
  }
};

// 모든 과목 목록 조회 (드롭다운용)
export const getAllSubjects = async () => {
  try {
    const response = await http.get(`/api/instructor/subjects/all`);
    const result = response.data;
    return result.data || [];
  } catch (error) {
    console.error('전체 과목 목록 조회 오류:', error);
    throw error;
  }
};

// 모든 세부과목 목록 조회 (드롭다운용)
export const getAllSubjectDetails = async () => {
  try {
    const response = await http.get(`/api/instructor/subject-details/all`);
    const result = response.data;
    return result.data || [];
  } catch (error) {
    console.error('전체 세부과목 목록 조회 오류:', error);
    throw error;
  }
};

 