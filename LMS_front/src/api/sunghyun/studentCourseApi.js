// 학생 강의 관련 API 함수들

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:19091';

// 학생이 수강하는 모든 강의 목록 조회 (member 테이블의 id로 courseId 조회 후 course 테이블에서 강의 정보 가져오기)
export const getStudentCoursesByMemberId = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/student/courses`, {
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
        throw new Error('수강 과목 목록을 불러오는데 실패했습니다.');
      }
    }
    return await response.json();
  } catch (error) {
    console.error('수강 과목 목록 조회 오류:', error);
    throw error;
  }
};



// 학생용 강의계획서 조회 (lectureplan 테이블 사용)
export const getStudentSyllabus = async (courseId) => {
  try {
    // 1. courseId로 planId 조회
    const planIdRes = await fetch(`${API_BASE_URL}/api/instructor/lectureplan/course/${courseId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    if (!planIdRes.ok) throw new Error('강의계획서를 불러오는데 실패했습니다.');
    const planIdData = await planIdRes.json();
    const planId = planIdData.data?.planId;
    if (!planId) throw new Error('강의계획서가 없습니다.');

    // 2. planId로 강의계획서 상세 조회
    const planDetailRes = await fetch(`${API_BASE_URL}/api/instructor/lectureplan/plan/${planId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    if (!planDetailRes.ok) throw new Error('강의계획서를 불러오는데 실패했습니다.');
    const planDetailData = await planDetailRes.json();
    const syllabusData = planDetailData.data;

    // 3. planId로 주차별 계획 조회
    const weeklyPlanRes = await fetch(`${API_BASE_URL}/api/instructor/lectureplan/${planId}/weeklyplan`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    if (weeklyPlanRes.ok) {
      const weeklyPlanData = await weeklyPlanRes.json();
      syllabusData.weeklyPlan = weeklyPlanData.data || [];
    } else {
      syllabusData.weeklyPlan = [];
    }

    return syllabusData;
  } catch (error) {
    console.error('강의계획서 조회 오류:', error);
    throw error;
  }
}; 