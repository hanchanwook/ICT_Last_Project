import React, { useState, useEffect } from "react"
import { useLocation } from "react-router-dom"
import { ArrowLeft, Save, Plus, Minus, X, FileText, BookOpen, Calendar, Users, Search, Upload, Eye } from "lucide-react"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { getMenuItems } from "@/components/ui/menuConfig"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Link } from "react-router-dom"
import { http } from "@/components/auth/http"
import { getSubjectList, getSubDetailList, getQuestions, getMyCourses } from "@/api/kayoung/questionBankApi"
import { createCompleteExam } from "@/api/kayoung/templateApi"

export default function InstructorExamMyExamCreatePage() {
  const [examData, setExamData] = useState({
    title: "",
    course: "",
    selectedSubject: "", // 선택된 메인 과목
    selectedSubDetail: "", // 선택된 세부과목 추가
    type: "퀴즈",
    duration: 60,
    maxScore: 100, // 100점으로 고정
    passScore: 60,
    startDate: "",
    endDate: "",
    allowLateSubmission: false,
    showResults: true,
    randomizeQuestions: false,
  })

  const [questions, setQuestions] = useState([
    {
      id: 1,
      type: "객관식",
      question: "",
      options: ["", "", "", ""],
      correctAnswer: 0,
      score: 5, // 자동으로 5점 배점
      explanation: "",
      subDetail: "", // 각 문제별 세부과목
      fromBank: false, // 문제은행에서 가져온 문제인지 여부
    },
  ])

  const [isLoading, setIsLoading] = useState(false)
  const [errors, setErrors] = useState({})

  const [isQuestionBankOpen, setIsQuestionBankOpen] = useState(false)
  const [questionBankData, setQuestionBankData] = useState([])
  const [selectedQuestions, setSelectedQuestions] = useState([])
  const [bankSearchTerm, setBankSearchTerm] = useState("")
  const [bankSelectedSubject, setBankSelectedSubject] = useState("all")

  // 실제 데이터 상태
  const [myCourses, setMyCourses] = useState([])
  const [coursesLoading, setCoursesLoading] = useState(true)
  const [coursesError, setCoursesError] = useState(null)
  
  // 선택된 과정의 과목 토글 상태
  const [selectedCourseSubjects, setSelectedCourseSubjects] = useState([])
  const [selectedSubDetails, setSelectedSubDetails] = useState([])
  const [showSubjects, setShowSubjects] = useState(false)
  const [showSubDetails, setShowSubDetails] = useState(false)
  
  // 문제은행 관련 상태 추가
  const [bankSubjects, setBankSubjects] = useState([])
  const [bankLoading, setBankLoading] = useState(false)

  const location = useLocation()
  const sidebarMenuItems = getMenuItems('instructor-exam')
  const currentPath = location.pathname

  // 강사가 담당하는 과정 목록 로드
  const loadMyCourses = async () => {
    try {
      setCoursesLoading(true)
      setCoursesError(null)
      
      const courses = await getMyCourses()
      setMyCourses(courses)
    } catch (error) {
      setCoursesError('과정 목록을 불러오는 데 실패했습니다.')
      setMyCourses([])
    } finally {
      setCoursesLoading(false)
    }
  }

  // 컴포넌트 마운트 시 과정 목록 로드
  useEffect(() => {
    loadMyCourses()
  }, [])

  const questionTypes = ["객관식", "서술형"] //, "코드형"]

  const handleExamDataChange = (field, value) => {
    setExamData((prev) => ({
      ...prev,
      [field]: value,
    }))
    
    // 과정이 선택되면 해당 과정의 과목들을 설정하고 선택된 과목 초기화
    if (field === "course") {
      const selectedCourse = myCourses.find(course => `${course.courseName} (${course.courseCode})` === value)
      if (selectedCourse && selectedCourse.subjects) {
        
        // 중복된 과목 제거 (동일한 subjectName 기준)
        const uniqueSubjects = selectedCourse.subjects.reduce((acc, current) => {
          const existingSubject = acc.find(subject => subject.subjectName === current.subjectName)
          if (!existingSubject) {
            acc.push(current)
          }
          return acc
        }, [])
        
        setSelectedCourseSubjects(uniqueSubjects)
        setShowSubjects(true)
        // 과정이 변경되면 선택된 과목과 세부과목 초기화
        setExamData(prev => ({
          ...prev,
          selectedSubject: "",
          selectedSubDetail: ""
        }))
        setSelectedSubDetails([])
        setShowSubDetails(false)
      } else {
        setSelectedCourseSubjects([])
        setShowSubjects(false)
        setSelectedSubDetails([])
        setShowSubDetails(false)
        // 과정이 없으면 선택된 과목과 세부과목 초기화
        setExamData(prev => ({
          ...prev,
          selectedSubject: "",
          selectedSubDetail: ""
        }))
      }
    }
    
    // 메인 과목이 선택되면 해당 과목의 세부과목들을 설정
    if (field === "selectedSubject") {
      const selectedSubject = selectedCourseSubjects.find(subject => subject.subjectName === value)
      
      if (selectedSubject) {
        // 백엔드에서 subDetails가 포함된 경우
        if (selectedSubject.subDetails && Array.isArray(selectedSubject.subDetails)) {
          setSelectedSubDetails(selectedSubject.subDetails)
          setShowSubDetails(true)
        } 
        // 백엔드에서 subDetails가 없는 경우 - 임시로 과목명을 세부과목으로 사용
        else {
          const tempSubDetails = [{
            subDetailId: selectedSubject.subjectId,
            subDetailName: selectedSubject.subjectName
          }]
          setSelectedSubDetails(tempSubDetails)
          setShowSubDetails(true)
        }
        
        // 과목이 변경되면 선택된 세부과목 초기화
        setExamData(prev => ({
          ...prev,
          selectedSubDetail: ""
        }))
      } else {
        setSelectedSubDetails([])
        setShowSubDetails(false)
        // 과목이 없으면 선택된 세부과목 초기화
        setExamData(prev => ({
          ...prev,
          selectedSubDetail: ""
        }))
      }
    }
    
    // 시작일시가 변경되면 종료일시 초기화
    if (field === "startDate") {
      setExamData(prev => ({
        ...prev,
        endDate: ""
      }))
    }
  }

  const handleQuestionChange = (questionId, field, value) => {
    setQuestions((prev) => prev.map((q) => (q.id === questionId ? { ...q, [field]: value } : q)))
  }

  const handleOptionChange = (questionId, optionIndex, value) => {
    setQuestions((prev) =>
      prev.map((q) =>
        q.id === questionId
          ? {
              ...q,
              options: q.options.map((opt, idx) => (idx === optionIndex ? value : opt)),
            }
          : q,
      ),
    )
  }

  const addQuestion = () => {
    // 현재 선택된 세부과목이 있는지 확인
    const currentSubDetail = selectedSubDetails.length > 0 ? selectedSubDetails[0].subDetailName : ""
    
    const newQuestion = {
      id: Math.max(...questions.map(q => q.id), 0) + 1, // 기존 ID 중 최대값 + 1
      type: "객관식",
      question: "",
      options: ["", "", "", ""],
      correctAnswer: 0,
      score: 5, // 자동으로 5점 배점
      explanation: "",
      subDetail: currentSubDetail, // 현재 선택된 세부과목으로 설정
      fromBank: false, // 새로 생성한 문제
    }
    setQuestions((prev) => [...prev, newQuestion])
    
    // 새로 추가된 문제로 스크롤 (맨 밑부분으로)
    setTimeout(() => {
      const newQuestionElement = document.getElementById(`question-${newQuestion.id}`)
      if (newQuestionElement) {
        newQuestionElement.scrollIntoView({ behavior: 'smooth', block: 'end' })
      }
    }, 100)
  }

  const removeQuestion = (questionId) => {
    if (questions.length > 1) {
      setQuestions((prev) => prev.filter((q) => q.id !== questionId))
    }
  }

  const validateForm = () => {
    const newErrors = {}

    if (!examData.title.trim()) newErrors.title = "시험 제목을 입력해주세요"
    if (!examData.course) newErrors.course = "과정을 선택해주세요"
    if (!examData.selectedSubject) newErrors.selectedSubject = "출제 과목을 선택해주세요"
    
    // 각 문제의 세부과목 검증
    questions.forEach((q, index) => {
      if (!q.subDetail || !q.subDetail.trim()) {
        newErrors[`subDetail_${q.id}`] = `${index + 1}번 문제의 세부과목을 선택해주세요`
      }
    })
    
    // 시작일시가 선택된 경우 종료일시 필수
    if (examData.startDate && !examData.endDate) {
      newErrors.endDate = "종료일시를 선택해주세요"
    }
    
    // 시작일시와 종료일시가 모두 입력된 경우 날짜 검증
    if (examData.startDate && examData.endDate && examData.startDate >= examData.endDate) {
      newErrors.endDate = "종료일은 시작일보다 늦어야 합니다"
    }

    // 배점 합계 검증
    const totalScore = questions.reduce((total, q) => total + (q.score || 0), 0)
    if (totalScore !== 100) {
      newErrors.totalScore = `문제 배점의 합이 100점이어야 합니다. (현재: ${totalScore}점)`
    }

    questions.forEach((q, index) => {
      if (!q.question.trim()) {
        newErrors[`question_${q.id}`] = `${index + 1}번 문제를 입력해주세요`
      }
      if (q.type === "객관식" && q.options.some((opt) => !opt.trim())) {
        newErrors[`options_${q.id}`] = `${index + 1}번 문제의 모든 선택지를 입력해주세요`
      }
    })

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async () => {
    if (!validateForm()) return
    
    // 이미 로딩 중이면 중복 요청 방지
    if (isLoading) {
      return
    }

    setIsLoading(true)
    try {
      // 선택된 과정에서 필요한 정보 추출
      const selectedCourse = myCourses.find(course => `${course.courseName} (${course.courseCode})` === examData.course)
      const selectedSubject = selectedCourse?.subjects?.find(subject => subject.subjectName === examData.selectedSubject)

      if (!selectedCourse || !selectedSubject) {
        throw new Error('선택된 과정 또는 과목 정보를 찾을 수 없습니다.')
      }

      // 시험 데이터 준비
      const examDataForAPI = {
        ...examData,
        courseId: selectedCourse.courseId,
        selectedSubjectId: selectedSubject.subjectId,
        selectedSubDetails: selectedSubDetails, // 세부과목 목록 추가
        memberId: localStorage.getItem('currentUser') ? JSON.parse(localStorage.getItem('currentUser')).memberId : null,
        // 날짜가 비어있으면 null로 설정
        startDate: examData.startDate || null,
        endDate: examData.endDate || null
      }

      // 현재 사용자 정보 상세 로그
      const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')

      // 새로 생성한 문제들과 문제은행에서 가져온 문제들 분리
      const newQuestions = questions.filter(q => !q.fromBank)
      const bankQuestions = questions.filter(q => q.fromBank)

      // 통합 시험 생성 API 호출
      const completeExamData = {
        templateName: examDataForAPI.title,
        courseId: examDataForAPI.courseId,
        subjectId: examDataForAPI.selectedSubjectId,
        templateTime: examDataForAPI.duration,
        templateOpen: examDataForAPI.startDate ? new Date(examDataForAPI.startDate).toISOString() : null,
        templateClose: examDataForAPI.endDate ? new Date(examDataForAPI.endDate).toISOString() : null,
        templateNumber: 1, // TODO: 동적으로 계산 필요 (기존 템플릿 수 + 1)
        // memberId: examDataForAPI.memberId, // 강사 ID 명시적 추가
        
        // 새로 생성할 문제들
        newQuestions: newQuestions.map(q => {
          // 세부과목명 검증
          if (!q.subDetail || !q.subDetail.trim()) {
            throw new Error(`문제 "${q.question.substring(0, 20)}..."의 세부과목을 선택해주세요.`)
          }
          
          // 선택된 세부과목에서 ID 찾기
          const selectedSubDetail = selectedSubDetails.find(subDetail => subDetail.subDetailName === q.subDetail.trim())
          if (!selectedSubDetail) {
            throw new Error(`문제 "${q.question.substring(0, 20)}..."의 세부과목 ID를 찾을 수 없습니다.`)
          }
          
          return {
            questionText: q.question,
            questionType: q.type,
            correctAnswer: q.type === "객관식" ? q.options[q.correctAnswer] : q.correctAnswer || "",
            explanation: q.explanation || "",
            options: q.type === "객관식" ? q.options.map((option, index) => ({
              optText: option,
              optIsCorrect: index === q.correctAnswer ? 1 : 0
            })) : [],
            score: q.score,
            subDetailId: selectedSubDetail.subDetailId
          }
        }),
        
        // 문제은행에서 가져온 문제들
        bankQuestions: bankQuestions.map(q => {
          // 문제은행 문제의 세부과목 ID 찾기
          const bankQuestion = questionBankData.find(bq => bq.id === q.bankId)
          if (!bankQuestion || !bankQuestion.subDetailId) {
            throw new Error(`문제은행 문제의 세부과목 ID를 찾을 수 없습니다.`)
          }
          
          return {
            questionId: q.bankId,
            score: q.score,
            subDetailId: bankQuestion.subDetailId
          }
        })
      }
      
      const result = await createCompleteExam(completeExamData)

      // 성공 메시지 표시
      alert(`시험이 성공적으로 생성되었습니다!\n새 문제: ${result.data.newQuestions}개\n문제은행 문제: ${result.data.bankQuestions}개\n총 문제: ${result.data.totalQuestions}개`)

      // 목록 페이지로 이동
      window.location.href = '/instructor/exam/my-exams'

    } catch (error) {
      
      // 백엔드 에러 메시지 추출
      let errorMessage = error.message
      if (error.response?.data?.resultMessage) {
        errorMessage = error.response.data.resultMessage
      }
      
      // 동시성 에러인 경우 특별한 메시지 표시
      if (errorMessage.includes('Row was updated or deleted by another transaction')) {
        alert('시험 생성 중 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.')
      } else {
        alert(`시험 생성에 실패했습니다: ${errorMessage}`)
      }
    } finally {
      setIsLoading(false)
    }
  }

  // 문제은행 관련 함수들
  const openQuestionBank = async () => {
    try {
      setBankLoading(true)
      setIsQuestionBankOpen(true)
      
      // 선택된 과목의 세부과목 ID들 추출
      const selectedSubject = selectedCourseSubjects.find(subject => subject.subjectName === examData.selectedSubject)
      const selectedSubDetailIds = selectedSubject?.subDetails?.map(subDetail => subDetail.subDetailId) || []
      
      // 과목 목록과 문제 목록을 동시에 로드
      const [subjectsData, questionsData] = await Promise.all([
        getSubjectList(),
        getQuestions()
      ])
      
      // 과목 목록 설정 (선택된 과목만 표시)
      setBankSubjects([examData.selectedSubject])
      
      // 문제 데이터 변환 및 설정
      const questionsArray = questionsData?.data?.questions?.questions || questionsData?.questions || questionsData || []
      
      const transformedQuestions = questionsArray.map(q => {
        // 객관식 문제의 경우 옵션 처리
        let options = []
        let correctAnswer = q.questionAnswer
        
        if (q.questionType === '객관식' && Array.isArray(q.options) && q.options.length > 0) {
          options = q.options.map(opt => opt.optText)
          const correctOption = q.options.find(opt => opt.optIsCorrect === 1)
          if (correctOption) {
            correctAnswer = correctOption.optText
          }
        }
        
        return {
          id: q.questionId,
          type: q.questionType,
          question: q.questionText,
          correctAnswer: correctAnswer,
          options: options,
          subject: q.subDetailName || '미분류',
          detailSubject: q.subDetailName || '미분류',
          mainSubject: q.subjectName || null,
          subDetailId: q.subDetailId, // 세부과목 ID 추가
          score: 5, // 기본 배점
          explanation: q.explanation || "",
          usageCount: 0, // 사용 횟수 (기본값)
          correctRate: 0, // 정답률 (기본값)
          instructor: q.memberName || '강사',
          createdDate: q.createdAt ? new Date(q.createdAt).toLocaleDateString() : '날짜 없음'
        }
      })
      
      // 선택된 세부과목들의 문제들만 필터링
      const filteredQuestions = transformedQuestions.filter(q => {
        // subDetailId가 있는 경우 해당 ID로 필터링
        if (q.subDetailId && selectedSubDetailIds.includes(q.subDetailId)) {
          return true
        }
        // subDetailId가 없는 경우 과목명으로 필터링 (fallback)
        if (!q.subDetailId && q.mainSubject === examData.selectedSubject) {
          return true
        }
        return false
      })
      
      
      setQuestionBankData(filteredQuestions)
      setSelectedQuestions([])
      
    } catch (error) {
      setQuestionBankData([])
      setBankSubjects([])
    } finally {
      setBankLoading(false)
    }
  }

  const closeQuestionBank = () => {
    setIsQuestionBankOpen(false)
    setSelectedQuestions([])
    setBankSearchTerm("")
    setBankSelectedSubject("all")
  }

  const toggleQuestionSelection = (questionId) => {
    setSelectedQuestions((prev) =>
      prev.includes(questionId) ? prev.filter((id) => id !== questionId) : [...prev, questionId],
    )
  }

  const addSelectedQuestions = () => {
    const maxId = Math.max(...questions.map(q => q.id), 0) // 기존 ID 중 최대값
    
    const questionsToAdd = questionBankData
      .filter((q) => selectedQuestions.includes(q.id))
      .map((q, index) => {
        // 객관식 문제의 경우 정답 인덱스 계산
        let correctAnswerIndex = 0
        if (q.type === '객관식' && Array.isArray(q.options) && q.options.length > 0) {
          // 정답 텍스트와 일치하는 옵션의 인덱스를 찾기
          const correctIndex = q.options.findIndex(option => option === q.correctAnswer)
          correctAnswerIndex = correctIndex >= 0 ? correctIndex : 0
        }

        return {
          id: maxId + index + 1, // 기존 최대 ID + 인덱스 + 1
          type: q.type,
          question: q.question,
          options: q.options || ["", "", "", ""],
          correctAnswer: q.type === '객관식' ? correctAnswerIndex : q.correctAnswer || 0,
          score: q.score,
          explanation: q.explanation || "",
          subDetail: q.subject || q.subDetailName || "", // 문제의 세부과목 정보
          fromBank: true,
          bankId: q.id,
        }
      })

    setQuestions((prev) => [...prev, ...questionsToAdd])
    closeQuestionBank()
  }

  // 필터링된 문제은행 문제들
  const filteredBankQuestions = questionBankData.filter((question) => {
    const matchesSearch =
      question.question.toLowerCase().includes(bankSearchTerm.toLowerCase()) ||
      (question.subject && question.subject.toLowerCase().includes(bankSearchTerm.toLowerCase())) ||
      (question.mainSubject && question.mainSubject.toLowerCase().includes(bankSearchTerm.toLowerCase())) ||
      (question.instructor && question.instructor.toLowerCase().includes(bankSearchTerm.toLowerCase()))

    return matchesSearch
  })

  const getTotalScore = () => {
    return questions.reduce((total, q) => total + (q.score || 0), 0)
  }

  return (
    <React.Fragment>
      <PageLayout currentPage="exam" userRole="instructor">
        <div className="flex">
          <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
          <main className="flex-1 p-6">
            <div className="space-y-6">
              {/* 페이지 헤더 */}
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-4 mb-2">
                    <Link to="/instructor/exam/my-exams">
                      <Button variant="outline" size="sm" className="bg-transparent">
                        <ArrowLeft className="w-4 h-4 mr-2" />
                        목록으로
                      </Button>
                    </Link>
                    <h1 className="text-3xl font-bold" style={{ color: "#2C3E50" }}>
                      새 시험 만들기
                    </h1>
                  </div>
                  <p className="text-gray-600">담당 과정에 새로운 시험을 출제하고 관리할 수 있습니다.</p>
                </div>
                <div className="flex gap-3">
                  <Button 
                    onClick={handleSubmit} 
                    disabled={isLoading} 
                    className="bg-blue-600 hover:bg-blue-700"
                  >
                    <Save className="w-4 h-4 mr-2" />
                    {isLoading ? "저장 중..." : "시험 생성"}
                  </Button>
                </div>
              </div>

              {/* 시험 기본 정보 */}
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50" }}>시험 기본 정보</CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                  {/* 첫 번째 줄 */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        시험 제목 <span className="text-red-500">*</span>
                      </label>
                      <Input
                        value={examData.title}
                        onChange={(e) => handleExamDataChange("title", e.target.value)}
                        placeholder="시험 제목을 입력하세요"
                        className={errors.title ? "border-red-500" : ""}
                      />
                      {errors.title && <p className="text-red-500 text-xs mt-1">{errors.title}</p>}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">시험 시간 (분)</label>
                      <Input
                        type="number"
                        value={examData.duration}
                        onChange={(e) => handleExamDataChange("duration", Number.parseInt(e.target.value))}
                        min="1"
                        max="300"
                      />
                    </div>
                  </div>

                  {/* 두 번째 줄 */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        담당 과정 <span className="text-red-500">*</span>
                      </label>
                      <select
                        value={examData.courseId || ""}
                        onChange={e => {
                          const selected = myCourses.find(c => c.courseId === e.target.value)
                          handleExamDataChange("course", selected ? `${selected.courseName} (${selected.courseCode})` : "")
                          handleExamDataChange("courseId", e.target.value)
                        }}
                        className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${errors.course ? "border-red-500" : ""}`}
                        disabled={coursesLoading}
                      >
                        <option value="">
                          {coursesLoading ? "로딩 중..." : "과정을 선택하세요"}
                        </option>
                        {myCourses.map(course => (
                          <option key={course.courseId} value={course.courseId}>
                            {course.courseName} ({course.courseCode})
                          </option>
                        ))}
                      </select>
                      {coursesError && <p className="text-red-500 text-xs mt-1">{coursesError}</p>}
                      {!coursesLoading && myCourses.length === 0 && (
                        <p className="text-gray-500 text-xs mt-1">담당하는 과정이 없습니다.</p>
                      )}
                      {errors.course && <p className="text-red-500 text-xs mt-1">{errors.course}</p>}
                  </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        출제 과목 <span className="text-red-500">*</span>
                      </label>
                      <select
                        value={examData.selectedSubjectId || ""}
                        onChange={e => {
                          const selected = selectedCourseSubjects.find(s => s.subjectId === e.target.value)
                          handleExamDataChange("selectedSubject", selected ? selected.subjectName : "")
                          handleExamDataChange("selectedSubjectId", e.target.value)
                        }}
                        className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${errors.selectedSubject ? "border-red-500" : ""}`}
                        disabled={!examData.courseId || selectedCourseSubjects.length === 0}
                      >
                        <option value="">
                          {!examData.courseId ? "과정을 먼저 선택하세요" : selectedCourseSubjects.length === 0 ? "과목이 없습니다" : "과목을 선택하세요"}
                        </option>
                        {selectedCourseSubjects.map(subject => (
                          <option key={subject.subjectId} value={subject.subjectId}>
                            {subject.subjectName}
                          </option>
                        ))}
                      </select>
                      {errors.selectedSubject && <p className="text-red-500 text-xs mt-1">{errors.selectedSubject}</p>}
                    </div>
                  </div>



                  {/* 네 번째 줄 */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        시작일시
                      </label>
                      <Input
                        type="datetime-local"
                        value={examData.startDate}
                        onChange={(e) => handleExamDataChange("startDate", e.target.value)}
                        className={errors.startDate ? "border-red-500" : ""}
                      />
                      {errors.startDate && <p className="text-red-500 text-xs mt-1">{errors.startDate}</p>}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        종료일시 {examData.startDate && <span className="text-red-500">*</span>}
                      </label>
                      <Input
                        type="datetime-local"
                        value={examData.endDate}
                        onChange={(e) => handleExamDataChange("endDate", e.target.value)}
                        min={examData.startDate || undefined}
                        disabled={!examData.startDate}
                        className={`${errors.endDate ? "border-red-500" : ""} ${!examData.startDate ? "bg-gray-100" : ""}`}
                      />
                      {!examData.startDate && (
                        <p className="text-xs text-gray-500 mt-1">시작일시를 먼저 선택해주세요.</p>
                      )}
                      {errors.endDate && <p className="text-red-500 text-xs mt-1">{errors.endDate}</p>}
                    </div>
                  </div>

                  {/* 다섯 번째 줄 */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">문제 배점 합</label>
                      <Input
                        type="number"
                        value={getTotalScore()}
                        disabled
                        className="bg-gray-100"
                      />
                      <p className="text-xs text-gray-500 mt-1">자동으로 계산됩니다.</p>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">만점</label>
                      <Input
                        type="number"
                        value={examData.maxScore}
                        disabled
                        className="bg-gray-100"
                      />
                      <p className="text-xs text-gray-500 mt-1">만점은 100점으로 고정됩니다.</p>
                    </div>
                  </div>
                    

                </CardContent>
              </Card>

              {/* 문제 출제 */}
              <Card>
                <CardHeader>
                  <div className="flex justify-between items-center">
                    <div>
                      <CardTitle style={{ color: "#2C3E50" }}>문제 출제</CardTitle>
                      <p className="text-sm text-gray-600 mt-1">
                        총 {questions.length}문항 | 총점: {getTotalScore()}점
                      </p>
                      {errors.totalScore && (
                        <p className="text-red-500 text-sm mt-1">{errors.totalScore}</p>
                      )}
                    </div>
                    <div className="flex gap-2">
                      <Button 
                        variant="outline" 
                        size="sm" 
                        className={`${!examData.selectedSubject ? 'bg-gray-100 text-gray-400 cursor-not-allowed' : 'bg-transparent hover:bg-gray-50'}`}
                        onClick={openQuestionBank}
                        disabled={!examData.selectedSubject}
                      >
                        <Upload className="w-4 h-4 mr-2" />
                        문제 가져오기
                        {!examData.selectedSubject && <span className="text-xs ml-1">(과목 선택 필요)</span>}
                      </Button>
                      <Button onClick={addQuestion} size="sm" className="bg-green-600 hover:bg-green-700">
                        <Plus className="w-4 h-4 mr-2" />
                        문제 추가
                      </Button>
                    </div>
                  </div>
                  
                  {/* 선택된 과목 및 세부과목 정보 */}
                  {examData.selectedSubject && (
                    <div className="mt-4 p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center gap-4">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium text-gray-700">선택된 과목:</span>
                          <Badge variant="outline" className="text-sm">
                            {examData.selectedSubject}
                          </Badge>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium text-gray-700">사용 가능한 세부과목:</span>
                          <div className="flex gap-1">
                            {selectedSubDetails.map((subDetail) => (
                              <Badge key={subDetail.subDetailId} variant="secondary" className="text-xs">
                                {subDetail.subDetailName}
                              </Badge>
                            ))}
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </CardHeader>
                <CardContent>
                  <div className="space-y-6">
                    {questions.map((question, index) => (
                      <div key={question.id} id={`question-${question.id}`} className="border rounded-lg p-6 bg-white">
                        {/* 세부과목 선택 */}
                        <div className="mb-4 p-3 bg-blue-50 rounded-lg">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-4">
                              <label className="text-sm font-medium text-gray-700">
                                출제 세부과목 <span className="text-red-500">*</span>
                                {question.fromBank && <span className="text-blue-600 text-xs ml-2">(문제은행에서 가져온 문제)</span>}
                              </label>
                              <select
                                value={question.subDetail}
                                onChange={(e) => handleQuestionChange(question.id, "subDetail", e.target.value)}
                                className={`px-3 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm ${
                                  question.fromBank ? "bg-gray-50" : ""
                                }`}
                                disabled={question.fromBank || !examData.selectedSubject || selectedSubDetails.length === 0}
                              >
                                <option value="">
                                  {!examData.selectedSubject ? "과목을 먼저 선택하세요" : 
                                   selectedSubDetails.length === 0 ? "세부과목이 없습니다" : "세부과목을 선택하세요"}
                                </option>
                                {selectedSubDetails.map((subDetail) => (
                                  <option key={subDetail.subDetailId} value={subDetail.subDetailName}>
                                    {subDetail.subDetailName}
                                  </option>
                                ))}
                              </select>
                              {question.fromBank && (
                                <span className="text-xs text-gray-500">변경 불가</span>
                              )}
                            </div>
                            {question.subDetail && (
                              <div className="flex items-center gap-2">
                                <span className="text-xs text-gray-600">선택된 세부과목:</span>
                                <Badge variant="secondary" className="text-xs">
                                  {question.subDetail}
                                </Badge>
                              </div>
                            )}
                          </div>
                          {errors[`subDetail_${question.id}`] && (
                            <p className="text-red-500 text-xs mt-2">{errors[`subDetail_${question.id}`]}</p>
                          )}
                        </div>
                        
                        <div className="flex justify-between items-start mb-4">
                          <h4 className="text-lg font-semibold" style={{ color: "#2C3E50" }}>
                            문제 {index + 1}
                          </h4>
                          <div className="flex items-center gap-2">
                            <select
                              value={question.type}
                              onChange={(e) => handleQuestionChange(question.id, "type", e.target.value)}
                              className={`px-3 py-1 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${question.fromBank ? "bg-gray-50" : ""}`}
                              disabled={question.fromBank}
                            >
                              {questionTypes.map((type) => (
                                <option key={type} value={type}>
                                  {type}
                                </option>
                              ))}
                            </select>
                            <Input
                              type="number"
                              value={question.score}
                              onChange={(e) =>
                                handleQuestionChange(question.id, "score", Number.parseInt(e.target.value))
                              }
                              min="1"
                              max="50"
                              className="w-20 text-sm"
                              placeholder="점수"
                            />
                            <span className="text-sm text-gray-500">점</span>
                            {questions.length > 1 && (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => removeQuestion(question.id)}
                                className="text-red-600 border-red-600 hover:bg-red-50 bg-transparent"
                              >
                                <Minus className="w-4 h-4" />
                              </Button>
                            )}
                          </div>
                        </div>

                        <div className="space-y-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                              문제 <span className="text-red-500">*</span>
                              {question.fromBank && <span className="text-blue-600 text-xs ml-2">(문제은행에서 가져온 문제)</span>}
                            </label>
                            <Textarea
                              value={question.question}
                              onChange={(e) => handleQuestionChange(question.id, "question", e.target.value)}
                              placeholder="문제를 입력하세요"
                              rows={3}
                              className={`${errors[`question_${question.id}`] ? "border-red-500" : ""} ${question.fromBank ? "bg-gray-50" : ""}`}
                              readOnly={question.fromBank}
                            />
                            {errors[`question_${question.id}`] && (
                              <p className="text-red-500 text-xs mt-1">{errors[`question_${question.id}`]}</p>
                            )}
                          </div>

                          {question.type === "객관식" && (
                            <div>
                              <div className="flex justify-between items-center mb-2">
                                <label className="block text-sm font-medium text-gray-700">
                                  선택지 <span className="text-red-500">*</span>
                                  {question.fromBank && <span className="text-blue-600 text-xs ml-2">(문제은행에서 가져온 문제)</span>}
                                </label>
                              </div>
                              <div className="space-y-2">
                                {question.options.map((option, optionIndex) => {
                                  // option이 객체인 경우 optText를 사용, 문자열인 경우 그대로 사용
                                  const optionText = typeof option === 'object' && option.optText ? option.optText : option;
                                  return (
                                    <div key={optionIndex} className="flex items-center gap-2">
                                      <input
                                        type="radio"
                                        name={`correct_${question.id}`}
                                        checked={question.correctAnswer === optionIndex}
                                        onChange={() => handleQuestionChange(question.id, "correctAnswer", optionIndex)}
                                        className="text-blue-600"
                                        disabled={question.fromBank}
                                      />
                                      <span className="text-sm font-medium text-gray-700 w-6">
                                        {String.fromCharCode(65 + optionIndex)}.
                                      </span>
                                      <Input
                                        value={optionText}
                                        onChange={(e) => handleOptionChange(question.id, optionIndex, e.target.value)}
                                        placeholder={`선택지 ${String.fromCharCode(65 + optionIndex)}`}
                                        className={`flex-1 ${question.fromBank ? "bg-gray-50" : ""}`}
                                        readOnly={question.fromBank}
                                      />
                                    </div>
                                  );
                                })}
                              </div>
                              {errors[`options_${question.id}`] && (
                                <p className="text-red-500 text-xs mt-1">{errors[`options_${question.id}`]}</p>
                              )}
                            </div>
                          )}

                          {question.type === "서술형" && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700 mb-2">
                                정답 (선택사항)
                                {question.fromBank && <span className="text-blue-600 text-xs ml-2">(문제은행에서 가져온 문제)</span>}
                              </label>
                              <Textarea
                                value={question.correctAnswer || ""}
                                onChange={(e) => handleQuestionChange(question.id, "correctAnswer", e.target.value)}
                                placeholder="서술형 문제의 정답을 입력하세요 (선택사항)"
                                rows={3}
                                className={`w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${question.fromBank ? "bg-gray-50" : ""}`}
                                readOnly={question.fromBank}
                              />
                              <p className="text-sm text-gray-600 mt-2">
                                정답을 입력하면 타 강사나 채점 시 도움이 될 수 있습니다.
                              </p>
                            </div>
                          )}

                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                              해설 (선택사항)
                              {question.fromBank && <span className="text-blue-600 text-xs ml-2">(문제은행에서 가져온 문제)</span>}
                            </label>
                            <Textarea
                              value={question.explanation}
                              onChange={(e) => handleQuestionChange(question.id, "explanation", e.target.value)}
                              placeholder="문제에 대한 해설을 입력하세요"
                              rows={2}
                              className={question.fromBank ? "bg-gray-50" : ""}
                              readOnly={question.fromBank}
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              {/* 맨 위로 가기 버튼 */}
              <div className="flex justify-center">
                <Button
                  onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
                  variant="outline"
                  size="sm"
                  className="bg-white hover:bg-gray-50 border-gray-300"
                >
                  <ArrowLeft className="w-4 h-4 mr-2 rotate-90" />
                  맨 위로
                </Button>
              </div>

              {/* 시험 생성 안내 */}
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50" }}>시험 생성 안내</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                    <div className="p-4 bg-blue-50 rounded-lg">
                      <h4 className="font-medium text-blue-800 mb-2">시험 설정</h4>
                      <p className="text-blue-600">시험 기본 정보와 응시 조건을 설정할 수 있습니다.</p>
                    </div>
                    <div className="p-4 bg-green-50 rounded-lg">
                      <h4 className="font-medium text-green-800 mb-2">문제 출제</h4>
                      <p className="text-green-600">다양한 유형의 문제를 출제하고 배점을 설정할 수 있습니다.</p>
                    </div>
                    <div className="p-4 bg-purple-50 rounded-lg">
                      <h4 className="font-medium text-purple-800 mb-2">시험 관리</h4>
                      <p className="text-purple-600">생성된 시험을 수정하고 응시 현황을 관리할 수 있습니다.</p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </main>
        </div>
      </PageLayout>

      {/* 문제은행 모달 */}
      {isQuestionBankOpen && (
        <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg w-full max-w-6xl max-h-[90vh] overflow-hidden">
            <div className="flex flex-col h-[90vh]">
              {/* 모달 헤더 */}
              <div className="flex justify-between items-center p-6 border-b">
                <div>
                  <h2 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                    문제은행에서 문제 가져오기
                  </h2>
                  <p className="text-sm text-gray-600 mt-1">
                    기존에 생성된 문제들을 선택하여 시험에 추가할 수 있습니다.
                  </p>
                </div>
                <button onClick={closeQuestionBank} className="text-gray-500 hover:text-gray-700">
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              {/* 검색 및 필터 */}
              <div className="p-6 border-b bg-gray-50">
                <div className="flex flex-col md:flex-row gap-4">
                  <div className="flex-1">
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                      <input
                        type="text"
                        placeholder="문제 내용으로 검색..."
                        value={bankSearchTerm}
                        onChange={(e) => setBankSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* 문제 목록 */}
              <div className="flex-1 overflow-y-auto p-6">
                {bankLoading ? (
                  <div className="text-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-gray-500">문제 목록을 불러오는 중...</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {filteredBankQuestions.map((question) => (
                      <div
                        key={question.id}
                        className={`border rounded-lg p-4 cursor-pointer transition-all ${
                          selectedQuestions.includes(question.id)
                            ? "border-blue-500 bg-blue-50"
                            : "border-gray-200 hover:border-gray-300"
                        }`}
                        onClick={() => toggleQuestionSelection(question.id)}
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-2">
                              <input
                                type="checkbox"
                                checked={selectedQuestions.includes(question.id)}
                                onChange={() => toggleQuestionSelection(question.id)}
                                className="rounded text-blue-600"
                              />
                              <span className="text-sm font-medium text-blue-600">
                                {question.mainSubject && `${question.mainSubject} - `}{question.subject}
                              </span>
                              <span className="px-2 py-1 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                                {question.type}
                              </span>
                            </div>
                            <p className="text-gray-800 mb-2">{question.question}</p>
                            {question.options && question.options.length > 0 && (
                              <div className="text-sm text-gray-600 mb-2">
                                <div className="grid grid-cols-2 gap-1">
                                  {question.options.map((option, idx) => {
                                    // option이 객체인 경우 optText를 사용, 문자열인 경우 그대로 사용
                                    const optionText = typeof option === 'object' && option.optText ? option.optText : option;
                                    return (
                                      <div
                                        key={idx}
                                        className={`${optionText === question.correctAnswer ? "font-medium text-green-600" : ""}`}
                                      >
                                        {String.fromCharCode(65 + idx)}. {optionText}
                                      </div>
                                    );
                                  })}
                                </div>
                              </div>
                            )}
                            <div className="flex items-center gap-4 text-xs text-gray-500">
                              <span>출제자: {question.instructor}</span>
                              <span>생성일: {question.createdDate}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {!bankLoading && filteredBankQuestions.length === 0 && (
                  <div className="text-center py-8">
                    <p className="text-gray-500">
                      {bankSearchTerm || bankSelectedSubject !== "all" 
                        ? "검색 조건에 맞는 문제가 없습니다." 
                        : "문제은행에서 가져올 문제가 없습니다."}
                    </p>
                    <p className="text-sm text-gray-400 mt-2">
                      {bankSearchTerm || bankSelectedSubject !== "all" 
                        ? "다른 검색어나 필터를 시도해보세요." 
                        : "문제은행에 등록된 문제가 없습니다."}
                    </p>
                  </div>
                )}
              </div>

              {/* 모달 푸터 */}
              <div className="flex justify-between items-center p-6 border-t bg-gray-50">
                <div className="text-sm text-gray-600">{selectedQuestions.length}개 문제 선택됨</div>
                <div className="flex gap-3">
                  <Button variant="outline" onClick={closeQuestionBank} className="bg-transparent">
                    취소
                  </Button>
                  <Button
                    onClick={addSelectedQuestions}
                    disabled={selectedQuestions.length === 0}
                    className="bg-blue-600 hover:bg-blue-700"
                  >
                    선택한 문제 추가 ({selectedQuestions.length}개)
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </React.Fragment>
  )
}
