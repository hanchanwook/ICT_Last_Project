import { useState, useEffect } from "react"
import { useNavigate, useSearchParams } from "react-router-dom"
import { Save, Plus, Minus, RotateCcw, Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getAllSubDetail, createSubject, getAllSubject, updateSubject } from "@/api/suhyeon/courseApi"
import { coursesMenuItems, createDynamicMenuItems } from "@/components/ui/menuConfig"

export default function SubjectsRegisterPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const editMode = searchParams.get('edit')
  
  const [formData, setFormData] = useState({
    subjectName: "",
    subjectInfo: "",
    curriculum: [], // 초기에 빈 배열로 시작
  })
  
  // 선택된 세부과목 ID들을 쉼표로 구분하여 저장할 변수
  const [selectedSubDetailIds, setSelectedSubDetailIds] = useState("")
  const [searchTerm, setSearchTerm] = useState("")
  const [isSubjectModalOpen, setIsSubjectModalOpen] = useState(false)
  const [selectedCurriculumIndex, setSelectedCurriculumIndex] = useState(null)
  const [availableSubjects, setAvailableSubjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [isEditLoading, setIsEditLoading] = useState(false)
  const [selectedSubjects, setSelectedSubjects] = useState([]) // 다중 선택을 위한 상태

  // API에서 세부과목 데이터 가져오기
  useEffect(() => {
    const fetchSubDetails = async () => {
      try {
        setLoading(true)
        const data = await getAllSubDetail()
        if (data && Array.isArray(data) && data.length > 0) {
          setAvailableSubjects(data)
        }
      } catch (error) {
        alert("세부과목 데이터를 불러오는데 실패했습니다.")
        setAvailableSubjects([])
      } finally {
        setLoading(false)
      }
    }

    fetchSubDetails()
  }, [])

  // 편집 모드일 때 기존 과목 데이터 불러오기
  useEffect(() => {
    const fetchSubjectForEdit = async () => {
      if (!editMode) return
      
      try {
        setIsEditLoading(true)
        
        const subjects = await getAllSubject()
        
        const subjectToEdit = subjects.find(s => s.subjectId === editMode)
        
        if (subjectToEdit) {
          // 폼 데이터 설정
          setFormData({
            subjectName: subjectToEdit.subjectName || "",
            subjectInfo: subjectToEdit.subjectInfo || "",
            curriculum: subjectToEdit.subDetails || [],
          })
          
          // 선택된 세부과목 ID들 설정
          if (subjectToEdit.subDetails && subjectToEdit.subDetails.length > 0) {
            const ids = subjectToEdit.subDetails.map(item => item.subDetailId).join(',')
            setSelectedSubDetailIds(ids)
          }
        } else {
          alert("편집할 과목을 찾을 수 없습니다.")
          navigate("/courses/subjects")
        }
      } catch (error) {
        alert("과목 데이터를 불러오는데 실패했습니다.")
        navigate("/courses/subjects")
      } finally {
        setIsEditLoading(false)
      }
    }

    fetchSubjectForEdit()
  }, [editMode, navigate])

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }  

  const addCurriculumItem = () => {
    // 최대 개수 체크 (10개)
    if (formData.curriculum.length >= 10) {
      alert("세부과목은 최대 10개까지 선택할 수 있습니다.")
      return
    }
    
    // 모달 열 때 기존 curriculum의 항목들을 selectedSubjects에 미리 설정
    setSelectedSubjects(formData.curriculum)
    setIsSubjectModalOpen(true)
  }

  const handleSelectSubject = (subject) => {
    // 최대 개수 체크 (10개) - 편집 모드가 아닐 때만 체크
    if (selectedCurriculumIndex === null && formData.curriculum.length >= 10) {
      alert("세부과목은 최대 10개까지 선택할 수 있습니다.")
      return
    }

    if (selectedCurriculumIndex !== null) {
      // 기존 항목 수정
      const newCurriculum = [...formData.curriculum]
      newCurriculum[selectedCurriculumIndex] = {
        subDetailId: subject.subDetailId,
        subDetailName: subject.subDetailName,
        subDetailInfo: subject.subDetailInfo || "설명 없음",
      }
      setFormData((prev) => ({
        ...prev,
        curriculum: newCurriculum,
      }))
      
      // 선택된 ID들 업데이트
      updateSelectedIds(newCurriculum)
      setIsSubjectModalOpen(false)
      setSelectedCurriculumIndex(null)
    } else {
      // 다중 선택 모드 - 임시 선택 목록에 추가/제거
      setSelectedSubjects(prev => {
        const isAlreadySelected = prev.some(item => item.subDetailId === subject.subDetailId)
        if (isAlreadySelected) {
          return prev.filter(item => item.subDetailId !== subject.subDetailId)
        } else {
          return [...prev, subject]
        }
      })
    }
  }

  const handleCloseSubjectModal = () => {
    setIsSubjectModalOpen(false)
    setSelectedCurriculumIndex(null)
    setSelectedSubjects([]) // 선택된 항목들 초기화
  }

  const handleConfirmSelection = () => {
    // 선택된 항목들을 curriculum으로 교체
    const newCurriculumItems = selectedSubjects.map(subject => ({
      subDetailId: subject.subDetailId,
      subDetailName: subject.subDetailName,
      subDetailInfo: subject.subDetailInfo || "설명 없음",
    }))
    
    setFormData((prev) => ({
      ...prev,
      curriculum: newCurriculumItems,
    }))
    
    // 선택된 ID들 업데이트
    updateSelectedIds(newCurriculumItems)
    
    // 모달 닫기 및 상태 초기화
    setIsSubjectModalOpen(false)
    setSelectedCurriculumIndex(null)
    setSelectedSubjects([])
  }

  // 선택된 세부과목 ID들을 쉼표로 구분하여 업데이트하는 함수
  const updateSelectedIds = (curriculum) => {
    const ids = curriculum.map(item => item.subDetailId).join(',')
    setSelectedSubDetailIds(ids)
  }

  const removeCurriculumItem = (index) => {
    const newCurriculum = formData.curriculum.filter((_, i) => i !== index)
    setFormData((prev) => ({
      ...prev,
      curriculum: newCurriculum,
    }))
    
    // 선택된 ID들 업데이트
    updateSelectedIds(newCurriculum)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    // 필수 필드 검증
    if (!formData.subjectName.trim()) {
      alert("과목명을 입력해주세요.")
      return
    }
    
    // 세부과목 개수 검증 (최소 1개, 최대 10개)
    if (formData.curriculum.length === 0) {
      alert("세부과목을 최소 1개 이상 선택해주세요.")
      return
    }
    
    if (formData.curriculum.length > 10) {
      alert("세부과목은 최대 10개까지 선택할 수 있습니다.")
      return
    }
    
    try {
      const trimmedName = formData.subjectName.trim()
      
      if (editMode) {
        // 편집 모드 - 수정 API 호출
        const subjectData = {
          subjectName: trimmedName,
          subjectInfo: formData.subjectInfo,
          subDetailId: selectedSubDetailIds,
        }
        
        await updateSubject(editMode, subjectData)
        
        alert(`${trimmedName} 과목이 성공적으로 수정되었습니다!`)
        navigate("/courses/subjects")
      } else {
        // 등록 모드 - 중복 검사 후 등록 API 호출
        const existingSubjects = await getAllSubject()
        const duplicateSubject = existingSubjects.find(subject => 
          subject.subjectName.toLowerCase() === trimmedName.toLowerCase()
        )
        
        if (duplicateSubject) {
          alert("이미 존재하는 과목명입니다. 다른 이름을 사용해주세요.")
          return
        }
        
        const subjectData = {
          subjectName: trimmedName,
          subjectInfo: formData.subjectInfo,
          subDetailId: selectedSubDetailIds,
        }
        
        await createSubject(subjectData)
        
        alert(`${trimmedName} 과목이 성공적으로 등록되었습니다!`)
        navigate("/courses/subjects")
      }
      
    } catch (error) {
      alert(editMode ? "과목 수정에 실패했습니다. 다시 시도해주세요." : "과목 등록에 실패했습니다. 다시 시도해주세요.")
    }
  }

  // 검색어에 따른 필터링된 세부과목 데이터를 반환하는 함수
  const getFilteredSubjects = () => {
    return availableSubjects
      .filter((subject) => {
        return (
          (subject.subDetailName && subject.subDetailName.toLowerCase().includes(searchTerm.toLowerCase())) ||
          (subject.subDetailInfo && subject.subDetailInfo.toLowerCase().includes(searchTerm.toLowerCase()))
        )
      })
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)) // 최신등록순 정렬
  }

  const handleReset = () => {
    setFormData({
      subjectName: "",
      subjectInfo: "",
      curriculum: [], // 초기화할 때도 빈 배열
    })
    setSelectedSubDetailIds("") // 선택된 ID들도 초기화
    setSearchTerm("") // 검색어도 초기화
    setSelectedSubjects([]) // 선택된 항목들도 초기화
  }

  return (
    <PageLayout currentPage="courses">
      <div className="flex">
        <Sidebar title="과정 관리" menuItems={createDynamicMenuItems(coursesMenuItems, null, editMode, "/courses/subjects/register")} currentPath="/courses/subjects/register" />

        <main className="flex-1 p-8">
          <div className="max-w-6xl mx-auto">
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                {editMode ? "과목 수정" : "새 과목 등록"}
              </h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                {editMode ? "기존 과목 정보를 수정합니다." : "새로운 과목을 등록합니다."}
              </p>
            </div>

            {isEditLoading ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-4"></div>
                <p style={{ color: "#95A5A6" }}>과목 정보를 불러오는 중...</p>
              </div>
            ) : (
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* 기본 정보 */}
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50" }}>기본 정보</CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                  <div className="grid grid-cols-1 md:grid-cols-1 gap-6">
                    <div className="space-y-2">
                      <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                        과목명 <span className="text-red-500">*</span> (최대 30자)
                      </label>
                      <Input
                        placeholder="과목명을 입력하세요"
                        value={formData.subjectName}
                        onChange={(e) => {
                          if (e.target.value.length <= 30) {
                            handleInputChange("subjectName", e.target.value)
                          }
                        }}
                        maxLength={30}
                        required
                      />
                      <div className="text-right">
                        <span className="text-xs" style={{ color: "#95A5A6" }}>
                          {formData.subjectName.length}/30
                        </span>
                      </div>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                      과목 설명 <span className="text-red-500">*</span> (최대 100자)
                    </label>
                    <Textarea
                      placeholder="과목에 대한 상세한 설명을 입력하세요"
                      value={formData.subjectInfo}
                      onChange={(e) => {
                        if (e.target.value.length <= 100) {
                          handleInputChange("subjectInfo", e.target.value)
                        }
                      }}
                      rows={4}
                      maxLength={100}
                      required
                    />
                    <div className="text-right">
                      <span className="text-xs" style={{ color: "#95A5A6" }}>
                        {formData.subjectInfo.length}/100
                      </span>
                    </div>
                  </div>
                </CardContent>
              </Card>
              {/* 세부과목*/}
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50" }}>세부과목</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  {formData.curriculum.length === 0 ? (
                    <div className="text-center py-8">
                      <div className="text-lg mb-4" style={{ color: "#95A5A6" }}>
                        등록된 세부과목이 없습니다
                      </div>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={addCurriculumItem}
                        className="bg-transparent text-[#1abc9c] border border-[#1abc9c] hover:!bg-[#1abc9c] hover:!text-white"
                      >
                        <Plus className="w-4 h-4 mr-2" />
                        세부과목 추가하기
                      </Button>
                    </div>
                  ) : (
                    <>
                      {formData.curriculum.map((item, index) => (
                        <div
                          key={index}
                          className="p-4 border rounded-lg space-y-4"
                          style={{ borderColor: "#e0e0e0" }}
                        >
                          <div className="flex items-center justify-between">
                            <div>
                              <p style={{ color: "#95A5A6" }}>
                                세부과목 {index + 1}
                              </p>
                              <h4 className="font-medium" style={{ color: "#2C3E50", fontWeight: "bold" }}>{item.subDetailName}</h4>
                            </div>
                            <Button
                              type="button"
                              size="sm"
                              variant="outline"
                              onClick={() => removeCurriculumItem(index)}
                              className="bg-transparent border border-[#e74c3c] text-[#e74c3c]
                               hover:!bg-red-300 hover:!text-[#e74c3c]"
                            >
                              <Minus className="w-4 h-4" />
                            </Button>
                          </div>
                          <div className="space-y-2">
                            <label className="text-sm font-medium" style={{ color: "#95A5A6" }}>
                              세부과목 설명
                            </label>
                            {item.subDetailName ? (
                                <div className="p-3 border rounded-md" style={{ borderColor: "#e0e0e0" }}>
                                  <div className="flex items-center justify-between">
                                  <div>
                                    <p className="text-sm" >{item.subDetailInfo}</p>
                                  </div>
                                </div>
                              </div>
                            ) : (
                              <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={() => {
                                  setSelectedCurriculumIndex(index)
                                  setIsSubjectModalOpen(true)
                                }}
                                className="w-full bg-transparent"
                                style={{ borderColor: "#1ABC9C", color: "#1ABC9C" }}
                              >
                                세부과목 선택하기
                              </Button>
                            )}
                          </div>
                        </div>
                      ))}
                      <Button
                        type="button"
                        size="sm"
                        onClick={addCurriculumItem}
                        className="bg-transparent text-[#1ABC9C] border border-[#1ABC9C] hover:bg-[#1ABC9C] hover:text-white"
                      >
                        <Plus className="w-4 h-4 mr-2" />
                        세부과목 추가하기
                      </Button>
                    </>
                  )}
                </CardContent>
              </Card>

              {/* 버튼 그룹 */}
              <div className="flex justify-end space-x-4 pt-6">
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleReset}
                  className="flex items-center space-x-2 bg-transparent
                  text-[#95A5A5] border border-[#95A5A5] hover:!bg-gray-100
                  hover:text-[#95A5A5]"
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>초기화</span>
                </Button>
                <Button
                  type="submit"
                  className="text-white font-medium flex items-center space-x-2
                  bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                  disabled={isEditLoading}
                >
                  <Save className="w-4 h-4" />
                  <span>{editMode ? "과목 수정" : "과목 등록"}</span>
                </Button>
              </div>
            </form>
            )}

            {/* 등록 안내 */}
            <Card className="mt-6" style={{ borderColor: "#e0e0e0", borderWidth: "1px" }}>
              <CardContent className="pt-6">
                <h3 className="font-semibold mb-2" style={{ color: "#2C3E50" }}>
                  {editMode ? "과목 수정" : "과목 등록"} 안내사항
                </h3>
                <ul className="space-y-1 text-sm" style={{ color: "#95A5A6" }}>
                  <li>• 필수 항목(*)은 반드시 입력해주세요.</li>
                  <li>• 과목명은 중복될 수 없습니다.</li>
                  {editMode ? (
                    <>
                      <li>• 수정된 과목 정보는 즉시 반영됩니다.</li>
                      <li>• 수정 후 과목 정보는 다시 변경할 수 있습니다.</li>
                    </>
                  ) : (
                    <>
                      <li>• 등록된 과목은 즉시 과목 목록에 표시됩니다.</li>
                      <li>• 등록 후 과목 정보는 수정할 수 있습니다.</li>
                    </>
                  )}
                </ul>
              </CardContent>
            </Card>
          </div>
        </main>
      </div>

      {/* 세부과목 선택 모달 */}
      {isSubjectModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <Card className="w-full max-w-4xl mx-4 max-h-[80vh] overflow-y-auto">
            <CardHeader>
              <div className="flex items-center justify-between mb-4">
                <CardTitle style={{ color: "#2C3E50" }}>세부과목 선택</CardTitle>
                <Button variant="ghost" size="sm" onClick={handleCloseSubjectModal} style={{ color: "#95A5A6" }}>
                  ✕
                </Button>
              </div>
              <div className="flex-1 mb-4">
                <div className="relative">
                  <Search
                    className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4"
                    style={{ color: "#95A5A6" }}
                  />
                  <Input
                    placeholder="세부 과목명으로 검색..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
              </div>
                             {selectedCurriculumIndex === null && (
                 <div className="flex items-center justify-between">
                   <div className="text-sm" style={{ color: "#95A5A6" }}>
                     선택된 항목: {selectedSubjects.length}개
                   </div>
                   <div className="flex space-x-2">
                     <Button
                       variant="outline"
                       size="sm"
                       onClick={() => setSelectedSubjects(formData.curriculum)}
                       className="text-[#e74c3c] border-[#e74c3c] hover:bg-[#e74c3c] hover:text-white"
                     >
                       선택 취소
                     </Button>
                     <Button
                       size="sm"
                       onClick={handleConfirmSelection}
                       disabled={selectedSubjects.length === 0}
                       className="bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white"
                     >
                       선택 완료 ({selectedSubjects.length}개)
                     </Button>
                   </div>
                 </div>
               )}
            </CardHeader>
            <CardContent>
              <div className="mb-4">
                <p className="text-sm" style={{ color: "#95A5A6" }}>
                  총 세부과목: {availableSubjects.length}개
                </p>
              </div>
              {/* 검색어에 따른 필터링된 세부과목 */}
              {(() => {
                const filteredSubjects = getFilteredSubjects()
                
                return (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {filteredSubjects.map((subject) => {
                      // 현재 모달에서 선택된 세부과목인지 확인
                      const isCurrentlySelected = selectedSubjects.some(item => item.subDetailId === subject.subDetailId)
                      
                      return (
                        <div
                          key={subject.subDetailId}
                          className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                            isCurrentlySelected ? 'border-[#1abc9c] bg-[#f0f9ff]' : 'border-gray-200'
                          }`}
                          onClick={() => handleSelectSubject(subject)}
                        >
                          <div className="flex items-start justify-between mb-2">
                            <h4 className="font-semibold" style={{ color: "#2C3E50" }}>
                              {subject.subDetailName}
                            </h4>
                            <Button
                              size="sm"
                              className={`${
                                isCurrentlySelected 
                                  ? "bg-[#1abc9c] text-white" 
                                  : "bg-gray-200 text-gray-700 hover:bg-[#1abc9c] hover:text-white"
                              }`}
                              onClick={(e) => {
                                e.stopPropagation();
                                handleSelectSubject(subject);
                              }}
                            >
                              {isCurrentlySelected ? "선택됨" : "선택"}
                            </Button>
                          </div>
                          <p className="text-sm mb-3" style={{ color: "#95A5A6" }}>
                            {subject.subDetailInfo || "설명 없음"}
                          </p>
                        </div>
                      )
                    })}
                  </div>
                )
              })()}

              {loading ? (
                <div className="text-center py-8">
                  <div className="text-lg" style={{ color: "#95A5A6" }}>
                    세부과목 데이터를 불러오는 중...
                  </div>
                </div>
              ) : availableSubjects.length === 0 ? (
                <div className="text-center py-8">
                  <div
                    className="w-16 h-16 mx-auto mb-4 flex items-center justify-center rounded-full"
                    style={{ backgroundColor: "#e0e0e0" }}
                  >
                    <span className="text-2xl">📚</span>
                  </div>
                  <h3 className="text-xl font-semibold mb-2" style={{ color: "#2C3E50" }}>
                    등록된 세부과목이 없습니다
                  </h3>
                  <p style={{ color: "#95A5A6" }}>먼저 세부과목을 등록해주세요.</p>
                </div>
              ) : (() => {
                const filteredSubjects = getFilteredSubjects()
                
                return filteredSubjects.length === 0 ? (
                  <div className="text-center py-8">
                    <div
                      className="w-16 h-16 mx-auto mb-4 flex items-center justify-center rounded-full"
                      style={{ backgroundColor: "#e0e0e0" }}
                    >
                      <span className="text-2xl">🔍</span>
                    </div>
                    <h3 className="text-xl font-semibold mb-2" style={{ color: "#2C3E50" }}>
                      검색 결과가 없습니다
                    </h3>
                    <p style={{ color: "#95A5A6" }}>다른 검색어를 사용해보세요.</p>
                  </div>
                ) : null
              })()}
            </CardContent>
          </Card>
        </div>
      )}
    </PageLayout>
  )
}
