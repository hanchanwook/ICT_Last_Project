import { useState, useEffect } from "react"
import { useLocation } from "react-router-dom"
import React from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { BookOpen, Search, Filter, ChevronDown, ChevronUp, Clock, CheckCircle } from "lucide-react"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getMenuItems } from "@/components/ui/menuConfig"
import { getStaffCourses, getCourseExams, getCourseGrades } from "@/api/kayoung/examCoursesApi"
import { getExamQuestionStats } from "@/api/kayoung/staffexam"

export default function ExamCoursesPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [expandedCourse, setExpandedCourse] = useState(null)
  const [showGradeModal, setShowGradeModal] = useState(false)
  const [selectedSubject, setSelectedSubject] = useState(null)
  const [courses, setCourses] = useState([])
  const [subjectGrades, setSubjectGrades] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [questionStats, setQuestionStats] = useState(null)
  const [questionStatsLoading, setQuestionStatsLoading] = useState(false)

  const location = useLocation()

  // 동적 사이드바 메뉴 생성
  const sidebarMenuItems = getMenuItems('exam')
  const currentPath = location.pathname

  // 요일을 월화수목금 순으로 정렬하는 함수
  const sortDaysInOrder = (daysString) => {
    if (!daysString) return ""
    
    // 요일 매핑 (정렬 순서)
    const dayOrder = {
      '월': 1, '화': 2, '수': 3, '목': 4, '금': 5, '토': 6, '일': 7
    }
    
    // 문자열을 요일로 분리
    const days = daysString.split('').filter(day => dayOrder.hasOwnProperty(day))
    
    // 요일 순서대로 정렬
    const sortedDays = days.sort((a, b) => dayOrder[a] - dayOrder[b])
    
    return sortedDays.join('')
  }

  // API에서 과정 데이터 로드
  useEffect(() => {
    const loadCourses = async () => {
      try {
        setLoading(true)
        setError(null)
        
        // 직원용 과정 목록 조회 (해당 학원의 educationId 기준)
        const response = await getStaffCourses()
        
        if (response.success && response.data) {
          // 과정 데이터를 컴포넌트에서 사용할 형태로 변환
          const formattedCourses = response.data.map((course, index) => ({
            id: course.courseId || index + 1,
            code: course.courseCode || `COURSE${index + 1}`,
            name: course.courseName,
            instructor: course.memberName || "강사",
            duration: sortDaysInOrder(course.courseDays || ""),
            students: course.studentCount || 0,
            examStatus: course.examCount > 0 ? "진행중" : "예정",
            examCount: course.examCount || 0,
            avgScore: course.averageScore || 0,
            startDate: course.courseStartDay || "날짜 미정",
            endDate: course.courseEndDay || "날짜 미정",
            subjects: [], // API에서 subjects 정보가 없으므로 빈 배열
            courseActive: course.courseActive,
            maxCapacity: course.maxCapacity,
            classNumber: course.classNumber,
            startTime: course.startTime,
            endTime: course.endTime
          }))
          
          setCourses(formattedCourses)
        } else {
          console.warn('과정 데이터 로드 실패:', response.message)
          setCourses([])
        }
        
        // 과목별 성적 데이터는 빈 객체로 초기화 (필요할 때 로드)
        setSubjectGrades({})
        
      } catch (error) {
        console.error('과정 데이터 로드 실패:', error)
        setError('과정 데이터를 불러오는데 실패했습니다.')
        setCourses([])
      } finally {
        setLoading(false)
      }
    }
    
    loadCourses()
  }, [])

  // 통계 계산
  const statuses = ["all", "진행중", "완료", "예정"]

  const filteredCourses = courses.filter((course) => {
    const matchesSearch =
      course.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      course.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
      course.instructor.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = selectedStatus === "all" || course.examStatus === selectedStatus
    return matchesSearch && matchesStatus
  })

  const getStatusColor = (status) => {
    switch (status) {
      case "진행중":
        return "bg-[#EFF6FF] text-[#3498db]"
      case "완료":
        return "bg-[#EFF6FF] text-[#b0c4de]"
      case "예정":
        return "bg-[#e4f5eb] text-[#1abc9c]"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const handleToggleSubjects = async (courseId) => {
    if (expandedCourse === courseId) {
      // 접기
      setExpandedCourse(null)
    } else {
      // 펼치기 - 해당 과정의 시험과 성적 정보 로드
      setExpandedCourse(courseId)
      
      try {
        // 병렬로 시험 목록과 성적 통계 가져오기
        const [examsResponse, gradesResponse] = await Promise.all([
          getCourseExams(courseId),
          getCourseGrades(courseId)
        ])
        
        // 성적 통계 데이터에서 시험별 상세 정보 구성
        let subjectsData = []
        if (gradesResponse.success && gradesResponse.data) {
          const gradeData = gradesResponse.data
          subjectsData = gradeData.examDetails ? 
            gradeData.examDetails.map(exam => ({
              id: exam.examId,
              name: exam.examName,
              avgScore: exam.avgScore || 0,
              passRate: exam.passRate || 0,
              participantCount: exam.participantCount || 0,
              questionCount: exam.questionCount || 0,
              gradeDistribution: exam.gradeDistribution || {},
              examStatus: exam.examStatus || 'NO_SCHEDULE',
              examStatusDescription: exam.examStatusDescription || '일정 미설정',
              // 전체 과정 통계도 포함
              totalAvgScore: gradeData.avgScore || 0,
              totalPassRate: gradeData.passRate || 0,
              totalExamCount: gradeData.examCount || 0,
              studentCount: gradeData.studentCount || 0
            })) : []
        }
        
        setSubjectGrades(prev => ({
          ...prev,
          [courseId]: subjectsData
        }))
        
      } catch (error) {
        console.error('과정별 데이터 로드 실패:', error)
        setSubjectGrades(prev => ({
          ...prev,
          [courseId]: []
        }))
      }
    }
  }

  const handleView = (courseId) => {
  }

  const handleEdit = (courseId) => {
  }

  const handleDelete = (courseId) => {
  }

  const handleCreateExam = (courseId) => {
  }

  const handleSubjectClick = async (subject, courseInfo) => {
    try {
      setQuestionStatsLoading(true)
      setQuestionStats(null)
      
      // 선택한 시험의 정보만 포함하여 설정
      setSelectedSubject({ 
        ...subject, 
        courseInfo,
        selectedExam: subject // 선택한 특정 시험 정보
      })
      setShowGradeModal(true)
      
      // 문제별 통계 가져오기
      const statsResponse = await getExamQuestionStats(subject.id)
      
      if (statsResponse.success && statsResponse.data) {
        setQuestionStats(statsResponse.data)
      } else {
        console.warn('문제별 통계 로드 실패:', statsResponse.message)
        setQuestionStats(null)
      }
      
    } catch (error) {
      console.error('문제별 통계 로드 실패:', error)
      setQuestionStats(null)
    } finally {
      setQuestionStatsLoading(false)
    }
  }

  return (
    <React.Fragment>
      <PageLayout currentPage="exam" userRole="staff">
        <div className="flex">
          <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
          <main className="flex-1 p-8">
            <div className="space-y-6 max-w-7xl mx-auto">
              {/* 페이지 헤더 */}
              <div className="flex justify-between items-center">
                <div>
                  <h1 className="text-3xl font-bold" style={{ color: "#2C3E50" }}>
                    과정 리스트
                  </h1>
                  <p className="text-gray-600 mt-2">시험이 있는 전체 과정을 관리할 수 있습니다.</p>
                </div>
              </div>

              {/* 통계 카드 */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <Card>
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="ml-4">
                        <p className="text-m font-medium text-gray-600">전체 과정</p>
                        <p className="text-3xl font-bold" style={{ color: "#3498db" }}>
                          {courses.length}개
                        </p>
                      </div>
                      <div className="bg-[#EFF6FF] rounded-full p-3 mr-4">
                        <BookOpen className="w-10 h-10" style={{ color: "#3498db" }} />
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="ml-4">
                        <p className="text-m font-medium text-gray-600">진행 중인 과정</p>
                        <p className="text-3xl font-bold" style={{ color: "#1abc9c" }}>
                          {courses.filter(c => c.examStatus === "진행중").length}개
                        </p>
                      </div>
                      <div className="bg-[#e4f5eb] rounded-full p-3 mr-4">
                        <Clock className="w-10 h-10" style={{ color: "#1abc9c" }} />
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="ml-4">
                        <p className="text-m font-medium text-gray-600">완료된 과정</p>
                        <p className="text-3xl font-bold" style={{ color: "#b0c4de" }}>
                          {courses.filter(c => c.examStatus === "완료").length}개
                        </p>
                      </div>
                      <div className="bg-[#EFF6FF] rounded-full p-3 mr-4">
                        <CheckCircle className="w-10 h-10" style={{ color: "#b0c4de" }} />
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* 검색 및 필터 */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2" style={{ color: "#2C3E50" }}>
                    <Filter className="w-5 h-5" />
                    검색 및 필터
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="flex flex-col md:flex-row gap-4">
                    <div className="flex-1">
                      <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                        <Input
                          placeholder="과정명, 과정코드, 강사명으로 검색..."
                          value={searchTerm}
                          onChange={(e) => setSearchTerm(e.target.value)}
                          className="pl-10 h-10"
                        />
                      </div>
                    </div>
                    <select
                      value={selectedStatus}
                      onChange={(e) => setSelectedStatus(e.target.value)}
                      className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
                    >
                      {statuses.map((status) => (
                        <option key={status} value={status}>
                          {status === "all" ? "전체 상태" : status}
                        </option>
                      ))}
                    </select>
                  </div>
                </CardContent>
              </Card>

              {/* 과정 목록 */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2" style={{ color: "#2C3E50" }}>
                    <BookOpen className="w-5 h-5" />
                    과정 목록 ({filteredCourses.length}개)
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b">
                          <th className="text-left py-3 px-4 font-semibold text-gray-700">과정 정보</th>
                          <th className="text-left py-3 px-4 font-semibold text-gray-700">강사</th>
                          <th className="text-left py-3 px-4 font-semibold text-gray-700">기간</th>
                          <th className="text-left py-3 px-4 font-semibold text-gray-700">수강생</th>
                          <th className="text-left py-3 px-4 font-semibold text-gray-700">시험 현황</th>
                          <th className="text-left py-3 px-4 font-semibold text-gray-700">평균 점수</th>
                          <th className="text-center py-3 px-4 font-semibold text-gray-700"></th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredCourses.flatMap((course) => [
                          <tr key={`${course.id}-main`} className="border-b hover:bg-gray-50">
                            <td className="py-3 px-4">
                              <div>
                                <div className="font-semibold" style={{ color: "#2C3E50" }}>
                                  {course.name}
                                </div>
                                <div className="text-sm text-gray-600">{course.code}</div>
                              </div>
                            </td>
                            <td className="py-3 px-4 text-gray-700">{course.instructor}</td>
                            <td className="py-3 px-4">
                              <div className="text-sm">
                                <div>{course.duration}</div>
                                <div className="text-gray-500">
                                  {course.startDate} ~ {course.endDate}
                                </div>
                              </div>
                            </td>
                            <td className="py-3 px-4">
                              <div className="text-center">
                                <div className="font-semibold">{course.students}명</div>
                              </div>
                            </td>
                            <td className="py-3 px-4">
                              <div className="space-y-1">
                                <Badge className={getStatusColor(course.examStatus)}>{course.examStatus}</Badge>
                                <div className="text-xs text-gray-600">시험 {course.examCount}개</div>
                              </div>
                            </td>
                            <td className="py-3 px-4">
                              <div className="text-center">
                                {course.avgScore > 0 ? (
                                  <div className="font-semibold" style={{ color: "#2c3e50" }}>
                                    {course.avgScore}점
                                  </div>
                                ) : (
                                  <span className="text-gray-400">-</span>
                                )}
                              </div>
                            </td>
                            <td className="py-3 px-4">
                              <div className="flex justify-center">
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={() => handleToggleSubjects(course.id)}
                                  className="p-2"
                                >
                                  {expandedCourse === course.id ? (
                                    <ChevronUp className="w-4 h-4 " style={{ color: "#2C3E50" }} />
                                  ) : (
                                    <ChevronDown className="w-4 h-4" style={{ color: "#2C3E50" }} />
                                  )}
                                </Button>
                              </div>
                            </td>
                          </tr>,
                          expandedCourse === course.id && (
                            <tr key={`${course.id}-subjects`}>
                              <td colSpan="7" className="py-4 px-4 bg-gray-50">
                                <div className="space-y-3">
                                  <h4 className="font-semibold text-gray-700 mb-3">시험별 성적 현황</h4>
                                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                                    {subjectGrades[course.id]?.map((subject) => (
                                      <div
                                        key={subject.id}
                                        className="bg-white p-4 rounded-lg border cursor-pointer hover:shadow-md transition-shadow"
                                        onClick={() => handleSubjectClick(subject, course)}
                                      >
                                        <h5 className="font-medium text-gray-800 mb-2">{subject.name}</h5>
                                        <div className="space-y-2 text-sm">
                                          <div className="flex justify-between">
                                            <span className="text-gray-600">평균 점수:</span>
                                            <span className="font-semibold" style={{ color: "#3498db" }}>
                                              {subject.examStatus === 'ENDED' ? 
                                                (subject.avgScore !== null ? `${subject.avgScore}점` : "0점") : 
                                                "미실시"}
                                            </span>
                                          </div>
                                          <div className="flex justify-between">
                                            <span className="text-gray-600">합격률:</span>
                                            <span className="font-semibold text-green-600">
                                              {subject.examStatus === 'ENDED' ? 
                                                (subject.passRate !== null ? `${subject.passRate}%` : "0%") : 
                                                "미실시"}
                                            </span>
                                          </div>
                                          <div className="flex justify-between">
                                            <span className="text-gray-600">문제 수:</span>
                                            <span className="font-semibold text-blue-600">{subject.questionCount}개</span>
                                          </div>
                                        </div>
                                      </div>
                                    )) || (
                                      <div className="col-span-full text-center text-gray-500 py-4">
                                        등록된 과목이 없습니다.
                                      </div>
                                    )}
                                  </div>
                                </div>
                              </td>
                            </tr>
                          )
                        ].filter(Boolean))}
                      </tbody>
                    </table>
                  </div>
                </CardContent>
              </Card>
            </div>
          </main>
        </div>
      </PageLayout>
      {showGradeModal && selectedSubject && (
        <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <div>
                <h2 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>
                  {selectedSubject.name} - 성적 상세정보
                </h2>
                <p className="text-gray-600 mt-1">
                  {selectedSubject.courseInfo.name} ({selectedSubject.courseInfo.code})
                </p>
              </div>
              <Button
                variant="ghost"
                onClick={() => setShowGradeModal(false)}
                className="text-gray-500 hover:text-gray-700"
              >
                ✕
              </Button>
            </div>

            {selectedSubject.selectedExam.examStatus === 'ENDED' ? (
              // 시험이 완료된 경우에만 통계 섹션 표시
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* 기본 통계 */}
                <div className="lg:col-span-1">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-lg" style={{ color: "#9b59b6" }}>
                        기본 통계
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4 h-64 flex flex-col justify-center">
                      <div className="flex justify-between">
                        <span className="text-gray-600">평균 점수:</span>
                        <span className="font-semibold" style={{ color: "#9b59b6" }}>
                          {selectedSubject.selectedExam.avgScore}점
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-600">합격률:</span>
                        <span className="font-semibold text-green-600">
                          {selectedSubject.selectedExam.passRate}%
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-600">참여자 수:</span>
                        <span className="font-semibold text-blue-600">{selectedSubject.selectedExam.participantCount}명</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-600">전체 수강생:</span>
                        <span className="font-semibold">{selectedSubject.studentCount}명</span>
                      </div>
                    </CardContent>
                  </Card>
                </div>

                {/* 문제별 통계 */}
                <div className="lg:col-span-2">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-lg" style={{ color: "#9b59b6" }}>
                        문제별 통계
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        {questionStatsLoading ? (
                          <div className="text-center py-8">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600 mx-auto"></div>
                            <p className="text-gray-600 mt-2">문제별 통계를 불러오는 중...</p>
                          </div>
                        ) : questionStats && questionStats.questionStats && questionStats.questionStats.length > 0 ? (
                          <div className="space-y-3">
                            <div className="flex justify-between items-center mb-4">
                              <h4 className="font-semibold text-gray-800">{questionStats.examName}</h4>
                              <div className="text-sm text-gray-600">
                                총 {questionStats.totalQuestions}개 문제 | 평균 정답률: {questionStats.avgCorrectRate}%
                              </div>
                            </div>
                            <div className="h-64 overflow-y-auto pr-2">
                              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                                {questionStats.questionStats.map((question, index) => (
                                  <div key={index} className="border rounded-lg p-4 bg-gray-50">
                                    <div className="text-center mb-3">
                                      <h5 className="font-medium text-gray-800 text-lg">문제 {question.questionNumber || index + 1}</h5>
                                      <div className="text-2xl font-bold mt-1" style={{ color: "#9b59b6" }}>
                                        {question.correctRate}%
                                      </div>
                                      <div className="text-sm text-gray-500 mt-1">{question.questionType}</div>
                                    </div>
                                    <div className="space-y-3 text-sm">
                                      <div className="flex justify-between items-center">
                                        <span className="text-gray-600">정답자:</span>
                                        <span className="font-semibold text-green-600">{question.correctCount}명</span>
                                      </div>
                                      <div className="flex justify-between items-center">
                                        <span className="text-gray-600">오답자:</span>
                                        <span className="font-semibold text-red-600">{question.incorrectCount}명</span>
                                      </div>
                                      <div className="flex justify-between items-center">
                                        <span className="text-gray-600">미응답:</span>
                                        <span className="font-semibold text-gray-600">{question.noAnswerCount || 0}명</span>
                                      </div>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          </div>
                        ) : (
                          <div className="text-center py-8 text-gray-500">
                            문제별 통계 정보가 없습니다.
                          </div>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                </div>
              </div>
            ) : (
              // 시험이 미실시인 경우 - 안내 메시지 표시
              <div className="text-center py-12">
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-8 max-w-md mx-auto">
                  <div className="text-yellow-600 mb-4">
                    <svg className="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                    </svg>
                  </div>
                  <h3 className="text-lg font-semibold text-gray-800 mb-2">시험 미실시</h3>
                  <p className="text-gray-600 mb-4">
                    아직 시험이 진행되지 않았습니다.<br />
                    시험 완료 후 통계를 확인할 수 있습니다.
                  </p>
                  <div className="text-sm text-gray-500">
                    <div className="flex justify-center items-center gap-4">
                      <span>총 문제: {selectedSubject.selectedExam.questionCount}개</span>
                      <span>수강생: {selectedSubject.studentCount}명</span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 성적 분포 - 시험 완료된 경우에만 표시 */}
            {selectedSubject.selectedExam.examStatus === 'ENDED' && selectedSubject.selectedExam.gradeDistribution && Object.keys(selectedSubject.selectedExam.gradeDistribution).length > 0 && (
              <div className="mt-6">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg" style={{ color: "#9b59b6" }}>
                      {selectedSubject.selectedExam.name} 시험 성적 분포
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-5 gap-4 text-center">
                      <div className="bg-red-50 p-4 rounded-lg">
                        <div className="text-2xl font-bold text-red-600">{selectedSubject.selectedExam.gradeDistribution.F || 0}명</div>
                        <div className="text-sm text-gray-600">F (0-59점)</div>
                      </div>
                      <div className="bg-orange-50 p-4 rounded-lg">
                        <div className="text-2xl font-bold text-orange-600">{selectedSubject.selectedExam.gradeDistribution.D || 0}명</div>
                        <div className="text-sm text-gray-600">D (60-69점)</div>
                      </div>
                      <div className="bg-yellow-50 p-4 rounded-lg">
                        <div className="text-2xl font-bold text-yellow-600">{selectedSubject.selectedExam.gradeDistribution.C || 0}명</div>
                        <div className="text-sm text-gray-600">C (70-79점)</div>
                      </div>
                      <div className="bg-blue-50 p-4 rounded-lg">
                        <div className="text-2xl font-bold text-blue-600">{selectedSubject.selectedExam.gradeDistribution.B || 0}명</div>
                        <div className="text-sm text-gray-600">B (80-89점)</div>
                      </div>
                      <div className="bg-green-50 p-4 rounded-lg">
                        <div className="text-2xl font-bold text-green-600">{selectedSubject.selectedExam.gradeDistribution.A || 0}명</div>
                        <div className="text-sm text-gray-600">A (90-100점)</div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}

            {/* 하단 버튼 */}
            <div className="flex justify-end gap-3 mt-6">
              <Button variant="outline" onClick={() => setShowGradeModal(false)}>
                닫기
              </Button>
            </div>
          </div>
        </div>
      )}
    </React.Fragment>
  )
}
