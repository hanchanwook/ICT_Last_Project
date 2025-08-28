import { useState, useEffect } from "react"
import { Search, Trash2, Eye, BookOpen, Download } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { useNavigate } from "react-router-dom"
import { getAllCourse, deleteCourse } from "@/api/suhyeon/courseApi"
import { coursesMenuItems } from "@/components/ui/menuConfig"
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

/**
 * 과정 목록 페이지
 * 등록된 모든 과정을 조회하고 관리할 수 있는 페이지
 */
export default function CoursesListPage() {
  // 상태 관리
  const [searchTerm, setSearchTerm] = useState("") // 검색어
  const [selectedStatus, setSelectedStatus] = useState("all") // 선택된 상태 필터
  const [coursesData, setCoursesData] = useState([]) // 과정 데이터
  const [loading, setLoading] = useState(true) // 로딩 상태

  const navigate = useNavigate()

  /**
   * 날짜를 기준으로 과정 상태를 계산하는 함수
   * @param {Object} course - 과정 정보 객체
   * @returns {string} 과정 상태 ("완료", "진행중", "모집중")
   */
  const calculateCourseStatus = (course) => {
    const today = new Date()
    const startDate = new Date(course.courseStartDay)
    const endDate = new Date(course.courseEndDay)
    
    // 날짜만 비교하기 위해 시간 정보 제거
    const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    const startDateOnly = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate())
    const endDateOnly = new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate())
    
    // 오늘이 종료일인 경우도 진행중으로 처리
    if (todayDate > endDateOnly) return "완료"
    if (todayDate >= startDateOnly && todayDate <= endDateOnly) return "진행중"
    if (todayDate < startDateOnly) return "모집중"
    return "모집중"
  }

  /**
   * 필터링된 과정 데이터 (최신순 정렬)
   * 검색어와 상태 필터를 적용하여 과정 목록을 필터링
   */
  const filteredCourses = Array.isArray(coursesData) ? coursesData.filter((course) => {
    // 과목명 검색을 위한 처리
    const subjectNames = course.subjects && Array.isArray(course.subjects) 
      ? course.subjects.map(subject => subject.subjectName || '').join(' ')
      : '';
    
    const matchesSearch =
      (course.courseName && course.courseName.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (course.courseId && course.courseId.includes(searchTerm)) ||
      (course.memberName && course.memberName.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (course.courseCode && course.courseCode.includes(searchTerm)) ||
      (subjectNames && subjectNames.toLowerCase().includes(searchTerm.toLowerCase()));
    const courseStatus = calculateCourseStatus(course);
    const matchesStatus = selectedStatus === "all" || courseStatus === selectedStatus;
    const matchesStudentCount = course.studentCount >= course.minCapacity && course.studentCount <= course.maxCapacity;
    return matchesSearch && matchesStatus;
  }).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)) : [];

  /**
   * 과정 상태에 따른 색상 반환
   * @param {string} status - 과정 상태
   * @returns {string} 색상 코드
   */
  const getStatusColor = (status) => {
    switch (status) {
      case "진행중":
        return "#1ABC9C"
      case "모집중":
        return "#3498db"
      case "마감":
        return "#f39c12"
      case "완료":
        return "#95A5A6"
      default:
        return "#95A5A6"
    }
  }

  /**
   * 과정 데이터 불러오기
   */
  useEffect(() => {
    const fetchCourses = async () => {
      try {
        setLoading(true)
        const data = await getAllCourse()
        setCoursesData(data)
      } catch (error) {
        alert("과정 데이터를 불러오는데 실패했습니다.")
      } finally {
        setLoading(false)
      }
    }
    fetchCourses()
  }, [])

  /**
   * 과정 삭제 처리
   * @param {string} courseId - 삭제할 과정 ID
   */
  const handleDelete = async (courseId) => {
    if (confirm("정말로 이 과정을 삭제하시겠습니까?")) {
      try {
        await deleteCourse(courseId)
        alert(`${courseId} 과정이 삭제되었습니다.`)
        const updatedData = await getAllCourse()
        setCoursesData(updatedData)
      } catch (error) {
        alert("과정 삭제 에러")
      }
    }
  }

  /**
   * 과정 상세 페이지로 이동
   * @param {string} courseId - 조회할 과정 ID
   */
  const handleView = (courseId) => {
    navigate(`/courses/course/${courseId}`)
  }

  /**
   * 엑셀 내보내기 함수
   * 필터링된 과정 데이터를 엑셀 파일로 내보냄
   */
  const handleExportToExcel = () => {
    try {
      // 엑셀에 포함할 데이터 준비
      const exportData = filteredCourses.map(course => {
        // 과목별 시간 정보 처리
        let subjectsInfo = ''
        let totalSubjectHours = 0
        
        if (course.subjects && Array.isArray(course.subjects)) {
          subjectsInfo = course.subjects.map(subject => 
            `${subject.subjectName || '과목명 없음'}: ${subject.subjectTime || 0}시간`
          ).join(', ')
          totalSubjectHours = course.subjects.reduce((sum, subject) => 
            sum + (parseInt(subject.subjectTime) || 0), 0
          )
        }
        
        return {
          '과정코드': course.courseCode || '',
          '과정명': course.courseName || '',
          '강사명': course.memberName || '',
          '상태': calculateCourseStatus(course),
          '수강생': `${course.studentCount || 0}/${course.maxCapacity}명`,
          '시작일': course.courseStartDay || '',
          '종료일': course.courseEndDay || '',
          '강의실': course.className || course.classNumber || course.classId || '',
          '수업시간': `${course.startTime || ''} - ${course.endTime || ''}`,
          '수업요일': Array.isArray(course.courseDays) ? course.courseDays.join(', ') : course.courseDays || '',
          '과목별시간': subjectsInfo,
          '총과목시간': `${totalSubjectHours}시간`,
          '최소수강생': course.minCapacity || '',
          '최대수강생': course.maxCapacity || '',
          '등록일': course.createdAt ? new Date(course.createdAt).toLocaleDateString('ko-KR') : '',
        }
      })

      // 워크북 생성
      const wb = XLSX.utils.book_new()
      const ws = XLSX.utils.json_to_sheet(exportData)

      // 컬럼 너비 자동 조정
      const colWidths = [
        { wch: 15 }, // 과정코드
        { wch: 25 }, // 과정명
        { wch: 12 }, // 강사명
        { wch: 10 }, // 상태
        { wch: 12 }, // 수강생
        { wch: 12 }, // 시작일
        { wch: 12 }, // 종료일
        { wch: 15 }, // 강의실
        { wch: 15 }, // 수업시간
        { wch: 15 }, // 수업요일
        { wch: 40 }, // 과목별시간
        { wch: 12 }, // 총과목시간
        { wch: 12 }, // 최소수강생
        { wch: 12 }, // 최대수강생
        { wch: 12 }, // 등록일
      ]
      ws['!cols'] = colWidths

      // 워크시트를 워크북에 추가
      XLSX.utils.book_append_sheet(wb, ws, '과정목록')

      // 파일명 생성 (현재 날짜 포함)
      const today = new Date().toISOString().split('T')[0]
      const fileName = `과정목록_${today}.xlsx`

      // 엑셀 파일 생성 및 다운로드
      const excelBuffer = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
      const data = new Blob([excelBuffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      saveAs(data, fileName)

      alert(`${exportData.length}개의 과정이 엑셀 파일로 내보내졌습니다.`)
    } catch (error) {
      alert('엑셀 내보내기에 실패했습니다.')
    }
  }

  /**
   * 과정 통계 계산
   * 전체 과정 수, 진행중, 모집중, 완료, 총 수강생 수, 총 과목 시간을 계산
   */
  const stats = {
    total: Array.isArray(coursesData) ? coursesData.length : 0,
    active: Array.isArray(coursesData) ? coursesData.filter((c) => calculateCourseStatus(c) === "진행중").length : 0,
    recruiting: Array.isArray(coursesData) ? coursesData.filter((c) => calculateCourseStatus(c) === "모집중").length : 0,
    completed: Array.isArray(coursesData) ? coursesData.filter((c) => calculateCourseStatus(c) === "완료").length : 0,
    totalStudents: Array.isArray(coursesData) ? coursesData.reduce((sum, course) => sum + (course.studentCount || 0), 0) : 0,
    totalSubjectHours: Array.isArray(coursesData) ? coursesData.reduce((sum, course) => {
      if (course.subjects && Array.isArray(course.subjects)) {
        return sum + course.subjects.reduce((subjectSum, subject) => 
          subjectSum + (parseInt(subject.subjectTime) || 0), 0
        )
      }
      return sum
    }, 0) : 0,
  }

  return (
    <PageLayout currentPage="courses">
      <div className="flex">
        <Sidebar title="과정 관리" menuItems={coursesMenuItems} currentPath="/courses/course" />
        <main className="flex-1 p-8">
          <div className="max-w-7xl mx-auto">
            {/* 페이지 헤더 */}
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                전체 과정 목록
              </h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                등록된 모든 과정의 정보를 조회하고 관리할 수 있습니다.
              </p>
            </div>
            
            {/* 검색 및 필터 */}
            <Card className="mb-6">
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>검색 및 필터</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-col md:flex-row gap-4">
                  {/* 검색 입력 필드 */}
                  <div className="flex-1">
                    <div className="relative">
                      <Search
                        className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4"
                        style={{ color: "#95A5A6" }}
                      />
                      <Input
                        placeholder="과정명, 강사명, 과목명으로 검색..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="pl-10 h-10.5" style={{ borderColor: "#95A5A6", color: "#2C3E50" }}
                      />
                    </div>
                  </div>
                  {/* 필터 및 내보내기 버튼 */}
                  <div className="flex gap-2">
                    <select
                      value={selectedStatus}
                      onChange={(e) => setSelectedStatus(e.target.value)}
                      className="px-3 py-2 border rounded-md"
                      style={{ borderColor: "#95A5A6" }}
                    >
                      <option value="all">전체 상태</option>
                      <option value="모집중">모집중</option>
                      <option value="진행중">진행중</option>
                      <option value="마감">마감</option>
                      <option value="완료">완료</option>
                    </select>
                    <Button
                      onClick={handleExportToExcel}
                      className="flex items-center space-x-2 
                      text-[#1abc9c] bg-white hover:bg-[#1abc9c] hover:text-white
                      border border-[#1abc9c]"
                      disabled={filteredCourses.length === 0}
                    >
                      <Download className="w-4 h-4" />
                      <span>엑셀 내보내기</span>
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
            
            {/* 과정 통계 */}
            <div className="grid grid-cols-2 md:grid-cols-6 gap-4 mb-6">
              {/* 전체 과정 수 */}
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#3498db" }}>
                    {stats.total}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    전체 과정
                  </div>
                </CardContent>
              </Card>
              {/* 진행중인 과정 수 */}
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#1ABC9C" }}>
                    {stats.active}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    진행중
                  </div>
                </CardContent>
              </Card>
              {/* 모집중인 과정 수 */}
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#3498db" }}>
                    {stats.recruiting}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    모집중
                  </div>
                </CardContent>
              </Card>
              {/* 완료된 과정 수 */}
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#95A5A6" }}>
                    {stats.completed}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    완료
                  </div>
                </CardContent>
              </Card>
              {/* 총 수강생 수 */}
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#1ABC9C" }}>
                    {stats.totalStudents}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    총 수강생 
                  </div>
                </CardContent>
              </Card>
            </div>
            
            {/* 과정 목록 테이블 */}
            <Card>
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>과정 목록 ({filteredCourses.length}개)</CardTitle>
              </CardHeader>
              <CardContent>
                {loading ? (
                  // 로딩 상태 표시
                  <div className="text-center py-8">
                    <div className="text-lg" style={{ color: "#95A5A6" }}>데이터를 불러오는 중...</div>
                  </div>
                ) : (
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b" style={{ borderColor: "#95A5A6" }}>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          과정코드
                        </th>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          과정명
                        </th>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          강사명
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          상태
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          수강생
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          기간
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          과정 시간
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          관리
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {/* 과정 목록 행들 */}
                      {filteredCourses.map((course) => (
                        <tr key={course.courseId} className="border-b" style={{ borderColor: "#f1f2f6" }}>
                          {/* 과정 코드 */}
                          <td className="py-3 px-4 font-mono text-sm" style={{ color: "#2C3E50" }}>
                            {course.courseCode}
                          </td>
                          {/* 과정명 */}
                          <td className="py-3 px-4">
                            <div>
                              <p className="font-medium" style={{ color: "#2C3E50" }}>
                                {course.courseName}
                              </p>
                            </div>
                          </td>
                          {/* 강사명 */}
                          <td className="py-3 px-4" style={{ color: "#95A5A6" }}>
                            {course.memberName}
                          </td>
                          {/* 과정 상태 */}
                          <td className="py-3 px-4 text-center">
                            <Badge className="text-white" style={{ backgroundColor: getStatusColor(calculateCourseStatus(course)) }}>
                              {calculateCourseStatus(course)}
                            </Badge>
                          </td>
                          {/* 수강생 정보 */}
                          <td className="py-3 px-4 text-center">
                            <div className="flex flex-col items-center">
                              <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                              {course.studentCount || 0}/{course.maxCapacity}명
                              </span>
                              {/* 수강생 비율 표시 바 */}
                              <div className="w-16 bg-gray-200 rounded-full h-1 mt-1">
                                <div
                                  className="h-1 rounded-full"
                                  style={{
                                    width: `${course.maxCapacity > 0 ? ((course.studentCount || 0) / course.maxCapacity) * 100 : 0}%`,
                                    backgroundColor: "#1ABC9C",
                                  }}
                                ></div>
                              </div>
                              <span className="text-xs mt-1" style={{ color: "#95A5A6" }}>
                                -
                              </span>
                            </div>
                          </td>
                          {/* 과정 기간 */}
                          <td className="py-3 px-4 text-center text-sm" style={{ color: "#95A5A6" }}>
                            {course.courseStartDay} - {course.courseEndDay}
                          </td>
                          {/* 과목별 시간 정보 */}
                          <td className="py-3 px-4 text-center">
                            {(() => {
                              if (course.subjects && Array.isArray(course.subjects) && course.subjects.length > 0) {
                                const totalHours = course.subjects.reduce((sum, subject) => 
                                  sum + (parseInt(subject.subjectTime) || 0), 0
                                )
                                const subjectCount = course.subjects.length
                                return (
                                  <div className="flex flex-col items-center">
                                    <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                      {totalHours}시간
                                    </span>
                                    <span className="text-xs" style={{ color: "#95A5A6" }}>
                                      {subjectCount}개 과목
                                    </span>
                                    {/* 과목별 상세 정보 툴팁 */}
                                    <div className="group relative">
                                      <div className="absolute bottom-full left-0 transform -translate-x-full mb-2 px-3 py-2 bg-gray-800 text-white text-xs rounded-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 whitespace-nowrap z-50">
                                        {course.subjects.map((subject, index) => (
                                          <div key={index}>
                                            {subject.subjectName || '과목명 없음'}: {subject.subjectTime || 0}시간
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  </div>
                                )
                              } else {
                                return (
                                  <span className="text-sm" style={{ color: "#95A5A6" }}>
                                    -
                                  </span>
                                )
                              }
                            })()}
                          </td>
                          {/* 관리 버튼 */}
                          <td className="py-3 px-4">
                            <div className="flex justify-center space-x-2">
                              {/* 상세보기 버튼 */}
                              <Button size="sm" variant="ghost" onClick={() => handleView(course.courseId)} className="p-1 hover:bg-blue-50 hover:scale-110 transition-all duration-200">
                                <Eye className="w-4 h-4" style={{ color: "#1ABC9C" }} />
                              </Button>
                              {/* 삭제 버튼 (모집중 상태일 때만 활성화) */}
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => handleDelete(course.courseId)}
                                className="p-1 hover:bg-red-50 hover:scale-110 transition-all duration-200"
                                disabled={calculateCourseStatus(course) !== "모집중"}
                              >
                                <Trash2 className="w-4 h-4" style={{ color: calculateCourseStatus(course) === "모집중" ? "#e74c3c" : "#ccc" }} />
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {/* 검색 결과가 없을 때 표시 */}
                  {filteredCourses.length === 0 && (
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
    </PageLayout>
  )
}
  