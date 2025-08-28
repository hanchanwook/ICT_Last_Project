import React, { useState, useEffect, useMemo } from "react"
import { Search, Calendar, FileText, Award, Eye, Play, CheckCircle, XCircle, AlertCircle } from "lucide-react"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { getMenuItems } from "@/components/ui/menuConfig"
import { useLocation } from "react-router-dom"
import { Card, CardContent } from "@/components/ui/card"
import { getStudentCourses, getStudentExams, startNewExam as startExamAPI, submitExam as submitExamAPI } from "@/api/kayoung/studentExamApi"


export default function StudentExamsPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [subjectFilter, setSubjectFilter] = useState("all")
  const [selectedExam, setSelectedExam] = useState(null)
  const [isExamModalOpen, setIsExamModalOpen] = useState(false)
  const [currentQuestion, setCurrentQuestion] = useState(0)
  const [answers, setAnswers] = useState({})
  const [timeLeft, setTimeLeft] = useState(0)
  const [examStarted, setExamStarted] = useState(false)

  const [studentCourses, setStudentCourses] = useState([])
  const [loading, setLoading] = useState(true)

  const location = useLocation()
  const sidebarMenuItems = getMenuItems('student-exam')
  const currentPath = location.pathname

  // 시험 데이터
  const [exams, setExams] = useState([])

  // 학생의 과정 정보와 시험 정보 가져오기
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true)
        
        // 병렬로 과정 정보와 시험 정보 가져오기
        const [coursesData, examsData] = await Promise.all([
          getStudentCourses(),
          getStudentExams()
        ])
        
        // API 응답 구조에 따라 데이터 추출
        const courses = coursesData.data || coursesData || []
        const exams = examsData.data || examsData || []
        
        // 각 시험의 상세 정보 로그
        exams.forEach((exam, index) => {
        })
        
        setStudentCourses(courses)
        setExams(exams)
        
      } catch (error) {
        console.error('❌ 오류 상세:', error.response?.data || error.message)
        setStudentCourses([])
        setExams([])
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [])

  // 시험 상태별 스타일
  const getStatusStyle = (status, graded = false) => {
    switch (status) {
      case "available":
        return "bg-green-100 text-green-800"
      case "scheduled":
        return "bg-yellow-100 text-yellow-800"
      case "unavailable":
        return "bg-red-100 text-red-800"
      case "submitted":
        return "bg-blue-100 text-blue-800"        // 제출 완료
      case "completed":
        if (!graded) {
          return "bg-orange-100 text-orange-800"  // 채점 대기
        }
        return "bg-purple-100 text-purple-800"    // 채점 완료
      case "waiting":
        return "bg-blue-100 text-blue-800"        // 시험 대기
      case "expired":
        return "bg-red-100 text-red-800"          // 응시 기간 종료
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const getStatusText = (status, graded = false) => {
    switch (status) {
      case "available":
        return "응시 가능"
      case "scheduled":
        return "시험 예정"
      case "unavailable":
        return "응시 불가"
      case "submitted":
        return "제출 완료"    // 제출 완료 상태
      case "completed":
        if (!graded) {
          return "채점 대기"  // 채점 대기 상태
        }
        return "채점 완료"    // 채점 완료 상태
      case "waiting":
        return "시험 대기"
      case "expired":
        return "응시 기간 종료"
      default:
        return status || "알 수 없음"
    }
  }

  // 시험 상태 계산 함수
  const getExamStatus = (exam) => {
    const now = new Date();
    const openTime = exam.templateOpen ? new Date(new Date(exam.templateOpen).getTime() + 9 * 60 * 60 * 1000) : null;
    const closeTime = exam.templateClose ? new Date(new Date(exam.templateClose).getTime() + 9 * 60 * 60 * 1000) : null;
    
    // 이미 제출한 시험은 "submitted" 상태로 명확히 구분
    if (exam.submitted) {
      return "submitted";
    }
    
    // 이미 완료된 시험
    if (exam.status === "completed") {
      if (!exam.graded) {
        return "completed"; // 채점 대기
      }
      return "completed"; // 채점 완료
    }
    
    // 응시 시작 시간이 없는 경우 (시험 예정)
    if (!openTime) {
      return "scheduled"; // 시험 예정
    }
    
    // 응시 종료 시간이 없는 경우 (무제한)
    if (!closeTime) {
      // 현재 시간이 응시 시작 시간보다 이전
      if (now < openTime) {
        return "scheduled"; // 시험 예정
      }
      // 응시 시작 시간 이후면 응시 가능
      return "available"; // 응시 가능
    }
    
    // 응시 시작 시간과 종료 시간이 모두 있는 경우
    // 현재 시간이 응시 시작 시간보다 이전
    if (now < openTime) {
      return "scheduled"; // 시험 예정
    }
    
    // 현재 시간이 응시 종료 시간보다 이후
    if (now > closeTime) {
      return "expired"; // 응시 기간 종료
    }
    
    // 응시 기간 내
    if (now >= openTime && now <= closeTime) {
      return "available"; // 응시 가능
    }
    
    return "scheduled"; // 기본값을 시험 예정으로 변경
  }

  // 통계 계산
  const totalExams = exams.length
  const completedExams = exams.filter((exam) => 
    exam.submitted || getExamStatus(exam) === "expired"
  ).length
  const pendingGradingExams = exams.filter((exam) => exam.status === "completed" && !exam.graded).length
  const availableExams = exams.filter((exam) => getExamStatus(exam) === "available").length
  const averageScore =
    exams.filter((exam) => exam.myScore !== null).reduce((sum, exam) => sum + exam.myScore, 0) /
      exams.filter((exam) => exam.myScore !== null).length || 0

  // 필터링된 시험 목록
  const filteredExams = exams.filter((exam) => {
    const matchesSearch =
      (exam.templateName || exam.subject || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (exam.memberName || exam.instructor || '').toLowerCase().includes(searchTerm.toLowerCase())
    
    // 상태 필터 매핑
    let statusToCheck = statusFilter
    if (statusFilter === "시험 예정") statusToCheck = "scheduled"
    if (statusFilter === "응시 가능") statusToCheck = "available"
    if (statusFilter === "응시 기간 종료") statusToCheck = "expired"
    if (statusFilter === "제출완료") statusToCheck = "completed"
    
    // 상태 매칭 로직
    let matchesStatus = statusFilter === "all"
    if (statusFilter === "시험 예정") matchesStatus = getExamStatus(exam) === "scheduled"
    if (statusFilter === "응시 가능") matchesStatus = getExamStatus(exam) === "available"
    if (statusFilter === "응시 기간 종료") matchesStatus = getExamStatus(exam) === "expired"
    if (statusFilter === "제출완료") matchesStatus = exam.submitted || exam.status === "completed" || exam.status === "submitted"
    const matchesSubject = subjectFilter === "all" || (exam.courseName || exam.templateName || exam.subject) === subjectFilter
    return matchesSearch && matchesStatus && matchesSubject
  })
  
  // 필터링 과정 상세 로그
  exams.forEach((exam, index) => {
    const examStatus = getExamStatus(exam)
    const matchesSearch = (exam.templateName || exam.subject || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (exam.memberName || exam.instructor || '').toLowerCase().includes(searchTerm.toLowerCase())
    const matchesSubject = subjectFilter === "all" || (exam.courseName || exam.templateName || exam.subject) === subjectFilter
    
  })

  // 학생이 수강하는 과정 목록 추출 (과정명만 사용)
  const studentCourseList = studentCourses
    .map(course => ({
      courseId: course.courseId,
      courseName: course.courseName || course.subjectName || course.name || course.subject || '',
      courseCode: course.courseCode,
      instructor: course.memberId
    }))
    .filter(course => course.courseName !== '')
    .map(course => course.courseName) // 과정명만 추출
  
  
  // 시험 시작
  const startExam = async (exam) => {
    try {
      // API 호출하여 시험 문제 가져오기
      const response = await startExamAPI(exam.templateId || exam.id, exam.courseId)
      
      
      if (response.resultCode === "200") {
        const examData = response.data;
        
        // API 응답 구조에 맞춰 시험 데이터 설정
        setSelectedExam({
          templateId: examData.templateId,
          templateName: examData.templateName,
          templateTime: examData.templateTime,
          templateOpen: examData.templateOpen,
          templateClose: examData.templateClose,
          courseId: exam.courseId,
          courseName: exam.courseName,
          memberName: exam.memberName,
          questions: examData.questions || [],
          totalQuestions: examData.totalQuestions,
          totalScore: examData.questions?.reduce((sum, q) => sum + (q.questionScore || 0), 0) || 0
        })
        setCurrentQuestion(0)
        
        // 모든 문제에 대해 빈 답안으로 초기화
        const initialAnswers = {};
        (examData.questions || []).forEach((question) => {
          const questionId = question.questionId;
          initialAnswers[questionId] = "";
        });
        setAnswers(initialAnswers);
        
        examData.questions?.forEach((question, index) => {
        })
        
        setTimeLeft((examData.templateTime || 60) * 60) // 분을 초로 변환
        setExamStarted(true)
        setIsExamModalOpen(true)
        
        // 성공 메시지 표시
        if (examData.message) {
          console.log('🚀 시험 시작 메시지:', examData.message)
        }
        
      } else {
        console.error('❌ 시험 시작 실패:', response.resultMessage)
        alert('시험을 시작할 수 없습니다: ' + response.resultMessage)
      }
    } catch (error) {
      console.error('❌ 오류 상세:', error.response?.data || error.message)
      alert('시험을 시작할 수 없습니다.')
    }
  }

  // 시험 타이머
  useEffect(() => {
    let timer
    if (examStarted && timeLeft > 0) {
      timer = setInterval(() => {
        setTimeLeft((prev) => {
          if (prev <= 1) {
            handleSubmitExam()
            return 0
          }
          return prev - 1
        })
      }, 1000)
    }
    return () => clearInterval(timer)
  }, [examStarted, timeLeft])

  // 시간 포맷팅
  const formatTime = (seconds) => {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = seconds % 60
    return `${hours.toString().padStart(2, "0")}:${minutes.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`
  }

  // 답안 저장
  const saveAnswer = (questionId, answer) => {
    setAnswers((prev) => {
      const newAnswers = {
        ...prev,
        [questionId]: answer || "", // 빈 값도 저장
      }
      return newAnswers
    })
  }

  // 시험 제출
  const handleSubmitExam = async () => {
    try {
      // 모든 문제에 대해 답안을 포함한 완전한 답안 객체 생성
      const completeAnswers = {};
      
      selectedExam.questions.forEach((question) => {
        const questionId = question.questionId;
        // 답안이 있으면 사용하고, 없으면 빈 문자열로 설정
        completeAnswers[questionId] = answers[questionId] || "";
      });
      
      // API 호출하여 시험 제출
      const response = await submitExamAPI(selectedExam.templateId, selectedExam.courseId, completeAnswers)
      
      if (response.resultCode === "200") {
        const submitData = response.data;
        // 시험 결과 업데이트
        setExams((prev) =>
          prev.map((exam) =>
            exam.templateId === selectedExam.templateId
              ? {
                  ...exam,
                  submitted: true,
                  status: "completed",
                  graded: false, // 아직 채점되지 않음
                  attempts: (exam.attempts || 0) + 1,
                  submittedAt: submitData.submittedAt || new Date().toISOString(),
                  submittedAnswers: submitData.submittedAnswers,
                  totalQuestions: submitData.totalQuestions || selectedExam.totalQuestions,
                }
              : exam,
          ),
        )

        setExamStarted(false)
        setIsExamModalOpen(false)
        alert(submitData.message || '시험이 성공적으로 제출되었습니다!')
      } else {
        console.error('❌ 시험 제출 실패:', response.resultMessage)
        // 실패 시에도 모달 상태 초기화
        setExamStarted(false)
        setIsExamModalOpen(false)
        alert('시험 제출에 실패했습니다: ' + response.resultMessage)
      }
    } catch (error) {
      console.error('❌ 오류 상세:', error.response?.data || error.message)
      // 오류 발생 시에도 모달 상태 초기화
      setExamStarted(false)
      setIsExamModalOpen(false)
      alert('시험 제출에 실패했습니다.')
    }
  }

  return (
    <React.Fragment>
      <PageLayout currentPage="my-exam" userRole="student">
        <div className="flex">
          <Sidebar title="시험 및 성적" menuItems={sidebarMenuItems} currentPath={currentPath} />
          <main className="flex-1 p-6">
            <div className="space-y-6 max-w-7xl mx-auto">
              {/* 페이지 헤더 */}
              <div>
                <h1 className="text-2xl font-bold text-gray-900">시험 목록</h1>
                <p className="text-gray-600 mt-1">시험을 확인하고 응시할 수 있습니다.</p>
              </div>

              {/* 통계 카드 */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                <Card>
                  <CardContent className="p-6 flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-base font-medium text-gray-600">전체 시험</p>
                      <p className="text-3xl font-bold text-[#3498db]">{totalExams}개</p>
                    </div>
                    <div className="bg-[#EFF6FF] rounded-full p-3 mr-3">
                      <FileText className="w-10 h-10 text-[#3498db]" />
                    </div>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="p-6 flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-base font-medium text-gray-600">완료한 시험</p>
                      <p className="text-3xl font-bold text-[#1abc9c]">{completedExams}개</p>
                    </div>
                    <div className="bg-[#e4f5eb] rounded-full p-3 mr-3">
                      <CheckCircle className="w-10 h-10 text-[#1abc9c]" />
                    </div>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="p-6 flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-base font-medium text-gray-600">응시 가능</p>
                      <p className="text-3xl font-bold text-[#b0c4de]">{availableExams}개</p>
                    </div>
                    <div className="bg-[#eff6ff] rounded-full p-3 mr-3">
                      <AlertCircle className="w-10 h-10 text-[#b0c4de]" />
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* 디버깅 정보
              {process.env.NODE_ENV === 'development' && (
                <div className="bg-yellow-50 p-4 rounded-lg border border-yellow-200">
                  <h3 className="text-sm font-semibold text-yellow-800 mb-2">디버깅 정보</h3>
                  <div className="text-xs text-yellow-700 space-y-1">
                    <div>과정 데이터 개수: {studentCourses.length}</div>
                    <div>시험 데이터 개수: {exams.length}</div>
                    <div>수강 과정명 목록: {studentCourseList.join(', ') || '없음'}</div>
                    <div>선택된 필터: {subjectFilter}</div>
                    <div>로딩 상태: {loading ? '로딩 중' : '완료'}</div>
                  </div>
                </div>
              )} */}

              {/* 검색 및 필터 */}
              <div className="bg-white p-4 rounded-lg shadow-sm border mb-6">
                <div className="flex flex-col lg:flex-row gap-4">
                  <div className="flex-1 relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                    <input
                      type="text"
                      placeholder="과정명, 강사명으로 검색..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div className="flex gap-4">
                    <select
                      value={statusFilter}
                      onChange={(e) => setStatusFilter(e.target.value)}
                      className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="all">모든 상태</option>
                      <option value="시험 예정">시험 예정</option>
                      <option value="응시 가능">응시 가능</option>
                      <option value="응시 기간 종료">응시 기간 종료</option>
                      <option value="제출완료">제출완료</option>
                    </select>
                    <select
                      value={subjectFilter}
                      onChange={(e) => setSubjectFilter(e.target.value)}
                      className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      disabled={loading}
                    >
                      <option value="all">모든 과정</option>
                      {loading ? (
                        <option disabled>과정 정보 로딩 중...</option>
                      ) : studentCourseList.length === 0 ? (
                        <option disabled>수강 중인 과정이 없습니다</option>
                      ) : (
                        studentCourseList.map((courseName, index) => (
                          <option key={index} value={courseName}>
                            {courseName}
                          </option>
                        ))
                      )}
                    </select>
                  </div>
                </div>
              </div>

              {/* 시험 목록 */}
              <div className="space-y-4">
                {filteredExams.length === 0 ? (
                  <div className="bg-white p-8 rounded-lg shadow-sm border text-center">
                    <FileText className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500">조건에 맞는 시험이 없습니다.</p>
                  </div>
                ) : (
                  filteredExams.map((exam) => (
                    <div key={exam.id} className="bg-white p-4 rounded-lg shadow-sm border">
                      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between">
                        <div className="flex-1">
                          <div className="flex items-start justify-between mb-4">
                            <div>
                              <h3 className="text-lg font-semibold text-gray-900 mb-2">{exam.title}</h3>
                              <div className="flex flex-wrap gap-2 mb-3">
                                <span
                                  className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusStyle(getExamStatus(exam), exam.graded)}`}
                                >
                                  {getStatusText(getExamStatus(exam), exam.graded)}
                                </span>
                              </div>
                            </div>
                          </div>

                          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 text-sm text-gray-600 mb-4">
                            <div>
                              <span className="font-medium">시험명:</span> {exam.templateName || exam.title}
                            </div>
                            <div>
                              <span className="font-medium">담당강사:</span> {exam.memberName || exam.instructor}
                            </div>
                            <div>
                              <span className="font-medium">시험시간:</span> {exam.templateTime || exam.duration}분
                            </div>
                            <div>
                              <span className="font-medium">응시기간:</span> {exam.templateOpen ? new Date(new Date(exam.templateOpen).getTime() + 9 * 60 * 60 * 1000).toLocaleString('ko-KR', { 
                                year: 'numeric', 
                                month: 'numeric', 
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit'
                              }) : exam.startDate} ~ {exam.templateClose ? new Date(new Date(exam.templateClose).getTime() + 9 * 60 * 60 * 1000).toLocaleString('ko-KR', { 
                                year: 'numeric', 
                                month: 'numeric', 
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit'
                              }) : exam.endDate}
                            </div>
                          </div>

                        </div>

                        <div className="mt-4 lg:mt-0 lg:ml-6 flex flex-col gap-2">
                          {getExamStatus(exam) === "available" && (
                            <button
                              onClick={() => startExam(exam)}
                              className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors"
                            >
                              시험 시작
                            </button>
                          )}
                          {getExamStatus(exam) === "submitted" && (
                            <button
                              disabled
                              className="bg-blue-400 text-white px-4 py-2 rounded-lg flex items-center gap-2 cursor-not-allowed"
                              title="이미 제출한 시험입니다."
                            >
                              시험 제출 완료
                            </button>
                          )}
                          {getExamStatus(exam) === "waiting" && (
                            <button
                              disabled
                              className="bg-gray-400 text-white px-4 py-2 rounded-lg flex items-center gap-2 cursor-not-allowed"
                              title="시험 시작 시간이 되지 않았습니다."
                            >
                              시험 대기
                            </button>
                          )}
                          {getExamStatus(exam) === "expired" && (
                            <button
                              disabled
                              className="bg-red-400 text-white px-4 py-2 rounded-lg flex items-center gap-2 cursor-not-allowed"
                              title="시험 응시 기간이 종료되었습니다."
                            >
                              응시 종료
                            </button>
                          )}
                          {getExamStatus(exam) === "completed" && exam.graded && exam.myScore !== null && exam.grade !== null && (
                            <button
                              disabled
                              className="bg-purple-400 text-white px-4 py-2 rounded-lg flex items-center gap-2 cursor-not-allowed"
                              title="채점이 완료되었습니다."
                            >
                              채점 완료
                            </button>
                          )}
                          {getExamStatus(exam) === "completed" && !exam.graded && (
                            <button
                              disabled
                              className="bg-orange-400 text-white px-4 py-2 rounded-lg flex items-center gap-2 cursor-not-allowed"
                              title="채점 대기 중입니다."
                            >
                              채점 대기
                            </button>
                          )}
                          {getExamStatus(exam) === "scheduled" && (
                            <button
                              disabled
                              className="bg-gray-400 text-white px-4 py-2 rounded-lg flex items-center gap-2 cursor-not-allowed"
                            >
                              시험 예정
                            </button>
                          )}
                          {getExamStatus(exam) === "unavailable" && (
                            <button
                              disabled
                              className="bg-red-400 text-white px-4 py-2 rounded-lg flex items-center gap-2 cursor-not-allowed"
                            >
                              응시 불가
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </main>
        </div>
      </PageLayout>

      {/* 시험 응시 모달 */}
      {isExamModalOpen && selectedExam && (
        <div className="fixed inset-0 flex items-center justify-center z-50 p-4" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden">
            {/* 모달 헤더 */}
            <div className="bg-blue-600 text-white p-4">
              <div className="flex justify-between items-center">
                <div>
                  <h2 className="text-xl font-bold">{selectedExam.templateName}</h2>
                  <p className="text-blue-100">
                    {selectedExam.courseName} | {selectedExam.memberName}
                  </p>
                  <p className="text-blue-100 text-sm">
                    총점: {selectedExam.totalScore}점 | 문제: {selectedExam.totalQuestions}개
                  </p>
                </div>
                <div className="text-right">
                  <div className="text-2xl font-bold">{formatTime(timeLeft)}</div>
                  <div className="text-blue-100 text-sm">남은 시간</div>
                </div>
              </div>
            </div>

            {/* 진행 상황 */}
            <div className="p-4 border-b bg-gray-50">
              <div className="flex justify-between items-center mb-2">
                <span className="text-sm text-gray-600">
                  문제 {currentQuestion + 1} / {selectedExam.totalQuestions || selectedExam.questions.length}
                </span>
                <span className="text-sm text-gray-600">
                  답변 완료: {Object.values(answers).filter(answer => answer && answer.trim() !== '').length} / {selectedExam.totalQuestions || selectedExam.questions.length}
                </span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${((currentQuestion + 1) / (selectedExam.totalQuestions || selectedExam.questions.length)) * 100}%` }}
                ></div>
              </div>
              <div className="mt-2 text-xs text-gray-500">
                시험 제목: {selectedExam.templateName} | 총점: {selectedExam.totalScore}점 | 제한시간: {selectedExam.templateTime}분
              </div>
            </div>

            {/* 문제 내용 */}
            <div className="p-6 flex-1 overflow-y-auto" style={{ maxHeight: "calc(90vh - 200px)" }}>
              {selectedExam.questions && selectedExam.questions[currentQuestion] && (
                <div className="space-y-6">
                  <div>
                    <div className="flex items-center gap-2 mb-4">
                      <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded text-sm font-medium">
                        {selectedExam.questions[currentQuestion].questionType}
                      </span>
                      <span className="text-sm text-gray-600">
                        배점: {selectedExam.questions[currentQuestion].questionScore}점
                      </span>
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">
                      {selectedExam.questions[currentQuestion].questionText}
                    </h3>
                  </div>

                  {(selectedExam.questions[currentQuestion].questionType === "객관식") && (
                    <div className="space-y-3">
                      {/* 객관식 선택지 표시 */}
                      <div className="space-y-2">
                        {selectedExam.questions[currentQuestion].options && selectedExam.questions[currentQuestion].options.length > 0 ? (
                          selectedExam.questions[currentQuestion].options.map((option, index) => {
                            const question = selectedExam.questions[currentQuestion];
                            return (
                              <label key={`${question.questionId}-option-${option.optId || index}`} className="flex items-center space-x-3 p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                                <input
                                  type="radio"
                                  name={`question-${question.questionId}`}
                                  value={option.optText}
                                  checked={answers[question.questionId] === option.optText}
                                  onChange={(e) => saveAnswer(question.questionId, e.target.value)}
                                  className="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500"
                                />
                                <span className="text-gray-700">{option.optText}</span>
                              </label>
                            );
                          })
                        ) : (
                          <div className="text-center py-4 text-gray-500">
                            선택지 정보가 없습니다.
                          </div>
                        )}
                      </div>
                    </div>
                  )}

                  {(selectedExam.questions[currentQuestion].questionType === "서술형") && (
                    <div>
                      <div className="mb-2">
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          답안 입력
                        </label>
                      </div>
                      <textarea
                        value={answers[selectedExam.questions[currentQuestion].questionId] || ""}
                        onChange={(e) => saveAnswer(selectedExam.questions[currentQuestion].questionId, e.target.value)}
                        placeholder="답안을 입력하세요..."
                        className="w-full p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        rows={6}
                      />
                    </div>
                  )}

                  {selectedExam.questions[currentQuestion].questionType === "code" && (
                    <div>
                      <div className="mb-2">
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          코드 답안
                        </label>
                      </div>
                      <textarea
                        value={answers[selectedExam.questions[currentQuestion].questionId] || ""}
                        onChange={(e) => saveAnswer(selectedExam.questions[currentQuestion].questionId, e.target.value)}
                        placeholder="코드를 입력하세요..."
                        className="w-full p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                        rows={8}
                        style={{ fontFamily: 'Consolas, Monaco, "Courier New", monospace' }}
                      />
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* 모달 푸터 */}
            <div className="p-4 border-t bg-gray-50 flex justify-between">
              <div className="flex gap-2">
                <button
                  onClick={() => setCurrentQuestion(Math.max(0, currentQuestion - 1))}
                  disabled={currentQuestion === 0}
                  className="px-4 py-2 text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white rounded-lg disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  이전 문제
                </button>
                <button
                  onClick={() => setCurrentQuestion(Math.min(selectedExam.questions.length - 1, currentQuestion + 1))}
                  disabled={currentQuestion === selectedExam.questions.length - 1}
                  className="px-4 py-2 text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white rounded-lg disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  다음 문제
                </button>
              </div>
              <div className="flex gap-2">
                {/* <button
                  onClick={() => {
                    if (confirm("시험을 종료하시겠습니까? 저장되지 않은 답안은 사라집니다.")) {
                      setIsExamModalOpen(false)
                      setExamStarted(false)
                    }
                  }}
                  className="px-4 py-2 text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white rounded-lg"
                >
                  시험 종료
                </button> */}
                <button
                  onClick={() => {
                    if (confirm("시험을 제출하시겠습니까?")) {
                      handleSubmitExam()
                    }
                  }}
                  className="px-4 py-2 bg-[#1abc9c] text-white border border-[#1abc9c] hover:bg-[#16a085] rounded-lg"
                >
                  시험 제출
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </React.Fragment>
  )
}
