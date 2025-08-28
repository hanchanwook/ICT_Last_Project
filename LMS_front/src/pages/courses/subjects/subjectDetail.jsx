import { useState, useEffect } from "react"
import { ArrowLeft, Edit, BookOpen } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { useNavigate, useParams } from "react-router-dom"
import { getAllSubject } from "@/api/suhyeon/courseApi"
import { coursesMenuItems } from "@/components/ui/menuConfig"

export default function SubjectDetailPage() {
  const navigate = useNavigate()
  const { id: subjectId } = useParams()
  
  // 상태 관리
  const [subject, setSubject] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // API에서 과목 데이터 가져오기
  useEffect(() => {
    const fetchSubject = async () => {
      if (!subjectId) return
      
      try {
        setLoading(true)
        setError(null)
        
        const res = await getAllSubject()
        
        if (!res || !Array.isArray(res)) {
          setError("과목 데이터를 불러올 수 없습니다.")
          return
        }
        
        const foundSubject = res.find(s => s.subjectId === subjectId)
        
        if (foundSubject) {
          setSubject(foundSubject)
        } else {
          setError("과목을 찾을 수 없습니다.")
        }
      } catch (error) {
        setError("과목 데이터를 불러오는데 실패했습니다.")
      } finally {
        setLoading(false)
      }
    }

    fetchSubject()
  }, [subjectId])

const handleEdit = () => {
    navigate(`/courses/subjects/register?edit=${subjectId}`)
  }

  const handleBack = () => {
    navigate("/courses/subjects")
  }

  return (
    <PageLayout currentPage="courses">
      <div className="flex">
        <Sidebar title="과목 관리" menuItems={coursesMenuItems} currentPath="/courses/subjects" />

        <main className="flex-1 p-8">
          <div className="max-w-7xl mx-auto">
            {/* 로딩 상태 */}
            {loading && (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-4"></div>
                <p style={{ color: "#95A5A6" }}>과목 정보를 불러오는 중...</p>
              </div>
            )}

            {/* 에러 상태 */}
            {error && (
              <div className="text-center py-8">
                <BookOpen className="w-16 h-16 mx-auto mb-4" style={{ color: "#e74c3c" }} />
                <h3 className="text-xl font-semibold mb-2" style={{ color: "#e74c3c" }}>
                  데이터 로딩 실패
                </h3>
                <p style={{ color: "#95A5A6" }}>{error}</p>
                <Button
                  onClick={handleBack}
                  className="mt-4"
                  style={{ backgroundColor: "#1ABC9C", color: "white" }}
                >
                  목록으로 돌아가기
                </Button>
              </div>
            )}

            {/* 과목 상세 정보 */}
            {subject && !loading && !error && (
              <>
                {/* 헤더 */}
                <div className="flex items-center justify-between mb-8">
                  <div className="flex items-center space-x-4">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={handleBack}
                      className="flex items-center space-x-2"
                      style={{ color: "#95A5A6" }}
                    >
                      <ArrowLeft className="w-4 h-4" />
                      <span>목록으로</span>
                    </Button>
                    <div>
                      <h1 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>
                        {subject.subjectName}
                      </h1>
                      <p className="text-lg" style={{ color: "#95A5A6" }}>
                        과목 상세 정보
                      </p>
                    </div>
                  </div>
                  <Button
                    onClick={handleEdit}
                    className="text-white font-medium flex items-center space-x-2
                    bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                  >
                    <Edit className="w-4 h-4" />
                    <span>편집</span>
                  </Button>
                </div>

                <div className="space-y-6">
                    {/* 과목 소개 */}
                    <Card>
                      <CardHeader>
                        <CardTitle style={{ color: "#2C3E50" }}>과목 소개</CardTitle>                        
                        <div className="text-center mb-6">
                          <div
                            className="w-20 h-20 mx-auto mb-4 flex items-center justify-center rounded-full"
                            style={{ backgroundColor: "#f0f0f0" }}
                          >
                            <BookOpen className="w-10 h-10" style={{ color: "#1ABC9C" }} />
                          </div>
                          <h2 className="text-xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                            {subject.subjectName}
                          </h2>
                        </div>
                      </CardHeader>
                      <CardContent>
                        <p className="text-base leading-relaxed mb-6" style={{ color: "#2C3E50" }}>
                          {subject.subjectInfo}
                        </p>
                        <div className="text-center p-4 rounded-lg mb-6" style={{ backgroundColor: "#f8f9fa" }}>
                          <div className="text-2xl font-bold" style={{ color: "#1ABC9C" }}>
                            {subject.subDetails?.length || 0}
                          </div>
                          <div className="text-sm" style={{ color: "#95A5A6" }}>
                            세부과목
                          </div>
                        </div>
                      </CardContent>
                    </Card>

                    {/* 세부과목 */}
                    <Card>
                      <CardHeader>
                        <CardTitle style={{ color: "#2C3E50" }}>세부과목</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="space-y-4">
                          {subject.subDetails?.length > 0 ? (
                            subject.subDetails.map((subDetail, index) => (
                              <div
                                key={subDetail.subDetailId}
                                className="p-4 border rounded-lg"
                                style={{ borderColor: "#e0e0e0", backgroundColor: "#f8f9fa" }}
                              >
                                <div className="flex items-center space-x-3 mb-3">
                                  <div
                                    className="w-8 h-8 flex items-center justify-center rounded-full text-white text-sm font-medium"
                                    style={{ backgroundColor: "#1ABC9C" }}
                                  >
                                    {index + 1}
                                  </div>
                                  <h4 className="font-medium" style={{ color: "#2C3E50" }}>
                                    {subDetail.subDetailName}
                                  </h4>
                                </div>
                                <p className="text-sm ml-11" style={{ color: "#2C3E50" }}>
                                  {subDetail.subDetailInfo}
                                </p>
                              </div>
                            ))
                          ) : (
                            <div className="text-center py-8">
                              <p style={{ color: "#95A5A6" }}>등록된 세부과목이 없습니다.</p>
                            </div>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                </div>
              </>
            )}
          </div>
        </main>
      </div>
    </PageLayout>
  )
}
