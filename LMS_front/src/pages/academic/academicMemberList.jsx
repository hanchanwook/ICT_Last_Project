import { useState, useEffect, useRef } from "react"
import { Search, ChevronDown } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { useNavigate, useSearchParams } from "react-router-dom"
import { getMemberList } from "@/api/hancw/staffAcademicAxios"
import { getMenuItems } from "@/components/ui/menuConfig"
import { jwtDecode } from "jwt-decode"

export default function MemberListPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [memberOption, setMemberOption] = useState("all")
  const [educationId, setEducationId] = useState(null)
  const [activeTab, setActiveTab] = useState("전체") // "전체", "학생", "강사", "직원"
  
  // 페이징 상태
  const [currentPage, setCurrentPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(20)
  const [isItemsPerPageOpen, setIsItemsPerPageOpen] = useState(false)

  // 현재 사용자 정보
  const [currentUser, setCurrentUser] = useState(null)

  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const itemsPerPageRef = useRef(null)

  // JWT 토큰에서 educationId 추출
  const extractEducationIdFromToken = () => {
    try {
      // 쿠키에서 JWT 토큰 가져오기
      const getCookie = (name) => {
        const value = `; ${document.cookie}`
        const parts = value.split(`; ${name}=`)
        if (parts.length === 2) return parts.pop().split(';').shift()
        return null
      }
      
      // refresh 쿠키에서 JWT 토큰 가져오기
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

  // 전체 회원 데이터 조회
  const fetchMembersData = async () => {
    setLoading(true)
    setError(null)
    console.log("=== 전체 회원 데이터 조회 시작 ===")
    console.log("사용할 educationId:", educationId)
    
    try {
      // educationId를 포함하여 API 호출
      const data = await getMemberList(educationId)
      console.log("API 응답 데이터:", data)
      console.log("데이터 타입:", typeof data)
      console.log("배열 여부:", Array.isArray(data))
      
      const membersData = Array.isArray(data) ? data : (data?.members ?? [])
      console.log("전체 회원 데이터:", membersData)
      
      // memberRole에 따라 데이터 분류
      const students = membersData.filter(member => member.memberRole === 'ROLE_STUDENT')
      const instructors = membersData.filter(member => member.memberRole === 'ROLE_INSTRUCTOR')
      const employees = membersData.filter(member => member.memberRole === 'ROLE_STAFF')
      
      console.log("분류된 데이터 - 학생:", students.length, "강사:", instructors.length, "직원:", employees.length)
      
      // 강사 데이터 필드 확인
      if (instructors.length > 0) {
        console.log("첫 번째 강사 데이터 필드:", Object.keys(instructors[0]))
        console.log("첫 번째 강사 데이터:", instructors[0])
      }
      
      setStudentsData(students)
      setInstructorsData(instructors)
      setEmployeesData(employees)
      
    } catch (err) {
      console.error("API 호출 실패:", err)
      setError("회원 데이터를 불러오는데 실패했습니다.")
      setStudentsData([])
      setInstructorsData([])
      setEmployeesData([])
      console.error("회원 데이터 조회 실패:", err)
    } finally {
      setLoading(false)
      console.log("=== 전체 회원 데이터 조회 완료 ===")
    }
  }

  // 컴포넌트 마운트 시에만 데이터 조회
  useEffect(() => {
    // URL 파라미터에서 탭 정보 확인
    const tabParam = searchParams.get('tab')
    if (tabParam && ["전체", "학생", "강사", "직원"].includes(tabParam)) {
      setActiveTab(tabParam)
    }
    
    // JWT 토큰에서 educationId 추출
    const extractedEducationId = extractEducationIdFromToken()
    setEducationId(extractedEducationId)
    
    // educationId가 설정된 후에 데이터 조회
    if (extractedEducationId) {
      // educationId가 설정된 후에 데이터 호출
      setTimeout(() => {
        fetchMembersData()
      }, 100)
    } else {
      console.error('educationId를 찾을 수 없습니다.')
      setError("교육기관 정보를 찾을 수 없습니다.")
    }
  }, [searchParams])

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

  // 현재 사용자 정보 가져오기
  useEffect(() => {
    const userData = localStorage.getItem("currentUser")
    if (userData) {
      setCurrentUser(JSON.parse(userData))
    }
  }, [])

  const sidebarMenuItems = getMenuItems('academic')

  // 데이터 상태
  const [studentsData, setStudentsData] = useState([])
  const [instructorsData, setInstructorsData] = useState([])
  const [employeesData, setEmployeesData] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // 필터링된 학생 데이터
  const filteredStudents = studentsData
    // 중복 제거 (userId 기준)
    .filter((student, index, self) => 
      index === self.findIndex(s => s.userId === student.userId)
    )
    .filter((student) => {
      // 검색어 필터링
      const searchLower = searchTerm.toLowerCase()
      const matchesSearch = searchTerm === "" || 
        student.memberName?.toLowerCase().includes(searchLower) ||
        student.studentId?.toLowerCase().includes(searchLower) ||
        student.phone?.includes(searchTerm) ||
        student.email?.toLowerCase().includes(searchLower)

      // 검색 조건 필터링
      let matchesOption = true
      if (memberOption !== "all" && searchTerm !== "") {
        switch (memberOption) {
          case "memberName":
            matchesOption = student.memberName?.toLowerCase().includes(searchTerm.toLowerCase())
            break
          case "phone":
            matchesOption = student.phone?.includes(searchTerm)
            break
          default:
            matchesOption = true
        }
      }

      return matchesSearch && matchesOption
    })

  // 필터링된 강사 데이터
  const filteredInstructors = instructorsData.filter((instructor) => {
    const searchLower = searchTerm.toLowerCase()
    const matchesSearch = searchTerm === "" || 
      instructor.memberName?.toLowerCase().includes(searchLower) ||
      instructor.instructorId?.toLowerCase().includes(searchLower) ||
      instructor.phone?.includes(searchTerm) ||
      instructor.email?.toLowerCase().includes(searchLower) ||
      instructor.department?.toLowerCase().includes(searchLower) ||
      instructor.specialty?.toLowerCase().includes(searchLower)

    let matchesOption = true
    if (memberOption !== "all" && searchTerm !== "") {
      switch (memberOption) {
        case "instructorName":
          matchesOption = instructor.memberName?.toLowerCase().includes(searchTerm.toLowerCase())
          break
        case "phone":
          matchesOption = instructor.phone?.includes(searchTerm)
          break
        case "department":
          matchesOption = instructor.department?.toLowerCase().includes(searchTerm.toLowerCase())
          break
        default:
          matchesOption = true
      }
    }

    return matchesSearch && matchesOption
  })

  // 필터링된 직원 데이터
  const filteredEmployees = employeesData.filter((employee) => {
    const searchLower = searchTerm.toLowerCase()
    const matchesSearch = searchTerm === "" || 
      employee.memberName?.toLowerCase().includes(searchLower) ||
      employee.employeeId?.toLowerCase().includes(searchLower) ||
      employee.phone?.includes(searchTerm) ||
      employee.email?.toLowerCase().includes(searchLower) ||
      employee.department?.toLowerCase().includes(searchLower)

    let matchesOption = true
    if (memberOption !== "all" && searchTerm !== "") {
      switch (memberOption) {
        case "employeeName":
          matchesOption = employee.memberName?.toLowerCase().includes(searchTerm.toLowerCase())
          break
        case "phone":
          matchesOption = employee.phone?.includes(searchTerm)
          break
        case "department":
          matchesOption = employee.department?.toLowerCase().includes(searchTerm.toLowerCase())
          break
        default:
          matchesOption = true
      }
    }

    return matchesSearch && matchesOption
  })

  const handleView = (id) => {
    console.log("handleView 호출됨 - ID:", id, "타입:", typeof id);
    console.log("현재 활성 탭:", activeTab);
    
    if (!id) {
      console.error("Error: Attempted to navigate to member detail with undefined ID.");
      alert("회원 정보를 불러올 수 없습니다. 유효한 ID가 없습니다.");
      return;
    }
    
    console.log("네비게이션 실행:", `/academic/students/${id}`);
    // 모든 회원 타입을 하나의 경로로 통합
    navigate(`/academic/students/${id}`)
  }

  const getSearchPlaceholder = () => {
    if (activeTab === "전체") {
      return "이메일, 이름, 연락처로 검색..."
    } else if (activeTab === "학생") {
      return "이메일, 이름, 연락처로 검색..."
    } else if (activeTab === "강사") {
      return "이메일, 이름, 연락처로 검색..."
    } else {
      return "이메일, 이름, 연락처로 검색..."
    }
  }

  const getCurrentData = () => {
    if (activeTab === "전체") return [...filteredStudents, ...filteredInstructors, ...filteredEmployees]
    if (activeTab === "학생") return filteredStudents
    if (activeTab === "강사") return filteredInstructors
    return filteredEmployees
  }

  // 페이징된 데이터 계산
  const getPaginatedData = () => {
    const allData = getCurrentData()
    const startIndex = (currentPage - 1) * itemsPerPage
    const endIndex = startIndex + itemsPerPage
    return allData.slice(startIndex, endIndex)
  }

  // 총 페이지 수 계산
  const getTotalPages = () => {
    const totalItems = getCurrentData().length
    return Math.ceil(totalItems / itemsPerPage)
  }

  // 페이지 변경 핸들러
  const handlePageChange = (page) => {
    setCurrentPage(page)
  }

  // 탭 변경 시 페이지 초기화
  const handleTabChangeWithReset = (tab) => {
    setActiveTab(tab)
    setCurrentPage(1) // 페이지를 1로 초기화
    navigate(`/academic/students?tab=${tab}`, { replace: true })
  }

  // 검색어나 필터 변경 시 페이지 초기화
  useEffect(() => {
    setCurrentPage(1)
  }, [searchTerm, memberOption, itemsPerPage])

  const getCurrentCount = () => {
    if (activeTab === "전체") return filteredStudents.length + filteredInstructors.length + filteredEmployees.length
    if (activeTab === "학생") return filteredStudents.length
    if (activeTab === "강사") return filteredInstructors.length
    return filteredEmployees.length
  }

  const getCurrentDataKey = () => {
    // 모든 회원 타입이 userId를 사용
    return "userId"
  }

  const getCurrentNameField = () => {
    // 모든 회원 타입이 memberName을 사용
    return "memberName"
  }

  const getCurrentEmailField = () => {
    if (activeTab === "학생") return "email"
    if (activeTab === "강사") return "email"
    return "email"
  }

  const getCurrentPhoneField = () => {
    if (activeTab === "학생") return "phone"
    if (activeTab === "강사") return "phone"
    return "phone"
  }

  const getCurrentStatusField = () => {
    // activated 필드를 사용
    return "activated"
  }

  const getCurrentExtraField = () => {
    if (activeTab === "전체") return "memberRole" // 전체에서는 회원 타입을 표시
    if (activeTab === "학생") return "courseName"
    if (activeTab === "강사") return "courseName"
    return "" // 직원은 직책 컬럼 없음
  }

  const getCurrentExtraFieldLabel = () => {
    if (activeTab === "전체") return "회원 타입"
    if (activeTab === "학생") return "과정"
    if (activeTab === "강사") return "과정"
    return "" // 직원은 직책 컬럼 없음
  }

  // 탭 변경 시 URL 업데이트 (기존 함수는 제거하고 handleTabChangeWithReset 사용)

  return (
    <PageLayout currentPage="academic" userRole="staff">
      <div className="flex">
        <Sidebar title="회원 정보" menuItems={sidebarMenuItems} currentPath="/academic/students" />

        <main className="flex-1 p-8">
          <div className="max-w-7xl mx-auto">
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                전체 회원 목록
              </h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                등록된 모든 학생, 강사, 직원의 정보를 조회하고 관리할 수 있습니다.
              </p>
            </div>

            {/* 탭 네비게이션 */}
            <div className="mb-6">
              <div className="flex border-b" style={{ borderColor: "#95A5A6" }}>
                <button
                  onClick={() => handleTabChangeWithReset("전체")}
                  className={`px-6 py-3 font-medium text-sm transition-colors duration-200 ${
                    activeTab === "전체"
                      ? "border-b-2 text-blue-600"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                  style={{
                    borderBottomColor: activeTab === "전체" ? "#3498db" : "transparent"
                  }}
                >
                  전체 ({filteredStudents.length + filteredInstructors.length + filteredEmployees.length}명)
                </button>
                <button
                  onClick={() => handleTabChangeWithReset("학생")}
                  className={`px-6 py-3 font-medium text-sm transition-colors duration-200 ${
                    activeTab === "학생"
                      ? "border-b-2 text-blue-600"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                  style={{
                    borderBottomColor: activeTab === "학생" ? "#3498db" : "transparent"
                  }}
                >
                  학생 ({filteredStudents.length}명)
                </button>
                <button
                  onClick={() => handleTabChangeWithReset("강사")}
                  className={`px-6 py-3 font-medium text-sm transition-colors duration-200 ${
                    activeTab === "강사"
                      ? "border-b-2 text-blue-600"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                  style={{
                    borderBottomColor: activeTab === "강사" ? "#3498db" : "transparent"
                  }}
                >
                  강사 ({filteredInstructors.length}명)
                </button>
                <button
                  onClick={() => handleTabChangeWithReset("직원")}
                  className={`px-6 py-3 font-medium text-sm transition-colors duration-200 ${
                    activeTab === "직원"
                      ? "border-b-2 text-blue-600"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                  style={{
                    borderBottomColor: activeTab === "직원" ? "#3498db" : "transparent"
                  }}
                >
                  직원 ({filteredEmployees.length}명)
                </button>
              </div>
            </div>

            {/* 검색 및 필터 */}
            <Card className="mb-6">
              <CardContent>
                <div className="flex flex-col md:flex-row gap-4">
                  <div className="flex-1">
                    <div className="relative">
                      <Search
                        className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4"
                        style={{ color: "#95A5A6" }}
                      />
                      <Input
                        placeholder={getSearchPlaceholder()}
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="pl-10"
                      />
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <select
                      value={memberOption}
                      onChange={(e) => setMemberOption(e.target.value)}
                      className="px-3 py-2 border rounded-md"
                      style={{ borderColor: "#95A5A6" }}
                    >
                      <option value="all">검색 조건</option>
                      <option value={activeTab === "학생" ? "memberName" : activeTab === "강사" ? "instructorName" : "employeeName"}>
                        이름
                      </option>
                      <option value="phone">연락처</option>
                      {(activeTab === "강사" || activeTab === "직원") && (
                        <option value="department">부서</option>
                      )}
                    </select>

                    {/* 페이지당 항목 수 선택 */}
                    <div className="relative" ref={itemsPerPageRef}>
                      <button
                        onClick={() => setIsItemsPerPageOpen(!isItemsPerPageOpen)}
                        className="flex items-center justify-between px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white min-w-[120px]"
                        style={{ borderColor: "#95A5A6" }}
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
                </div>
              </CardContent>
            </Card>

            {/* 회원 목록 테이블 */}
            <Card>
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>
                  {activeTab} 목록 ({getCurrentCount()}명)
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
                       onClick={fetchMembersData}
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
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          이메일
                        </th>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          이름
                        </th>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          연락처
                        </th>
                        {getCurrentExtraFieldLabel() && (
                          <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                            {getCurrentExtraFieldLabel()}
                          </th>
                        )}
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          상태
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          상세보기
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {getPaginatedData().map((item, index) => (
                        <tr key={item[getCurrentDataKey()] || `member-${index}`} className="border-b hover:bg-gray-50" style={{ borderColor: "#f1f2f6" }}>
                                                     <td className="py-3 px-4 font-mono text-sm" style={{ color: "#2C3E50" }}>
                             {(() => {
                               const email = item[getCurrentEmailField()] || `user-${item[getCurrentDataKey()]?.slice(-8) || 'unknown'}@example.com`
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
                                                     <td className="py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                             {(() => {
                               const name = item[getCurrentNameField()]
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
                                                     <td className="py-3 px-4 font-mono text-sm" style={{ color: "#95A5A6" }}>
                             {(() => {
                               const phone = item[getCurrentPhoneField()]
                               if (!phone) return "-"
                               // 017-1234-1236 형태를 017-****-1236으로 변환
                               const parts = phone.split('-')
                               if (parts.length === 3) {
                                 return `${parts[0]}-****-${parts[2]}`
                               }
                               return phone
                             })()}
                           </td>
                          {getCurrentExtraFieldLabel() && (
                            <td className="py-3 px-4 text-sm" style={{ color: "#95A5A6" }}>
                              {activeTab === "전체" ? 
                                (item.memberRole === 'ROLE_STUDENT' ? '학생' : 
                                 item.memberRole === 'ROLE_INSTRUCTOR' ? '강사' : 
                                 item.memberRole === 'ROLE_STAFF' ? '직원' : '기타') :
                                (item[getCurrentExtraField()] || `${getCurrentExtraFieldLabel()} 정보 없음`)
                              }
                            </td>
                          )}
                                                     <td className="py-3 px-4">
                             <span
                               className={`px-2 py-1 rounded-full text-xs font-medium ${
                                 item[getCurrentStatusField()] === 0 ? "text-white" : "text-white"
                               }`}
                               style={{
                                 backgroundColor: item[getCurrentStatusField()] === 0 ? "#e74c3c" : "#1ABC9C",
                               }}
                             >
                               {item[getCurrentStatusField()] === 0 ? "비활성화" : "활성화"}
                             </span>
                           </td>
                          <td className="py-3 px-4">
                            <div className="flex justify-center">
                              <Button 
                                size="sm" 
                                variant="outline" 
                                onClick={() => handleView(item[getCurrentDataKey()])}
                                className="text-sm transition-colors duration-200"
                                style={{ borderColor: "#1ABC9C", color: "#1ABC9C" }}
                                onMouseEnter={e => {
                                  e.currentTarget.style.backgroundColor = '#1ABC9C';
                                  e.currentTarget.style.color = '#fff';
                                }}
                                onMouseLeave={e => {
                                  e.currentTarget.style.backgroundColor = 'transparent';
                                  e.currentTarget.style.color = '#1ABC9C';
                                }}
                              >
                                상세보기
                              </Button>
                            </div>
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

            {/* 페이지 정보 표시 */}
            {!loading && !error && getCurrentData().length > 0 && (
              <div className="text-center mt-4">
                <p style={{ color: "#95A5A6" }}>
                  {((currentPage - 1) * itemsPerPage) + 1} - {Math.min(currentPage * itemsPerPage, getCurrentData().length)} / {getCurrentData().length}명
                </p>
              </div>
            )}
          </div>
        </main>
      </div>
    </PageLayout>
  )
}
