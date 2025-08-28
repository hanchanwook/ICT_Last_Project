import { useState, useEffect } from "react"
import { Search, Plus, Trash2, Eye, FileText, Filter, X, MessageSquare, Star, FileStack, ListChecks } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { evaluationMenuItems } from "@/components/ui/menuConfig"
import { createEvaluationTemplate, getEvaluationTemplateList, getAllEvaluation, updateEvaluationTemplate, deleteEvaluationTemplate } from "@/api/suhyeon/evaluationApi"
import { Card, CardContent } from "@/components/ui/card"

export default function questionTemplate() {
  const [searchTerm, setSearchTerm] = useState("")
  const [showTemplateModal, setShowTemplateModal] = useState(false)
  const [templateData, setTemplateData] = useState({
    questionTemplateName: "",
    description: "",
    category: "강의 평가"
  })
  const [templateSelectedQuestions, setTemplateSelectedQuestions] = useState([])
  const [templateSearchTerm, setTemplateSearchTerm] = useState("")
  const [templateSelectedType, setTemplateSelectedType] = useState("전체")
  const [isLoading, setIsLoading] = useState(false)
  const [templates, setTemplates] = useState([])
  const [templatesLoading, setTemplatesLoading] = useState(true)
  const [showDetailModal, setShowDetailModal] = useState(false)
  const [detailTemplate, setDetailTemplate] = useState(null)
  const [isEditMode, setIsEditMode] = useState(false)
  const [questions, setQuestions] = useState([])

  // 템플릿 목록 불러오기
  const fetchTemplates = async () => {
    try {
      setTemplatesLoading(true)
      const response = await getEvaluationTemplateList()
      setTemplates(response)
    } catch (error) {
      console.error("템플릿 목록 조회 실패:", error)
      setTemplates([])
    } finally {
      setTemplatesLoading(false)
    }
  }

  // 질문 데이터 불러오기
  const fetchQuestions = async () => {
    try {
      const response = await getAllEvaluation()
      setQuestions(response)
    } catch (error) {
      console.error("질문 목록 조회 실패:", error)
      setQuestions([])
    }
  }

  useEffect(() => {
    fetchTemplates()
    fetchQuestions()
  }, [])

  // 타입별 라벨 함수
  const getTypeLabel = (type) => {
    switch (type) {
      case 0:
        return "5점 척도"
      case 1:
        return "주관식"
      default:
        return "기타"
    }
  }

  // 타입별 색상 함수
  const getTypeColor = (type) => {
    switch (type) {
      case 0:
        return "bg-green-100 text-green-800"
      case 1:
        return "bg-blue-100 text-blue-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  // 필터링된 질문들
  const templateFilteredItems = questions.filter((item) => {
    const matchesSearch = 
      (item.question || item.evalQuestionText || "").toLowerCase().includes(templateSearchTerm.toLowerCase())
    const matchesType = templateSelectedType === "전체" || getTypeLabel(item.evalQuestionType) === templateSelectedType
    return matchesSearch && matchesType
  })

  const types = ["전체", "5점 척도", "주관식"]

  // 필터링된 템플릿
  const filteredTemplates = templates
    ?.filter((template) => {
      const matchesSearch =
        template.questionTemplateName.toLowerCase().includes(searchTerm.toLowerCase())
      return matchesSearch
    })
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)) || []

  const handleView = (templateNum) => {
    const template = templates.find(t => t.questionTemplateNum === templateNum)
    
    if (template) {
      setDetailTemplate(template)
      setShowDetailModal(true)
    }
  }

  const handleDelete = async (templateNum) => {
    if (confirm("정말로 이 템플릿을 삭제하시겠습니까?")) {
      try {
        await deleteEvaluationTemplate(templateNum)
        alert("템플릿이 성공적으로 삭제되었습니다.")
        fetchTemplates()
      } catch (error) {
        console.error("템플릿 삭제 실패:", error)
        alert("템플릿 삭제에 실패했습니다. 다시 시도해주세요.")
      }
    }
  }

  // 모달 닫기 함수
  const handleTemplateModalClose = () => {
    setShowTemplateModal(false)
    setTemplateData({ questionTemplateName: "", description: "", category: "강의 평가" })
    setTemplateSelectedQuestions([])
    setTemplateSearchTerm("")
    setTemplateSelectedType("전체")
    setIsLoading(false)
    setIsEditMode(false)
  }

  // 템플릿 생성/수정 함수
  const handleCreateTemplate = async () => {
    if (!templateData.questionTemplateName || templateSelectedQuestions.length === 0) {
      alert("템플릿 이름과 최소 1개 이상의 질문을 선택해주세요.")
      return
    }

    // 중복 이름 검사 (수정 모드가 아닐 때만)
    if (!isEditMode) {
      const trimmedName = templateData.questionTemplateName.trim()
      const isDuplicate = templates.some(template => {
        const existingName = (template.questionTemplateName || "").trim()
        return existingName.toLowerCase() === trimmedName.toLowerCase()
      })
      
      if (isDuplicate) {
        alert("이미 존재하는 템플릿 이름입니다. 다른 이름을 입력해주세요.")
        return
      }
    }

    setIsLoading(true)
    try {
      const requestData = {
        questionTemplateName: templateData.questionTemplateName.trim(),
        questionList: templateSelectedQuestions.map((questionId, index) => ({
          questionNum: index + 1,
          evalQuestionId: questionId
        }))
      }

      let response
      if (isEditMode) {
        const updateData = {
          ...requestData,
          templateId: detailTemplate.templateId,
          questionTemplateNum: detailTemplate.questionTemplateNum
        }
        response = await updateEvaluationTemplate(updateData)
        alert("템플릿이 성공적으로 수정되었습니다.")
      } else {
        response = await createEvaluationTemplate(requestData)
        alert("템플릿이 성공적으로 생성되었습니다.")
      }
      
      handleTemplateModalClose()
      fetchTemplates()
      
    } catch (error) {
      console.error("템플릿 생성/수정 실패:", error)
      alert(`템플릿 ${isEditMode ? '수정' : '생성'}에 실패했습니다. 다시 시도해주세요.`)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <PageLayout currentPage="evaluations">
      <div className="flex min-h-screen bg-gray-50 justify-center">
        <Sidebar title="설문 평가 관리" menuItems={evaluationMenuItems} currentPath="/evaluations/templates" />
        <div className="flex-1 p-6 max-w-[1200px] mx-auto">
          {/* 페이지 헤더 */}
          <div className="mb-6">
            <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
              템플릿 목록
            </h1>
            <p className="text-gray-600">과정 평가를 위한 템플릿을 관리합니다.</p>
          </div>
          <div className="space-y-6">
            {/* 통계 카드 */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
               <Card className="h-40">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600 mb-2">전체 템플릿</p>
                      <p className="text-3xl font-bold" style={{ color: "#3498db" }}>
                        {templatesLoading ? "..." : (templates?.length || 0)}개
                      </p>
                    </div>
                    <div
                      className="rounded-full flex items-center justify-center mr-4 p-3"
                      style={{ backgroundColor: "#EFF6FF" }}
                    >
                      <FileStack className="w-10 h-10 text-[#3498db]" />
                    </div>
                  </div>
                </CardContent>
              </Card>

                             <Card className="h-40">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600 mb-2">평가항목</p>
                      <p className="text-3xl font-bold" style={{ color: "#1abc9c" }}>
                        {questions?.length || 0}개
                      </p>
                    </div>
                    <div
                      className="rounded-full flex items-center justify-center mr-4 p-3"
                      style={{ backgroundColor: "#e4f5eb" }}
                    >
                      <ListChecks className="w-10 h-10" style={{ color: "#1abc9c" }} />
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* 검색 및 필터 */}
            <div className="bg-white p-6 rounded-lg shadow-sm border">
              <div className="flex flex-col md:flex-row gap-4 items-center justify-between">
                <div className="flex flex-1 gap-4">
                  <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                    <input
                      type="text"
                      placeholder="템플릿명으로 검색..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                    />
                  </div>
                </div>
                <Button
                  onClick={() => setShowTemplateModal(true)}
                  className="text-[#1abc9c] bg-white border border-[#1abc9c]
                  hover:bg-[#1abc9c] hover:text-white"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  템플릿 만들기
                </Button>
              </div>
            </div>

            {/* 템플릿 목록 */}
            <div className="bg-white rounded-lg shadow-sm border overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead style={{ backgroundColor: "#f8f9fa" }}>
                    <tr>
                      <th className="py-3 px-4 text-left text-sm font-medium text-gray-700">템플릿명</th>
                      <th className="py-3 px-4 text-left text-sm font-medium text-gray-700">질문 수</th>
                      <th className="py-3 px-4 text-left text-sm font-medium text-gray-700">등록일</th>
                      <th className="py-3 px-4 text-center text-sm font-medium text-gray-700">관리</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {templatesLoading ? (
                      <tr>
                        <td colSpan="4" className="py-8 text-center text-gray-500">
                          템플릿 목록을 불러오는 중...
                        </td>
                      </tr>
                    ) : filteredTemplates.length === 0 ? (
                      <tr>
                        <td colSpan="4" className="py-8 text-center text-gray-500">
                          템플릿이 없습니다.
                        </td>
                      </tr>
                    ) : (
                      filteredTemplates.map((template) => (
                        <tr key={template.templateId || template.questionTemplateNum || template.id || `template-${template.questionTemplateName}`} >
                          <td className="py-3 px-4">
                            <div>
                              <div className="font-medium text-gray-900">{template.questionTemplateName}</div>
                            </div>
                          </td>
                          <td className="py-3 px-4 text-sm text-gray-900">{template.questionList?.length || 0}개</td>
                          <td className="py-3 px-4 text-sm text-gray-900">
                            {template.createdAt ? new Date(template.createdAt).toLocaleDateString('ko-KR') : '-'}
                          </td>
                          <td className="py-3 px-4">
                            <div className="flex justify-center space-x-2">
                                                             <Button size="sm" variant="ghost" 
                               onClick={() => handleView(template.questionTemplateNum)} 
                               className="p-1 hover:bg-blue-50 
                               hover:scale-110 transition-all duration-200">
                                <Eye className="w-4 h-4" 
                                style={{ color: "#1abc9c" }} />
                              </Button>
                              <Button size="sm" variant="ghost" 
                              onClick={() => handleDelete(template.questionTemplateNum)} 
                              className="p-1 hover:bg-red-50 
                              hover:scale-110 transition-all duration-200">
                                <Trash2 className="w-4 h-4" style={{ color: "#e74c3c" }} />
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* 템플릿 생성 모달 */}
            {showTemplateModal && (
              <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
                <div className="bg-white rounded-lg p-6 w-full max-w-4xl mx-4 max-h-[90vh] overflow-y-auto">
                  <div className="flex justify-between items-center mb-6">
                    <h2 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                      {isEditMode ? "템플릿 수정" : "설문 템플릿 만들기"}
                    </h2>
                    <Button variant="ghost" onClick={handleTemplateModalClose} className="p-1">
                      <X className="w-5 h-5" />
                    </Button>
                  </div>

                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    {/* 템플릿 정보 */}
                    <div className="space-y-4">
                      <h3 className="text-lg font-semibold text-gray-800">템플릿 정보</h3>

                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          템플릿 이름 <span className="text-red-500">*</span> (최대 30자)
                        </label>
                        <Input
                          value={templateData.questionTemplateName}
                          onChange={(e) => {
                            if (e.target.value.length <= 30) {
                              setTemplateData({ ...templateData, questionTemplateName: e.target.value })
                            }
                          }}
                          placeholder="템플릿 이름을 입력해주세요"
                          maxLength={30}
                          required
                        />
                        <div className="text-right mt-1">
                          <span className="text-xs" style={{ color: "#95A5A6" }}>
                            {templateData.questionTemplateName.length}/30
                          </span>
                        </div>
                      </div>
                      {/* 선택된 질문 리스트 */}
                      <div className="bg-green-50 p-4 rounded-lg">
                        <h4 className="font-medium text-green-900 mb-2">선택된 질문</h4>
                        <ol className="list-decimal pl-5 space-y-1">
                          {templateSelectedQuestions
                            .map(id => questions.find(item => item.evalQuestionId === id))
                            .filter(Boolean)
                            .map((item, idx) => (
                              <li key={item.evalQuestionId || `question-${idx}`} className="text-green-800 text-sm">
                                {item.question || item.evalQuestionText}
                              </li>
                            ))}
                        </ol>
                        <p className="text-sm text-green-800">총 {templateSelectedQuestions.length}개의 질문이 선택되었습니다.</p>
                        {templateSelectedQuestions.length === 0 && (
                          <p className="text-sm text-red-600 mt-2">최소 1개 이상의 질문을 선택해주세요.</p>
                        )}
                      </div>
                    </div>

                    {/* 질문 선택 */}
                    <div className="space-y-4">
                      <div className="flex justify-between items-center">
                        <h3 className="text-lg font-semibold text-gray-800">질문 선택</h3>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            if (templateSelectedQuestions.length === templateFilteredItems.length) {
                              setTemplateSelectedQuestions([]);
                            } else {
                              setTemplateSelectedQuestions(templateFilteredItems.map((item) => item.evalQuestionId));
                            }
                          }}
                          className="text-sm bg-transparent hover:bg-gray-200"
                        >
                          {templateSelectedQuestions.length === templateFilteredItems.length ? "전체 해제" : "전체 선택"}
                        </Button>
                      </div>

                      {/* 검색 및 필터 */}
                      <div className="space-y-3">
                        <div className="relative">
                          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                          <Input
                            placeholder="질문 내용으로 검색..."
                            value={templateSearchTerm}
                            onChange={(e) => setTemplateSearchTerm(e.target.value)}
                            className="pl-10"
                          />
                        </div>
                        <div className="flex gap-2">
                          <select
                            value={templateSelectedType}
                            onChange={(e) => setTemplateSelectedType(e.target.value)}
                            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                          >
                            {types.map((type) => (
                              <option key={type} value={type}>
                                {type}
                              </option>
                            ))}
                          </select>
                        </div>
                      </div>

                      <div className="border rounded-lg max-h-96 overflow-y-auto">
                        {templateFilteredItems.map((item, index) => (
                          <div
                            key={item.evalQuestionId || `item-${index}`}
                            className={`p-3 border-b last:border-b-0 hover:bg-gray-50 ${
                              templateSelectedQuestions.includes(item.evalQuestionId) ? "bg-green-50" : ""
                            }`}
                          >
                            <label className="flex items-start gap-3 cursor-pointer">
                              <input
                                type="checkbox"
                                checked={templateSelectedQuestions.includes(item.evalQuestionId)}
                                onChange={(e) => {
                                  if (e.target.checked) {
                                    setTemplateSelectedQuestions([...templateSelectedQuestions, item.evalQuestionId])
                                  } else {
                                    setTemplateSelectedQuestions(templateSelectedQuestions.filter((id) => id !== item.evalQuestionId))
                                  }
                                }}
                                className="w-4 h-4 text-green-600 focus:ring-green-500 border-gray-300 rounded mt-1"
                              />
                              <div className="flex-1">
                                <p className="text-sm font-medium text-gray-900">{item.question || item.evalQuestionText}</p>
                                <div className="flex gap-2 mt-1">
                                  <Badge className={`text-xs ${getTypeColor(item.type || item.evalQuestionType)}`}>
                                    {getTypeLabel(item.type || item.evalQuestionType)}
                                  </Badge>
                                </div>
                              </div>
                            </label>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* 모달 하단 버튼 */}
                  <div className="flex justify-end gap-3 mt-6 pt-6 border-t">
                    <Button variant="outline" onClick={handleTemplateModalClose} 
                    disabled={isLoading} className="hover:bg-gray-200">
                      취소
                    </Button>
                    <Button 
                      className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                      onClick={handleCreateTemplate}
                      disabled={!templateData.questionTemplateName || templateSelectedQuestions.length === 0 || isLoading}
                    >
                      {isLoading ? (isEditMode ? "수정 중..." : "생성 중...") : (isEditMode ? "템플릿 수정" : "템플릿 생성")}
                    </Button>
                  </div>
                </div>
              </div>
            )}

            {/* 템플릿 상세보기 모달 */}
            {showDetailModal && detailTemplate && (
              <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
                <div className="bg-white rounded-lg p-6 w-full max-w-4xl mx-4 max-h-[90vh] overflow-y-auto">
                  <div className="flex justify-between items-center mb-6">
                    <h2 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                      템플릿 상세보기
                    </h2>
                    <Button variant="ghost" onClick={() => setShowDetailModal(false)} className="p-1">
                      <X className="w-5 h-5" />
                    </Button>
                  </div>

                  {/* 템플릿 기본 정보 */}
                  <div className="bg-gray-100 p-4 rounded-lg mb-6">
                    <h3 className="text-lg font-semibold text-gray-800 mb-4">템플릿 정보</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                      <div>
                        <p className="text-sm text-gray-600">템플릿명</p>
                        <p className="font-medium">{detailTemplate.questionTemplateName}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">템플릿 번호</p>
                        <p className="font-medium">{detailTemplate.questionTemplateNum}번</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">질문 수</p>
                        <p className="font-medium">{detailTemplate.questionList?.length || 0}개</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">생성일</p>
                        <p className="font-medium">
                          {detailTemplate.createdAt ? new Date(detailTemplate.createdAt).toLocaleDateString('ko-KR') : '-'}
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* 템플릿 통계 */}
                  <div className="bg-gray-100 p-4 rounded-lg mb-6">
                    <h3 className="text-lg font-semibold text-gray-800 mb-4">템플릿 통계</h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      <div className="p-4 rounded-lg bg-white border border-1px solid black">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-sm font-medium">총 질문 수</p>
                            <p className="text-2xl font-bold text-blue-700">{detailTemplate.questionList?.length || 0}개</p>
                            <p className="text-xs">템플릿 내 질문</p>
                          </div>
                          <MessageSquare className="w-8 h-8 text-blue-500" />
                        </div>
                      </div>
                      <div className="border border-1px solid black p-4 rounded-lg">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-sm font-medium">5점 척도</p>
                            <p className="text-2xl font-bold text-[#1abc9c]">
                              {detailTemplate.questionList?.filter(q => q.evalQuestionType === 0).length || 0}개
                            </p>
                            <p className="text-x">객관식 질문</p>
                          </div>
                          <Star className="w-8 h-8 text-[#abc9c]" />
                        </div>
                      </div>
                      <div className="border border-1px solid black p-4 rounded-lg">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-sm font-medium">주관식</p>
                            <p className="text-2xl font-bold text-[#b0c4de]">
                              {detailTemplate.questionList?.filter(q => q.evalQuestionType === 1).length || 0}개
                            </p>
                            <p className="text-xs">서술형 질문</p>
                          </div>
                          <FileText className="w-8 h-8 text-[#b0c4de]" />
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 질문 목록 */}
                  <div className="bg-gray-100 p-4 rounded-lg">
                    <h3 className="text-lg font-semibold text-gray-800 mb-4">템플릿 질문 목록</h3>
                    <div className="space-y-3">
                      {detailTemplate.questionList && detailTemplate.questionList.length > 0 ? (
                        detailTemplate.questionList.map((question, index) => (
                          <div key={question.questionNum || index} className="border border-gray-200 rounded-lg p-4 bg-white">
                            <div className="flex justify-between items-start mb-3">
                              <div className="flex items-center gap-3">
                                <div className="w-8 h-8 bg-[#b0c4de] rounded-full flex items-center justify-center">
                                  <span className="text-sm font-bold text-[#2C3E50]">{question.questionNum || index + 1}</span>
                                </div>
                                <div className="flex-1">
                                  <h4 className="font-medium text-gray-900 mb-1">
                                    {question.evalQuestionText}
                                  </h4>
                                  <div className="flex items-center gap-2">
                                    <Badge className={`text-xs ${getTypeColor(question.evalQuestionType)}`}>
                                      {getTypeLabel(question.evalQuestionType)}
                                    </Badge>
                                  </div>
                                </div>
                              </div>
                            </div>      
                          </div>
                        ))
                      ) : (
                        <div className="text-center py-8">
                          <FileText className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                          <p className="text-gray-500">템플릿에 포함된 질문이 없습니다.</p>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* 모달 하단 버튼 */}
                  <div className="flex justify-end gap-3 mt-6 pt-6 border-t">
                    <Button variant="outline" onClick={() => setShowDetailModal(false)}
                      className="hover:bg-gray-200"
                    >
                      닫기
                    </Button>
                    <Button
                      onClick={() => {
                        setShowDetailModal(false)
                        setShowTemplateModal(true)
                        setIsEditMode(true)
                        setTemplateData({
                          questionTemplateName: detailTemplate.questionTemplateName,
                          description: detailTemplate.description || "",
                          category: detailTemplate.category || "강의 평가"
                        })
                        setTemplateSelectedQuestions(
                          detailTemplate.questionList?.map(q => q.evalQuestionId) || []
                        )
                      }}
                      className="bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white"
                    >
                      수정
                    </Button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </PageLayout>
  )
}
