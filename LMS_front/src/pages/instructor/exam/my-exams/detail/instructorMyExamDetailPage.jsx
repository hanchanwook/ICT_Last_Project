import React, { useState, useEffect, useCallback, useMemo } from "react"
import { useParams, useLocation, useNavigate } from "react-router-dom"
import StatsCard from "@/components/ui/stats-card"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  FileText,
  Users,
  TrendingUp,
  ArrowLeft,
  Download,
  Edit,
  Eye,
  CheckCircle,
  Calendar,
  BookOpen,
  X,
  Play,
  Square,
  RefreshCw,
  RotateCcw,
  Trash2,
} from "lucide-react"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { getMenuItems } from "@/components/ui/menuConfig"
import { deactivateExamTemplate, getExamTemplateDetail, openExam, closeExamWithAutoSubmission, updateExamTemplate } from "@/api/kayoung/templateApi"
import { getCourseStudents, getExamSubmissions, getStudentExamAnswers, submitGrading } from "@/api/kayoung/studentExamApi"

export default function InstructorExamMyExamDetailPage() {
  const { id: templateId } = useParams()
  const navigate = useNavigate()
  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [isGradingModalOpen, setIsGradingModalOpen] = useState(false)
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [isEditModalOpen, setIsEditModalOpen] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState(null)

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  
  // 실제 데이터 상태
  const [templateDetail, setTemplateDetail] = useState(null)
  const [studentResults, setStudentResults] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  
  // 채점 모달 상태
  const [gradingScores, setGradingScores] = useState({})
  const [gradingComments, setGradingComments] = useState({})
  const [overallFeedback, setOverallFeedback] = useState("")
  

  
  // 자동 새로고침 상태
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [lastRefreshTime, setLastRefreshTime] = useState(new Date())
  const [refreshInterval, setRefreshInterval] = useState(30000) // 30초

  const location = useLocation()
  const sidebarMenuItems = getMenuItems('instructor-exam')
  const currentPath = location.pathname

  // 날짜 포맷팅 함수
  const formatSubmittedDate = (dateString) => {
    if (!dateString) return ""
    
    try {
      // 이미 한국 시간으로 오므로 그대로 사용
      const date = new Date(dateString)
      
      // YYYY-MM-DD HH:MM 형식으로 포맷팅
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      
      return `${year}-${month}-${day} ${hours}:${minutes}`
    } catch (error) {
      return dateString || ""
    }
  }



  // 과정별 학생 조회 함수 (subGroupId 사용)
  const loadCourseStudents = async (subGroupId) => {
    try {
      const response = await getCourseStudents(subGroupId)
      const studentsData = response?.data || response || []
      return studentsData
    } catch (error) {
      return []
    }
  }

  // 시험별 답안 제출 현황 조회 함수
  const loadExamSubmissions = async (templateId) => {
    try {
      const response = await getExamSubmissions(templateId)
      const submissionsData = response?.data || response || []
      return submissionsData
    } catch (error) {
      return []
    }
  }



  // 채점 제출 API는 studentExamApi.js에서 import하여 사용

  // 데이터 로딩 함수
  const loadTemplateData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      // 시험 템플릿 상세 정보 로드
      const templateResponse = await getExamTemplateDetail(templateId)
      
      // 응답 데이터 처리
      const templateData = templateResponse?.data || templateResponse
      
      if (!templateData) {
        throw new Error(`시험 템플릿을 찾을 수 없습니다. (templateId: ${templateId})`)
      }
      
      // 서브그룹 ID 추출 (API에서 subGroupId를 사용해야 함)
      // subGroupId가 없으면 courseId를 사용 (임시 해결책)
      const subGroupId = templateData.subGroupId || templateData.courseId
      
      // 과정별 학생 리스트와 시험별 제출 현황을 병렬로 로드
      // getExamScores API는 제거하고 getExamSubmissions에서 모든 정보를 가져옴
      const [courseStudents, examSubmissions] = await Promise.all([
        loadCourseStudents(subGroupId),
        loadExamSubmissions(templateId)
      ])
      
      // 새로운 API 응답 구조 활용
      // examSubmissions에서 학생별 제출 현황, 점수, 상태, 등급 등 모든 정보를 가져옴
      const studentsData = courseStudents.map(student => {
        // examSubmissions에서 해당 학생의 데이터 찾기
        const submission = examSubmissions.find(sub => 
          sub.memberId === student.memberId || sub.memberEmail === student.email
        )
        
        // 새로운 API 응답 구조에서 정보 추출
        const finalScore = submission?.answerScore || null
        const finalGraded = submission?.status === "채점완료"
        const finalIsChecked = submission?.isChecked || 0
        const finalStatus = submission?.status || "미제출"
        const finalGrade = submission?.grade || null
        
        return {
          id: student.studentId || student.memberId,
          studentId: student.studentId,
          memberId: student.memberId,
          name: student.name || student.studentName,
          email: student.email,
          submittedAt: submission?.submittedAt || null,
          graded: finalGraded,
          score: finalScore,
          isChecked: finalIsChecked,
          status: finalStatus,
          grade: finalGrade,
          timeSpent: submission?.timeSpent || null,
          answeredQuestions: submission?.answeredQuestions || 0,
          correctAnswers: submission?.correctAnswers || 0
        }
      })
      
      // 과정 정보를 templateData에서 직접 가져오기
      const courseInfo = {
        courseName: templateData.courseName || "과정명",
        courseCode: templateData.courseCode || "COURSE001"
      }
      
      // 문제 상세 정보를 templateData에서 직접 가져오기 (API 호출 제거)
      const questionDetails = templateData.questions ? templateData.questions.map(q => ({
        id: q.questionId || q.id,
        type: q.questionType || "객관식",
        question: q.questionText || q.question || `문제 ${q.questionId || q.id}`,
        points: q.templateQuestionScore || q.questionScore || 0,
        options: q.options || [],
        correctAnswer: q.questionAnswer || "",
        explanation: q.explanation || ""
      })) : []
      
      // 백엔드 응답 구조를 프론트엔드에서 사용하는 구조로 변환
      const transformedTemplateData = {
        id: templateData.templateId,
        title: templateData.templateName || "제목 없음",
        courseId: templateData.courseId, // 과정 ID 추가
        course: courseInfo.courseName,
        courseCode: courseInfo.courseCode,
        type: "", // 시험 유형은 제거 (퀴즈 고정값 제거)
        status: (() => {
          // templateActive가 0이 아니면 모든 상태가 무효
          // if (templateData.templateActive !== 0) {
          //   return "비활성화"
          // }
          
          // templateOpen과 templateClose가 모두 null인 경우
          if (templateData.templateOpen === null && templateData.templateClose === null) {
            return "예정"
          }
          
          // templateOpen만 있고 templateClose가 null인 경우 (시험 열린 상태)
          if (templateData.templateOpen && templateData.templateClose === null) {
            return "진행중"
          }
          
          // 시험 기간이 설정된 경우
          if (templateData.templateOpen && templateData.templateClose) {
            const today = new Date()
            const openDate = new Date(templateData.templateOpen)
            const closeDate = new Date(templateData.templateClose)
            
            
            // 오늘 날짜가 오픈 날짜보다 이후이고 닫은 날짜보다 이전인 경우
            if (today >= openDate && today <= closeDate) {
              return "진행중"
            }
            // 오늘 날짜가 닫은 날짜보다 이후인 경우
            else if (today > closeDate) {
              return "완료"
            }
            // 오늘 날짜가 오픈 날짜보다 이전인 경우
            else {
              return "예정"
            }
          }
          
          return "예정"
        })(),
        duration: templateData.templateTime || 60,
        maxScore: 100, // 총점 100점으로 고정 (myExamsPage와 동일)
        passingScore: 60, // 60점 이상 합격 (myExamsPage와 동일)
        startDate: templateData.templateOpen ? new Date(new Date(templateData.templateOpen).getTime() + 9 * 60 * 60 * 1000).toLocaleString('ko-KR', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        }) : "날짜 미정",
        endDate: templateData.templateClose ? new Date(new Date(templateData.templateClose).getTime() + 9 * 60 * 60 * 1000).toLocaleString('ko-KR', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        }) : "날짜 미정",
        endDateForComparison: templateData.templateClose ? new Date(new Date(templateData.templateClose).getTime() + 9 * 60 * 60 * 1000) : null,
        createdDate: templateData.createdAt ? new Date(templateData.createdAt).toLocaleDateString('ko-KR', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        }) : "날짜 미정",
        // 새로운 API 응답 구조에 맞춰 필드 업데이트
        avgScore: templateData.averageScore || 0,
        participants: templateData.totalStudents || templateData.participantCount || studentsData.length,
        submitted: templateData.submittedCount || studentsData.filter(s => s.submittedAt).length,
        graded: templateData.gradedCount || studentsData.filter(s => s.graded).length,
        waitingForGrading: templateData.waitingForGradingCount || 0,
        submissionRate: templateData.submissionRate || "0%",
        gradingRate: templateData.gradingRate || "0%",
        totalQuestions: templateData.totalQuestions || templateData.questions?.length || 0,

        questions: questionDetails
      }
      
      setTemplateDetail(transformedTemplateData)
      setStudentResults(studentsData)
      
    } catch (err) {
      setError(`시험 템플릿 데이터를 불러오는 데 실패했습니다: ${err.message}`)
    } finally {
      setLoading(false)
    }
  }
  
  // loadTemplateData를 useCallback으로 메모이제이션
  const loadTemplateDataCallback = useCallback(async () => {
    if (templateId) {
      await loadTemplateData()
      setLastRefreshTime(new Date())
    }
  }, [templateId])

  // 컴포넌트 마운트 시 데이터 로드
  useEffect(() => {
    // 강사 과정 정보 로딩 (현재 사용되지 않음)
    // loadInstructorCourses()
    
    // 시험 템플릿 데이터 로딩
    loadTemplateDataCallback()
  }, [loadTemplateDataCallback])

  // 디버깅용: templateDetail이 로드되면 전체 상태 로깅
  useEffect(() => {
    if (templateDetail && process.env.NODE_ENV === 'development') {
      const now = new Date()
      const koreanTime = new Date(now.getTime() + (9 * 60 * 60 * 1000))
      
      studentResults.forEach((student, index) => {
      })
    }
  }, [templateDetail, studentResults])

  // 자동 새로고침 설정
  useEffect(() => {
    if (!autoRefresh || !templateDetail) return

    const interval = setInterval(() => {
      loadTemplateDataCallback()
    }, refreshInterval)

    return () => clearInterval(interval)
  }, [autoRefresh, refreshInterval, loadTemplateDataCallback, templateDetail])

  // 수동 새로고침 함수
  const handleManualRefresh = async () => {
    try {
      await loadTemplateDataCallback()
    } catch (error) {
      console.error('수동 새로고침 실패:', error)
    }
  }



  const filteredResults = templateDetail ? studentResults.filter((result) => {
    const matchesSearch =
      result.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      result.studentId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      result.email.toLowerCase().includes(searchTerm.toLowerCase())
    
    // getStudentStatus 함수가 안전하게 호출될 수 있도록 try-catch 사용
    let studentStatus = "미제출"
    try {
      studentStatus = getStudentStatus(result)
    } catch (error) {
      studentStatus = "미제출"
    }
    
    const matchesStatus = statusFilter === "all" || studentStatus === statusFilter
    return matchesSearch && matchesStatus
  }) : []

  const getStatusColor = (status) => {
    switch (status) {
      case "채점완료":
        return "bg-green-100 text-green-800"
      case "채점대기":
        return "bg-yellow-100 text-yellow-800"
      case "제출완료":
        return "bg-blue-100 text-blue-800"
      case "미제출":
        return "bg-red-100 text-red-800"
      case "진행중":
        return "bg-blue-100 text-blue-800"
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

  const getScoreColor = (score, maxScore) => {
    if (!score) return "text-gray-500"
    const percentage = (score / maxScore) * 100
    if (percentage >= 90) return "text-green-600 font-bold"
    if (percentage >= 80) return "text-blue-600 font-semibold"
    if (percentage >= 70) return "text-orange-600"
    return "text-red-600"
  }

  const getProgressPercentage = (submitted, total) => {
    return total > 0 ? Math.round((submitted / total) * 100) : 0
  }

  const getGradingPercentage = (graded, submitted) => {
    return submitted > 0 ? Math.round((graded / submitted) * 100) : 0
  }



  // 총점 계산 함수
  const calculateTotalScore = () => {
    if (!templateDetail?.questions || !selectedStudent) return 0
    
    return templateDetail.questions.reduce((total, question) => {
      if (question.type === "객관식") {
        // 객관식은 자동 채점
        const studentAnswer = selectedStudent.answers && selectedStudent.answers[question.id]
        const correctAnswer = question.correctAnswer
        
        // 답안 데이터 정규화 (공백 제거, 문자열 변환)
        const normalizedStudentAnswer = studentAnswer ? String(studentAnswer).trim() : null
        const normalizedCorrectAnswer = correctAnswer ? String(correctAnswer).trim() : null
        
        
        const isCorrect = normalizedStudentAnswer === normalizedCorrectAnswer
        return total + (isCorrect ? question.points : 0)
      } else {
        // 서술형/코드형은 수동 채점
        const score = gradingScores[question.id] || 0
        return total + score
      }
    }, 0)
  }

  const handleGradeStudent = async (student) => {
    try {
      // 학생 답안 상세 조회
      const answersResponse = await getStudentExamAnswers(templateId, student.memberId)
      const answersData = answersResponse?.data || answersResponse || {}
      
      // 학생 정보에 답안 데이터 추가
      const studentWithAnswers = {
        ...student,
        answers: answersData.answers || answersData || {}
      }
      
      setSelectedStudent(studentWithAnswers)
      setIsGradingModalOpen(true)
      
      // 채점 상태 초기화
      setGradingScores({})
      setGradingComments({})
      setOverallFeedback("")
      
    } catch (error) {
      console.error('학생 답안 조회 에러:', error)
      alert('학생 답안을 불러오는데 실패했습니다.')
    }
  }

  // 점수 변경 핸들러
  const handleScoreChange = (questionId, score) => {
    // 해당 문제의 배점 찾기
    const question = templateDetail.questions.find(q => q.id === questionId)
    const maxPoints = question ? question.points : 0
    
    // 입력값을 숫자로 변환
    let numericScore = parseInt(score) || 0
    
    // 배점을 넘지 못하도록 제한
    if (numericScore > maxPoints) {
      numericScore = maxPoints
    }
    
    // 음수가 되지 않도록 제한
    if (numericScore < 0) {
      numericScore = 0
    }
    
    setGradingScores(prev => ({
      ...prev,
      [questionId]: numericScore
    }))
  }

  // 코멘트 변경 핸들러
  const handleCommentChange = (questionId, comment) => {
    setGradingComments(prev => ({
      ...prev,
      [questionId]: comment
    }))
  }

  const handleGradingSubmit = async () => {
    try {
      // 이미 채점이 완료되었는지 확인
      if (selectedStudent.graded) {
        alert('이미 채점이 완료된 학생입니다.')
        return
      }
      
      const totalScore = calculateTotalScore()
      
      // 백엔드로 전송할 데이터 구조
      // isChecked 필드 의미 (백엔드 요구사항):
      // - 0: 채점 미완료
      // - 1: 채점 완료
      const gradingData = {
        memberId: selectedStudent.memberId,
        templateId: templateId,
        score: totalScore,
                 isChecked: 1, // 채점 완료 (강사가 채점 완료, 학생 미확인)
        totalComment: overallFeedback, // 백엔드에서 요구하는 필드명
        questionDetails: templateDetail.questions.map(question => {
          let questionScore = 0
          let isCorrect = false
          
          if (question.type === "객관식") {
            // 객관식은 자동 채점
            const studentAnswer = selectedStudent.answers && selectedStudent.answers[question.id]
            const correctAnswer = question.correctAnswer
            
            // 답안 데이터 정규화 (공백 제거, 문자열 변환)
            const normalizedStudentAnswer = studentAnswer ? String(studentAnswer).trim() : null
            const normalizedCorrectAnswer = correctAnswer ? String(correctAnswer).trim() : null
            
            isCorrect = normalizedStudentAnswer === normalizedCorrectAnswer
            questionScore = isCorrect ? question.points : 0
          } else {
            // 서술형/코드형은 수동 채점
            questionScore = gradingScores[question.id] || 0
            isCorrect = questionScore === question.points // 만점이면 정답으로 간주
          }
          
          return {
            questionId: question.id,
            score: questionScore,
            comment: gradingComments[question.id] || "",
            isCorrect: isCorrect
          }
        })
      }
      
      // 백엔드 API 호출
      const result = await submitGrading(gradingData)
      
      // 성공 확인
      if (!result) {
        throw new Error('채점 제출 결과가 없습니다.')
      }
      
      setIsGradingModalOpen(false)
      setSelectedStudent(null)
      
      // 데이터 새로고침
      await loadTemplateDataCallback()
      
      alert('채점이 성공적으로 완료되었습니다!')
    } catch (error) {
      // 에러 메시지 처리
      let errorMessage = '채점 제출에 실패했습니다.'
      
      if (error.response) {
        // 서버 응답이 있는 경우
        const status = error.response.status
        const data = error.response.data
        
        switch (status) {
          case 400:
            errorMessage = data.message || '필수 정보가 누락되었습니다.'
            break
          case 404:
            errorMessage = data.message || '학생 또는 시험 정보를 찾을 수 없습니다.'
            break
          case 409:
            errorMessage = data.message || '이미 채점이 완료된 시험입니다.'
            break
          case 500:
            errorMessage = data.message || '서버 오류가 발생했습니다.'
            break
          default:
            errorMessage = data.message || '채점 제출에 실패했습니다.'
        }
      } else if (error.message) {
        errorMessage = error.message
      }
      
      alert(errorMessage)
    }
  }

  const handleEditTemplate = () => {
    // 날짜와 시간을 HTML datetime-local input 형식으로 변환
    const formatDateTimeForInput = (dateString) => {
      if (dateString === "날짜 미정") return ""
      
      // 한국 시간 형식에서 날짜와 시간 추출 (예: "2025. 08. 05. 오후 12:55")
      const match = dateString.match(/(\d{4})\.\s*(\d{1,2})\.\s*(\d{1,2})\.\s*(오전|오후)\s*(\d{1,2}):(\d{1,2})/)
      if (match) {
        const year = match[1]
        const month = match[2].padStart(2, '0')
        const day = match[3].padStart(2, '0')
        const ampm = match[4]
        let hour = parseInt(match[5])
        const minute = match[6].padStart(2, '0')
        
        // 오후인 경우 12를 더함 (단, 오후 12시는 12로 유지)
        if (ampm === "오후" && hour !== 12) {
          hour += 12
        }
        // 오전 12시는 0시로 변환
        if (ampm === "오전" && hour === 12) {
          hour = 0
        }
        
        const hourStr = hour.toString().padStart(2, '0')
        return `${year}-${month}-${day}T${hourStr}:${minute}`
      }
      return ""
    }
    
    setEditingTemplate({
      title: templateDetail.title,
      course: templateDetail.course,
      duration: templateDetail.duration,
      maxScore: templateDetail.maxScore,
      passingScore: templateDetail.passingScore,
      startDate: formatDateTimeForInput(templateDetail.startDate),
      endDate: formatDateTimeForInput(templateDetail.endDate),
    })
    setIsEditModalOpen(true)
  }

  const handleEditSubmit = async () => {
    try {
      // 필수 필드 검증
      if (!editingTemplate.title?.trim()) {
        alert('시험 제목을 입력해주세요.')
        return
      }
      
      // 시험 템플릿 기본 정보 수정
      const templateUpdateData = {
        templateName: editingTemplate.title,
        templateTime: editingTemplate.duration,
        templateOpen: editingTemplate.startDate ? new Date(editingTemplate.startDate).toISOString() : null,
        templateClose: editingTemplate.endDate ? new Date(editingTemplate.endDate).toISOString() : null
      }
      
      await updateExamTemplate(templateId, templateUpdateData)
      
      // 데이터 새로고침
      await loadTemplateDataCallback()
      
      setIsEditModalOpen(false)
      setEditingTemplate(null)
      
      alert('시험이 성공적으로 수정되었습니다.')
    } catch (error) {
      alert('시험 수정에 실패했습니다.')
    }
  }

  const handleEditCancel = () => {
    setIsEditModalOpen(false)
    setEditingTemplate(null)
  }

  const handleExamFieldChange = (field, value) => {
    setEditingTemplate((prev) => ({
      ...prev,
      [field]: value,
    }))
  }



  const handleDeleteTemplate = () => {
    setIsDeleteModalOpen(true)
  }

  const confirmDeleteTemplate = async () => {
    try {
      setIsDeleting(true)
      
      await deactivateExamTemplate(templateId)
      
      // 시험 목록 페이지로 이동
      navigate('/instructor/exam/my-exams')
    } catch (error) {
      alert('시험 템플릿 삭제에 실패했습니다.')
    } finally {
      setIsDeleting(false)
      setIsDeleteModalOpen(false)
    }
  }

  const cancelDeleteExam = () => {
    setIsDeleteModalOpen(false)
  }

  // 시험 열기 함수
  const handleOpenExam = async () => {
    try {
      await openExam(templateId)
      
      // 데이터 새로고침
      await loadTemplateDataCallback()
      
      alert('시험이 열렸습니다.')
    } catch (error) {
      alert('시험 열기에 실패했습니다.')
    }
  }

  // 시험 닫기 함수
  const handleCloseExam = async () => {
    try {
      // 과정의 모든 학생들의 memberId 수집
      // subGroupId가 없으면 courseId를 사용 (임시 해결책)
      const subGroupId = templateDetail?.subGroupId || templateDetail?.courseId
      
      if (!subGroupId) {
        throw new Error('서브그룹 ID 또는 과정 ID를 찾을 수 없습니다.')
      }

      const courseStudents = await loadCourseStudents(subGroupId)
      
      const studentMemberIds = courseStudents.map(student => student.memberId)

      // 미제출 학생 수 계산
      
      const unsubmittedStudents = studentResults.filter(student => {
        const status = getStudentStatus(student)
        return !student.submittedAt && status === "미제출"
      })
      const unsubmittedCount = unsubmittedStudents.length

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
      await loadTemplateDataCallback()
      
      // 성공 메시지 (미제출 학생 수 포함)
      if (unsubmittedCount > 0) {
        alert(`시험이 닫혔습니다.\n\n미제출 학생 ${unsubmittedCount}명이 있습니다.\n\n백엔드에서 미제출 학생 자동 처리 기능이 구현되면 0점으로 처리됩니다.`)
      } else {
        alert('시험이 닫혔습니다.')
      }
    } catch (error) {
      alert('시험 닫기에 실패했습니다.')
    }
  }

  // 시험 종료 여부 계산 (한국 시간 기준)
  const isExamEnded = useMemo(() => {
    if (!templateDetail?.endDateForComparison) {
      return false // 종료일이 없으면 진행 중으로 간주
    }
    
    // 한국 시간으로 현재 시간 계산 (UTC+9)
    const now = new Date()
    const koreanTime = new Date(now.getTime() + (9 * 60 * 60 * 1000))
    const endDate = templateDetail.endDateForComparison
    
    
    return koreanTime >= endDate
  }, [templateDetail?.endDateForComparison])

  // 로딩 중이거나 에러 상태일 때 표시할 내용
  if (loading) {
    return (
      <React.Fragment>
        <PageLayout currentPage="exam" userRole="instructor">
          <div className="flex">
            <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="animate-pulse space-y-6">
                <div className="h-8 bg-gray-200 rounded w-1/4"></div>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                  {[...Array(4)].map((_, i) => (
                    <div key={i} className="bg-white p-4 rounded-lg shadow-sm">
                      <div className="space-y-2">
                        <div className="h-4 bg-gray-200 rounded w-20"></div>
                        <div className="h-6 bg-gray-200 rounded w-12"></div>
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

  // 학생별 상태 계산 함수
  const getStudentStatus = (student) => {
    // API에서 받은 status가 "채점필요"인 경우 우선 처리
    if (student.status === "채점필요") {
      return "채점필요"
    }
    
    // 답안 제출 여부 확인
    const hasSubmission = student.submittedAt !== null && student.submittedAt !== undefined
    
    // 시험이 종료된 경우: 채점 완료 또는 채점 대기만 표시 (API status 무시)
    if (isExamEnded) {
      const finalStatus = student.graded ? "채점완료" : "채점대기"
      
      return finalStatus
    }
    
    // 시험이 진행 중인 경우: API status 우선 사용, 없으면 기존 로직
    if (student.status && student.status !== "미제출") {
      return student.status
    }
    
    // API status가 없거나 "미제출"인 경우 기존 로직 사용
    if (!hasSubmission) {
      return "미제출"  // 시험이 진행 중인 미제출 상태
    } else if (student.graded) {
      return "채점완료"  // 채점이 완료된 경우
    } else {
      return "제출완료"  // 답안은 있지만 채점이 안된 경우 (종료일이 안 지남)
    }
  }

  if (error) {
    return (
      <React.Fragment>
        <PageLayout currentPage="exam" userRole="instructor">
          <div className="flex">
            <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                    <X className="w-5 h-5 text-red-600" />
                  </div>
                  <h3 className="text-lg font-semibold text-red-800">데이터 로드 오류</h3>
                </div>
                <p className="text-red-700 mb-3">시험 정보를 불러오는 중 오류가 발생했습니다.</p>
                <p className="text-red-600 text-sm mb-4">{error}</p>
                <div className="flex gap-3">
                  <Button 
                    onClick={() => window.location.reload()} 
                    className="bg-red-600 hover:bg-red-700"
                  >
                    페이지 새로고침
                  </Button>
                  <Button 
                    variant="outline" 
                    onClick={() => navigate('/instructor/exam/my-exams')}
                    className="bg-transparent"
                  >
                    목록으로 돌아가기
                  </Button>
                </div>
              </div>
            </main>
          </div>
        </PageLayout>
      </React.Fragment>
    )
  }

  if (!templateDetail) {
    return (
      <React.Fragment>
        <PageLayout currentPage="exam" userRole="instructor">
          <div className="flex">
            <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <p className="text-yellow-800">시험 템플릿 정보를 찾을 수 없습니다.</p>
              </div>
            </main>
          </div>
        </PageLayout>
      </React.Fragment>
    )
  }

  // 통계 계산 (학생 데이터에서 계산)
  const submittedCount = studentResults.filter(s => s.submittedAt).length
  const gradedCount = studentResults.filter(s => s.graded).length
  const avgScore = (() => {
    // 학생 점수의 합 / 수강생 수로 평균 점수 계산
    const totalScore = studentResults.reduce((sum, s) => sum + (s.score || 0), 0)
    return studentResults.length > 0 
      ? (totalScore / studentResults.length).toFixed(1)
      : 0
  })()

    const stats = [
    {
      title: "총 참여자",
      value: `${templateDetail.participants || studentResults.length}명`,
      icon: Users,
      color: "#3498db",
    },
    {
      title: "제출 완료",
      value: `${templateDetail.submitted || submittedCount}명`,
      icon: CheckCircle,
      color: "#1abc9c",
    },
    {
      title: "채점 대기",
      value: `${templateDetail.waitingForGrading || studentResults.filter(s => getStudentStatus(s) === "채점대기" || getStudentStatus(s) === "채점필요").length}명`,
      icon: Edit,
      color: "#b0c4de",
    },
           {
         title: "학생 미확인",
         value: `${studentResults.filter(s => s.graded && s.isChecked === 1).length}명`,
         icon: Eye,
         color: "#e67e22",
       },
    {
      title: "평균 점수",
      value: `${templateDetail.avgScore > 0 ? templateDetail.avgScore.toFixed(1) : avgScore}점`,
      icon: TrendingUp,
      color: "#2980b9",
    },
  ]
  


  return (
    <React.Fragment>
      <PageLayout currentPage="exam" userRole="instructor">
      <div className="flex">
          <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
        <main className="flex-1 p-6">
          <div className="space-y-6">
            {/* 페이지 헤더 */}
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-4">
                <Button 
                  variant="outline" 
                  size="sm" 
                  className="bg-transparent"
                  onClick={() => navigate('/instructor/exam/my-exams')}
                >
                  <ArrowLeft className="w-4 h-4 mr-2" />
                  목록으로
                </Button>
                <div>
                  <h1 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>
                    {templateDetail.title}
                  </h1>
                  <p className="text-gray-600">
                    {templateDetail.course} ({templateDetail.courseCode})
                  </p>
                </div>
              </div>
              
              {/* 새로고침 컨트롤 */}
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2 text-sm text-gray-600">
                  <input
                    type="checkbox"
                    id="autoRefresh"
                    checked={autoRefresh}
                    onChange={(e) => setAutoRefresh(e.target.checked)}
                    className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                  />
                  <label htmlFor="autoRefresh">자동 새로고침</label>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleManualRefresh}
                  className="bg-transparent"
                  title="수동 새로고침"
                >
                  <RefreshCw className="w-4 h-4" />
                </Button>
                <div className="text-xs text-gray-500">
                  마지막 업데이트: {lastRefreshTime.toLocaleTimeString()}
                </div>
              </div>
            </div>

            {/* 통계 카드 */}
            <div className="grid grid-cols-5 gap-4">
              {stats.map((stat, index) => (
                <StatsCard key={index} title={stat.title} value={stat.value} icon={stat.icon} color={stat.color} />
              ))}
            </div>

             

            {/* 시험 기본 정보 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2" style={{ color: "#2C3E50" }}>
                    <FileText className="w-5 h-5" />
                    시험 정보
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm text-gray-500">시험 상태</p>
                      <Badge className={getStatusColor(templateDetail.status)}>{templateDetail.status}</Badge>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">문항 수</p>
                      <p className="font-medium" style={{ color: "#2C3E50" }}>
                        {templateDetail.totalQuestions}문항
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">시험 시간</p>
                      <p className="font-medium" style={{ color: "#2C3E50" }}>
                        {templateDetail.duration}분
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">만점</p>
                      <p className="font-medium" style={{ color: "#2C3E50" }}>
                        {templateDetail.maxScore}점
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">합격 점수</p>
                      <p className="font-medium" style={{ color: "#2C3E50" }}>
                        {templateDetail.passingScore}점
                      </p>
                    </div>
                  </div>

                  <div className="flex gap-2 pt-4">
                    {templateDetail.status === "예정" ? (
                      <>
                        <Button 
                          size="sm" 
                          variant="outline" 
                          className="text-[#95a5a6] border-[#95a5a6] hover:bg-blue-50 hover:text-[#95a5a6] bg-transparent"
                          onClick={handleEditTemplate}
                        >
                          <Edit className="w-4 h-4 mr-1" />
                          수정
                        </Button>
                        <Button 
                          size="sm" 
                          variant="outline" 
                          className="text-red-600 border-red-600 hover:bg-red-50 hover:text-red-600 bg-transparent"
                          onClick={handleDeleteTemplate}
                        >
                          <Trash2 className="w-4 h-4 mr-1" />
                          삭제
                        </Button>
                      </>
                    ) : (
                      <div className="text-sm text-gray-500 italic">
                        {templateDetail.status === "진행중" ? "진행 중인 시험은 수정할 수 없습니다." : 
                         templateDetail.status === "완료" ? "완료된 시험은 수정할 수 없습니다." : 
                         "시험을 수정할 수 없습니다."}
                  </div>
                    )}
                  </div>

                  {/* 시험 열기/닫기 버튼 */}
                  <div className="flex gap-2 pt-4 border-t">
                    {templateDetail.status === "예정" && (
                      <Button 
                        size="sm" 
                        className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                        onClick={handleOpenExam}
                      >
                        <Play className="w-4 h-4 mr-2" />
                        시험 열기
                      </Button>
                    )}
                    {templateDetail.status === "진행중" && (
                      <Button 
                        size="sm" 
                        className="text-gray-700 border border-gray-700 hover:bg-gray-200"
                        onClick={handleCloseExam}
                      >
                        시험 닫기
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2" style={{ color: "#2C3E50" }}>
                    <Calendar className="w-5 h-5" />
                    일정 및 현황
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm text-gray-500">시작일</p>
                      <p className="font-medium" style={{ color: "#2C3E50" }}>
                        {templateDetail.startDate}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">종료일</p>
                      <p className="font-medium" style={{ color: "#2C3E50" }}>
                        {templateDetail.endDate}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">출제일</p>
                      <p className="font-medium" style={{ color: "#2C3E50" }}>
                        {templateDetail.createdDate}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">평균 점수</p>
                      <p className="font-medium" style={{ color: "#2C3E50" }}>
                        {templateDetail.avgScore > 0 ? templateDetail.avgScore.toFixed(1) : avgScore}점
                      </p>
                    </div>
                  </div>

                  <div className="space-y-3">
                    <div>
                      <div className="flex justify-between text-sm mb-1">
                        <span className="text-gray-500">제출률</span>
                        <span style={{ color: "#2C3E50" }}>
                          {templateDetail.submitted || submittedCount}/{templateDetail.participants || studentResults.length} (
                          {templateDetail.submissionRate || (studentResults.length > 0 ? getProgressPercentage(submittedCount, studentResults.length) : 0) + "%"})
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-blue-600 h-2 rounded-full"
                          style={{ width: `${parseInt(templateDetail.submissionRate) || (studentResults.length > 0 ? getProgressPercentage(submittedCount, studentResults.length) : 0)}%` }}
                        ></div>
                      </div>
                    </div>

                    <div>
                      <div className="flex justify-between text-sm mb-1">
                        <span className="text-gray-500">채점률</span>
                        <span style={{ color: "#2C3E50" }}>
                          {templateDetail.graded || gradedCount}/{templateDetail.submitted || submittedCount} (
                          {templateDetail.gradingRate || (submittedCount > 0 ? getGradingPercentage(gradedCount, submittedCount) : 0) + "%"})
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-green-600 h-2 rounded-full"
                          style={{ width: `${parseInt(templateDetail.gradingRate) || (submittedCount > 0 ? getGradingPercentage(gradedCount, submittedCount) : 0)}%` }}
                        ></div>
                      </div>
                    </div>
                    
                    <div>
                      
                      {/* <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-yellow-600 h-2 rounded-full"
                          style={{ width: `${submittedCount > 0 ? (studentResults.filter(s => getStudentStatus(s) === "채점대기").length / submittedCount) * 100 : 0}%` }}
                        ></div>
                      </div> */}
                    </div>
                  </div>

                  {/* <div className="flex gap-2 pt-4">
                    <Button size="sm" variant="outline" className="bg-transparent">
                      <Download className="w-4 h-4 mr-2" />
                      결과 다운로드
                    </Button>
                  </div> */}
                </CardContent>
              </Card>
            </div>

            {/* 문제 목록 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2" style={{ color: "#2C3E50" }}>
                  <BookOpen className="w-5 h-5" />
                  출제 문제 ({templateDetail.questions.length}문항)
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4 max-h-140 overflow-y-auto">
                  {templateDetail.questions.map((question, index) => (
                    <div key={question.id} className="border rounded-lg p-4">
                      <div className="flex items-start justify-between mb-3">
                        <div className="flex items-center gap-3">
                          <span className="bg-[#eff6ff] text-gray-600 px-2 py-1 rounded text-sm font-medium">
                            {index + 1}번
                          </span>
                          <Badge variant="outline">{question.type}</Badge>
                          <span className="text-sm text-gray-500">{question.points}점</span>
                        </div>
                      </div>
                      <p className="text-sm mb-3" style={{ color: "#2C3E50" }}>
                        {question.question}
                      </p>
                      {question.type === "객관식" && question.options && question.options.length > 0 ? (
                        <div className="grid grid-cols-2 gap-2 text-sm">
                          {question.options.map((option, optIndex) => {
                            // option이 객체인 경우 optText를 사용, 문자열인 경우 그대로 사용
                            const optionText = typeof option === 'object' && option.optText ? option.optText : option;
                            return (
                              <div
                                key={optIndex}
                                className={`p-2 rounded ${
                                  optionText === question.correctAnswer
                                    ? "bg-green-50 text-green-800 border border-green-200"
                                    : "bg-gray-50 text-gray-700"
                                }`}
                              >
                                {String.fromCharCode(65 + optIndex)}. {optionText}
                                {optionText === question.correctAnswer && (
                                  <CheckCircle className="w-4 h-4 inline ml-2 text-green-600" />
                                )}
                              </div>
                            );
                          })}
                        </div>
                      ) : question.type === "서술형" ? (
                        <div className="space-y-3">
                          {question.correctAnswer ? (
                            <div className="p-2 bg-green-50 rounded text-sm">
                              <p className="font-medium text-green-800 mb-1">정답:</p>
                              <p className="text-green-700">{question.correctAnswer}</p>
                            </div>
                          ) : (
                            <div className="text-sm text-gray-500 italic bg-gray-50 p-2 rounded">
                              답은 정해져 있지 않습니다.
                            </div>
                          )}
                        </div>
                      // ) : question.type === "코드형" ? (
                      //   <div className="space-y-3">
                      //     <div className="text-sm text-gray-500 italic bg-gray-50 p-2 rounded">
                      //       코드형 문제입니다
                      //     </div>
                      //     {question.correctAnswer ? (
                      //       <div className="p-2 bg-green-50 rounded text-sm">
                      //         <p className="font-medium text-green-800 mb-1">정답:</p>
                      //         <p className="text-green-700">{question.correctAnswer}</p>
                      //       </div>
                      //     ) : (
                      //       <div className="text-sm text-gray-500 italic bg-gray-50 p-2 rounded">
                      //         답은 정해져 있지 않습니다.
                      //       </div>
                      //     )}
                      //   </div>
                      ) : (
                        <div className="text-sm text-gray-500 italic bg-gray-50 p-2 rounded">
                          선택지 정보를 불러올 수 없습니다
                        </div>
                      )}
                      
                      {/* 해설 표시 */}
                      <div className="mt-3 p-2 bg-blue-50 rounded text-sm">
                        <p className="font-medium text-blue-800 mb-1">해설:</p>
                        {question.explanation ? (
                          <p className="text-blue-700">{question.explanation}</p>
                        ) : (
                          <p className="text-gray-500 italic">해설이 없습니다.</p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* 학생별 응시 현황 */}
            <Card>
              <CardHeader>
                <div className="flex justify-between items-center">
                  <CardTitle className="flex items-center gap-2" style={{ color: "#2C3E50" }}>
                    <Users className="w-5 h-5" />
                    학생별 응시 현황 ({filteredResults.length}명)
                  </CardTitle>
                  <div className="flex gap-2">
                    <Input
                      placeholder="학생명으로 검색..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="w-64"
                    />
                    <select
                      value={statusFilter}
                      onChange={(e) => setStatusFilter(e.target.value)}
                      className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
                    >
                      <option value="all">전체 상태</option>
                      <option value="미제출">미제출</option>
                      <option value="제출완료">제출완료</option>
                      <option value="채점대기">채점대기</option>
                      <option value="채점필요">채점필요</option>
                      <option value="채점완료">채점완료</option>
                    </select>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto max-h-96 overflow-y-auto">
                  <table className="w-full">
                    <thead className="sticky top-0 bg-white z-10">
                      <tr className="border-b">
                        <th className="text-left py-3 px-4 font-medium text-gray-500 bg-white">학생 정보</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-500 bg-white">제출일시</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-500 bg-white">상태</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-500 bg-white">점수</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-500 bg-white">등급</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-500 bg-white">성적 확인</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-500 bg-white">관리</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredResults.map((result) => (
                        <tr key={result.id} className="border-b hover:bg-gray-50">
                          <td className="py-4 px-4">
                            <div>
                              <p className="font-medium" style={{ color: "#2C3E50" }}>
                                {result.name}
                              </p>
                              <p className="text-sm text-gray-500">{result.studentId}</p>
                              <p className="text-xs text-gray-400">{result.email}</p>
                            </div>
                          </td>
                          <td className="py-4 px-4">
                            <p className="text-sm" style={{ color: "#2C3E50" }}>
                              {result.submittedAt ? formatSubmittedDate(result.submittedAt) : 
                               (templateDetail.endDate && templateDetail.endDate !== "날짜 미정" ? 
                                `시험 종료 시점 (${templateDetail.endDate})` : "미제출")}
                            </p>
                          </td>
                          <td className="py-4 px-4">
                            <Badge className={getStatusColor(getStudentStatus(result))}>{getStudentStatus(result)}</Badge>
                          </td>
                                                     <td className="py-4 px-4">
                             <div className="flex flex-col gap-1">
                               {/* 점수는 채점 완료 상태에서만 표시 */}
                               {getStudentStatus(result) === "채점완료" ? (
                                 <p className={`text-sm ${getScoreColor(result.score, templateDetail.maxScore)}`}>
                                   {result.score !== null && result.score !== undefined ? `${result.score}점` : "0점"}
                                 </p>
                               ) : (
                                 /* 채점 완료가 아닌 경우 상태별 메시지 표시 */
                                 <p className="text-sm text-gray-500">
                                   {getStudentStatus(result) === "미제출" ? "미제출" :
                                    getStudentStatus(result) === "제출완료" ? "제출완료" :
                                    getStudentStatus(result) === "채점대기" ? "채점 대기" : ""}
                                 </p>
                               )}
                             </div>
                           </td>

                          <td className="py-4 px-4">
                            <p className="text-sm" style={{ color: "#2C3E50" }}>
                              {getStudentStatus(result) === "채점완료" && result.grade ? result.grade : 
                               (getStudentStatus(result) === "미제출" ? "미제출" :
                                getStudentStatus(result) === "제출완료" ? "제출완료" :
                                getStudentStatus(result) === "채점대기" ? "채점 대기" : "")}
                            </p>
                          </td>
                          <td className="py-4 px-4">
                            {(result.graded || (getStudentStatus(result) === "미제출" && templateDetail.endDate && templateDetail.endDate !== "날짜 미정")) && (
                              <div className="text-sm">
                                {result.isChecked === 0 && result.graded && (result.submittedAt || getStudentStatus(result) === "미제출") ? (
                                  <span className="text-blue-600 font-medium">✓ 확인완료</span>
                                ) : (
                                  <span className="text-orange-600 font-medium">⏳ 미확인</span>
                                )}
                              </div>
                            )}
                          </td>
                          <td className="py-4 px-4">
                            <div className="flex gap-2">
                              {/* {result.submittedAt && (
                                <Button
                                  size="sm"
                                  variant="outline"
                                  className="text-blue-600 border-blue-600 hover:bg-blue-50 bg-transparent"
                                >
                                  <Eye className="w-3 h-3 mr-1" />
                                  답안보기
                                </Button>
                              )} */}
                              {/* 시험이 종료된 경우에만 채점하기 버튼 표시 */}
                              {isExamEnded && (getStudentStatus(result) === "채점대기" || getStudentStatus(result) === "채점필요") && (
                                <Button
                                  size="sm"
                                  className="bg-green-600 hover:bg-green-700"
                                  onClick={() => handleGradeStudent(result)}
                                >
                                  <Edit className="w-3 h-3 mr-1" />
                                  채점하기
                                </Button>
                              )}
                              {getStudentStatus(result) === "채점완료" && (
                                <div className="text-xs text-green-600 font-medium">
                                  ✓ 채점 완료
                                </div>
                              )}
                              {/* 시험이 종료되지 않았고 채점이 필요한 경우 */}
                              {!isExamEnded && (getStudentStatus(result) === "채점대기" || getStudentStatus(result) === "채점필요" || getStudentStatus(result) === "제출완료") && (
                                <div className="text-xs text-gray-500 italic">
                                  종료일 후 채점 가능
                                </div>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {filteredResults.length === 0 && (
                  <div className="text-center py-12">
                    <Users className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-gray-900 mb-2">검색 결과가 없습니다</h3>
                    <p className="text-gray-500">다른 검색어나 필터를 사용해보세요.</p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
      </PageLayout>

      {/* 채점 모달 */}
      {isGradingModalOpen && selectedStudent && (
        <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                시험 채점 - {selectedStudent.name}
              </h2>
              <button onClick={() => setIsGradingModalOpen(false)} className="text-gray-500 hover:text-gray-700">
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="space-y-6">
              {/* 학생 정보 */}
              <div className="bg-gradient-to-r from-purple-50 to-blue-50 border border-purple-200 rounded-lg p-4">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-8 h-8 bg-purple-100 rounded-full flex items-center justify-center">
                    <Users className="w-4 h-4 text-purple-600" />
                  </div>
                  <h3 className="text-lg font-semibold text-purple-800">채점 대상 학생</h3>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-2 gap-4">
                  <div className="bg-white p-3 rounded border border-purple-200">
                    <p className="text-sm text-gray-500 mb-1">학생명</p>
                    <p className="font-semibold text-lg" style={{ color: "#2C3E50" }}>
                      {selectedStudent.name}
                    </p>
                  </div>
                  
                  <div className="bg-white p-3 rounded border border-purple-200">
                    <p className="text-sm text-gray-500 mb-1">제출일시</p>
                    <p className="font-semibold text-lg" style={{ color: "#2C3E50" }}>
                      {formatSubmittedDate(selectedStudent.submittedAt)}
                    </p>
                  </div>
                </div>
              </div>

              {/* 문제별 채점 */}
              <div className="space-y-4">
                <h3 className="text-lg font-semibold" style={{ color: "#2C3E50" }}>
                  문제별 채점
                </h3>

                {templateDetail.questions.map((question, index) => (
                  <div key={question.id} className="border rounded-lg p-4">
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-3">
                        <span className="bg-purple-100 text-purple-800 px-2 py-1 rounded text-sm font-medium">
                          {index + 1}번
                        </span>
                        <Badge variant="outline">{question.type}</Badge>
                        <span className="text-sm text-gray-500">배점: {question.points}점</span>
                      </div>
                    </div>

                    <div className="mb-4">
                      <p className="font-medium mb-2" style={{ color: "#2C3E50" }}>
                        문제
                      </p>
                      <p className="text-sm text-gray-700">{question.question}</p>
                    </div>

                    {question.type === "객관식" && (
                      <div className="mb-4">
                        <p className="font-medium mb-2" style={{ color: "#2C3E50" }}>
                          선택지
                        </p>
                        <div className="grid grid-cols-2 gap-2 text-sm">
                          {question.options.map((option, optIndex) => {
                            // option이 객체인 경우 optText를 사용, 문자열인 경우 그대로 사용
                            const optionText = typeof option === 'object' && option.optText ? option.optText : option;
                            return (
                              <div
                                key={optIndex}
                                className={`p-2 rounded ${
                                  optionText === question.correctAnswer
                                    ? "bg-green-50 text-green-800 border border-green-200"
                                    : selectedStudent.answers && String(selectedStudent.answers[question.id]).trim() === String(optionText).trim()
                                      ? "bg-red-50 text-red-800 border border-red-200"
                                      : "bg-gray-50 text-gray-700"
                                }`}
                              >
                                {optIndex + 1}. {optionText}
                                {optionText === question.correctAnswer && (
                                  <CheckCircle className="w-4 h-4 inline ml-2 text-green-600" />
                                )}
                              </div>
                            );
                          })}
                        </div>
                        <div className="mt-3">
                          <div className="bg-blue-50 border-l-4 border-blue-400 p-3 rounded-r">
                            <div className="flex items-center gap-2 mb-2">
                              <div className="w-2 h-2 bg-blue-400 rounded-full"></div>
                              <p className="text-sm font-semibold text-blue-800">학생 답안</p>
                              <Badge variant="outline" className="text-xs bg-blue-100 text-blue-700 border-blue-300">
                                객관식
                              </Badge>
                            </div>
                            <div className="bg-white p-3 rounded border border-blue-200 shadow-sm">
                              <div className="flex items-center gap-2">
                                <div className="w-6 h-6 bg-blue-100 rounded-full flex items-center justify-center">
                                  <span className="text-xs font-bold text-blue-600">A</span>
                                </div>
                                <p className="text-sm font-medium text-gray-800">
                                  {selectedStudent.answers && selectedStudent.answers[question.id] 
                                    ? selectedStudent.answers[question.id] 
                                    : "답안이 없습니다."}
                                </p>
                              </div>
                            </div>
                            
                          </div>
                        </div>
                      </div>
                    )}

                    {(question.type === "서술형" || question.type === "코드형") && (
                      <div className="mb-4">
                        <div className="bg-orange-50 border-l-4 border-orange-400 p-3 rounded-r">
                          <div className="flex items-center gap-2 mb-2">
                            <div className="w-2 h-2 bg-orange-400 rounded-full"></div>
                            <p className="text-sm font-semibold text-orange-800">학생 답안</p>
                            <Badge variant="outline" className="text-xs bg-orange-100 text-orange-700 border-orange-300">
                              {question.type}
                            </Badge>
                          </div>
                          <div className="bg-white p-4 rounded border border-orange-200 shadow-sm">
                            <div className="whitespace-pre-wrap text-sm font-medium text-gray-800 leading-relaxed">
                              {selectedStudent.answers && selectedStudent.answers[question.id] 
                                ? selectedStudent.answers[question.id] 
                                : "답안이 없습니다."}
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {question.type === "객관식" ? (
                      <div className="space-y-3">
                        <div className="flex items-center gap-4">
                          <div className="flex items-center gap-2">
                            <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                              점수:
                            </label>
                            <div className="w-20 px-2 py-1 border border-gray-300 rounded text-sm bg-gray-50 text-gray-600">
                              {selectedStudent.answers && selectedStudent.answers[question.id] === question.correctAnswer ? question.points : 0}
                            </div>
                            <span className="text-sm text-gray-500">/ {question.points}점</span>
                          </div>
                          <div className="flex-1">
                            <div className="text-sm text-gray-500 italic">
                              {selectedStudent.answers && selectedStudent.answers[question.id] === question.correctAnswer 
                                ? "정답입니다." 
                                : "오답입니다."}
                            </div>
                          </div>
                        </div>
                        
                        {/* 객관식 코멘트 추가 */}
                        <div>
                          <label className="block text-sm font-medium mb-2" style={{ color: "#2C3E50" }}>
                            코멘트/해설:
                          </label>
                          <textarea
                            rows="3"
                            value={gradingComments[question.id] || ""}
                            onChange={(e) => handleCommentChange(question.id, e.target.value)}
                            placeholder="학생 답안에 대한 코멘트나 해설을 작성해주세요..."
                            className="w-full px-3 py-2 border border-gray-300 rounded text-sm resize-none"
                          ></textarea>
                        </div>
                      </div>
                    ) : (
                      <div className="space-y-3">
                        <div className="space-y-2">
                          <div className="flex items-center gap-2">
                            <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                              점수:
                            </label>
                            <input
                              type="number"
                              min="0"
                              max={question.points}
                              value={gradingScores[question.id] || 0}
                              onChange={(e) => handleScoreChange(question.id, e.target.value)}
                              className={`w-20 px-2 py-1 border rounded text-sm ${
                                (gradingScores[question.id] || 0) > question.points 
                                  ? 'border-red-300 bg-red-50' 
                                  : 'border-gray-300'
                              }`}
                            />
                            <span className="text-sm text-gray-500">/ {question.points}점</span>
                          </div>
                          
                          {/* 점수 경고 메시지 */}
                          {(gradingScores[question.id] || 0) > question.points && (
                            <div className="flex items-center gap-1 text-xs text-red-600 bg-red-50 px-2 py-1 rounded">
                              <span className="w-1 h-1 bg-red-600 rounded-full"></span>
                              배점({question.points}점)을 초과할 수 없습니다.
                            </div>
                          )}
                          
                          {/* 점수 안내 메시지 */}
                          {(gradingScores[question.id] || 0) === question.points && (
                            <div className="flex items-center gap-1 text-xs text-green-600 bg-green-50 px-2 py-1 rounded">
                              <span className="w-1 h-1 bg-green-600 rounded-full"></span>
                              만점입니다!
                            </div>
                          )}
                        </div>
                        <div>
                          <label className="block text-sm font-medium mb-2" style={{ color: "#2C3E50" }}>
                            코멘트/해설:
                          </label>
                          <textarea
                            rows="3"
                            value={gradingComments[question.id] || ""}
                            onChange={(e) => handleCommentChange(question.id, e.target.value)}
                            placeholder="학생 답안에 대한 코멘트나 해설을 작성해주세요..."
                            className="w-full px-3 py-2 border border-gray-300 rounded text-sm resize-none"
                          ></textarea>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>

              {/* 총점 및 전체 피드백 */}
              <div className="border-t pt-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-2" style={{ color: "#2C3E50" }}>
                      총점
                    </label>
                    <div className="flex items-center gap-2">
                      <div className="w-24 px-3 py-2 border border-gray-300 rounded bg-gray-50 text-gray-600">
                        {calculateTotalScore()}
                      </div>
                      <span className="text-sm text-gray-500">/ {templateDetail.maxScore}점</span>
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-2" style={{ color: "#2C3E50" }}>
                      전체 피드백
                    </label>
                    <textarea
                      rows="3"
                      value={overallFeedback}
                      onChange={(e) => setOverallFeedback(e.target.value)}
                      placeholder="학생에게 전달할 전체적인 피드백을 작성해주세요..."
                      className="w-full px-3 py-2 border border-gray-300 rounded resize-none"
                    ></textarea>
                  </div>
                </div>
              </div>

              {/* 버튼 */}
              <div className="flex justify-end gap-3 pt-4 border-t">
                <Button variant="outline" onClick={() => setIsGradingModalOpen(false)} className="bg-transparent">
                  취소
                </Button>
                <Button
                  onClick={handleGradingSubmit}
                  className="bg-green-600 hover:bg-green-700"
                >
                  채점 완료
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 시험 수정 모달 */}
      {isEditModalOpen && editingTemplate && (
        <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg w-full max-w-6xl max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center p-6 border-b">
              <h2 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                시험 수정 - {templateDetail.title}
              </h2>
              <button onClick={handleEditCancel} className="text-gray-500 hover:text-gray-700">
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              {/* 시험 기본 정보 수정 */}
              <div className="space-y-4">
                <h3 className="text-lg font-semibold" style={{ color: "#2C3E50" }}>
                  시험 기본 정보
                </h3>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">시험 제목</label>
                    <input
                      type="text"
                    value={editingTemplate.title}
                      onChange={(e) => handleExamFieldChange("title", e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">시험 시간 (분)</label>
                    <input
                      type="number"
                      value={editingTemplate.duration}
                      onChange={(e) => handleExamFieldChange("duration", Number.parseInt(e.target.value))}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">만점</label>
                    <div className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-gray-600">
                      {editingTemplate.maxScore}점
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">합격 점수</label>
                    <div className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-gray-600">
                      {editingTemplate.passingScore}점
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">시작일시</label>
                    <input
                      type="datetime-local"
                      value={editingTemplate.startDate}
                      onChange={(e) => handleExamFieldChange("startDate", e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">종료일시</label>
                    <input
                      type="datetime-local"
                      value={editingTemplate.endDate}
                      onChange={(e) => handleExamFieldChange("endDate", e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                </div>


              </div>


            </div>

            {/* 모달 푸터 */}
            <div className="flex justify-end gap-3 p-6 border-t">
              <Button variant="outline" onClick={handleEditCancel} className="bg-transparent">
                취소
              </Button>
              <Button onClick={handleEditSubmit} className="bg-blue-600 hover:bg-blue-700">
                수정 완료
              </Button>
            </div>
          </div>
        </div>
      )}

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
                <strong>"{templateDetail.title}"</strong> 시험을 삭제하시겠습니까?
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
                onClick={confirmDeleteTemplate}
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

