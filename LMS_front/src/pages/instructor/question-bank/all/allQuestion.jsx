import React, { useState, useEffect, useCallback, useMemo } from "react"
import { useLocation } from "react-router-dom"
import { getQuestions, updateQuestion, deactivateQuestion, getInstructors, getQuestionBankStats } from "@/api/kayoung/questionBankApi"
import { getAllSubject } from "@/api/suhyeon/courseApi"
import { Search, Filter, BookOpen, Code, FileText, Users, Eye, Edit, X, Trash2, Calendar, Award,RefreshCw,AlertCircle, CheckCircle, Layers, Code2} from "lucide-react"
import Editor from "@monaco-editor/react"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getMenuItems } from "@/components/ui/menuConfig"
import { Card, CardContent } from "@/components/ui/card"

export default function InstructorQuestionBankAllPage() {
  // 문제 데이터 상태
  const [questions, setQuestions] = useState([])
  const [filteredQuestions, setFilteredQuestions] = useState([])
  
  // 검색 및 필터링 상태
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedSubject, setSelectedSubject] = useState("")
  const [selectedType, setSelectedType] = useState("")
  const [selectedInstructor, setSelectedInstructor] = useState("")
  
  // 로딩 및 에러 상태 (세분화)
  const [questionsLoading, setQuestionsLoading] = useState(true)
  const [filtersLoading, setFiltersLoading] = useState(true)
  const [statsLoading, setStatsLoading] = useState(true)
  const [error, setError] = useState(null)
  
  // 현재 로그인한 사용자 정보
  const [currentUser, setCurrentUser] = useState(null)
  const [currentUserId, setCurrentUserId] = useState(null)
  
  // 모달 상태
  const [isEditModalOpen, setIsEditModalOpen] = useState(false)
  const [editingQuestion, setEditingQuestion] = useState(null)
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false)
  const [selectedQuestion, setSelectedQuestion] = useState(null)
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)
  const [deletingQuestionId, setDeletingQuestionId] = useState(null)
  
  // 필터 옵션 데이터
  const [instructors, setInstructors] = useState([])
  const [subjects, setSubjects] = useState([])
  const [subjectMapping, setSubjectMapping] = useState({}) // 과목-세부과목 매핑
  
  // 통계 데이터
  const [questionStats, setQuestionStats] = useState({
    totalQuestions: 0,
    objectiveCount: 0,
    descriptiveCount: 0,
    codeCount: 0
  })
  
  // 데이터 소스 상태 (실제 DB 데이터인지 샘플 데이터인지)
  const [dataSource, setDataSource] = useState('loading') // 'db', 'sample', 'loading', 'error'
  
  // 페이지네이션 상태
  const [currentPage, setCurrentPage] = useState(1)
  const [totalQuestionsCount, setTotalQuestionsCount] = useState(0)
  const questionsPerPage = 5

  const location = useLocation()
  const sidebarMenuItems = getMenuItems('question-bank')
  const currentPath = location.pathname

  // 문제 데이터 로드 (클라이언트 사이드 필터링 사용)
  const fetchQuestionsData = useCallback(async () => {
    // 모달이 열려있으면 데이터 로딩 건너뛰기
    if (isEditModalOpen || isDetailModalOpen || isDeleteModalOpen) {
      return
    }
    
    try {
      setQuestionsLoading(true)
      setError(null)
      
      // getQuestions 함수로 원복
      const questionsResponse = await getQuestions()
      
      // 백엔드 응답 구조에 맞게 문제 목록 추출
      const questionsArray = questionsResponse?.data?.questions?.questions || questionsResponse?.questions || questionsResponse || []
      
      // 강사 정보 추출
      const instructorsArray = questionsResponse?.data?.instructors || []
      
      // 강사 ID를 키로 하는 맵 생성
      const instructorMap = {}
      instructorsArray.forEach(instructor => {
        instructorMap[instructor.memberId] = instructor
      })
      
      const transformedQuestions = questionsArray.map(q => {
        // 강사 이름 처리 개선 - instructors 배열에서 매핑
        
        let instructorName = '알 수 없는 강사'
        let instructorEmail = ''
        
        // instructors 배열에서 해당 memberId로 강사 정보 찾기
        const instructorInfo = instructorMap[q.memberId]
        if (instructorInfo) {
          instructorName = instructorInfo.memberName || '알 수 없는 강사'
          instructorEmail = instructorInfo.memberEmail || ''
        } else if (q.memberName && q.memberName.trim() !== '') {
          instructorName = q.memberName
        } else if (q.memberEmail && q.memberEmail.trim() !== '') {
          // 이메일에서 이름 추출 시도
          const emailName = q.memberEmail.split('@')[0]
          instructorName = emailName || '알 수 없는 강사'
        } else if (q.memberId) {
          // UUID가 아닌 경우에만 ID 표시
          const memberId = q.memberId.toString()
          if (memberId.length < 20) {
            instructorName = `강사 (ID: ${memberId})`
          } else {
            instructorName = '알 수 없는 강사'
          }
        }
        
        let options = []
        let correctAnswer = q.questionAnswer
        if (q.questionType === '객관식' && Array.isArray(q.options) && q.options.length > 0) {
          options = q.options.map(opt => opt.optText)
          const correctOption = q.options.find(opt => opt.optIsCorrect === 1)
          if (correctOption) {
            correctAnswer = correctOption.optText
          }
        }
        const transformedQuestion = {
          id: q.questionId,
          type: q.questionType,
          question: q.questionText,
          correctAnswer: correctAnswer,
          options: options,
          subject: q.subDetailName || '미분류',
          subjectId: q.subDetailId || null,
          mainSubject: q.subjectName || null,
          instructor: instructorName,
          instructorId: q.memberId,
          instructorEmail: instructorEmail || q.memberEmail,
          createdDate: q.createdAt ? new Date(q.createdAt).toLocaleDateString() : '날짜 없음',
          explanation: q.explanation,
          status: q.status,
          optionCount: q.optionCount || 0
        }
        
        // subDetailId 디버깅 로그
        if (q.subDetailId) {
        }
        
        return transformedQuestion
      })
      setQuestions(transformedQuestions)
      setTotalQuestionsCount(transformedQuestions.length)
      
      // 강사 목록은 instructors 배열에서 가져오기
      const uniqueInstructors = instructorsArray.map(instructor => instructor.memberName).filter(Boolean)
      setInstructors(uniqueInstructors)
    } catch (err) {
      setError("문제 데이터를 불러오는 데 실패했습니다.")
      setQuestions([])
      setTotalQuestionsCount(0)
    } finally {
      setQuestionsLoading(false)
    }
  }, [isEditModalOpen, isDetailModalOpen, isDeleteModalOpen])

  // 서버 사이드 페이지네이션 (대용량 데이터용 - 주석 처리)
  /*
  // 문제 데이터 로드 (서버 사이드 페이지네이션)
  const fetchQuestionsData = useCallback(async () => {
    try {
      setQuestionsLoading(true)
      setError(null)
      
      // 서버에 필터링 조건과 페이지네이션 정보 전송
      const params = {
        keyword: debouncedSearchTerm,
        subject: selectedSubject,
        type: selectedType,
        instructor: selectedInstructor,
        page: currentPage,
        limit: questionsPerPage,
      }
      
      const questionsResponse = await getQuestions(params)
      
      const questionsArray = Array.isArray(questionsResponse) 
        ? questionsResponse 
        : (questionsResponse?.questions || questionsResponse?.data || [])
      
      setQuestions(questionsArray)
      setFilteredQuestions(questionsArray)
      setTotalQuestionsCount(questionsResponse.totalCount || questionsArray.length)
      
    } catch (err) {
      setError('문제 데이터를 불러오는 데 실패했습니다.')
      
      setQuestions([])
      setFilteredQuestions([])
      setTotalQuestionsCount(0)
    } finally {
      setQuestionsLoading(false)
    }
  }, [searchTerm, selectedSubject, selectedType, selectedInstructor, currentPage])
  */

  // 필터 옵션 및 통계 로드
  const loadFilterOptionsAndStats = useCallback(async () => {
    // 모달이 열려있으면 데이터 로딩 건너뛰기
    if (isEditModalOpen || isDetailModalOpen || isDeleteModalOpen) {
      return
    }
    
    try {
      setFiltersLoading(true)
      setStatsLoading(true)
      
      const [subjectsData, statsData] = await Promise.all([
        getAllSubject(),
        getQuestionBankStats()
      ])
      
      // 강사 목록은 문제 데이터에서 추출하므로 별도 API 호출 불필요
      setInstructors([]) // 빈 배열로 초기화
      
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
ㅌ         } else if (subject.subDetails && Array.isArray(subject.subDetails)) {
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
      
      // 통계 데이터 처리
      const processedStats = {
        totalQuestions: statsData?.data?.totalQuestions || statsData?.data?.total || statsData?.totalQuestions || statsData?.total || 0,
        objectiveCount: statsData?.data?.objectiveCount || statsData?.data?.multipleChoice || statsData?.objectiveCount || 0,
        descriptiveCount: statsData?.data?.descriptiveCount || statsData?.data?.essay || statsData?.descriptiveCount || 0,
        codeCount: statsData?.data?.codeCount || statsData?.data?.code || statsData?.codeCount || 0,
      }
      setQuestionStats(processedStats)
      
      // 데이터 소스 확인
      setDataSource('db')
      
    } catch (err) {
      setDataSource('error')
      
      setInstructors([])
      setSubjects([])
      setSubjectMapping({})
      setQuestionStats({
        totalQuestions: 0,
        objectiveCount: 0,
        descriptiveCount: 0,
        codeCount: 0,
      })
    } finally {
      setFiltersLoading(false)
      setStatsLoading(false)
    }
  }, [isEditModalOpen, isDetailModalOpen, isDeleteModalOpen])

  // 현재 로그인한 사용자 정보 가져오기
  const getCurrentUser = () => {
    try {
      // localStorage에서 사용자 정보 가져오기
      const userInfo = localStorage.getItem('currentUser');
      if (userInfo) {
        const user = JSON.parse(userInfo);
        return user;
      } else {
        return null;
      }
    } catch (error) {
      return null;
    }
  };
  
  // 현재 로그인한 사용자 정보 초기화
  useEffect(() => {
    const user = getCurrentUser()
    
    if (user) {
      setCurrentUser(user.name)
      setCurrentUserId(user.memberId)
    } else {
      setCurrentUser(null)
      setCurrentUserId(null)
    }
  }, [])

  // 필터링된 문제 목록 계산
  const filteredQuestionsComputed = useMemo(() => {
    // questions가 배열인지 확인하고, 배열이 아니면 빈 배열 반환
    if (!Array.isArray(questions)) {
      return []
    }
    
    const filtered = questions.filter((question) => {
      const matchesSearch = !searchTerm || 
        question.question.toLowerCase().includes(searchTerm.toLowerCase()) ||
        question.subject.toLowerCase().includes(searchTerm.toLowerCase()) ||
        question.instructor.toLowerCase().includes(searchTerm.toLowerCase())
      
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
      const matchesInstructor = !selectedInstructor || question.instructor === selectedInstructor
      
      return matchesSearch && matchesSubject && matchesType && matchesInstructor
    })
    
    // 필터링 결과 요약 로그
    if (selectedSubject || selectedType || selectedInstructor || searchTerm) {
     
    }
    
    return filtered
  }, [questions, searchTerm, selectedSubject, selectedType, selectedInstructor, subjectMapping])

  // 필터링된 결과를 상태에 저장
  useEffect(() => {
    setFilteredQuestions(filteredQuestionsComputed)
  }, [filteredQuestionsComputed])

  // 컴포넌트 마운트 시 데이터 로드
  useEffect(() => {
    fetchQuestionsData()
    loadFilterOptionsAndStats()
  }, [fetchQuestionsData, loadFilterOptionsAndStats])

  const getTypeIcon = useMemo(() => (type) => {
    switch (type) {
      case '객관식': return <BookOpen size={16} className="text-blue-500" />
      case '서술형': return <FileText size={16} className="text-purple-500" />
      // case '코드형': return <Code size={16} className="text-orange-500" />
      default: return null
    }
  }, [])

  // 코드 언어 감지 함수
  // const detectCodeLanguage = (questionText, code) => {
  //   const text = (questionText + ' ' + code).toLowerCase()
    
  //   if (text.includes('python') || text.includes('print(') || text.includes('def ') || text.includes('import ')) {
  //     return 'python'
  //   }
  //   if (text.includes('java') || text.includes('public class') || text.includes('system.out') || text.includes('string[]')) {
  //     return 'java'
  //   }
  //   if (text.includes('javascript') || text.includes('js') || text.includes('console.log') || text.includes('function(')) {
  //     return 'javascript'
  //   }
  //   if (text.includes('c++') || text.includes('cout') || text.includes('#include')) {
  //     return 'cpp'
  //   }
  //   if (text.includes('c#') || text.includes('console.writeline')) {
  //     return 'csharp'
  //   }
  //   if (text.includes('html') || text.includes('<html') || text.includes('<div')) {
  //     return 'html'
  //   }
  //   if (text.includes('css') || text.includes('{') && text.includes(':')) {
  //     return 'css'
  //   }
  //   if (text.includes('sql') || text.includes('select') || text.includes('from') || text.includes('where')) {
  //     return 'sql'
  //   }
    
  //   return 'javascript' // 기본값
  // }

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

  // 권한 확인 함수 (사용자 이름 기반)
  const checkPermission = useCallback((questionInstructorId, questionInstructorName) => {
    // currentUser가 null이면 권한 없음
    if (currentUser === null) {
      return false
    }
    
    // 사용자 이름으로 권한 확인 (주요 방법)
    const hasPermissionByName = questionInstructorName === currentUser
    
    // memberId로도 권한 확인 (백업 방법)
    const hasPermissionById = currentUserId && questionInstructorId === currentUserId
    
    const hasPermission = hasPermissionByName || hasPermissionById
    
    // 모달이 열려있을 때는 로그 출력하지 않음
    if (!isEditModalOpen && !isDetailModalOpen && !isDeleteModalOpen) {
     
    }
    
    return hasPermission
  }, [currentUser, currentUserId, isEditModalOpen, isDetailModalOpen, isDeleteModalOpen])

  // 로딩 상태 통합
  const isLoading = questionsLoading || filtersLoading || statsLoading

  // 문제 수정 모달 열기
  const handleEditQuestion = (question) => {
    // 코드형 문제인 경우 코드 언어 초기화
    let questionToEdit = { ...question }
    
    // if (question.type === "코드형" && !question.codeLanguage) {
    //   const detectedLanguage = detectCodeLanguage(question.question || "", question.correctAnswer || "")
    //   questionToEdit.codeLanguage = detectedLanguage
    // }
    
    setEditingQuestion(questionToEdit)
    setIsEditModalOpen(true)
  }

  // 문제 상세보기 모달 열기
  const handleViewDetail = (question) => {
    setSelectedQuestion({ ...question })
    setIsDetailModalOpen(true)
  }

  // 문제 수정 저장
  const handleSaveEdit = async () => {
    
    if (!editingQuestion?.question?.trim()) {
      alert("문제 내용을 입력해주세요.")
      return
    }

    // 객관식 문제에서 중복 선택지 확인
    if (editingQuestion.type === "객관식" && editingQuestion.options) {
      const uniqueOptions = new Set(editingQuestion.options.filter(option => option.trim() !== ''))
      if (uniqueOptions.size !== editingQuestion.options.filter(option => option.trim() !== '').length) {
        alert("중복된 선택지가 있습니다. 모든 선택지는 서로 달라야 합니다.")
        return
      }
      
      // 빈 선택지 확인
      const emptyOptions = editingQuestion.options.filter(option => !option.trim())
      if (emptyOptions.length > 0) {
        alert("빈 선택지가 있습니다. 모든 선택지를 입력해주세요.")
        return
      }
    }

    // 정답이 선택되었는지 확인 (객관식)
    if (editingQuestion.type === "객관식" && (!editingQuestion.correctAnswer || !editingQuestion.options?.includes(editingQuestion.correctAnswer))) {
      alert("정답을 선택해주세요.")
      return
    }

    //서술형/코드형에서 정답 입력 확인 - 정답이 비어있어도 허용
    if ((editingQuestion.type === "서술형" || editingQuestion.type === "코드형") && !editingQuestion.correctAnswer?.trim()) {
      alert("정답을 입력해주세요.")
      return
    }

    try {
      
      // 코드형 문제인 경우 언어 타입 추가
      let questionDataToSend = { ...editingQuestion }
      
      // if (editingQuestion.type === "코드형") {
      //   const detectedLanguage = detectCodeLanguage(editingQuestion.question, editingQuestion.correctAnswer)
      //   questionDataToSend.codeLanguage = detectedLanguage
      // }
      
      await updateQuestion(editingQuestion.id, questionDataToSend)
      
      // 성공 시 데이터 재로드
      await fetchQuestionsData()
      setIsEditModalOpen(false)
      setEditingQuestion(null)
    } catch (err) {
      
      alert('문제 수정에 실패했습니다.')
    }
  }

  // 수정 모달 입력 필드 변경
  const handleEditChange = (field, value) => {
    setEditingQuestion((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  // 객관식 옵션 변경
  const handleOptionChange = (index, value) => {
    const newOptions = [...editingQuestion.options]
    newOptions[index] = value
    setEditingQuestion((prev) => ({
      ...prev,
      options: newOptions,
    }))
  }

  // 비활성화 확인 모달 열기
  const handleDeactivateClick = (questionId) => {
    setDeletingQuestionId(questionId)
    setIsDeleteModalOpen(true)
  }

  // 문제 비활성화 실행 (확인 모달에서 호출)
  const handleDeactivateConfirm = async () => {
    
    if (!deletingQuestionId) {
      return
    }

    try {
      const result = await deactivateQuestion(deletingQuestionId)
      
      // 성공 시 데이터 재로드
      await fetchQuestionsData()
      
      // 모달들 닫기
      setIsDeleteModalOpen(false)
      setDeletingQuestionId(null)
      if (isDetailModalOpen) {
        setIsDetailModalOpen(false)
        setSelectedQuestion(null)
      }
      
    } catch (err) {
      
    }
  }

  // 문제 비활성화 (직접 호출 - 기존 방식 유지)
  const handleDeactivateQuestion = async (questionId) => {
    if (!window.confirm("정말로 이 문제를 비활성화하시겠습니까?")) {
      return
    }

    try {
      await deactivateQuestion(questionId)
      
      // 성공 시 데이터 재로드
      await fetchQuestionsData()
      
      // 상세보기 모달이 열려있다면 닫기
      if (isDetailModalOpen) {
        setIsDetailModalOpen(false)
        setSelectedQuestion(null)
      }
      
    } catch (err) {
    }
  }

  // 데이터 새로고침
  const handleRefresh = () => {
    setError(null)
    fetchQuestionsData()
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
    } else if (name === 'instructor') {
      setSelectedInstructor(value)
    }
    setCurrentPage(1) // 필터 변경 시 첫 페이지로 이동
  }

  // 페이지 변경 핸들러
  const handlePageChange = (page) => {
    setCurrentPage(page)
  }

  // 문제 유형별 색상 반환 함수
  const getTypeColor = (type) => {
    switch (type) {
      case '객관식':
        return 'bg-[#e4f5eb] text-[#1abc9c]'
      case '서술형':
        return 'bg-blue-50 text-[#b4c8fa]'
      case '코드형':
        return 'bg-blue-50 text-[#3f5c77]'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  if (isLoading) {
    return (
      <React.Fragment>
        <PageLayout currentPage="question-bank">
          <div className="flex">
            <Sidebar title="문제 은행" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="animate-pulse space-y-6 ">
                {/* 헤더 스켈레톤 */}
                <div className="space-y-2">
                  <div className="h-8 bg-gray-200 rounded w-1/4"></div>
                  <div className="h-4 bg-gray-200 rounded w-1/2"></div>
                </div>
                
                {/* 통계 카드 스켈레톤 */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  {[...Array(4)].map((_, i) => (
                    <div key={i} className="bg-white p-4 rounded-lg shadow-sm">
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
                <div className="bg-white p-4 rounded-lg">
                  <div className="flex items-center gap-4">
                    <div className="flex-1 h-10 bg-gray-200 rounded"></div>
                    <div className="h-10 bg-gray-200 rounded w-24"></div>
                    <div className="h-10 bg-gray-200 rounded w-32"></div>
                    <div className="h-10 bg-gray-200 rounded w-32"></div>
                  </div>
                </div>
                
                {/* 문제 목록 스켈레톤 */}
                <div className="space-y-4">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="bg-white p-6 rounded-lg shadow-sm">
                      <div className="space-y-3">
                        <div className="flex items-center gap-3">
                          <div className="h-6 bg-gray-200 rounded w-16"></div>
                          <div className="h-4 bg-gray-200 rounded w-24"></div>
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
      <PageLayout currentPage="question-bank" userRole="instructor">
        <div className="flex">
          <Sidebar title="문제 은행 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
          <main className="flex-1 p-6">
            <div className="space-y-6 max-w-7xl mx-auto">
              {/* 페이지 헤더 */}
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">전체 문제 은행</h1>
                  <p className="text-gray-600 mt-1">모든 강사들이 출제한 문제들을 확인하고 참고할 수 있습니다.</p>
                </div>
                <button
                  onClick={handleRefresh}
                  disabled={questionsLoading}
                  className="flex items-center gap-2 px-4 py-2 
                  bg-[#1abc9c] hover:bg-[rgb(10,150,120)] 
                  disabled:bg-gray-400 text-white 
                  rounded-lg transition-colors"
                >
                  <RefreshCw className={`w-4 h-4 ${questionsLoading ? 'animate-spin' : ''}`} />
                  새로고침
                </button>
              </div>

              {/* 통계 카드 */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="ml-3">
                        <p className="text-m text-gray-600 mb-1">전체 문제</p>
                        <p className="text-3xl font-bold text-[#3498db]">{questionStats.totalQuestions || 0}개</p>
                        
                      </div>
                      <div className="p-3 bg-[#EFF6FF] rounded-full mr-3">
                        <Layers className="w-10 h-10 text-[#3498db]" />
                      </div>
                    </div>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="ml-3">
                        <p className="text-m text-gray-600 mb-1">객관식 문제</p>
                        <p className="text-3xl font-bold text-[#1abc9c]">{questionStats.objectiveCount}개</p>
                      </div>
                      <div className="p-3 bg-[#e4f5eb] rounded-full mr-3">
                        <CheckCircle className="w-10 h-10 text-[#1abc9c]" />
                      </div>
                    </div>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="ml-3">
                        <p className="text-m text-gray-600 mb-1">서술형 문제</p>
                        <p className="text-3xl font-bold text-[#b0c4de]">{questionStats.descriptiveCount}개</p>
                      </div>
                      <div className="p-3 bg-[#EFF6FF] rounded-full mr-3">
                        <Edit className="w-10 h-10 text-[#b0c4de]" />
                      </div>
                    </div>
                  </CardContent>
                </Card>
                {/* <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-m text-gray-600 mb-1">코드형 문제</p>
                        <p className="text-3xl font-bold text-[#3f5c77]">{questionStats.codeCount}개</p>
                      </div>
                      <div className="p-3 bg-[#EFF6FF] rounded-full  ">
                        <Code2 className="w-10 h-10 text-[#3f5c77]" />
                      </div>
                    </div>
                  </CardContent>
                </Card> */}
              </div>

              {/* 검색 및 필터 */}
              <div className="bg-white p-4 rounded-lg ">
                <div className="flex items-center gap-2">
                  <div className="flex-1 relative w-1/2">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                    <input
                      type="text"
                      placeholder="문제 내용, 과목명, 강사명으로 검색..."
                      value={searchTerm}
                      onChange={handleSearchChange}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    <Filter className="w-4 h-4 text-gray-500" />
                    <span className="text-sm text-gray-600">필터:</span>
                    <select
                      name="instructor"
                      value={selectedInstructor}
                      onChange={handleFilterChange}
                      className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 w-32"
                      disabled={filtersLoading}
                    >
                    <option value="">
                      {filtersLoading ? "로딩..." : "전체 강사"}
                    </option>
                    {instructors.map((instructor) => (
                      <option key={instructor} value={instructor}>
                        {instructor}
                      </option>
                    ))}
                  </select>

                  <select
                    name="subject"
                    value={selectedSubject}
                    onChange={handleFilterChange}
                    className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 w-36"
                    disabled={filtersLoading}
                  >
                    <option value="">
                      {filtersLoading ? "로딩..." : "전체 과목"}
                    </option>
                    {subjects.length > 0 && subjects.map((subject) => (
                      <option key={subject} value={subject}>
                        {subject}
                      </option>
                    ))}
                  </select>

                                  <select
                    name="type"
                    value={selectedType}
                    onChange={handleFilterChange}
                    className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 w-32"
                  >
                    <option value="">전체 유형</option>
                    <option value="객관식">객관식</option>
                    <option value="서술형">서술형</option>
                    {/* <option value="코드형">코드형</option> */}
                  </select>
                    </div>

                </div>
              </div>


              
              {dataSource === 'error' && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <div className="flex items-center gap-2">
                    <AlertCircle className="w-5 h-5 text-red-600" />
                    <div>
                      <p className="text-red-800 font-medium">서버 연결 오류</p>
                      <p className="text-red-700 text-sm">백엔드 서버에 연결할 수 없습니다. 서버 상태를 확인하세요.</p>
                    </div>
                  </div>
                </div>
              )}

              {/* 에러 메시지 */}
              {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <p className="text-red-800">데이터 로드 중 오류가 발생했습니다: {error}</p>
                </div>
              )}

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
                            <span className="text-sm text-gray-600">{question.subject}</span>
                            <div className="flex items-center gap-1 text-sm text-blue-600 bg-blue-50 px-2 py-1 rounded">
                              <Users className="w-3 h-3" />
                              {question.instructor}
                            </div>
                            <div className="flex items-center gap-2 flex-shrink-0 ml-auto">
                          
                                <button 
                                  onClick={() => handleViewDetail(question)}
                                  className="p-2 text-[#1abc9c] hover:bg-green-50 hover:scale-110 rounded-lg transition-colors"
                                  title="상세보기"
                                >
                                  <Eye className="w-4 h-4" />
                                </button>
                            {checkPermission(question.instructorId, question.instructor) && (
                              <>
                              <button
                                onClick={() => handleEditQuestion(question)}
                                className="p-2 text-[#b0c4de] hover:bg-blue-50 hover:scale-110 rounded-lg transition-colors"
                                title="수정하기"
                                >
                                <Edit className="w-4 h-4" />
                              </button>
                              <button
                                onClick={() => handleDeactivateQuestion(question.id)}
                                className="p-2 text-[#e74c3c] hover:bg-red-50 hover:scale-110 rounded-lg transition-colors"
                                title="비활성화하기"
                                >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </>
                          )}
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
                          {question.type === "코드형" && question.correctAnswer && (
                            <div className="mb-3">
                              <p className="text-sm text-gray-600 mb-1">정답:</p>
                              <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                                <pre className="text-sm text-green-700 whitespace-pre-wrap font-mono">{question.correctAnswer}</pre>
                              </div>
                            </div>
                          )}

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

      {/* 비활성화 확인 모달 */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex items-center gap-3 mb-4">
              <AlertCircle className="w-6 h-6 text-red-500" />
              <h2 className="text-xl font-bold text-gray-900">문제 비활성화</h2>
            </div>
            <p className="text-gray-600 mb-6">
              정말로 이 문제를 비활성화하시겠습니까? 비활성화된 문제는 더 이상 사용할 수 없습니다.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setIsDeleteModalOpen(false)}
                className="flex-1 px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              >
                취소
              </button>
              <button
                onClick={handleDeactivateConfirm}
                className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors"
              >
                비활성화
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 문제 수정 모달 */}
      {isEditModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold">문제 수정</h2>
              <button onClick={() => setIsEditModalOpen(false)} className="text-gray-500 hover:text-gray-700">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">문제 유형</label>
                  <select
                    value={editingQuestion?.type || ""}
                    disabled
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-600 cursor-not-allowed"
                    title="문제 유형은 수정할 수 없습니다"
                  >
                    <option value="객관식">객관식</option>
                    <option value="서술형">서술형</option>
                    <option value="코드형">코드형</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">과목</label>
                  <input
                    type="text"
                    value={editingQuestion?.subject || ""}
                    disabled
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-600 cursor-not-allowed"
                    title="과목은 수정할 수 없습니다"
                  />
                </div>
              </div>


              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">문제 내용</label>
                <textarea
                  value={editingQuestion?.question || ""}
                  onChange={(e) => handleEditChange("question", e.target.value)}
                  rows={4}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="문제 내용을 입력하세요..."
                />
              </div>

              {editingQuestion?.type === "객관식" && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">선택지</label>
                    <div className="space-y-2">
                      {editingQuestion.options?.map((option, index) => (
                        <div key={index} className="flex items-center gap-2">
                          <input
                            type="radio"
                            name="correctAnswer"
                            checked={editingQuestion.correctAnswer === option}
                            onChange={() => handleEditChange("correctAnswer", option)}
                            className="text-blue-600"
                          />
                          <span className="text-sm font-medium">{index + 1}.</span>
                          <input
                            type="text"
                            value={option}
                            onChange={(e) => handleOptionChange(index, e.target.value)}
                            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder={`선택지 ${index + 1}`}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">해설</label>
                    <textarea
                      value={editingQuestion?.explanation || ""}
                      onChange={(e) => handleEditChange("explanation", e.target.value)}
                      rows={4}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="문제 해설을 입력하세요..."
                    />
                  </div>
                </div>
              )}

              {/* 서술형 정답 입력 */}
              {editingQuestion?.type === "서술형" && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">정답</label>
                  <textarea
                    value={editingQuestion?.correctAnswer || ""}
                    onChange={(e) => handleEditChange("correctAnswer", e.target.value)}
                    rows={4}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="정답을 입력하세요..."
                  />
                </div>
              )}

              {/* 코드형 정답 입력 */}
              {editingQuestion?.type === "코드형" && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">코드 언어</label>
                    <select
                      value={editingQuestion?.codeLanguage || ""}
                      onChange={(e) => handleEditChange("codeLanguage", e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="javascript">JavaScript</option>
                      <option value="python">Python</option>
                      <option value="java">Java</option>
                      <option value="cpp">C++</option>
                      <option value="csharp">C#</option>
                      <option value="html">HTML</option>
                      <option value="css">CSS</option>
                      <option value="sql">SQL</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">정답</label>
                    <div className="border border-gray-200 rounded-lg overflow-hidden">
                      <Editor
                        height="200px"
                        defaultLanguage={editingQuestion?.codeLanguage || "javascript"}
                        value={editingQuestion?.correctAnswer || ""}
                        onChange={(value) => handleEditChange("correctAnswer", value)}
                        options={{
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
                </div>
              )}
            </div>

            <div className="flex gap-2 mt-6">
              <button
                onClick={() => setIsEditModalOpen(false)}
                className="flex-1 px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              >
                취소
              </button>
              <button
                onClick={handleSaveEdit}
                className="flex-1 px-4 py-2 bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white rounded-lg transition-colors"
              >
                수정 완료
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 문제 상세보기 모달 */}
      {isDetailModalOpen && selectedQuestion && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-bold text-gray-900">문제 상세보기</h2>
                <button 
                  onClick={() => {
                    setIsDetailModalOpen(false)
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
                  <div className="grid grid-cols-3 gap-4 text-sm">
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
                    <div>
                      <p className="text-gray-500 mb-1">출제자</p>
                      <p className="font-medium">{selectedQuestion.instructor}</p>
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

                {/* 객관식 선택지와 해설 - 객관식일 때만 표시 */}
                {selectedQuestion.type === "객관식" && selectedQuestion.options && (
                  <div className="space-y-4">
                    {/* 선택지 */}
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
                    <div className="bg-gray-50 p-4 rounded-lg">
                      <div className="flex items-center gap-2 mb-2">
                        <Calendar className="w-4 h-4 text-[#1abc9c]" />
                        <span className="text-sm text-gray-600">생성일</span>
                      </div>
                      <p className="text-lg font-semibold ">{selectedQuestion.createdDate}</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* 버튼 */}
              <div className="flex justify-end gap-3 mt-8 pt-6 border-t">
                <button
                  onClick={() => {
                    setIsDetailModalOpen(false)
                    setSelectedQuestion(null)
                  }}
                  className="px-4 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  닫기
                </button>
                {checkPermission(selectedQuestion.instructorId, selectedQuestion.instructor) && (
                  <>
                    <button
                      onClick={() => {
                        setIsDetailModalOpen(false)
                        setSelectedQuestion(null)
                        handleEditQuestion(selectedQuestion)
                      }}
                      className="px-4 py-2 bg-[#1abc9c] hover:bg-[rgb(10,150,120)]
                      text-white rounded-lg transition-colors flex items-center gap-2"
                    >
                      <Edit className="w-4 h-4" />
                      수정하기
                    </button>
                    <button
                      onClick={() => handleDeactivateQuestion(selectedQuestion.id)}
                      className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors flex items-center gap-2"
                    >
                      <Trash2 className="w-4 h-4" />
                      문제 삭제
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </React.Fragment>
  )
}
