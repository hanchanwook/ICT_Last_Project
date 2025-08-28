import { useState, useEffect } from "react"
import { Search, Plus, Trash2, Eye, Star, MessageSquare, FileText, BarChart3, Users, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { evaluationMenuItems } from "@/components/ui/menuConfig"
import { getAllEvaluation, createEvaluation, createEvaluationTemplate, getEvaluationTemplateList } from "@/api/suhyeon/evaluationApi"
import { deleteEvaluation } from "@/api/suhyeon/evaluationApi"

export default function evaluationQuestion() {
  const [currentPath] = useState("/survey/items")
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedType, setSelectedType] = useState("전체")
  const [showTemplateModal, setShowTemplateModal] = useState(false)
  const [templateSelectedQuestions, setTemplateSelectedQuestions] = useState([])
  // 템플릿 모달 전용 검색 및 필터 상태
  const [templateSearchTerm, setTemplateSearchTerm] = useState("")
  const [templateSelectedType, setTemplateSelectedType] = useState("전체")
  const [templateData, setTemplateData] = useState({
    questionTemplateName: "",
    name: "",
    description: "",
    category: "일반",
  })

  const [showItemModal, setShowItemModal] = useState(false)
  const [itemType, setItemType] = useState("주관식")
  const [itemContent, setItemContent] = useState("")

  const [showDetailModal, setShowDetailModal] = useState(false)
  const [detailItem, setDetailItem] = useState(null)

  // 질문지 데이터 (API에서 받아올 예정)
  const [surveyItems, setSurveyItems] = useState([])
  const [templates, setTemplates] = useState([])

  // 통계 데이터
  // 실제 데이터 기반 통계 계산
  const totalCount = surveyItems.length
  // usageCount가 30 이상인 질문을 자주 사용되는 질문으로 가정
  const frequentCount = surveyItems.filter(item => (item.useEvalQuestion || 0) >= 30).length
  // createdAt이 이번 달인 질문 개수 (없으면 lastUsed 기준으로 대체)
  const now = new Date()
  const thisMonth = now.getMonth() + 1
  const thisYear = now.getFullYear()
  const recentCount = surveyItems.filter(item => {
    const dateStr = item.createdAt || item.lastUsed
    if (!dateStr) return false
    const d = new Date(dateStr)
    return d.getFullYear() === thisYear && (d.getMonth() + 1) === thisMonth
  }).length

  const stats = [
    {
      title: "전체 질문",
      value: `${totalCount}개`,
      icon: MessageSquare,
      color: "#3498db", 
      backgroundcolor: "#EFF6FF",
      description: "등록된 질문 수",
    },
    {
      title: "최근 추가된 질문",
      value: `${recentCount}개`,
      icon: Plus,
      color: "#b0c4de",
      backgroundcolor: "#EFF6FF",
      description: "이번 달 추가",
    },
  ]

  useEffect(() => {
    getAllEvaluation()
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) {
          // API 데이터가 있으면 surveyItems를 갱신
          setSurveyItems(data)
        }
      })
      .catch((err) => {
        // 에러 발생 시 더미 데이터 유지
        console.error("평가 항목 불러오기 실패", err)
      })

    // 템플릿 목록도 함께 불러오기
    getEvaluationTemplateList()
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) {
          setTemplates(data)
        }
      })
      .catch((err) => {
        console.error("템플릿 목록 불러오기 실패", err)
        setTemplates([])
      })
  }, [])

  const types = ["전체", "5점 척도", "주관식"]

  // 유형 변환 함수 (0: 5점 척도, 1: 서술형)
  const getTypeLabel = (type) => {
    if (
      type === 0 || type === "0" ||
      type === "5점 척도" ||
      type === "객관식(5점 척도)" ||
      type === "객관식"
    ) return "5점 척도"
    if (
      type === 1 || type === "1" ||
      type === "주관식" || type === "서술형"
    ) return "주관식"
    return "-"
  }

  const getTypeColor = (type) => {
    if (type === 0 || type === "0" || type === "5점 척도") {
      return "bg-[#e4f5eb] text-[#1bac9c]"
    }
    if (type === 1 || type === "1" || type === "서술형" || type === "주관식") {
      return "bg-blue-100 text-blue-800"
    }
    return "bg-gray-100 text-gray-800"
  }

  // createdAt 기준 내림차순 정렬 (최신이 위로)
  const sortedSurveyItems = [...surveyItems].sort((a, b) => {
    if (!a.createdAt && !b.createdAt) return 0;
    if (!a.createdAt) return 1;
    if (!b.createdAt) return -1;
    return new Date(b.createdAt) - new Date(a.createdAt);
  })

  // type 정보가 없는 데이터는 아예 제외
  const validSurveyItems = sortedSurveyItems.filter(
    item => getTypeLabel(item.type ?? item.evalQuestionType) !== "-"
  )

  const filteredItems = validSurveyItems.filter((item) => {
    const questionText = item.question || item.evalQuestionText || ""
    const typeText = item.type ?? item.evalQuestionType ?? ""
    const label = getTypeLabel(typeText)
    const matchesSearch = questionText.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesType = selectedType === "전체" || label === selectedType
    return matchesSearch && matchesType
  })

  // 템플릿 모달 전용 필터링된 아이템
  const templateFilteredItems = validSurveyItems.filter((item) => {
    const questionText = item.question || item.evalQuestionText || ""
    const typeText = item.type ?? item.evalQuestionType ?? ""
    const label = getTypeLabel(typeText)
    const matchesSearch = questionText.toLowerCase().includes(templateSearchTerm.toLowerCase())
    const matchesType = templateSelectedType === "전체" || label === templateSelectedType
    return matchesSearch && matchesType
  })

  const handleView = (itemId) => {
    const item = surveyItems.find((i) => i.evalQuestionId === itemId)
    setDetailItem(item)
    setShowDetailModal(true)
  }

  const handleDelete = async (itemId) => {
    if (!window.confirm("정말로 이 질문을 삭제하시겠습니까?")) return;
    try {
      const res = await deleteEvaluation(itemId);
      // 삭제 후 목록 새로고침
      const data = await getAllEvaluation();
      if (Array.isArray(data) && data.length > 0) {
        setSurveyItems(data);
      } else {
        setSurveyItems([]);
      }
      alert("삭제가 완료되었습니다.");
    } catch (err) {
      console.error("삭제 실패:", err);
      alert("삭제 실패: " + (err?.message || err));
    }
  }

  const handleCreateTemplate = async () => {
    try {
      // 템플릿 이름 중복 검사
      const trimmedName = templateData.questionTemplateName.trim()
      const isDuplicate = templates.some(template => {
        const existingName = (template.questionTemplateName || "").trim()
        return existingName.toLowerCase() === trimmedName.toLowerCase()
      })
      
      if (isDuplicate) {
        alert("이미 존재하는 템플릿 이름입니다. 다른 이름을 입력해주세요.")
        return
      }

      const questionList = templateSelectedQuestions.map((id, idx) => ({
        evalQuestionId: id,
        questionNum: idx + 1,
      }));
      
      const templatePayload = {
        questionTemplateName: templateData.questionTemplateName,
        questionList,
      };
      
      const response = await createEvaluationTemplate(templatePayload);
      
      alert("템플릿이 성공적으로 생성되었습니다.");
      setShowTemplateModal(false);
      setTemplateSelectedQuestions([]);
      setTemplateData({
        questionTemplateName: "",
        name: "",
        description: "",
        category: "일반",
      });
    } catch (error) {
      console.error("템플릿 생성 실패:", error);
      alert("템플릿 생성에 실패했습니다: " + (error?.message || error));
    }
  }

  const handleTemplateModalClose = () => {
    setShowTemplateModal(false)
    setTemplateSearchTerm("")
    setTemplateSelectedType("전체")
    setTemplateData({
      questionTemplateName: "",
      name: "",
      description: "",
      category: "일반",
    })
  }

  return (
    <PageLayout currentPage="evaluations">

      <div className="flex">
        <Sidebar title="설문 평가 관리" menuItems={evaluationMenuItems} currentPath="/evaluations" />

        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
            {/* 페이지 헤더 */}
            <div className="mb-6">
              <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                평가 항목 관리
              </h1>
              <p className="text-gray-600">설문에 사용할 질문들을 관리합니다.</p>
            </div>

            {/* 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
              {stats.map((stat, index) => (
                <Card key={index} className="bg-white p-6 rounded-lg ">
                  
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium text-gray-600 mb-1">{stat.title}</p>
                        <p className="text-3xl font-bold" style={{ color: stat.color }}>
                          {stat.value}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">{stat.description}</p>
                      </div>
                      <div className="p-3 rounded-full" style={{ backgroundColor: stat.backgroundcolor }}>
                        <stat.icon className="w-10 h-10" style={{ color: stat.color }} />
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>

            {/* 검색 및 필터 */}
            <Card className="mb-6">
              <CardContent className="p-6">
                <div className="flex flex-col md:flex-row gap-4">
                  <div className="flex-1">
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                      <Input
                        placeholder="질문 내용으로 검색..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="pl-10"
                      />
                    </div>
                  </div>
                  <div className="flex gap-2">
                   <select
                     value={selectedType}
                     onChange={(e) => setSelectedType(e.target.value)}
                     className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                   >
                     {types.map((type) => (
                       <option key={type} value={type}>
                         {type}
                       </option>
                     ))}
                   </select>
                    <Button
                      onClick={() => {
                        setShowTemplateModal(true);
                        setTemplateSelectedQuestions([]);
                      }}
                      className="bg-wihte hover:bg-[#1abc9c] hover:text-white 
                      text-[#1abc9c] border border-1px solid #1abc9c"
                    >
                      <FileText className="w-4 h-4 mr-2" />
                      템플릿 만들기
                    </Button>
                    <Button onClick={() => setShowItemModal(true)} 
                      className="hover:bg-[#1abc9c] hover:text-white bg-white
                      text-[#1abc9c] border border-[#1abc9c]">
                      <Plus className="w-4 h-4 mr-2" />새 질문 추가
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 질문 목록 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center justify-between">
                  <span className="flex items-center gap-2">
                    <BarChart3 className="w-5 h-5" style={{ color: "#1abc9c" }} />
                    질문 목록 ({filteredItems.length}개)
                  </span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b">
                        <th className="text-left py-3 px-4 font-medium text-gray-600">질문 내용</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-600">유형</th>
                        <th className="text-center py-3 px-4 font-medium text-gray-600">사용 횟수</th>
                        <th className="text-center py-3 px-4 font-medium text-gray-600">최근 사용</th>
                        <th className="text-center py-3 px-4 font-medium text-gray-600">관리</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredItems.map((item, index) => (
                        <tr key={item.evalQuestionId || index} className="border-b">
                          <td className="py-3 px-4">
                            <div>
                              <p className="font-medium text-sm">
                                {(item.question || item.evalQuestionText || "").length > 35
                                  ? `${(item.question || item.evalQuestionText).substring(0, 35)}...`
                                  : (item.question || item.evalQuestionText)}
                              </p>
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <Badge className={`text-xs ${getTypeColor(item.type || item.evalQuestionType)}`}>{getTypeLabel(item.type || item.evalQuestionType)}</Badge>
                          </td>
                          <td className="py-3 px-4 text-center">
                            <div className="flex items-center justify-center gap-1">
                              <Users className="w-4 h-4 text-gray-400" />
                              <span className="text-sm font-medium">{item.useEvalQuestion}</span>
                            </div>
                          </td>
                          <td className="py-3 px-4 text-center text-sm text-gray-600">{item.templateCreatedAt}</td>
                          <td className="py-3 px-4">
                            <div className="flex justify-center space-x-2">
                              <Button size="sm" variant="ghost" 
                                onClick={() => handleView(item.evalQuestionId)} 
                                className="p-1 hover:bg-blue-50 
                                hover:scale-110 transition-all duration-200"
                              >
                                <Eye className="w-4 h-4 " 
                                style={{ color: "#1abc9c" }} />
                              </Button>
                              <Button size="sm" variant="ghost" 
                                onClick={() => handleDelete(item.evalQuestionId)} 
                                className="p-1 hover:bg-red-50 
                                hover:scale-110 transition-all duration-200"
                              >
                                <Trash2 className="w-4 h-4" style={{ color: "#e74c3c" }} />
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {filteredItems.length === 0 && (
                  <div className="text-center py-8">
                    <FileText className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500">검색 조건에 맞는 질문이 없습니다.</p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
    
      {/* 템플릿 생성 모달 */}
      {showTemplateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 w-full max-w-4xl mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                설문 템플릿 만들기
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
                {/* 선택된 질문 리스트 (순서 중요) */}
                <div className="bg-blue-50 p-4 rounded-lg">
                  <h4 className="font-medium text-blue-900 mb-2">선택된 질문 (순서대로 질문을 선택해주세요.)</h4>
                  <ol className="list-decimal pl-5 space-y-1">
                    {templateSelectedQuestions
                      .map(id => validSurveyItems.find(item => item.evalQuestionId === id))
                      .filter(Boolean)
                      .map((item, idx) => (
                        <li key={item.evalQuestionId} className="text-green-800 text-sm">
                          {item.question || item.evalQuestionText}
                        </li>
                      ))}
                  </ol>
                  <p className="text-sm text-blue-800">총 {templateSelectedQuestions.length}개의 질문이 선택되었습니다.</p>
                  {templateSelectedQuestions.length === 0 && (
                    <p className="text-sm text-red-500 mt-2">최소 1개 이상의 질문을 선택해주세요.</p>
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
                    className="text-sm bg-transparent"
                  >
                    {templateSelectedQuestions.length === templateFilteredItems.length ? "전체 해제" : "전체 선택"}
                  </Button>
                </div>

                {/* 템플릿 모달 내 검색 및 필터 */}
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
                  {templateFilteredItems.map((item) => (
                    <div
                      key={item.evalQuestionId}
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
                          
                            <Badge className={`text-xs ${getTypeColor(item.type || item.evalQuestionType)}`}>{getTypeLabel(item.type || item.evalQuestionType)}</Badge>
                          </div>
                        </div>
                      </label>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* 버튼 영역 */}
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t">
              <Button variant="outline" onClick={handleTemplateModalClose}
              className="text-gray-500 border border-gray-300 hover:bg-gray-100">
                취소
              </Button>
              <Button
                onClick={() => handleCreateTemplate()}
                disabled={!templateData.questionTemplateName.trim() || templateSelectedQuestions.length === 0}
                className="bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white"
              >
                템플릿 생성
              </Button>
            </div>
          </div>
        </div>
      )}
      
      {/* 항목 등록 모달 */}
      {showItemModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                항목 등록
              </h2>
              <Button variant="ghost" onClick={() => setShowItemModal(false)} className="p-2">
                <X className="w-5 h-5" />
              </Button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-2" style={{ color: "#2C3E50" }}>
                  유형 선택 *
                </label>
                <select
                  value={itemType}
                  onChange={e => setItemType(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                >
                  <option value="0">객관식(5점 척도)</option>
                  <option value="1">주관식</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium mb-2" style={{ color: "#2C3E50" }}>
                  질문 내용 * (최대 100자)
                </label>
                <textarea
                  value={itemContent}
                  onChange={e => {
                    if (e.target.value.length <= 100) {
                      setItemContent(e.target.value)
                    }
                  }}
                  placeholder="질문 내용을 입력하세요"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md resize-none"
                  rows={3}
                  maxLength={100}
                />
                <div className="text-right mt-1">
                  <span className="text-xs" style={{ color: "#95A5A6" }}>
                    {itemContent.length}/100
                  </span>
                </div>
              </div>
            </div>
              {/* 안내사항 */}
              <div className="bg-blue-50 p-4 rounded-lg">
                <h4 className="font-medium text-blue-900 mb-2">질문 작성 가이드</h4>
                <ul className="text-sm text-blue-800 space-y-1">
                  <li>• 명확하고 이해하기 쉬운 질문을 작성해주세요</li>
                  <li>• 5점 척도: 1(매우 불만족) ~ 5(매우 만족) 형태로 평가</li>
                  <li>• 주관식: 자유롭게 의견을 작성할 수 있는 질문</li>
                  <li>• 등록한 항목은 수정이 불가능 합니다. 삭제 후 다시 등록해주세요. </li>
                  <li>• 템플릿 생성 시 선택된 질문은 템플릿에 포함됩니다.</li>
                </ul>
              </div>

            {/* 버튼 영역 */}
            <div className="flex justify-end space-x-3 mt-6">
        <Button
          variant="outline"
          onClick={() => setShowItemModal(false)}
          className="text-gray-500 border border-gray-300 hover:bg-gray-100"
        >
          취소
        </Button>
        <Button
          onClick={async () => {
            try {
              // 입력값 검증
              const trimmedContent = itemContent.trim()
              
              if (!trimmedContent) {
                alert("질문 내용을 입력해주세요.")
                return
              }
              
              if (trimmedContent.length < 5) {
                alert("질문 내용은 최소 5자 이상 입력해주세요.")
                return
              }
              
              // 중복 검사 강화 (대소문자 구분 없이, 공백 제거 후 비교)
              const isDuplicate = surveyItems.some(item => {
                const existingContent = (item.evalQuestionText || item.question || "").trim()
                const normalizedExisting = existingContent.toLowerCase().replace(/\s+/g, ' ')
                const normalizedNew = trimmedContent.toLowerCase().replace(/\s+/g, ' ')
                const isMatch = normalizedExisting === normalizedNew
                return isMatch
              })
              
              if (isDuplicate) {
                alert("이미 등록된 질문과 동일한 내용입니다. 다른 질문을 입력해주세요.")
                return
              }
              
              // 등록할 데이터 구조
              const newItem = {
                evalQuestionText: trimmedContent,
                evalQuestionType: Number(itemType), // 0: 5점 척도, 1: 주관식
              }
              
              // API 호출
              const result = await createEvaluation(newItem)
              
              // 등록 후 목록 새로고침
              const updatedData = await getAllEvaluation()
              
              if (Array.isArray(updatedData) && updatedData.length > 0) {
                setSurveyItems(updatedData)
              } else {
                setSurveyItems([])
              }
              
              // 모달 초기화
              setShowItemModal(false)
              setItemType("0")
              setItemContent("")
              
              alert("평가 항목이 성공적으로 등록되었습니다.")
              
            } catch (err) {
              console.error("등록 실패:", err)
              console.error("에러 상세:", err.response?.data || err.message)
              
              let errorMessage = "등록에 실패했습니다."
              if (err.response?.data?.message) {
                errorMessage += `\n${err.response.data.message}`
              } else if (err.message) {
                errorMessage += `\n${err.message}`
              }
              
              alert(errorMessage)
            }
          }}
          className="bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white"
          disabled={!itemContent.trim()}
        >
          등록
        </Button>
      </div>
          </div>
        </div>
      )}
      {/* 상세보기 모달 */}
      {showDetailModal && detailItem && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                질문 상세보기
              </h2>
              <Button variant="ghost" onClick={() => setShowDetailModal(false)} className="p-2">
                <X className="w-5 h-5" />
              </Button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1 text-gray-500">유형</label>
                <div>{getTypeLabel(detailItem.type || detailItem.evalQuestionType)}</div>
              </div>
              <div>
                <label className="block text-sm font-medium mb-1 text-gray-500">질문 내용</label>
                <div className="text-base font-semibold">{detailItem.evalQuestionText || detailItem.question}</div>
              </div>
            </div>
          </div>
        </div>
      )}
    </PageLayout>
  )
}
