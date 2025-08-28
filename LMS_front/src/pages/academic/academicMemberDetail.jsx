import { useState, useEffect } from "react"
import { useSearchParams, useParams, useNavigate } from "react-router-dom"
import { ArrowLeft, Mail, Phone, Calendar, User, GraduationCap, MapPin, Edit, PlusCircle, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getMemberDetail, updateMember, registerCourse, getAvailableCourses } from "@/api/hancw/staffAcademicAxios"
import { getMenuItems } from "@/components/ui/menuConfig"
import { baseURL } from "@/components/auth/http"
import { jwtDecode } from "jwt-decode"

function getCookie(name) {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) return parts.pop().split(";").shift()
}

export default function MemberDetailPage() { 
  const { id } = useParams()
  const navigate = useNavigate()

  const sidebarMenuItems = getMenuItems("academic")

  const [student, setStudent] = useState(null)
  const [studentList, setStudentList] = useState([])
  const [selectedStudentIndex, setSelectedStudentIndex] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [currentUser, setCurrentUser] = useState(null)
  const [educationId, setEducationId] = useState(null)

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [courseList, setCourseList] = useState([])
  const [formData, setFormData] = useState({})
  const [selectedCourses, setSelectedCourses] = useState([]) // 🔹 선택된 과정 ID 배열

  // JWT 토큰에서 educationId 추출
  const extractEducationIdFromToken = () => {
    try {
      const token = getCookie('refresh')
      
      if (token) {
        const decoded = jwtDecode(token)
        console.log('JWT 토큰 디코딩 결과:', decoded)
        console.log('추출된 educationId:', decoded.educationId)
        return decoded.educationId
      } else {
        console.error('JWT 토큰을 쿠키에서 찾을 수 없습니다.')
        console.log('현재 쿠키들:', document.cookie)
      }
    } catch (error) {
      console.error('JWT 토큰 디코딩 오류:', error)
    }
    return null
  }

  useEffect(() => {
    const userData = localStorage.getItem("currentUser")
    if (userData) {
      setCurrentUser(JSON.parse(userData))
    }
    
    // JWT 토큰에서 educationId 추출
    const extractedEducationId = extractEducationIdFromToken()
    setEducationId(extractedEducationId)
  }, [])

  const getMemberData = async () => {
    if (!id || !currentUser?.memberId) return
    setLoading(true)
    setError(null)
    try {
      // educationId를 포함하여 회원 상세 정보 조회
      const data = await getMemberDetail(id, currentUser.memberId, educationId)
      console.log("받은 회원 데이터:", data)
  
      // memberRole에 따라 다른 처리
      if ((data?.memberRole === 'ROLE_STUDENT' || data?.memberRole === 'ROLE_INSTRUCTOR') && data?.courses?.length > 0) {
        // 학생/강사인 경우: courses 배열 처리
        setStudentList(data.courses)
        setStudent({
          ...data,
          courseId: data.courses[0].courseId,
          courseName: data.courses[0].courseName,
          educationId: data.courses[0].educationId,
          memberExpired: data.courses[0].memberExpired,
          status: data.courses[0].status,
        })
      } else {
        // 직원인 경우 또는 과정 정보가 없는 학생/강사
        setStudentList([])
        setStudent(data)
      }
      setSelectedStudentIndex(0)
    } catch (err) {
      console.error("API 호출 실패:", err)
      setError("회원 정보를 불러오는데 실패했습니다.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (currentUser?.memberId && id && educationId) getMemberData()
  }, [currentUser, id, educationId])

  const [searchParams] = useSearchParams()
  const editMode = searchParams.get("edit") === "true"

  const [isEditing, setIsEditing] = useState(editMode)

  const handleEdit = () => {
    setIsEditing(true)
    // 백엔드 필드명을 프론트엔드 필드명으로 매핑하여 formData 설정
    setFormData({
      ...student,
      birthDate: student?.memberBirthday || student?.birthDate,
      address: student?.memberAddress || student?.address,
    })
  }

  // student 데이터가 로드되면 formData도 업데이트
  useEffect(() => {
    if (student) {
      // 백엔드 필드명을 프론트엔드 필드명으로 매핑
      setFormData({
        ...student,
        birthDate: student.memberBirthday || student.birthDate, // memberBirthday를 birthDate로 매핑
        address: student.memberAddress || student.address, // memberAddress를 address로 매핑
      })
    }
  }, [student])

  const handleSave = async () => {
    try {
      console.log('=== 저장 시작 ===')
      console.log('회원 ID:', id)
      console.log('회원 역할:', student?.memberRole)
      console.log('현재 formData:', formData)
      
      // 백엔드 필드명으로 변환
      const backendData = {
        ...formData,
        memberBirthday: formData.birthDate, // birthDate를 memberBirthday로 변환
        memberAddress: formData.address, // address를 memberAddress로 변환
      }
      
      if (student?.memberRole === 'ROLE_STUDENT' || student?.memberRole === 'ROLE_INSTRUCTOR') {
        // 학생/강사인 경우: 먼저 기본 정보 업데이트, 그 다음 과정별 정보 업데이트
        console.log(`${student?.memberRole === 'ROLE_STUDENT' ? '학생' : '강사'} - 기본 정보 업데이트 시작`)
        console.log('기본 정보 데이터:', backendData)
        await updateMember(id, backendData)
        
        console.log(`${student?.memberRole === 'ROLE_STUDENT' ? '학생' : '강사'} - 과정별 업데이트 시작`)
        for (const courseStudent of studentList) {
          console.log('과정별 데이터:', courseStudent)
          await updateMember(id, courseStudent)
        }
      } else {
        // 직원인 경우: 전체 회원 정보 업데이트
        console.log('직원 - 전체 정보 업데이트')
        console.log('전송할 데이터:', backendData)
        await updateMember(id, backendData)
      }
      alert(`${student?.memberName} 회원의 모든 정보가 수정되었습니다.`)
      setIsEditing(false)
      getMemberData()
    } catch (err) {
      alert("회원 정보 수정에 실패했습니다.")
      console.error("회원 정보 수정 실패:", err)
    }
  }

  const handleStudentSelect = (index) => {
    setSelectedStudentIndex(index)
    
    // 원본 회원 데이터를 유지하면서 선택된 과정 정보만 업데이트
    const selectedCourse = studentList[index]
    const updatedStudent = {
      ...student, // 원본 회원 데이터 유지
      courseId: selectedCourse.courseId,
      courseName: selectedCourse.courseName,
      educationId: selectedCourse.educationId,
      memberExpired: selectedCourse.memberExpired,
      status: selectedCourse.status,
    }
    setStudent(updatedStudent)
    
    // formData도 동일하게 업데이트
    setFormData(prev => ({
      ...prev, // 기존 회원 정보 유지
      courseId: selectedCourse.courseId,
      courseName: selectedCourse.courseName,
      educationId: selectedCourse.educationId,
      memberExpired: selectedCourse.memberExpired,
      status: selectedCourse.status,
    }))
  }

  const handleCancel = () => {
    // 취소 시 원래 데이터로 복원 (백엔드 필드명 매핑 포함)
    if (student) {
      setFormData({
        ...student,
        birthDate: student.memberBirthday || student.birthDate,
        address: student.memberAddress || student.address,
      })
    }
    setIsEditing(false)
  }

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  const handleMemberToggle = () => {
    console.log('=== 활성화/비활성화 토글 ===')
    console.log('현재 activated:', student?.activated)
    console.log('현재 formData:', formData)
    
    // activated 값 토글 (1 → 0, 0 → 1)
    const newActivated = student?.activated === 1 ? 0 : 1
    console.log('새로운 activated 값:', newActivated)
    
    // 로컬 상태만 업데이트 (백엔드 저장은 저장 버튼에서 처리)
    setStudent({ ...student, activated: newActivated })
    setFormData({ ...formData, activated: newActivated })
    
    console.log('활성화 상태 로컬 업데이트 완료:', newActivated === 1 ? '활성화' : '비활성화')
  }

  const handleCourseToggle = (courseIndex) => {
    const updatedStudentList = [...studentList]
    const courseStudent = updatedStudentList[courseIndex]
    if (courseStudent.memberExpired) {
      updatedStudentList[courseIndex] = { ...courseStudent, memberExpired: null, status: "재학중" }
    } else {
      updatedStudentList[courseIndex] = {
        ...courseStudent,
        memberExpired: new Date().toISOString(),
        status: "퇴학",
      }
    }
    setStudentList(updatedStudentList)
    if (selectedStudentIndex === courseIndex) {
      // 원본 회원 데이터를 유지하면서 업데이트된 과정 정보만 적용
      const updatedCourse = updatedStudentList[courseIndex]
      const updatedStudent = {
        ...student, // 원본 회원 데이터 유지
        courseId: updatedCourse.courseId,
        courseName: updatedCourse.courseName,
        educationId: updatedCourse.educationId,
        memberExpired: updatedCourse.memberExpired,
        status: updatedCourse.status,
      }
      setStudent(updatedStudent)
      
      setFormData(prev => ({
        ...prev, // 기존 회원 정보 유지
        courseId: updatedCourse.courseId,
        courseName: updatedCourse.courseName,
        educationId: updatedCourse.educationId,
        memberExpired: updatedCourse.memberExpired,
        status: updatedCourse.status,
      }))
    }
  }

  const handleCourseAdd = () => {
    setSelectedCourses([]) // 모달 열 때 초기화
    setIsModalOpen(true)
  }

  const closeModal = () => {
    setIsModalOpen(false)
  }

  const toggleCourseSelection = (courseId) => {
    setSelectedCourses((prev) =>
      prev.includes(courseId) ? prev.filter((id) => id !== courseId) : [...prev, courseId]
    )
  }

  const handleApplyCourses = async () => {
    try {
      // 현재 상세 화면에 있는 학생의 userId (URL 파라미터에서 직접 가져옴)
      const targetStudentUserId = id // URL 파라미터에서 가져온 학생 ID를 직접 사용
      
      // 기존에 수강 중인 모든 과정의 courseId들을 배열로 수집
      const existingCourseIds = studentList.map(course => course.courseId)
      
      // 선택된 과정 정보 추출 - 배열 형태로 변경
      const selectedCourseObjects = courseList
        .filter(course => selectedCourses.includes(course.courseId))
        .map(course => ({
          userId: targetStudentUserId, // 상세 화면에 있는 학생의 userId
          courseId: course.courseId,
          courseName: course.courseName,
          memberEmail: formData.email || student?.email,
          existingCourseIds: existingCourseIds
        }))
  
      console.log('=== 과정 신청 디버깅 정보 ===')
      console.log('URL 파라미터 id (상세 화면 학생 ID):', id)
      console.log('student?.memberId (학생의 실제 ID):', student?.memberId)
      console.log('currentUser?.memberId (현재 로그인한 사용자 ID):', currentUser?.memberId)
      console.log('targetStudentUserId (사용할 ID):', targetStudentUserId)
      console.log('전송할 데이터:', selectedCourseObjects)
      console.log('기존 수강 과정 courseIds:', existingCourseIds)
      console.log('새로 신청할 과정 courseIds:', selectedCourses)
      console.log('=== 과정 신청 전 최종 확인 ===')
      console.log('실제로 백엔드로 보낼 userId:', targetStudentUserId)
      console.log('이 값이 올바른 학생 ID인지 확인:', targetStudentUserId === '159272e7-2726-4d45-a0c7-f8de3117bb21')
  
      // 새로 만든 axios 함수 사용
      await registerCourse(selectedCourseObjects)
  
      alert(student?.memberRole === 'ROLE_STUDENT' ? "신청되었습니다" : "담당 과정이 추가되었습니다")
      closeModal()
      
      // 화면 새로고침
      window.location.reload()
    } catch (error) {
      console.error("수강 신청 실패:", error)
      alert(student?.memberRole === 'ROLE_STUDENT' ? "신청 실패: 관리자에게 문의하세요." : "담당 과정 추가 실패: 관리자에게 문의하세요.")
    }
  }

  useEffect(() => {
    const token = getCookie("refresh")
    if (!token) return

    const decoded = jwtDecode(token)
    if (!decoded?.userId) return

    setFormData((prev) => ({
      ...prev,
      // userId는 JWT에서 가져오지 않고, URL 파라미터의 학생 ID를 사용
      educationId: decoded.educationId
    }))
  }, [])

  useEffect(() => {
    if (!id) return // URL 파라미터에서 가져온 학생 ID가 없으면 리턴
    const fetchCourses = async () => {
      try {
        console.log('과정 목록 조회 시작 - 학생 userId:', id)
        const data = await getAvailableCourses(id) // 상세 화면에 있는 학생의 ID 사용
        console.log('백엔드에서 받은 과정 데이터:', data)
        const courseData = Array.isArray(data.availableCourses) ? data.availableCourses : []
        console.log('처리된 과정 목록:', courseData)
        setCourseList(courseData)
      } catch (err) {
        console.error("과정 불러오기 실패:", err)
        setCourseList([])
      }
    }
    fetchCourses()
  }, [id]) // formData.userId 대신 id 사용

  return (
    <PageLayout currentPage="academic" userRole="staff">
      <div className="flex">
        <Sidebar title="회원 정보" menuItems={sidebarMenuItems} currentPath="/academic/students" />

        <main className="flex-1 p-8">
          <div className="max-w-7xl mx-auto">
            {loading && <div className="text-center py-8"><p style={{ color: "#95A5A6" }}>회원 정보를 불러오는 중...</p></div>}
            {error && (
              <div className="text-center py-8">
                <p style={{ color: "#e74c3c" }}>{error}</p>
                <Button onClick={getMemberData} className="mt-2" style={{ backgroundColor: "#1ABC9C" }}>다시 시도</Button>
              </div>
            )}
            {!loading && !error && student && (
              <>
                {/* 헤더 */}
                <div className="mb-8">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                      <Button variant="ghost" size="sm" className="flex items-center space-x-2 hover:bg-gray-100" onClick={() => navigate("/academic/students")}>
                        <ArrowLeft className="w-4 h-4" /><span>목록으로</span>
                      </Button>
                      <div>
                        <h1 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>회원 상세 정보</h1>
                        <p className="text-lg" style={{ color: "#95A5A6" }}>{student?.memberName}님의 상세 정보입니다.</p>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2">
                      {isEditing ? (
                        <>
                          <Button onClick={handleCancel} variant="outline" className="flex items-center space-x-2 bg-transparent hover:bg-gray-200">취소</Button>
                          <Button onClick={handleSave} className="text-white bg-[#1ABC9C] hover:bg-[rgb(10,150,120)] font-medium flex items-center space-x-2">저장</Button>
                        </>
                      ) : (
                        <>
                          <Button onClick={handleEdit} className="text-white bg-[#1abc9c] font-medium flex items-center space-x-2 hover:bg-[rgb(10,150,120)]">
                            <Edit className="w-4 h-4" /><span>정보 수정</span>
                          </Button>
                          {student?.memberRole === 'ROLE_STUDENT' && (
                            <Button onClick={handleCourseAdd} className="text-white bg-[#1abc9c] font-medium flex items-center space-x-2 hover:bg-[rgb(10,150,120)]">
                              <PlusCircle className="w-4 h-4" /><span>과정 신청</span>
                            </Button>
                          )}
                        </>
                      )}
                    </div>
                  </div>
                </div>
                {/* 학생 강사 직원 정보 카드 */}
                <Card>
                  <CardHeader><CardTitle style={{ color: "#2C3E50" }}>회원 정보</CardTitle></CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                      {/* 과정 목록 */}
                      <div className="space-y-4">
                                                 <div className="text-center mb-6">
                           <div className="w-20 h-20 rounded-full mx-auto mb-4 flex items-center justify-center text-2xl font-bold text-white" style={{ backgroundColor: "#1ABC9C" }}>
                           </div>
                           {isEditing ? (
                             <input type="text" value={formData?.memberName || ""} onChange={(e) => handleInputChange("memberName", e.target.value)}
                               className="text-xl font-bold text-center bg-transparent border-b-2 border-emerald-500 focus:outline-none" style={{ color: "#2C3E50" }} />
                           ) : (
                             <h3 className="text-xl font-bold" style={{ color: "#2C3E50" }}>{formData?.memberName || ""}</h3>
                           )}
                                                                                 <div className="flex items-center justify-center space-x-2 mt-2">
                              {isEditing ? (
                                <button
                                  onClick={() => handleMemberToggle()}
                                  className={`px-3 py-1 rounded-full text-sm font-medium transition-colors cursor-pointer ${
                                    student?.activated === 0 
                                      ? "text-white bg-red-500 hover:bg-red-600" 
                                      : "text-white bg-green-500 hover:bg-green-600"
                                  }`}
                                >
                                  {student?.activated === 0 ? "비활성화" : "활성화"}
                                </button>
                              ) : (
                                <Badge 
                                  className={`text-white cursor-pointer ${isEditing ? 'hover:opacity-80' : ''}`} 
                                  style={{ backgroundColor: student?.activated === 0 ? "#e74c3c" : "#1ABC9C" }}
                                  onClick={isEditing ? () => handleMemberToggle() : undefined}
                                >
                                  {student?.activated === 0 ? "비활성화" : "활성화"}
                                </Badge>
                              )}
                            </div>
                        </div>
                        {(student?.memberRole === 'ROLE_STUDENT' || student?.memberRole === 'ROLE_INSTRUCTOR') && (
                          <div className="flex items-start space-x-3">
                            <GraduationCap className="w-4 h-4 mt-1" style={{ color: "#95A5A6" }} />
                            <div className="flex-1">
                              <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                {student?.memberRole === 'ROLE_STUDENT' ? '수강 과정' : '담당 과정'} ({studentList.length}개)
                              </p>
                              <div className="space-y-2 mt-2">
                                {studentList.map((studentItem, index) => (
                                  <div key={index} className={`p-2 rounded-lg border cursor-pointer transition-colors ${selectedStudentIndex === index ? "border-emerald-500 bg-emerald-50" : "border-gray-200 hover:border-gray-300"}`} onClick={() => handleStudentSelect(index)}>
                                    <div className="flex items-center justify-between">
                                      <div>
                                        <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>{studentItem.courseName || "과정 없음"}</p>
                                      </div>
                                                                             <div className="flex items-center space-x-2">
                                                                                   {student?.memberRole === 'ROLE_STUDENT' && (
                                            <>
                                              {isEditing ? (
                                                <button onClick={(e) => { e.stopPropagation(); handleCourseToggle(index) }}
                                                  className={`px-2 py-1 rounded-full text-xs font-medium transition-colors ${studentItem.memberExpired ? "text-red-600 bg-red-50 hover:bg-red-100" : "text-green-600 bg-green-50 hover:bg-green-100"}`}>
                                                  {studentItem.memberExpired ? "비활성화" : "활성화"}
                                                </button>
                                              ) : (
                                                <Badge className={`text-xs font-medium ${studentItem.memberExpired ? "text-red-600 bg-red-50" : "text-green-600 bg-green-50"}`}>
                                                  {studentItem.memberExpired ? "비활성화" : "활성화"}
                                                </Badge>
                                              )}
                                            </>
                                          )}
                                       </div>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                      {/* 정보 영역 */}
                      <div>
                        <h4 className="text-lg font-semibold mb-4" style={{ color: "#2C3E50" }}>정보</h4>
                        <div className="space-y-4">
                          <div className="flex items-center space-x-3">
                            <Phone className="w-4 h-4" style={{ color: "#95A5A6" }} />
                            <div className="flex-1">
                              <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>휴대폰</p>
                              {isEditing ? (<input type="text" value={formData?.phone || ""} onChange={(e) => handleInputChange("phone", e.target.value)} className="text-sm bg-transparent border-b border-gray-300 focus:border-emerald-500 focus:outline-none w-full" style={{ color: "#95A5A6" }} />) : (<p className="text-sm" style={{ color: "#95A5A6" }}>{student?.phone || formData?.phone || "-"}</p>)}
                            </div>
                          </div>
                          <div className="flex items-center space-x-3">
                            <Mail className="w-4 h-4" style={{ color: "#95A5A6" }} />
                            <div className="flex-1">
                              <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>이메일</p>
                              {isEditing ? (<input type="email" value={formData?.email || ""} onChange={(e) => handleInputChange("email", e.target.value)} className="text-sm bg-transparent border-b border-gray-300 focus:border-emerald-500 focus:outline-none w-full" style={{ color: "#95A5A6" }} />) : (<p className="text-sm" style={{ color: "#95A5A6" }}>{student?.email || formData?.email || "-"}</p>)}
                            </div>
                          </div>
                          <div className="flex items-center space-x-3">
                            <MapPin className="w-4 h-4" style={{ color: "#95A5A6" }} />
                            <div className="flex-1">
                              <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>주소</p>
                              {isEditing ? (<input type="text" value={formData?.address || ""} onChange={(e) => handleInputChange("address", e.target.value)} className="text-sm bg-transparent border-b border-gray-300 focus:border-emerald-500 focus:outline-none w-full" style={{ color: "#95A5A6" }} />) : (<p className="text-sm" style={{ color: "#95A5A6" }}>{student?.address || formData?.address || "-"}</p>)}
                            </div>
                          </div>
                                                    <div className="flex items-center space-x-3">
                            <Calendar className="w-4 h-4" style={{ color: "#95A5A6" }} />
                            <div><p className="text-sm font-medium" style={{ color: "#2C3E50" }}>입학일</p><p className="text-sm" style={{ color: "#95A5A6" }}>{student?.enrollmentDate}</p></div>
                          </div>
                          <div className="flex items-center space-x-3">
                            <User className="w-4 h-4" style={{ color: "#95A5A6" }} />
                            <div className="flex-1">
                              <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>생년월일</p>
                              {isEditing ? (
                                <input 
                                  type="date" 
                                  value={formData?.birthDate || ""} 
                                  onChange={(e) => handleInputChange("birthDate", e.target.value)} 
                                  className="text-sm bg-transparent border-b border-gray-300 focus:border-emerald-500 focus:outline-none w-full" 
                                  style={{ color: "#95A5A6" }} 
                                />
                              ) : (
                                <p className="text-sm" style={{ color: "#95A5A6" }}>{student?.birthDate || formData?.birthDate || "-"}</p>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </>
            )}
          </div>
        </main>
      </div>

      {/* 모달 */}
      {isModalOpen && (
        <div className="fixed inset-0 flex items-center justify-center bg-[rgba(0,0,0,0.5)] z-50">
          <div className="bg-white rounded-lg shadow-lg w-150 h-150 p-6        relative">
            <h2 className="text-xl font-bold mb-4">
              {student?.memberRole === 'ROLE_STUDENT' ? '수강 신청 가능 과정' : '담당 가능 과정'}
            </h2>
            <ul className="space-y-2 max-h-150 overflow-y-auto">
              {courseList.length > 0 ? (
                courseList.map((course) => (
                  <li
                    key={course.courseId}
                    className={`p-2 border rounded cursor-pointer transition-colors ${
                      selectedCourses.includes(course.courseId)
                        ? "bg-emerald-100 border-emerald-400"
                        : "hover:bg-emerald-50"
                    }`}
                    onClick={() => toggleCourseSelection(course.courseId)}
                  >
                    {course.courseName}
                  </li>
                ))
              ) : (
                <li className="text-gray-500 text-sm">
                  {student?.memberRole === 'ROLE_STUDENT' ? '신청 가능한 과정이 없습니다.' : '담당 가능한 과정이 없습니다.'}
                </li>
              )}
            </ul>
            <div className="mt-4 flex justify-end space-x-2">
              <Button
                onClick={closeModal}
                className="hover:bg-gray-100 font-medium border border-gray-300"
              >
                닫기
              </Button>
              <Button
                disabled={selectedCourses.length === 0}
                onClick={handleApplyCourses}
                className={`text-white font-medium ${
                  selectedCourses.length === 0
                    ? "bg-gray-300 cursor-not-allowed"
                    : "bg-emerald-500 hover:bg-emerald-600"
                }`}
              >
                {student?.memberRole === 'ROLE_STUDENT' ? '신청' : '담당'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </PageLayout>
  )
}
