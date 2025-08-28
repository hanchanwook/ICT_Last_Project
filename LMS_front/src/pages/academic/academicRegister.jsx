import { useState, useEffect } from "react"
import { useLocation } from "react-router-dom"
import { Search, Calendar, Clock, CheckCircle, XCircle, AlertCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getAllAttendances, updateAttendance } from "@/api/hancw/staffAcademicAxios"
import { getMenuItems } from "@/components/ui/menuConfig"

export default function AttendanceRegisterPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [startDate, setStartDate] = useState(() => {
    const today = new Date()
    const sevenDaysAgo = new Date(today)
    sevenDaysAgo.setDate(today.getDate() - 7)
    return sevenDaysAgo.toISOString().split("T")[0]
  })
  const [endDate, setEndDate] = useState(new Date().toISOString().split("T")[0])
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [isDropdownOpen, setIsDropdownOpen] = useState(false)
  
  // 페이징 상태
  const [currentPage, setCurrentPage] = useState(1)
  const [itemsPerPage] = useState(20)

  const [selectedStudent, setSelectedStudent] = useState(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editFormData, setEditFormData] = useState({})

  // 출석 데이터 상태
  const [attendanceData, setAttendanceData] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const [currentUser, setCurrentUser] = useState(null)
  const location = useLocation()

  // 동적 사이드바 메뉴 생성
  const sidebarMenuItems = getMenuItems('academic')
  const currentPath = location.pathname

  // 출석 데이터 조회
  const fetchAttendanceData = async () => {
    setLoading(true)
    setError(null)
    try {
      // 전체 학생 출석 기록 조회 (userId 없이)
      const data = await getAllAttendances()
      setAttendanceData(data)
    } catch (err) {
      setError("출석 데이터를 불러오는데 실패했습니다.")
      console.error("출석 데이터 조회 실패:", err)
    } finally {
      setLoading(false)
    }
  }

  // 날짜나 필터 변경 시 데이터 재조회 및 페이지 초기화
  useEffect(() => {
    fetchAttendanceData()
    setCurrentPage(1) // 필터 변경 시 페이지를 1로 초기화
  }, [startDate, endDate, selectedStatus, searchTerm])

  // 현재 사용자 정보 가져오기
  useEffect(() => {
    const userData = localStorage.getItem("currentUser")
    if (userData) {
      setCurrentUser(JSON.parse(userData))
    }
  }, [])

  // 강사별 담당 강의 정보 상태
  const [instructorCourses, setInstructorCourses] = useState({})

  // 필터링된 출석 데이터
  const filteredAttendance = attendanceData.filter((attendance) => {
    // 출석 데이터 구조에 맞게 수정
    const attendanceStatus = attendance?.attendanceStatus || ''
    const attendanceDate = attendance?.lectureDate || ''
    const memberName = attendance?.memberName || attendance?.studentName || ''
    const memberEmail = attendance?.memberEmail || attendance?.email || ''
    const courseName = attendance?.courseName || attendance?.classroomInfo || ''
    const userId = attendance?.userId || ''

    const matchesSearch =
      memberName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      memberEmail.toLowerCase().includes(searchTerm.toLowerCase()) ||
      courseName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      userId.toLowerCase().includes(searchTerm.toLowerCase())

    const matchesStatus = selectedStatus === "all" || attendanceStatus === selectedStatus

    // 날짜 범위 필터링
    const matchesDateRange = attendanceDate >= startDate && attendanceDate <= endDate

    // 강사 계정인 경우 담당 강의 학생만 필터링
    let matchesInstructor = true
    if (currentUser && currentUser.role === "instructor") {
      const instructorStudents = instructorCourses[currentUser.userType] || []
      const allStudentIds = instructorStudents.flatMap((course) => course.students)
      matchesInstructor = allStudentIds.includes(userId)
    }

    return matchesSearch && matchesStatus && matchesDateRange && matchesInstructor
  })

  const getStatusIcon = (status) => {
    switch (status) {
      case "출석":
        return <CheckCircle className="w-4 h-4" style={{ color: "#1ABC9C" }} />
      case "지각":
        return <AlertCircle className="w-4 h-4" style={{ color: "#f39c12" }} />
      case "결석":
        return <XCircle className="w-4 h-4" style={{ color: "#e74c3c" }} />
      case "조퇴":
        return <Clock className="w-4 h-4" style={{ color: "#f39c12" }} />
      default:
        return <AlertCircle className="w-4 h-4" style={{ color: "#95A5A6" }} />
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case "출석":
        return "#1ABC9C"
      case "지각":
        return "#f39c12"
      case "결석":
        return "#e74c3c"
      case "조퇴":
        return "#f39c12"
      default:
        return "#95A5A6"
    }
  }

  const handleRowClick = (student) => {
    setSelectedStudent(student)
    
    // checkIn 날짜와 시간을 분리
    let checkInDate = student.lectureDate
    let checkInTime = ""
    if (student.checkIn && student.checkIn.includes('T')) {
      const [datePart, timePart] = student.checkIn.split('T')
      checkInDate = datePart // YYYY-MM-DD 형식
      if (timePart) {
        checkInTime = timePart.slice(0, 5) // HH:MM 형식
      }
    }
    
    // checkOut 날짜와 시간을 분리
    let checkOutDate = student.lectureDate
    let checkOutTime = ""
    if (student.checkOut && student.checkOut.includes('T')) {
      const [datePart, timePart] = student.checkOut.split('T')
      checkOutDate = datePart // YYYY-MM-DD 형식
      if (timePart) {
        checkOutTime = timePart.slice(0, 5) // HH:MM 형식
      }
    }
    
    setEditFormData({
      status: student.attendanceStatus,
      checkInDate: checkInDate,
      checkInTime: checkInTime,
      checkOutDate: checkOutDate,
      checkOutTime: checkOutTime,
      note: student.note,
    })
    setIsModalOpen(true)
  }

  const handleModalClose = () => {
    setIsModalOpen(false)
    setSelectedStudent(null)
    setEditFormData({})
  }

  const handleFormChange = (field, value) => {
    setEditFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  const handleSaveChanges = async () => {
    try {
      console.log('=== 출석 수정 디버깅 ===')
      console.log('selectedStudent:', selectedStudent)
      console.log('selectedStudent.memberId:', selectedStudent.memberId)
      console.log('selectedStudent.userId:', selectedStudent.userId)
      
      const checkIn = editFormData.checkInDate && editFormData.checkInTime ? 
        `${editFormData.checkInDate}T${editFormData.checkInTime}:00` : null
      
      const checkOut = editFormData.checkOutDate && editFormData.checkOutTime ? 
        `${editFormData.checkOutDate}T${editFormData.checkOutTime}:00` : null
      
      const studentId = selectedStudent.memberId || selectedStudent.userId
      console.log('사용할 studentId:', studentId)
      
             const updateData = {
         studentId: studentId,
         courseId: selectedStudent.courseId,
         classId: selectedStudent.classId,
         date: selectedStudent.lectureDate,
         attendanceStatus: editFormData.status,
         checkIn: checkIn,
         checkOut: checkOut,
         note: editFormData.note || ""
       }
       
       console.log('=== 백엔드로 보내는 수정 데이터 ===')
       console.log('updateData:', updateData)
       console.log('attendanceId:', selectedStudent.attendanceId)
      
      await updateAttendance(selectedStudent.attendanceId, updateData)
      alert(`${selectedStudent.memberName || selectedStudent.studentName} 학생의 출석 정보가 수정되었습니다.`)
      
      // 데이터 재조회
      fetchAttendanceData()
      handleModalClose()
    } catch (err) {
      alert("출석 정보 수정에 실패했습니다.")
      console.error("출석 정보 수정 실패:", err)
    }
  }

  // 페이징된 데이터 계산
  const getPaginatedData = () => {
    const startIndex = (currentPage - 1) * itemsPerPage
    const endIndex = startIndex + itemsPerPage
    return filteredAttendance.slice(startIndex, endIndex)
  }

  // 총 페이지 수 계산
  const getTotalPages = () => {
    return Math.ceil(filteredAttendance.length / itemsPerPage)
  }

  // 페이지 변경 핸들러
  const handlePageChange = (page) => {
    setCurrentPage(page)
  }

  // 출석 통계 계산
  const stats = {
    total: filteredAttendance.length,
    present: filteredAttendance.filter((s) => s.attendanceStatus === "출석").length,
    late: filteredAttendance.filter((s) => s.attendanceStatus === "지각").length,
    absent: filteredAttendance.filter((s) => s.attendanceStatus === "결석").length,
    earlyLeave: filteredAttendance.filter((s) => s.attendanceStatus === "조퇴").length,
  }

  return (
    <PageLayout currentPage="academic" userRole="staff">
      <div className="flex">
        <Sidebar title="회원 정보" menuItems={sidebarMenuItems} currentPath={currentPath} />

        <main className="flex-1 p-8">
          <div className="max-w-7xl mx-auto">
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                학생 출/결 처리
              </h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                학생들의 출석, 지각, 결석, 조퇴 상태를 확인하고 관리할 수 있습니다.
              </p>
            </div>

            {/* 강사 담당 강의 정보 */}
            {currentUser && currentUser.role === "instructor" && (
              <Card className="mb-6">
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50" }}>담당 강의 정보</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {instructorCourses[currentUser.userType]?.map((course, index) => (
                      <div key={index} className="p-4 bg-gray-50 rounded-lg">
                        <h4 className="font-semibold mb-2" style={{ color: "#2C3E50" }}>
                          {course.courseName} ({course.courseId})
                        </h4>
                        <p className="text-sm" style={{ color: "#95A5A6" }}>
                          수강생: {course.students.length}명
                        </p>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}

            {/* 검색 및 필터 */}
            <Card className="mb-6">
              <CardHeader>
                <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                  <CardTitle style={{ color: "#2C3E50" }}>
                    {currentUser && currentUser.role === "instructor"
                      ? `담당 강의 학생 출석 현황 (${startDate} ~ ${endDate}) - ${filteredAttendance.length}명`
                      : `출석 현황 (${startDate} ~ ${endDate}) - ${filteredAttendance.length}명`}
                  </CardTitle>
                  <div className="flex items-center space-x-2">
                    <Calendar className="w-4 h-4" style={{ color: "#95A5A6" }} />
                    <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                      기간:
                    </span>
                    <input
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      className="px-2 py-1 border rounded text-sm"
                      style={{ borderColor: "#95A5A6" }}
                    />
                    <span className="text-sm" style={{ color: "#95A5A6" }}>
                      ~
                    </span>
                    <input
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      className="px-2 py-1 border rounded text-sm"
                      style={{ borderColor: "#95A5A6" }}
                    />
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div className="relative">
                    <Search
                      className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4"
                      style={{ color: "#95A5A6" }}
                    />
                    <Input
                      placeholder="이름, 이메일, 과정으로 검색..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-10 h-10.5" style={{ borderColor: "#95A5A6"}}
                    />
                  </div>

                  <div className="relative">
                    <button
                      type="button"
                      onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                      className="w-full px-3 py-2 border rounded-md text-left flex justify-between items-center"
                      style={{ borderColor: "#95A5A6", backgroundColor: "white" }}
                    >
                      <span>{selectedStatus === "all" ? "전체 상태" : selectedStatus}</span>
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>
                    
                    {isDropdownOpen && (
                      <div className="absolute z-10 w-full mt-1 bg-white border rounded-md shadow-lg" style={{ borderColor: "#95A5A6" }}>
                        <div 
                          className="px-3 py-2 cursor-pointer hover:bg-gray-100 border-b"
                          style={{ borderColor: "#95A5A6" }}
                          onClick={() => {
                            setSelectedStatus("all")
                            setIsDropdownOpen(false)
                          }}
                        >
                          전체 상태
                        </div>
                        <div 
                          className="px-3 py-2 cursor-pointer hover:bg-gray-100 border-b"
                          style={{ borderColor: "#95A5A6" }}
                          onClick={() => {
                            setSelectedStatus("출석")
                            setIsDropdownOpen(false)
                          }}
                        >
                          출석
                        </div>
                        <div 
                          className="px-3 py-2 cursor-pointer hover:bg-gray-100 border-b"
                          style={{ borderColor: "#95A5A6" }}
                          onClick={() => {
                            setSelectedStatus("지각")
                            setIsDropdownOpen(false)
                          }}
                        >
                          지각
                        </div>
                        <div 
                          className="px-3 py-2 cursor-pointer hover:bg-gray-100 border-b"
                          style={{ borderColor: "#95A5A6" }}
                          onClick={() => {
                            setSelectedStatus("결석")
                            setIsDropdownOpen(false)
                          }}
                        >
                          결석
                        </div>
                        <div 
                          className="px-3 py-2 cursor-pointer hover:bg-gray-100"
                          onClick={() => {
                            setSelectedStatus("조퇴")
                            setIsDropdownOpen(false)
                          }}
                        >
                          조퇴
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 출석 통계 */}
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#3498db" }}>
                    {stats.total}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    전체 학생
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#1ABC9C" }}>
                    {stats.present}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    출석
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#f39c12" }}>
                    {stats.late}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    지각
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#e74c3c" }}>
                    {stats.absent}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    결석
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#f39c12" }}>
                    {stats.earlyLeave}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    조퇴
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* 출석 현황 테이블 */}
            <Card>
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>
                  출석 현황 ({startDate} ~ {endDate}) - {filteredAttendance.length}명
                </CardTitle>
              </CardHeader>
              <CardContent>
                {loading && (
                  <div className="text-center py-8">
                    <p style={{ color: "#95A5A6" }}>데이터를 불러오는 중...</p>
                  </div>
                )}
                
                {error && (
                  <div className="text-center py-8">
                    <p style={{ color: "#e74c3c" }}>{error}</p>
                    <Button 
                      onClick={fetchAttendanceData}
                      className="mt-2"
                      style={{ backgroundColor: "#1ABC9C" }}
                    >
                      다시 시도
                    </Button>
                  </div>
                )}
                
                {!loading && !error && (
                <div className="overflow-x-auto">
                  <table className="w-full">
                                         <thead>
                       <tr className="border-b" style={{ borderColor: "#95A5A6" }}>
                         <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                           이메일
                         </th>
                         <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                           이름
                         </th>
                         <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                           과정
                         </th>
                         <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                           출석상태
                         </th>
                         <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                           등교시간
                         </th>
                         <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                           하교시간
                         </th>
                         <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                           날짜
                         </th>
                       </tr>
                     </thead>
                    <tbody>
                      {getPaginatedData().map((attendance, index) => (
                                                 <tr
                           key={`${attendance.attendanceId}-${attendance.userId}-${attendance.lectureDate}-${index}`}
                           className="border-b hover:bg-gray-50 cursor-pointer"
                           style={{ borderColor: "#f1f2f6" }}
                           onClick={() => handleRowClick(attendance)}
                         >
                           <td className="py-3 px-4 font-mono text-sm text-center" style={{ color: "#2C3E50" }}>
                             {(() => {
                               const email = attendance.memberEmail || attendance.email || `user-${attendance.userId?.slice(-8) || 'unknown'}@example.com`
                               if (!email) return "-"
                               // stu14@naver.com 형태를 st**4@naver.com으로 변환
                               const atIndex = email.indexOf('@')
                               if (atIndex > 2) {
                                 const beforeAt = email.substring(0, atIndex)
                                 const afterAt = email.substring(atIndex)
                                 if (beforeAt.length > 3) {
                                   return `${beforeAt.substring(0, 2)}**${beforeAt.substring(beforeAt.length - 1)}${afterAt}`
                                 }
                                 return `${beforeAt.substring(0, 1)}**${afterAt}`
                               }
                               return email
                             })()}
                           </td>
                           <td className="py-3 px-4 font-medium text-center" style={{ color: "#2C3E50" }}>
                             {(() => {
                               const name = attendance.memberName || attendance.studentName || `학생 (${attendance.userId?.slice(-8) || 'ID 없음'})`
                               if (!name) return "-"
                               // (ict)박서연 형태를 (ict)박서*으로 변환
                               const match = name.match(/^(\([^)]+\))(.+)$/)
                               if (match) {
                                 const prefix = match[1] // (ict)
                                 const actualName = match[2] // 박서연
                                 if (actualName.length > 1) {
                                   return `${prefix}${actualName.slice(0, -1)}*`
                                 }
                                 return `${prefix}*`
                               }
                               // 괄호가 없는 경우 마지막 글자만 마스킹
                               if (name.length > 1) {
                                 return `${name.slice(0, -1)}*`
                               }
                               return name
                             })()}
                           </td>
                           <td className="py-3 px-4 text-center" style={{ color: "#95A5A6" }}>
                             {attendance.courseName || attendance.classroomInfo || "과정 정보 없음"}
                           </td>
                          <td className="py-3 px-4 text-center">
                            <div className="flex items-center justify-center space-x-2">
                              {getStatusIcon(attendance.attendanceStatus)}
                              <Badge className="text-white" style={{ backgroundColor: getStatusColor(attendance.attendanceStatus) }}>
                                {attendance.attendanceStatus}
                              </Badge>
                            </div>
                          </td>
                          <td className="py-3 px-4 text-center font-mono text-sm" style={{ color: "#95A5A6" }}>
                            {attendance.checkIn ? 
                              (attendance.checkIn.includes('T') ? 
                                new Date(attendance.checkIn).toLocaleTimeString('ko-KR', {
                                  hour: '2-digit',
                                  minute: '2-digit',
                                  second: '2-digit'
                                }) : 
                                attendance.checkIn
                              ) : "-"
                            }
                          </td>
                          <td className="py-3 px-4 text-center font-mono text-sm" style={{ color: "#95A5A6" }}>
                            {attendance.checkOut ? 
                              (attendance.checkOut.includes('T') ? 
                                new Date(attendance.checkOut).toLocaleTimeString('ko-KR', {
                                  hour: '2-digit',
                                  minute: '2-digit',
                                  second: '2-digit'
                                }) : 
                                attendance.checkOut
                              ) : "-"
                            }
                          </td>
                          <td className="py-3 px-4 text-center text-sm" style={{ color: "#95A5A6" }}>
                            {attendance.lectureDate ? 
                              attendance.lectureDate.replace(/^\d{4}-/, '') : 
                              '-'
                            }
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>

                  {getPaginatedData().length === 0 && (
                    <div className="text-center py-8">
                      <p style={{ color: "#95A5A6" }}>검색 결과가 없습니다.</p>
                    </div>
                  )}
                                 </div>
                 )}
               </CardContent>
            </Card>

            {/* 페이징 컴포넌트 */}
            {!loading && !error && getTotalPages() > 1 && (
              <div className="flex justify-center items-center mt-6 space-x-2">
                {/* 이전 페이지 버튼 */}
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 1}
                  className="px-3 py-1 transition-colors duration-200"
                  style={{ 
                    borderColor: "#1ABC9C", 
                    color: "#1ABC9C",
                    opacity: currentPage === 1 ? 0.5 : 1
                  }}
                  onMouseEnter={e => {
                    if (currentPage !== 1) {
                      e.currentTarget.style.backgroundColor = '#1ABC9C';
                      e.currentTarget.style.color = '#fff';
                    }
                  }}
                  onMouseLeave={e => {
                    if (currentPage !== 1) {
                      e.currentTarget.style.backgroundColor = 'transparent';
                      e.currentTarget.style.color = '#1ABC9C';
                    }
                  }}
                >
                  이전
                </Button>

                {/* 페이지 번호들 */}
                {Array.from({ length: getTotalPages() }, (_, i) => i + 1).map((page) => (
                  <Button
                    key={page}
                    variant={currentPage === page ? "default" : "outline"}
                    size="sm"
                    onClick={() => handlePageChange(page)}
                    className="px-3 py-1 transition-colors duration-200"
                    style={{
                      backgroundColor: currentPage === page ? "#1ABC9C" : "transparent",
                      borderColor: "#1ABC9C",
                      color: currentPage === page ? "white" : "#1ABC9C"
                    }}
                    onMouseEnter={e => {
                      if (currentPage !== page) {
                        e.currentTarget.style.backgroundColor = '#1ABC9C';
                        e.currentTarget.style.color = '#fff';
                      }
                    }}
                    onMouseLeave={e => {
                      if (currentPage !== page) {
                        e.currentTarget.style.backgroundColor = 'transparent';
                        e.currentTarget.style.color = '#1ABC9C';
                      }
                    }}
                  >
                    {page}
                  </Button>
                ))}

                {/* 다음 페이지 버튼 */}
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage === getTotalPages()}
                  className="px-3 py-1 transition-colors duration-200"
                  style={{ 
                    borderColor: "#1ABC9C", 
                    color: "#1ABC9C",
                    opacity: currentPage === getTotalPages() ? 0.5 : 1
                  }}
                  onMouseEnter={e => {
                    if (currentPage !== getTotalPages()) {
                      e.currentTarget.style.backgroundColor = '#1ABC9C';
                      e.currentTarget.style.color = '#fff';
                    }
                  }}
                  onMouseLeave={e => {
                    if (currentPage !== getTotalPages()) {
                      e.currentTarget.style.backgroundColor = 'transparent';
                      e.currentTarget.style.color = '#1ABC9C';
                    }
                  }}
                >
                  다음
                </Button>
              </div>
            )}

            {/* 출석 정보 수정 모달 */}
                {isModalOpen && selectedStudent && (
                  <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center z-50">
                    <Card className="w-full max-w-2xl mx-4 max-h-[90vh] overflow-y-auto">
                      <CardHeader>
                        <div className="flex items-center justify-between">
                          <CardTitle style={{ color: "#2C3E50" }}>출석 정보 수정 - {selectedStudent.memberName || selectedStudent.studentName}</CardTitle>
                          <Button variant="ghost" size="sm" onClick={handleModalClose} style={{ color: "#95A5A6" }}>
                            ✕
                          </Button>
                        </div>
                      </CardHeader>
                      <CardContent className="space-y-6">
                        {/* 학생 기본 정보 */}
                        <div className="bg-gray-50 p-4 rounded-lg">
                          <h3 className="font-semibold mb-3" style={{ color: "#2C3E50" }}>
                            학생 정보
                          </h3>
                          <div className="grid grid-cols-2 gap-4 text-sm">
                            <div>
                              <span className="font-medium" style={{ color: "#2C3E50" }}>
                                이름:
                              </span>
                              <span className="ml-2" style={{ color: "#95A5A6" }}>
                                {selectedStudent.memberName || selectedStudent.studentName}
                              </span>
                            </div>
                            <div>
                              <span className="font-medium" style={{ color: "#2C3E50" }}>
                                과정:
                              </span>
                              <span className="ml-2" style={{ color: "#95A5A6" }}>
                                {selectedStudent.courseName}
                              </span>
                            </div>
                            <div>
                              <span className="font-medium" style={{ color: "#2C3E50" }}>
                                이메일:
                              </span>
                              <span className="ml-2" style={{ color: "#95A5A6" }}>
                                {selectedStudent.memberEmail || selectedStudent.email}
                              </span>
                            </div>
                            <div>
                              <span className="font-medium" style={{ color: "#2C3E50" }}>
                                날짜:
                              </span>
                              <span className="ml-2" style={{ color: "#95A5A6" }}>
                                {selectedStudent.lectureDate}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* 출석 정보 수정 */}
                        <div className="space-y-4">
                          <h3 className="font-semibold" style={{ color: "#2C3E50" }}>
                            출석 정보 수정
                          </h3>

                          <div className="space-y-4">
                            <div className="space-y-2">
                              <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                출석 상태
                              </label>
                              <select
                                value={editFormData.status}
                                onChange={(e) => handleFormChange("status", e.target.value)}
                                className="w-full px-3 py-2 border rounded-md"
                                style={{ borderColor: "#95A5A6" }}
                              >
                                <option value="출석">출석</option>
                                <option value="공가">공가</option>
                                <option value="지각">지각</option>
                                <option value="결석">결석</option>
                                <option value="조퇴">조퇴</option>
                              </select>
                            </div>

                            <div className="space-y-2">
                              <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                등교 날짜/시간
                              </label>
                              <div className="flex gap-2">
                                <input
                                  type="date"
                                  value={editFormData.checkInDate || selectedStudent?.lectureDate || ""}
                                  onChange={(e) => handleFormChange("checkInDate", e.target.value)}
                                  className="flex-1 px-3 py-2 border rounded-md"
                                  style={{ borderColor: "#95A5A6" }}
                                />
                                <input
                                  type="time"
                                  value={editFormData.checkInTime || ""}
                                  onChange={(e) => handleFormChange("checkInTime", e.target.value)}
                                  className="flex-1 px-3 py-2 border rounded-md"
                                  style={{ borderColor: "#95A5A6" }}
                                />
                              </div>
                            </div>

                            <div className="space-y-2">
                              <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                하교 날짜/시간
                              </label>
                              <div className="flex gap-2">
                                <input
                                  type="date"
                                  value={editFormData.checkOutDate || selectedStudent?.lectureDate || ""}
                                  onChange={(e) => handleFormChange("checkOutDate", e.target.value)}
                                  className="flex-1 px-3 py-2 border rounded-md"
                                  style={{ borderColor: "#95A5A6" }}
                                />
                                <input
                                  type="time"
                                  value={editFormData.checkOutTime || ""}
                                  onChange={(e) => handleFormChange("checkOutTime", e.target.value)}
                                  className="flex-1 px-3 py-2 border rounded-md"
                                  style={{ borderColor: "#95A5A6" }}
                                />
                              </div>
                            </div>
                          </div>

                          <div className="space-y-2">
                            <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                              비고 사항
                            </label>
                            <textarea
                              value={editFormData.note}
                              onChange={(e) => handleFormChange("note", e.target.value)}
                              placeholder="출석 관련 특이사항을 입력하세요..."
                              className="w-full px-3 py-2 border rounded-md resize-none"
                              style={{ borderColor: "#95A5A6" }}
                              rows={3}
                            />
                          </div>
                        </div>

                        {/* 버튼 그룹 */}
                        <div className="flex justify-end space-x-4 pt-4 border-t">
                          <Button
                            variant="outline"
                            onClick={handleModalClose}
                            className="bg-transparent text-[#95A5A6] hover:bg-gray-100 border border-[#95A5A6]"
                          >
                            취소
                          </Button>
                          <Button
                            onClick={handleSaveChanges}
                            className="text-white font-medium text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                          >
                            저장
                          </Button>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                )}
          </div>
        </main>
      </div>
    </PageLayout>
  )
}

