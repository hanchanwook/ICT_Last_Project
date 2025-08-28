import { useState, useRef, useEffect } from "react"
import Header from "@/components/layout/header"
import Sidebar from "@/components/layout/sidebar"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Calendar, Clock, CheckCircle, XCircle, AlertCircle, User, MapPin, Search, Filter, ChevronDown, CalendarDays } from "lucide-react"
import { getMenuItems } from "@/components/ui/menuConfig"
import { 
  getStudentAttendanceRecords, 
  getAttendanceStatusIcon,
  getAttendanceStatusColor 
} from "@/api/hancw/studentCourseAxios"

export default function AttendanceHistoryPage() {
  const [selectedSubject, setSelectedSubject] = useState("all")
  const [isSubjectOpen, setIsSubjectOpen] = useState(false)
  const [isCalendarOpen, setIsCalendarOpen] = useState(false)
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")
  const [attendanceRecords, setAttendanceRecords] = useState([])
  const [subjects, setSubjects] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [currentPage, setCurrentPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(20)
  const [isItemsPerPageOpen, setIsItemsPerPageOpen] = useState(false)
  const subjectRef = useRef(null)
  const calendarRef = useRef(null)
  const itemsPerPageRef = useRef(null)

  useEffect(() => {
    console.log('=== studentAttendancePage.jsx useEffect 시작 ===')
    const userInfo = localStorage.getItem('currentUser')
    console.log('localStorage에서 가져온 userInfo:', userInfo)
    
    if (userInfo) {
      const user = JSON.parse(userInfo)
      console.log('파싱된 user 객체:', user)
      const userId = user.userInfo?.userId || user.memberId
      console.log('사용할 userId:', userId)
      loadAttendanceData(userId)
    } else {
      console.error('localStorage에 currentUser가 없습니다!')
    }
  }, [])

  const loadAttendanceData = async (studentId) => {
    console.log('=== loadAttendanceData 함수 시작 ===')
    console.log('받은 studentId:', studentId)
    
    try {
      setIsLoading(true)
      console.log('API 호출 시작...')
      
      // 임시로 과목 목록 API 호출 제거 (백엔드 엔드포인트 없음)
      const records = await getStudentAttendanceRecords(studentId)
      
      console.log('API 응답 - records:', records)
      console.log('API 응답 타입:', typeof records)
      console.log('API 응답 키들:', Object.keys(records || {}))
      
      // 백엔드 응답 구조에 맞게 수정
      const attendanceArray = records.attendances || records || []
      console.log('처리된 attendanceArray:', attendanceArray)
      console.log('attendanceArray 타입:', typeof attendanceArray)
      console.log('attendanceArray 길이:', Array.isArray(attendanceArray) ? attendanceArray.length : '배열 아님')
      
      // 출석 기록에서 고유한 과정명들을 추출하여 드롭다운 옵션으로 설정
      const uniqueCourseNames = Array.isArray(attendanceArray) 
        ? [...new Set(attendanceArray.map(record => record.courseName).filter(name => name))] 
        : []
      console.log('추출된 고유 과정명들:', uniqueCourseNames)
      
      setAttendanceRecords(attendanceArray)
      setSubjects(uniqueCourseNames)
      console.log('상태 업데이트 완료')
    } catch (error) {
      console.error('출석 데이터 로드 실패:', error)
      console.error('에러 상세 정보:', {
        message: error.message,
        stack: error.stack,
        name: error.name
      })
    } finally {
      setIsLoading(false)
      console.log('로딩 상태 해제')
    }
  }

  const sidebarMenuItems = getMenuItems('student-my-courses')

  // 날짜 범위 필터링 함수
  const isInDateRange = (recordDate) => {
    if (!startDate || !endDate) return true
    
    const recordDateTime = new Date(recordDate)
    const start = new Date(startDate)
    const end = new Date(endDate)
    start.setHours(0, 0, 0, 0)
    end.setHours(23, 59, 59, 999)
    
    return recordDateTime >= start && recordDateTime <= end
  }

  // 필터링된 기록
  const filteredRecords = Array.isArray(attendanceRecords) ? attendanceRecords.filter((record) => {
    // 날짜 범위 필터링
    const dateMatch = isInDateRange(record.lectureDate)

    // 과정 필터링
    const subjectMatch = selectedSubject === "all" || record.courseName === selectedSubject

    return dateMatch && subjectMatch
  }) : []

  // === 출석/지각/결석 카운트 계산 ===
  const total = filteredRecords.length
  const attended = filteredRecords.filter(r => r.attendanceStatus === "출석").length
  const late = filteredRecords.filter(r => r.attendanceStatus === "지각").length
  const absent = filteredRecords.filter(r => r.attendanceStatus === "결석").length

  // === 페이징 처리 ===
  const totalPages = Math.ceil(filteredRecords.length / itemsPerPage)
  const startIndex = (currentPage - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  const currentRecords = filteredRecords.slice(startIndex, endIndex)

  // 페이지 변경 시 현재 페이지를 1로 리셋
  useEffect(() => {
    setCurrentPage(1)
  }, [startDate, endDate, selectedSubject, itemsPerPage])

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (subjectRef.current && !subjectRef.current.contains(event.target)) {
        setIsSubjectOpen(false)
      }
      if (calendarRef.current && !calendarRef.current.contains(event.target)) {
        setIsCalendarOpen(false)
      }
      if (itemsPerPageRef.current && !itemsPerPageRef.current.contains(event.target)) {
        setIsItemsPerPageOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const getStatusIcon = (status) => {
    const iconName = getAttendanceStatusIcon(status)
    switch (iconName) {
      case "check-circle":
        return <CheckCircle className="w-4 h-4 text-green-600" />
      case "alert-circle":
        return <AlertCircle className="w-4 h-4 text-yellow-600" />
      case "x-circle":
        return <XCircle className="w-4 h-4 text-red-600" />
      default:
        return null
    }
  }

  const getStatusColor = (status) => {
    return getAttendanceStatusColor(status)
  }

  console.log('=== studentAttendancePage.jsx 렌더링 ===')
  console.log('현재 상태:', {
    attendanceRecords: Array.isArray(attendanceRecords) ? attendanceRecords.length : '배열 아님',
    attendanceRecordsType: typeof attendanceRecords,
    attendanceRecordsData: attendanceRecords,
    subjects: subjects.length,
    isLoading,
    selectedSubject
  })

  // 출석 기록 상세 로그 (관리자 모드용)
  if (Array.isArray(attendanceRecords) && attendanceRecords.length > 0) {
    console.log('=== 출석 기록 상세 정보 (관리자 모드) ===')
    attendanceRecords.forEach((record, index) => {
      console.log(`[출석 기록 ${index + 1}]`, {
        attendanceId: record.attendanceId,
        userId: record.userId,
        lectureDate: record.lectureDate,
        lectureStartTime: record.lectureStartTime,
        lectureEndTime: record.lectureEndTime,
        checkIn: record.checkIn,
        checkOut: record.checkOut,
        attendanceStatus: record.attendanceStatus,
        courseName: record.courseName,
        instructorName: record.instructorName,
        classroomName: record.classroomName,
        classId: record.classId,
        note: record.note,
        createdAt: record.createdAt,
        updatedAt: record.updatedAt
      })
    })
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header currentPage="my-courses" userRole="student" userName="학생" />
      <div className="flex">
        <Sidebar title="출석 관리" menuItems={sidebarMenuItems} currentPath="/student/my-courses" />
        <main className="flex-1 p-6">
          {isLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
              <span className="ml-3 text-gray-600">데이터를 불러오는 중...</span>
            </div>
          ) : (
            <div className="max-w-7xl mx-auto">
              <h1 className="text-2xl font-bold text-gray-900 mb-6">출석 기록</h1>
              <div className="space-y-6">

                 {/* 출석 통계 */}
                 <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                   <Card>
                     <CardContent className="p-6">
                       <div className="text-center">
                         <div className="text-3xl font-bold text-[#3498db]">{total}</div>
                         <div className="text-m text-gray-600">총 수업</div>
                       </div>
                     </CardContent>
                   </Card>

                   <Card>
                     <CardContent className="p-6">
                       <div className="text-center">
                         <div className="text-3xl font-bold text-[#1abc9c]">{attended}</div>
                         <div className="text-m text-gray-600">출석</div>
                       </div>
                     </CardContent>
                   </Card>

                   <Card>
                     <CardContent className="p-6">
                       <div className="text-center">
                         <div className="text-3xl font-bold text-[#F1C40F]">{late}</div>
                         <div className="text-m text-gray-600">지각</div>
                       </div>
                     </CardContent>
                   </Card>

                   <Card>
                     <CardContent className="p-6">
                       <div className="text-center">
                         <div className="text-3xl font-bold text-[#e74c3c]">{absent}</div>
                         <div className="text-m text-gray-600">결석</div>
                       </div>
                     </CardContent>
                   </Card>
                 </div>

                                 {/* 출석 기록 테이블 */}
                 <div className="bg-white rounded-lg shadow-sm border">
                   {/* 테이블 헤더 */}
                   <div className="p-6 border-b">
                     <div className="flex flex-col md:flex-row md:items-center md:justify-between space-y-4 md:space-y-0">
                       <div>
                         <h2 className="text-lg font-semibold text-gray-900">출석 기록</h2>
                         <p className="text-sm text-gray-600 mt-1">
                           총 {filteredRecords.length}개의 기록
                           {startDate && endDate && (
                             <span className="ml-2 text-blue-600">
                               (날짜 범위 필터 적용)
                             </span>
                           )}
                           {totalPages > 1 && (
                             <span className="ml-2 text-gray-500">
                               (페이지 {currentPage}/{totalPages})
                             </span>
                           )}
                         </p>
                       </div>

                                                                       {/* 필터 섹션 */}
                        <div className="flex flex-col space-y-3">
                          <div className="flex flex-col md:flex-row md:items-center space-y-4 md:space-y-0 md:space-x-4">
                            {/* 초기화 버튼 */}
                            {(startDate || endDate || selectedSubject !== "all") && (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => {
                                  setSelectedSubject("all")
                                  setStartDate("")
                                  setEndDate("")
                                }}
                                className="text-gray-600 hover:text-gray-800"
                              >
                                필터 초기화
                              </Button>
                            )}

                            

                            {/* 날짜 범위 선택 달력 */}
                            <div className="relative" ref={calendarRef}>
                              <button
                                onClick={() => setIsCalendarOpen(!isCalendarOpen)}
                                className="flex items-center justify-between w-full px-3 py-2 border-2 border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white min-w-[200px]"
                              >
                                <div className="flex items-center space-x-2">
                                  <CalendarDays className="w-4 h-4 text-gray-600" />
                                  <span className="text-gray-700">
                                    {startDate && endDate 
                                      ? `${new Date(startDate).toLocaleDateString('ko-KR')} ~ ${new Date(endDate).toLocaleDateString('ko-KR')}`
                                      : "날짜 범위 선택"
                                    }
                                  </span>
                                </div>
                                <ChevronDown className={`w-4 h-4 text-gray-600 transition-transform ${isCalendarOpen ? 'rotate-180' : ''}`} />
                              </button>
                              
                              {isCalendarOpen && (
                                <div className="absolute top-full left-0 mt-1 bg-white border-2 border-gray-300 rounded-md shadow-lg z-20 p-4 min-w-[300px]">
                                  <div className="space-y-4">
                                    <div>
                                      <label className="block text-sm font-medium text-gray-700 mb-2">시작일</label>
                                      <input
                                        type="date"
                                        value={startDate}
                                        onChange={(e) => setStartDate(e.target.value)}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                                      />
                                    </div>
                                    <div>
                                      <label className="block text-sm font-medium text-gray-700 mb-2">종료일</label>
                                      <input
                                        type="date"
                                        value={endDate}
                                        onChange={(e) => setEndDate(e.target.value)}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                                      />
                                    </div>
                                    <div className="flex space-x-2 pt-2">
                                      <Button
                                        size="sm"
                                        onClick={() => {
                                          if (startDate && endDate) {
                                            setIsCalendarOpen(false)
                                          } else {
                                            alert('시작일과 종료일을 모두 선택해주세요.')
                                          }
                                        }}
                                        className="flex-1"
                                      >
                                        적용
                                      </Button>
                                      <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => {
                                          setStartDate("")
                                          setEndDate("")
                                          setIsCalendarOpen(false)
                                        }}
                                        className="flex-1"
                                      >
                                        취소
                                      </Button>
                                    </div>
                                  </div>
                                </div>
                              )}
                            </div>

                            {/* 커스텀 과정 드롭다운 */}
                            <div className="relative" ref={subjectRef}>
                              <button
                                onClick={() => setIsSubjectOpen(!isSubjectOpen)}
                                className="flex items-center justify-between w-full px-3 py-2 border-2 border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white min-w-[140px]"
                              >
                                <span>
                                  {selectedSubject === "all" ? "과정 선택" : selectedSubject}
                                </span>
                                <ChevronDown className={`w-4 h-4 transition-transform ${isSubjectOpen ? 'rotate-180' : ''}`} />
                              </button>
                              
                              {isSubjectOpen && (
                                <div className="absolute top-full left-0 right-0 mt-1 bg-white border-2 border-gray-300 rounded-md shadow-lg z-10">
                                  <div 
                                    className="px-3 py-2 hover:bg-blue-50 cursor-pointer border-b border-gray-200"
                                    onClick={() => {
                                      setSelectedSubject("all")
                                      setIsSubjectOpen(false)
                                    }}
                                  >
                                    전체 과정
                                  </div>
                                  {subjects.map((subject, index) => (
                                    <div 
                                      key={subject}
                                      className={`px-3 py-2 hover:bg-blue-50 cursor-pointer ${index < subjects.length - 1 ? 'border-b border-gray-200' : ''}`}
                                      onClick={() => {
                                        setSelectedSubject(subject)
                                        setIsSubjectOpen(false)
                                      }}
                                    >
                                      {subject}
                                    </div>
                                  ))}
                                </div>
                              )}
                                                         </div>

                             {/* 페이지당 항목 수 선택 */}
                             <div className="relative" ref={itemsPerPageRef}>
                               <button
                                 onClick={() => setIsItemsPerPageOpen(!isItemsPerPageOpen)}
                                 className="flex items-center justify-between w-full px-3 py-2 border-2 border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white min-w-[120px]"
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

                                                      {/* 적용된 필터 표시 (작은 크기) */}
                            {(startDate || endDate || selectedSubject !== "all") && (
                             <div className="flex items-center justify-end space-x-2 text-xs text-gray-600 bg-gray-50 px-3 py-2 rounded-md">
                               {startDate && endDate && (
                                 <Badge variant="secondary" className="bg-blue-50 text-blue-700 text-xs px-2 py-1">
                                   {`${new Date(startDate).toLocaleDateString('ko-KR')} ~ ${new Date(endDate).toLocaleDateString('ko-KR')}`}
                                 </Badge>
                               )}
                               {selectedSubject !== "all" && (
                                 <Badge variant="secondary" className="bg-blue-50 text-blue-700 text-xs px-2 py-1">
                                   {selectedSubject}
                                 </Badge>
                               )}
                             </div>
                           )}
                        </div>
                     </div>
                   </div>
                  <div className="overflow-x-auto">
                                         <table className="w-full">
                       <thead className="bg-gray-50">
                         <tr>
                           <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                             날짜
                           </th>
                           <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                             과정
                           </th>
                           <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                             강의실
                           </th>
                           <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                             출석시간
                           </th>
                           <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                             퇴실시간
                           </th>
                           <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                             상태
                           </th>
                         </tr>
                       </thead>
                                              <tbody className="bg-white divide-y divide-gray-200">
                          {currentRecords.length === 0 ? (
                            <tr>
                              <td colSpan="6" className="px-6 py-12 text-center text-gray-500">
                                <Calendar className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                                {attendanceRecords.length === 0 
                                  ? "출석 이력이 없습니다."
                                  : "선택한 조건에 해당하는 출석 기록이 없습니다."
                                }
                              </td>
                            </tr>
                          ) : (
                           currentRecords.map((record) => (
                             <tr key={record.attendanceId} className="hover:bg-gray-50">
                               <td className="px-6 py-4 whitespace-nowrap text-center">
                                 <div className="text-sm text-gray-900">{record.lectureDate}</div>
                               </td>
                               <td className="px-6 py-4 whitespace-nowrap text-center">
                                 <div className="text-sm font-medium text-gray-900">{record.courseName || "미지정"}</div>
                               </td>
                               <td className="px-6 py-4 whitespace-nowrap text-center">
                                 <div className="text-sm text-gray-900">{record.classroomName || record.classId || "미지정"}</div>
                               </td>
                               <td className="px-6 py-4 whitespace-nowrap text-center">
                                 <div className="text-sm text-gray-900">
                                   {record.checkIn ? new Date(record.checkIn).toLocaleTimeString('ko-KR', {hour: '2-digit', minute: '2-digit'}) : "-"}
                                 </div>
                               </td>
                               <td className="px-6 py-4 whitespace-nowrap text-center">
                                 <div className="text-sm text-gray-900">
                                   {record.checkOut ? new Date(record.checkOut).toLocaleTimeString('ko-KR', {hour: '2-digit', minute: '2-digit'}) : "-"}
                                 </div>
                               </td>
                               <td className="px-6 py-4 whitespace-nowrap text-center">
                                 <div
                                   className={`inline-flex items-center space-x-1 px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(record.attendanceStatus)}`}
                                 >
                                   {getStatusIcon(record.attendanceStatus)}
                                   <span>{record.attendanceStatus}</span>
                                 </div>
                               </td>
                             </tr>
                           ))
                         )}
                       </tbody>
                     </table>
                  </div>

                  {/* 페이징 컨트롤 */}
                  {totalPages > 1 && (
                    <div className="px-6 py-4 border-t bg-gray-50">
                      <div className="flex items-center justify-between">
                        <div className="text-sm text-gray-700">
                          {startIndex + 1} - {Math.min(endIndex, filteredRecords.length)} / {filteredRecords.length}개
                        </div>
                        <div className="flex items-center space-x-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                            disabled={currentPage === 1}
                            className="px-3 py-1"
                          >
                            이전
                          </Button>
                          
                          {/* 페이지 번호들 */}
                          <div className="flex items-center space-x-1">
                            {Array.from({ length: totalPages }, (_, i) => i + 1).map(pageNum => {
                              // 현재 페이지 주변의 5개 페이지만 표시
                              if (
                                pageNum === 1 ||
                                pageNum === totalPages ||
                                (pageNum >= currentPage - 2 && pageNum <= currentPage + 2)
                              ) {
                                return (
                                  <Button
                                    key={pageNum}
                                    variant={currentPage === pageNum ? "default" : "outline"}
                                    size="sm"
                                    onClick={() => setCurrentPage(pageNum)}
                                    className="px-3 py-1 min-w-[40px]"
                                  >
                                    {pageNum}
                                  </Button>
                                )
                              } else if (
                                pageNum === currentPage - 3 ||
                                pageNum === currentPage + 3
                              ) {
                                return (
                                  <span key={pageNum} className="px-2 text-gray-500">
                                    ...
                                  </span>
                                )
                              }
                              return null
                            })}
                          </div>
                          
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                            disabled={currentPage === totalPages}
                            className="px-3 py-1"
                          >
                            다음
                          </Button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
