import React, { useEffect, useState } from "react"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { getMenuItems } from "@/components/ui/menuConfig"
import { useLocation } from "react-router-dom"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { ChevronDown, ChevronRight, TrendingUp, Award, BookOpen, FileText, Download, BarChart3, Target, Eye, X, CheckCircle } from "lucide-react"
import { getStudentExams, getStudentAnswers, startExam, confirmGrade } from "@/api/kayoung/studentExamApi"

export default function StudentGradesPage() {
  const [gradesData, setGradesData] = useState([])
  const [loading, setLoading] = useState(true)
  const [expandedCourses, setExpandedCourses] = useState(new Set())
  
  // 성적 확인 모달 상태
  const [isGradeConfirmModalOpen, setIsGradeConfirmModalOpen] = useState(false)
  const [selectedExam, setSelectedExam] = useState(null)
  const [studentSignature, setStudentSignature] = useState("")
  const [isSubmittingSignature, setIsSubmittingSignature] = useState(false)
  
  // 답안지 및 코멘트 상태
  const [examAnswers, setExamAnswers] = useState(null)
  const [questionDetails, setQuestionDetails] = useState([])
  const [loadingAnswers, setLoadingAnswers] = useState(false)

  const location = useLocation()
  const sidebarMenuItems = getMenuItems('student-exam')
  const currentPath = location.pathname

  // 시험 데이터 로드
  useEffect(() => {
    const fetchExams = async () => {
      try {
        setLoading(true)
        const response = await getStudentExams()
        
        if (response && response.data) {
          // 시험 데이터를 과정별로 그룹화
          const examsByCourse = response.data.reduce((acc, exam) => {
            const courseId = exam.courseId
            if (!acc[courseId]) {
              acc[courseId] = {
                courseId: courseId,
                courseName: exam.courseName,
                courseCode: exam.courseCode,
                instructor: exam.memberName,
                exams: []
              }
            }
            
            // 시험 정보 변환
            const examData = {
              examId: exam.templateId,
              examName: exam.templateName,
              courseId: exam.courseId,
              memberId: exam.memberId,
              examStartDate: exam.templateOpen ? new Date(new Date(exam.templateOpen).getTime() + 9 * 60 * 60 * 1000) : null,
              examEndDate: exam.templateClose ? new Date(new Date(exam.templateClose).getTime() + 9 * 60 * 60 * 1000) : null,
              examDate: exam.templateOpen ? new Date(new Date(exam.templateOpen).getTime() + 9 * 60 * 60 * 1000).toLocaleString('ko-KR', { 
                year: 'numeric', 
                month: 'numeric', 
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
              }) : '미정',
              totalScore: exam.myScore || 0,
              maxScore: 100, // 기본값, 실제로는 문제별 점수 합계
              grade: exam.grade || '미정',
              submitted: exam.submitted,
              submittedAt: exam.submittedAt,
              status: exam.status || 'unknown'
            }
            
            acc[courseId].exams.push(examData)
            return acc
          }, {})
          
          // 시험이 없는 과정도 포함하여 모든 과정 표시
          const allCourses = Object.values(examsByCourse)
          
          // 시험이 없는 과정에 대한 빈 시험 배열 추가
          allCourses.forEach(course => {
            if (!course.exams || course.exams.length === 0) {
              course.exams = []
            }
          })
          
          setGradesData(allCourses)
        } else {
          setGradesData([])
        }
      } catch (error) {
        console.error('시험 데이터 로드 실패:', error)
        setGradesData([])
      } finally {
        setLoading(false)
      }
    }

    fetchExams()
  }, [])

  const toggleCourseExpansion = (courseId) => {
    const newExpanded = new Set(expandedCourses)
    if (newExpanded.has(courseId)) {
      newExpanded.delete(courseId)
    } else {
      newExpanded.add(courseId)
    }
    setExpandedCourses(newExpanded)
  }

  const getGradeColor = (grade) => {
    const colors = {
      "A": "text-green-600 bg-green-50 border-green-200",
      "B": "text-blue-600 bg-blue-50 border-blue-200", 
      "C": "text-yellow-600 bg-yellow-50 border-yellow-200",
      "D": "text-orange-600 bg-orange-50 border-orange-200",
      "F": "text-red-600 bg-red-50 border-red-200"
    }
    return colors[grade] || "text-gray-600 bg-gray-50 border-gray-200"
  }

  const getGradeText = (grade) => {
    const gradeMap = {
      "A": "탁월",
      "B": "우수", 
      "C": "양호",
      "D": "보통",
      "F": "노력"
    }
    return gradeMap[grade] || grade
  }

  const getScoreColor = (score, maxScore) => {
    const percentage = (score / maxScore) * 100
    if (percentage >= 90) return "text-green-600"
    if (percentage >= 80) return "text-blue-600"
    if (percentage >= 70) return "text-yellow-600"
    if (percentage >= 60) return "text-orange-600"
    return "text-red-600"
  }

  // 성적 확인 모달 열기
  const handleOpenGradeConfirmModal = async (exam) => {
    try {
      setLoadingAnswers(true)
      setSelectedExam(exam)
      setStudentSignature("")
      setIsGradeConfirmModalOpen(true)
      
      // 답안지 및 코멘트 데이터 로드
      await loadExamAnswersAndComments(exam.examId)
    } catch (error) {
      console.error('답안지 로딩 실패:', error)
      alert('답안지를 불러오는데 실패했습니다.')
    } finally {
      setLoadingAnswers(false)
    }
  }

  // 성적 확인 모달 닫기
  const handleCloseGradeConfirmModal = () => {
    setIsGradeConfirmModalOpen(false)
    setSelectedExam(null)
    setStudentSignature("")
    setExamAnswers(null)
    setQuestionDetails([])
  }

  // 답안지 및 코멘트 로드 함수
  const loadExamAnswersAndComments = async (templateId) => {
    try {
      // 문제 상세 정보 로드 (기존 API 사용)
      const questionsResponse = await startExam(templateId)
      
      setQuestionDetails(questionsResponse.data || questionsResponse)
      
    } catch (error) {
      console.error('답안지 및 코멘트 로딩 실패:', error)
      throw error
    }
  }

  // 서명 제출
  const handleSubmitSignature = async () => {
    if (!studentSignature.trim()) {
      alert('서명을 입력해주세요.')
      return
    }

    try {
      setIsSubmittingSignature(true)
      
      // 기존 API 사용하여 성적 확인 제출
      const result = await confirmGrade(selectedExam.examId, studentSignature, selectedExam.courseId, selectedExam.memberId)
      
      if (result.success || result.resultCode === "200" || (result.data && result.data.isChecked === 0)) {
        alert('성적 확인이 완료되었습니다.')
        handleCloseGradeConfirmModal()
        // 데이터 새로고침
        window.location.reload()
      } else {
        alert('성적 확인에 실패했습니다: ' + (result.message || result.resultMessage || '알 수 없는 오류'))
      }
    } catch (error) {
      console.error('오류 상세:', error.response?.data || error.message)
      
      // 더 구체적인 오류 메시지 표시
      if (error.response?.data?.message) {
        alert('서명 제출 중 오류가 발생했습니다: ' + error.response.data.message)
      } else if (error.response?.data?.resultMessage) {
        alert('서명 제출 중 오류가 발생했습니다: ' + error.response.data.resultMessage)
      } else {
        alert('서명 제출 중 오류가 발생했습니다: ' + error.message)
      }
    } finally {
      setIsSubmittingSignature(false)
    }
  }

  if (loading) {
    return (
      <React.Fragment>
        <PageLayout currentPage="my-exam" userRole="student">
          <div className="flex">
            <Sidebar title="시험 및 성적" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="flex justify-center items-center h-64">
                <div className="text-lg">성적 정보를 불러오는 중...</div>
              </div>
            </main>
          </div>
        </PageLayout>
      </React.Fragment>
    )
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
                <h1 className="text-2xl font-bold text-gray-900">성적 조회</h1>
                <p className="text-gray-600 mt-1">과정별 시험 성적을 확인할 수 있습니다.</p>
              </div>

              {/* 전체 통계 */}
              {gradesData.length > 0 && (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
                  <Card>
                    <CardContent className="p-6 flex items-center justify-between">
                      <div className="ml-4">
                        <p className="text-base font-medium text-gray-600">총 시험 수</p>
                        <p className="text-3xl font-bold" style={{ color: "#3498db" }}>
                          {gradesData.reduce((sum, course) => sum + course.exams.length, 0)}개
                        </p>
                      </div>
                      <div className="bg-[#EFF6FF] rounded-full p-3 mr-4">
                        <BarChart3 className="w-10 h-10" style={{ color: "#3498db" }} />
                      </div>
                    </CardContent>
                  </Card>
                  
                  <Card>
                    <CardContent className="p-6 flex items-center justify-between">
                      <div className="ml-4">
                        <p className="text-base font-medium text-gray-600">응시한 시험 수</p>
                        <p className="text-3xl font-bold" style={{ color: "#1abc9c" }}>
                          {(() => {
                            const allExams = gradesData.flatMap(course => course.exams)
                            return allExams.filter(exam => exam.submitted).length
                          })()}개
                        </p>
                      </div>
                      <div className="bg-[#e4f5eb] rounded-full p-3 mr-4">
                        <CheckCircle className="w-10 h-10" style={{ color: "#1abc9c" }} />
                      </div>
                    </CardContent>
                  </Card>
                  
                  <Card>
                    <CardContent className="p-6 flex items-center justify-between">
                      <div className="ml-4">
                        <p className="text-base font-medium text-gray-600">평균 점수</p>
                        <p className="text-3xl font-bold" style={{ color: "#b0c4de" }}>
                          {(() => {
                            const allExams = gradesData.flatMap(course => course.exams)
                            const completedExams = allExams.filter(exam => exam.status === "completed")
                            if (completedExams.length === 0) return 0
                            const avgScore = completedExams.reduce((sum, exam) => sum + exam.totalScore, 0) / completedExams.length
                            return avgScore.toFixed(1)
                          })()}점
                        </p>
                      </div>
                      <div className="bg-[#eff6ff] rounded-full p-3 mr-4">
                        <TrendingUp className="w-10 h-10" style={{ color: "#b0c4de" }} />
                      </div>
                    </CardContent>
                  </Card>
                </div>
              )}

              {/* 과정별 성적 아코디언 */}
              <div className="space-y-4">
                {gradesData.length === 0 ? (
                  <div className="text-center py-12">
                    <div className="text-gray-500 text-lg">등록된 성적이 없습니다.</div>
                  </div>
                ) : (
                  gradesData.map((course) => (
                    <Card key={course.courseId} className="overflow-hidden">
                      <CardHeader 
                        className="cursor-pointer hover:bg-gray-50 transition-colors"
                        onClick={() => toggleCourseExpansion(course.courseId)}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-3">
                            {expandedCourses.has(course.courseId) ? (
                              <ChevronDown className="h-5 w-5 text-gray-500" />
                            ) : (
                              <ChevronRight className="h-5 w-5 text-gray-500" />
                            )}
                            <div>
                              <CardTitle className="text-lg">{course.courseName}</CardTitle>
                              <div className="flex items-center space-x-4 text-sm text-gray-600 mt-1">
                                <span>과목코드: {course.courseCode}</span>
                                <span>담당강사: {course.instructor}</span>
                                <span>시험 수: {course.exams.length}개</span>
                              </div>
                            </div>
                          </div>
                          <div className="flex items-center space-x-2">
                            <Badge variant="outline" className="text-sm">
                              평균: {(() => {
                                const completedExams = course.exams.filter(exam => exam.status === "completed")
                                if (completedExams.length === 0) return 0
                                const avgScore = completedExams.reduce((sum, exam) => sum + exam.totalScore, 0) / completedExams.length
                                return avgScore.toFixed(1)
                              })()}점
                            </Badge>
                          </div>
                        </div>
                      </CardHeader>
                      
                      {expandedCourses.has(course.courseId) && (
                        <CardContent className="pt-0">
                          <div className="space-y-4">
                            {course.exams.length === 0 ? (
                              <div className="text-center py-8 text-gray-500">
                                <FileText className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                                <p>등록된 시험이 없습니다.</p>
                              </div>
                            ) : (
                              course.exams.map((exam) => (
                              <div key={exam.examId} className="border rounded-lg p-4 bg-gray-50">
                                <div className="flex items-center justify-between mb-3">
                                  <div>
                                    <h4 className="font-semibold text-gray-900">{exam.examName}</h4>
                                    <div className="text-sm text-gray-600">
                                      <div>시작: {exam.examStartDate ? exam.examStartDate.toLocaleString('ko-KR', { 
                                        year: 'numeric', 
                                        month: 'numeric', 
                                        day: 'numeric',
                                        hour: '2-digit',
                                        minute: '2-digit'
                                      }) : '미정'}</div>
                                      <div>종료: {exam.examEndDate ? exam.examEndDate.toLocaleString('ko-KR', { 
                                        year: 'numeric', 
                                        month: 'numeric', 
                                        day: 'numeric',
                                        hour: '2-digit',
                                        minute: '2-digit'
                                      }) : '무제한'}</div>
                                    </div>
                                  </div>
                                  <div className="flex items-center space-x-3">
                                    <div className="text-right">
                                      <div className={`text-lg font-bold ${getScoreColor(exam.totalScore, exam.maxScore)}`}>
                                        {exam.totalScore} / {exam.maxScore}점
                                      </div>
                                      <div className="text-sm text-gray-600">
                                        {((exam.totalScore / exam.maxScore) * 100).toFixed(1)}%
                                      </div>
                                    </div>
                                    <Badge className={`${getGradeColor(exam.grade)}`}>
                                      {getGradeText(exam.grade)}
                                    </Badge>
                                  </div>
                                </div>
                                
                                {/* 성적 확인하기 버튼 */}
                                <div className="flex justify-end mt-3">
                                  {exam.status === "completed" ? (
                                    <Button
                                      size="sm"
                                      className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                                      onClick={() => handleOpenGradeConfirmModal(exam)}
                                    >
                                      성적 확인하기
                                    </Button>
                                  ) : (
                                    <Button
                                      size="sm"
                                      disabled
                                      className="bg-gray-400 text-white cursor-not-allowed"
                                    >
                                      {exam.submitted ? "채점 대기" : "미응시"}
                                    </Button>
                                  )}
                                </div>
                                

                              </div>
                            ))
                            )}
                          </div>
                        </CardContent>
                      )}
                    </Card>
                  ))
                )}
              </div>

              
            </div>
          </main>
        </div>
      </PageLayout>

      {/* 성적 확인 모달 */}
      {isGradeConfirmModalOpen && selectedExam && (
        <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}>
          <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[90vh] overflow-y-auto mx-4">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold text-gray-900">
                시험 성적 확인 - {selectedExam.examName}
              </h2>
              <button 
                onClick={handleCloseGradeConfirmModal} 
                className="text-gray-500 hover:text-gray-700"
              >
                닫기
              </button>
            </div>

            {loadingAnswers ? (
              <div className="flex items-center justify-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                <span className="ml-3 text-gray-600">답안지를 불러오는 중...</span>
              </div>
            ) : (
              <div className="space-y-6">
                {/* 시험 정보 */}
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm text-gray-600 mb-1">시험명</p>
                      <p className="font-semibold text-lg text-gray-900">{selectedExam.examName}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600 mb-1">시험 기간</p>
                      <div className="font-semibold text-sm text-gray-900">
                        <div>시작: {selectedExam.examStartDate ? selectedExam.examStartDate.toLocaleString('ko-KR', { 
                          year: 'numeric', 
                          month: 'numeric', 
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        }) : '미정'}</div>
                        <div>종료: {selectedExam.examEndDate ? selectedExam.examEndDate.toLocaleString('ko-KR', { 
                          year: 'numeric', 
                          month: 'numeric', 
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        }) : '무제한'}</div>
                      </div>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600 mb-1">총점</p>
                      <p className={`font-semibold text-lg ${getScoreColor(selectedExam.totalScore, selectedExam.maxScore)}`}>
                        {selectedExam.totalScore} / {selectedExam.maxScore}점
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600 mb-1">등급</p>
                      <Badge className={`${getGradeColor(selectedExam.grade)}`}>
                        {getGradeText(selectedExam.grade)}
                      </Badge>
                    </div>
                  </div>
                </div>

                {/* 답안지 및 코멘트 */}
                {questionDetails.length > 0 && (
                  <div className="space-y-4">
                    <h3 className="text-lg font-semibold text-gray-900">답안지 및 코멘트</h3>
                    
                    {questionDetails.map((question, index) => (
                      <div key={question.questionId} className="border rounded-lg p-4">
                                                 <div className="flex items-start justify-between mb-3">
                           <div className="flex items-center gap-3">
                             <span className="bg-purple-100 text-purple-800 px-2 py-1 rounded text-sm font-medium">
                               {index + 1}번
                             </span>
                             <Badge variant="outline">{question.questionType}</Badge>
                             <span className="text-sm text-gray-500">{question.questionScore}점</span>
                           </div>
                           <div className="text-right">
                             <span className={`text-lg font-bold ${getScoreColor(question.studentScore, question.questionScore)}`}>
                               {question.studentScore}점
                             </span>
                           </div>
                         </div>

                        <div className="mb-4">
                          <p className="font-medium mb-2 text-gray-900">문제</p>
                          <p className="text-sm text-gray-700">{question.questionText}</p>
                        </div>

                        {/* 학생 답안 */}
                        <div className="mb-4">
                          <p className="font-medium mb-2 text-gray-900">내 답안</p>
                          <div className="bg-blue-50 border border-blue-200 rounded p-3">
                            <div className="whitespace-pre-wrap text-sm text-gray-800">
                              {question.studentAnswer || "답안이 없습니다."}
                            </div>
                          </div>
                        </div>

                                                 {/* 강사 코멘트 */}
                         {question.explanation && (
                           <div className="mb-4">
                             <p className="font-medium mb-2 text-gray-900">강사 코멘트</p>
                             <div className="bg-green-50 border border-green-200 rounded p-3">
                               <div className="whitespace-pre-wrap text-sm text-gray-800">
                                 {question.explanation}
                               </div>
                             </div>
                           </div>
                         )}

                         {/* 서술형인 경우 정답 표시 */}
                         {question.questionType === "서술형" && question.questionAnswer && (
                           <div className="mb-4">
                             <p className="font-medium mb-2 text-gray-900">정답</p>
                             <div className="bg-green-50 border border-green-200 rounded p-3">
                               <div className="whitespace-pre-wrap text-sm text-gray-800">
                                 {question.questionAnswer}
                               </div>
                             </div>
                           </div>
                         )}

                                                 {/* 객관식인 경우 정답 표시 */}
                         {question.questionType === "객관식" && question.options && (
                           <div className="mb-4">
                             <p className="font-medium mb-2 text-gray-900">정답</p>
                             <div className="grid grid-cols-2 gap-2 text-sm">
                               {question.options.map((option, optIndex) => {
                                 // option이 객체인 경우 optText를 사용, 문자열인 경우 그대로 사용
                                 const optionText = typeof option === 'object' && option.optText ? option.optText : option;
                                 return (
                                   <div
                                     key={optIndex}
                                     className={`p-2 rounded ${
                                       optionText === question.questionAnswer
                                         ? "bg-green-50 text-green-800 border border-green-200"
                                         : question.studentAnswer === optionText
                                           ? "bg-red-50 text-red-800 border border-red-200"
                                           : "bg-gray-50 text-gray-700"
                                     }`}
                                   >
                                     {String.fromCharCode(65 + optIndex)}. {optionText}
                                     {optionText === question.questionAnswer && (
                                       <CheckCircle className="w-4 h-4 inline ml-2 text-green-600" />
                                     )}
                                   </div>
                                 );
                               })}
                             </div>
                           </div>
                         )}
                      </div>
                    ))}
                  </div>
                )}



                                 {/* 서명 입력 - isChecked 값에 따라 다르게 표시 */}
                 {(() => {
                   // 첫 번째 문제의 isChecked 값을 기준으로 판단 (모든 문제가 동일한 상태라고 가정)
                   const isChecked = questionDetails.length > 0 ? questionDetails[0].isChecked : 1;
                   
                   if (isChecked === 0) {
                     // isChecked가 0이면 서명완료 상태
                     return (
                       <div className="border border-green-200 rounded-lg p-4 bg-green-50">
                         <div className="flex items-center">
                           <CheckCircle className="w-5 h-5 text-green-600 mr-2" />
                           <span className="text-green-800 font-medium">서명완료</span>
                         </div>
                       </div>
                     );
                   } else {
                     // isChecked가 1이면 서명 입력란 표시
                     return (
                       <div className="border border-gray-200 rounded-lg p-4">
                         <h3 className="text-lg font-semibold text-gray-900 mb-4">성적 확인 서명</h3>
                         <p className="text-sm text-gray-600 mb-4">
                           "본인은 위 성적을 확인하였습니다." 문구를 정확히 입력하여 성적 확인을 완료해주세요.
                         </p>
                         <div>
                           <label className="block text-sm font-medium text-gray-700 mb-2">
                             서명 <span className="text-red-500">*</span>
                           </label>
                           <input
                             type="text"
                             value={studentSignature}
                             onChange={(e) => setStudentSignature(e.target.value)}
                             placeholder="본인은 위 성적을 확인하였습니다."
                             className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                               studentSignature && studentSignature !== "본인은 위 성적을 확인하였습니다." 
                                 ? "border-red-300 focus:ring-red-500" 
                                 : "border-gray-300 focus:ring-blue-500"
                             }`}
                             required
                           />
                           {studentSignature && studentSignature !== "본인은 위 성적을 확인하였습니다." && (
                             <p className="text-red-500 text-sm mt-1">
                               정확한 문구를 입력해주세요: "본인은 위 성적을 확인하였습니다."
                             </p>
                           )}
                         </div>
                       </div>
                     );
                   }
                 })()}
              </div>
            )}

            {/* 모달 푸터 */}
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t">
              <Button 
                onClick={handleCloseGradeConfirmModal}
                className="hover:bg-gray-200 border" >
                닫기
              </Button>
                             {(() => {
                 // 첫 번째 문제의 isChecked 값을 기준으로 판단
                 const isChecked = questionDetails.length > 0 ? questionDetails[0].isChecked : 1;
                 
                 if (isChecked === 1) {
                   // isChecked가 1이면 서명 제출 버튼 표시
                   return (
                     <Button 
                       onClick={handleSubmitSignature}
                       disabled={isSubmittingSignature || studentSignature !== "본인은 위 성적을 확인하였습니다." || loadingAnswers}
                       className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                     >
                       {isSubmittingSignature ? "처리 중..." : "성적 확인"}
                     </Button>
                   );
                 }
                 return null;
               })()}
            </div>
          </div>
        </div>
      )}
    </React.Fragment>
  )
}
