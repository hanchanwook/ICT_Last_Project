import React, { useState, useEffect } from "react"
import { useLocation, useNavigate } from "react-router-dom"
import StatsCard from "@/components/ui/stats-card"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { FileText, Clock, Users, TrendingUp, Plus, Eye, Edit, Search, Download, Calendar, CheckCircle, X, BookOpen, Trash, Trash2 } from "lucide-react"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { getMenuItems } from "@/components/ui/menuConfig"
import { Link } from "react-router-dom"
import { getMyExamTemplates, deactivateExamTemplate, openExam, closeExamWithAutoSubmission } from "@/api/kayoung/templateApi"
import { getCourseStudents, getExamSubmissions } from "@/api/kayoung/studentExamApi"

export default function InstructorExamMyExamsPage() {
  const navigate = useNavigate()
  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [courseFilter, setCourseFilter] = useState("all")
  const [examStatus, setExamStatus] = useState({})
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [examToDelete, setExamToDelete] = useState(null)
  
  // 실제 데이터 상태
  const [stats, setStats] = useState([
    {
      title: "총 출제 시험",
      value: "0개",
      icon: FileText,
      color: "#9b59b6",
    },
    {
      title: "진행 중인 시험",
      value: "0개",
      icon: Clock,
      color: "#9b59b6",
    },
    {
      title: "채점 대기",
      value: "0건",
      icon: Edit,
      color: "#9b59b6",
    },
    {
      title: "평균 응시율",
      value: "0%",
      icon: TrendingUp,
      color: "#9b59b6",
    },
  ])
  
  const [gradingQueue, setGradingQueue] = useState([])
  const [myExams, setMyExams] = useState([])
  const [courses, setCourses] = useState(["all"])
  const [statuses, setStatuses] = useState(["all", "예정", "진행중", "채점중", "완료"])
  
  // 로딩 상태
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const location = useLocation()
  const sidebarMenuItems = getMenuItems('instructor-exam')
  const currentPath = location.pathname

  // 과정별 학생 목록 조회 함수 (subGroupId 사용)
  const loadCourseStudents = async (subGroupId) => {
    try {
      if (!subGroupId) {
        return []
      }
      const response = await getCourseStudents(subGroupId)
      const studentsData = response?.data || response || []
      return studentsData
    } catch (error) {
      console.error('에러 상세 정보:', {
        message: error.message,
        status: error.response?.status,
        data: error.response?.data
      })
      return []
    }
  }



  const loadMyExams = async () => {
    try {
      const response = await getMyExamTemplates()
      
      // API 응답 구조에 따라 데이터 변환
      const examsData = response?.data || response || []
      
      // templateActive가 0인 시험만 필터링
      const activeExamsData = examsData.filter(exam => exam.templateActive === 0)
      
      // 각 시험의 원본 데이터 확인
      // examsData.forEach((exam, index) => {
      //   console.log(`시험 ${index + 1} 원본 데이터:`, {
      //     templateId: exam.templateId,
      //     templateName: exam.templateName,
      //     subGroupId: exam.subGroupId,
      //     courseId: exam.courseId,
      //     courseName: exam.courseName,
      //     courseCode: exam.courseCode
      //   })
      // })
      
      // 시험 데이터를 UI에서 사용하는 형식으로 변환
      const transformedExams = activeExamsData.map(exam => ({
        id: exam.templateId || exam.id,
        templateId: exam.templateId || exam.id,
        title: exam.templateName || exam.title,
        course: exam.courseName || exam.course,
        courseId: exam.courseId,
        subGroupId: exam.subGroupId,
        courseCode: exam.courseCode,
        type: exam.templateType || exam.type || "",
        status: (() => {
          
          // templateOpen과 templateClose가 모두 null인 경우
          if (exam.templateOpen === null && exam.templateClose === null) {
            return "예정"
          }
          
          // templateOpen만 있고 templateClose가 null인 경우 (시험 열린 상태)
          if (exam.templateOpen && exam.templateClose === null) {
            return "진행중"
          }
          
          // 시험 기간이 설정된 경우
          if (exam.templateOpen && exam.templateClose) {
            const now = new Date()
            const openDate = new Date(new Date(exam.templateOpen).getTime() + 9 * 60 * 60 * 1000) // 한국 시간으로 변환
            const closeDate = new Date(new Date(exam.templateClose).getTime() + 9 * 60 * 60 * 1000) // 한국 시간으로 변환
            
            if (now < openDate) {
              return "예정"
            } else if (now >= openDate && now <= closeDate) {
              return "진행중"
            } else {
              return "종료"
            }
          }
          
          // 기본값
          return "예정"
        })(),
        openDate: exam.templateOpen ? new Date(new Date(exam.templateOpen).getTime() + 9 * 60 * 60 * 1000).toLocaleString() : null,
        closeDate: exam.templateClose ? new Date(new Date(exam.templateClose).getTime() + 9 * 60 * 60 * 1000).toLocaleString() : null,
        duration: exam.templateTime || 0,
        participants: exam.totalStudents || exam.participantCount || 0,
        submitted: exam.submittedCount || 0,
        graded: exam.gradedCount || 0,
        waitingForGrading: exam.waitingForGradingCount || 0,
        avgScore: exam.averageScore || 0,
        totalQuestions: exam.totalQuestions || 0,
        submissionRate: exam.submissionRate || "0%",
        gradingRate: exam.gradingRate || "0%",
        createdDate: exam.createdAt ? new Date(exam.createdAt).toLocaleDateString() : '날짜 없음'
      }))
      
      
      setMyExams(transformedExams)
    } catch (error) {
      setMyExams([])
    }
  }

  const loadCourses = async () => {
    try {
      // 시험 목록에서 고유한 과정 정보를 추출
      const uniqueCourses = []
      const courseMap = new Map()
      
      myExams.forEach(exam => {
        if (exam.course && exam.courseId && !courseMap.has(exam.courseId)) {
          courseMap.set(exam.courseId, {
            courseId: exam.courseId,
            courseName: exam.course,
            courseCode: exam.courseCode
          })
          uniqueCourses.push({
            courseId: exam.courseId,
            courseName: exam.course,
            courseCode: exam.courseCode
          })
        }
      })
      
      setCourses(['all', ...uniqueCourses])
    } catch (error) {
      console.error('과정 목록 로드 실패:', error)
      setCourses(['all'])
    }
  }

  // 컴포넌트 마운트 시 데이터 로드
  useEffect(() => {
    const loadData = async () => {
      setLoading(true)
      setError(null)
      
      try {
        await Promise.all([
          loadMyExams()
        ])
        // loadCourses는 myExams가 설정된 후 별도 useEffect에서 호출됨
      } catch (error) {
        console.error('데이터 로드 실패:', error)
        setError('데이터를 불러오는 데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [])

  // myExams가 변경될 때마다 과정 목록 업데이트
  useEffect(() => {
    if (myExams.length > 0) {
      loadCourses()
    }
  }, [myExams])

  // 시험 목록이 로드된 후 통계 계산
  useEffect(() => {
    if (myExams.length > 0) {
      const totalExams = myExams.length
      const activeExams = myExams.filter(exam => exam.status === "진행중").length
      const scheduledExams = myExams.filter(exam => exam.status === "예정").length
      const completedExams = myExams.filter(exam => exam.status === "종료").length
      const pendingGrading = myExams.reduce((sum, exam) => sum + exam.waitingForGrading, 0)
      const avgParticipation = myExams.length > 0 
        ? Math.round(myExams.reduce((sum, exam) => {
            const rate = parseInt(exam.submissionRate) || 0
            return sum + rate
          }, 0) / myExams.length)
        : 0

      setStats([
        {
          title: "총 출제 시험",
          value: `${totalExams}개`,
          icon: FileText,
          color: "#3498db",
        },
        {
          title: "진행/예정 시험",
          value: `${activeExams + scheduledExams}개`,
          icon: Clock,
          color: "#1abc9c",
        },
        {
          title: "채점 대기",
          value: `${pendingGrading}건`,
          icon: Edit,
          color: "#b0c4de",
        },
        {
          title: "평균 응시율",
          value: `${avgParticipation}%`,
          icon: TrendingUp,
          color: "#3498db",
        },
      ])
    }
  }, [myExams])

  const filteredExams = myExams.filter((exam) => {
    const matchesSearch =
      exam.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      exam.course.toLowerCase().includes(searchTerm.toLowerCase()) ||
      exam.courseCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (exam.subject && exam.subject.toLowerCase().includes(searchTerm.toLowerCase()))
    const matchesStatus = statusFilter === "all" || exam.status === statusFilter
    const matchesCourse = courseFilter === "all" || exam.courseId === courseFilter
    return matchesSearch && matchesStatus && matchesCourse
  })

  const getStatusColor = (status) => {
    switch (status) {
      case "진행중":
        return "bg-blue-100 text-blue-800"
      case "채점중":
        return "bg-yellow-100 text-yellow-800"
      case "완료":
        return "bg-green-100 text-green-800"
      case "예정":
        return "bg-gray-100 text-gray-800"
      // case "비활성화":
      //   return "bg-red-100 text-red-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const getGradingStatusColor = (status) => {
    switch (status) {
      case "채점 대기":
        return "bg-red-100 text-red-800"
      case "채점 완료":
        return "bg-green-100 text-green-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const getTypeColor = (type) => {
    switch (type) {
      case "중간고사":
        return "bg-purple-100 text-purple-800"
      case "기말고사":
        return "bg-red-100 text-red-800"
      case "":
        return "bg-gray-100 text-gray-800"
      case "실습평가":
        return "bg-orange-100 text-orange-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const getProgressPercentage = (submitted, total) => {
    return total > 0 ? Math.round((submitted / total) * 100) : 0
  }

  const getGradingPercentage = (graded, submitted) => {
    return submitted > 0 ? Math.round((graded / submitted) * 100) : 0
  }

  const toggleExamStatus = async (templateId, currentStatus) => {
    try {
      
      if (currentStatus === "예정") {
        // 시험 열기
        
        await openExam(templateId)
        
        // 데이터 새로고침
        await loadMyExams()
        
        alert('시험이 열렸습니다.')
      } else if (currentStatus === "진행중") {
        // 시험 닫기
        
        // 해당 시험 정보 찾기
        const exam = myExams.find(e => e.templateId === templateId)
        
        // 과정의 모든 학생들의 memberId 수집
        // subGroupId가 없으면 courseId를 사용 (임시 해결책)
        const subGroupId = exam?.subGroupId || exam?.courseId
        
        if (!subGroupId) {
          throw new Error('서브그룹 ID 또는 과정 ID를 찾을 수 없습니다.')
        }

        const courseStudents = await loadCourseStudents(subGroupId)
        
        const studentMemberIds = courseStudents.map(student => student.memberId)

        // 미제출 학생 수 계산
        const totalStudents = exam.participants || courseStudents.length
        const submittedStudents = exam.submitted || 0
        const unsubmittedCount = Math.max(0, totalStudents - submittedStudents)

        // 미제출 학생이 있는 경우 확인 메시지 표시
        if (unsubmittedCount > 0) {
          const confirmMessage = `미제출 학생이 ${unsubmittedCount}명 있습니다.\n\n미제출 학생들은 자동으로 0점 처리됩니다.\n\n정말로 시험을 닫으시겠습니까?`
          const isConfirmed = window.confirm(confirmMessage)
          
          if (!isConfirmed) {
            return
          }
        } else {
          // 모든 학생이 제출한 경우
          const isConfirmed = window.confirm('모든 학생이 시험을 제출했습니다.\n\n시험을 닫으시겠습니까?')
          
          if (!isConfirmed) {
            return
          }
        }

        await closeExamWithAutoSubmission(templateId, studentMemberIds)
        
        // 데이터 새로고침
        await loadMyExams()
        
        // 성공 메시지 (미제출 학생 수 포함)
        if (unsubmittedCount > 0) {
          alert(`시험이 닫혔습니다.\n\n미제출 학생 ${unsubmittedCount}명이 있습니다.\n\n백엔드에서 미제출 학생 자동 처리 기능이 구현되면 0점으로 처리됩니다.`)
        } else {
          alert('시험이 닫혔습니다.')
        }
      } else {
        // 완료 상태에서는 변경 불가
        alert('완료된 시험은 상태를 변경할 수 없습니다.')
        return
      }
      
    } catch (error) {
      console.error('시험 상태 변경 실패:', error)
      alert('시험 상태 변경에 실패했습니다.')
    }
  }

  // 데이터 새로고침
  const handleRefresh = async () => {
    setLoading(true)
    setError(null)
    
    try {
      await Promise.all([
        loadMyExams(),
        loadCourses()
      ])
    } catch (error) {
      console.error('데이터 새로고침 실패:', error)
      setError('데이터를 새로고침하는 데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteExam = (exam) => {
    setExamToDelete(exam)
    setIsDeleteModalOpen(true)
  }

  const confirmDeleteExam = async () => {
    try {
      setIsDeleting(true)
      
      await deactivateExamTemplate(examToDelete.id)
      
      // 로컬에서 시험 제거
      setMyExams(prev => prev.filter(exam => exam.id !== examToDelete.id))
      
      // 모달 닫기
      setIsDeleteModalOpen(false)
      setExamToDelete(null)
    } catch (error) {
      console.error('시험 삭제 실패:', error)
      alert('시험 삭제에 실패했습니다.')
    } finally {
      setIsDeleting(false)
    }
  }

  const cancelDeleteExam = () => {
    setIsDeleteModalOpen(false)
    setExamToDelete(null)
  }

  // 로딩 중일 때 스켈레톤 UI 표시
  if (loading) {
    return (
      <React.Fragment>
        <PageLayout currentPage="exam" userRole="instructor">
          <div className="flex">
            <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="space-y-6">
                {/* 페이지 헤더 스켈레톤 */}
                <div className="text-center py-8">
                  <div className="h-8 bg-gray-200 rounded w-1/3 mx-auto mb-4"></div>
                  <div className="h-4 bg-gray-200 rounded w-1/2 mx-auto"></div>
                </div>

                {/* 통계 카드 스켈레톤 */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                  {[...Array(4)].map((_, i) => (
                    <div key={i} className="bg-white p-6 rounded-lg shadow-sm">
                      <div className="flex items-center justify-between">
                        <div className="space-y-2">
                          <div className="h-4 bg-gray-200 rounded w-20"></div>
                          <div className="h-6 bg-gray-200 rounded w-12"></div>
                        </div>
                        <div className="w-10 h-10 bg-gray-200 rounded-lg"></div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* 검색 필터 스켈레톤 */}
                <div className="bg-white p-6 rounded-lg shadow-sm">
                  <div className="flex items-center gap-4">
                    <div className="flex-1 h-10 bg-gray-200 rounded"></div>
                    <div className="h-10 bg-gray-200 rounded w-24"></div>
                    <div className="h-10 bg-gray-200 rounded w-32"></div>
                    <div className="h-10 bg-gray-200 rounded w-32"></div>
                  </div>
                </div>

                {/* 시험 목록 스켈레톤 */}
                <div className="space-y-4">
                  {[...Array(3)].map((_, i) => (
                    <div key={i} className="bg-white p-6 rounded-lg shadow-sm">
                      <div className="space-y-3">
                        <div className="flex items-center gap-3">
                          <div className="h-6 bg-gray-200 rounded w-48"></div>
                          <div className="h-4 bg-gray-200 rounded w-16"></div>
                          <div className="h-4 bg-gray-200 rounded w-20"></div>
                        </div>
                        <div className="h-5 bg-gray-200 rounded w-3/4"></div>
                        <div className="h-4 bg-gray-200 rounded w-1/4"></div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </main>
          </div>
        </PageLayout>
      </React.Fragment>
    )
  }

  return (
    <React.Fragment>
      <PageLayout currentPage="exam" userRole="instructor">
        <div className="flex">
          <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
          <main className="flex-1 p-6">
            <div className="space-y-6">
              {/* 페이지 헤더 */}
              <div className="py-4">
                <h1 className="text-3xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                  내 시험 관리
                </h1>
                <p className="text-gray-600">출제한 시험을 관리하고 채점 현황을 확인할 수 있습니다.</p>
              </div>

              {/* 에러 메시지 */}
              {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <div className="flex items-center justify-between">
                    <p className="text-red-800">{error}</p>
                    <button
                      onClick={handleRefresh}
                      className="text-red-600 hover:text-red-800 underline"
                    >
                      다시 시도
                    </button>
                  </div>
                </div>
              )}

              {/* 통계 카드 */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                {stats.map((stat, index) => (
                  <StatsCard key={index} title={stat.title} value={stat.value} icon={stat.icon} color={stat.color} />
                ))}
              </div>

              {/* 검색 및 필터 */}
              <Card>
                <CardContent className="p-6">
                  <div className="flex flex-col md:flex-row gap-4">
                    <div className="flex-1">
                      <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                        <Input
                          placeholder="시험명, 과정명, 과정코드로 검색..."
                          value={searchTerm}
                          onChange={(e) => setSearchTerm(e.target.value)}
                          className="pl-10"
                        />
                      </div>
                    </div>
                    <div className="flex gap-4">
                      <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
                      >
                        {statuses.map((status) => (
                          <option key={status} value={status}>
                            {status === "all" ? "전체 상태" : status}
                          </option>
                        ))}
                      </select>
                      <select
                        value={courseFilter}
                        onChange={e => setCourseFilter(e.target.value)}
                        className="px-3 py-2 border border-gray-300 rounded-md w-42 focus:outline-none focus:ring-2 focus:ring-purple-500"
                      >
                        <option value="all">전체 과정</option>
                        {courses.filter(course => course !== "all").map(course => (
                          <option key={course.courseId} value={course.courseId}>
                            {course.courseName} ({course.courseCode})
                          </option>
                        ))}
                      </select>
                      <Link to="/instructor/exam/my-exams/create">
                        <Button className="text-[#1abc9c] hover:bg-[#1abc9c] hover:text-white bg-transparent border border-[#1abc9c]">
                          <Plus className="w-4 h-4 mr-2" />새 시험 만들기
                        </Button>
                      </Link>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* 시험 목록 */}
              <Card>
                <CardHeader>
                  <div className="flex justify-between items-center">
                    <CardTitle className="flex items-center gap-2" style={{ color: "#2C3E50" }}>
                      <FileText className="w-5 h-5" />
                      출제 시험 목록 ({filteredExams.length}개)
                    </CardTitle>
                    <div className="flex gap-2">
                      {/* <Button variant="outline" size="sm" className="bg-transparent">
                        <Download className="w-4 h-4 mr-2" />
                        결과 내보내기
                      </Button> */}
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {filteredExams.map((exam) => (
                      <div key={exam.id} className="border rounded-lg p-6 hover:bg-gray-50 transition-colors">
                        <div className="flex items-start justify-between mb-4">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                              <h3 className="text-lg font-semibold" style={{ color: "#2C3E50" }}>
                                {exam.title}
                              </h3>
                              <div className="flex gap-2">
                                <Button
                                  onClick={() => toggleExamStatus(exam.templateId, exam.status)}
                                  disabled={exam.status === "종료"}
                                  className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${
                                    exam.status === "예정"
                                      ? "bg-[#1abc9c] hover:bg-[#16a085] text-white"
                                      : exam.status === "진행중"
                                      ? "bg-[#b0c4de] hover:bg-[#3498db] text-white"
                                      : "bg-gray-400 text-white cursor-not-allowed"
                                  }`}
                                >
                                  {exam.status === "예정" 
                                    ? "시험 열기"
                                    : exam.status === "진행중"
                                    ? "시험 닫기"
                                    : "시험 종료"}
                                </Button>
                              </div>
                              <Badge className={getStatusColor(exam.status)}>{exam.status}</Badge>
                              <Badge className={getTypeColor(exam.type)}>{exam.type}</Badge>
                            </div>
                            <div className="flex items-center gap-4 text-sm text-gray-600 mb-3">
                              <span className="flex items-center gap-1">
                                <FileText className="w-4 h-4" />
                                {exam.course} ({exam.courseCode})
                              </span>
                              <span className="flex items-center gap-1">
                                <Clock className="w-4 h-4" />
                                {exam.duration}분
                              </span>
                              <span className="flex items-center gap-1">
                                <Users className="w-4 h-4" />
                                {exam.totalQuestions}문항
                              </span>
                            </div>
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                              <div>
                                <p className="text-gray-500">수강생 수</p>
                                <p className="font-medium" style={{ color: "#2C3E50" }}>
                                  {exam.participants}명
                                </p>
                              </div>
                              <div>
                                <p className="text-gray-500">제출률</p>
                                <div className="flex items-center gap-2">
                                  <p className="font-medium" style={{ color: "#2C3E50" }}>
                                    {exam.submitted}/{exam.participants}
                                  </p>
                                  <div className="flex-1 bg-gray-200 rounded-full h-2">
                                    <div
                                      className="bg-blue-600 h-2 rounded-full"
                                      style={{ width: `${parseInt(exam.submissionRate)}%` }}
                                    ></div>
                                  </div>
                                  <span className="text-xs text-gray-500">
                                    {exam.submissionRate}
                                  </span>
                                </div>
                              </div>
                              <div>
                                <p className="text-gray-500">채점률</p>
                                <div className="flex items-center gap-2">
                                  <p className="font-medium" style={{ color: "#2C3E50" }}>
                                    {exam.graded}/{exam.submitted}
                                  </p>
                                  <div className="flex-1 bg-gray-200 rounded-full h-2">
                                    <div
                                      className="bg-green-600 h-2 rounded-full"
                                      style={{ width: `${parseInt(exam.gradingRate)}%` }}
                                    ></div>
                                  </div>
                                  <span className="text-xs text-gray-500">
                                    {exam.gradingRate}
                                  </span>
                                </div>
                              </div>
                              <div>
                                <p className="text-gray-500">평균 점수</p>
                                <p className="font-medium" style={{ color: "#2C3E50" }}>
                                  {exam.avgScore > 0 ? `${exam.avgScore.toFixed(1)}점` : "-"}
                                </p>
                              </div>
                            </div>
                          </div>
                          <div className="flex flex-col gap-2 ml-6">
                            <div className="text-right text-sm">
                              <p className="text-gray-500">시험 기간</p>
                              <p className="font-medium" style={{ color: "#2C3E50" }}>
                                {exam.openDate === null && exam.closeDate === null 
                                  ? "날짜 미정" 
                                  : `${exam.openDate} ~ ${exam.closeDate}`}
                              </p>
                            </div>
                            <div className="flex gap-1 justify-end">
                              {exam.status === "예정" ? (
                                <>
                                  <Link to={`/instructor/exam/my-exams/detail/${exam.templateId}`}>
                                    <Button
                                      size="sm"
                                      className="text-[#1abc9c] hover:bg-green-50 hover:scale-110 bg-transparent"
                                    >
                                      <Eye className="w-4 h-4 mr-1" />
                                    </Button>
                                  </Link>
                                  <Button
                                    size="sm"
                                    className="text-red-600 hover:bg-red-50 hover:scale-110 bg-transparent"
                                    onClick={() => handleDeleteExam(exam)}
                                  >
                                    <Trash2 className="w-4 h-4" />
                                  </Button>
                                </>
                              ) : (
                                <Link to={`/instructor/exam/my-exams/detail/${exam.templateId}`}>
                                <Button
                                      size="sm"
                                      className="text-[#1abc9c] hover:bg-green-50 hover:scale-110 bg-transparent"
                                    >
                                      <Eye className="w-4 h-4 mr-1" />
                                    </Button>
                              </Link>
                              )}
                              {/* <Link to={`/instructor/exam/grading/${exam.id}`}>
                                <Button
                                  size="sm"
                                  variant="outline"
                                  className="text-green-600 border-green-600 hover:bg-green-50 bg-transparent"
                                >
                                  <Edit className="w-4 h-4 mr-1" />
                                  채점하기
                                </Button>
                              </Link> */}
                            </div>
                          </div>
                        </div>

                        <div className="border-t pt-3 mt-3">
                          <div className="flex items-center justify-between text-xs text-gray-500">
                            <span className="flex items-center gap-1">
                              <Calendar className="w-3 h-3" />
                              출제일: {exam.createdDate}
                            </span>
                            <div className="flex items-center gap-4">
                              <span
                                className={`font-medium ${
                                  (examStatus[exam.id] === "open" || (!examStatus[exam.id] && exam.status === "진행중")) && exam.status !== "예정"
                                    ? "text-green-600"
                                    : "text-red-600"
                                }`}
                              >
                                {(examStatus[exam.id] === "open" || (!examStatus[exam.id] && exam.status === "진행중")) && exam.status !== "예정"
                                  ? "학생 응시 가능"
                                  : "학생 응시 불가"}
                              </span>
                              {exam.waitingForGrading > 0 && (
                                <span className="text-orange-600 font-medium">
                                  채점 대기: {exam.waitingForGrading}건
                                </span>
                              )}
                              {exam.status === "진행중" && (
                                <span className="text-blue-600 font-medium">
                                  미제출: {Math.max(0, exam.participants - exam.submitted)}명
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {filteredExams.length === 0 && (
                    <div className="text-center py-12">
                      <FileText className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                      <h3 className="text-lg font-medium text-gray-900 mb-2">검색 결과가 없습니다</h3>
                      <p className="text-gray-500">다른 검색어나 필터를 사용해보세요.</p>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* 채점 대기 목록 */}
              {/* <Card>
                <CardHeader>
                  <div className="flex justify-between items-center">
                    <CardTitle className="flex items-center gap-2" style={{ color: "#2C3E50" }}>
                      <Edit className="w-5 h-5" />
                      채점 대기 목록 ({gradingQueue.filter(item => item.status === "채점 대기").length}건)
                    </CardTitle>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm" className="bg-transparent">
                        <Download className="w-4 h-4 mr-2" />
                        목록 내보내기
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {gradingQueue.map((item) => (
                      <div key={item.id} className="border rounded-lg p-4 hover:bg-gray-50 transition-colors">
                        <div className="flex items-center justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                              <h4 className="font-semibold text-gray-900">{item.studentName}</h4>
                              <Badge className={getGradingStatusColor(item.status)}>{item.status}</Badge>
                            </div>
                            <div className="text-sm text-gray-600 mb-1">
                              {item.assignmentTitle}
                            </div>
                            <div className="text-xs text-gray-500">
                              제출일: {item.submissionDate}
                            </div>
                          </div>
                          <div className="flex gap-2">
                            {item.status === "채점 대기" ? (
                              <Link to={`/instructor/exam/grading/${item.templateId}`}>
                                <Button size="sm" className="bg-green-600 hover:bg-green-700">
                                  <Edit className="w-4 h-4 mr-2" />
                                  채점하기
                                </Button>
                              </Link>
                            ) : (
                              <Link to={`/instructor/exam/grading/${item.templateId}`}>
                                <Button size="sm" variant="outline" className="bg-transparent">
                                  <CheckCircle className="w-4 h-4 mr-2" />
                                  채점 완료
                                </Button>
                              </Link>
                            )}
                            <Link to={`/instructor/exam/my-exams/${item.templateId}`}>
                              <Button size="sm" variant="outline" className="bg-transparent">
                                <Eye className="w-4 h-4 mr-2" />
                                보기
                              </Button>
                            </Link>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {gradingQueue.filter(item => item.status === "채점 대기").length === 0 && (
                    <div className="text-center py-8">
                      <CheckCircle className="w-12 h-12 text-green-400 mx-auto mb-4" />
                      <h3 className="text-lg font-medium text-gray-900 mb-2">채점 대기 항목이 없습니다</h3>
                      <p className="text-gray-500">모든 제출물이 채점 완료되었습니다.</p>
                    </div>
                  )}
                </CardContent>
              </Card> */}

              {/* 빠른 작업 안내 */}
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50" }}>시험 관리 안내</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-1 gap-4 text-sm">
                    <div className="p-4 bg-blue-50 rounded-lggrid grid-cols-1 md:grid-cols-3">
                      <div>

                      <h4 className="font-medium text-blue-800 mb-2">시험 생성</h4>
                      <p className="text-blue-600">새로운 시험을 만들고 문제를 출제할 수 있습니다.</p>
                      </div>
                      <div>

                      <h4 className="font-medium text-blue-800 mb-2">채점 관리</h4>
                      <p className="text-blue-600">제출된 답안을 채점하고 피드백을 제공할 수 있습니다.</p>
                      </div>
                      <div>

                      <h4 className="font-medium text-blue-800 mb-2">결과 분석</h4>
                      <p className="text-blue-600">시험 결과를 분석하고 통계를 확인할 수 있습니다.</p>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </main>
        </div>
      </PageLayout>

      {/* 시험 삭제 확인 모달 */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 w-96 max-w-md mx-4">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
                <X className="w-6 h-6 text-red-600" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-900">시험 삭제</h3>
                <p className="text-sm text-gray-500">이 작업은 되돌릴 수 없습니다.</p>
              </div>
            </div>
            
            <div className="mb-6">
              <p className="text-gray-700 mb-2">
                <strong>"{examToDelete?.title}"</strong> 시험을 삭제하시겠습니까?
              </p>
              <p className="text-sm text-gray-500">
                시험을 삭제하면 해당 시험과 관련된 모든 데이터가 비활성화됩니다.
              </p>
            </div>
            
            <div className="flex justify-end gap-3">
              <Button 
                variant="outline" 
                onClick={cancelDeleteExam}
                disabled={isDeleting}
                className="bg-transparent"
              >
                취소
              </Button>
              <Button 
                onClick={confirmDeleteExam}
                disabled={isDeleting}
                className="bg-red-600 hover:bg-red-700 text-white"
              >
                {isDeleting ? "삭제 중..." : "삭제"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </React.Fragment>
  )
}
