// 세부 과목 목록/관리 페이지 (과목별 세부과목 리스트, 등록/상세/삭제 등 UI)
import { useState, useEffect } from "react"
import { Search, Plus, Trash2, Eye, BookOpen, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { useNavigate } from "react-router-dom"
import { getAllSubDetail, createSubDetail, updateSubDetail, deleteSubDetail } from "@/api/suhyeon/courseApi"
import { coursesMenuItems } from "@/components/ui/menuConfig"

export default function SubDetailListPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [showDetailModal, setShowDetailModal] = useState(false)
  const [selectedSubject, setSelectedSubject] = useState(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newSubDetailName, setNewSubDetailName] = useState("")
  const [newSubDetailInfo, setNewSubDetailInfo] = useState("")
  
  // 통합 모달 상태 관리 (생성/편집 모드)
  const [isEditMode, setIsEditMode] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  
  // 세부과목 데이터, 로딩/에러 상태
  const [detailSubjectsData, setDetailSubjectsData] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const navigate = useNavigate()

  // 컴포넌트 마운트 시 세부과목 목록 API 호출
  useEffect(() => {
    const fetchDetailSubjects = async () => {
      try {
        setLoading(true)
        setError(null)
        const data = await getAllSubDetail()
        setDetailSubjectsData(data)
      } catch (err) {
        console.error('세부과목 데이터 로딩 실패:', err)
        setError('세부과목 목록을 불러오는데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }
    fetchDetailSubjects()
  }, [])

  // 검색어에 따른 필터링된 세부과목 데이터를 반환하는 함수
  const getFilteredSubjects = () => {
    return detailSubjectsData
      .filter((subject) => {
        return (
          (subject.subDetailName && subject.subDetailName.toLowerCase().includes(searchTerm.toLowerCase())) ||
          (subject.subDetailInfo && subject.subDetailInfo.toLowerCase().includes(searchTerm.toLowerCase()))
        )
      })
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  }

  // 검색 적용된 세부과목 데이터 (최신등록순 정렬)
  const filteredDetailSubjects = getFilteredSubjects()

  // 세부과목 삭제 (확인 후)
  const handleDelete = async (subjectId) => {
    if (confirm("정말로 이 세부 과목을 삭제하시겠습니까?")) {
      try {
        await deleteSubDetail(subjectId)
        alert(`세부 과목이 삭제되었습니다.`)
        // 삭제 후 목록 갱신
        const updatedData = await getAllSubDetail()
        setDetailSubjectsData(updatedData)
      } catch (err) {
        alert("세부 과목 삭제에 실패했습니다.")
      }
    }
  }

  // 세부과목 상세 모달 열기
  const handleView = (subjectId) => {
    const subject = detailSubjectsData.find((s) => s.subDetailId === subjectId)
    setSelectedSubject(subject)
    setShowDetailModal(true)
  }

  // 세부과목 편집 모달 열기 (상세 모달의 편집 버튼에서 호출)
  const handleEdit = (subjectId) => {
    const subject = detailSubjectsData.find((s) => s.subDetailId === subjectId)
    setEditTarget(subject)
    setIsEditMode(true)
    setNewSubDetailName(subject.subDetailName)
    setNewSubDetailInfo(subject.subDetailInfo)
    setShowCreateModal(true)
  }

  // 세부과목 등록 모달 열기 (새로 만들기)
  const handleCreateNew = () => {
    setIsEditMode(false)
    setEditTarget(null)
    setNewSubDetailName("")
    setNewSubDetailInfo("")
    setShowCreateModal(true)
  }

  // 동일 이름 검사 함수
  const checkDuplicateName = (name, excludeId = null) => {
    const trimmedName = name.trim()
    return detailSubjectsData.find(subject => 
      (excludeId ? subject.subDetailId !== excludeId : true) && 
      subject.subDetailName.toLowerCase() === trimmedName.toLowerCase()
    )
  }

  // 세부과목 등록/편집 처리
  const handleCreateSubmit = async () => {
    if (!newSubDetailName.trim() || !newSubDetailInfo.trim()) {
      alert("세부 과목명과 설명을 모두 입력해주세요.")
      return
    }
    
    // 동일 이름 검사
    const duplicateSubject = isEditMode 
      ? checkDuplicateName(newSubDetailName, editTarget.subDetailId)
      : checkDuplicateName(newSubDetailName)
    
    if (duplicateSubject) {
      alert("이미 존재하는 세부 과목명입니다. 다른 이름을 사용해주세요.")
      return
    }
    
    try {
      const trimmedName = newSubDetailName.trim()
      if (isEditMode) {
        // 편집 모드: 기존 세부과목 수정
        const updateData = {
          subDetailName: trimmedName,
          subDetailInfo: newSubDetailInfo,
        }
        await updateSubDetail(editTarget.subDetailId, updateData)
        alert(`"${trimmedName}" 세부 과목이 수정되었습니다.`)
      } else {
        // 생성 모드: 새 세부과목 등록
        const createData = {
          subDetailName: trimmedName,
          subDetailInfo: newSubDetailInfo,
        }
        await createSubDetail(createData)
        alert(`"${trimmedName}" 세부 과목이 생성되었습니다.`)
      }
      
      // 성공 후 모달 닫기 및 상태 초기화
      setShowCreateModal(false)
      setNewSubDetailName("")
      setNewSubDetailInfo("")
      setIsEditMode(false)
      setEditTarget(null)
      
      // 목록 새로고침
      const updatedData = await getAllSubDetail()
      setDetailSubjectsData(updatedData)
      
    } catch (error) {
      console.error('세부과목 처리 실패:', error)
      alert(isEditMode ? '세부 과목 수정에 실패했습니다.' : '세부 과목 생성에 실패했습니다.')
    }
  }

  // 세부과목 등록 모달 닫기
  const handleCreateCancel = () => {
    setShowCreateModal(false)
    setNewSubDetailName("")
    setNewSubDetailInfo("")
    setIsEditMode(false)
    setEditTarget(null)
  }

  return (
    <PageLayout currentPage="courses">
      <div className="flex">
        <Sidebar title="과정 관리" menuItems={coursesMenuItems} currentPath="/courses/subjectdetail" />
        <main className="flex-1 p-8">
          <div className="max-w-7xl mx-auto">
            {/* 상단 타이틀/설명 */}
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                세부 과목 목록
              </h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                등록된 모든 세부 과목의 정보를 조회하고 관리할 수 있습니다.
              </p>
            </div>
            {/* 통계 카드 */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-3xl font-bold" style={{ color: "#3498db" }}>
                    {detailSubjectsData.length}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    전체 세부 과목
                  </div>
                </CardContent>
              </Card>
            </div>
            {/* 검색/필터 영역 */}
            <Card className="mb-6">
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>검색 및 필터</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-col md:flex-row gap-1">
                  <div className="flex-1">
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
                  <div className="flex gap-2">
                    <Button
                      className="text-[#1abc9c] border border-[#1abc9c] font-medium flex items-center space-x-2 hover:bg-[#1abc9c] hover:text-white"
                      onClick={handleCreateNew}
                    >
                      <Plus className="w-4 h-4" />
                      <span>세부 과목 추가</span>
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
            
            {/* 세부 과목 목록 테이블 */}
            <Card>
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>세부 과목 목록 ({filteredDetailSubjects.length}개)</CardTitle>
              </CardHeader>
              <CardContent>
                {loading ? (
                  <div className="text-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-4"></div>
                    <p style={{ color: "#95A5A6" }}>세부과목 목록을 불러오는 중...</p>
                  </div>
                ) : error ? (
                  <div className="text-center py-8">
                    <BookOpen className="w-16 h-16 mx-auto mb-4" style={{ color: "#e74c3c" }} />
                    <h3 className="text-xl font-semibold mb-2" style={{ color: "#e74c3c" }}>
                      데이터 로딩 실패
                    </h3>
                    <p style={{ color: "#95A5A6" }}>{error}</p>
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b" style={{ borderColor: "#95A5A6" }}>
                          <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                            세부 과목명
                          </th>
                          <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                            세부과목 설명
                          </th>
                          <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                            관리
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredDetailSubjects.map((subject) => (
                          <tr key={subject.subDetailId} className="border-b hover:bg-gray-50" style={{ borderColor: "#f1f2f6" }}>
                            <td className="py-3 px-4">
                              <div>
                                <p className="font-medium" style={{ color: "#2C3E50" }}>
                                  {subject.subDetailName}
                                </p>
                              </div>
                            </td>
                            <td className="py-3 px-4">
                              <div>
                                <p className="text-xs mt-1" style={{ color: "#95A5A6" }}>
                                  {subject.subDetailInfo && subject.subDetailInfo.length > 30
                                    ? `${subject.subDetailInfo.substring(0, 30)}...`
                                    : subject.subDetailInfo}
                                </p>
                              </div>
                            </td>
                            <td className="py-3 px-4">
                              <div className="flex justify-center space-x-2">
                                <Button size="sm" variant="ghost" onClick={() => handleView(subject.subDetailId)} className="p-1 hover:bg-blue-50 hover:scale-110 transition-all duration-200">
                                  <Eye className="w-4 h-4" style={{ color: "#1ABC9C" }} />
                                </Button>
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={() => handleDelete(subject.subDetailId)}
                                  className="p-1 hover:bg-red-50 hover:scale-110 transition-all duration-200"
                                >
                                  <Trash2 className="w-4 h-4" style={{ color: "#e74c3c" }} />
                                </Button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    {/* 데이터 없을 때 안내 */}
                    {filteredDetailSubjects.length === 0 && !loading && !error && (
                      <div className="text-center py-8">
                        <BookOpen className="w-16 h-16 mx-auto mb-4" style={{ color: "#95A5A6" }} />
                        <h3 className="text-xl font-semibold mb-2" style={{ color: "#2C3E50" }}>
                          검색 결과가 없습니다
                        </h3>
                        <p style={{ color: "#95A5A6" }}>다른 검색어나 필터를 사용해보세요.</p>
                      </div>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
      {/* 세부 과목 상세보기/등록 모달 등은 아래에 위치 */}
      {showDetailModal && selectedSubject && (
        <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 max-w-xl w-full mx-4">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>
                세부 과목 상세 정보
              </h2>
              <Button variant="ghost" onClick={() => setShowDetailModal(false)} className="p-2">
                <X className="w-5 h-5" />
              </Button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-1 gap-1">
              {/* 기본 정보 */}
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50" }}>기본 정보</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <label className="text-sm font-medium" style={{ color: "#95A5A6" }}>
                      세부 과목명
                    </label>
                    <p className="text-lg font-medium" style={{ color: "#2C3E50" }}>
                      {selectedSubject.subDetailName}
                    </p>
                  </div>
                  <div>
                    <label className="text-sm font-medium" style={{ color: "#95A5A6" }}>
                      과목 설명
                    </label>
                    <p style={{ color: "#2C3E50" }}>{selectedSubject.subDetailInfo}</p>
                  </div>
                </CardContent>
              </Card>
            </div>
            <div className="flex justify-end space-x-3 mt-6">
              <Button
                variant="outline"
                onClick={() => setShowDetailModal(false)}
                style={{ borderColor: "#95A5A6", color: "#95A5A6" }}
              >
                닫기
              </Button>
              <Button
                onClick={() => {
                  setShowDetailModal(false)
                  handleEdit(selectedSubject.subDetailId)
                }}
                style={{ backgroundColor: "#1ABC9C", color: "white" }}
              >
                편집
              </Button>
            </div>
          </div>
        </div>
      )}
      {/* 세부 과목 생성/편집 통합 모달 */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold" style={{ color: "#2C3E50" }}>
                {isEditMode ? "세부 과목 편집" : "새 세부 과목 만들기"}
              </h2>
              <Button variant="ghost" onClick={handleCreateCancel} className="p-2">
                <X className="w-5 h-5" />
              </Button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-2" style={{ color: "#2C3E50" }}>
                  세부 과목명 * (최대 30자)
                </label>
                <Input
                  value={newSubDetailName}
                  onChange={(e) => {
                    if (e.target.value.length <= 30) {
                      setNewSubDetailName(e.target.value)
                    }
                  }}
                  placeholder="세부 과목명을 입력하세요"
                  className="w-full"
                  maxLength={30}
                />
                <div className="text-right mt-1">
                  <span className="text-xs" style={{ color: "#95A5A6" }}>
                    {newSubDetailName.length}/30
                  </span>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-2" style={{ color: "#2C3E50" }}>
                  설명 * (최대 100자)
                </label>
                <textarea
                  value={newSubDetailInfo}
                  onChange={(e) => {
                    if (e.target.value.length <= 100) {
                      setNewSubDetailInfo(e.target.value)
                    }
                  }}
                  placeholder="세부 과목에 대한 설명을 입력하세요"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md resize-none"
                  rows={4}
                  maxLength={100}
                  style={{ borderColor: "#95A5A6" }}
                />
                <div className="text-right mt-1">
                  <span className="text-xs" style={{ color: "#95A5A6" }}>
                    {newSubDetailInfo.length}/100
                  </span>
                </div>
              </div>
            </div>

            <div className="flex justify-end space-x-3 mt-6">
              <Button
                variant="outline"
                onClick={handleCreateCancel}
                className="text-[#95A5A5] border border-[#95A5A5] hover:!bg-gray-100
                hover:text-[#95A5A5]"
              >
                취소
              </Button>
              <Button onClick={handleCreateSubmit} className="text-white font-medium flex items-center space-x-2
              bg-[#1abc9c] hover:bg-[rgb(10,150,120)]">
                {isEditMode ? "수정" : "생성"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </PageLayout>
  )
}
