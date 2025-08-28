// 사이드바 메뉴 설정을 중앙화하는 유틸리티

// 회원 정보 메뉴 설정
export const academicMenuItems = [
  { href: "/academic/students", label: "회원 목록", key: "academic-students" },
  { href: "/academic/register", label: "학생 출/결 처리", key: "academic-register" },
]

// 강의실 메뉴 설정
export const classroomMenuItems = [
  { href: "/classroom/room", label: "강의실 목록", key: "classroom-list" },
  { href: "/classroom/register", label: "강의실 등록", key: "classroom-register" },
]

// 강사 회원 정보 메뉴 설정
export const instructorAcademicMenuItems = [
  { href: "/instructor/academic/students", label: "학생 목록", key: "students" },
  { href: "/instructor/academic/attendance", label: "출석 현황", key: "attendance" }
]

export const studentMyCoursesMenuItems = [
  { href: "/student/my-courses", label: "출석 기록", key: "attendance" },
  { href: "/student/login", label: "출석/퇴실", key: "pc-attendance" }
]

// 학생 과제 메뉴 설정
export const studentAssignmentsMenuItems = [
  { href: "/student/assignments", label: "과제 목록", key: "assignments" }
]


// 설문 평가 메뉴 설정
export const evaluationMenuItems = [
  { href: "/evaluations", label: "평가 항목", key: "survey-items" },
  { href: "/evaluations/templates", label: "템플릿 목록", key: "survey-templates" },
  { href: "/evaluations/course", label: "과정 리스트", key: "survey-lectures" },
]

// 학생 설문 평가 메뉴 설정
export const studentEvaluationMenuItems = [
  { href: "/student/evaluation", label: "설문 평가", key: "evaluation" },
]


// 과정 관리 메뉴 설정
export const coursesMenuItems = [
  { href: "/courses/course", label: "과정 리스트", key: "course-list" },
  { href: "/courses/course/register", label: "과정 등록", key: "course-register" },
  { href: "/courses/subjects", label: "과목 리스트", key: "subject-list" },
  { href: "/courses/subjects/register", label: "과목 등록", key: "subject-register" },
  { href: "/courses/subjectdetail", label: "세부 과목 등록", key: "subject-detail" },
]

// 강사 과정 관리 메뉴 설정
export const instructorCoursesMenuItems = [
  { href: "/instructor/courses/lectures", label: "담당 강의", key: "lectures" },
  { href: "/instructor/courses/assignments", label: "과제 리스트", key: "assignments" }
]

// 과정 리스트 페이지 메뉴 설정
export const courseListMenuItems = [
  { href: "/courses/course", label: "과정 리스트", key: "course-list" },
  { href: "/courses/course/register", label: "과정 등록", key: "course-register" },
  { href: "/courses/subjects", label: "과목 리스트", key: "subject-list" },
  { href: "/courses/subjects/register", label: "과목 등록", key: "subject-register" },
  { href: "/courses/subjectdetail", label: "세부 과목 등록", key: "subject-detail" },
]

// 과정 등록 페이지 메뉴 설정
export const courseRegisterMenuItems = [
  { href: "/courses/course", label: "과정 리스트", key: "course-list" },
  { href: "/courses/course/register", label: "과정 등록", key: "course-register" },
  { href: "/courses/subjects", label: "과목 리스트", key: "subject-list" },
  { href: "/courses/subjects/register", label: "과목 등록", key: "subject-register" },
  { href: "/courses/subjectdetail", label: "세부 과목 등록", key: "subject-detail" },
]

// 과정 상세 페이지 메뉴 설정
export const courseDetailMenuItems = [
  { href: "/courses/course", label: "과정 리스트", key: "course-list" },
  { href: "/courses/course/register", label: "과정 등록", key: "course-register" },
  { href: "/courses/subjects", label: "과목 리스트", key: "subject-list" },
  { href: "/courses/subjects/register", label: "과목 등록", key: "subject-register" },
  { href: "/courses/subjectdetail", label: "세부 과목 등록", key: "subject-detail" },
]

// 과목 리스트 페이지 메뉴 설정
export const subjectListMenuItems = [
  { href: "/courses/course", label: "과정 리스트", key: "course-list" },
  { href: "/courses/course/register", label: "과정 등록", key: "course-register" },
  { href: "/courses/subjects", label: "과목 리스트", key: "subject-list" },
  { href: "/courses/subjects/register", label: "과목 등록", key: "subject-register" },
  { href: "/courses/subjectdetail", label: "세부 과목 등록", key: "subject-detail" },
]

// 과목 등록 페이지 메뉴 설정
export const subjectRegisterMenuItems = [
  { href: "/courses/course", label: "과정 리스트", key: "course-list" },
  { href: "/courses/course/register", label: "과정 등록", key: "course-register" },
  { href: "/courses/subjects", label: "과목 리스트", key: "subject-list" },
  { href: "/courses/subjects/register", label: "과목 등록", key: "subject-register" },
  { href: "/courses/subjectdetail", label: "세부 과목 등록", key: "subject-detail" },
]

// 과목 상세 페이지 메뉴 설정
export const subjectDetailMenuItems = [
  { href: "/courses/course", label: "과정 리스트", key: "course-list" },
  { href: "/courses/course/register", label: "과정 등록", key: "course-register" },
  { href: "/courses/subjects", label: "과목 리스트", key: "subject-list" },
  { href: "/courses/subjects/register", label: "과목 등록", key: "subject-register" },
  { href: "/courses/subjectdetail", label: "세부 과목 등록", key: "subject-detail" },
]

// 세부 과목 등록 페이지 메뉴 설정
export const subDetailListMenuItems = [
  { href: "/courses/course", label: "과정 리스트", key: "course-list" },
  { href: "/courses/course/register", label: "과정 등록", key: "course-register" },
  { href: "/courses/subjects", label: "과목 리스트", key: "subject-list" },
  { href: "/courses/subjects/register", label: "과목 등록", key: "subject-register" },
  { href: "/courses/subjectdetail", label: "세부 과목 등록", key: "subject-detail" },
]


// 시험 및 성적 관리 메뉴 설정
export const examMenuItems = [
  { href: "/exam/courses", label: "과정별 시험 리스트", key: "courses-list" },
  { href: "/exam/questions", label: "과목별 문제 리스트", key: "question-list" },
]

// 문제 은행 메뉴 설정
export const questionBankMenuItems = [
  { href: "/instructor/question-bank/all", label: "전체 문제 은행", key: "all-question-bank" },
  { href: "/instructor/question-bank/my", label: "내 문제 은행", key: "my-question-bank" },
]

// 강사 시험 관리 메뉴 설정
export const instructorExamMenuItems = [
  { href: "/instructor/exam/my-exams", label: "내 시험 관리", key: "my-exams" },
  { href: "/instructor/exam/lectures/history", label: "내 과정 성적 관리", key: "history" },
]

// 학생 시험 관리 메뉴 설정
export const studentExamMenuItems = [
  { href: "/student/exams", label: "시험 목록", key: "exam-list" },
  { href: "/student/grades", label: "성적 조회", key: "exam-results" },
]

// 학생 강의계획서 메뉴 설정
export const studentSyllabusMenuItems = [
  { href: "/student/syllabus", label: "강의계획서", key: "syllabus" }
]

// 계정 등록 메뉴 설정
export const accountMenuItems = [
  { href: "/account/individual", label: "개별 등록", key: "individual" },
  { href: "/account/bulk", label: "일괄 등록", key: "bulk" }
]

// 학원 관리 메뉴 설정
export const institutionMenuItems = [
  { href: "/institution/register", label: "학원 등록", key: "register" },
  { href: "/institution/invite", label: "초대장 관리", key: "invite" }
]

// 공지사항 메뉴 설정 (조회용)
export const noticeMenuItems = [
  { href: "/notice", label: "공지사항", key: "notice" }
]

// 학생용 공지사항 메뉴 설정 (조회 전용)
export const studentNoticeMenuItems = [
  { href: "/notice", label: "공지사항", key: "notice" }
]

// 공지사항 관리 메뉴 설정 (관리용)
export const noticeManageMenuItems = [
  { href: "/notice", label: "공지사항 관리", key: "notice-manage" },
  { href: "/notice/popup", label: "팝업 관리", key: "popup-manage" }
]


// 메뉴 설정을 가져오는 함수
export const getMenuItems = (section) => {
  switch (section) {
    case 'academic':
      return academicMenuItems
    case 'classroom':
      return classroomMenuItems
    case 'instructor-academic':
      return instructorAcademicMenuItems
    case 'student-my-courses':
      return studentMyCoursesMenuItems
    case 'evaluation':
      return evaluationMenuItems
    case 'student-evaluation':
      return studentEvaluationMenuItems
    case 'courses':
      return coursesMenuItems
    case 'instructor-courses':
      return instructorCoursesMenuItems
    case 'course-list':
      return courseListMenuItems
    case 'course-register':
      return courseRegisterMenuItems
    case 'course-detail':
      return courseDetailMenuItems
    case 'subject-list':
      return subjectListMenuItems
    case 'subject-register':
      return subjectRegisterMenuItems
    case 'subject-detail':
      return subjectDetailMenuItems
    case 'sub-detail-list':
      return subDetailListMenuItems
    case 'exam':
      return examMenuItems
    case 'question-bank':
      return questionBankMenuItems
    case 'instructor-exam':
      return instructorExamMenuItems
    case 'student-exam':
      return studentExamMenuItems
    case 'student-assignments':
      return studentAssignmentsMenuItems
    case 'student-syllabus':
      return studentSyllabusMenuItems
    case 'account':
      return accountMenuItems
    case 'institution':
      return institutionMenuItems
    case 'notice':
      return noticeMenuItems
    case 'student-notice':
      return studentNoticeMenuItems
    case 'notice-manage':
      return noticeManageMenuItems
    default:
      return []
  }
}

// 현재 경로에 따른 활성 메뉴 키를 찾는 함수
export const getActiveMenuKey = (pathname, menuItems) => {
  const menuItem = menuItems.find(item => pathname.startsWith(item.href))
  return menuItem ? menuItem.key : 'main'
}

// 동적으로 메뉴 라벨을 생성하는 함수
export const createDynamicMenuItems = (baseMenuItems, courseId, editMode, currentPath) => {
  return baseMenuItems.map(item => {
    // 과정 등록/수정 페이지에서만 과정 등록 메뉴 변경
    if (currentPath === "/courses/course/register" && item.href === "/courses/course/register") {
      return {
        ...item,
        label: courseId ? "과정 수정" : "과정 등록"
      };
    }
    // 과목 등록/수정 페이지에서만 과목 등록 메뉴 변경
    if (currentPath === "/courses/subjects/register" && item.href === "/courses/subjects/register") {
      return {
        ...item,
        label: editMode ? "과목 수정" : "과목 등록"
      };
    }
    // 다른 메뉴 아이템들은 원래대로 유지
    return item;
  });
};

// (필요하다면 getMenuItems에 추가하지 않고, 각 courses 페이지에서 직접 import해서 사용) 

