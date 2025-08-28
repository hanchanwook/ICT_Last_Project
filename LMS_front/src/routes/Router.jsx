import { createBrowserRouter, RouterProvider, Navigate, Outlet } from "react-router-dom"
import RootLayout from "../components/layout/root-layout"

// Pages
import LoginPage from "../pages/page"
import MobileLoginPage from "../pages/mobile/login/page"
import Dashboard from "../pages/dashboard/page"

// Account pages
// import AccountPage from "../pages/account/page"
import AccountRegisterPage from "../pages/account/register/page"
import AccountIndividualPage from "../pages/account/individual/page"
import AccountBulkPage from "../pages/account/bulk/page"

// Academic pages
import AcademicStudentsPage from "../pages/academic/academicMemberList"          // 학원 회원 리스트
import AcademicStudentDetailPage from "../pages/academic/academicMemberDetail"   // 학원 회원 상세
import AcademicRegisterPage from "../pages/academic/academicRegister"             // 학생 출/결 관리

// 직원 목록/과목/과정 페이지
import CoursesListPage from "../pages/courses/course/courseList" // 과정 리스트
import CourseDetailPage from "../pages/courses/course/courseDetail" // 과정 상세
import CoursesRegisterPage from "../pages/courses/course/courseRegister" // 과정 등록
import SubjectsListPage from "../pages/courses/subjects/subjectList" // 과목 리스트
import SubjectDetailPage from "../pages/courses/subjects/subjectDetail" // 과목 상세
import SubjectsRegisterPage from "../pages/courses/subjects/subjectRegister" // 과목 등록
import SubDetailListPage from "../pages/courses/subjectdetail/subDetailList" // 세부과목 리스트(등록,수정,삭제)


// Classroom pages
import ClassroomRoomPage from "../pages/classroom/classroomList"            // 강의실 리스트
import ClassroomRegisterPage from "../pages/classroom/classroomRegister"    // 강의실 등록

// 직원 강의평과 페이지
import EvaluationQuestion from "../pages/evaluation/evaluationQuestion" // 평가 항목
import CourseEvaluation from "../pages/evaluation/courseEvaluation" // 강의 리스트
import QuestionTemplate from "../pages/evaluation/questionTemplate" // 템플릿 목록

// Exam pages
// import ExamPage from "../pages/exam/page"  // 시험 조회 (리다이렉트)
import ExamCoursesPage from "../pages/exam/examCoursesPage"  // 시험 과정 조회
import ExamQuestionsPage from "../pages/exam/examQuestionsPage"  // 문제 조회

// Permission pages
import PermissionPage from "../pages/permission/page"

// Chat pages
import ChatPage from "../pages/chat/page"

// Institution pages
import InstitutionRegisterPage from "../pages/institution/register/page"
import InstitutionInvitePage from "../pages/institution/invite/page"
import InstitutionAuthPage from "../pages/institution/auth/page"

// Notice pages
import NoticePage from "../pages/notice/page"
import PopupPage from "../pages/notice/popup/page"

// Instructor pages
import InstructorCoursesPage from "../pages/instructor/courses/page"

import InstructorStudentListPage from "../pages/instructor/academic/instructorStudentList"          // 강사 학생 목록
import InstructorStudentDetailPage from "../pages/instructor/academic/instructorStudentDetail"    // 강사 학생 상세
import InstructorAcademicAttendancePage from "../pages/instructor/academic/instructorAttendance"  // 강사 학생 출/결 확인


import InstructorExamMyExamsPage from "../pages/instructor/exam/my-exams/myExamsPage"  // 내 시험 관리
import InstructorExamMyExamDetailPage from "../pages/instructor/exam/my-exams/detail/instructorMyExamDetailPage"  // 시험 상세
import InstructorExamMyExamCreatePage from "../pages/instructor/exam/my-exams/create/instructorMyExamCreatePage"  // 시험 등록
import InstructorExamLecturesHistoryPage from "../pages/instructor/exam/lectures/history/lecturesHistoryPage"  // 과정 성적 관리
import InstructorExamLectureDetailPage from "../pages/instructor/exam/lectures/history/examLectureDetailPage"  // 과정 성적 상세
import InstructorQuestionBankAllPage from "../pages/instructor/question-bank/all/allQuestion"  // 문제 은행 전체 조회
import InstructorExamQuestionBankPage from "../pages/instructor/question-bank/my/myQuestion"  // 문제 은행

// 강사 강의평과 페이지
import EvaluationCourseList from "../pages/instructor/evaluation/evaluationCourseList"

import InstructorCoursesLecturesPage from "../pages/instructor/courses/lectures/page"
import InstructorCoursesLectureDetailPage from "../pages/instructor/courses/lectures/[id]/page"
import InstructorCoursesLecturePlanCreatePage from "../pages/instructor/courses/lectures/[id]/syllabus/create/page"
import InstructorCoursesAssignmentsPage from "../pages/instructor/courses/assignments/page"
import InstructorCoursesAssignmentDetailPage from "../pages/instructor/courses/assignments/detail/page"

// Student pages
import StudentLoginPage from "../pages/student/my-courses/studentAttendancePC"
import StudentMyCoursesPage from "../pages/student/my-courses/studentAttendancePage"

import StudentMyCoursesHistoryPage from "../pages/student/my-courses/studentAttendancePage"
import StudentMobileAttendancePage from "../pages/student/my-courses/studentAttendanceMobile"
import StudentSyllabusPage from "../pages/student/syllabus/page"
import StudentAssignmentsPage from "../pages/student/assignments/page"

import StudentExamsPage from "../pages/student/exams/studentExamsPage"  // 시험 조회
import StudentGradesPage from "../pages/student/grades/studentGradePage"  // 성적 조회

// 학생 강의평과 페이지
import EvaluationResponse from "../pages/student/evaluation/evaluationResponse" // 강의평가하기

const router = createBrowserRouter([
  {
    path: "/",
    element: <RootLayout />,
    children: [
      {
        index: true,
        element: <LoginPage />,
      },
      {
        path: "login",
        element: <LoginPage />,
      },
      {
        path: "mobile/login",
        element: <MobileLoginPage />,
      },
      {
        path: "student/login",
        element: <StudentLoginPage />,
      },
      {
        path: "dashboard",
        element: <Dashboard />,
      },
      {
        path: "account",
        children: [
          {
            index: true,
            element: <AccountIndividualPage />,
          },
          {
            path: "register",
            element: <AccountRegisterPage />,
          },
          {
            path: "individual",
            element: <AccountIndividualPage />,
          },
          {
            path: "bulk",
            element: <AccountBulkPage />,
          },
        ],
      },
      {
        path: "academic",
        children: [
          {
            index: true,
            element: <Navigate to="/academic/students" />,
          },
          {
            path: "students",
            children: [
              {
                index: true,
                element: <AcademicStudentsPage />,
              },
              {
                path: ":id",
                element: <AcademicStudentDetailPage />,
              },
            ],
          },
          {
            path: "register",
            element: <AcademicRegisterPage />,
          },
        ],
      },
      {
        path: "courses",
        children: [
          {
            index: true,
            element: <CoursesListPage />,
          },
          {
            path: "course",
            children: [
              {
                index: true,
                element: <CoursesListPage />,
              },
              {
                path: ":id",
                element: <CourseDetailPage />,
              },
              {
                path: "register",
                element: <CoursesRegisterPage />,
              },
            ],
          },          
          {
            path: "subjects",
            children: [
              {
                index: true,
                element: <SubjectsListPage />,
              },
              {
                path: ":id",
                element: <SubjectDetailPage />,
              },
              {
                path: "register",
                element: <SubjectsRegisterPage />,
              },
            ],
          },
          {
            path: "subjectdetail",
            children: [
              {
                index: true,
                element: <SubDetailListPage />,
              },
            ],
          },
        ],
      }, 
      {
        path: "classroom",
        children: [
          {
            index: true,
            element: <Navigate to="/classroom/room" />,
          },
          {
            path: "room",
            element: <ClassroomRoomPage />,
          },
          {
            path: "register",
            element: <ClassroomRegisterPage />,
          },
        ],
      },
      {
        path: "evaluations",
        children: [
          {
            index: true,
            element: <EvaluationQuestion />,
          },
          {
            path: "course",
            element: <CourseEvaluation />,
          },
          {
            path: "templates",
            element: <QuestionTemplate />,
          },
        ],
      },
      {
        path: "exam",
        children: [
          // {
          //   index: true,
          //   element: <ExamPage />,
          // },
          {
            path: "courses",
            element: <ExamCoursesPage />,
          },
          {
            path: "questions",
            element: <ExamQuestionsPage />,
          },
        ],
      },
      {
        path: "permission",
        element: <PermissionPage />,
      },
      {
        path: "chat",
        element: <ChatPage />,
      },
      {
        path: "institution",
        children: [
          {
            path: "register",
            element: <InstitutionRegisterPage />,
          },
          {
            path: "invite",
            element: <InstitutionInvitePage />,
          },
        ],
      },
      {
        path: "invite-register",
        element: <InstitutionAuthPage />,
      },
      {
        path: "notice",
        children: [
          {
            index: true,
            element: <NoticePage />,
          },
          {
            path: "popup",
            element: <PopupPage />,
          },
        ],
      },
      {
        path: "instructor",
        children: [
          {
            path: "courses",
            children: [
              {
                index: true,
                element: <InstructorCoursesPage />,
              },
              {
                path: "lectures",
                children: [
                  {
                    index: true,
                    element: <InstructorCoursesLecturesPage />,
                  },
                  {
                    path: ":id",
                    element: <InstructorCoursesLectureDetailPage />,
                  },
                  {
                    path: ":id/lectureplan/create",
                    element: <InstructorCoursesLecturePlanCreatePage />,
                  },
                ],
              },
              {
                path: "assignments",
                children: [
                  {
                    index: true,
                    element: <InstructorCoursesAssignmentsPage />,
                  },
                  {
                    path: "detail/:id",
                    element: <InstructorCoursesAssignmentDetailPage />,
                  },
                ],
              },
            ],
          },
          {
            path: "academic",
            children: [
              {
                index: true,
                element: <InstructorStudentListPage />,
              },
              {
                path: "students",
                element: <InstructorStudentListPage />,
              },
              {
                path: "students/:id",
                element: <InstructorStudentDetailPage />,
              },
              {
                path: "attendance",
                element: <InstructorAcademicAttendancePage />,
              },

            ],
          },
          {
            path: "exam",
            children: [
              {
                path: "my-exams",
                children: [
                  {
                    index: true,
                    element: <InstructorExamMyExamsPage />,
                  },
                  {
                    path: "detail/:id",
                    element: <InstructorExamMyExamDetailPage />,
                  },
                  {
                    path: "create",
                    element: <InstructorExamMyExamCreatePage />,
                  },
                ],
              },

              {
                path: "lectures/history",
                children: [
                  {
                    index: true,
                    element: <InstructorExamLecturesHistoryPage />,
                  },
                  {
                    path: ":id",
                    element: <InstructorExamLectureDetailPage />,
                  },
                ],
              },
            ],
          },
          {
            path: "question-bank",
            children: [
              {
                path: "all",
                element: <InstructorQuestionBankAllPage />,
              },
              {
                path: "my",
                element: <InstructorExamQuestionBankPage />,
              },
            ],
          },
          {
            path: "evaluation",
            children: [
              {
                index: true,
                element: <EvaluationCourseList />,
              },
            ],
          },
        ],
      },
      {
        path: "student",
        children: [
          {
            path: "my-courses",
            children: [
              {
                index: true,
                element: <StudentMyCoursesHistoryPage />,
              },
              {
                path: "history",
                element: <StudentMyCoursesHistoryPage />,
              },
              {
                path: "mobileQR",
                element: <StudentMobileAttendancePage />,
              },
            ],
          },
          {
            path: "syllabus",
            element: <StudentSyllabusPage />,
          },
          {
            path: "assignments",
            element: <StudentAssignmentsPage />,
          },
          {
            path: "exams",
            element: <StudentExamsPage />,
          },
          {
            path: "grades",
            element: <StudentGradesPage />,
          },
          {
            path: "evaluation",
            element: <EvaluationResponse />,
          },
        ],
      },
    ],
  },
])

export default function AppRouter() {
  return <RouterProvider router={router} />
}
