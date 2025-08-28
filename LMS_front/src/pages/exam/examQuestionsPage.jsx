import { useState, useEffect } from "react"
import { useLocation } from "react-router-dom"
import { Search, Plus, FileText, BarChart3, ChevronDown, ChevronUp } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { Button } from "@/components/ui/button"
import { getMenuItems } from "@/components/ui/menuConfig"
import { getSubDetailList, getQuestionsBySubDetail, getQuestionDetail } from "@/api/kayoung/examQuestionsApi"
import React from "react"

export default function ExamQuestionsPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [subjects, setSubjects] = useState([])
  const [subjectQuestions, setSubjectQuestions] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [loadingQuestions, setLoadingQuestions] = useState({})
  const [subDetailsData, setSubDetailsData] = useState([])

  const [expandedSubject, setExpandedSubject] = useState(null)
  const [selectedQuestion, setSelectedQuestion] = useState(null)
  const [isQuestionModalOpen, setIsQuestionModalOpen] = useState(false)

  const location = useLocation()

  // 동적 사이드바 메뉴 생성
  const sidebarMenuItems = getMenuItems('exam')
  const currentPath = location.pathname

  // API에서 세부과목 데이터 로드
  useEffect(() => {
    const loadSubDetails = async () => {
      try {
        setLoading(true)
        setError(null)
        
        // 세부과목 목록 조회
        const subDetailsData = await getSubDetailList()
        
        // 원본 데이터 저장 (문제 목록 포함)
        setSubDetailsData(subDetailsData)
        
        // 세부과목 데이터를 컴포넌트에서 사용할 형태로 변환
        const formattedSubDetails = subDetailsData.map((subDetail, index) => ({
          id: subDetail.subDetailId || index + 1,
          name: subDetail.subDetailName,
          code: subDetail.subDetailId || `SUB${index + 1}`,
          mainSubject: subDetail.subjectName,
          info: subDetail.subDetailInfo || '', // 실제 API 응답에서 가져옴
          totalQuestions: subDetail.questionCount || 0, // API에서 제공하는 문제 수
          lastUpdated: new Date().toISOString().split('T')[0], // 오늘 날짜
          status: subDetail.subDetailActive === 0 ? "활성" : "비활성"
        }))
        
        setSubjects(formattedSubDetails)
        
        // 문제 데이터는 필요할 때만 로드하도록 변경 (지연 로딩)
        setSubjectQuestions({})
        
      } catch (error) {
        console.error('세부과목 데이터 로드 실패:', error)
        setError('세부과목 데이터를 불러오는데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }
    
    loadSubDetails()
  }, [])

  // 필터링된 세부과목 목록
  const filteredSubjects = subjects.filter((subject) => {
    const matchesSearch =
      subject.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (subject.mainSubject && subject.mainSubject.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (subject.info && subject.info.toLowerCase().includes(searchTerm.toLowerCase()))

    return matchesSearch
  })

  // 통계 계산
  const totalSubjects = subjects.length
  const activeSubjects = subjects.filter((s) => s.status === "활성").length
  const totalQuestions = subjects.reduce((sum, s) => sum + (Number(s.totalQuestions) || 0), 0)

  const handleView = (subjectId) => {
  }

  const handleEdit = (subjectId) => {
  }

  const handleDelete = (subjectId) => {
  }

  // 직원 권한으로는 문제 추가 불가
  const handleAddQuestion = (subjectId) => {
    alert("직원 권한으로는 문제를 추가할 수 없습니다.")
  }



  const handleToggle = async (subjectId) => {
    if (expandedSubject === subjectId) {
      // 접기
      setExpandedSubject(null)
    } else {
      // 펼치기 - 문제 데이터 로드
      setExpandedSubject(subjectId)
      
      // 이미 로드된 데이터가 있으면 다시 로드하지 않음
      if (subjectQuestions[subjectId] && subjectQuestions[subjectId].length > 0) {
        return
      }
      
      try {
        setLoadingQuestions(prev => ({ ...prev, [subjectId]: true }))
        const subject = subjects.find(s => s.id === subjectId)
        if (subject) {
          // 실제 API 응답에서 이미 포함된 문제 목록 사용
          const originalSubDetail = subDetailsData.find(s => s.subDetailId === subjectId)
          const questions = originalSubDetail?.questions || []
          
          // 문제 데이터를 컴포넌트에서 사용할 형태로 변환
          const formattedQuestions = questions.map(question => ({
            id: question.questionId,
            question: question.questionText,
            type: question.questionType,
            points: 10, // 기본 배점
            createdDate: question.createdAt ? new Date(question.createdAt).toLocaleDateString('ko-KR') : '날짜 없음',
            detailSubject: subject.name,
            options: question.questionAnswer ? [question.questionAnswer] : [],
            correctAnswer: question.questionAnswer,
            explanation: question.explanation,
            memberId: question.memberId,
            memberName: question.memberName
          }))
          
          setSubjectQuestions(prev => ({
            ...prev,
            [subjectId]: formattedQuestions
          }))
        }
      } catch (error) {
        console.error(`세부과목 ${subjectId}의 문제 로드 실패:`, error)
        setSubjectQuestions(prev => ({
          ...prev,
          [subjectId]: []
        }))
      } finally {
        setLoadingQuestions(prev => ({ ...prev, [subjectId]: false }))
      }
    }
  }

  const handleQuestionClick = async (question, subject) => {
    try {
      // 문제 상세 정보를 API에서 가져오기
      const questionDetail = await getQuestionDetail(question.id)
      if (questionDetail) {
        // 세부과목 정보 추가
        setSelectedQuestion({
          ...questionDetail,
          detailSubject: subject.name
        })
      } else {
        // API에서 가져오지 못한 경우 기존 데이터 사용 (세부과목 정보 포함)
        setSelectedQuestion({
          ...question,
          detailSubject: subject.name
        })
      }
      setIsQuestionModalOpen(true)
    } catch (error) {
      console.error('문제 상세 정보 조회 실패:', error)
      // 에러 발생 시 기존 데이터 사용 (세부과목 정보 포함)
      setSelectedQuestion({
        ...question,
        detailSubject: subject.name
      })
      setIsQuestionModalOpen(true)
    }
  }

  const closeQuestionModal = () => {
    setIsQuestionModalOpen(false)
    setSelectedQuestion(null)
  }

  return (
    <React.Fragment>
      <PageLayout currentPage="exam" userRole="staff">
        <div className="flex">
          <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
          <main className="flex-1 p-6 max-w-7xl mx-auto">
            <div className="mb-6">
              <h1 className="text-2xl font-bold mb-2" style={{ color: "#2c3e50" }}>
                세부과목 문제 리스트
              </h1>
              <p className="text-gray-600">세부과목별 문제를 관리하고 새로운 문제를 추가할 수 있습니다.</p>
            </div>

            {/* 로딩 상태 */}
            {loading && (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600 mx-auto mb-4"></div>
                <p className="text-gray-600">세부과목 데이터를 불러오는 중...</p>
              </div>
            )}

            {/* 에러 상태 */}
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
                <div className="flex">
                  <div className="flex-shrink-0">
                    <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <div className="ml-3">
                    <h3 className="text-sm font-medium text-red-800">데이터 로드 실패</h3>
                    <div className="mt-2 text-sm text-red-700">
                      <p>{error}</p>
                    </div>
                    <div className="mt-4">
                      <button
                        onClick={() => window.location.reload()}
                        className="bg-red-100 text-red-800 px-3 py-2 rounded-md text-sm font-medium hover:bg-red-200"
                      >
                        다시 시도
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 통계 카드 */}
            {!loading && !error && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                <Card>
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="ml-4">
                        <p className="text-m font-medium text-gray-600">전체 세부과목</p>
                        <p className="text-3xl font-bold" style={{ color: "#3498db" }}>
                          {totalSubjects}개
                        </p>
                      </div>
                      <div className="bg-[#EFF6FF] rounded-full p-3 mr-4">
                        <FileText className="w-10 h-10" style={{ color: "#3498db" }} />
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="ml-4">
                        <p className="text-m font-medium text-gray-600">활성 세부과목</p>
                        <p className="text-3xl font-bold" style={{ color: "#1abc9c" }}>
                          {activeSubjects}개
                        </p>
                      </div>
                      <div className="bg-[#e4f5eb] rounded-full p-3 mr-4">
                        <BarChart3 className="w-10 h-10" style={{ color: "#1abc9c" }} />
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div className="ml-4">
                        <p className="text-m font-medium text-gray-600">총 문제 수</p>
                        <p className="text-3xl font-bold" style={{ color: "#b0c4de" }}>
                          {totalQuestions}개
                        </p>
                      </div>
                      <div className="bg-[#f5f5f5] rounded-full p-3 mr-4">
                        <Plus className="w-10 h-10" style={{ color: "#b0c4de" }} />
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}

            {/* 검색 */}
            {!loading && !error && (
              <div className="bg-white p-4 rounded-lg shadow-sm border mb-6">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                  <input
                    type="text"
                    placeholder="세부과목명으로 검색..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
                  />
                </div>
              </div>
            )}

            {/* 과목 목록 테이블 */}
            {!loading && !error && (
              <div className="bg-white rounded-lg shadow-sm border overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          세부과목 정보
                        </th>
                        <th className="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          문제 현황
                        </th>
                        <th className="py-3 px-4 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                          관리
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {filteredSubjects.map((subject) => (
                        <React.Fragment key={subject.id}>
                          <tr>
                            <td className="py-3 px-4">
                              <div>
                                <div className="text-sm font-medium text-gray-900">{subject.name}</div>
                                {subject.info && (
                                  <div className="text-xs text-gray-400 mt-1">
                                    {subject.info.length > 50 ? `${subject.info.substring(0, 50)}...` : subject.info}
                                  </div>
                                )}
                              </div>
                            </td>
                            <td className="py-3 px-4">
                              <div className="text-sm font-medium text-gray-900">총 {Number(subject.totalQuestions) || 0}개</div>
                            </td>
                            <td className="py-3 px-4">
                              <div className="flex justify-center">
                                <Button size="sm" variant="ghost" onClick={() => handleToggle(subject.id)} className="p-1">
                                  {expandedSubject === subject.id ? (
                                    <ChevronUp className="w-4 h-4" style={{ color: "#2c3e50" }} />
                                  ) : (
                                    <ChevronDown className="w-4 h-4" style={{ color: "#2c3e50" }} />
                                  )}
                                </Button>
                              </div>
                            </td>
                          </tr>
                          {expandedSubject === subject.id && (
                            <tr>
                              <td colSpan="3" className="py-0 px-0">
                                <div className="bg-gray-50 p-4 border-t">
                                  <h4 className="font-semibold text-gray-700 mb-3">세부과목 문제 목록</h4>
                                  <div className="space-y-3">
                                    {loadingQuestions[subject.id] ? (
                                      <div className="text-center py-4">
                                        <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-purple-600 mx-auto mb-2"></div>
                                        <p className="text-sm text-gray-500">문제를 불러오는 중...</p>
                                      </div>
                                    ) : subjectQuestions[subject.id]?.length > 0 ? (
                                      subjectQuestions[subject.id].map((question) => (
                                        <div
                                          key={question.id}
                                          className="bg-white p-4 rounded-lg border hover:shadow-md cursor-pointer transition-shadow"
                                          onClick={() => handleQuestionClick(question, subject)}
                                        >
                                          <div className="flex justify-between items-start mb-2">
                                            <div className="flex-1">
                                              <div className="flex items-center gap-2 mb-1">
                                                <span className="text-sm font-medium text-[#2c3e50]">
                                                  {question.type || '문제'}
                                                </span>
                                                <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                                                  question.type === '객관식' 
                                                    ? 'bg-blue-100 text-blue-800' 
                                                    : question.type === '서술형'
                                                    ? 'bg-green-100 text-green-800'
                                                    : 'bg-gray-100 text-gray-800'
                                                }`}>
                                                  {question.type || '문제'}
                                                </span>
                                              </div>
                                              <p className="text-sm text-gray-800 mb-2">{question.question}</p>
                                              <div className="flex items-center gap-4 text-xs text-gray-500">
                                                <span>출제일: {question.createdDate || '날짜 없음'}</span>
                                              </div>
                                            </div>
                                          </div>
                                        </div>
                                      ))
                                    ) : (
                                      <div className="text-center py-4 text-gray-500">등록된 문제가 없습니다.</div>
                                    )}
                                  </div>
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

            {!loading && !error && filteredSubjects.length === 0 && (
              <div className="text-center py-8">
                <p className="text-gray-500">검색 조건에 맞는 세부과목이 없습니다.</p>
              </div>
            )}
          </main>
        </div>
      </PageLayout>

      {isQuestionModalOpen && selectedQuestion && (
        <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold" style={{ color: "#2c3e50" }}>
                문제 상세정보
              </h2>
              <button onClick={closeQuestionModal} className="text-gray-500 hover:text-gray-700">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* 기본 정보 */}
              <div className="space-y-4">
                <div className="bg-gray-50 p-4 rounded-lg">
                  <h3 className="font-semibold text-gray-700 mb-3">기본 정보</h3>
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-sm text-gray-600">세부과목:</span>
                      <span className="text-sm font-medium">{selectedQuestion.detailSubject || '세부과목 정보 없음'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-sm text-gray-600">문제 유형:</span>
                      <span className="text-sm font-medium">{selectedQuestion.type}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-sm text-gray-600">출제일:</span>
                      <span className="text-sm font-medium">
                        {selectedQuestion.createdDate ? 
                          (typeof selectedQuestion.createdDate === 'string' && selectedQuestion.createdDate.includes('T') ? 
                            new Date(selectedQuestion.createdDate).toLocaleDateString('ko-KR') : 
                            selectedQuestion.createdDate) : 
                          '날짜 없음'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* 문제 내용 */}
              <div className="space-y-4">
                <div className="bg-gray-50 p-4 rounded-lg">
                  <h3 className="font-semibold text-gray-700 mb-3">문제 내용</h3>
                  <div className="bg-white p-4 rounded border">
                    <p className="text-gray-800 leading-relaxed">{selectedQuestion.question}</p>
                  </div>
                </div>

                {selectedQuestion.type === "객관식" && (
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h3 className="font-semibold text-gray-700 mb-3">선택지</h3>
                    <div className="space-y-2">
                      <div className="bg-white p-3 rounded border">
                        <span className="font-medium text-gray-700">1. </span>
                        <span className="text-gray-800">함수 선언문</span>
                      </div>
                      <div className="bg-white p-3 rounded border">
                        <span className="font-medium text-gray-700">2. </span>
                        <span className="text-gray-800">함수 표현식</span>
                      </div>
                      <div className="bg-white p-3 rounded border">
                        <span className="font-medium text-gray-700">3. </span>
                        <span className="text-gray-800">화살표 함수</span>
                      </div>
                      <div className="bg-green-50 p-3 rounded border border-green-200">
                        <span className="font-medium text-gray-700">4. </span>
                        <span className="text-gray-800">클래스 메서드</span>
                        <span className="ml-2 text-xs text-green-600 font-medium">(정답)</span>
                      </div>
                    </div>
                  </div>
                )}


              </div>
            </div>

            <div className="flex justify-end space-x-3 mt-6 pt-4 border-t">
              <Button onClick={closeQuestionModal} variant="outline" 
              className="px-4 py-2 bg-transparent hover:bg-gray-200">
                닫기
              </Button>
            </div>
          </div>
        </div>
      )}
    </React.Fragment>
  )
}
