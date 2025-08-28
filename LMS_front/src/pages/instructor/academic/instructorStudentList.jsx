import { useState, useEffect, useRef } from "react"
import PageLayout from "@/components/ui/page-layout"
import { Button } from "@/components/ui/button"
import Sidebar from "@/components/layout/sidebar"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Search, Users, BookOpen, TrendingUp, Calendar, Eye, ChevronLeft, ChevronRight, ChevronDown } from "lucide-react"
import { Link } from "react-router-dom"
import { getMenuItems } from "@/components/ui/menuConfig"
import {  
  getInstructorStudents, 
  getInstructorDashboard 
} from "@/api/hancw/instructorAcademicAxios"

export default function InstructorAcademicPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedCourse, setSelectedCourse] = useState("all")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [students, setStudents] = useState([])
  const [courses, setCourses] = useState([])
  const [dashboardStats, setDashboardStats] = useState({
    totalStudents: 0,
    activeStudents: 0,
    averageAttendance: 0,
    averageScore: 0
  })
  
  // 페이지네이션 상태 추가
  const [currentPage, setCurrentPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(20)
  const [isItemsPerPageOpen, setIsItemsPerPageOpen] = useState(false)
  const itemsPerPageRef = useRef(null)

  // 강사 전용 사이드바 메뉴
  const sidebarItems = getMenuItems('instructor-academic')

  // 데이터 로드
  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true)
        setError(null)
        
        // 현재 로그인한 사용자 정보 가져오기
        const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')
        const memberId = currentUser.memberId
        
        // 병렬로 데이터 로드
        const [studentsData, dashboardData] = await Promise.all([
          getInstructorStudents(memberId),
          getInstructorDashboard(memberId)
        ])
        
        // 디버깅을 위한 로그
        console.log('강사 학생 데이터:', studentsData)
        console.log('강사 대시보드 데이터:', dashboardData)
        
        // 데이터 안전성 검증 및 올바른 매핑
        
        // 학생 데이터는 students 필드 안에 있음
        const studentsList = studentsData && studentsData.students ? studentsData.students : []
        setStudents(studentsList)
        
        // 학생 데이터에서 고유한 과정 목록 추출
        console.log('학생 목록에서 과정 추출:', studentsList)
        const uniqueCourses = studentsList.reduce((acc, student) => {
          const courseId = student.courseId || student.courseCode || student.educationId
          const courseName = student.course || student.courseName
          
          // 과정이 있고, 이미 추가되지 않은 과정만 추가
          if (courseName && !acc.find(c => c.courseName === courseName)) {
            acc.push({ courseId: courseId || courseName, courseName })
          }
          return acc
        }, [])
        console.log('추출된 과정 목록:', uniqueCourses)
        setCourses(uniqueCourses)
        
        // 대시보드 데이터 매핑
        setDashboardStats({
          totalStudents: dashboardData?.totalStudents || 0,
          activeStudents: dashboardData?.totalStudents || 0, // activeStudents 필드가 없으므로 totalStudents 사용
          averageAttendance: dashboardData?.averageAttendanceRate || 0,
          averageScore: dashboardData?.averageGrade || 0
        })
      } catch (err) {
        console.error('데이터 로드 실패:', err)
        setError('데이터를 불러오는데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [])

  const filteredStudents = (Array.isArray(students) ? students : [])
    // 중복 제거 (userId 기준)
    .filter((student, index, self) => 
      index === self.findIndex(s => (s.userId || s.id) === (student.userId || student.id))
    )
    .filter((student) => {
      const matchesSearch =
        (student.memberName || student.name || student.studentName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (student.userId || student.id || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (student.course || student.courseName || '').toLowerCase().includes(searchTerm.toLowerCase())
      
      // 과정 필터링: selectedCourse가 "all"이거나 학생의 과정명과 일치하는 경우
      const studentCourseName = student.course || student.courseName
      const matchesCourse = selectedCourse === "all" || studentCourseName === selectedCourse
      
      console.log('필터링:', { 
        studentName: student.memberName || student.name || student.studentName, 
        studentCourseName, 
        selectedCourse, 
        matchesCourse 
      })
      
      return matchesSearch && matchesCourse
    })

  // 페이지네이션 로직
  const totalPages = Math.ceil(filteredStudents.length / itemsPerPage)
  const startIndex = (currentPage - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  const currentStudents = filteredStudents.slice(startIndex, endIndex)

  // 페이지 변경 함수
  const handlePageChange = (page) => {
    setCurrentPage(page)
  }

  // 검색어나 필터가 변경될 때 첫 페이지로 이동
  useEffect(() => {
    setCurrentPage(1)
  }, [searchTerm, selectedCourse, itemsPerPage])

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (itemsPerPageRef.current && !itemsPerPageRef.current.contains(event.target)) {
        setIsItemsPerPageOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const getAttendanceColor = (attendance) => {
    if (attendance >= 90) return "text-green-600"
    if (attendance >= 80) return "text-yellow-600"
    return "text-red-600"
  }

  const getScoreColor = (score) => {
    if (score >= 90) return "text-green-600"
    if (score >= 80) return "text-blue-600"
    if (score >= 70) return "text-yellow-600"
    return "text-red-600"
  }

  // 요일 정렬 함수
  const sortWeekdays = (weekdays) => {
    if (!weekdays) return '요일 정보 없음'
    
    const weekdayOrder = ['월', '화', '수', '목', '금', '토', '일']
    const days = weekdays.split(',').map(day => day.trim())
    
    return days
      .sort((a, b) => weekdayOrder.indexOf(a) - weekdayOrder.indexOf(b))
      .join(' ')
  }

  if (loading) {
    return (
      <PageLayout currentPage="academic" userRole="instructor">
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-600">데이터를 불러오는 중...</p>
          </div>
        </div>
      </PageLayout>
    )
  }
  if (error) {
    return (
      <PageLayout currentPage="academic" userRole="instructor">
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <p className="text-red-600 mb-4">{error}</p>
            <Button onClick={() => window.location.reload()}>다시 시도</Button>
          </div>
        </div>
      </PageLayout>
    )
  }
  return (
    <PageLayout currentPage="academic" userRole="instructor">
      <div className="flex">
        <Sidebar title="회원 정보" menuItems={sidebarItems} currentPath="/instructor/academic/students" />

        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
            {/* 페이지 헤더 */}
            <div className="mb-6">
              <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                담당 학생 관리
              </h1>
              <p className="text-gray-600">강사님이 담당하고 계신 학생들의 학습 현황을 관리하세요.</p>
            </div>


            {/* 검색 및 필터 */}
            <Card className="mb-6">
              <CardContent className="p-4">
                <div className="flex flex-col md:flex-row gap-4">
                  <div className="flex-1 relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                    <input
                      type="text"
                      placeholder="학생명, 학번, 과정으로 검색..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <select
                    value={selectedCourse}
                    onChange={(e) => setSelectedCourse(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="all">전체 과정</option>
                    {courses.map((course) => (
                      <option key={course.courseId} value={course.courseName}>
                        {course.courseName}
                      </option>
                    ))}
                  </select>

                  {/* 페이지당 항목 수 선택 */}
                  <div className="relative" ref={itemsPerPageRef}>
                    <button
                      onClick={() => setIsItemsPerPageOpen(!isItemsPerPageOpen)}
                      className="flex items-center justify-between px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white min-w-[120px]"
                    >
                      <span className="text-gray-700">
                        {itemsPerPage}개씩
                      </span>
                      <ChevronDown className={`w-4 h-4 text-gray-600 transition-transform ${isItemsPerPageOpen ? 'rotate-180' : ''}`} />
                    </button>
                    
                    {isItemsPerPageOpen && (
                      <div className="absolute top-full left-0 right-0 mt-1 bg-white border-2 border-gray-300 rounded-md shadow-lg z-10">
                        {[20, 50, 100].map((size, index) => (
                          <div 
                            key={size}
                            className={`px-3 py-2 hover:bg-blue-50 cursor-pointer ${index < 2 ? 'border-b border-gray-200' : ''}`}
                            onClick={() => {
                              setItemsPerPage(size)
                              setIsItemsPerPageOpen(false)
                            }}
                          >
                            {size}개씩
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 학생 목록 테이블 */}
            <Card>
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>
                  담당 학생 목록 ({filteredStudents.length}명)
                  {totalPages > 1 && (
                    <span className="text-sm font-normal text-gray-500 ml-2">
                      (페이지 {currentPage} / {totalPages})
                    </span>
                  )}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                          이름
                        </th>
                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                          이메일
                        </th>
                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                          과정
                        </th>
                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                          과정기간
                        </th>
                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                          수업요일
                        </th>
                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                          상세 보기
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {currentStudents.map((student, index) => (
                        <tr key={`${student.id || student.userId || 'unknown'}-${index}`} className="hover:bg-gray-50">
                          <td className="px-6 py-4 whitespace-nowrap text-center">
                            <div className="text-sm font-medium text-gray-900">
                              {student.memberName || student.name || student.studentName || '이름 없음'}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-center">
                            <div className="text-sm text-gray-500">
                              {student.memberEmail || student.email || '이메일 없음'}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-center">
                            <div className="text-sm font-medium text-gray-900">
                              {student.courseName || student.course || '과정명 없음'}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-center">
                            <div className="text-sm text-gray-500">
                              {student.courseStartDay || '시작일 없음'} ~ {student.courseEndDay || '종료일 없음'}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-center">
                            <div className="text-sm text-gray-500">
                              {sortWeekdays(student.courseDays)}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-center">
                            <div className="flex space-x-2">
                              <Link to={`/instructor/academic/students/${student.id || student.userId || 'unknown'}`}>
                                <Button size="sm" variant="outline" 
                                className="text-[#1abc9c] bg-transparent border border-[#1abc9c]
                                hover:bg-[#1abc9c] hover:text-white transition-all duration-200">
                                  상세보기
                                </Button>
                              </Link>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* 페이지네이션 UI */}
                {totalPages > 1 && (
                  <div className="flex items-center justify-between mt-6">
                    <div className="text-sm text-gray-700">
                      {startIndex + 1} - {Math.min(endIndex, filteredStudents.length)} / {filteredStudents.length}명
                    </div>
                    <div className="flex items-center space-x-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handlePageChange(currentPage - 1)}
                        disabled={currentPage === 1}
                        className="flex items-center space-x-1"
                      >
                        <ChevronLeft className="w-4 h-4" />
                        <span>이전</span>
                      </Button>
                      
                      {/* 페이지 번호 버튼들 */}
                      <div className="flex items-center space-x-1">
                        {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                          let pageNum
                          if (totalPages <= 5) {
                            pageNum = i + 1
                          } else if (currentPage <= 3) {
                            pageNum = i + 1
                          } else if (currentPage >= totalPages - 2) {
                            pageNum = totalPages - 4 + i
                          } else {
                            pageNum = currentPage - 2 + i
                          }
                          
                          return (
                            <Button
                              key={pageNum}
                              variant={currentPage === pageNum ? "default" : "outline"}
                              size="sm"
                              onClick={() => handlePageChange(pageNum)}
                              className="w-8 h-8 p-0"
                            >
                              {pageNum}
                            </Button>
                          )
                        })}
                      </div>
                      
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handlePageChange(currentPage + 1)}
                        disabled={currentPage === totalPages}
                        className="flex items-center space-x-1"
                      >
                        <span>다음</span>
                        <ChevronRight className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* 강사 안내사항 */}
            <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
              <h3 className="text-sm font-medium text-blue-800 mb-2">강사 학적부 안내사항</h3>
              <ul className="text-sm text-blue-700 space-y-1">
                <li>• 담당하고 계신 과정의 학생들만 표시됩니다.</li>
                <li>• 출석률이 80% 미만인 학생은 개별 관리가 필요합니다.</li>
                <li>• 과제 미제출 학생에게는 별도 안내를 해주세요.</li>
                <li>• 학생별 상세 정보는 상세보기 버튼을 통해 확인하세요.</li>
              </ul>
            </div>
          </div>
        </main>
      </div>
    </PageLayout>
  )
}
