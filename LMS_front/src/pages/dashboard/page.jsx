import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import StatsCard from "@/components/ui/stats-card"
import PageLayout from "@/components/ui/page-layout"
import { getAllCourse } from "@/api/suhyeon/courseApi"
import { http, getCookie } from "@/components/auth/http"
import { jwtDecode } from "jwt-decode"
import { getAttendanceStatus } from "@/api/hancw/instructorAcademicAxios"
import { getStudentAttendanceRecords } from "@/api/hancw/studentCourseAxios"
import { getStudentCoursesByMemberId } from "@/api/sunghyun/studentCourseApi"
import { getStudentAssignments } from "@/api/sunghyun/studentAssignmentApi"
import { getMyAssignmentSubmissions } from "@/api/sunghyun/studentAssignmentApi"
import { getStudentCourseList } from "@/api/suhyeon/evaluationApi"
import { getAllClassrooms } from "@/api/hancw/classroomAxios"
import { getMemberList } from "@/api/hancw/staffAcademicAxios"
import { getCourseEvaluation } from "@/api/suhyeon/evaluationApi"
import { getInstructorLectures } from "@/api/sunghyun/instructorCourseApi"
import { getMyExamTemplates } from "@/api/kayoung/templateApi"
import { getInstructorEvaluationList } from "@/api/suhyeon/evaluationApi"
import { getStaffCourses } from "@/api/kayoung/examCoursesApi"
import { getStudentExams } from "@/api/kayoung/studentExamApi"
// import Popup from "@/components/popup/popup"

export default function Dashboard() {
  const [currentUser, setCurrentUser] = useState(null)
  const [educationId, setEducationId] = useState(null)
  const [todayCourses, setTodayCourses] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingUserInfo, setIsLoadingUserInfo] = useState(false)
  const [userInfo, setUserInfo] = useState(null)
  const [attendanceRate, setAttendanceRate] = useState("0%")
  const [isLoadingAttendance, setIsLoadingAttendance] = useState(false)
  const [studentAttendanceRate, setStudentAttendanceRate] = useState("0%")
  const [isLoadingStudentAttendance, setIsLoadingStudentAttendance] = useState(false)
  const [studentCoursesCount, setStudentCoursesCount] = useState(0)
  const [isLoadingStudentCourses, setIsLoadingStudentCourses] = useState(false)
  const [studentAssignmentsCount, setStudentAssignmentsCount] = useState(0)
  const [isLoadingStudentAssignments, setIsLoadingStudentAssignments] = useState(false)
  const [registeredInstitutionsCount, setRegisteredInstitutionsCount] = useState(0)
  const [isLoadingInstitutions, setIsLoadingInstitutions] = useState(false)
  const [usedClassroomsCount, setUsedClassroomsCount] = useState(0)
  const [isLoadingClassrooms, setIsLoadingClassrooms] = useState(false)
  const [registeredStudentsCount, setRegisteredStudentsCount] = useState(0)
  const [isLoadingStudents, setIsLoadingStudents] = useState(false)
  const [totalInstructorsCount, setTotalInstructorsCount] = useState(0)
  const [isLoadingInstructors, setIsLoadingInstructors] = useState(false)
  const [incompleteSurveysCount, setIncompleteSurveysCount] = useState(0)
  const [isLoadingSurveys, setIsLoadingSurveys] = useState(false)
  const [activeSurveysCount, setActiveSurveysCount] = useState(0)
  const [isLoadingActiveSurveys, setIsLoadingActiveSurveys] = useState(false)
  const [instructorLecturesCount, setInstructorLecturesCount] = useState(0)
  const [isLoadingInstructorLectures, setIsLoadingInstructorLectures] = useState(false)
  const [instructorExamsCount, setInstructorExamsCount] = useState(0)
  const [isLoadingInstructorExams, setIsLoadingInstructorExams] = useState(false)
  const [instructorActiveSurveysCount, setInstructorActiveSurveysCount] = useState(0)
  const [isLoadingInstructorActiveSurveys, setIsLoadingInstructorActiveSurveys] = useState(false)
  const [instructorActiveExamsCount, setInstructorActiveExamsCount] = useState(0)
  const [isLoadingInstructorActiveExams, setIsLoadingInstructorActiveExams] = useState(false)
  const [staffActiveExamsCount, setStaffActiveExamsCount] = useState(0)
  const [isLoadingStaffActiveExams, setIsLoadingStaffActiveExams] = useState(false)
  const [studentPendingAssignmentsCount, setStudentPendingAssignmentsCount] = useState(0)
  const [isLoadingStudentPendingAssignments, setIsLoadingStudentPendingAssignments] = useState(false)
  const [studentIncompleteSurveysCount, setStudentIncompleteSurveysCount] = useState(0)
  const [isLoadingStudentIncompleteSurveys, setIsLoadingStudentIncompleteSurveys] = useState(false)
  const [studentAvailableExamsCount, setStudentAvailableExamsCount] = useState(0)
  const [isLoadingStudentAvailableExams, setIsLoadingStudentAvailableExams] = useState(false)
  const [studentRecentGrade, setStudentRecentGrade] = useState("시험 응시전")
  const [isLoadingStudentGrade, setIsLoadingStudentGrade] = useState(false)
  const [grantedPmIds, setGrantedPmIds] = useState([])
  const [isPermissionsLoaded, setIsPermissionsLoaded] = useState(false)
  const navigate = useNavigate()

  // 오늘 날짜를 YYYY-MM-DD 형식으로 가져오기
  const getTodayString = () => {
    const today = new Date()
    return today.toISOString().slice(0, 10)
  }

  // === 강사 출석률 조회 ===
  const fetchAttendanceRate = async () => {
    if (!currentUser || currentUser.role !== "ROLE_INSTRUCTOR") return
    
    setIsLoadingAttendance(true)
    try {
      const response = await getAttendanceStatus(currentUser.memberId || currentUser.educationId)
      console.log("출석률 조회 결과:", response)
      
      // 출석률 계산 (예시: 전체 학생 중 출석한 학생 비율)
      if (response.data && response.data.length > 0) {
        const totalStudents = response.data.reduce((sum, course) => sum + (course.totalStudents || 0), 0)
        const presentStudents = response.data.reduce((sum, course) => sum + (course.presentStudents || 0), 0)
        
        if (totalStudents > 0) {
          const rate = Math.round((presentStudents / totalStudents) * 100)
          setAttendanceRate(`${rate}%`)
        } else {
          setAttendanceRate("0%")
        }
      } else {
        setAttendanceRate("0%")
      }
    } catch (error) {
      console.error("출석률 조회 실패:", error)
      setAttendanceRate("0%")
    } finally {
      setIsLoadingAttendance(false)
    }
  }

  // === 학생 출석률 조회 ===
  const fetchStudentAttendanceRate = async () => {
    if (!currentUser || currentUser.role !== "ROLE_STUDENT") return
    
    setIsLoadingStudentAttendance(true)
    try {
      const response = await getStudentAttendanceRecords(currentUser.memberId || currentUser.educationId)
      console.log("학생 출석 기록 조회 결과:", response)
      
      if (response.data && response.data.length > 0) {
        // 이번 주 시작일(월요일)과 종료일(일요일) 계산
        const today = new Date()
        const dayOfWeek = today.getDay() // 0: 일요일, 1: 월요일, ..., 6: 토요일
        const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek // 월요일까지의 오프셋
        
        const monday = new Date(today)
        monday.setDate(today.getDate() + mondayOffset)
        monday.setHours(0, 0, 0, 0)
        
        const sunday = new Date(monday)
        sunday.setDate(monday.getDate() + 6)
        sunday.setHours(23, 59, 59, 999)
        
        // 이번 주 출석 기록만 필터링
        const thisWeekRecords = response.data.filter(record => {
          const recordDate = new Date(record.lectureDate)
          return recordDate >= monday && recordDate <= sunday
        })
        
        // checkIn 기준으로 출석률 계산
        const totalRecords = thisWeekRecords.length
        const attendedRecords = thisWeekRecords.filter(record => record.checkIn).length
        
        if (totalRecords > 0) {
          const rate = Math.round((attendedRecords / totalRecords) * 100)
          setStudentAttendanceRate(`${rate}%`)
        } else {
          setStudentAttendanceRate("0%")
        }
      } else {
        setStudentAttendanceRate("0%")
      }
    } catch (error) {
      console.error("학생 출석률 조회 실패:", error)
      setStudentAttendanceRate("0%")
    } finally {
      setIsLoadingStudentAttendance(false)
    }
  }

  // 학생 수강중인 과정 개수 조회
  const fetchStudentCoursesCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_STUDENT") return
    setIsLoadingStudentCourses(true)
    try {
      const data = await getStudentCoursesByMemberId()
      const coursesData = Array.isArray(data) ? data : (data.data || [])
      const today = new Date().toISOString().slice(0, 10)
      // 오늘이 courseStartDay ~ courseEndDay 사이인 과정만
      const filtered = coursesData.filter(c => {
        return c.courseStartDay && c.courseEndDay && today >= c.courseStartDay && today <= c.courseEndDay
      })
      const uniqueCourseIds = new Set(filtered.map(c => c.courseId))
      setStudentCoursesCount(uniqueCourseIds.size)
    } catch (error) {
      setStudentCoursesCount(0)
    } finally {
      setIsLoadingStudentCourses(false)
    }
  }

  // 학생 미제출 과제 개수 조회
  const fetchStudentAssignmentsCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_STUDENT") return
    setIsLoadingStudentAssignments(true)
    try {
      const data = await getStudentAssignments()
      const assignments = Array.isArray(data) ? data : (data.data || [])
      // 미제출 과제만 필터링 (submitted === false 또는 status === '미제출')
      const notSubmitted = assignments.filter(a => a.submitted === false || a.status === '미제출')
      setStudentAssignmentsCount(notSubmitted.length)
    } catch (error) {
      setStudentAssignmentsCount(0)
    } finally {
      setIsLoadingStudentAssignments(false)
    }
  }

  // 등록된 학원 수 조회
  const fetchRegisteredInstitutionsCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_ADMIN") return
    setIsLoadingInstitutions(true)
    try {
      const response = await http.get("/api/education/list", {
        withCredentials: true
      })
      setRegisteredInstitutionsCount(response.data.length)
    } catch (error) {
      console.error("등록된 학원 수 조회 실패:", error)
      setRegisteredInstitutionsCount(0)
    } finally {
      setIsLoadingInstitutions(false)
    }
  }

  // 사용가능한 강의  강의실 수 조회
  const fetchUsedClassroomsCount = async () => {
    if (!currentUser || (currentUser.role !== "ROLE_DIRECTOR" && currentUser.role !== "ROLE_STAFF")) return
    setIsLoadingClassrooms(true)
    try {
      const data = await getAllClassrooms()
      const classrooms = Array.isArray(data) ? data : (data.data || [])
      // classroomList.jsx와 동일한 로직으로 사용가능한 강의실 필터링
      // classRent === 0 (사용 가능) && classActive === 0 (사용 가능)
      const availableClassrooms = classrooms.filter(classroom => 
        classroom.classRent === 0 && classroom.classActive === 0
      )
      setUsedClassroomsCount(availableClassrooms.length)
    } catch (error) {
      console.error("사용가능한 강의실 수 조회 실패:", error)
      setUsedClassroomsCount(0)
    } finally {
      setIsLoadingClassrooms(false)
    }
  }

  // 등록된 학생 수 조회
  const fetchRegisteredStudentsCount = async () => {
    if (!currentUser || (currentUser.role !== "ROLE_DIRECTOR" && currentUser.role !== "ROLE_STAFF") || !educationId) return
    setIsLoadingStudents(true)
    try {
      const data = await getMemberList(educationId)
      // academicMemberList.jsx와 동일한 로직으로 데이터 처리
      const membersData = Array.isArray(data) ? data : (data?.members ?? [])
      // 학생만 필터링
      const students = membersData.filter(member => member.memberRole === 'ROLE_STUDENT')
      // 중복 제거 (userId 기준)
      const uniqueStudents = students.filter((student, index, self) => 
        index === self.findIndex(s => s.userId === student.userId)
      )
      setRegisteredStudentsCount(uniqueStudents.length)
    } catch (error) {
      console.error("등록된 학생 수 조회 실패:", error)
      setRegisteredStudentsCount(0)
    } finally {
      setIsLoadingStudents(false)
    }
  }

  // 전체 강사 수 조회
  const fetchTotalInstructorsCount = async () => {
    if (!currentUser || (currentUser.role !== "ROLE_DIRECTOR" && currentUser.role !== "ROLE_STAFF") || !educationId) return
    setIsLoadingInstructors(true)
    try {
      const data = await getMemberList(educationId)
      // academicMemberList.jsx와 동일한 로직으로 데이터 처리
      const membersData = Array.isArray(data) ? data : (data?.members ?? [])
      // 강사만 필터링
      const instructors = membersData.filter(member => member.memberRole === 'ROLE_INSTRUCTOR')
      // 중복 제거 (userId 기준)
      const uniqueInstructors = instructors.filter((instructor, index, self) => 
        index === self.findIndex(i => i.userId === instructor.userId)
      )
      setTotalInstructorsCount(uniqueInstructors.length)
    } catch (error) {
      console.error("전체 강사 수 조회 실패:", error)
      setTotalInstructorsCount(0)
    } finally {
      setIsLoadingInstructors(false)
    }
  }

  // 미완료 설문 수 조회
  const fetchIncompleteSurveysCount = async () => {
    if (!currentUser || (currentUser.role !== "ROLE_DIRECTOR" && currentUser.role !== "ROLE_STAFF")) return
    setIsLoadingSurveys(true)
    try {
      const data = await getCourseEvaluation()
      const lectures = Array.isArray(data) ? data : []
      
      // courseEvaluation.jsx와 동일한 로직으로 미완료 설문 계산
      // 완료된 설문: answerList가 있는 과정
      const completedSurveys = lectures.filter(lecture => 
        lecture.answerList && lecture.answerList.length > 0
      ).length
      
      // 미완료 설문: 전체 과정 - 완료된 설문
      const incompleteSurveys = lectures.length - completedSurveys
      setIncompleteSurveysCount(incompleteSurveys)
    } catch (error) {
      console.error("미완료 설문 수 조회 실패:", error)
      setIncompleteSurveysCount(0)
    } finally {
      setIsLoadingSurveys(false)
    }
  }

  // 평가 진행중인 과정 수 조회
  const fetchActiveSurveysCount = async () => {
    if (!currentUser || (currentUser.role !== "ROLE_DIRECTOR" && currentUser.role !== "ROLE_STAFF")) return
    setIsLoadingActiveSurveys(true)
    try {
      const data = await getCourseEvaluation()
      const lectures = Array.isArray(data) ? data : []
      
      // courseEvaluation.jsx와 동일한 로직으로 평가 진행중인 과정 계산
      const today = getTodayString()
      const activeSurveys = lectures.filter(lecture => 
        lecture.courseDate && today >= lecture.courseDate && today < lecture.courseEndDay
      ).length
      
      setActiveSurveysCount(activeSurveys)
    } catch (error) {
      console.error("평가 진행중인 과정 수 조회 실패:", error)
      setActiveSurveysCount(0)
    } finally {
      setIsLoadingActiveSurveys(false)
    }
  }

  // 강사 진행중인 과정 수 조회
  const fetchInstructorLecturesCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_INSTRUCTOR") return
    setIsLoadingInstructorLectures(true)
    try {
      const data = await getInstructorLectures()
      const lectures = Array.isArray(data) ? data : []
      
      // page.jsx의 getLectureStatus 함수와 동일한 로직으로 진행중인 과정 계산
      const now = new Date()
      const ongoingLectures = lectures.filter(lecture => {
        const startDate = lecture.courseStartDay ? new Date(lecture.courseStartDay) : null
        const endDate = lecture.courseEndDay ? new Date(lecture.courseEndDay) : null
        
        if (!startDate) return '예정'
        
        if (now < startDate) {
          return false // 예정
        } else if (now >= startDate && now <= endDate) {
          return true // 진행중
        } else {
          return false // 완료
        }
      }).length
      
      setInstructorLecturesCount(ongoingLectures)
    } catch (error) {
      console.error("강사 진행중인 과정 수 조회 실패:", error)
      setInstructorLecturesCount(0)
    } finally {
      setIsLoadingInstructorLectures(false)
    }
  }

  // 강사 예정 + 진행중 시험 수 조회
  const fetchInstructorExamsCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_INSTRUCTOR") return
    setIsLoadingInstructorExams(true)
    try {
      const response = await getMyExamTemplates()
      console.log('받아온 시험 목록:', response)
      
      // myExamsPage.jsx와 동일한 데이터 처리 방식
      const examsData = response?.data || response || []
      console.log('변환된 시험 데이터:', examsData)
      
      // myExamsPage.jsx와 동일한 로직으로 예정 + 진행중 시험 계산
      const activeExams = examsData.filter(exam => {
        // templateOpen과 templateClose가 모두 null인 경우
        if (exam.templateOpen === null && exam.templateClose === null) {
          return true // 예정
        }
        
        // templateOpen만 있고 templateClose가 null인 경우 (시험 열린 상태)
        if (exam.templateOpen && exam.templateClose === null) {
          return true // 진행중
        }
        
        // 시험 기간이 설정된 경우
        if (exam.templateOpen && exam.templateClose) {
          const today = new Date()
          const openDate = new Date(exam.templateOpen)
          const closeDate = new Date(exam.templateClose)
          
          // 오늘 날짜가 오픈 날짜보다 이후이고 닫은 날짜보다 이전인 경우
          if (today >= openDate && today <= closeDate) {
            return true // 진행중
          }
          // 오늘 날짜가 오픈 날짜보다 이전인 경우
          else if (today < openDate) {
            return true // 예정
          }
        }
        
        return false
      }).length
      
      console.log('예정 + 진행중 시험 수:', activeExams)
      setInstructorExamsCount(activeExams)
    } catch (error) {
      console.error("강사 예정 + 진행중 시험 수 조회 실패:", error)
      setInstructorExamsCount(0)
    } finally {
      setIsLoadingInstructorExams(false)
    }
  }

  // 강사 진행중인 설문 수 조회
  const fetchInstructorActiveSurveysCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_INSTRUCTOR") return
    setIsLoadingInstructorActiveSurveys(true)
    try {
      const response = await getInstructorEvaluationList()
      console.log('받아온 과정 평가 목록:', response)
      
      // evaluationCourseList.jsx와 동일한 데이터 처리 방식
      const lecturesData = response || []
      console.log('변환된 과정 데이터:', lecturesData)
      
      // evaluationCourseList.jsx와 동일한 로직으로 진행중인 설문 계산
      const today = getTodayString()
      const activeSurveys = lecturesData.filter(lecture => {
        return lecture.courseDate && today >= lecture.courseDate && today < lecture.courseEndDay
      }).length
      
      console.log('진행중인 설문 수:', activeSurveys)
      setInstructorActiveSurveysCount(activeSurveys)
    } catch (error) {
      console.error("강사 진행중인 설문 수 조회 실패:", error)
      setInstructorActiveSurveysCount(0)
    } finally {
      setIsLoadingInstructorActiveSurveys(false)
    }
  }

  // 강사 진행중인 시험 수 조회
  const fetchInstructorActiveExamsCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_INSTRUCTOR") return
    setIsLoadingInstructorActiveExams(true)
    try {
      const response = await getMyExamTemplates()
      console.log('받아온 시험 목록:', response)
      
      // myExamsPage.jsx와 동일한 데이터 처리 방식
      const examsData = response?.data || response || []
      console.log('변환된 시험 데이터:', examsData)
      
      // myExamsPage.jsx와 동일한 로직으로 진행중인 시험만 계산
      const activeExams = examsData.filter(exam => {
        // templateOpen만 있고 templateClose가 null인 경우 (시험 열린 상태)
        if (exam.templateOpen && exam.templateClose === null) {
          return true // 진행중
        }
        
        // 시험 기간이 설정된 경우
        if (exam.templateOpen && exam.templateClose) {
          const today = new Date()
          const openDate = new Date(exam.templateOpen)
          const closeDate = new Date(exam.templateClose)
          
          // 오늘 날짜가 오픈 날짜보다 이후이고 닫은 날짜보다 이전인 경우
          if (today >= openDate && today <= closeDate) {
            return true // 진행중
          }
        }
        
        return false
      }).length
      
      console.log('진행중인 시험 수:', activeExams)
      setInstructorActiveExamsCount(activeExams)
    } catch (error) {
      console.error("강사 진행중인 시험 수 조회 실패:", error)
      setInstructorActiveExamsCount(0)
    } finally {
      setIsLoadingInstructorActiveExams(false)
    }
  }

  // 직원 진행중인 시험 수 조회
  const fetchStaffActiveExamsCount = async () => {
    if (!currentUser || (currentUser.role !== "ROLE_DIRECTOR" && currentUser.role !== "ROLE_STAFF")) return
    setIsLoadingStaffActiveExams(true)
    try {
      const response = await getStaffCourses()
      console.log('받아온 직원용 과정 데이터:', response)
      
      if (response.success && response.data) {
        // examCoursesPage.jsx와 동일한 데이터 처리 방식
        const coursesData = response.data
        console.log('변환된 과정 데이터:', coursesData)
        
        // examCoursesPage.jsx와 동일한 로직으로 진행중인 시험 계산
        // examCount > 0인 과정을 진행중으로 간주 (examCoursesPage.jsx와 동일한 로직)
        const activeExams = coursesData.filter(course => {
          return (course.examCount || 0) > 0
        }).length
        
        console.log('진행중인 시험 수:', activeExams)
        setStaffActiveExamsCount(activeExams)
      } else {
        console.warn('직원용 과정 데이터 로드 실패:', response.message)
        setStaffActiveExamsCount(0)
      }
    } catch (error) {
      console.error("직원 진행중인 시험 수 조회 실패:", error)
      setStaffActiveExamsCount(0)
    } finally {
      setIsLoadingStaffActiveExams(false)
    }
  }

  // 학생 미제출 과제 수 조회
  const fetchStudentPendingAssignmentsCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_STUDENT") return
    setIsLoadingStudentPendingAssignments(true)
    try {
      // student/assignments/page.jsx와 동일한 API 호출
      const assignmentsData = await getStudentAssignments()
      const submissionsData = await getMyAssignmentSubmissions()
      
      console.log('받아온 과제 데이터:', assignmentsData)
      console.log('받아온 제출 데이터:', submissionsData)
      
      // student/assignments/page.jsx와 동일한 데이터 처리 방식
      const safeSubmissions = Array.isArray(submissionsData) ? submissionsData : []
      
      // student/assignments/page.jsx와 동일한 로직으로 미제출 과제 계산
      const assignmentsWithStatus = assignmentsData.map(assignment => {
        // 다양한 필드명으로 mySubmission 찾기
        const mySubmission = safeSubmissions.find(sub => {
          const assignmentMatch = sub.assignmentId === assignment.assignmentId;
          const memberMatch = sub.id === currentUser?.memberId || 
                             sub.memberId === currentUser?.memberId ||
                             sub.studentId === currentUser?.memberId;
          
          return assignmentMatch && memberMatch;
        });
        
        const now = new Date();
        // dueDate에 시간이 포함되어 있으면 그대로 사용, 없으면 23:59:59로 설정
        let due = new Date(assignment.dueDate);
        if (assignment.dueDate && assignment.dueDate.length === 10) {
          // 날짜만 있는 경우 (YYYY-MM-DD), 시간을 23:59:59로 설정
          due = new Date(assignment.dueDate + 'T23:59:59');
        }
        let status = "";
        
        if (mySubmission) {
          // 제출 완료된 경우, 제출 시점이 기한을 넘었는지 확인
          const submittedAt = new Date(mySubmission.submittedAt || mySubmission.submittedDate || mySubmission.createdAt);
          if (submittedAt > due) {
            status = "submitted_overdue"; // 제출 완료이지만 기한 초과
          } else {
            status = "submitted"; // 제출 완료
          }
        } else {
          // 미제출인 경우, 현재가 기한을 넘었는지 확인
          if (now > due) {
            status = "overdue"; // 기한 초과 (하지만 제출 가능)
          } else {
            status = "pending"; // 제출 대기
          }
        }
        
        return { ...assignment, mySubmission, status };
      });
      
      // pending 상태인 과제만 카운트 (제출대기 과제)
      const pendingAssignments = assignmentsWithStatus.filter(a => a.status === "pending").length;
      
      console.log('제출대기 과제 수:', pendingAssignments)
      setStudentPendingAssignmentsCount(pendingAssignments)
    } catch (error) {
      console.error("학생 제출대기 과제 수 조회 실패:", error)
      setStudentPendingAssignmentsCount(0)
    } finally {
      setIsLoadingStudentPendingAssignments(false)
    }
  }

  // 학생 미완료 설문 수 조회
  const fetchStudentIncompleteSurveysCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_STUDENT") return
    setIsLoadingStudentIncompleteSurveys(true)
    try {
      // student/evaluation/evaluationResponse.jsx와 동일한 API 호출
      const response = await getStudentCourseList()
      
      console.log('받아온 학생 수강 과정 데이터:', response)
      
      if (!response || !Array.isArray(response)) {
        console.warn('학생 수강 과정 데이터가 비어있거나 배열이 아닙니다.')
        setStudentIncompleteSurveysCount(0)
        return
      }
      
      // student/evaluation/evaluationResponse.jsx와 동일한 데이터 처리 방식
      const formattedCourses = response.map((course) => ({
        id: course.courseId || "UNKNOWN",
        code: course.courseCode || course.courseId || "UNKNOWN",
        name: course.courseName || course.questionTemplateName || "과정명 없음",
        instructor: course.memberName || "강사명 없음",
        period: course.courseStartDay && course.courseEndDay 
          ? `${course.courseStartDay} ~ ${course.courseEndDay}` 
          : course.courseDate || "날짜 정보 없음",
        time: course.startTime && course.endTime 
          ? `${course.startTime} ~ ${course.endTime}`
          : "시간 정보 없음",
        surveyActive: course.response !== null && course.courseDate !== null,
        surveyStartDate: course.courseDate || "",
        surveyEndDate: course.courseEndDay || "",
        status: course.courseDate === null 
          ? "평가기간이 아님" 
          : course.response === null 
            ? "응답 가능" 
            : course.response 
              ? "응답 완료" 
              : "응답 가능",
        category: "설문 평가",
        schedule: course.courseDays || "설문 평가",
        templateGroupId: course.templateGroupId || "",
        studentCount: course.studentCount || 0,
        maxCapacity: course.maxCapacity || 0,
      }))
      
      // student/evaluation/evaluationResponse.jsx와 동일한 로직으로 미완료 설문 계산
      // "응답 가능" 상태인 설문만 카운트 (미완료 설문)
      const incompleteSurveys = formattedCourses.filter(course => course.status === "응답 가능").length;
      
      console.log('미완료 설문 수:', incompleteSurveys)
      setStudentIncompleteSurveysCount(incompleteSurveys)
    } catch (error) {
      console.error("학생 미완료 설문 수 조회 실패:", error)
      setStudentIncompleteSurveysCount(0)
    } finally {
      setIsLoadingStudentIncompleteSurveys(false)
    }
  }

  // 학생 응시가능한 시험 수 조회
  const fetchStudentAvailableExamsCount = async () => {
    if (!currentUser || currentUser.role !== "ROLE_STUDENT") return
    setIsLoadingStudentAvailableExams(true)
    try {
      const response = await getStudentExams()
      console.log('받아온 학생 시험 목록:', response)
      
      // studentExamsPage.jsx와 동일한 데이터 처리 방식
      const examsData = response?.data || response || []
      console.log('변환된 학생 시험 데이터:', examsData)
      
      // studentExamsPage.jsx의 getExamStatus 함수와 동일한 로직 적용
      const getExamStatus = (exam) => {
        const now = new Date();
        const openTime = exam.templateOpen ? new Date(exam.templateOpen) : null;
        const closeTime = exam.templateClose ? new Date(exam.templateClose) : null;
        
        // 이미 제출한 시험은 "completed" 상태
        if (exam.submitted) {
          return "completed";
        }
        
        // 이미 완료된 시험
        if (exam.status === "completed") {
          if (!exam.graded) {
            return "completed"; // 채점 대기
          }
          return "completed"; // 채점 완료
        }
        
        // 응시 시작 시간이 없는 경우
        if (!openTime) {
          return exam.status || "unavailable";
        }
        
        // 응시 종료 시간이 없는 경우 (무제한)
        if (!closeTime) {
          // 현재 시간이 응시 시작 시간보다 이전
          if (now < openTime) {
            return "waiting"; // 시험 대기
          }
          // 응시 시작 시간 이후면 응시 가능
          return "available"; // 응시 가능
        }
        
        // 응시 시작 시간과 종료 시간이 모두 있는 경우
        // 현재 시간이 응시 시작 시간보다 이전
        if (now < openTime) {
          return "waiting"; // 시험 대기
        }
        
        // 현재 시간이 응시 종료 시간보다 이후
        if (now > closeTime) {
          return "expired"; // 응시 기간 종료
        }
        
        // 응시 기간 내
        if (now >= openTime && now <= closeTime) {
          return "available"; // 응시 가능
        }
        
        return exam.status || "unavailable";
      }
      
      // 응시 가능한 시험만 카운트
      const availableExams = examsData.filter(exam => getExamStatus(exam) === "available").length
      
      console.log('학생 응시가능한 시험 수:', availableExams)
      setStudentAvailableExamsCount(availableExams)
    } catch (error) {
      console.error("학생 응시가능한 시험 수 조회 실패:", error)
      setStudentAvailableExamsCount(0)
    } finally {
      setIsLoadingStudentAvailableExams(false)
    }
  }

  // 학생 최근 성적 조회
  const fetchStudentRecentGrade = async () => {
    if (!currentUser || currentUser.role !== "ROLE_STUDENT") return
    setIsLoadingStudentGrade(true)
    try {
      const response = await getStudentExams()
      console.log('학생 성적 조회 결과:', response)
      
      if (response && response.data && response.data.length > 0) {
        // 제출된 시험 중 점수가 있는 시험만 필터링
        const submittedExams = response.data.filter(exam => 
          exam.submitted && exam.myScore && exam.myScore > 0
        )
        
        if (submittedExams.length > 0) {
          // 전체 평균 점수 계산
          const totalScore = submittedExams.reduce((sum, exam) => sum + exam.myScore, 0)
          const averageScore = totalScore / submittedExams.length
          setStudentRecentGrade(`${averageScore.toFixed(1)}점`)
        } else {
          setStudentRecentGrade("시험 응시전")
        }
      } else {
        setStudentRecentGrade("시험 응시전")
      }
    } catch (error) {
      console.error("학생 최근 성적 조회 실패:", error)
      setStudentRecentGrade("시험 응시전")
    } finally {
      setIsLoadingStudentGrade(false)
    }
  }

  // === 사용자 정보 조회 ===
  const fetchUserInfo = async () => {
    if (!currentUser) return
    setIsLoadingUserInfo(true)
    try {
      const { data } = await http.post(
        "/api/mypage/get-my-info",
        {
          name: currentUser.name,
          email: currentUser.email,
          educationId: currentUser.educationId
        },
        { withCredentials: true }
      )

      setUserInfo({
        name: data.name || "이름 없음",
        email: data.email || "이메일 없음",
        phone: data.phone || "전화번호 없음",
        educationName: data.educationName || "학원명 없음",
        role: data.role || "권한 없음"
      })
    } catch (error) {
      console.error("사용자 정보 조회 실패:", error)
      setUserInfo(null)
    } finally {
      setIsLoadingUserInfo(false)
    }
  }

  // === 토큰에서 사용자 정보 추출 ===
  useEffect(() => {
    const token = getCookie("refresh")
    if (token) {
      try {
        const decoded = jwtDecode(token)
        console.log("JWT 토큰 디코딩 결과:", decoded) // 디버깅용
        
        // name 필드가 없을 경우 다른 필드명들도 시도
        const userName = decoded.name || decoded.memberName || decoded.userName || decoded.username || "사용자"
        
        setCurrentUser({
          name: userName,
          email: decoded.email || decoded.memberEmail || "",
          educationId: decoded.educationId,
          memberId: decoded.memberId,
          role: decoded.role
        })
        setEducationId(decoded.educationId)
        
        console.log("설정된 currentUser:", {
          name: userName,
          email: decoded.email || decoded.memberEmail || "",
          educationId: decoded.educationId,
          memberId: decoded.memberId,
          role: decoded.role
        }) // 디버깅용
      } catch (error) {
        console.error("JWT 토큰 디코딩 실패:", error)
        // JWT 디코딩 실패 시 localStorage에서 가져오기
        const userInfo = localStorage.getItem('currentUser')
        if (userInfo) {
          setCurrentUser(JSON.parse(userInfo))
        }
      }
    } else {
      // 토큰이 없으면 localStorage에서 가져오기
      const userInfo = localStorage.getItem('currentUser')
      if (userInfo) {
        setCurrentUser(JSON.parse(userInfo))
      }
    }
  }, [])

  // === 권한 조회 (header.jsx와 동일한 로직) ===
  useEffect(() => {
    if (currentUser?.role !== "ROLE_STAFF") {
      setIsPermissionsLoaded(true);
      return;
    }
  
    http.get("/api/staff/my-permissions", { withCredentials: true })
      .then(res => {
        const { memberId, mainPermissions, subPermissions } = res.data.data;
        const all = [...mainPermissions, ...subPermissions];
        const grantedIds = all.filter(p => p.isGranted).map(p => Number(p.pmId));
        setGrantedPmIds(grantedIds);
        console.log("권한 조회 성공:", grantedIds);
        console.log("원본 권한 데이터:", all);
      })
              .catch(err => {
            console.error("Dashboard 권한 조회 실패", err);
            // 권한 조회 실패 시 권한 없음으로 설정 (카드 숨김)
            setGrantedPmIds([]);
        })
      .finally(() => setIsPermissionsLoaded(true));
  }, [currentUser?.role]);

    // 오늘의 과정 수 계산
  const fetchTodayCourses = async () => {
    try {
      setIsLoading(true)
      const courses = await getAllCourse()
      const today = getTodayString()
      
      // 오늘이 courseStartDay와 courseEndDay 사이에 있는 과정 개수 계산
      const todayCoursesCount = courses.filter(course => {
        const startDate = course.courseStartDay
        const endDate = course.courseEndDay
        return startDate && endDate && today >= startDate && today <= endDate
      }).length
      
      setTodayCourses(todayCoursesCount)
    } catch (error) {
      console.error("과정 목록 조회 실패:", error)
      setTodayCourses(0)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchTodayCourses()
  }, [])

  // currentUser가 설정되면 사용자 정보 조회 및 출석률 조회
  useEffect(() => {
    if (currentUser) {
      console.log("현재 사용자 역할:", currentUser.role)
      fetchUserInfo()
      
      if (currentUser.role === "ROLE_INSTRUCTOR") {
        console.log("강사 권한으로 데이터 로드 시작")
        // 각 함수를 개별적으로 호출하여 에러가 발생해도 다른 함수들이 실행되도록 함
        Promise.allSettled([
          fetchAttendanceRate(),
          fetchInstructorLecturesCount(),
          fetchInstructorExamsCount(),
          fetchInstructorActiveSurveysCount(),
          fetchInstructorActiveExamsCount()
        ]).then(results => {
          results.forEach((result, index) => {
            if (result.status === 'rejected') {
              console.error(`강사 데이터 로드 실패 (${index}):`, result.reason)
            }
          })
        })
      }
      if (currentUser.role === "ROLE_STUDENT") {
        fetchStudentAttendanceRate()
        fetchStudentCoursesCount()
        fetchStudentAssignmentsCount()
        fetchStudentPendingAssignmentsCount()
        fetchStudentIncompleteSurveysCount()
        fetchStudentAvailableExamsCount()
        fetchStudentRecentGrade()
      }
      if (currentUser.role === "ROLE_ADMIN") {
        fetchRegisteredInstitutionsCount()
      }
      if (currentUser.role === "ROLE_DIRECTOR" || currentUser.role === "ROLE_STAFF") {
        fetchUsedClassroomsCount()
        fetchIncompleteSurveysCount()
        fetchActiveSurveysCount()
        fetchStaffActiveExamsCount()
      }
    }
  }, [currentUser])

  // educationId가 설정되면 학생 수 조회
  useEffect(() => {
    if (educationId && (currentUser?.role === "ROLE_DIRECTOR" || currentUser?.role === "ROLE_STAFF")) {
      fetchRegisteredStudentsCount()
      fetchTotalInstructorsCount()
    }
  }, [educationId, currentUser])

  const getRoleText = (role) => {
    switch (role) {
      case "ROLE_ADMIN": return "관리자권한"
      case "ROLE_DIRECTOR": return "학원장권한"
      case "ROLE_STAFF": return "일반직원권한"
      case "ROLE_INSTRUCTOR": return "강사권한"
      case "ROLE_STUDENT": return "학생권한"
      default: return "권한"
    }
  }

  // 권한별 통계 데이터 설정 (header.jsx와 유사한 구조로 개선)
  const getStatsData = (userRole) => {
    const baseStats = {
      ROLE_ADMIN: [
        {
          title: "등록된 학원",
          value: isLoadingInstitutions ? "로딩 중..." : `${registeredInstitutionsCount}개의`,
          unit: "학원",
          onClick: () => navigate("/institution/invite"),
          key: "institution"
        },
      ],
      ROLE_DIRECTOR: [
        {
          title: "진행중인 과정",
          value: isLoading ? "로딩 중..." : `${todayCourses}개의`,
          unit: "과정",
          onClick: () => navigate("/courses/course"),
          key: "courses"
        },
        {
          title: "등록된 학생",
          value: isLoadingStudents ? "로딩 중..." : `${registeredStudentsCount}명의`,
          unit: "학생",
          onClick: () => navigate("/academic/students?tab=학생"),
          key: "academic"
        },
        {
          title: "전체 강사",
          value: isLoadingInstructors ? "로딩 중..." : `${totalInstructorsCount}명의`,
          unit: "강사",
          onClick: () => navigate("/academic/students?tab=강사"),
          key: "instructors"
        },
        {
          title: "사용가능한 강의실",
          value: isLoadingClassrooms ? "로딩 중..." : `${usedClassroomsCount}개의`,
          unit: "강의실",
          onClick: () => navigate("/classroom/room"),
          key: "classroom"
        },
      ],
      ROLE_STAFF: [
        {
          title: "진행중인 과정",
          value: isLoading ? "로딩 중..." : `${todayCourses}개의`,
          unit: "과정",
          onClick: () => navigate("/courses/course"),
          key: "courses"
        },
        {
          title: "등록된 학생",
          value: isLoadingStudents ? "로딩 중..." : `${registeredStudentsCount}명의`,
          unit: "학생",
          onClick: () => navigate("/academic/students"),
          key: "academic"
        },
        {
          title: "전체 강사",
          value: isLoadingInstructors ? "로딩 중..." : `${totalInstructorsCount}명의`,
          unit: "강사",
          onClick: () => navigate("/academic/students?tab=강사"),
          key: "academic"
        },
        {
          title: "사용가능한 강의실",
          value: isLoadingClassrooms ? "로딩 중..." : `${usedClassroomsCount}개의`,
          unit: "강의실",
          onClick: () => navigate("/classroom/room"),
          key: "classroom"
        },
        {
          title: "평가 진행중인 과정",
          value: isLoadingActiveSurveys ? "로딩 중..." : `${activeSurveysCount}개의`,
          unit: "과정",
          onClick: () => navigate("/evaluations"),
          key: "evaluations"
        },
        {
          title: "진행중인 시험",
          value: isLoadingStaffActiveExams ? "로딩 중..." : `${staffActiveExamsCount}개의`,
          unit: "시험",
          onClick: () => navigate("/exam/courses"),
          key: "exam"
        },
      ],
      ROLE_INSTRUCTOR: [
        {
          title: "내 과정",
          value: isLoadingInstructorLectures ? "로딩 중..." : `${instructorLecturesCount}개의`,
          unit: "과정",
          onClick: () => navigate("/instructor/courses/lectures"),
          key: "courses"
        },
        {
          title: "예정/진행중 시험",
          value: isLoadingInstructorExams ? "로딩 중..." : `${instructorExamsCount}개의`,
          unit: "시험",
          onClick: () => navigate("/instructor/exam/my-exams"),
          key: "exam"
        },
        {
          title: "진행중인 설문",
          value: isLoadingInstructorActiveSurveys ? "로딩 중..." : `${instructorActiveSurveysCount}개의`,
          unit: "설문",
          onClick: () => navigate("/instructor/evaluation"),
          key: "evaluation"
        },
      ],
      ROLE_STUDENT: [
        {
          title: "수강중인 과정",
          value: isLoadingStudentCourses ? "로딩 중..." : `${studentCoursesCount}개의`,
          unit: "과정",
          onClick: () => navigate("/student/syllabus"),
          key: "syllabus"
        },
        {
          title: "미제출 과제",
          value: isLoadingStudentPendingAssignments ? "로딩 중..." : `${studentPendingAssignmentsCount}개의`,
          unit: "과제",
          onClick: () => navigate("/student/assignments"),
          key: "assignments"
        },
        {
          title: "응시가능한 시험",
          value: isLoadingStudentAvailableExams ? "로딩 중..." : `${studentAvailableExamsCount}개의`,
          unit: "시험",
          onClick: () => navigate("/student/exams"),
          key: "exams"
        },
        {
          title: "미완료 설문",
          value: isLoadingStudentIncompleteSurveys ? "로딩 중..." : `${studentIncompleteSurveysCount}개의`,
          unit: "설문",
          onClick: () => navigate("/student/evaluation"),
          key: "evaluation"
        },
        {
          title: "최근 성적",
          value: isLoadingStudentGrade ? "로딩 중..." : studentRecentGrade,
          unit: "",
          onClick: () => navigate("/student/grades"),
          key: "grades"
        },
      ],
    }

    return baseStats[userRole] || baseStats.ROLE_STUDENT
  }

  // 권한별 환영 메시지 설정 (header.jsx와 유사한 구조로 개선)
  const getWelcomeMessage = (userRole, userName) => {
    const messages = {
      ROLE_ADMIN: {
        title: `${userName}님, 환영합니다!`,
        subtitle: `${userName}님, 전체 시스템을 관리하고 모니터링하세요.`,
        description: "시스템 전체 현황을 한눈에 확인하고 관리할 수 있습니다."
      },
      ROLE_DIRECTOR: {
        title: `${userName}님, 환영합니다!`,
        subtitle: `${userName}님, 학원의 전반적인 현황을 한눈에 확인하세요.`,
        description: "학원 운영에 필요한 모든 정보를 체계적으로 관리할 수 있습니다."
      },
      ROLE_STAFF: {
        title: `${userName}님, 환영합니다!`,
        subtitle: `${userName}님, 학원 운영을 위한 업무를 효율적으로 관리하세요.`,
        description: "학생 관리, 과정 관리, 시험 관리 등 업무를 효율적으로 처리할 수 있습니다."
      },
      ROLE_INSTRUCTOR: {
        title: `${userName}님, 환영합니다!`,
        subtitle: `${userName}님, 과정와 학생 관리를 체계적으로 진행하세요.`,
        description: "과정 진행, 학생 관리, 시험 출제 등 강사 업무를 체계적으로 관리할 수 있습니다."
      },
      ROLE_STUDENT: {
        title: `${userName}님, 환영합니다!`,
        subtitle: `${userName}님, 학습에 집중하고 성장하는 시간을 보내세요.`,
        description: "수강 과정, 과제, 시험, 성적 등 학습 관련 정보를 한눈에 확인할 수 있습니다."
      }
    }

    // 권한이 있으면 해당 권한의 메시지 반환, 없으면 기본 메시지 반환
    if (messages[userRole]) {
      return messages[userRole]
    } else {
      // 알 수 없는 권한인 경우 기본 메시지
      return {
        title: `${userName}님, 환영합니다!`,
        subtitle: `${userName}님, LMSync에 오신 것을 환영합니다.`,
        description: "LMSync 시스템을 통해 효율적인 학습과 관리를 경험하세요."
      }
    }
  }

  // 권한별 대시보드 레이아웃 설정
  const getDashboardLayout = (userRole) => {
    const layouts = {
      ROLE_ADMIN: {
        gridCols: "md:grid-cols-1 lg:grid-cols-1",
        cardSpan: "col-span-1"
      },
      ROLE_DIRECTOR: {
        gridCols: "md:grid-cols-2 lg:grid-cols-3",
        cardSpan: "col-span-1"
      },
      ROLE_STAFF: {
        gridCols: "md:grid-cols-2 lg:grid-cols-3",
        cardSpan: "col-span-1"
      },
      ROLE_INSTRUCTOR: {
        gridCols: "md:grid-cols-2 lg:grid-cols-3",
        cardSpan: "col-span-1"
      },
      ROLE_STUDENT: {
        gridCols: "md:grid-cols-2 lg:grid-cols-3",
        cardSpan: "col-span-1"
      }
    }

    return layouts[userRole] || layouts.ROLE_STUDENT
  }

  // 권한별 통계 카드 필터링 (header.jsx와 유사한 권한 체크 로직)
  const filterStatsByPermissions = (stats, userRole) => {
    if (userRole !== "ROLE_STAFF") {
      return stats // STAFF가 아닌 경우 모든 통계 표시
    }

    // STAFF 권한 체크 (header.jsx와 동일한 로직)
    const permissionMap = { 
      account: 1, 
      academic: 2, 
      courses: 3, 
      classroom: 4, 
      evaluations: 5, 
      exam: 6 
    }

    return stats.map(stat => {
      const requiredPm = permissionMap[stat.key]
      if (!requiredPm) return stat // 권한이 필요하지 않은 항목은 그대로 표시
      
      // 권한이 있으면 onClick 유지, 없으면 onClick을 null로 설정
      const hasPermission = grantedPmIds.includes(requiredPm)
      return {
        ...stat,
        onClick: hasPermission ? stat.onClick : null
      }
    })
  }

  const actualUserRole = currentUser?.role || "ROLE_STUDENT"
  const actualUserName = currentUser?.name || "사용자"
  const allStatsData = getStatsData(actualUserRole)
  const filteredStatsData = filterStatsByPermissions(allStatsData, actualUserRole)
  const welcomeMessage = getWelcomeMessage(actualUserRole, userInfo?.name || actualUserName)
  const dashboardLayout = getDashboardLayout(actualUserRole)

  return (
    <PageLayout 
      currentPage="dashboard"
      userRole={actualUserRole}
      userName={actualUserName}
    >
      <main className="p-8">
        <div className="max-w-6xl mx-auto">
          {/* 권한별 환영 메시지 섹션 */}
          <div className="text-center mb-12">
            <h1 className="text-3xl font-bold mb-4" style={{ color: "#2C3E50" }}>
              {welcomeMessage.title}
            </h1>
            <p className="text-lg mb-2" style={{ color: "#95A5A6" }}>
              {welcomeMessage.subtitle}
            </p>
            <p className="text-sm" style={{ color: "#BDC3C7" }}>
              {welcomeMessage.description}
            </p>
          </div>

          {/* 권한별 통계 카드 섹션 */}
          <div className={`grid gap-6 ${dashboardLayout.gridCols}`}>
            {filteredStatsData.map((stat, index) => (
              <div key={stat.key || index} className={dashboardLayout.cardSpan}>
                <StatsCard 
                  title={stat.title} 
                  value={stat.value} 
                  unit={stat.unit} 
                  onClick={stat.onClick}
                />
              </div>
            ))}
          </div>
        </div>
      </main>
      {/* <Popup educationId={educationId} /> */}
    </PageLayout>
  )
}