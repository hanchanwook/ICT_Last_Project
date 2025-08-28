import { useState, useEffect } from "react"
import { useParams, useNavigate } from "react-router-dom"
import {
  ArrowLeft,
  Mail,
  Phone,
  Calendar,
  User,
  MapPin,
  FileText,
  BookOpen,
  TrendingUp,
  Clock,
  Award,
  Edit,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getMenuItems } from "@/components/ui/menuConfig"
import { getStudentDetail, getStudentAttendanceHistory, getStudentGrades } from "@/api/hancw/instructorAcademicAxios"

export default function InstructorStudentDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  // 강사 전용 사이드바 메뉴
  const sidebarItems = getMenuItems('instructor-academic')

  // 학생 데이터 상태
  const [student, setStudent] = useState(null)
  const [studentList, setStudentList] = useState([]) // 학생의 여러 과정 목록
  const [selectedStudentIndex, setSelectedStudentIndex] = useState(0) // 선택된 과정 인덱스
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [currentUser, setCurrentUser] = useState(null)
  
  // 출석 이력 상태
  const [attendanceHistory, setAttendanceHistory] = useState([])
  const [attendanceLoading, setAttendanceLoading] = useState(false)
  const [attendanceError, setAttendanceError] = useState(null)
  
  // 성적 이력 상태
  const [gradesHistory, setGradesHistory] = useState([])
  const [gradesLoading, setGradesLoading] = useState(false)
  const [gradesError, setGradesError] = useState(null)

  // 현재 사용자 정보 로드
  useEffect(() => {
    const userData = localStorage.getItem("currentUser")
    if (userData) {
      const user = JSON.parse(userData)
      setCurrentUser(user)
      console.log('현재 사용자 정보:', user)
    }
  }, [])

  // 학생 데이터 조회
  const getStudentData = async () => {
    if (!id || !currentUser?.memberId) {
      console.log('학생 ID 또는 사용자 정보가 없습니다.')
      return
    }

    setLoading(true)
    setError(null)
    console.log("=== 강사 학생 상세 데이터 조회 시작 ===")
    console.log("조회할 학생 ID:", id)
    console.log("현재 사용자 ID:", currentUser.memberId)
    
    try {
      const data = await getStudentDetail(id, currentUser.memberId)
      console.log("API 응답 데이터:", data)
      
      // 백엔드에서 여러 과정 정보를 받아온 경우
      if (data?.courses?.length > 0) {
        
        // student = 기본 학생 정보 + 첫 번째 과정
        const firstCourse = data.courses[0]
        const mappedData = {
          id: data.userId || id,
          name: data.memberName || "이름 없음",
          userId: data.userId || "-",
          email: data.memberEmail || "-",
          phone: data.memberPhone || "-",
          course: firstCourse.courseName || "-",
          courseCode: firstCourse.courseCode || "-",
          enrollDate: firstCourse.courseStartDay || "-",
          status: "수강중", 
          attendance: firstCourse.attendanceRate || 0,
          averageScore: firstCourse.averageScore || 0,
          lastActivity: firstCourse.lastActivity || "-",
          progress: firstCourse.progress || 0,
          birthDate: data.memberBirthday || "--",
          address: data.memberAddress || "--",
          gpa: firstCourse.gpa || "-",
          totalCredits: firstCourse.totalCredits || "-",
          advisor: firstCourse.advisor || "-",
          recentGrades: firstCourse.recentGrades || [],
          attendanceHistory: firstCourse.attendanceHistory || [],
          assignmentHistory: firstCourse.assignmentHistory || [],
          // 추가 필드들
          courseStartDay: firstCourse.courseStartDay,
          courseEndDay: firstCourse.courseEndDay,
          courseDays: firstCourse.courseDays,
          educationId: firstCourse.educationId,
          courseId: firstCourse.courseId
        }
        
        // courses 배열에 status 필드 추가
        const coursesWithStatus = data.courses.map(course => ({
          ...course,
          status: course.status || "수강중"
        }))
        setStudentList(coursesWithStatus)
        setStudent(mappedData)
        setSelectedStudentIndex(0)
      } else {
        // 과정 정보가 없는 경우 (기존 방식)
        const mappedData = {
          id: data.userId || id,
          name: data.memberName || "이름 없음",
          userId: data.userId || "-",
          email: data.memberEmail || "-",
          phone: data.memberPhone || "-",
          course: data.courseName || "-",
          courseCode: data.courseCode || "-",
          enrollDate: data.courseStartDay || "-",
          status: "수강중", 
          attendance: data.attendanceRate || 0,
          averageScore: data.averageScore || 0,
          lastActivity: data.lastActivity || "-",
          progress: data.progress || 0,
          birthDate: data.memberBirthday || "--",
          address: data.memberAddress || "--",
          gpa: data.gpa || "-",
          totalCredits: data.totalCredits || "-",
          advisor: data.advisor || "-",
          recentGrades: data.recentGrades || [],
          attendanceHistory: data.attendanceHistory || [],
          assignmentHistory: data.assignmentHistory || [],
          // 추가 필드들
          courseStartDay: data.courseStartDay,
          courseEndDay: data.courseEndDay,
          courseDays: data.courseDays,
          educationId: data.educationId
        }
        setStudent(mappedData)
        setStudentList([])
        setSelectedStudentIndex(0)
      }
    } catch (err) {
      console.error("API 호출 실패:", err)
      setError("학생 정보를 불러오는데 실패했습니다.")
      console.error("학생 정보 조회 실패:", err)
    } finally {
      setLoading(false)
      console.log("=== 강사 학생 상세 데이터 조회 완료 ===")
    }
  }

  // 과정 선택 함수
  const handleStudentSelect = (index) => {
    setSelectedStudentIndex(index)
    const selectedCourse = studentList[index]
    
    // 선택된 과정의 정보로 student 업데이트 (학생 이름은 유지)
    const updatedStudent = {
      ...student,
      // name은 그대로 유지 (memberName이 동일하므로)
      course: selectedCourse.courseName || "-",
      courseCode: selectedCourse.courseCode || "-",
      enrollDate: selectedCourse.courseStartDay || "-",
      attendance: selectedCourse.attendanceRate || 0,
      averageScore: selectedCourse.averageScore || 0,
      lastActivity: selectedCourse.lastActivity || "-",
      progress: selectedCourse.progress || 0,
      gpa: selectedCourse.gpa || "-",
      totalCredits: selectedCourse.totalCredits || "-",
      advisor: selectedCourse.advisor || "-",
      recentGrades: selectedCourse.recentGrades || [],
      attendanceHistory: selectedCourse.attendanceHistory || [],
      assignmentHistory: selectedCourse.assignmentHistory || [],
      courseStartDay: selectedCourse.courseStartDay,
      courseEndDay: selectedCourse.courseEndDay,
      courseDays: selectedCourse.courseDays,
      educationId: selectedCourse.educationId,
      courseId: selectedCourse.courseId
    }
    
    setStudent(updatedStudent)
    
    // 현재 활성화된 탭이 출석 이력이면 새로운 과정의 출석 이력도 새로고침
    if (activeTab === "attendance") {
      getAttendanceHistoryData(selectedCourse.courseId)
    }
  }

  // 출석 이력 데이터 조회
  const getAttendanceHistoryData = async (courseId = null) => {
    if (!id || !currentUser?.memberId) {
      console.log('학생 ID 또는 사용자 정보가 없습니다.')
      return
    }

    setAttendanceLoading(true)
    setAttendanceError(null)
    console.log("=== 강사 학생 출석 이력 조회 시작 ===")
    console.log("조회할 학생 ID:", id)
    
    // courseId 파라미터가 있으면 사용, 없으면 현재 선택된 과정의 courseId 사용
    const currentCourseId = courseId || displayStudent.courseId
    console.log("현재 선택된 과정 ID:", currentCourseId)
    
    try {
      // courseId를 파라미터로 전달
      const data = await getStudentAttendanceHistory(id, { courseId: currentCourseId })
      console.log("출석 이력 API 응답 데이터:", data)
      // API 응답에서 attendanceHistory 배열만 추출
      const historyArray = data && data.attendanceHistory ? data.attendanceHistory : []
      console.log("추출된 출석 이력 배열:", historyArray)
      
      // 현재 선택된 과정의 courseId와 일치하는 데이터만 필터링
      const filteredHistory = historyArray.filter(record => 
        record.courseId === currentCourseId
      )
      console.log("필터링된 출석 이력 (courseId 일치):", filteredHistory)
      
      setAttendanceHistory(filteredHistory)
    } catch (err) {
      console.error("출석 이력 API 호출 실패:", err)
      setAttendanceError("출석 이력을 불러오는데 실패했습니다.")
    } finally {
      setAttendanceLoading(false)
      console.log("=== 강사 학생 출석 이력 조회 완료 ===")
    }
  }

  // 성적 이력 데이터 조회
  const getGradesHistoryData = async () => {
    if (!id || !currentUser?.memberId) {
      console.log('학생 ID 또는 사용자 정보가 없습니다.')
      return
    }

    setGradesLoading(true)
    setGradesError(null)
    console.log("=== 강사 학생 성적 이력 조회 시작 ===")
    console.log("조회할 학생 ID:", id)
    
    try {
      const data = await getStudentGrades(id)
      console.log("성적 이력 API 응답 데이터:", data)
      // API 응답에서 grades 배열 추출
      const gradesArray = Array.isArray(data) ? data : (data?.grades || [])
      console.log("추출된 성적 이력 배열:", gradesArray)
      setGradesHistory(gradesArray)
    } catch (err) {
      console.error("성적 이력 API 호출 실패:", err)
      setGradesError("성적 이력을 불러오는데 실패했습니다.")
    } finally {
      setGradesLoading(false)
      console.log("=== 강사 학생 성적 이력 조회 완료 ===")
    }
  }

  // 컴포넌트 마운트 시 및 사용자 정보 변경 시 데이터 조회
  useEffect(() => {
    if (currentUser?.memberId && id) {
      getStudentData()
    }
  }, [currentUser, id])

  // 기본 학생 데이터 (로딩 중이거나 에러 시 표시)
  const defaultStudent = {
    id: id,
    name: loading ? "로딩 중..." : error ? "학생 정보 없음" : "학생 정보 없음",
    userId: "-",
    email: "-",
    phone: "-",
    course: "-",
    courseCode: "-",
    enrollDate: "-",
    status: "정보없음",
    attendance: 0,
    averageScore: 0,
    lastActivity: "-",
    progress: 0,
    birthDate: "-",
    address: "-",
    gpa: "-",
    totalCredits: "-",
    advisor: "-",
    recentGrades: [],
    attendanceHistory: [],
    assignmentHistory: [],
  }

  const displayStudent = student || defaultStudent

  const [activeTab, setActiveTab] = useState("personal")

  const getStatusColor = (status) => {
    switch (status) {
      case "수강중":
        return "bg-blue-100 text-blue-800"
      case "완료":
        return "bg-green-100 text-green-800"
      case "휴학":
        return "bg-yellow-100 text-yellow-800"
      case "중도포기":
        return "bg-red-100 text-red-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const getAttendanceColor = (status) => {
    switch (status) {
      case "출석":
        return "bg-green-100 text-green-800"
      case "지각":
        return "bg-yellow-100 text-yellow-800"
      case "결석":
        return "bg-red-100 text-red-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }


  // 요일 정렬 함수
  const sortWeekdays = (weekdays) => {
    if (!weekdays) return '요일 정보 없음'
    
    const weekdayOrder = ['월', '화', '수', '목', '금', '토', '일']
    const days = weekdays.split(',').map(day => day.trim())
    
    return days
      .sort((a, b) => weekdayOrder.indexOf(a) - weekdayOrder.indexOf(b))
      .join(' ')
  }

  if (loading) {
    return (
      <PageLayout currentPage="academic" userRole="instructor">
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-600">데이터를 불러오는 중...</p>
          </div>
        </div>
      </PageLayout>
    )
  }

  if (error) {
    return (
      <PageLayout currentPage="academic" userRole="instructor">
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <p className="text-red-600 mb-4">{error}</p>
            <Button onClick={() => window.location.reload()}>다시 시도</Button>
          </div>
        </div>
      </PageLayout>
    )
  }

  return (
    <PageLayout currentPage="academic" userRole="instructor">
      <div className="flex">
        <Sidebar title="회원 정보" menuItems={sidebarItems} currentPath="/instructor/academic" />

        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
            {/* 헤더 */}
            <div className="mb-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    className="flex items-center space-x-2 hover:bg-gray-100 hover:text-gray-900 transition-colors duration-200"
                    onClick={() => navigate('/instructor/academic')}
                  >
                    <ArrowLeft className="w-4 h-4" />
                    <span>목록으로</span>
                  </Button>
                  <div>
                    <h1 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>
                      학생 상세 정보
                    </h1>
                    <p className="text-lg" style={{ color: "#95A5A6" }}>
                      {displayStudent.name}님의 상세 정보입니다.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* 정보 카드들 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
              {/* 학생 기본 정보 카드 */}
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50" }}>과정 정보</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="text-center mb-6">
                    <div
                      className="w-20 h-20 rounded-full mx-auto mb-4 flex items-center justify-center text-2xl font-bold text-white"
                      style={{ backgroundColor: "#1ABC9C" }}
                    >
                      {displayStudent.name ? displayStudent.name.charAt(0) : '?'}
                    </div>
                    <h3 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                      {displayStudent.name || '이름 없음'}
                    </h3>
                    <Badge
                      className={`mt-2 ${displayStudent.status === "수강중" ? "text-white" : "text-gray-600"}`}
                      style={{
                        backgroundColor: displayStudent.status === "수강중" ? "#1ABC9C" : "#95A5A6",
                      }}
                    >
                      {displayStudent.status}
                    </Badge>
                  </div>

                  <div className="space-y-3">
                    <div className="flex items-start space-x-3">
                      <BookOpen className="w-4 h-4 mt-1" style={{ color: "#95A5A6" }} />
                      <div className="flex-1">
                        <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                          수강 과정 ({studentList.length}개)
                        </p>
                        {studentList.length > 0 ? (
                          <div className="space-y-2 mt-2">
                            {studentList.map((studentItem, index) => (
                              <div 
                                key={index} 
                                className={`p-2 rounded-lg border cursor-pointer transition-colors ${
                                  selectedStudentIndex === index 
                                    ? "border-emerald-500 bg-emerald-50" 
                                    : "border-gray-200 hover:border-gray-300"
                                }`} 
                                onClick={() => handleStudentSelect(index)}
                              >
                                <div className="flex items-center justify-between">
                                  <div>
                                    <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                      {studentItem.courseName || "과정 없음"}
                                    </p>
                                  </div>
                                  <div className="flex items-center space-x-2">
                                    <Badge
                                      className={`text-xs ${
                                        (studentItem.status === "재학중" || studentItem.status === "수강중" || !studentItem.status) ? "text-white" : "text-gray-600"
                                      }`}
                                      style={{
                                        backgroundColor: (studentItem.status === "재학중" || studentItem.status === "수강중" || !studentItem.status) ? "#1ABC9C" : "#95A5A6",
                                      }}
                                    >
                                      {studentItem.status || "수강중"}
                                    </Badge>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            {displayStudent.course}
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center space-x-3">
                      <Calendar className="w-4 h-4" style={{ color: "#95A5A6" }} />
                      <div>
                        <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                          기간
                        </p>
                        <p className="text-sm" style={{ color: "#95A5A6" }}>
                          {displayStudent.courseStartDay} ~ {displayStudent.courseEndDay}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center space-x-3">
                      <Clock className="w-4 h-4" style={{ color: "#95A5A6" }} />
                      <div>
                        <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                          요일
                        </p>
                        <p className="text-sm" style={{ color: "#95A5A6" }}>
                          {sortWeekdays(displayStudent.courseDays)}
                        </p>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* 학생 정보 카드 */}
              <Card>
                <CardHeader>
                  <div className="flex space-x-4 border-b">
                    <button
                      onClick={() => setActiveTab("personal")}
                      className={`pb-2 px-1 border-b-3 font-medium text-sm ${
                        activeTab === "personal"
                          ? ""
                          : "border-transparent text-gray-500 hover:text-gray-900"
                      }`}
                    >
                      개인 정보
                    </button>
                    <button
                      onClick={() => {
                        setActiveTab("grades")
                        // 성적 탭 클릭 시 API 호출
                        if (activeTab !== "grades") {
                          getGradesHistoryData()
                        }
                      }}
                      className={`pb-2 px-1 border-b-3 font-medium text-sm ${
                        activeTab === "grades"
                          ? ""
                          : "border-transparent text-gray-500 hover:text-gray-700"
                      }`}
                    >
                      성적
                    </button>
                                         <button
                       onClick={() => {
                         setActiveTab("attendance")
                         // 출석 이력 탭 클릭 시 API 호출
                         if (activeTab !== "attendance") {
                           getAttendanceHistoryData()
                         }
                       }}
                       className={`pb-2 px-1 border-b-3 font-medium text-sm ${
                         activeTab === "attendance"
                           ? ""
                           : "border-transparent text-gray-500 hover:text-gray-700"
                       }`}
                     >
                       최근 출석 이력
                     </button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {activeTab === "personal" && (
                    <div className="space-y-3">
                      <div className="flex items-center space-x-3">
                        <Phone className="w-4 h-4" style={{ color: "#95A5A6" }} />
                        <div>
                          <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            휴대폰
                          </p>
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            {displayStudent.phone}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center space-x-3">
                        <Mail className="w-4 h-4" style={{ color: "#95A5A6" }} />
                        <div>
                          <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            이메일
                          </p>
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            {displayStudent.email}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center space-x-3">
                        <Calendar className="w-4 h-4" style={{ color: "#95A5A6" }} />
                        <div>
                          <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            생년월일
                          </p>
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            {displayStudent.birthDate}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center space-x-3">
                        <MapPin className="w-4 h-4" style={{ color: "#95A5A6" }} />
                        <div>
                          <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            주소
                          </p>
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            {displayStudent.address}
                          </p>
                        </div>
                      </div>
                    </div>
                  )}

                  {activeTab === "grades" && (
                    <div className="space-y-4">
                      <h4 className="font-semibold" style={{ color: "#2C3E50" }}>
                        성적
                      </h4>
                      <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200">
                          <thead className="bg-gray-50">
                            <tr>
                              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                시험명
                              </th>
                              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                날짜
                              </th>
                              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                점수
                              </th>
                              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                확인
                              </th>
                            </tr>
                          </thead>
                          <tbody className="bg-white divide-y divide-gray-200">
                            {gradesLoading ? (
                              <tr>
                                <td colSpan="4" className="px-3 py-2 text-center text-sm text-gray-500">
                                  <div className="flex items-center justify-center">
                                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600 mr-2"></div>
                                    성적 이력을 불러오는 중...
                                  </div>
                                </td>
                              </tr>
                            ) : gradesError ? (
                              <tr>
                                <td colSpan="4" className="px-3 py-2 text-center text-sm text-red-500">
                                  {gradesError}
                                </td>
                              </tr>
                            ) : gradesHistory && gradesHistory.length > 0 ? (
                              gradesHistory.map((grade, index) => (
                                <tr key={index}>
                                  <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-900">
                                    {grade.templateName || "시험명 없음"}
                                  </td>
                                  <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-500">
                                    {grade.createdAt ? new Date(grade.createdAt).toLocaleDateString('ko-KR') : "날짜 없음"}
                                  </td>
                                  <td className="px-3 py-2 whitespace-nowrap">
                                    <span
                                      className={`text-sm font-medium ${
                                        grade.score >= 90
                                          ? "text-green-600"
                                          : grade.score >= 80
                                            ? "text-blue-600"
                                            : grade.score >= 70
                                              ? "text-yellow-600"
                                              : "text-red-600"
                                      }`}
                                    >
                                      {grade.score}점
                                    </span>
                                  </td>
                                  <td className="px-3 py-2 whitespace-nowrap">
                                    <span
                                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                                        grade.isChecked
                                          ? "bg-green-100 text-green-800"
                                          : "bg-gray-100 text-gray-800"
                                      }`}
                                    >
                                      {grade.isChecked ? "확인완료" : "미확인"}
                                    </span>
                                  </td>
                                </tr>
                              ))
                            ) : (
                              <tr>
                                <td colSpan="4" className="px-3 py-2 text-center text-sm text-gray-500">
                                  성적 이력이 없습니다.
                                </td>
                              </tr>
                            )}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}

                  {activeTab === "attendance" && (
                    <div className="space-y-4">
                      <h4 className="font-semibold" style={{ color: "#2C3E50" }}>
                        최근 출석 이력
                      </h4>
                      
                      <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200">
                          <thead className="bg-gray-50">
                            <tr>
                              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                날짜
                              </th>
                              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                출석 상태
                              </th>
                              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                입실 시간
                              </th>
                              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                                퇴실 시간
                              </th>
                            </tr>
                          </thead>
                          <tbody className="bg-white divide-y divide-gray-200">
                            {attendanceLoading ? (
                              <tr>
                                <td colSpan="4" className="px-3 py-2 text-center text-sm text-gray-500">
                                  <div className="flex items-center justify-center">
                                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600 mr-2"></div>
                                    출석 이력을 불러오는 중...
                                  </div>
                                </td>
                              </tr>
                            ) : attendanceError ? (
                              <tr>
                                <td colSpan="4" className="px-3 py-2 text-center text-sm text-red-500">
                                  {attendanceError}
                                </td>
                              </tr>
                                                         ) : attendanceHistory && attendanceHistory.length > 0 ? (
                               attendanceHistory
                                 .sort((a, b) => new Date(b.date) - new Date(a.date)) // 최신 날짜순으로 정렬
                                 .slice(0, 10) // 최근 10개만 표시
                                 .map((record, index) => (
                                <tr key={index}>
                                  <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-900">
                                    {record.date ? new Date(record.date).toLocaleDateString('ko-KR') : "날짜 없음"}
                                  </td>
                                  <td className="px-3 py-2 whitespace-nowrap">
                                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getAttendanceColor(record.status)}`}>
                                      {record.status || "미확인"}
                                    </span>
                                  </td>
                                  <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-500">
                                    {record.checkInTime ? new Date(record.checkInTime).toLocaleTimeString('ko-KR', { 
                                      hour: '2-digit', 
                                      minute: '2-digit' 
                                    }) : "-"}
                                  </td>
                                  <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-500">
                                    {record.checkOutTime ? new Date(record.checkOutTime).toLocaleTimeString('ko-KR', { 
                                      hour: '2-digit', 
                                      minute: '2-digit' 
                                    }) : "-"}
                                  </td>
                                </tr>
                              ))
                            ) : (
                              <tr>
                                <td colSpan="4" className="px-3 py-2 text-center text-sm text-gray-500">
                                  출석 이력이 없습니다.
                                </td>
                              </tr>
                            )}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>



            {/* 강사 안내사항 */}
            <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
              <h3 className="text-sm font-medium text-blue-800 mb-2">담당 학생 관리 안내</h3>
              <ul className="text-sm text-blue-700 space-y-1">
                <li>• 학생의 성적을 확인하고 관리하세요.</li>
                <li>• 출석률이 낮거나 성적이 부진한 학생에게는 개별 상담을 진행해주세요.</li>
              </ul>
            </div>
          </div>
        </main>
      </div>
    </PageLayout>
  )
}
