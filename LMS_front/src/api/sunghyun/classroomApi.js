import { http } from '@/components/auth/http'

// 교실 상세 정보 조회 (장비 정보 제외)
export const getClassroomDetail = async (classId) => {
  try {
    // 강사가 사용하는 것과 동일한 API 사용
    const response = await http.get(`/api/instructor/class/${classId}`)
    
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
    console.error('교실 상세 정보 조회 실패:', error)
    
    // 에러 발생 시 기본값 반환
    return {
      classId: classId,
      classCode: '미정',
      className: '강의실 미정'
    };
  }
} 