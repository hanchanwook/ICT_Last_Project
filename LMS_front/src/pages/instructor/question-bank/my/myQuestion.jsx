import React, { useState, useEffect, useMemo } from "react"
import { useLocation } from "react-router-dom"
import { Search, Plus, Edit, Trash2, Eye, Filter, BookOpen, X, FileText, Calendar, Layers, CheckCircle } from "lucide-react"
import Editor from "@monaco-editor/react"
import { getQuestions, getInstructors, createQuestion, getQuestionsByTeacher, deactivateQuestion } from "@/api/kayoung/questionBankApi"
import { getAllSubject } from "@/api/suhyeon/courseApi"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getMenuItems } from "@/components/ui/menuConfig"
import { Card, CardContent } from "@/components/ui/card"

export default function InstructorExamQuestionBankPage() {
  const [questions, setQuestions] = useState([])
  const [filteredQuestions, setFilteredQuestions] = useState([])
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedSubject, setSelectedSubject] = useState("")
  const [selectedType, setSelectedType] = useState("")

  const [loading, setLoading] = useState(true)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newQuestion, setNewQuestion] = useState({
    question: "",
    type: "객관식",
    subject: "",
    options: ["", "", "", ""],
    correctAnswer: 0,
    explanation: "",
    codeBlock: "",
    expectedOutput: "",
    codeLanguage: "",
  })

  const [showEditModal, setShowEditModal] = useState(false)
  const [editingQuestion, setEditingQuestion] = useState(null)
  const [showDetailModal, setShowDetailModal] = useState(false)
  const [selectedQuestion, setSelectedQuestion] = useState(null)

  // 과목 데이터 상태 추가
  const [subjects, setSubjects] = useState([]) // 메인 과목 목록 (필터용)
  const [subDetails, setSubDetails] = useState([]) // 세부과목 목록 (새 문제 만들기용)
  const [subjectMapping, setSubjectMapping] = useState({}) // 과목-세부과목 매핑

  // 페이지네이션 상태 추가
  const [currentPage, setCurrentPage] = useState(1)
  const [totalQuestionsCount, setTotalQuestionsCount] = useState(0)
  const questionsPerPage = 5

  const location = useLocation()
  const sidebarMenuItems = getMenuItems('question-bank')
  const currentPath = location.pathname

  // 현재 사용자 ID 가져오기 (임시로 하드코딩)
  const getCurrentUserId = () => {
    try {
      // localStorage에서 사용자 정보 가져오기
      const userInfo = localStorage.getItem('currentUser');
      if (userInfo) {
        const user = JSON.parse(userInfo);
        return user.memberId ; // memberId가 있으면 사용, 없으면 기본값
      } else {
        return ; // 기본값
      }
    } catch (error) {
      return ; // 기본값
    }
  }

  // 페이지 변경 핸들러
  const handlePageChange = (page) => {
    setCurrentPage(page)
  }

  // 검색어 변경 핸들러
  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value)
    setCurrentPage(1) // 검색 시 첫 페이지로 이동
  }

  // 필터 변경 핸들러
  const handleFilterChange = (e) => {
    const { name, value } = e.target
    if (name === 'subject') {
      setSelectedSubject(value)
    } else if (name === 'type') {
      setSelectedType(value)
    }
    setCurrentPage(1) // 필터 변경 시 첫 페이지로 이동
  }

  // 내 문제 데이터 로드
  const loadMyQuestions = async () => {
    try {
      setLoading(true)
      const userId = getCurrentUserId()
      
      // 백엔드에서 사용자별로 필터링된 문제만 가져오기
      const response = await getQuestionsByTeacher()
      
      // 응답 구조에 따라 문제 목록 추출
      const myQuestions = response?.data?.questions || response?.questions || response || []
      
      // 백엔드 데이터 구조를 프론트엔드 구조로 변환
      const transformedQuestions = myQuestions.map(q => {
        // 객관식 문제의 경우 옵션 처리
        let options = []
        let correctAnswer = q.questionAnswer
        let correctAnswerIndex = null
        
        if (q.questionType === '객관식' && Array.isArray(q.options) && q.options.length > 0) {
          // 옵션 배열에서 텍스트만 추출
          options = q.options.map(opt => opt.optText)
          
          // 정답 찾기 (optIsCorrect가 1인 옵션)
          const correctOption = q.options.find(opt => opt.optIsCorrect === 1)
          if (correctOption) {
            correctAnswer = correctOption.optText
            // 정답 인덱스 찾기
            correctAnswerIndex = q.options.findIndex(opt => opt.optIsCorrect === 1)
          }
        }
        
        const transformedQuestion = {
          id: q.questionId,
          type: q.questionType,
          question: q.questionText,
          correctAnswer: correctAnswer,
          correctAnswerIndex: correctAnswerIndex, // 정답 인덱스 추가
          options: options,
          subject: q.subDetailName || '미분류',
          subjectId: q.subDetailId || null,
          mainSubject: q.subjectName || null,
          instructor: q.memberName || '알 수 없는 강사',
          instructorId: q.memberId,
          instructorEmail: q.memberEmail,
          createdDate: q.createdAt ? new Date(q.createdAt).toLocaleDateString() : '날짜 없음',
          explanation: q.explanation,
          status: q.status,
          optionCount: q.optionCount || 0
        }
        
        
        return transformedQuestion
      })
      setQuestions(transformedQuestions)
      setTotalQuestionsCount(transformedQuestions.length)
      
    } catch (err) {
      setQuestions([])
      setTotalQuestionsCount(0)
    } finally {
      setLoading(false)
    }
  }

  // 과목 데이터 로드 - 새로운 API 구조 사용
  const loadSubjectData = async () => {
    try {
      
      // getAllSubject API 사용
      const subjectsData = await getAllSubject()
      
      // getAllSubject API에서 과목명 추출
      const subjectsArray = subjectsData?.map(subject => subject.subjectName) || []
      
      setSubjects(subjectsArray)
      
      // 과목명 -> subDetailId 매핑 생성 (하나의 과목에 여러 subDetailId가 있을 수 있음)
      const mapping = {}
      subjectsData?.forEach(subject => {
        if (subject.subjectName) {
          // subDetailId가 배열인지 단일 값인지 확인
          if (Array.isArray(subject.subDetailId)) {
            // 배열인 경우: [subDetailId1, subDetailId2, ...]
            mapping[subject.subjectName] = subject.subDetailId
          } else if (subject.subDetailId) {
            // 단일 값인 경우: [subDetailId]
            mapping[subject.subjectName] = [subject.subDetailId]
          } else if (subject.subDetails && Array.isArray(subject.subDetails)) {
            // subDetails 배열에서 subDetailId 추출
            const subDetailIds = subject.subDetails.map(subDetail => subDetail.subDetailId).filter(id => id)
            mapping[subject.subjectName] = subDetailIds
          } else {
            // subDetailId가 없는 경우: 빈 배열
            mapping[subject.subjectName] = []
          }
        } else {
        }
      })
      setSubjectMapping(mapping)
      
      // 세부과목 목록도 동일한 데이터에서 추출
      const allSubDetails = []
      subjectsData?.forEach(subject => {
        if (subject.subDetails && Array.isArray(subject.subDetails)) {
          subject.subDetails.forEach(subDetail => {
            allSubDetails.push({
              id: subDetail.subDetailId,
              name: subDetail.subDetailName,
              mainSubject: subject.subjectName
            })
          })
        }
      })
      
      // 중복 제거
      const uniqueSubDetails = allSubDetails.filter((subDetail, index, self) => 
        index === self.findIndex(s => s.id === subDetail.id)
      )
      setSubDetails(uniqueSubDetails)
      
    } catch (err) {
      setSubjects([])
      setSubDetails([])
      setSubjectMapping({})
    }
  }

  // 데이터 초기화
  useEffect(() => {
    const initializeData = async () => {
      await loadSubjectData()
      await loadMyQuestions() // 내 문제 데이터 로드
    }

    initializeData()
  }, [])

  // 필터링된 문제 목록 계산 - subDetailId 기반 필터링
  const filteredQuestionsComputed = useMemo(() => {
    // questions가 배열인지 확인하고, 배열이 아니면 빈 배열 반환
    if (!Array.isArray(questions)) {
      return []
    }
    
    const filtered = questions.filter((question) => {
      const matchesSearch = !searchTerm || 
        question.question.toLowerCase().includes(searchTerm.toLowerCase()) ||
        question.subject.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (question.mainSubject && question.mainSubject.toLowerCase().includes(searchTerm.toLowerCase()))
      
      const matchesSubject = !selectedSubject || 
        question.subject === selectedSubject || 
        (question.mainSubject === selectedSubject) ||
        // subDetailId 기반 매칭 (주요 방법) - 배열 처리
        (selectedSubject && question.subjectId && Array.isArray(subjectMapping[selectedSubject]) && subjectMapping[selectedSubject].includes(question.subjectId)) ||
        // 기존 매핑 방식 (백업)
        (subjectMapping[selectedSubject] && subjectMapping[selectedSubject].includes(question.subject))
      
      // 선택된 과목이 있을 때 디버깅 정보 출력
      if (selectedSubject) {
        
      }
      
      // 디버깅을 위한 로그 (선택된 과목이 있을 때만)
      if (selectedSubject && !matchesSubject) {
        
      }
      
      const matchesType = !selectedType || question.type === selectedType

      return matchesSearch && matchesSubject && matchesType
    })
    
    // 필터링 결과 요약 로그
    if (selectedSubject || selectedType || searchTerm) {
      
    }
    
    return filtered
  }, [questions, searchTerm, selectedSubject, selectedType, subjectMapping])

  // 필터링된 결과를 상태에 저장
  useEffect(() => {
    setFilteredQuestions(filteredQuestionsComputed)
    setTotalQuestionsCount(filteredQuestionsComputed.length)
    setCurrentPage(1) // 필터링 시 1페이지로 리셋
  }, [filteredQuestionsComputed])

  const handleDeactivateQuestion = async (questionId) => {
    if (confirm("정말로 이 문제를 비활성화하시겠습니까?")) {
      try {
        await deactivateQuestion(questionId)
        alert("문제가 성공적으로 비활성화되었습니다.")
        await loadMyQuestions()
      } catch (error) {
        alert("문제 비활성화에 실패했습니다. 다시 시도해주세요.")
      }
    }
  }

  const getTypeColor = (type) => {
    switch (type) {
      case "객관식":
        return "text-[#1bac9c] bg-[#e4f5eb]"
      case "서술형":
        return "text-blue-600 bg-blue-50"
      // case "코드형":
      //   return "text-orange-600 bg-orange-50"
      default:
        return "text-gray-600 bg-gray-50"
    }
  }

  const handleCreateQuestion = async () => {
    if (!newQuestion.question.trim()) {
      alert("문제 내용을 입력해주세요.")
      return
    }
    
    if (!newQuestion.subject.trim()) {
      alert("세부 과목을 선택해주세요.")
      return
    }

    if (newQuestion.type === "객관식" && newQuestion.options.some((opt) => !opt.trim())) {
      alert("모든 선택지를 입력해주세요.")
      return
    }

    // 객관식 문제에서 선택지 중복 검사
    if (newQuestion.type === "객관식") {
      const optionTexts = newQuestion.options.map(opt => opt.trim());
      const uniqueOptions = new Set(optionTexts);
      
      if (uniqueOptions.size !== optionTexts.length) {
        alert("객관식 선택지가 중복됩니다. 모든 선택지를 다르게 입력해주세요.")
        return
      }
    }

    try {
      // 백엔드 API 호출을 위한 데이터 준비
      let questionData = {
        questionText: newQuestion.question,
        questionType: newQuestion.type,
        subDetailName: newQuestion.subject, // 세부과목명
        explanation: newQuestion.explanation || "",
      };

      // 문제 유형별로 전송 필드 정리
      switch (newQuestion.type) {
        case "객관식":
          questionData = {
            ...questionData,
            questionAnswer: newQuestion.options[newQuestion.correctAnswer] || "",
            options: newQuestion.options.map((option, index) => ({
              optText: option,
              optIsCorrect: index === newQuestion.correctAnswer ? 1 : 0
            }))
          };
          break;
        case "서술형":
          questionData = {
            ...questionData,
            questionAnswer: newQuestion.correctAnswer || "", // 사용자가 입력한 정답 사용
            
          };
          break;
        // case "코드형":
        //   questionData = {
        //     ...questionData,
        //     questionAnswer: newQuestion.correctAnswer || "",
        //     
        //   };
        //   break;
        default:
          questionData = {
            ...questionData,
            questionAnswer: "",
            
          };
      }

      // 백엔드 API 호출
      const createdQuestion = await createQuestion(questionData)
      
      // 성공 시 모달 닫기 및 폼 초기화
      setShowCreateModal(false)
      setNewQuestion({
        question: "",
        type: "객관식",
        subject: "",
        options: ["", "", "", ""],
        correctAnswer: 0,
        explanation: "",
        codeBlock: "",
        expectedOutput: "",
        codeLanguage: "",
      })
      
      // 문제 목록 새로고침
      await loadMyQuestions()
      
      alert("문제가 성공적으로 생성되었습니다.")
      
    } catch (error) {
      // 사용자에게 더 구체적인 에러 메시지 표시
      const errorMessage = error.response?.data?.message || error.message || "문제 생성에 실패했습니다."
      alert(`문제 생성 실패: ${errorMessage}`)
    }
  }

  const handleEditQuestion = (question) => {
    setEditingQuestion({
      ...question,
      options: question.options || ["", "", "", ""],
      // 객관식 문제의 경우 correctAnswer를 인덱스로 설정
      correctAnswer: question.type === "객관식" ? question.correctAnswerIndex : question.correctAnswer,
    })
    setShowEditModal(true)
  }

  const handleViewDetail = (question) => {
    setSelectedQuestion({ ...question })
    setShowDetailModal(true)
  }

  const handleUpdateQuestion = async () => {
    if (!editingQuestion.question.trim()) {
      alert("문제 내용을 입력해주세요.")
      return
    }
    
    if (!editingQuestion.subject.trim()) {
      alert("세부 과목을 선택해주세요.")
      return
    }

    if (editingQuestion.type === "객관식" && editingQuestion.options.some((opt) => !opt.trim())) {
      alert("모든 선택지를 입력해주세요.")
      return
    }

    // 객관식 문제에서 선택지 중복 검사
    if (editingQuestion.type === "객관식") {
      const optionTexts = editingQuestion.options.map(opt => opt.trim());
      const uniqueOptions = new Set(optionTexts);
      
      if (uniqueOptions.size !== optionTexts.length) {
        alert("객관식 선택지가 중복됩니다. 모든 선택지를 다르게 입력해주세요.")
        return
      }
    }

    try {
      // 백엔드 API 호출을 위한 데이터 준비
      let questionData = {
        questionText: editingQuestion.question,
        questionType: editingQuestion.type,
        subDetailName: editingQuestion.subject,
        explanation: editingQuestion.explanation || "",
      };

      // 문제 유형별로 전송 필드 정리
      switch (editingQuestion.type) {
        case "객관식":
          questionData = {
            ...questionData,
            questionAnswer: editingQuestion.options[editingQuestion.correctAnswer] || "",
            options: editingQuestion.options.map((option, index) => ({
              optText: option,
              optIsCorrect: index === editingQuestion.correctAnswer ? 1 : 0
            }))
          };
          break;
        case "서술형":
          questionData = {
            ...questionData,
            questionAnswer: editingQuestion.correctAnswer || "", // 사용자가 입력한 정답 사용
            options: [] // 생략 가능
          };
          break;
        case "코드형":
          questionData = {
            ...questionData,
            questionAnswer: editingQuestion.correctAnswer || "",
            options: [] // 생략 가능
          };
          break;
        default:
          questionData = {
            ...questionData,
            questionAnswer: "",
            options: []
          };
      }

      // 백엔드 API 호출 (updateQuestion 함수 사용)
      const { updateQuestion } = await import("@/api/kayoung/questionBankApi")
      await updateQuestion(editingQuestion.id, questionData)
      
      // 성공 시 모달 닫기 및 문제 목록 새로고침
      setShowEditModal(false)
      setEditingQuestion(null)
      await loadMyQuestions()
      
      alert("문제가 성공적으로 수정되었습니다.")
      
    } catch (error) {
      alert("문제 수정에 실패했습니다. 다시 시도해주세요.")
    }
  }

  const handleEditQuestionChange = (field, value) => {
    setEditingQuestion((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  const handleEditOptionChange = (index, value) => {
    setEditingQuestion((prev) => ({
      ...prev,
      options: prev.options.map((opt, i) => (i === index ? value : opt)),
    }))
  }

  const handleQuestionChange = (field, value) => {
    setNewQuestion((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  const handleOptionChange = (index, value) => {
    setNewQuestion((prev) => ({
      ...prev,
      options: prev.options.map((opt, i) => (i === index ? value : opt)),
    }))
  }

  // 페이지네이션 계산
  const totalPages = Math.ceil(totalQuestionsCount / questionsPerPage)
  const pageNumbers = useMemo(() => {
    const pages = []
    const maxVisiblePages = 5
    let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2))
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1)
    
    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1)
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i)
    }
    return pages
  }, [currentPage, totalPages])

  // 현재 페이지의 문제들
  const currentQuestions = useMemo(() => {
    const startIndex = (currentPage - 1) * questionsPerPage
    const endIndex = startIndex + questionsPerPage
    return filteredQuestions.slice(startIndex, endIndex)
  }, [filteredQuestions, currentPage, questionsPerPage])

  // 코드 언어 감지 함수
  const detectCodeLanguage = (questionText, code) => {
    const text = (questionText + ' ' + code).toLowerCase()
    
    if (text.includes('python') || text.includes('print(') || text.includes('def ') || text.includes('import ')) {
      return 'python'
    }
    if (text.includes('java') || text.includes('public class') || text.includes('system.out') || text.includes('string[]')) {
      return 'java'
    }
    if (text.includes('javascript') || text.includes('js') || text.includes('console.log') || text.includes('function(')) {
      return 'javascript'
    }
    if (text.includes('c++') || text.includes('cout') || text.includes('#include')) {
      return 'cpp'
    }
    if (text.includes('c#') || text.includes('console.writeline')) {
      return 'csharp'
    }
    if (text.includes('html') || text.includes('<html') || text.includes('<div')) {
      return 'html'
    }
    if (text.includes('css') || text.includes('{') && text.includes(':')) {
      return 'css'
    }
    if (text.includes('sql') || text.includes('select') || text.includes('from') || text.includes('where')) {
      return 'sql'
    }
    
    return 'javascript' // 기본값
  }

  if (loading) {
    return (
      <React.Fragment>
        <PageLayout currentPage="question-bank">
          <div className="flex">
            <Sidebar title="문제 은행" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="animate-pulse space-y-4">
                <div className="h-8 bg-gray-200 rounded w-1/4"></div>
                <div className="h-12 bg-gray-200 rounded"></div>
                <div className="space-y-3">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="h-24 bg-gray-200 rounded"></div>
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
      <PageLayout currentPage="question-bank" userRole="instructor">
        <div className="flex">
          <Sidebar title="문제 은행 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
          <main className="flex-1 p-6 max-w-6xl mx-auto">
            <div className="space-y-6">
              {/* 페이지 헤더 */}
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">내 문제 은행</h1>
                  <p className="text-gray-600 mt-1">내가 출제한 문제들을 관리하고 재사용할 수 있습니다.</p>
                </div>
                <button
                  onClick={() => setShowCreateModal(true)}
                  className="bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white 
                  px-4 py-2 rounded-lg flex items-center gap-2 transition-colors"
                >
                  <Plus className="w-4 h-4" />새 문제 만들기
                </button>
              </div>

              {/* 통계 카드 */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
                <Card >
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-m text-gray-600 mb-1">전체 문제</p>
                        <p className="text-3xl font-bold text-[#3498db]">{questions.length}개</p>
                      </div>
                      <div className="p-2 bg-[#EFF6FF] rounded-full">
                        <Layers className="w-10 h-10 text-[#3498db]" />
                      </div>
                    </div>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-m text-gray-600 mb-1">객관식 문제</p>
                        <p className="text-3xl font-bold text-[#1abc9c]">{questions.filter(q => q.type === "객관식").length}개</p>
                      </div>
                      <div className="p-2 bg-[#e4f5eb] rounded-full">
                        <CheckCircle className="w-10 h-10 text-[#1abc9c]" />
                      </div>
                    </div>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-m text-gray-600 mb-1">서술형 문제</p>
                        <p className="text-3xl font-bold text-[#b0c4de]">{questions.filter(q => q.type === "서술형").length}개</p>
                      </div>
                      <div className="p-2 bg-[#EFF6FF] rounded-full">
                        <Edit className="w-10 h-10 text-[#b0c4de]" />
                      </div>
                    </div>
                  </CardContent>
                </Card>
                {/* <Card className="border-l-4 border-l-orange-500">
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm text-gray-600 mb-1">코드형 문제</p>
                        <p className="text-xl font-bold text-orange-600">{questions.filter(q => q.type === "코드형").length}개</p>
                      </div>
                      <div className="p-2 bg-orange-50 rounded-lg">
                        <FileText className="w-5 h-5 text-orange-600" />
                      </div>
                    </div>
                  </CardContent>
                </Card> */}
              </div>

              {/* 검색 및 필터 */}
              <div className="bg-white p-4 rounded-lg ">
                <div className="flex items-center gap-4">
                  <div className="flex-1 relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                    <input
                      type="text"
                      placeholder="문제 내용이나 과목명으로 검색..."
                      value={searchTerm}
                      onChange={handleSearchChange}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    <Filter className="w-4 h-4 text-gray-500" />
                    <span className="text-sm text-gray-600">필터:</span>
                  </div>
                  <select
                    name="subject"
                    value={selectedSubject}
                    onChange={handleFilterChange}
                    className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 w-32"
                  >
                    <option value="">전체 과목</option>
                    {subjects.map((subject) => (
                      <option key={subject} value={subject}>
                        {subject}
                      </option>
                    ))}
                  </select>

                  <select
                    name="type"
                    value={selectedType}
                    onChange={handleFilterChange}
                    className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">전체 유형</option>
                    <option value="객관식">객관식</option>
                    <option value="서술형">서술형</option>
                    {/* <option value="코드형">코드형</option> */}
                  </select>
                </div>
              </div>

              {/* 문제 목록 */}
              <div className="space-y-4">
                {(currentQuestions || []).length === 0 ? (
                  <div className="bg-white p-8 rounded-lg  text-center">
                    <BookOpen className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-gray-900 mb-2">검색 결과가 없습니다</h3>
                    <p className="text-gray-600">다른 검색어나 필터 조건을 시도해보세요.</p>
                  </div>
                ) : (
                  (currentQuestions || []).map((question) => (
                    <div key={question.id} className="bg-white p-6 rounded-lg  hover:shadow-md transition-shadow">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-3">
                            <span className={`px-2 py-1 rounded-full text-xs font-medium ${getTypeColor(question.type)}`}>
                              {question.type}
                            </span>
                            <span className="text-sm text-gray-600">
                              {question.mainSubject && `${question.mainSubject} - `}{question.subject}
                            </span>
                            <div className="flex items-center gap-2 flex-shrink-0 ml-auto">
                              <button 
                                onClick={() => handleViewDetail(question)}
                                className="p-2 text-[#1abc9c] hover:bg-green-50 rounded-lg transition-colors
                                hover:scale-110"
                                title="상세보기"
                              >
                                <Eye className="w-4 h-4" />
                              </button>
                              <button
                                onClick={() => handleEditQuestion(question)}
                                className="p-2 text-[#b0c4de] hover:bg-blue-50 
                                rounded-lg transition-colors hover:scale-110"
                                title="수정하기"
                              >
                                <Edit className="w-4 h-4" />
                              </button>
                              <button
                                onClick={() => handleDeactivateQuestion(question.id)}
                                className="p-2 text-red-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors
                                hover:scale-110"
                                title="비활성화하기"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </div>
                          </div>

                          <h3 className="text-lg font-medium text-gray-900 mb-2 line-clamp-2">{question.question}</h3>

                          {question.type === "객관식" && question.options && (
                            <div className="mb-3">
                              <p className="text-sm text-gray-600 mb-1">선택지:</p>
                              <div className="grid grid-cols-2 gap-2">
                                {question.options.map((option, index) => {
                                  // option이 객체인 경우 optText를 사용, 문자열인 경우 그대로 사용
                                  const optionText = typeof option === 'object' && option.optText ? option.optText : option;
                                  return (
                                    <span
                                      key={index}
                                      className={`text-sm px-2 py-1 rounded ${
                                        optionText === question.correctAnswer
                                          ? "bg-green-50 text-green-700 font-medium"
                                          : "bg-gray-50 text-gray-600"
                                      }`}
                                    >
                                      {index + 1}. {optionText}
                                    </span>
                                  );
                                })}
                              </div>
                            </div>
                          )}

                          {/* 서술형 정답 표시 */}
                          {question.type === "서술형" && question.correctAnswer && (
                            <div className="mb-3">
                              <p className="text-sm text-gray-600 mb-1">정답:</p>
                              <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                                <pre className="text-sm text-green-700 whitespace-pre-wrap">{question.correctAnswer}</pre>
                              </div>
                            </div>
                          )}

                          {/* 코드형 정답 표시 */}
                          {/* {question.type === "코드형" && question.correctAnswer && (
                            <div className="mb-3">
                              <p className="text-sm text-gray-600 mb-1">정답:</p>
                              <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                                <pre className="text-sm text-green-700 whitespace-pre-wrap font-mono">{question.correctAnswer}</pre>
                              </div>
                            </div>
                          )} */}

                          <div className="flex items-center gap-4 text-sm text-gray-600">
                            <div className="flex items-center gap-1">
                              <Calendar className="w-3 h-3" />
                              생성일: {new Date(question.createdDate).toLocaleDateString()}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>

              {/* 페이지네이션 */}
              {totalPages > 1 && (
                <div className="flex justify-center mt-8">
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handlePageChange(Math.max(1, currentPage - 1))}
                      disabled={currentPage === 1}
                      className="px-3 py-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      이전
                    </button>
                    
                    {pageNumbers.map((page) => (
                      <button
                        key={page}
                        onClick={() => handlePageChange(page)}
                        className={`px-3 py-2 rounded-lg transition-colors ${
                          currentPage === page
                            ? "bg-blue-600 text-white"
                            : "text-gray-600 hover:text-gray-900 hover:bg-gray-100"
                        }`}
                      >
                        {page}
                      </button>
                    ))}
                    
                    <button
                      onClick={() => handlePageChange(Math.min(totalPages, currentPage + 1))}
                      disabled={currentPage === totalPages}
                      className="px-3 py-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      다음
                    </button>
                  </div>
                </div>
                          )}
          </div>
        </main>
      </div>
    </PageLayout>

      {/* 새 문제 만들기 모달 */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-bold text-gray-900">새 문제 만들기</h2>
                <button onClick={() => setShowCreateModal(false)} className="text-gray-400 hover:text-gray-600">
                  <X className="w-6 h-6" />
                </button>
              </div>

              <div className="space-y-6">
                {/* 기본 정보 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">문제 유형</label>
                    <select
                      value={newQuestion.type}
                      onChange={(e) => handleQuestionChange("type", e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="객관식">객관식</option>
                      <option value="서술형">서술형</option>
                      {/* <option value="코드형">코드형</option> */}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">세부 과목</label>
                    {/* 세부 과목 목록 불러와야함 */}
                    <select
                      value={newQuestion.subject}
                      onChange={(e) => handleQuestionChange("subject", e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="">세부 과목을 선택하세요</option>
                      {subDetails.map((subDetail) => (
                        <option key={subDetail.id} value={subDetail.name}>
                          {subDetail.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>



                {/* 문제 내용 */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">문제 내용</label>
                  <textarea
                    value={newQuestion.question}
                    onChange={(e) => handleQuestionChange("question", e.target.value)}
                    placeholder="문제 내용을 입력하세요"
                    rows={4}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* 객관식 선택지 */}
                {newQuestion.type === "객관식" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">선택지</label>
                    <div className="space-y-3">
                      {newQuestion.options.map((option, index) => {
                        // option이 객체인 경우 optText를 사용, 문자열인 경우 그대로 사용
                        const optionText = typeof option === 'object' && option.optText ? option.optText : option;
                        return (
                          <div key={index} className="flex items-center gap-3">
                            <input
                              type="radio"
                              name="correctAnswer"
                              checked={newQuestion.correctAnswer === index}
                              onChange={() => handleQuestionChange("correctAnswer", index)}
                              className="text-blue-600"
                            />
                            <span className="text-sm font-medium text-gray-700 w-8">{index + 1}.</span>
                            <input
                              type="text"
                              value={optionText}
                              onChange={(e) => handleOptionChange(index, e.target.value)}
                              placeholder={`선택지 ${index + 1}`}
                              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                          </div>
                        );
                      })}
                    </div>
                    <p className="text-sm text-gray-600 mt-2">정답을 선택해주세요.</p>
                  </div>
                )}

                {/* 서술형 정답 입력 */}
                {newQuestion.type === "서술형" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">정답 (선택사항)</label>
                    <textarea
                      value={newQuestion.correctAnswer || ""}
                      onChange={(e) => handleQuestionChange("correctAnswer", e.target.value)}
                      placeholder="서술형 문제의 정답을 입력하세요 (선택사항)"
                      rows={4}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    <p className="text-sm text-gray-600 mt-2">서술형 문제의 정답을 입력하면 타강사들이 참고할 수 있습니다.</p>
                  </div>
                )}

                {/* 코드블럭형 문제 입력 */}
                {newQuestion.type === "코드형" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">코드 블록</label>
                    <div className="space-y-4">
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <label className="block text-sm text-gray-600">문제 코드</label>
                          <select
                            value={newQuestion.codeLanguage}
                            onChange={(e) => handleQuestionChange("codeLanguage", e.target.value)}
                            className="px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                          >
                            <option value="javascript">JavaScript</option>
                            <option value="python">Python</option>
                            <option value="java">Java</option>
                            <option value="cpp">C++</option>
                            <option value="csharp">C#</option>
                            <option value="php">PHP</option>
                            <option value="ruby">Ruby</option>
                            <option value="go">Go</option>
                            <option value="rust">Rust</option>
                            <option value="typescript">TypeScript</option>
                            <option value="html">HTML</option>
                            <option value="css">CSS</option>
                            <option value="sql">SQL</option>
                          </select>
                        </div>
                        <div className="border border-gray-300 rounded-lg overflow-hidden">
                          <Editor
                            height="200px"
                            language={newQuestion.codeLanguage}
                            value={newQuestion.codeBlock || ""}
                            onChange={(value) => handleQuestionChange("codeBlock", value || "")}
                            theme="vs-dark"
                            options={{
                              minimap: { enabled: false },
                              scrollBeyondLastLine: false,
                              fontSize: 14,
                              lineNumbers: "on",
                              roundedSelection: false,
                              scrollbar: {
                                vertical: "visible",
                                horizontal: "visible"
                              }
                            }}
                          />
                        </div>
                      </div>
                      <div>
                        <label className="block text-sm text-gray-600 mb-2">예상 출력</label>
                        <div className="border border-gray-300 rounded-lg overflow-hidden">
                          <Editor
                            height="120px"
                            defaultLanguage="plaintext"
                            value={newQuestion.expectedOutput || ""}
                            onChange={(value) => handleQuestionChange("expectedOutput", value || "")}
                            theme="vs-light"
                            options={{
                              minimap: { enabled: false },
                              scrollBeyondLastLine: false,
                              fontSize: 14,
                              lineNumbers: "on",
                              roundedSelection: false,
                              scrollbar: {
                                vertical: "visible",
                                horizontal: "visible"
                              }
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* 해설 */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">해설 (선택사항)</label>
                  <textarea
                    value={newQuestion.explanation}
                    onChange={(e) => handleQuestionChange("explanation", e.target.value)}
                    placeholder="문제 해설을 입력하세요"
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              {/* 버튼 */}
              <div className="flex justify-end gap-3 mt-8 pt-6 border-t">
                <button
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  취소
                </button>
                <button
                  onClick={handleCreateQuestion}
                  className="px-4 py-2 bg-[#1abc9c] hover:bg-[rgb(10,150,120)]
                  text-white rounded-lg transition-colors"
                >
                  문제 생성
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
      {/* 문제 수정 모달 */}
      {showEditModal && editingQuestion && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-bold text-gray-900">문제 수정</h2>
                <button
                  onClick={() => {
                    setShowEditModal(false)
                    setEditingQuestion(null)
                  }}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              <div className="space-y-6">
                {/* 기본 정보 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">문제 유형</label>
                    <div className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-700">
                      {editingQuestion.type}
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">과목</label>
                    <div className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-700">
                      {editingQuestion.subject}
                    </div>
                  </div>
                </div>

                {/* 문제 내용 */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">문제 내용</label>
                  <textarea
                    value={editingQuestion.question}
                    onChange={(e) => handleEditQuestionChange("question", e.target.value)}
                    placeholder="문제 내용을 입력하세요"
                    rows={4}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* 객관식 선택지 */}
                {editingQuestion.type === "객관식" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">선택지</label>
                    <div className="space-y-3">
                      {editingQuestion.options.map((option, index) => {
                        // option이 객체인 경우 optText를 사용, 문자열인 경우 그대로 사용
                        const optionText = typeof option === 'object' && option.optText ? option.optText : option;
                        return (
                          <div key={index} className="flex items-center gap-3">
                            <input
                              type="radio"
                              name="editCorrectAnswer"
                              checked={editingQuestion.correctAnswer === index}
                              onChange={() => handleEditQuestionChange("correctAnswer", index)}
                              className="text-blue-600"
                            />
                            <span className="text-sm font-medium text-gray-700 w-8">{index + 1}.</span>
                            <input
                              type="text"
                              value={optionText}
                              onChange={(e) => handleEditOptionChange(index, e.target.value)}
                              placeholder={`선택지 ${index + 1}`}
                              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                          </div>
                        );
                      })}
                    </div>
                    <p className="text-sm text-gray-600 mt-2">정답을 선택해주세요.</p>
                    
                    {/* 객관식 해설 */}
                    <div className="mt-4">
                      <label className="block text-sm font-medium text-gray-700 mb-2">해설 (선택사항)</label>
                      <textarea
                        value={editingQuestion.explanation || ""}
                        onChange={(e) => handleEditQuestionChange("explanation", e.target.value)}
                        placeholder="객관식 문제의 해설을 입력하세요"
                        rows={3}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>
                )}

                {/* 서술형 정답 입력 */}
                {editingQuestion.type === "서술형" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">정답 (선택사항)</label>
                    <textarea
                      value={editingQuestion.correctAnswer || ""}
                      onChange={(e) => handleEditQuestionChange("correctAnswer", e.target.value)}
                      placeholder="서술형 문제의 정답을 입력하세요 (선택사항)"
                      rows={4}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    <p className="text-sm text-gray-600 mt-2">서술형 문제의 정답을 입력하면 학생들이 참고할 수 있습니다.</p>
                  </div>
                )}

                {/* 코드블럭형 문제 입력 */}
                {editingQuestion.type === "코드형" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">코드 블록</label>
                    <div className="space-y-4">
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <label className="block text-sm text-gray-600">문제 코드</label>
                          <select
                            value={editingQuestion.codeLanguage || "javascript"}
                            onChange={(e) => handleEditQuestionChange("codeLanguage", e.target.value)}
                            className="px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                          >
                            <option value="javascript">JavaScript</option>
                            <option value="python">Python</option>
                            <option value="java">Java</option>
                            <option value="cpp">C++</option>
                            <option value="csharp">C#</option>
                            <option value="php">PHP</option>
                            <option value="ruby">Ruby</option>
                            <option value="go">Go</option>
                            <option value="rust">Rust</option>
                            <option value="typescript">TypeScript</option>
                            <option value="html">HTML</option>
                            <option value="css">CSS</option>
                            <option value="sql">SQL</option>
                          </select>
                        </div>
                        <div className="border border-gray-300 rounded-lg overflow-hidden">
                          <Editor
                            height="200px"
                            language={editingQuestion.codeLanguage || "javascript"}
                            value={editingQuestion.codeBlock || ""}
                            onChange={(value) => handleEditQuestionChange("codeBlock", value || "")}
                            theme="vs-dark"
                            options={{
                              minimap: { enabled: false },
                              scrollBeyondLastLine: false,
                              fontSize: 14,
                              lineNumbers: "on",
                              roundedSelection: false,
                              scrollbar: {
                                vertical: "visible",
                                horizontal: "visible"
                              }
                            }}
                          />
                        </div>
                      </div>
                      <div>
                        <label className="block text-sm text-gray-600 mb-2">예상 출력</label>
                        <div className="border border-gray-300 rounded-lg overflow-hidden">
                          <Editor
                            height="120px"
                            defaultLanguage="plaintext"
                            value={editingQuestion.expectedOutput || ""}
                            onChange={(value) => handleEditQuestionChange("expectedOutput", value || "")}
                            theme="vs-light"
                            options={{
                              minimap: { enabled: false },
                              scrollBeyondLastLine: false,
                              fontSize: 14,
                              lineNumbers: "on",
                              roundedSelection: false,
                              scrollbar: {
                                vertical: "visible",
                                horizontal: "visible"
                              }
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* 해설 - 객관식이 아닌 경우에만 표시 */}
                {editingQuestion.type !== "객관식" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">해설 (선택사항)</label>
                    <textarea
                      value={editingQuestion.explanation || ""}
                      onChange={(e) => handleEditQuestionChange("explanation", e.target.value)}
                      placeholder="문제 해설을 입력하세요"
                      rows={3}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                )}
              </div>

              {/* 버튼 */}
              <div className="flex justify-end gap-3 mt-8 pt-6 border-t">
                <button
                  onClick={() => {
                    setShowEditModal(false)
                    setEditingQuestion(null)
                  }}
                  className="px-4 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  취소
                </button>
                <button
                  onClick={handleUpdateQuestion}
                  className="px-4 py-2 bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white rounded-lg transition-colors"
                >
                  수정 완료
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 문제 상세보기 모달 */}
      {showDetailModal && selectedQuestion && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-bold text-gray-900">문제 상세보기</h2>
                <button 
                  onClick={() => {
                    setShowDetailModal(false)
                    setSelectedQuestion(null)
                  }} 
                  className="text-gray-400 hover:text-gray-600"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              <div className="space-y-6">
                {/* 문제 기본 정보 */}
                <div className="bg-gray-50 p-3 rounded-lg">
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <p className="text-gray-500 mb-1">문제 유형</p>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${getTypeColor(selectedQuestion.type)}`}>
                        {selectedQuestion.type}
                      </span>
                    </div>
                    <div>
                      <p className="text-gray-500 mb-1">세부 과목</p>
                      <p className="font-medium">{selectedQuestion.subject}</p>
                    </div>
                  </div>
                </div>

                {/* 문제 내용 */}
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">문제 내용</h3>
                  <div className="bg-white border border-gray-200 rounded-lg p-4">
                    <pre className="whitespace-pre-wrap text-gray-800 font-medium">{selectedQuestion.question}</pre>
                  </div>
                </div>

                {/* 객관식 선택지 - 객관식일 때만 표시 */}
                {selectedQuestion.type === "객관식" && selectedQuestion.options && (
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-3">선택지</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {selectedQuestion.options.map((option, index) => {
                        // option이 객체인 경우 optText를 사용, 문자열인 경우 그대로 사용
                        const optionText = typeof option === 'object' && option.optText ? option.optText : option;
                        return (
                          <div
                            key={index}
                            className={`p-3 rounded-lg border-2 ${
                              optionText === selectedQuestion.correctAnswer
                                ? "border-green-500 bg-green-50"
                                : "border-gray-200 bg-gray-50"
                            }`}
                          >
                            <div className="flex items-center gap-2">
                              <span className={`w-6 h-6 rounded-full flex items-center justify-center text-sm font-medium ${
                                optionText === selectedQuestion.correctAnswer
                                  ? "bg-green-500 text-white"
                                  : "bg-gray-300 text-gray-600"
                              }`}>
                                {index + 1}
                              </span>
                              <span className={optionText === selectedQuestion.correctAnswer ? "font-medium text-green-700" : "text-gray-700"}>
                                {optionText}
                              </span>
                              {optionText === selectedQuestion.correctAnswer && (
                                <span className="ml-auto text-green-600 font-medium">정답</span>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* 객관식 정답과 해설 */}
                {selectedQuestion.type === "객관식" && (
                  <div className="space-y-4">
                    {/* 정답 */}
                    {selectedQuestion.correctAnswer && (
                      <div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-3">정답</h3>
                        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                          <pre className="text-sm text-green-700 whitespace-pre-wrap">{selectedQuestion.correctAnswer}</pre>
                        </div>
                      </div>
                    )}
                    
                    {/* 해설 */}
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900 mb-3">해설</h3>
                      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                        {selectedQuestion.explanation ? (
                          <pre className="text-sm text-blue-700 whitespace-pre-wrap">{selectedQuestion.explanation}</pre>
                        ) : (
                          <p className="text-sm text-gray-500 italic">해설 없음</p>
                        )}
                      </div>
                    </div>
                  </div>
                )}

                {/* 서술형 정답과 해설 */}
                {selectedQuestion.type === "서술형" && (
                  <div className="space-y-4">
                    {/* 정답 */}
                    {selectedQuestion.correctAnswer && (
                      <div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-3">정답</h3>
                        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                          <pre className="text-sm text-green-700 whitespace-pre-wrap">{selectedQuestion.correctAnswer}</pre>
                        </div>
                      </div>
                    )}
                    
                    {/* 해설 */}
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900 mb-3">해설</h3>
                      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                        {selectedQuestion.explanation ? (
                          <pre className="text-sm text-blue-700 whitespace-pre-wrap">{selectedQuestion.explanation}</pre>
                        ) : (
                          <p className="text-sm text-gray-500 italic">해설 없음</p>
                        )}
                      </div>
                    </div>
                  </div>
                )}

                {/* 코드형 정답 - Monaco Editor 사용 */}
                {selectedQuestion.type === "코드형" && selectedQuestion.correctAnswer && (
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <h3 className="text-lg font-semibold text-gray-900">정답</h3>
                      <span className="px-3 py-1 bg-orange-100 text-orange-700 text-sm font-medium rounded-full">
                        {detectCodeLanguage(selectedQuestion.question, selectedQuestion.correctAnswer).toUpperCase()}
                      </span>
                    </div>
                    <div className="border border-gray-200 rounded-lg overflow-hidden">
                      <Editor
                        height="300px"
                        defaultLanguage={detectCodeLanguage(selectedQuestion.question, selectedQuestion.correctAnswer)}
                        value={selectedQuestion.correctAnswer}
                        options={{
                          readOnly: true,
                          minimap: { enabled: false },
                          scrollBeyondLastLine: false,
                          fontSize: 14,
                          lineNumbers: "on",
                          wordWrap: "on",
                          theme: "vs-light",
                          automaticLayout: true
                        }}
                      />
                    </div>
                  </div>
                )}

                {/* 생성 정보 */}
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">생성 정보</h3>
                  <div className="grid grid-cols-1 gap-4">
                    <div className="bg-blue-50 p-4 rounded-lg">
                      <div className="flex items-center gap-2 mb-2">
                        <Calendar className="w-4 h-4 text-blue-600" />
                        <span className="text-sm text-gray-600">생성일</span>
                      </div>
                      <p className="text-lg font-semibold text-blue-600">{selectedQuestion.createdDate}</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* 버튼 */}
              <div className="flex justify-end gap-3 mt-8 pt-6 border-t">
                <button
                  onClick={() => {
                    setShowDetailModal(false)
                    setSelectedQuestion(null)
                  }}
                  className="px-4 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  닫기
                </button>
                <button
                  onClick={() => {
                    setShowDetailModal(false)
                    setSelectedQuestion(null)
                    handleEditQuestion(selectedQuestion)
                  }}
                  className="px-4 py-2 bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white rounded-lg transition-colors flex items-center gap-2"
                >
                  <Edit className="w-4 h-4" />
                  수정하기
                </button>
                <button
                  onClick={async () => {
                    if (confirm("정말로 이 문제를 삭제제하시겠습니까?")) {
                      try {
                        await handleDeactivateQuestion(selectedQuestion.id)
                        setShowDetailModal(false)
                        setSelectedQuestion(null)
                      } catch (error) {
                        // 에러는 handleDeactivateQuestion에서 이미 처리됨
                      }
                    }
                  }}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors flex items-center gap-2"
                >
                  <Trash2 className="w-4 h-4" />
                  삭제하기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </React.Fragment>
  )
}

