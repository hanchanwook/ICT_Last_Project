import React, { useState, useEffect } from "react"
import { useParams, useLocation } from "react-router-dom"
import Sidebar from "@/components/layout/sidebar"
import PageLayout from "@/components/ui/page-layout"
import { getMenuItems } from "@/components/ui/menuConfig"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ArrowLeft, TrendingUp, Users, Award, BarChart3, Calendar, Download, Search, Eye, EyeOff, FileText, X, ClipboardList } from "lucide-react"
import { getLectureDetail } from "@/api/kayoung/lectureHistoryApi"

export default function InstructorExamLectureDetailPage() {
  const { id: lectureId } = useParams()

  const [searchTerm, setSearchTerm] = useState("")
  const [sortBy, setSortBy] = useState("name")
  const [lectureDetail, setLectureDetail] = useState(null)
  const [students, setStudents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const location = useLocation()
  const sidebarMenuItems = getMenuItems('instructor-exam')
  const currentPath = location.pathname

  // 데이터 로딩 함수
  const loadLectureDetail = async () => {
    try {
      setLoading(true)
      setError(null)
      
      const response = await getLectureDetail(lectureId)
      
      if (response) {
        // 백엔드 응답 구조에 맞게 처리
        setLectureDetail(response)
        setStudents(response.students || [])
      } else {
        setError('강의 정보를 찾을 수 없습니다.')
      }
      
    } catch (err) {
      console.error('강의 상세 정보 로딩 실패:', err)
      setError('강의 정보를 불러오는 데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  // 성적 분포 계산 함수
  const calculateScoreDistribution = (students) => {
    const distribution = {
      outstanding: 0,    // 90점 이상
      excellent: 0,      // 80-89점
      good: 0,          // 70-79점
      average: 0,       // 60-69점
      needsImprovement: 0 // 60점 미만
    }

    students.forEach(student => {
      if (student.score !== null && student.score !== undefined) {
        if (student.score >= 90) {
          distribution.outstanding++
        } else if (student.score >= 80) {
          distribution.excellent++
        } else if (student.score >= 70) {
          distribution.good++
        } else if (student.score >= 60) {
          distribution.average++
        } else {
          distribution.needsImprovement++
        }
      }
    })

    return distribution
  }

  // 평균 점수 계산 함수
  const calculateAverageScore = (students) => {
    const validScores = students
      .filter(student => student.score !== null && student.score !== undefined)
      .map(student => student.score)
    
    if (validScores.length === 0) return 0
    
    const sum = validScores.reduce((acc, score) => acc + score, 0)
    return Math.round(sum / validScores.length)
  }

  // 합격률 계산 함수 (60점 이상을 합격으로 간주)
  const calculatePassRate = (students) => {
    const validScores = students.filter(student => 
      student.score !== null && student.score !== undefined
    )
    
    if (validScores.length === 0) return 0
    
    const passCount = validScores.filter(student => student.score >= 60).length
    return Math.round((passCount / validScores.length) * 100)
  }

  // 필터링된 학생 목록
  const filteredStudents = students.filter(student =>
    student.studentName.toLowerCase().includes(searchTerm.toLowerCase())
  ).sort((a, b) => {
    if (sortBy === "name") {
      return a.studentName.localeCompare(b.studentName)
    } else if (sortBy === "score") {
      const scoreA = a.score || 0
      const scoreB = b.score || 0
      return scoreB - scoreA
    }
    return 0
  })

  // 성적 분포 계산
  const scoreDistribution = calculateScoreDistribution(students)
  
  // 평균 점수 계산
  const averageScore = calculateAverageScore(students)
  
  // 합격률 계산
  const passRate = calculatePassRate(students)

  // 컴포넌트 마운트 시 데이터 로드
  useEffect(() => {
    if (lectureId) {
      loadLectureDetail()
    }
  }, [lectureId])

  // 로딩 중일 때 표시할 내용
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

  // 에러 상태일 때 표시할 내용
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
                  <p className="text-red-700 mb-3">강의 상세 정보를 불러오는 중 오류가 발생했습니다.</p>
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
                      onClick={() => loadLectureDetail()}
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

  // 데이터가 없을 때
  if (!lectureDetail) {
    return (
      <React.Fragment>
        <PageLayout currentPage="exam" userRole="instructor">
          <div className="flex">
            <Sidebar title="시험 및 성적 관리" menuItems={sidebarMenuItems} currentPath={currentPath} />
            <main className="flex-1 p-6">
              <div className="max-w-7xl mx-auto">
                <div className="text-center py-8">
                  <FileText className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">강의 정보를 찾을 수 없습니다</h3>
                  <p className="text-gray-600">요청하신 강의 정보가 존재하지 않습니다.</p>
                </div>
              </div>
            </main>
          </div>
        </PageLayout>
      </React.Fragment>
    )
  }





  const getGradeColor = (grade) => {
    switch (grade) {
      case "탁월":
        return "bg-indigo-100 text-indigo-800"
      case "우수":
        return "bg-blue-100 text-blue-800"
      case "양호":
        return "bg-green-100 text-green-800"
      case "보통":
        return "bg-yellow-100 text-yellow-800"
      case "노력":
        return "bg-red-100 text-red-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const getScoreColor = (score) => {
    if (score >= 90) return "text-indigo-700" // 탁월
    if (score >= 80) return "text-blue-600" // 우수
    if (score >= 70) return "text-green-600" // 양호
    if (score >= 60) return "text-yellow-600" // 보통
    return "text-red-600" // 노력
  }

  // 시험 상태를 결정하는 함수
  const getExamStatus = (templateOpen, templateClosed) => {
    const now = new Date()
    
    // templateOpen이 null인 경우
    if (!templateOpen) {
      return "시험 예정"
    }
    
    // templateOpen을 한국 시간으로 변환
    const openDate = new Date(templateOpen)
    const koreanOpenDate = new Date(openDate.getTime() + 9 * 60 * 60 * 1000)
    
    // 현재 시간이 templateOpen보다 이전인 경우
    if (now < koreanOpenDate) {
      return "시험 예정"
    }
    
    // templateClosed가 null이거나 현재 시간이 templateOpen과 templateClosed 사이에 있는 경우
    if (!templateClosed) {
      return "진행중"
    }
    
    // templateClosed를 안전하게 Date 객체로 변환
    const closedDate = new Date(templateClosed)
    const koreanClosedDate = new Date(closedDate.getTime() + 9 * 60 * 60 * 1000)
    
    // 현재 시간이 templateClosed보다 이후인 경우
    if (now > koreanClosedDate) {
      return "시험 완료"
    }
    
    // 현재 시간이 templateOpen과 templateClosed 사이에 있는 경우
    if (now >= koreanOpenDate && now <= koreanClosedDate) {
      return "진행중"
    }
    
    return "시험 예정"
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
                <div className="flex items-center gap-4 mb-4">
                  <Button variant="outline" size="sm" onClick={() => window.history.back()}>
                    <ArrowLeft className="w-4 h-4 mr-2" />
                    목록으로
                  </Button>
                  <div>
                    <h1 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>
                      {lectureDetail.title} 성적 상세
                    </h1>
                    <p className="text-gray-600">
                      {lectureDetail.code} | {lectureDetail.courseStartDay} - {lectureDetail.courseEndDay}
                    </p>
                  </div>
                </div>
              </div>

              {/* 핵심 성과 지표 */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                <Card className="border-0">
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                        <div>
                         <p className="text-m font-medium text-gray-600">평균 성적</p>
                         <p className={`text-3xl font-bold ${getScoreColor(averageScore)}`} style={{ color: "#3498db" }}>
                           {averageScore}점
                         </p>
                         <p className="text-xs text-gray-500 mt-1">전체 {students.length}명</p>
                       </div>
                      <div className="bg-[#eff6ff] rounded-full p-2 mr-3">
                        <TrendingUp className="w-10 h-10 text-[#3498db]" />
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card className="border-0">
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                                             <div>
                         <p className="text-m font-medium text-gray-600">합격률</p>
                         <p className="text-3xl font-bold text-[#1abc9c]">{passRate}%</p>
                         <p className="text-xs text-gray-500 mt-1">
                           {Math.round((passRate / 100) * students.filter(s => s.score !== null && s.score !== undefined).length)}명 합격
                         </p>
                       </div>
                      <div className="bg-[#e4f5eb] rounded-full p-2 mr-3">
                        <Award className="w-10 h-10 text-[#1abc9c]" />
                      </div>
                    </div>
                  </CardContent>
                </Card>



                <Card className="border-0">
                  <CardContent className="p-6">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-m font-medium text-gray-600">시험 개수</p>
                        <p className="text-3xl font-bold text-[#b0c4de]">{lectureDetail.totalClasses}개</p>
                        <p className="text-xs text-gray-500 mt-1">총 시험 수</p>
                      </div>
                      <div className="bg-[#eff6ff] rounded-full p-2 mr-3">
                        <ClipboardList className="w-10 h-10 text-[#b0c4de]" />
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* 성적 분포 및 주차별 성과 */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                {/* 성적 분포 */}
                <Card className="border-0">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <BarChart3 className="w-5 h-5" />
                      성적 분포
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="flex items-end justify-between h-64 gap-2">
                      <div className="flex flex-col items-center flex-1">
                        <div className="text-xs text-gray-600 mb-1">탁월</div>
                        <div className="w-full bg-gray-200 rounded-t h-32 flex items-end">
                          <div
                            className="bg-indigo-700 rounded-t transition-all duration-300 w-full"
                            style={{
                              height: `${students.length > 0 ? (scoreDistribution.outstanding / students.length) * 100 : 0}%`,
                              minHeight: '4px'
                            }}
                          />
                        </div>
                        <div className="text-xs font-semibold text-indigo-700 mt-1">
                          {scoreDistribution.outstanding}명
                        </div>
                        <div className="text-xs text-gray-500">90점+</div>
                      </div>
                      
                      <div className="flex flex-col items-center flex-1">
                        <div className="text-xs text-gray-600 mb-1">우수</div>
                        <div className="w-full bg-gray-200 rounded-t h-32 flex items-end">
                          <div
                            className="bg-blue-500 rounded-t transition-all duration-300 w-full"
                            style={{
                              height: `${students.length > 0 ? (scoreDistribution.excellent / students.length) * 100 : 0}%`,
                              minHeight: '4px'
                            }}
                          />
                        </div>
                        <div className="text-xs font-semibold text-blue-600 mt-1">
                          {scoreDistribution.excellent}명
                        </div>
                        <div className="text-xs text-gray-500">80-89점</div>
                      </div>
                      
                      <div className="flex flex-col items-center flex-1">
                        <div className="text-xs text-gray-600 mb-1">양호</div>
                        <div className="w-full bg-gray-200 rounded-t h-32 flex items-end">
                          <div
                            className="bg-green-500 rounded-t transition-all duration-300 w-full"
                            style={{
                              height: `${students.length > 0 ? (scoreDistribution.good / students.length) * 100 : 0}%`,
                              minHeight: '4px'
                            }}
                          />
                        </div>
                        <div className="text-xs font-semibold text-green-600 mt-1">
                          {scoreDistribution.good}명
                        </div>
                        <div className="text-xs text-gray-500">70-79점</div>
                      </div>
                      
                      <div className="flex flex-col items-center flex-1">
                        <div className="text-xs text-gray-600 mb-1">보통</div>
                        <div className="w-full bg-gray-200 rounded-t h-32 flex items-end">
                          <div
                            className="bg-yellow-500 rounded-t transition-all duration-300 w-full"
                            style={{
                              height: `${students.length > 0 ? (scoreDistribution.average / students.length) * 100 : 0}%`,
                              minHeight: '4px'
                            }}
                          />
                        </div>
                        <div className="text-xs font-semibold text-yellow-600 mt-1">
                          {scoreDistribution.average}명
                        </div>
                        <div className="text-xs text-gray-500">60-69점</div>
                      </div>
                      
                      <div className="flex flex-col items-center flex-1">
                        <div className="text-xs text-gray-600 mb-1">노력</div>
                        <div className="w-full bg-gray-200 rounded-t h-32 flex items-end">
                          <div
                            className="bg-red-500 rounded-t transition-all duration-300 w-full"
                            style={{
                              height: `${students.length > 0 ? (scoreDistribution.needsImprovement / students.length) * 100 : 0}%`,
                              minHeight: '4px'
                            }}
                          />
                        </div>
                        <div className="text-xs font-semibold text-red-600 mt-1">
                          {scoreDistribution.needsImprovement}명
                        </div>
                        <div className="text-xs text-gray-500">60점 미만</div>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                {/* 수업별 현황 */}
                <Card className="border-0">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <FileText className="w-5 h-5" />
                      현 과정의 시험 현황
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      {lectureDetail.classes && lectureDetail.classes.map((classItem, index) => (
                        <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded">
                          <div className="flex-1">
                            <div className="text-sm font-medium text-gray-800 mb-1">{classItem.className}</div>
                          </div>
                          <div className="flex items-center gap-4">
                            <div className="text-right">
                              <div className="text-sm font-medium text-gray-700">
                                {getExamStatus(classItem.templateOpen, classItem.templateClosed)}
                              </div>
                            </div>
                          </div>
                        </div>
                      ))}
                      {(!lectureDetail.classes || lectureDetail.classes.length === 0) && (
                        <div className="text-center py-4 text-gray-500">
                          등록된 수업이 없습니다.
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* 학생별 성적 목록 */}
              <Card className="border-0">
                <CardHeader>
                  <div className="flex justify-between items-center">
                    <CardTitle className="flex items-center gap-2">
                      <Users className="w-5 h-5" />
                      학생별 성적 현황
                    </CardTitle>
                    {/* <div className="flex gap-2">
                      <Button size="sm" variant="outline">
                        <Download className="w-4 h-4 mr-2" />
                        엑셀 다운로드
                      </Button>
                      <Button size="sm" variant="outline">
                        <FileText className="w-4 h-4 mr-2" />
                        성적표 출력
                      </Button>
                    </div> */}
                  </div>
                </CardHeader>
                <CardContent>
                  {/* 검색 및 필터 */}
                  <div className="flex flex-col md:flex-row gap-4 mb-6">
                    <div className="flex-1 relative">
                      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                      <input
                        type="text"
                        placeholder="학생명으로 검색..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    
                    <select
                      value={sortBy}
                      onChange={(e) => setSortBy(e.target.value)}
                      className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="name">이름순</option>
                      <option value="score">성적순</option>
                      {/* <option value="passRate">출석률순</option> */}
                    </select>
                  </div>

                  {/* 학생 목록 테이블 */}
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b bg-gray-50">
                          <th className="text-left p-3 font-medium">학생정보</th>
                          <th className="text-left p-3 font-medium">성적</th>
                          <th className="text-left p-3 font-medium">상태</th>
                          <th className="text-left p-3 font-medium">등록일</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredStudents.map((student) => (
                          <tr key={student.studentId} className="border-b hover:bg-gray-50">
                              <td className="p-3">
                                <div>
                                <div className="font-medium">{student.studentName}</div>
                                </div>
                              </td>
                              <td className="p-3">
                              <div className={`text-lg font-bold ${student.status === "수강예정" || student.score === null ? "text-gray-400" : getScoreColor(student.score)}`}>
                                {student.status === "수강예정" || student.score === null ? "-" : `${student.score}점`}
                                </div>
                              </td>
                              <td className="p-3">
                              <span className="font-semibold text-green-600">{student.status}</span>
                              </td>
                              <td className="p-3">
                              <span className="font-semibold text-gray-600">
                                {new Date(student.enrolledAt).toLocaleDateString()}
                                          </span>
                                </td>
                              </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>



                  {/* 검색 결과가 없을 때 */}
                  {filteredStudents.length === 0 && (
                    <div className="text-center py-8">
                      <Users className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                      <h3 className="text-lg font-medium text-gray-900 mb-2">검색 결과가 없습니다</h3>
                      <p className="text-gray-600">다른 검색 조건을 시도해보세요.</p>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* 성적 분석 요약 */}
              <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 className="text-sm font-medium text-blue-800 mb-2">성적 분석 요약</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm text-blue-700">
                  <div>
                    <strong>최고 성적:</strong> {students.length > 0 ? Math.max(...students.map((s) => s.score)) : 0}점
                    <br />
                    <strong>최저 성적:</strong> {students.length > 0 ? Math.min(...students.map((s) => s.score)) : 0}점
                  </div>
                  <div>
                    {/* <strong>평균 출석률:</strong>{" "}
                    {students.length > 0 ? (students.reduce((sum, s) => sum + s.attendanceRate, 0) / students.length).toFixed(1) : 0}%
                    <br /> */}
                    <strong>완료 학생:</strong> {students.filter((s) => s.status === "완료").length}명
                  </div>
                  <div>
                    <strong>전체 학생:</strong> {students.length}명
                    <br />
                    <strong>강의 상태:</strong> {lectureDetail.status}
                  </div>
                </div>
              </div>
            </div>
          </main>
        </div>
      </PageLayout>
    </React.Fragment>
  )
}
