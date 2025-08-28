import { useState, useEffect } from "react"
import { Search, Eye, BarChart3, Users, Calendar, TrendingUp, X, Star, BarChart, Percent, CheckCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import Header from "@/components/layout/header"
import Sidebar from "@/components/layout/sidebar"
import { getEvaluationTemplateList, getInstructorEvaluationList } from "@/api/suhyeon/evaluationApi"

export default function EvaluationCourseList() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [selectedPeriod, setSelectedPeriod] = useState("all")
  const [selectedLecture, setSelectedLecture] = useState(null)
  const [showEvaluationModal, setShowEvaluationModal] = useState(false)
  const [evaluationTemplates, setEvaluationTemplates] = useState([])
  const [isLoadingTemplates, setIsLoadingTemplates] = useState(false)
  const [lectures, setLectures] = useState([])
  const [isLoadingLectures, setIsLoadingLectures] = useState(true)
  const [selectedTemplateIndex, setSelectedTemplateIndex] = useState(0)
  
  // 사이드바 메뉴 항목
  const sidebarItems = [
    { href: "/instructor/evaluation", label: "담당 과정 설문", key: "evaluation" },
  ]

  // API에서 과정 목록 불러오기
  useEffect(() => {
    const fetchLectures = async () => {
      try {
        setIsLoadingLectures(true)
        const data = await getInstructorEvaluationList()
        
        // API 응답 데이터를 컴포넌트에서 사용하는 형식으로 변환
        const formattedLectures = data.map((evaluation) => {
          // evaluationTemplates 배열이 있는지 확인
          const hasTemplates = evaluation.evaluationTemplates && evaluation.evaluationTemplates.length > 0
          
          // 첫 번째 템플릿 정보 추출 (있는 경우)
          const firstTemplate = hasTemplates ? evaluation.evaluationTemplates[0] : null
          
          return {
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
            templateGroupId: evaluation.templateGroupId,
            courseDate: firstTemplate?.openDate || evaluation.courseDate,
            templateId: firstTemplate?.questionTemplateNum || evaluation.templateId,
            questionTemplateName: firstTemplate?.questionTemplateName || evaluation.questionTemplateName,
            // 모든 템플릿의 questionList와 answerList를 병합
            questionList: hasTemplates 
              ? evaluation.evaluationTemplates.flatMap(template => template.questionList || [])
              : (evaluation.questionList || []),
            answerList: hasTemplates 
              ? evaluation.evaluationTemplates.flatMap(template => template.answerList || [])
              : (evaluation.answerList || []),
            // 새로운 구조: evaluationTemplates 배열 추가
            evaluationTemplates: evaluation.evaluationTemplates || [],
            surveyActive: true,
            surveyResponses: 0,
          }
        })
        
        setLectures(formattedLectures)
      } catch (err) {
        console.error("과정 평가 목록 불러오기 실패:", err)
        setLectures([])
      } finally {
        setIsLoadingLectures(false)
      }
    }

    fetchLectures()
  }, [])

  // 과정 상세보기 모달 열기
  const handleViewDetails = async (lecture, templateIndex = 0) => {
    setSelectedLecture(lecture)
    setSelectedTemplateIndex(templateIndex)
    setShowEvaluationModal(true)
    setIsLoadingTemplates(true)
    
    try {
      // API로 평가 템플릿 리스트 불러오기
      const templates = await getEvaluationTemplateList()
      setEvaluationTemplates(templates || [])
    } catch (error) {
      console.error("평가 템플릿을 불러오는 중 오류 발생:", error)
      setEvaluationTemplates([])
    } finally {
      setIsLoadingTemplates(false)
    }
  }

  // 모달 닫기
  const closeModal = () => {
    setShowEvaluationModal(false)
    setSelectedLecture(null)
    setEvaluationTemplates([])
    setSelectedTemplateIndex(0)
  }

  // 설문 응답 보기
  const handleViewResponses = async (lecture) => {
    setSelectedLecture(lecture)
    setShowEvaluationModal(true)
    setIsLoadingTemplates(true)
    
    try {
      // API로 평가 템플릿 리스트 불러오기
      const templates = await getEvaluationTemplateList()
      // 해당 과정의 templateId와 매칭되는 템플릿만 필터링
      const matchedTemplates = templates.filter(template => 
        template.questionTemplateNum === lecture.templateId
      )
      setEvaluationTemplates(matchedTemplates || [])
    } catch (error) {
      console.error("평가 템플릿을 불러오는 중 오류 발생:", error)
      setEvaluationTemplates([])
    } finally {
      setIsLoadingTemplates(false)
    }
  }

  // 오늘 날짜를 YYYY-MM-DD 형식으로 가져오기
  const getTodayString = () => {
    const today = new Date()
    return today.toISOString().slice(0, 10)
  }

  // 과정 상태 계산 함수
  const getLectureStatus = (lecture) => {
    const today = getTodayString()
    
    // 완료: 과정 종료일이 지난 경우
    if (lecture.courseEndDay && today > lecture.courseEndDay) {
      return "완료"
    }
    
    // evaluationTemplates가 있는 경우 첫 번째 템플릿의 날짜 사용
    let courseDate = lecture.courseDate
    if (lecture.evaluationTemplates && lecture.evaluationTemplates.length > 0) {
      courseDate = lecture.evaluationTemplates[0].openDate
    }
    
    // 진행중: 평가 날짜가 설정되어 있고, 평가 날짜부터 과정 종료일까지
    if (courseDate && today >= courseDate && today <= lecture.courseEndDay) {
      return "진행중"
    }
    
    // 예정: courseDate가 설정되지 않았으면서 과정종료일이 지나지 않은 경우
    if (!courseDate && lecture.courseEndDay && today <= lecture.courseEndDay) {
      return "예정"
    }
    
    // 예정: 평가 날짜 전
    if (courseDate && today < courseDate) {
      return "예정"
    }
    
    // 기본값: 예정
    return "예정"
  }

  // 과정 시작/종료일에서 연도 추출하여 기간 필터 옵션 생성
  const getYearOptions = () => {
    const years = new Set()
    
    lectures.forEach(lecture => {
      if (lecture.courseStartDay) {
        const startYear = new Date(lecture.courseStartDay).getFullYear()
        years.add(startYear)
      }
      if (lecture.courseEndDay) {
        const endYear = new Date(lecture.courseEndDay).getFullYear()
        years.add(endYear)
      }
    })
    
    return Array.from(years).sort((a, b) => b - a) // 최신 연도부터 정렬
  }

  // 필터링된 과정 목록
  const filteredLectures = lectures.filter((lecture) => {
    const matchesSearch =
      (lecture.courseName || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (lecture.memberName || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (lecture.courseCode || "").toLowerCase().includes(searchTerm.toLowerCase())
    
    const lectureStatus = getLectureStatus(lecture)
    const matchesStatus = selectedStatus === "all" || lectureStatus === selectedStatus

    // 기간 필터링 (과정 시작일 또는 종료일이 선택된 연도에 포함되는지 확인)
    const matchesPeriod = selectedPeriod === "all" || (() => {
      if (!lecture.courseStartDay && !lecture.courseEndDay) return false
      
      const selectedYear = parseInt(selectedPeriod)
      const startYear = lecture.courseStartDay ? new Date(lecture.courseStartDay).getFullYear() : null
      const endYear = lecture.courseEndDay ? new Date(lecture.courseEndDay).getFullYear() : null
      
      return (startYear === selectedYear || endYear === selectedYear)
    })()

    return matchesSearch && matchesStatus && matchesPeriod
  })

  // 통계 계산 (개별 템플릿 기준)
  const totalLectures = lectures.length
  const today = getTodayString()
  
  // 모든 템플릿의 상태를 개별적으로 계산
  let activeSurveys = 0
  let completedSurveys = 0
  let totalTemplates = 0
  
  lectures.forEach((l) => {
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
    } else {
      // 기존 로직 (템플릿이 없는 경우)
      const courseDate = l.courseDate
      const isActive = courseDate && today >= courseDate && today < l.courseEndDay
      const isCompleted = l.courseEndDay && today > l.courseEndDay
      
      if (isActive) {
        activeSurveys++
      } else if (isCompleted) {
        completedSurveys++
      }
    }
  })
  
  // 평균 응답률 계산
  const totalResponses = lectures.reduce((sum, l) => {
    if (l.evaluationTemplates && l.evaluationTemplates.length > 0) {
      // 모든 템플릿의 answerList에서 고유 응답자 수 계산
      const allAnswers = l.evaluationTemplates.flatMap(template => template.answerList || [])
      if (allAnswers.length > 0) {
        const uniqueRespondents = new Set(allAnswers.map(answer => answer.memberId))
        return sum + uniqueRespondents.size
      }
    } else if (l.answerList && l.answerList.length > 0) {
      const uniqueRespondents = new Set(l.answerList.map(answer => answer.memberId))
      return sum + uniqueRespondents.size
    }
    return sum
  }, 0)
  const totalStudents = lectures.reduce((sum, l) => sum + (l.studentCount || 0), 0)
  const averageResponseRate = totalStudents > 0 ? Math.round((totalResponses / totalStudents) * 100) : 0
  
  // 평균 만족도 계산 (5점 척도 기준)
  const satisfactionScores = []
  lectures.forEach(l => {
    let allAnswers = []
    
    if (l.evaluationTemplates && l.evaluationTemplates.length > 0) {
      // 모든 템플릿의 answerList 병합
      allAnswers = l.evaluationTemplates.flatMap(template => template.answerList || [])
    } else if (l.answerList && l.answerList.length > 0) {
      allAnswers = l.answerList
    }
    
    if (allAnswers.length > 0) {
      allAnswers.forEach(answer => {
        if (answer.evalQuestionType === 0 && answer.score) { // 5점 척도 질문
          satisfactionScores.push(parseFloat(answer.score))
        }
      })
    }
  })
  const averageSatisfaction = satisfactionScores.length > 0 
    ? (satisfactionScores.reduce((sum, score) => sum + score, 0) / satisfactionScores.length).toFixed(1)
    : "0.0"

  const getStatusColor = (status) => {
    switch (status) {
      case "진행중":
        return "bg-[#e4f5eb] text-[#1abc9c]"
      case "완료":
        return "bg-blue-50 text-[#b0c4de]"
      case "예정":
        return "bg-blue-100 text-[#3498db]"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const getScoreColor = (score) => {
    if (score >= 4.5) return "text-green-600"
    if (score >= 4.0) return "text-blue-600"
    if (score >= 3.5) return "text-yellow-600"
    return "text-red-600"
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header currentPage="evaluation" userRole="instructor" userName="" />
      <div className="flex">
        <Sidebar title="설문 평가 관리" menuItems={sidebarItems} currentPath="/instructor/evaluation" />

        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
            {/* 페이지 헤더 */}
            <div className="mb-6">
              <h1 className="text-2xl font-bold text-gray-900 mb-2">담당 과정 설문 평가</h1>
              <p className="text-gray-600">담당하고 있는 과정들의 설문 평가 현황을 확인하고 관리하세요.</p>
            </div>

            {/* 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
              <Card>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-m font-medium text-gray-600">총 담당 과정</p>
                      <p className="text-3xl font-bold text-[#3498db]">{totalLectures}</p>
                    </div>
                    <div className="p-3 bg-[#EFF6FF] rounded-full  ">
                      <Users className="w-8 h-8 text-[#3498db]" />
                    </div>
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-m font-medium text-gray-600">진행중 설문</p>
                      <p className="text-3xl font-bold text-[#1abc9c]">{activeSurveys}</p>
                      <p className="text-xs text-gray-500 mt-1">평가 기간 내</p>
                    </div>
                    <div className="p-3 bg-[#e4f5eb] rounded-full  ">
                      <BarChart3 className="w-8 h-8 text-[#1abc9c]" />
                    </div>
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-m font-medium text-gray-600">완료된 설문</p>
                      <p className="text-3xl font-bold text-[#b0c4de]">{completedSurveys}</p>
                      <p className="text-xs text-gray-500 mt-1">평가 기간 종료</p>
                    </div>
                    <div className="p-3 bg-[#EFF6FF] rounded-full  ">
                      <CheckCircle className="w-8 h-8 text-[#b0c4de]" />
                    </div>
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-m font-medium text-gray-600">평균 응답률</p>
                      <p className="text-3xl font-bold text-[#3498db]">{averageResponseRate}%</p>
                    </div>
                    <div className="p-3 bg-[#EFF6FF] rounded-full  ">
                      <Percent className="w-8 h-8 text-[#3498db]" />
                    </div>
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-m font-medium text-gray-600">평균 만족도</p>
                      <p className="text-3xl font-bold text-[#3498db]">{averageSatisfaction}/5.0</p>
                    </div>
                    <div className="p-3 bg-[#EFF6FF] rounded-full  ">
                      <Star className="w-8 h-8 text-[#3498db]" />
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
                    placeholder="과정명, 과정코드로 검색..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="all">전체 상태</option>
                <option value="진행중">진행중</option>
                <option value="완료">완료</option>
                <option value="예정">예정</option>
              </select>
                <select
                  value={selectedPeriod}
                  onChange={(e) => setSelectedPeriod(e.target.value)}
                  className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="all">전체 기간</option>
                  {getYearOptions().map(year => (
                    <option key={year} value={year}>{year}년</option>
                  ))}
                </select>
              </div>
            </div>

             {/* 과정 목록 */}
             {isLoadingLectures ? (
               <div className="bg-white p-8 rounded-lg shadow-sm text-center">
                 <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
                 <p className="text-gray-600">과정 목록을 불러오는 중...</p>
               </div>
             ) : (
               <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                 {filteredLectures.map((lecture, lectureIndex) => (
                   <div key={lecture.courseId || `lecture-${lectureIndex}`} className="bg-white rounded-lg shadow-sm border hover:shadow-md transition-shadow">
                     <div className="p-6">
                       {/* 과정 기본 정보 */}
                       <div className="flex justify-between items-start mb-4">
                         <div>
                           <h3 className="text-lg font-semibold text-gray-900 mb-1">{lecture.courseName}</h3>
                           <p className="text-sm text-gray-600">
                             {lecture.courseCode} • {lecture.memberName}
                           </p>
                           <p className="text-sm text-gray-500">{lecture.courseStartDay} ~ {lecture.courseEndDay}</p>
                         </div>
                       </div>

                       {/* 과정 세부 정보 */}
                       <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
                         <div>
                           <p className="text-gray-600">
                             수강생: <span className="font-medium">{lecture.studentCount || 0}/{lecture.maxCapacity}명</span>
                           </p>
                           <p className="text-gray-600">
                             강사: <span className="font-medium">{lecture.memberName}</span>
                           </p>
                         </div>
                            <div>
                            <p className="text-gray-600">
                                평가 일정: <span className="font-medium">
                                {lecture.evaluationTemplates && lecture.evaluationTemplates.length > 0 
                                  ? (() => {
                                      // templateGroupId 기준으로 고유한 템플릿 그룹 수 계산
                                      const uniqueTemplateGroups = new Set(
                                        lecture.evaluationTemplates.map(template => template.templateGroupId)
                                      )
                                      return `${uniqueTemplateGroups.size}개`
                                    })()
                                  : "0개"}
                              </span>
                            </p>
                          </div>
                       </div>

                                               {/* 템플릿 목록 (상태 표시 포함) */}
                        {lecture.evaluationTemplates && lecture.evaluationTemplates.length > 0 ? (
                          <div className="space-y-2 mb-4">
                            <p className="text-sm font-medium text-gray-700">과정 평가:</p>
                            <div className="flex flex-wrap gap-2">
                              {lecture.evaluationTemplates.map((template, templateIndex) => {
                                // 템플릿 상태 계산
                                const today = getTodayString()
                                const isActive = template.openDate && template.closeDate && 
                                               today >= template.openDate && today <= template.closeDate
                                const isCompleted = template.closeDate && today > template.closeDate
                                const isPending = template.openDate && today < template.openDate
                                
                                                                 // 상태 텍스트 결정
                                 let statusText = "예정"
                                 
                                 if (isActive) {
                                   statusText = "진행중"
                                 } else if (isCompleted) {
                                   statusText = "완료"
                                 } else if (isPending) {
                                   statusText = "예정"
                                 }
                                 
                                 return (
                                    <button
                                      key={template.templateGroupId}
                                      onClick={() => handleViewDetails(lecture, templateIndex)}
                                      className="text-xs px-3 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors border border-gray-300 flex flex-col items-start"
                                    >
                                     <div className="flex items-center gap-2 mb-1">
                                       <span className="font-medium">
                                         {template.questionTemplateName || `평가 일정 ${templateIndex + 1}`}
                                       </span>
                                       <Badge className={`text-xs px-1 py-0 ${statusText === "진행중" ? "bg-green-200 text-green-800" : statusText === "완료" ? "bg-blue-200 text-blue-800" : "bg-yellow-200 text-yellow-800"}`}>
                                         {statusText}
                                       </Badge>
                                     </div>
                                    <span className="text-gray-600 text-xs">
                                      {template.openDate} ~ {template.closeDate}
                                    </span>
                                    <span className="text-gray-500 text-xs">
                                      {template.answerList && template.answerList.length > 0 
                                        ? `${new Set(template.answerList.map(a => a.memberId)).size}명 응답`
                                        : "0명 응답"}
                                    </span>
                                  </button>
                                )
                              })}
                            </div>
                          </div>
                        ) : (
                          <div className="text-center py-4 mb-4">
                            <BarChart3 className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                            <p className="text-sm text-gray-600">평가 일정이 정해지지 않았습니다.</p>
                          </div>
                        )}

                       
                     </div>
                   </div>
                 ))}
               </div>
             )}

            {/* 결과가 없을 때 */}
            {filteredLectures.length === 0 && !isLoadingLectures && (
              <div className="bg-white p-8 rounded-lg shadow-sm text-center">
                <BarChart3 className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">검색 결과가 없습니다</h3>
                <p className="text-gray-600">검색 조건을 변경해보세요.</p>
              </div>
            )}
          </div>

          {/* 과정 상세 모달 */}
          {showEvaluationModal && selectedLecture && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" style={{backgroundColor: "rgba(0, 0, 0, 0.3)"}}>
              <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
                {/* 모달 헤더 */}
                <div className="flex justify-between items-center p-6 border-b">
                  <div>
                    <h2 className="text-xl font-bold text-gray-900">{selectedLecture.courseName} 과정 상세</h2>
                    <p className="text-gray-600 mt-1">
                     과정 코드 :  {selectedLecture.courseCode} 
                    </p>
                  </div>
                  <Button variant="ghost" onClick={closeModal} className="p-2">
                    <X className="w-5 h-5" />
                  </Button>
                </div>

                  {/* 모달 내용 */}
                 <div className="p-6">
                    {/* 선택된 템플릿 정보 표시 */}
                    {selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0 && (
                      <div className="mb-6">
                        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                          <div className="flex justify-between items-start">
                            <div>
                              <h3 className="text-lg font-semibold text-blue-900 mb-2">
                                {selectedLecture.evaluationTemplates[selectedTemplateIndex]?.questionTemplateName || `템플릿 ${selectedTemplateIndex + 1}`}
                              </h3>
                              <p className="text-sm text-blue-700 mb-1">
                                평가 기간: {selectedLecture.evaluationTemplates[selectedTemplateIndex]?.openDate} ~ {selectedLecture.evaluationTemplates[selectedTemplateIndex]?.closeDate}
                              </p>
                              <p className="text-sm text-blue-700">
                                문항 수: {selectedLecture.evaluationTemplates[selectedTemplateIndex]?.questionList?.length || 0}개
                              </p>
                            </div>
                            <Badge className="bg-blue-200 text-blue-800">
                              템플릿 {selectedTemplateIndex + 1}
                            </Badge>
                          </div>
                        </div>
                      </div>
                    )}
                   
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
                          <p className="text-sm font-medium text-gray-600 mb-2">사용 템플릿</p>
                          <div className="flex items-center gap-2">
                            <p className="font-semibold text-gray-900">
                              {selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0 
                                ? selectedLecture.evaluationTemplates[selectedTemplateIndex]?.questionTemplateName || `템플릿 ${selectedTemplateIndex + 1}`
                                : (selectedLecture.questionList?.[0]?.questionTemplateName || "템플릿 미설정")}
                            </p>
                          </div>
                        </div>
                    </div>
                  </div>

                  {/* 설문 현황 */}
                  <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-3">설문 평가 현황</h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      <div>
                        <p className="text-sm text-gray-600">응답률</p>
                        <p className="text-lg font-bold text-[#3c566e]">
                        {(() => {
                              let allAnswers = []
                              if (selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0) {
                                // 평가가 있는 경우: templateGroupId로 그룹화
                                const selectedTemplate = selectedLecture.evaluationTemplates[selectedTemplateIndex]
                                if (selectedTemplate && selectedTemplate.answerList && selectedTemplate.answerList.length > 0) {
                                  // 같은 templateGroupId를 가진 모든 템플릿의 답변을 수집
                                  const sameGroupTemplates = selectedLecture.evaluationTemplates.filter(t => t.templateGroupId === selectedTemplate.templateGroupId)
                                  allAnswers = sameGroupTemplates.flatMap(t => t.answerList || [])
                                } else {
                                  // 평가가 없는 경우: 개별 템플릿의 답변만 사용
                                  allAnswers = selectedTemplate?.answerList || []
                                }
                              } else if (selectedLecture.answerList && selectedLecture.answerList.length > 0) {
                                allAnswers = selectedLecture.answerList
                              }
                              
                              if (allAnswers.length === 0) return 0;
                              const uniqueRespondents = new Set(allAnswers.map(answer => answer.memberId));
                              return uniqueRespondents.size;
                            })()}/{selectedLecture.studentCount || 0}명 
                            ({selectedLecture.studentCount > 0 ? Math.round(((() => {
                              let allAnswers = []
                              if (selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0) {
                                // 평가가 있는 경우: templateGroupId로 그룹화
                                const selectedTemplate = selectedLecture.evaluationTemplates[selectedTemplateIndex]
                                if (selectedTemplate && selectedTemplate.answerList && selectedTemplate.answerList.length > 0) {
                                  // 같은 templateGroupId를 가진 모든 템플릿의 답변을 수집
                                  const sameGroupTemplates = selectedLecture.evaluationTemplates.filter(t => t.templateGroupId === selectedTemplate.templateGroupId)
                                  allAnswers = sameGroupTemplates.flatMap(t => t.answerList || [])
                                } else {
                                  // 평가가 없는 경우: 개별 템플릿의 답변만 사용
                                  allAnswers = selectedTemplate?.answerList || []
                                }
                              } else if (selectedLecture.answerList && selectedLecture.answerList.length > 0) {
                                allAnswers = selectedLecture.answerList
                              }
                              
                              if (allAnswers.length === 0) return 0;
                              const uniqueRespondents = new Set(allAnswers.map(answer => answer.memberId));
                              return (uniqueRespondents.size / selectedLecture.studentCount) * 100;
                            })())) : 0}%)
                         </p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">평가 상태</p>
                        <p className="text-lg font-bold text-[#3c566e]">
                          {getLectureStatus(selectedLecture)}
                        </p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">문제 수</p>
                          <p className="text-lg font-bold text-[#3c566e]">
                            {(() => {
                              if (selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0) {
                                // 선택된 템플릿의 문항 수만 계산
                                const selectedTemplate = selectedLecture.evaluationTemplates[selectedTemplateIndex]
                                if (selectedTemplate && selectedTemplate.questionList) {
                                  // 같은 templateId(questionTemplateNum)를 가진 템플릿들 중 첫 번째 템플릿의 문항 수만 사용 (중복 제거)
                                  const sameTemplateIdTemplates = selectedLecture.evaluationTemplates.filter(t => t.questionTemplateNum === selectedTemplate.questionTemplateNum)
                                  const firstTemplate = sameTemplateIdTemplates[0]
                                  return firstTemplate.questionList ? firstTemplate.questionList.length : 0
                                }
                                return 0
                              } else if (selectedLecture.questionList) {
                                return selectedLecture.questionList.length
                              }
                              return 0
                            })()}개
                         </p>
                      </div>
                    </div>
                    <div className="mt-3">
                      <div className="w-full bg-gray-200 rounded-full h-2">
                          <div
                           className="bg-[#1abc9c] h-2 rounded-full"
                             style={{ 
                               width: `${selectedLecture.studentCount > 0 ? Math.round(((() => {
                                let allAnswers = []
                                if (selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0) {
                                  // 평가가 있는 경우: templateGroupId로 그룹화
                                  const selectedTemplate = selectedLecture.evaluationTemplates[selectedTemplateIndex]
                                  if (selectedTemplate && selectedTemplate.answerList && selectedTemplate.answerList.length > 0) {
                                    // 같은 templateGroupId를 가진 모든 템플릿의 답변을 수집
                                    const sameGroupTemplates = selectedLecture.evaluationTemplates.filter(t => t.templateGroupId === selectedTemplate.templateGroupId)
                                    allAnswers = sameGroupTemplates.flatMap(t => t.answerList || [])
                                  } else {
                                    // 평가가 없는 경우: 개별 템플릿의 답변만 사용
                                    allAnswers = selectedTemplate?.answerList || []
                                  }
                                } else if (selectedLecture.answerList && selectedLecture.answerList.length > 0) {
                                  allAnswers = selectedLecture.answerList
                                }
                                
                                if (allAnswers.length === 0) return 0;
                                const uniqueRespondents = new Set(allAnswers.map(answer => answer.memberId));
                                return (uniqueRespondents.size / selectedLecture.studentCount) * 100;
                              })())) : 0}%` 
                            }}
                         ></div>
                      </div>
                    </div>
                  </div>
                    {/* 답변 데이터 또는 평가 문항 */}
                   <div className="mb-6">
                                           <h3 className="text-lg font-semibold text-gray-900 mb-4">
                        {selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0 
                          ? (selectedLecture.evaluationTemplates[selectedTemplateIndex]?.answerList && selectedLecture.evaluationTemplates[selectedTemplateIndex].answerList.length > 0 
                              ? "문항별 설문 결과" 
                              : "평가 문항")
                          : (selectedLecture.answerList && selectedLecture.answerList.length > 0 
                              ? "문항별 설문 결과" 
                              : "평가 문항")}
                      </h3>
                      {(selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0 
                        ? selectedLecture.evaluationTemplates[selectedTemplateIndex]?.answerList && selectedLecture.evaluationTemplates[selectedTemplateIndex].answerList.length > 0
                        : (selectedLecture.answerList && selectedLecture.answerList.length > 0)) ? (
                       // 답변이 있는 경우 - 문항별로 그룹화하여 표시
                       (() => {
                          // 선택된 템플릿의 answerList 사용
                          let allAnswers = []
                          if (selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0) {
                            // 평가가 있는 경우: templateGroupId로 그룹화
                            const selectedTemplate = selectedLecture.evaluationTemplates[selectedTemplateIndex]
                            if (selectedTemplate && selectedTemplate.answerList && selectedTemplate.answerList.length > 0) {
                              // 같은 templateGroupId를 가진 모든 템플릿의 답변을 수집
                              const sameGroupTemplates = selectedLecture.evaluationTemplates.filter(t => t.templateGroupId === selectedTemplate.templateGroupId)
                              allAnswers = sameGroupTemplates.flatMap(t => t.answerList || [])
                            } else {
                              // 평가가 없는 경우: 개별 템플릿의 답변만 사용
                              allAnswers = selectedTemplate?.answerList || []
                            }
                          } else {
                            allAnswers = selectedLecture.answerList || []
                          }
                        
                        // answerList를 questionNum과 evalQuestionId로 그룹화
                        const groupedAnswers = allAnswers.reduce((groups, answer) => {
                          const key = `${answer.questionNum}-${answer.evalQuestionId}`
                          if (!groups[key]) {
                            groups[key] = {
                              questionNum: answer.questionNum,
                              evalQuestionId: answer.evalQuestionId,
                              evalQuestionText: answer.evalQuestionText,
                              evalQuestionType: answer.evalQuestionType,
                              answers: []
                            }
                          }
                          groups[key].answers.push(answer)
                          return groups
                        }, {})

                        // 그룹화된 답변을 questionNum 순서로 정렬
                        const sortedGroups = Object.values(groupedAnswers).sort((a, b) => {
                          // questionNum이 숫자인지 확인하고 정렬 (더 안전한 방법)
                          const aNum = Number(a.questionNum) || 0
                          const bNum = Number(b.questionNum) || 0
                          if (isNaN(aNum) && isNaN(bNum)) return 0
                          if (isNaN(aNum)) return 1
                          if (isNaN(bNum)) return -1
                          return aNum - bNum
                        })

                        return (
                          <div className="space-y-6">
                            {sortedGroups.map((group, index) => (
                                <div key={group.evalQuestionId} className="border border-gray-200 rounded-lg p-6">
                                  <div className="flex justify-between items-start mb-4">
                                    <div className="flex-1">
                                      <h4 className="text-lg font-semibold text-gray-900 mb-2">
                                        {group.questionNum}. {group.evalQuestionText}
                                      </h4>
                                      <p className="text-sm text-gray-600">
                                        유형: {group.evalQuestionType === 0 ? "5점 척도" : "주관식"}
                                      </p>
                                    </div>
                                    <Badge className="bg-blue-100 text-blue-800">
                                      {group.evalQuestionType === 0 ? "5점 척도" : "주관식"}
                                    </Badge>
                                  </div>
                                  {group.evalQuestionType === 0 ? (
                                    // 5점 척도 문항 - 평균 점수 계산
                                    (() => {
                                      const scores = group.answers.map(answer => answer.score || 0)
                                      const averageScore = scores.length > 0 ? scores.reduce((sum, score) => sum + score, 0) / scores.length : 0
                                      
                                      return (
                                        <div className="bg-gray-50 rounded-lg p-4">
                                          <div className="flex items-center gap-4">
                                            <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                                              <span className="text-lg font-bold text-blue-600">{averageScore.toFixed(1)}</span>
                                            </div>
                                            <div>
                                              <p className="text-sm text-gray-600">평균 점수</p>
                                              <p className="text-lg font-semibold text-gray-900">
                                                {averageScore.toFixed(1)}/5.0
                                              </p>
                                              <p className="text-xs text-gray-500">
                                                총 {group.answers.length}명 응답
                                              </p>
                                            </div>
                                          </div>
                                        </div>
                                      )
                                    })()
                                  ) : (
                                    // 주관식 문항 - 모든 답변 표시 (익명)
                                    <div className="space-y-3">
                                      <p className="text-sm text-gray-600 mb-3">총 {group.answers.length}개의 답변</p>
                                      {group.answers.map((answer, answerIndex) => (
                                        <div key={answerIndex} className="bg-gray-50 rounded-lg p-3">
                                          <p className="text-gray-900">
                                            {answer.answerText || "답변 내용 없음"}
                                          </p>
                                        </div>
                                      ))}
                                    </div>
                                  )}
                                </div>
                              )
                            )}
                          </div>
                        )
                      })()
                       ) : (selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0 
                       ? selectedLecture.evaluationTemplates[selectedTemplateIndex]?.questionList && selectedLecture.evaluationTemplates[selectedTemplateIndex].questionList.length > 0
                       : (selectedLecture.questionList && selectedLecture.questionList.length > 0)) ? (
                       // 답변이 없고 평가 기간인 경우 - 평가 문항 표시
                                               <div className="space-y-4">
                        {(selectedLecture.evaluationTemplates && selectedLecture.evaluationTemplates.length > 0 
                          ? (() => {
                              // 평가가 있는 경우: templateGroupId로 그룹화하되 중복 제거
                              const selectedTemplate = selectedLecture.evaluationTemplates[selectedTemplateIndex]
                              if (selectedTemplate && selectedTemplate.questionList && selectedTemplate.questionList.length > 0) {
                                // 같은 templateGroupId를 가진 모든 템플릿의 질문을 수집하되, 중복 제거
                                const sameGroupTemplates = selectedLecture.evaluationTemplates.filter(t => t.templateGroupId === selectedTemplate.templateGroupId)
                                
                                // 모든 질문을 수집하되, evalQuestionId 기준으로 중복 제거
                                const allGroupQuestions = sameGroupTemplates.flatMap(t => t.questionList || [])
                                const uniqueQuestionIds = new Set()
                                return allGroupQuestions.filter(question => {
                                  const questionKey = `${question.questionNum}-${question.evalQuestionId}`
                                  if (uniqueQuestionIds.has(questionKey)) {
                                    return false // 이미 존재하는 질문이면 제외
                                  }
                                  uniqueQuestionIds.add(questionKey)
                                  return true
                                })
                              } else {
                                // 평가가 없는 경우: 개별 템플릿의 질문만 사용
                                return selectedTemplate?.questionList || []
                              }
                            })()
                          : selectedLecture.questionList || [])
                         .sort((a, b) => {
                           // questionNum이 숫자인지 확인하고 정렬 (더 안전한 방법)
                           const aNum = Number(a.questionNum) || 0
                           const bNum = Number(b.questionNum) || 0
                           if (isNaN(aNum) && isNaN(bNum)) return 0
                           if (isNaN(aNum)) return 1
                           if (isNaN(bNum)) return -1
                           return aNum - bNum
                         })
                        .map((question, index) => (
                          <div key={question.evalQuestionId || index} className="border border-gray-200 rounded-lg p-4">
                            <div className="flex justify-between items-start">
                              <div className="flex-1">
                                <h4 className="font-medium text-gray-900 mb-2">
                                  {question.questionNum}. {question.evalQuestionText}
                                </h4>
                                <p className="text-sm text-gray-600">
                                  유형: {question.evalQuestionType === 0 ? "5점 척도" : "주관식"}
                                </p>
                                <p className="text-sm text-gray-500">
                                  템플릿: {question.questionTemplateName}
                                </p>
                              </div>
                              <Badge className="bg-blue-100 text-blue-800">평가 문항</Badge>
                            </div>
                          </div>
                        ))}
                    </div>
                    ) : (
                      <div className="text-center py-8">
                        <BarChart3 className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                        <p className="text-gray-600">
                          {getLectureStatus(selectedLecture) === "예정" 
                            ? "아직 평가 기간이 아닙니다." 
                            : "평가 문항이 설정되지 않았습니다."}
                        </p>
                      </div>
                    )}
                  </div>
                </div>

                {/* 모달 푸터 */}
                <div className="flex justify-end gap-2 p-6 border-t">
                  <Button variant="outline" onClick={closeModal} className="hover:bg-gray-200">
                    닫기
                  </Button>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
