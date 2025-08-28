import React, { useState, useEffect } from "react"
import { useLocation } from "react-router-dom"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { getMenuItems } from "@/components/ui/menuConfig"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Search, TrendingUp, Users, Award, BarChart3, Eye, BookOpen, Calendar, X, Layers, Layers2, Layers3 } from "lucide-react"
import { Link } from "react-router-dom"
import { getLecturesHistory, getLecturesStatistics, getFilteredLectures } from "@/api/kayoung/lectureHistoryApi"

export default function InstructorExamLecturesHistoryPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [lectures, setLectures] = useState([])
  const [statistics, setStatistics] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const location = useLocation()
  const sidebarMenuItems = getMenuItems('instructor-exam')
  const currentPath = location.pathname

  // 데이터 로딩 함수
  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      // 과정 이력과 통계를 병렬로 로드
      const [lecturesResponse, statisticsResponse] = await Promise.all([
        getLecturesHistory(),
        getLecturesStatistics()
      ])
      
      // 응답 데이터 처리 (이미 API에서 올바른 구조로 반환됨)
      const lecturesData = lecturesResponse || []
      const statisticsData = statisticsResponse || {}
      
      setLectures(lecturesData)
      setStatistics(statisticsData)
      
    } catch (err) {
      setError('데이터를 불러오는 데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  // 컴포넌트 마운트 시 데이터 로드
  useEffect(() => {
    loadData()
  }, [])

  // 필터링된 데이터 로드
  const loadFilteredData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      const response = await getFilteredLectures(searchTerm, selectedStatus)
      const filteredData = response?.data || response || []
      
      setLectures(filteredData)
      
    } catch (err) {
      console.error('필터링된 데이터 로딩 실패:', err)
      setError('필터링된 데이터를 불러오는 데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  // 검색어나 상태가 변경될 때 필터링된 데이터 로드
  useEffect(() => {
    if (searchTerm || selectedStatus !== 'all') {
      loadFilteredData()
    } else {
      loadData()
    }
  }, [searchTerm, selectedStatus])





  const getStatusColor = (status) => {
    switch (status) {
      case "진행중":
        return "bg-blue-100 text-blue-800"
      case "완료":
        return "bg-green-100 text-green-800"
      case "예정":
        return "bg-yellow-100 text-yellow-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const getScoreColor = (score) => {
    if (score >= 90) return "text-indigo-700" // 탁월
    if (score >= 80) return "text-blue-600" // 우수
    if (score >= 70) return "text-green-600" // 양호
    if (score >= 60) return "text-yellow-600" // 보통
    return "text-red-600" // 노력 필요
  }

  // 시험 상태 판단 함수
  const getExamStatus = (templateOpen, templateClosed) => {
    const now = new Date()
    
    if (!templateOpen) {
      return "예정"
    }
    
    const openTime = new Date(templateOpen)
    
    if (!templateClosed) {
      return "진행중"
    }
    
    const closedTime = new Date(templateClosed)
    
    if (now < openTime) {
      return "예정"
    } else if (now >= openTime && now <= closedTime) {
      return "진행중"
    } else {
      return "완료"
    }
  }

  // 시험 상태에 따른 색상 반환
  const getExamStatusColor = (status) => {
    switch (status) {
      case "예정":
        return "bg-yellow-100 text-yellow-800"
      case "진행중":
        return "bg-blue-100 text-blue-800"
      case "완료":
        return "bg-green-100 text-green-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }



  // 상태별 정렬 순서 정의
  const statusOrder = { "진행중": 1, "예정": 2, "완료": 3 }
  
  // 과정을 상태별로 정렬 (진행중 -> 예정 -> 완료 순)
  const sortedLectures = [...lectures].sort((a, b) => {
    const statusA = statusOrder[a.status] || 999
    const statusB = statusOrder[b.status] || 999
    return statusA - statusB
  })

  // 전체 통계 계산 (백엔드 데이터 우선, 없으면 클라이언트 계산)
  const totalLectures = statistics?.totalLectures || lectures.length
  const completedLectures = statistics?.completedLectures || lectures.filter((l) => l.status === "완료").length
  const totalStudents = statistics?.totalStudents || lectures.reduce((sum, l) => sum + l.totalStudents, 0)

  // 로딩 중이거나 에러 상태일 때 표시할 내용
  if (loading) {
    return (
      <React.Fragment>
        <PageLayout currentPage="exam" userRole="instructor">
          <div className="flex">
            <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="max-w-7xl mx-auto">
                <div className="animate-pulse space-y-6">
                  <div className="h-8 bg-gray-200 rounded w-1/4"></div>
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    {[...Array(4)].map((_, i) => (
                      <div key={i} className="bg-white p-4 rounded-lg shadow-sm">
                        <div className="space-y-2">
                          <div className="h-4 bg-gray-200 rounded w-20"></div>
                          <div className="h-6 bg-gray-200 rounded w-12"></div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </main>
          </div>
        </PageLayout>
      </React.Fragment>
    )
  }

  if (error) {
    return (
      <React.Fragment>
        <PageLayout currentPage="exam" userRole="instructor">
          <div className="flex">
            <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="max-w-7xl mx-auto">
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                      <X className="w-5 h-5 text-red-600" />
                    </div>
                    <h3 className="text-lg font-semibold text-red-800">데이터 로드 오류</h3>
                  </div>
                  <p className="text-red-700 mb-3">과정의 이력을 불러오는 중 오류가 발생했습니다.</p>
                  <p className="text-red-600 text-sm mb-4">{error}</p>
                  <div className="flex gap-3">
                    <Button 
                      onClick={() => window.location.reload()} 
                      className="bg-red-600 hover:bg-red-700"
                    >
                      페이지 새로고침
                    </Button>
                    <Button 
                      variant="outline" 
                      onClick={() => loadData()}
                      className="bg-transparent"
                    >
                      다시 시도
                    </Button>
                  </div>
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
      <PageLayout currentPage="exam" userRole="instructor">
      <div className="flex">
          <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
            {/* 페이지 헤더 */}
            <div className="mb-6">
              <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                담당 과정 이력
              </h1>
              <p className="text-gray-600">과거부터 현재까지 담당하신 모든 과정의 성과를 확인하세요.</p>
            </div>

            {/* 전체 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
              <Card className="relative overflow-hidden border-0 shadow-sm">
                <div className="absolute left-0 top-0 bottom-0 w-1 "></div>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600">총 과정 수</p>
                      <p className="text-3xl font-bold text-[#3498db]">
                        {totalLectures}개
                      </p>
                    </div>
                    <div className="bg-[#EFF6FF] rounded-full p-2 mr-3">
                      <Layers3 className="w-10 h-10 text-[#3498db]" />
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card className="relative overflow-hidden border-0 shadow-sm">
                <div className="absolute left-0 top-0 bottom-0 w-1 "></div>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600">완료 과정</p>
                      <p className="text-3xl font-bold text-[#1abc9c]">
                        {completedLectures}개
                      </p>
                    </div>
                    <div className="bg-[#e4f5eb] rounded-full p-2 mr-3">
                      <Award className="w-10 h-10 text-[#1abc9c]" />
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card className="relative overflow-hidden border-0 shadow-sm">
                <div className="absolute left-0 top-0 bottom-0 w-1"></div>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600">총 수강생</p>
                      <p className="text-3xl font-bold text-[#b0c4de]">
                        {totalStudents}명
                      </p>
                    </div>
                    <div className="bg-[#eff6ff] rounded-full p-2 mr-3">
                      <Users className="w-10 h-10 text-[#b0c4de]" />
                    </div>
                  </div>
                </CardContent>
              </Card>

              
            </div>

            {/* 검색 및 필터 */}
            <Card className="mb-6 py-3 mb-3 border-0 shadow-sm">
              <CardContent className="p-4">
                <div className="flex flex-col md:flex-row gap-4">
                  <div className="flex-1 relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-3" />
                    <input
                      type="text"
                      placeholder="과정명, 과정코드, 분야로 검색..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="w-full pl-10 pr-4 py-1.5 border border-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>
                  
                  <select
                    value={selectedStatus}
                    onChange={(e) => setSelectedStatus(e.target.value)}
                    className="px-3 py-1.5 border border-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="all">전체 상태</option>
                    <option value="완료">완료</option>
                    <option value="진행중">진행중</option>
                    <option value="예정">예정</option>
                  </select>
                </div>
              </CardContent>
            </Card>

            {/* 과정 목록 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {sortedLectures.map((lecture) => (
                <Card key={lecture.id} className="hover:shadow-lg transition-shadow border-0 shadow-sm">
                  <CardHeader>
                    <div className="flex justify-between items-start">
                      <div>
                        <CardTitle className="text-lg" style={{ color: "#2C3E50" }}>
                          {lecture.title}
                        </CardTitle>
                        <p className="text-sm text-gray-600 mt-1">
                          {lecture.code} | {lecture.year}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <span
                          className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(lecture.status)}`}
                        >
                          {lecture.status}
                        </span>

                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                                             {/* 기간 및 진행률 */}
                       <div className="flex items-center gap-2 text-sm text-gray-600">
                         <Calendar className="w-4 h-4" />
                         <span>{lecture.courseStartDay} - {lecture.courseEndDay}</span>
                       </div>

                      {/* 핵심 성과 지표 */}
                      <div className="grid grid-cols-2 gap-4">
                        <div className="bg-blue-50 p-3 rounded-lg">
                          <div className="flex items-center justify-between">
                            <span className="text-sm text-gray-600">평균 성적</span>
                            <TrendingUp className="w-4 h-4 text-blue-600" />
                          </div>
                          <div className={`text-xl font-bold ${getScoreColor(lecture.averageScore || 0)}`}>
                            {(lecture.averageScore || 0).toFixed(2)}점
                          </div>
                        </div>
                        <div className="bg-green-50 p-3 rounded-lg">
                          <div className="flex items-center justify-between">
                            <span className="text-sm text-gray-600">합격률</span>
                            <Award className="w-4 h-4 text-green-600" />
                          </div>
                          <div className="text-xl font-bold text-green-600">{lecture.passRate || 0}%</div>
                        </div>
                      </div>

                      {/* 성적 분포 */}
                      <div>
                        <div className="flex justify-between items-center mb-2">
                          <span className="text-sm font-medium text-gray-700">성적 분포</span>
                          <span className="text-xs text-gray-500">총 {lecture.totalStudents}명</span>
                        </div>
                        <div className="flex items-end justify-between h-16 gap-1">
                          <div className="flex flex-col items-center flex-1">
                            <div className="w-full bg-gray-200 rounded-t" style={{ height: '1px' }}>
                              <div
                                className="bg-indigo-700 rounded-t transition-all duration-300"
                                style={{
                                  height: `${(lecture.scoreDistribution.outstanding / lecture.totalStudents) * 100}%`,
                                  minHeight: '2px'
                                }}
                                title={`탁월(90점 이상): ${lecture.scoreDistribution.outstanding}명`}
                              />
                            </div>
                            <div className="text-xs text-indigo-700 font-semibold mt-1">
                              {lecture.scoreDistribution.outstanding}
                            </div>
                          </div>
                          
                          <div className="flex flex-col items-center flex-1">
                            <div className="w-full bg-gray-200 rounded-t" style={{ height: '1px' }}>
                              <div
                                className="bg-blue-500 rounded-t transition-all duration-300"
                                style={{
                                  height: `${(lecture.scoreDistribution.excellent / lecture.totalStudents) * 100}%`,
                                  minHeight: '2px'
                                }}
                                title={`우수(80-89점): ${lecture.scoreDistribution.excellent}명`}
                              />
                            </div>
                            <div className="text-xs text-blue-600 font-semibold mt-1">
                              {lecture.scoreDistribution.excellent}
                            </div>
                          </div>
                          
                          <div className="flex flex-col items-center flex-1">
                            <div className="w-full bg-gray-200 rounded-t" style={{ height: '1px' }}>
                              <div
                                className="bg-green-500 rounded-t transition-all duration-300"
                                style={{
                                  height: `${(lecture.scoreDistribution.good / lecture.totalStudents) * 100}%`,
                                  minHeight: '2px'
                                }}
                                title={`양호(70-79점): ${lecture.scoreDistribution.good}명`}
                              />
                            </div>
                            <div className="text-xs text-green-600 font-semibold mt-1">
                              {lecture.scoreDistribution.good}
                            </div>
                          </div>
                          
                          <div className="flex flex-col items-center flex-1">
                            <div className="w-full bg-gray-200 rounded-t" style={{ height: '1px' }}>
                              <div
                                className="bg-yellow-500 rounded-t transition-all duration-300"
                                style={{
                                  height: `${(lecture.scoreDistribution.average / lecture.totalStudents) * 100}%`,
                                  minHeight: '2px'
                                }}
                                title={`보통(60-69점): ${lecture.scoreDistribution.average}명`}
                              />
                            </div>
                            <div className="text-xs text-yellow-600 font-semibold mt-1">
                              {lecture.scoreDistribution.average}
                            </div>
                          </div>
                          
                          <div className="flex flex-col items-center flex-1">
                            <div className="w-full bg-gray-200 rounded-t" style={{ height: '1px' }}>
                              <div
                                className="bg-red-500 rounded-t transition-all duration-300"
                                style={{
                                  height: `${(lecture.scoreDistribution.needsImprovement / lecture.totalStudents) * 100}%`,
                                  minHeight: '2px'
                                }}
                                title={`노력 필요(60점 미만): ${lecture.scoreDistribution.needsImprovement}명`}
                              />
                            </div>
                            <div className="text-xs text-red-600 font-semibold mt-1">
                              {lecture.scoreDistribution.needsImprovement}
                            </div>
                          </div>
                        </div>
                        <div className="flex justify-between text-xs text-gray-500 mt-1">
                          <span>탁월</span>
                          <span>우수</span>
                          <span>양호</span>
                          <span>보통</span>
                          <span>노력</span>
                        </div>
                                             </div>

                       {/* 과목별 시험 현황 */}
                       {lecture.classes && lecture.classes.length > 0 && (
                         <div>
                           <div className="flex justify-between items-center mb-3">
                             <span className="text-sm font-medium text-gray-700">과목별 시험 현황</span>
                             <span className="text-xs text-gray-500">총 {lecture.classes.length}개 과목</span>
                           </div>
                           <div className="space-y-2">
                             {lecture.classes.map((classItem, index) => {
                               const examStatus = getExamStatus(classItem.templateOpen, classItem.templateClosed)
                               return (
                                 <div key={classItem.classId || index} className="flex items-center justify-between p-2 bg-gray-50 rounded-lg">
                                   <div className="flex items-center gap-3">
                                     <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                                     <div>
                                       <p className="text-sm font-medium text-gray-900">{classItem.templateName || classItem.className}</p>
                                       <p className="text-xs text-gray-500">{classItem.className}</p>
                                     </div>
                                   </div>
                                   <div className="flex items-center gap-2">
                                     <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getExamStatusColor(examStatus)}`}>
                                       {examStatus}
                                     </span>
                                     {classItem.attendanceCount !== undefined && (
                                       <span className="text-xs text-gray-500">
                                         출석: {classItem.attendanceCount}명
                                       </span>
                                     )}
                                   </div>
                                 </div>
                               )
                             })}
                           </div>
                         </div>
                       )}

                       {/* 액션 버튼 */}
                      <div className="flex gap-2 pt-2">
                        <Link to={`/instructor/exam/lectures/history/${lecture.id}`} className="flex-1">
                          <Button size="sm" variant="outline" 
                          className="w-full bg-transparent text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white">
                            <Eye className="w-4 h-4 mr-2" />
                            상세보기
                          </Button>
                        </Link>
                        {/* <Button size="sm" className="bg-blue-600 hover:bg-blue-700 text-white">
                          <BarChart3 className="w-4 h-4 mr-2" />
                          성적 분석
                        </Button> */}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>

            {/* 검색 결과가 없을 때 */}
            {sortedLectures.length === 0 && (
              <Card className="hover:shadow-lg transition-shadow border-0 shadow-sm">
                <CardContent className="p-8 text-center">
                  <BookOpen className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">검색 결과가 없습니다</h3>
                  <p className="text-gray-600">다른 검색 조건을 시도해보세요.</p>
                </CardContent>
              </Card>
            )}

            {/* 강사 안내사항 */}
            <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
              <h3 className="text-sm font-medium text-blue-800 mb-2">과정 이력 안내사항</h3>
              <ul className="text-sm text-blue-700 space-y-1">
                <li>• 평균 성적은 해당 과정의 모든 학생들의 최종 성적을 기준으로 계산됩니다.</li>
                <li>• 성적 분포는 탁월(90점 이상), 우수(80-89점), 양호(70-79점), 보통(60-69점), 노력 필요(60점 미만)로 구분됩니다.</li>
                <li>• 합격률은 70점 이상을 받은 학생의 비율입니다.</li>
                <li>• 상세보기를 통해 개별 학생의 성적과 출석 현황을 확인할 수 있습니다.</li>
              </ul>
            </div>
          </div>
        </main>
      </div>
      </PageLayout>
    </React.Fragment>
  )
}
