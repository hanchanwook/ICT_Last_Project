import React, { useState, useEffect } from "react"
import { Search, Eye, Settings, X, Star, Users, MessageSquare, ChevronDown, ChevronRight, PieChart, Activity, BarChart2, CheckCircle, Library, Clock, MessageCircle, Plus, Calendar, Edit, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { evaluationMenuItems } from "@/components/ui/menuConfig"
import { getCourseEvaluation, getEvaluationTemplateList, setCourseEvaluationTemplate, updateCourseEvaluationTemplate, deleteCourseEvaluationTemplate } from "@/api/suhyeon/evaluationApi"
import { Input } from "@/components/ui/input"

export default function courseEvaluation() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [lectures, setLectures] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  
  // 모달 상태
  const [showEvaluationModal, setShowEvaluationModal] = useState(false)
  const [showTemplateModal, setShowTemplateModal] = useState(false)
  const [selectedLecture, setSelectedLecture] = useState(null)
  const [selectedTemplateGroupId, setSelectedTemplateGroupId] = useState(null)
  const [selectedTemplate, setSelectedTemplate] = useState("")
  const [selectedEvalDate, setSelectedEvalDate] = useState(null)
  const [evalDateSearchTerm, setEvalDateSearchTerm] = useState("")

  // 템플릿 목록
  const [templates, setTemplates] = useState([])
  const [templatesLoading, setTemplatesLoading] = useState(false)
  const [expandedTemplate, setExpandedTemplate] = useState(null)

  // 새로운 상태 변수들
  const [expandedLectures, setExpandedLectures] = useState(new Set())
  const [selectedOpenDate, setSelectedOpenDate] = useState("")
  const [selectedCloseDate, setSelectedCloseDate] = useState("")
  const [editingTemplateGroupId, setEditingTemplateGroupId] = useState(null)

  // 답변 데이터 그룹화 함수 (새로운 구조에 맞게 수정)
  const processTemplateData = (templateData) => {
    if (!templateData.answerList || templateData.answerList.length === 0) {
      return {
        ...templateData,
        groupedAnswers: null,
        hasAnswers: false
      };
    }

    // 답변을 evalQuestionId로 그룹화
    const groupedAnswers = templateData.answerList.reduce((groups, answer) => {
      const evalQuestionId = answer.evalQuestionId;
      
      if (!groups[evalQuestionId]) {
        groups[evalQuestionId] = {
          questionNum: answer.questionNum,
          evalQuestionId: answer.evalQuestionId,
          evalQuestionText: answer.evalQuestionText,
          evalQuestionType: answer.evalQuestionType,
          answers: [],
          responseCount: 0
        };
      }
      
      groups[evalQuestionId].answers.push(answer);
      groups[evalQuestionId].responseCount++;
      
      return groups;
    }, {});

    // questionNum으로 정렬
    const sortedGroupedAnswers = Object.values(groupedAnswers).sort((a, b) => a.questionNum - b.questionNum)
    
    return {
      ...templateData,
      groupedAnswers: sortedGroupedAnswers,
      hasAnswers: true
    };
  };

  // 설문 데이터 (새로운 구조에 맞게 수정)
  const getSurveyData = (lecture, template = null) => {
    // 특정 템플릿에 대한 설문 데이터
    if (template) {
      const processedTemplate = processTemplateData(template)
      
      // 응답자 수 계산
      const uniqueRespondents = new Set(template.answerList?.map(answer => answer.memberId) || [])
      const responseCount = uniqueRespondents.size
      
      // 문항별 데이터 처리 - 중복 제거 및 questionNum으로 정렬
      const uniqueQuestions = template.questionList.reduce((acc, question) => {
        const existingQuestion = acc.find(q => q.evalQuestionId === question.evalQuestionId);
        if (!existingQuestion) {
          acc.push(question);
        }
        return acc;
      }, []);
      
      // questionNum으로 정렬
      const sortedQuestions = uniqueQuestions.sort((a, b) => a.questionNum - b.questionNum);
      
      const questions = sortedQuestions.map((question) => {
        const groupedQuestion = processedTemplate.groupedAnswers?.find(gq => gq.evalQuestionId === question.evalQuestionId)
        
        let averageScore = 0
        let subjectiveAnswers = []
        let responseCount = 0
        
        if (groupedQuestion && groupedQuestion.answers.length > 0) {
          responseCount = groupedQuestion.responseCount
          
          if (question.evalQuestionType === 0) { // 객관식 (점수)
            const scores = groupedQuestion.answers.map(answer => {
              if (answer.answerText && answer.answerText.trim() !== "") {
                return parseInt(answer.answerText) || 0
              } else {
                return answer.score || 0
              }
            })
            averageScore = scores.length > 0 ? scores.reduce((sum, score) => sum + score, 0) / scores.length : 0
          } else if (question.evalQuestionType === 1) { // 주관식 (텍스트)
            subjectiveAnswers = groupedQuestion.answers.map(answer => ({
              memberName: answer.memberName,
              answerText: answer.answerText,
              createdAt: answer.createdAt,
              memberId: answer.memberId
            }))
          }
        }
        
        return {
          id: question.evalQuestionId || `question-${question.questionNum || Math.random()}`,
          type: question.evalQuestionType === 0 ? "rating" : "subjective",
          question: question.evalQuestionText,
          averageScore: averageScore,
          responseCount: responseCount,
          subjectiveAnswers: subjectiveAnswers,
          questionNum: question.questionNum,
          groupedAnswers: groupedQuestion?.answers || []
        }
      })
      
      // 전체 평균 점수 계산 (5점 척도 문항만)
      const ratingQuestions = questions.filter(q => q.type === "rating")
      const totalAverageScore = ratingQuestions.length > 0 
        ? ratingQuestions.reduce((sum, q) => sum + q.averageScore, 0) / ratingQuestions.length 
        : 0
      
      return {
        id: template.templateGroupId,
        title: `${lecture.courseName} - ${template.questionTemplateName}`,
        startDate: template.openDate,
        endDate: template.closeDate,
        status: getTemplateStatus(template),
        totalQuestions: questions.length,
        responses: responseCount,
        responseRate: lecture.studentCount > 0 ? Math.round((responseCount / lecture.studentCount) * 100) : 0,
        averageScore: totalAverageScore,
        questions: questions,
        comments: [],
        hasAnswers: processedTemplate.hasAnswers,
        templateGroupId: template.templateGroupId,
        questionTemplateNum: template.questionTemplateNum
      }
    }
    
    // 전체 과정에 대한 설문 데이터 (기본값)
    return {
      id: lecture.courseId,
      title: `${lecture.courseName} 과정 만족도 조사`,
      startDate: lecture.courseStartDay,
      endDate: lecture.courseEndDay,
      status: "진행중",
      totalQuestions: 0,
      responses: 0,
      responseRate: 0,
      averageScore: 0,
      questions: [],
      comments: [],
      hasAnswers: false
    }
  }

  // 템플릿 상태 계산 함수
  const getTemplateStatus = (template) => {
    const today = getTodayString()
    
    if (today > template.closeDate) {
      return "완료"
    } else if (today >= template.openDate && today <= template.closeDate) {
      return "진행중"
    } else {
      return "예정"
    }
  }

  // 과정 토글 상태 관리
  const toggleLectureExpansion = (courseId) => {
    const newExpanded = new Set(expandedLectures)
    if (newExpanded.has(courseId)) {
      newExpanded.delete(courseId)
    } else {
      newExpanded.add(courseId)
    }
    setExpandedLectures(newExpanded)
  }

  // API에서 과정 목록 불러오기 (새로운 구조에 맞게 수정)
  useEffect(() => {
    const fetchLectures = async () => {
      try {
        setLoading(true)
        const data = await getCourseEvaluation()

        
        // API 응답 데이터를 courseId로 그룹화하여 중복 제거
        const lectureMap = new Map()
        
        data.forEach((evaluation, index) => {

          
          const courseId = evaluation.courseId
          
          if (lectureMap.has(courseId)) {
            // 이미 존재하는 과정인 경우, evaluationTemplates만 추가 (중복 제거)
            const existingLecture = lectureMap.get(courseId)
            if (evaluation.evaluationTemplates && evaluation.evaluationTemplates.length > 0) {
              // templateGroupId를 기준으로 중복 제거
              const existingTemplateIds = new Set(existingLecture.evaluationTemplates.map(t => t.templateGroupId))
              const newTemplates = evaluation.evaluationTemplates.filter(t => !existingTemplateIds.has(t.templateGroupId))
              existingLecture.evaluationTemplates.push(...newTemplates)
            }
          } else {
            // 새로운 과정인 경우, 새 객체 생성
            lectureMap.set(courseId, {
              courseId: evaluation.courseId,
              courseCode: evaluation.courseCode,
              courseName: evaluation.courseName,
              memberId: evaluation.memberId,
              memberName: evaluation.memberName,
              educationId: evaluation.educationId,
              maxCapacity: evaluation.maxCapacity,
              studentCount: evaluation.studentCount,
              courseStartDay: evaluation.courseStartDay,
              courseEndDay: evaluation.courseEndDay,
              evaluationTemplates: evaluation.evaluationTemplates || [],
            })
          }
        })
        
        const formattedLectures = Array.from(lectureMap.values())
        
        setLectures(formattedLectures)
      } catch (err) {
        console.error("과정 평가 목록 불러오기 실패:", err)
        setError("과정 평가 목록을 불러오는데 실패했습니다.")
      } finally {
        setLoading(false)
      }
    }

    fetchLectures()
  }, [])



  // 과정 평가 상세보기
  const handleView = (lectureId, templateGroupId = null) => {
    const lecture = lectures.find(l => l.courseId === lectureId)
    if (lecture) {
      setSelectedLecture(lecture)
      setSelectedTemplateGroupId(templateGroupId) // 선택된 템플릿 ID 저장
      setShowEvaluationModal(true)
    }
  }

  // 템플릿 설정
  const handleTemplateSettings = async (lectureId, template = null) => {
    const lecture = lectures.find(l => l.courseId === lectureId)
    
    if (lecture) {
      
      setSelectedLecture(lecture)
      setShowTemplateModal(true)
      
      // 편집 모드인 경우 기존 템플릿 정보 설정
      if (template) {
        setSelectedTemplate(template.questionTemplateNum)
        setSelectedOpenDate(template.openDate)
        setSelectedCloseDate(template.closeDate)
        setEditingTemplateGroupId(template.templateGroupId)
      } else {
        // 새로 추가하는 경우 초기화
        setSelectedTemplate("")
        setSelectedOpenDate("")
        setSelectedCloseDate("")
        setEditingTemplateGroupId(null)
      }
      
      // 템플릿 목록 불러오기
      try {
        setTemplatesLoading(true)
        const templateData = await getEvaluationTemplateList()
        
        if (templateData && templateData.length > 0) {
          templateData.forEach((temp, index) => {
          })
        }
        
        setTemplates(templateData || [])
      } catch (err) {
        console.error("템플릿 목록 불러오기 실패:", err)
        console.error("에러 상세:", err.response?.data || err.message)
        setTemplates([])
      } finally {
        setTemplatesLoading(false)
      }
    } else {
      console.error("과정를 찾을 수 없음 - lectureId:", lectureId)
    }
  }

  // 과정 평가 일정 추가/수정
  const handleTemplateSelect = async () => {
    if (selectedTemplate && selectedLecture) {
      // 날짜 필수 확인
      if (!selectedOpenDate || !selectedCloseDate) {
        alert("평가 시작일과 종료일을 모두 선택해주세요.")
        return
      }
      
      // 날짜 유효성 검사
      if (!isDateValid(selectedOpenDate, selectedCloseDate, selectedLecture.courseEndDay)) {
        alert("평가 시작일은 오늘 이후부터, 종료일은 과정 종료일 이전까지 선택 가능합니다.")
        return
      }
      
      try {
        
        // 편집 모드인지 확인 (기존 템플릿이 선택되어 있는지)
        const editMode = isEditMode()
        
        let result
        if (editMode) {
          // 편집 모드일 때는 templateGroupId를 포함한 완전한 데이터 전달
          const templateData = {
            templateGroupId: editingTemplateGroupId,
            courseId: selectedLecture.courseId,
            openDate: selectedOpenDate,
            closeDate: selectedCloseDate,
            questionTemplateNum: selectedTemplate
          }
                  result = await updateCourseEvaluationTemplate(templateData)
      } else {
        // 새로 추가하는 경우 기존 구조 유지
        const templateData = {
          courseId: selectedLecture.courseId,
          questionTemplateNum: selectedTemplate,
          openDate: selectedOpenDate,
          closeDate: selectedCloseDate
        }
        result = await setCourseEvaluationTemplate(templateData)
      }
        
        // 성공 시 모달 닫기
        setShowTemplateModal(false)
        setSelectedTemplate("")
        setSelectedLecture(null)
        setSelectedOpenDate("")
        setSelectedCloseDate("")
        setEvalDateSearchTerm("")
        
        // 성공 메시지 표시
        alert(editMode ? "템플릿이 성공적으로 수정되었습니다." : "템플릿이 성공적으로 설정되었습니다.")
        
        // 페이지 새로고침
        window.location.reload()
        
      } catch (error) {
        console.error("템플릿 설정 실패:", error)
        alert("템플릿 설정에 실패했습니다. 다시 시도해주세요.")
      }
    }
  }

  // 모달 닫기
  const closeModals = () => {
    setShowEvaluationModal(false)
    setShowTemplateModal(false)
    setSelectedLecture(null)
    setSelectedTemplateGroupId(null)
    setSelectedTemplate("")
    setExpandedTemplate(null)
    setSelectedOpenDate("")
    setSelectedCloseDate("")
    setEvalDateSearchTerm("")
    setEditingTemplateGroupId(null)
  }

  // 오늘 날짜를 YYYY-MM-DD 형식으로 가져오기
  const getTodayString = () => {
    const today = new Date()
    return today.toISOString().slice(0, 10)
  }

  // 날짜가 유효한지 확인 (시작일은 오늘 이후, 종료일은 과정종료일 이전)
  const isDateValid = (openDate, closeDate, courseEndDay) => {
    const today = getTodayString()
    return openDate >= today && closeDate < courseEndDay && openDate < closeDate
  }

  // 편집 모드인지 확인하는 헬퍼 함수
  const isEditMode = () => {
    return editingTemplateGroupId !== null && editingTemplateGroupId !== ""
  }

  // 평가가 시작되었는지 확인하는 헬퍼 함수
  const isEvaluationStarted = (template) => {
    const today = getTodayString()
    return today >= template.openDate
  }

  // 템플릿 삭제 함수
  const handleDeleteTemplate = async (lectureId, template) => {
    if (!confirm("정말로 이 평가 일정을 삭제하시겠습니까?")) {
      return
    }
    try {
      if (!template.templateGroupId) {
        alert("templateGroupId가 없습니다. 삭제할 수 없습니다.")
        return
      }
      
      const result = await deleteCourseEvaluationTemplate(template.templateGroupId)
      if (result && result.resultCode === "SUCCESS") {
        alert("평가 일정이 성공적으로 삭제되었습니다.")
        // 페이지 새로고침
        window.location.reload()
      } else {
        alert("평가 일정 삭제에 실패했습니다.")
      }
    } catch (error) {
      console.error("템플릿 삭제 오류:", error)
      console.error("오류 상세:", error.response?.data || error.message)
      alert("평가 일정 삭제 중 오류가 발생했습니다.")
    }
  }

  // 과정 상태 계산 함수 (새로운 구조에 맞게 수정)
  const getLectureStatus = (lecture) => {
    const today = getTodayString()
    
    // 완료: 과정 종료일이 지난 경우
    if (lecture.courseEndDay && today > lecture.courseEndDay) {
      return "완료"
    }
    
    // 평가 템플릿이 있는지 확인
    if (lecture.evaluationTemplates && lecture.evaluationTemplates.length > 0) {
      // 진행중인 템플릿이 있는지 확인
      const activeTemplate = lecture.evaluationTemplates.find(template => 
        today >= template.openDate && today <= template.closeDate
      )
      if (activeTemplate) {
        return "진행중"
      }
      
      // 예정인 템플릿이 있는지 확인
      const upcomingTemplate = lecture.evaluationTemplates.find(template => 
        today < template.openDate
      )
      if (upcomingTemplate) {
        return "예정"
      }
    }
    
    // 기본값: 예정
    return "예정"
  }

  // 필터링된 과정 목록
  const filteredLectures = lectures.filter((lecture) => {
    const matchesSearch =
      (lecture.courseName || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (lecture.memberName || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (lecture.courseCode || "").toLowerCase().includes(searchTerm.toLowerCase())
    
    const lectureStatus = getLectureStatus(lecture)
    const matchesStatus = selectedStatus === "all" || lectureStatus === selectedStatus

    return matchesSearch && matchesStatus
  }).sort((a, b) => {
    // 과정 시작일 기준으로 정렬 (시작일이 가까울수록 위로)
    const startDateA = new Date(a.courseStartDay || '9999-12-31')
    const startDateB = new Date(b.courseStartDay || '9999-12-31')
    return startDateA - startDateB
  })

  // 통계 계산 (개별 템플릿 기준)
  const totalLectures = filteredLectures.length
  const today = getTodayString()
  
  // 모든 템플릿의 상태를 개별적으로 계산
  let activeSurveys = 0
  let completedSurveys = 0
  let totalTemplates = 0
  
  filteredLectures.forEach((l) => {
    if (l.evaluationTemplates && l.evaluationTemplates.length > 0) {
      // 각 템플릿의 상태를 개별적으로 확인
      l.evaluationTemplates.forEach(template => {
        totalTemplates++
        
        // 템플릿 상태 계산
        const isActive = template.openDate && template.closeDate && 
                        today >= template.openDate && today <= template.closeDate
        
        const isCompleted = template.closeDate && today > template.closeDate
        
        if (isActive) {
          activeSurveys++
        } else if (isCompleted) {
          completedSurveys++
        }
      })
    }
  })

  return (
    <PageLayout currentPage="evaluations">
      <div className="flex">
        <Sidebar title="설문 평가 관리" menuItems={evaluationMenuItems} currentPath="/evaluations/course" />
        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
          {/* 페이지 헤더 */}
          <div className="mb-6">
            <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
            과정 리스트
            </h1>
            <p className="text-gray-600">전체 과정 목록과 설문 평가 현황을 관리하세요.</p>
          </div>

          {/* 통계 카드 */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div className="ml-4">
                    <p className="text-m font-medium text-[#2C3E50]">전체 과정</p>
                    <p className="text-3xl font-bold" style={{ color: "#3498db" }}>
                      {totalLectures}개
                    </p>
                  </div>
                  <div className="bg-[#EFF6FF] rounded-full p-2 mr-4">
                    <Library className="w-10 h-10 text-[#3498db] m-1" />
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card >
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div className="ml-4">
                    <p className="text-m font-medium text-[#2C3E50]">진행중인 설문</p>
                    <p className="text-3xl font-bold" style={{ color: "#1abc9c" }}>
                      {activeSurveys}개
                    </p>
                    <p className="text-xs text-gray-500 mt-1">평가 기간 내</p>
                  </div>
                  <div className="bg-[#e4f5eb] rounded-full p-2 mr-4">
                    <Clock className="w-10 h-10 text-[#1abc9c] m-1" />
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card >
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div className="ml-4">
                    <p className="text-m font-medium text-[#2C3E50]">완료된 설문</p>
                    <p className="text-3xl font-bold" style={{ color: "#b0c4de" }}>
                      {completedSurveys}개
                    </p>
                    <p className="text-xs text-gray-500 mt-1">평가 기간 종료</p>
                  </div>
                  <div className="bg-[#f5f5f5] rounded-full p-2 mr-4">
                    <CheckCircle className="w-10 h-10 text-[#b0c4de] m-1" />
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 검색 및 필터 */}
          <div className="bg-white p-4 rounded-lg shadow-sm mb-6">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                <input
                  type="text"
                  placeholder="과정명, 강사명, 과정코드로 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                />
              </div>
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
              >
                <option value="all">전체 상태</option>
                <option value="진행중">진행중</option>
                <option value="완료">완료</option>
                <option value="예정">예정</option>
              </select>
            </div>
          </div>

          {/* 로딩 상태 */}
          {loading && (
            <div className="bg-white rounded-lg shadow-sm p-8">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-500 mx-auto mb-4"></div>
                <p className="text-gray-600">과정 목록을 불러오는 중...</p>
              </div>
            </div>
          )}

          {/* 에러 상태 */}
          {error && (
            <div className="bg-white rounded-lg shadow-sm p-8">
              <div className="text-center">
                <p className="text-red-600 mb-4">{error}</p>
                <Button 
                  onClick={() => window.location.reload()} 
                  className="bg-red-500 hover:bg-red-600 text-white"
                >
                  다시 시도
                </Button>
              </div>
            </div>
          )}

          {/* 과정 목록 테이블 */}
          {!loading && !error && (
            <div className="bg-white rounded-lg shadow-sm overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead style={{ backgroundColor: "#f8f9fa" }}>
                    <tr>
                      <th className="py-3 px-4 text-left text-sm font-medium text-gray-700">과정 정보</th>
                      <th className="py-3 px-4 text-left text-sm font-medium text-gray-700">강사</th>
                      <th className="py-3 px-4 text-left text-sm font-medium text-gray-700">기간</th>
                      <th className="py-3 px-4 text-left text-sm font-medium text-gray-700">수강생</th>
                      <th className="py-3 px-4 text-left text-sm font-medium text-gray-700">설문 평가</th>
                      <th className="py-3 px-4 text-center text-sm font-medium text-gray-700">관리</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {filteredLectures.map((lecture) => (
                      <React.Fragment key={lecture.courseId}>
                        <tr className="hover:bg-gray-50">
                          <td className="py-3 px-4">
                            <div className="flex items-center gap-2">                              
                              <div>
                                <div className="font-medium text-gray-900">{lecture.courseName}</div>
                                <div className="text-sm text-gray-500">{lecture.courseCode}</div>
                              </div>
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <div className="text-sm text-gray-900">{lecture.memberName}</div>
                          </td>
                          <td className="py-3 px-4">
                            <div className="text-sm text-gray-900">
                              {lecture.courseStartDay} ~ {lecture.courseEndDay}
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <div className="text-sm text-gray-900">
                              {lecture.studentCount || 0}/{lecture.maxCapacity}명
                            </div>
                            <div className="w-full bg-gray-200 rounded-full h-2 mt-1 overflow-hidden">
                              <div
                                className="h-2 rounded-full max-w-full"
                                style={{
                                  width: `${lecture.maxCapacity > 0 ? ((lecture.studentCount || 0) / lecture.maxCapacity) * 100 : 0}%`,
                                  backgroundColor: "#1abc9c",
                                }}
                              ></div>
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <div>
                              <div className="text-sm font-medium text-gray-900">
                                {lecture.evaluationTemplates?.length > 0 
                                  ? `평가일정 ${lecture.evaluationTemplates.length}개` 
                                  : "평가일정 없음"}
                              </div>
                              {lecture.evaluationTemplates?.length > 0 && (
                                <div className="text-xs text-gray-500 mt-1">
                                  {lecture.evaluationTemplates.map(template => template.questionTemplateName).join(", ")}
                                </div>
                              )}
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <div className="flex justify-center">
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => toggleLectureExpansion(lecture.courseId)}
                                className="p-1 hover:scale-110 transition-transform duration-300 hover:bg-blue-50"
                                title="상세보기"
                              >
                                {expandedLectures.has(lecture.courseId) ? (
                                  <ChevronDown className="w-4 h-4" style={{ color: "#1abc9c" }} />
                                ) : (
                                  <ChevronRight className="w-4 h-4" style={{ color: "#1abc9c" }} />
                                )}
                              </Button>
                            </div>
                          </td>
                        </tr>
                        
                        {/* 확장된 템플릿 목록 */}
                        {expandedLectures.has(lecture.courseId) && (
                          <tr>
                            <td colSpan="7" className="bg-gray-50 p-0">
                              <div className="p-4">
                                <div className="flex justify-between items-center mb-4">
                                  <h4 className="text-sm font-medium text-gray-900">평가 템플릿 목록</h4>
                                  <div className="flex gap-2">
                                  
                                    <Button
                                      size="sm"
                                      variant="outline"
                                      onClick={() => handleTemplateSettings(lecture.courseId)}
                                      className="text-xs"
                                    >
                                      <Plus className="w-3 h-3 mr-1" />
                                      평가 일정 추가
                                    </Button>
                                  </div>
                                </div>
                                
                                {lecture.evaluationTemplates && lecture.evaluationTemplates.length > 0 ? (
                                  <div className="space-y-3">
                                    {lecture.evaluationTemplates.map((template, index) => (
                                      <div key={`${lecture.courseId}-${template.templateGroupId}-${index}`} className="bg-white rounded-lg border border-gray-200 p-4">
                                        <div className="flex justify-between items-start">
                                          <div className="flex-1">
                                            <div className="flex items-center gap-3 mb-2">
                                              <Badge 
                                                className={`px-2 py-1 text-xs ${
                                                  getTemplateStatus(template) === "진행중" 
                                                    ? "bg-green-100 text-green-800" 
                                                    : getTemplateStatus(template) === "완료"
                                                    ? "bg-gray-100 text-gray-800"
                                                    : "bg-blue-100 text-blue-800"
                                                }`}
                                              >
                                                {getTemplateStatus(template)}
                                              </Badge>
                                              <h5 className="font-medium text-gray-900">{template.questionTemplateName}</h5>
                                            </div>
                                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                                              <div>
                                                <span className="text-gray-500">평가 기간:</span>
                                                <div className="font-medium">{template.openDate} ~ {template.closeDate}</div>
                                              </div>
                                              <div>
                                                <span className="text-gray-500">문항 수:</span>
                                                <div className="font-medium">{template.questionList?.length || 0}개</div>
                                              </div>
                                              <div>
                                                <span className="text-gray-500">응답 수:</span>
                                                <div className="font-medium">
                                                  {(() => {
                                                    const uniqueRespondents = new Set(template.answerList?.map(answer => answer.memberId) || []);
                                                    return `${uniqueRespondents.size}/${lecture.studentCount || 0}명`;
                                                  })()}
                                                </div>
                                              </div>
                                            </div>
                                          </div>
                                          <div className="flex flex-col items-center gap-1">
                                            <div className="flex items-center gap-2">
                                              <Button
                                                size="sm"
                                                variant="ghost"
                                                onClick={() => handleView(lecture.courseId, template.templateGroupId)}
                                                className="p-1 hover:bg-blue-50"
                                                title="상세보기"
                                              >
                                                <Eye className="w-4 h-4 text-blue-600" />
                                              </Button>
                                              <Button
                                                size="sm"
                                                variant="ghost"
                                                onClick={() => handleTemplateSettings(lecture.courseId, template)}
                                                className={`p-1 ${
                                                  isEvaluationStarted(template) 
                                                    ? 'opacity-30 cursor-not-allowed bg-gray-100' 
                                                    : 'hover:bg-green-50'
                                                }`}
                                                title={isEvaluationStarted(template) ? "평가가 시작되어 수정할 수 없습니다" : "수정"}
                                                disabled={isEvaluationStarted(template)}
                                              >
                                                <Edit className={`w-4 h-4 ${
                                                  isEvaluationStarted(template) 
                                                    ? 'text-gray-400' 
                                                    : 'text-green-600'
                                                }`} />
                                              </Button>
                                              <Button
                                                size="sm"
                                                variant="ghost"
                                                onClick={() => handleDeleteTemplate(lecture.courseId, template)}
                                                className={`p-1 ${
                                                  isEvaluationStarted(template) 
                                                    ? 'opacity-30 cursor-not-allowed bg-gray-100' 
                                                    : 'hover:bg-red-50'
                                                }`}
                                                title={isEvaluationStarted(template) ? "평가가 시작되어 삭제할 수 없습니다" : "삭제"}
                                                disabled={isEvaluationStarted(template)}
                                              >
                                                <Trash2 className={`w-4 h-4 ${
                                                  isEvaluationStarted(template) 
                                                    ? 'text-gray-400' 
                                                    : 'text-red-600'
                                                }`} />
                                              </Button>
                                            </div>
                                            {isEvaluationStarted(template) && (
                                              <div className="text-xs text-gray-500 bg-gray-50 px-2 py-1 rounded-md border border-gray-200">
                                                평가 진행 중
                                              </div>
                                            )}
                                          </div>
                                        </div>
                                      </div>
                                    ))}
                                  </div>
                                ) : (
                                  <div className="text-center py-8 bg-white rounded-lg border border-gray-200">
                                    <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-3">
                                      <MessageSquare className="w-6 h-6 text-gray-400" />
                                    </div>
                                    <p className="text-gray-500 font-medium">설정된 평가 템플릿이 없습니다.</p>
                                    <p className="text-sm text-gray-400 mt-1">평가 일정을 추가해주세요.</p>
                                  </div>
                                )}
                              </div>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* 결과가 없을 때 */}
          {!loading && !error && filteredLectures.length === 0 && (
            <div className="text-center py-8">
              <p className="text-gray-500">검색 조건에 맞는 과정이 없습니다.</p>
            </div>
          )}

          {/* 과정 평가 상세 모달 */}
          {showEvaluationModal && selectedLecture && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
              <div className="bg-white rounded-xl max-w-6xl w-full max-h-[90vh] overflow-y-auto shadow-2xl border border-gray-200">
                {/* 모달 헤더 */}
                <div className="flex justify-between items-center p-6 border-b border-gray-200 bg-gradient-to-r from-gray-50 to-white">
                  <div>
                    <h2 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>{selectedLecture.courseName} 설문 상세</h2>
                    <p className="text-gray-600 mt-1 flex items-center gap-2">
                      <span className="px-2 py-1 bg-gray-100 rounded-md text-sm font-medium">{selectedLecture.courseCode}</span>
                      <span className="text-gray-400">•</span>
                      <span>{selectedLecture.category || "기타"}</span>
                    </p>
                  </div>
                  <Button variant="ghost" onClick={closeModals} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                    <X className="w-5 h-5 text-gray-500" />
                  </Button>
                </div>

                {/* 모달 내용 */}
                <div className="p-6 bg-gray-50">
                  {/* 과정 기본 정보 */}
                  <div className="bg-white rounded-xl p-6 mb-6 shadow-sm border border-gray-200">
                    <h3 className="text-xl font-bold mb-6" style={{ color: "#2C3E50" }}>과정 정보</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                      <div className="bg-gray-50 rounded-lg p-4">
                        <p className="text-sm font-medium text-gray-600 mb-2">과정 기간</p>
                        <p className="font-semibold text-gray-900">
                          {selectedLecture.courseStartDay} ~ {selectedLecture.courseEndDay}
                        </p>
                      </div>
                      <div className="bg-gray-50 rounded-lg p-4">
                        <p className="text-sm font-medium text-gray-600 mb-2">강사</p>
                        <p className="font-semibold text-gray-900">{selectedLecture.memberName}</p>
                      </div>
                      <div className="bg-gray-50 rounded-lg p-4">
                        <p className="text-sm font-medium text-gray-600 mb-2">수강생</p>
                        <p className="font-semibold text-gray-900">{selectedLecture.studentCount || 0}/{selectedLecture.maxCapacity}명</p>
                      </div>
                      <div className="bg-gray-50 rounded-lg p-4">
                        <p className="text-sm font-medium text-gray-600 mb-2">평가 템플릿</p>
                        <div className="flex items-center gap-2">
                          <p className="font-semibold text-gray-900">
                            {selectedLecture.evaluationTemplates?.length > 0 
                              ? `${selectedLecture.evaluationTemplates.length}개` 
                              : "0개"}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 평가 템플릿별 상세 정보 */}
                  {selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0 ? (
                    <div className="space-y-6">
                      {selectedLecture.evaluationTemplates
                        .filter(template => !selectedTemplateGroupId || template.templateGroupId === selectedTemplateGroupId)
                        .map((template, templateIndex) => {
                        const surveyData = getSurveyData(selectedLecture, template)
                        return (
                          <div key={`${selectedLecture.courseId}-${template.templateGroupId}-${templateIndex}`} className="bg-white rounded-xl p-6 shadow-sm border border-gray-200">
                            {/* 템플릿 헤더 */}
                            <div className="flex justify-between items-start mb-6">
                              <div>
                                <h3 className="text-xl font-bold mb-2" style={{ color: "#2C3E50" }}>{surveyData.title}</h3>
                                <div className="flex items-center gap-2">
                                  <span className="px-3 py-1 bg-gray-100 rounded-full text-sm font-medium">
                                    평가 기간: {template.openDate} ~ {template.closeDate}
                                  </span>
                                  <Badge className={`px-3 py-1 rounded-full font-medium ${
                                    surveyData.status === "진행중" 
                                      ? "bg-green-100 text-green-800" 
                                      : surveyData.status === "완료"
                                      ? "bg-gray-100 text-gray-800"
                                      : "bg-blue-100 text-blue-800"
                                  }`}>
                                    {surveyData.status}
                                  </Badge>
                                </div>
                              </div>
                            </div>

                            {/* 설문 통계 */}
                            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                              <div className="bg-white p-4 rounded-lg border border-gray-200 hover:shadow-md transition-shadow">
                                <div className="flex items-center gap-3">
                                  <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                                    <BarChart2 className="w-5 h-5 text-blue-600" />
                                  </div>
                                  <div>
                                    <p className="text-sm text-gray-600">응답률</p>
                                    <p className="text-xl font-semibold text-gray-900">{surveyData.responseRate}%</p>
                                    <p className="text-xs text-gray-500">
                                      {surveyData.responses}/{selectedLecture.studentCount || 0}명
                                    </p>
                                  </div>
                                </div>
                              </div>
                              <div className="bg-white p-4 rounded-lg border border-gray-200 hover:shadow-md transition-shadow">
                                <div className="flex items-center gap-3">
                                  <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                                    <Star className="w-5 h-5 text-green-600" />
                                  </div>
                                  <div>
                                    <p className="text-sm text-gray-600">평균 만족도</p>
                                    <p className="text-xl font-semibold text-gray-900">{surveyData.averageScore ? surveyData.averageScore.toFixed(1) : "응답 없음"}/5.0</p>
                                    <p className="text-xs text-gray-500">전체 평균</p>
                                  </div>
                                </div>
                              </div>
                              <div className="bg-white p-4 rounded-lg border border-gray-200 hover:shadow-md transition-shadow">
                                <div className="flex items-center gap-3">
                                  <div className="w-10 h-10 bg-blue-50 rounded-lg flex items-center justify-center">
                                    <MessageSquare className="w-5 h-5 text-[#b0c4de]" />
                                  </div>
                                  <div>
                                    <p className="text-sm text-gray-600">설문 문항</p>
                                    <p className="text-xl font-semibold text-gray-900">{surveyData.totalQuestions}개</p>
                                    <p className="text-xs text-gray-500">총 문항 수</p>
                                  </div>
                                </div>
                              </div>
                              <div className="bg-white p-4 rounded-lg border border-gray-200 hover:shadow-md transition-shadow">
                                <div className="flex items-center gap-3">
                                  <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
                                    <MessageCircle className="w-5 h-5 text-[#7f8c8d]" />
                                  </div>
                                  <div>
                                    <p className="text-sm text-gray-600">주관식 답변</p>
                                    <p className="text-xl font-semibold text-gray-900">
                                      {surveyData.questions.filter(q => q.type === "subjective").length}개
                                    </p>
                                    <p className="text-xs text-gray-500">주관식 문항</p>
                                  </div>
                                </div>
                              </div>
                            </div>

                            {/* 문항별 결과 */}
                            <div className="bg-gray-50 rounded-xl p-6 mb-6 border border-gray-200">
                              <h4 className="text-lg font-bold mb-6" style={{ color: "#2C3E50" }}>문항별 설문 결과</h4>
                              {surveyData.questions.length > 0 ? (
                                <div className="space-y-6">
                                  {surveyData.questions.map((question, index) => (
                                    <div key={`${surveyData.templateGroupId}-${question.id}-${index}`} className="bg-white rounded-xl p-6 mb-6 border border-gray-200">
                                      <div className="flex justify-between items-start mb-4">
                                        <div className="flex-1">
                                          <div className="flex items-start gap-3 mb-4">
                                            <div className="w-6 h-6 bg-gray-200 text-gray-700 rounded flex items-center justify-center text-xs font-medium flex-shrink-0 mt-1">
                                              {index + 1}
                                            </div>
                                            <div className="flex-1">
                                              <h5 className="text-base font-medium text-gray-900 mb-2">
                                                {question.question}
                                              </h5>
                                              {/* 문항 타입에 따라 다른 표시 */}
                                              {question.type === "rating" ? (
                                                <div className="flex items-center gap-2">
                                                  <span className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs font-medium">
                                                    5점 척도
                                                  </span>
                                                  <span className="text-sm text-gray-500">
                                                    평균: {question.averageScore ? question.averageScore.toFixed(1) : "응답 없음"}/5.0
                                                  </span>
                                                </div>
                                              ) : (
                                                <div className="flex items-center gap-2">
                                                  <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded text-xs font-medium">
                                                    주관식
                                                  </span>
                                                  <span className="text-sm text-gray-500">
                                                    답변 ({question.subjectiveAnswers?.length || 0}개 응답)
                                                  </span>
                                                </div>
                                              )}
                                            </div>
                                          </div>
                                        </div>
                                      </div>

                                      {/* 문항 타입에 따라 다른 결과 표시 */}
                                      {question.type === "rating" ? (
                                        <div className="space-y-4">
                                          {/* 평균 점수 */}
                                          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                                            <div className="flex items-center gap-4">
                                              <div className="w-12 h-12 bg-[#e4f5eb] rounded-lg flex items-center justify-center">
                                                <Star className="w-6 h-6 text-[#1abc9c]" />
                                              </div>
                                              <div>
                                                <p className="text-sm text-gray-600">평균 점수</p>
                                                <p className="text-lg font-semibold text-gray-900">
                                                  {question.averageScore ? `${question.averageScore.toFixed(1)}/5.0` : "응답 없음"}
                                                </p>
                                                <p className="text-xs text-gray-500">
                                                  {question.averageScore ? "학생들의 평균 평가" : "아직 응답이 없습니다"}
                                                </p>
                                              </div>
                                            </div>
                                          </div>
                                          
                                          {/* 답변 분포 */}
                                          {question.groupedAnswers && question.groupedAnswers.length > 0 && (
                                            <div className="bg-white rounded-lg p-4 border border-gray-200">
                                              <h6 className="text-sm font-medium text-gray-700 mb-3">답변 분포</h6>
                                              <div className="space-y-2">
                                                {[5, 4, 3, 2, 1].map((score) => {
                                                  const scoreAnswers = question.groupedAnswers.filter(answer => {
                                                    let answerScore = 0
                                                    if (answer.answerText && answer.answerText.trim() !== "") {
                                                      answerScore = parseInt(answer.answerText) || 0
                                                    } else {
                                                      answerScore = answer.score || 0
                                                    }
                                                    return answerScore === score
                                                  })
                                                  const count = scoreAnswers.length
                                                  const percentage = question.groupedAnswers.length > 0 ? Math.round((count / question.groupedAnswers.length) * 100) : 0
                                                  
                                                  return (
                                                    <div key={score} className="flex items-center gap-4">
                                                      <div className="w-8 text-sm text-gray-600 font-medium">{score}점</div>
                                                      <div className="flex-1 bg-gray-200 rounded-full h-4 relative">
                                                        <div
                                                          className="bg-blue-500 h-4 rounded-full flex items-center justify-end pr-2"
                                                          style={{ width: `${percentage}%` }}
                                                        >
                                                          {percentage > 15 && (
                                                            <span className="text-white text-xs font-medium">{count}명</span>
                                                          )}
                                                        </div>
                                                        {percentage <= 15 && count > 0 && (
                                                          <span className="absolute right-2 top-1/2 transform -translate-y-1/2 text-xs text-gray-600">
                                                            {count}명
                                                          </span>
                                                        )}
                                                      </div>
                                                      <div className="w-12 text-sm text-gray-600 text-right">{percentage}%</div>
                                                    </div>
                                                  )
                                                })}
                                              </div>
                                            </div>
                                          )}
                                        </div>
                                      ) : (
                                        // 주관식 답변 목록
                                        <div className="space-y-3">
                                          {question.subjectiveAnswers && question.subjectiveAnswers.length > 0 ? (
                                            question.subjectiveAnswers.map((answer, answerIndex) => (
                                              <div key={`${surveyData.templateGroupId}-${question.id}-answer-${answerIndex}`} className="bg-white rounded-lg p-3 border border-gray-200">
                                                <div className="flex justify-between items-start mb-2">
                                                  <span className="text-sm font-medium text-gray-700">
                                                    {answer.memberName || `학생 ${answerIndex + 1}`}
                                                  </span>
                                                  <span className="text-xs text-gray-500">
                                                    {answer.createdAt ? new Date(answer.createdAt).toLocaleDateString() : "날짜 없음"}
                                                  </span>
                                                </div>
                                                <p className="text-gray-900">{answer.answerText || "답변 내용 없음"}</p>
                                              </div>
                                            ))
                                          ) : (
                                            <div className="text-center py-4 text-gray-500">
                                              아직 주관식 답변이 없습니다.
                                            </div>
                                          )}
                                        </div>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              ) : (
                                <div className="text-center py-8">
                                  <div className="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <MessageSquare className="w-8 h-8 text-[#b0c4de]" />
                                  </div>
                                  <p className="text-gray-500 font-medium">
                                    {surveyData.hasAnswers ? "설문 문항이 없습니다." : "아직 설문 결과가 없습니다."}
                                  </p>
                                </div>
                              )}
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  ) : (
                    <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-200">
                      <div className="text-center py-12">
                        <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                          <MessageSquare className="w-8 h-8 text-gray-400" />
                        </div>
                        <p className="text-gray-500 font-medium">설정된 평가 템플릿이 없습니다.</p>
                        <p className="text-sm text-gray-400 mt-2">과정 평가를 위해 템플릿을 먼저 설정해주세요.</p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* 과정 평가 일정 추가 모달 */}
          {showTemplateModal && selectedLecture && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-end justify-center z-50 p-4" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
              <div className="bg-white rounded-lg max-w-4xl w-full flex flex-col" style={{ height: '80vh', minHeight: '500px', maxHeight: '80vh' }}>
                {/* 모달 헤더 */}
                <div className="flex justify-between items-center p-6 border-b">
                  <div>
                    <h2 className="text-xl font-bold text-gray-900">
                      {isEditMode() ? "과정 평가 일정 수정" : "과정 평가 일정 추가"}
                    </h2>
                    <p className="text-gray-600 mt-1">
                      {selectedLecture.courseName} 과정의 평가 일정을 {isEditMode() ? "수정" : "추가"}하세요
                    </p>
                  </div>
                  <Button variant="ghost" onClick={closeModals} className="p-2">
                    <X className="w-5 h-5" />
                  </Button>
                </div>

                {/* 날짜 선택 및 템플릿 검색 UI */}
                <div className="px-6 pt-4 pb-2 border-b">
                  <div className="flex flex-col gap-4">
                    <div className="flex items-center gap-4">
                      <span className="font-medium text-gray-700">평가 시작 날짜 <span className="text-red-500">*</span></span>
                      <Input
                        type="date"
                        value={selectedOpenDate}
                        onChange={(e) => setSelectedOpenDate(e.target.value)}
                        className="w-48"
                        required
                        min={getTodayString()}
                        max={selectedLecture?.courseEndDay ? selectedLecture.courseEndDay : undefined}
                      />
                      <span className="font-medium text-gray-700 ml-4">평가 종료 날짜 <span className="text-red-500">*</span></span>
                      <Input
                        type="date"
                        value={selectedCloseDate}
                        onChange={(e) => setSelectedCloseDate(e.target.value)}
                        className="w-48"
                        required
                        min={selectedOpenDate || getTodayString()}
                        max={selectedLecture?.courseEndDay ? selectedLecture.courseEndDay : undefined}
                      />
                    </div>
                    <div className="flex items-center gap-4">
                      <span className="font-medium text-gray-700">템플릿 검색</span>
                      <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                        <Input
                          type="text"
                          placeholder="템플릿 이름으로 검색..."
                          value={evalDateSearchTerm}
                          onChange={(e) => setEvalDateSearchTerm(e.target.value)}
                          className="w-64 pl-10"
                        />
                      </div>
                    </div>
                  </div>
                </div>

                {/* 모달 내용 */}
                <div className="p-6 flex-1 overflow-y-auto">
                  {templatesLoading ? (
                    <div className="text-center py-8">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-4"></div>
                      <p className="text-gray-500">템플릿 목록을 불러오는 중...</p>
                    </div>
                  ) : templates.length > 0 ? (
                    <>
                      <div className="space-y-4">
                        {(() => {
                          const filteredTemplates = templates.filter((template) =>
                            (template.questionTemplateName || template.name || "")
                              .toLowerCase()
                              .includes(evalDateSearchTerm.toLowerCase())
                          )
                          
                          if (filteredTemplates.length === 0) {
                            return (
                              <div className="text-center py-8">
                                <p className="text-gray-500">
                                  "{evalDateSearchTerm}"에 해당하는 템플릿이 없습니다.
                                </p>
                              </div>
                            )
                          }
                          
                          return filteredTemplates.map((template, index) => {
                            const templateId = template.templateId || template.questionTemplateNum || template.id || `template-${index}`
                            const isExpanded = expandedTemplate === templateId
                            const isSelected = selectedTemplate === templateId
                            
                            return (
                              <div
                                key={templateId}
                                className={`border rounded-lg transition-colors ${
                                  isSelected
                                    ? "border-blue-500 bg-blue-50"
                                    : "border-gray-200 hover:border-gray-300"
                                }`}
                              >
                                <div 
                                  className="p-4 cursor-pointer"
                                  onClick={() => setSelectedTemplate(selectedTemplate === templateId ? null : templateId)}
                                >
                                  <div className="flex items-start justify-between">
                                    <div className="flex-1">
                                      <h3 className="font-medium text-gray-900">{template.questionTemplateName || template.name}</h3>
                                      <p className="text-sm text-gray-600 mt-1">
                                        문항 수: {template.questionList?.length || 0}개
                                      </p>
                                    </div>
                                    <div className="flex items-center gap-2">
                                      {isSelected && (
                                        <div className="w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center">
                                          <div className="w-2 h-2 bg-white rounded-full"></div>
                                        </div>
                                      )}
                                      <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={(e) => {
                                          e.stopPropagation()
                                          setExpandedTemplate(isExpanded ? null : templateId)
                                        }}
                                        className="p-1"
                                      >
                                        {isExpanded ? (
                                          <ChevronDown className="w-4 h-4" />
                                        ) : (
                                          <ChevronRight className="w-4 h-4" />
                                        )}
                                      </Button>
                                    </div>
                                  </div>
                                </div>
                                
                                {/* 문항 목록 (토글) */}
                                {isExpanded && template.questionList && (
                                  <div className="border-t border-gray-200 bg-gray-50 p-4">
                                    <h4 className="font-medium text-gray-900 mb-3">문항 목록</h4>
                                    <div className="space-y-2">
                                      {template.questionList.map((question, qIndex) => (
                                        <div key={question.evalQuestionId || `question-${qIndex}`} className="bg-white rounded-lg p-3">
                                          <div className="flex items-start gap-3">
                                            <div className="w-6 h-6 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-xs font-medium">
                                              {question.questionNum}
                                            </div>
                                            <div className="flex-1">
                                              <p className="text-sm text-gray-900">{question.evalQuestionText}</p>
                                              <p className="text-xs text-gray-500 mt-1">
                                                유형: {question.evalQuestionType === 0 ? "객관식" : question.evalQuestionType === 1 ? "주관식" : "기타"}
                                              </p>
                                            </div>
                                          </div>
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}
                              </div>
                            )
                          })
                        })()}
                      </div>
                    </>
                  ) : (
                    <div className="text-center py-8">
                      <p className="text-gray-500">사용 가능한 템플릿이 없습니다.</p>
                    </div>
                  )}
                </div>
                
                {/* 모달 푸터 */}
                <div className="border-t border-gray-200 flex justify-end gap-3 p-6 flex-shrink-0 bg-white">
                  <Button variant="outline" onClick={closeModals}>
                    취소
                  </Button>
                  <Button 
                    onClick={handleTemplateSelect}
                    disabled={!selectedTemplate || !selectedOpenDate || !selectedCloseDate}
                    className="bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white"
                  >
                    {isEditMode() ? "일정 수정" : "일정 추가"}
                  </Button>
                </div>
              </div>
            </div>
          )}


          </div>
        </main>
      </div>
    </PageLayout>
  )
}

