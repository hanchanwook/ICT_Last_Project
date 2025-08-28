import { useState, useEffect } from "react"
import { Search, Plus, Trash2, Eye, BookOpen } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { useNavigate } from "react-router-dom"
import { deleteSubject, getAllSubject } from "@/api/suhyeon/courseApi"
import { coursesMenuItems } from "@/components/ui/menuConfig"

export default function SubjectsListPage() {
  const [searchTerm, setSearchTerm] = useState("")

  const navigate = useNavigate()

  // 과목 데이터 상태
  const [subjectsData, setSubjectsData] = useState([])
  const [loading, setLoading] = useState(true)

  // API에서 과목 데이터 가져오기
  useEffect(() => {
    const fetchSubjects = async () => {
      try {
        setLoading(true)
        const data = await getAllSubject()
        setSubjectsData(data)
      } catch (error) {
        alert("과목 데이터를 불러오는데 실패했습니다.")
      } finally {
        setLoading(false)
      }
    }
    fetchSubjects()
  }, [])

  // 검색어에 따른 필터링된 과목 데이터를 반환하는 함수
  const getFilteredSubjects = () => {
    return subjectsData
      .filter((subject) => {
        const matchesSearch =
          (subject.subjectName && subject.subjectName.toLowerCase().includes(searchTerm.toLowerCase())) ||
          (subject.subjectId && subject.subjectId.includes(searchTerm)) ||
          (subject.subjectInfo && subject.subjectInfo.toLowerCase().includes(searchTerm.toLowerCase()))

        return matchesSearch
      })
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)) // 최신등록순 정렬 (createdAt 내림차순)
  }

  // 필터링된 과목 데이터 (최신등록순 정렬)
  const filteredSubjects = getFilteredSubjects()

  const handleEdit = (subjectId) => {
    // 과목 등록 페이지로 이동하여 수정 모드로 동작
    navigate(`/courses/subjects/register?edit=${subjectId}`)
  }

  const handleDelete = async (subjectId) => {
    if (confirm("정말로 이 과목을 삭제하시겠습니까?")) {
      try {
        await deleteSubject(subjectId)
        alert(`과목이 삭제되었습니다.`)
        const updatedData = await getAllSubject()
        setSubjectsData(updatedData)
      } catch (error) {
        alert("과목 삭제에 실패했습니다.")
      }
    }
  }

  const handleView = (subjectId) => {
    navigate(`/courses/subjects/${subjectId}`)
  }

  // 과목 통계 계산
  const stats = {
    total: subjectsData.length,
  }

  return (
    <PageLayout currentPage="courses">
      <div className="flex">
        <Sidebar title="과정 관리" menuItems={coursesMenuItems} currentPath="/courses/subjects" />
        <main className="flex-1 p-8">
          <div className="max-w-7xl mx-auto">
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                전체 과목 목록
              </h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                등록된 모든 과목의 정보를 조회하고 관리할 수 있습니다.
              </p>
            </div>

            {/* 과목 통계 */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#3498db" }}>
                    {stats.total}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    전체 과목
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* 검색 및 필터 */}
            <Card className="mb-6">
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>검색 및 필터</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-col md:flex-row gap-4">
                  <div className="flex-1">
                    <div className="relative">
                      <Search
                        className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4"
                        style={{ color: "#95A5A6" }}
                      />
                      <Input
                        placeholder="과목명, 세부과목명으로 검색..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="pl-10"
                      />
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      className="text-[#1ABC9C] border border-[#1ABC9C] font-medium flex items-center space-x-2 hover:bg-[#1ABC9C] hover:text-white"
                      onClick={() => navigate("/courses/subjects/register")}
                    >
                      <Plus className="w-4 h-4" />
                      <span>과목 추가</span>
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            

            {/* 로딩 상태 표시 */}
            {loading && (
              <Card className="mb-6">
                <CardContent className="p-8 text-center">
                  <div className="text-lg" style={{ color: "#95A5A6" }}>
                    과목 데이터를 불러오는 중...
                  </div>
                </CardContent>
              </Card>
            )}

            {/* 과목 목록 테이블 */}
            <Card>
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>과목 목록 ({filteredSubjects.length}개)</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b" style={{ borderColor: "#95A5A6" }}>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          과목명
                        </th>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          세부과목
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          사용 과정
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          관리
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredSubjects.map((subject) => (
                        <tr key={subject.subjectId} className="border-b hover:bg-gray-50" style={{ borderColor: "#f1f2f6" }}>
                          <td className="py-3 px-4">
                            <div>
                              <p className="font-medium" style={{ color: "#2C3E50" }}>
                                {subject.subjectName}
                              </p>
                              <p className="text-sm" style={{ color: "#95A5A6" }}>
                                {subject.subjectInfo}
                              </p>
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <div>
                              {subject.subDetails && subject.subDetails.length > 0 
                                ? (
                                  <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                    {subject.subDetails.map((subDetail, index) => (
                                      <span key={subDetail.subDetailId}>
                                        {subDetail.subDetailName}
                                        {index < subject.subDetails.length - 1 ? ", " : ""}
                                      </span>
                                    ))}
                                  </p>
                                )
                                : <p className="text-sm" style={{ color: "#95A5A6" }}>세부과목 없음</p>
                              }
                            </div>
                          </td>
                              <td className="py-3 px-4 text-center">
                              <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                {subject.useSubject}
                              </span>
                            </td>
                          <td className="py-3 px-4">
                            {/* 과목 관리 버튼 그룹 */}
                            <div className="flex justify-center space-x-2">
                              {/* 과목 상세보기 버튼 - Eye 아이콘으로 표시 */}
                              <Button size="sm" variant="ghost" onClick={() => handleView(subject.subjectId)} className="p-1 hover:bg-blue-50 hover:scale-110 transition-all duration-200">
                                <Eye className="w-4 h-4" style={{ color: "#1ABC9C" }} />
                              </Button>
                              {/* 과목 삭제 버튼 - Trash2 아이콘으로 표시 */}
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => handleDelete(subject.subjectId)}
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

                  {filteredSubjects.length === 0 && (
                    <div className="text-center py-8">
                      <BookOpen className="w-16 h-16 mx-auto mb-4" style={{ color: "#95A5A6" }} />
                      <h3 className="text-xl font-semibold mb-2" style={{ color: "#2C3E50" }}>
                        검색 결과가 없습니다
                      </h3>
                      <p style={{ color: "#95A5A6" }}>다른 검색어나 필터를 사용해보세요.</p>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
    </PageLayout>
  )
}
