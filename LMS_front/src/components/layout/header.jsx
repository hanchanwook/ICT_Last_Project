import { Link, useLocation } from "react-router-dom"
import { MessageSquare, User, X } from "lucide-react"
import { useState, useRef, useEffect } from "react"
import { http, getCookie } from "../auth/http"
import { jwtDecode } from "jwt-decode"

export default function Header({ currentPage = "", userRole = "student", userName = "사용자" }) {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false)
  const dropdownRef = useRef(null)
  const [currentUser, setCurrentUser] = useState(null)
  const [grantedPmIds, setGrantedPmIds] = useState([])
  const [isNotificationOpen, setIsNotificationOpen] = useState(false)
  const [notifications, setNotifications] = useState([
    { id: 1, title: "새로운 학생이 등록되었습니다", message: "김철수 학생이 JavaScript 기초 과정에 등록했습니다.", time: "5분 전", isRead: false, type: "student" },
    { id: 2, title: "시험 결과가 업데이트되었습니다", message: "React 심화 과정의 중간고사 결과가 업로드되었습니다.", time: "1시간 전", isRead: false, type: "exam" },
    { id: 3, title: "강의실 예약 확인", message: "A101 강의실이 내일 오전 9시에 예약되었습니다.", time: "3시간 전", isRead: true, type: "room" },
    { id: 4, title: "설문 평가 완료", message: "Python 기초 과정의 설문 평가가 완료되었습니다.", time: "1일 전", isRead: true, type: "survey" },
  ])
  const notificationRef = useRef(null)

  const [userInfo, setUserInfo] = useState(null)
  const [isLoadingUserInfo, setIsLoadingUserInfo] = useState(false)
  const [isInfoModalOpen, setIsInfoModalOpen] = useState(false)
  const [isEditModalOpen, setIsEditModalOpen] = useState(false)
  const [editFormData, setEditFormData] = useState({
    name: "",
    email: "",
    phone: "",
    password: "",
    passwordConfirm: "",
    memberAddress: ""
  })
  const [isUpdating, setIsUpdating] = useState(false)
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)
  const [isPermissionsLoaded, setIsPermissionsLoaded] = useState(false)

  const location = useLocation()

  // === 토큰에서 사용자 정보 추출 (경로 변경 시 실행) ===
  useEffect(() => {
    // 쿠키와 localStorage 모두에서 토큰 확인
    let token = getCookie("refresh") || getCookie("accessToken")
    
    // 쿠키에 없으면 localStorage에서 확인
    if (!token) {
      token = localStorage.getItem("token") || localStorage.getItem("accessToken")
    }
    
    if (token) {
      try {
        const decoded = jwtDecode(token)
        
        // name 필드가 없을 경우 다른 필드명들도 시도
        const userName = decoded.name || decoded.memberName || decoded.userName || decoded.username || "사용자"
        
        const userInfo = {
          userId: decoded.userId,
          name: userName,
          email: decoded.email || decoded.memberEmail || "",
          educationId: decoded.educationId,
          role: decoded.role
        }
        
        setCurrentUser(userInfo)
      } catch (error) {
        console.error("JWT 토큰 디코딩 실패:", error)
        // localStorage의 currentUser 정보도 확인
        const storedUser = JSON.parse(localStorage.getItem("currentUser") || "{}")
        if (storedUser.name && storedUser.role) {
          // 로컬에서 역할 변환
          const normalizedRole = storedUser.role.startsWith('ROLE_') 
            ? storedUser.role 
            : `ROLE_${storedUser.role.toUpperCase()}`
          setCurrentUser({
            name: storedUser.name,
            role: normalizedRole,
            memberId: storedUser.memberId
          })
        } else {
          setCurrentUser(null)
        }
      }
    } else {
      // 토큰이 없어도 localStorage의 currentUser 정보 확인
      const storedUser = JSON.parse(localStorage.getItem("currentUser") || "{}")
      if (storedUser.name && storedUser.role) {
        // 로컬에서 역할 변환  
        const normalizedRole = storedUser.role.startsWith('ROLE_') 
          ? storedUser.role 
          : `ROLE_${storedUser.role.toUpperCase()}`
        setCurrentUser({
          name: storedUser.name,
          role: normalizedRole,
          memberId: storedUser.memberId
        })
      } else {
        setCurrentUser(null)
      }
    }
  }, [location.pathname]) // <-- 경로 변경 시 실행

  // === API에서 사용자 정보 가져오기 (토큰 정보와 동기화) ===
  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const response = await http.post("/api/mypage/get-my-info", {}, { withCredentials: true })
        const userData = response.data
        
        setCurrentUser(prev => ({
          ...prev,
          name: userData.name,
          email: userData.email,
          role: userData.role,
          educationId: userData.educationId,
          educationName: userData.educationName,
          phone: userData.phone
        }))
      } catch (error) {
        console.error("사용자 정보 조회 실패:", error)
      }
    }

    fetchUserInfo()
  }, [])

  // === 권한 조회 (직원 계정 전용) ===
  useEffect(() => {
    if (!currentUser?.userId) return;
  
    http.get(`/api/member/resolve-id/${currentUser.userId}`, { withCredentials: true })
      .then(res => {
        setCurrentUser(prev => ({
          ...prev,
          memberId: res.data.memberId // 서버에서 매핑된 memberId
        }));
      })
      .catch(err => console.error("memberId 조회 실패", err));
  }, [currentUser?.userId]);
  
  // === 권한 조회 (memberId 세팅 후 실행) ===
  useEffect(() => {
    if (!currentUser?.userId || currentUser.role !== "ROLE_STAFF") {
      setIsPermissionsLoaded(true);
      return;
    }
  
    http.get("/api/staff/my-permissions", { withCredentials: true })
      .then(res => {
        const { memberId, mainPermissions, subPermissions } = res.data.data;
        const all = [...mainPermissions, ...subPermissions];
        // 🔥 여기서 memberId를 currentUser에 추가
        setCurrentUser(prev => ({ ...prev, memberId }));
        setGrantedPmIds(all.filter(p => p.isGranted).map(p => Number(p.pmId)));
      })
      .catch(err => console.error("Header 권한 조회 실패", err))
      .finally(() => setIsPermissionsLoaded(true));
  }, [currentUser?.userId]);

  const handleNotificationClick = () => setIsNotificationOpen(!isNotificationOpen)
  const markAsRead = (id) => setNotifications(notifications.map(n => n.id === id ? { ...n, isRead: true } : n))
  const markAllAsRead = () => setNotifications(notifications.map(n => ({ ...n, isRead: true })))

  const getNotificationIcon = (type) => {
    switch (type) {
      case "student": return "👨‍🎓"
      case "exam": return "📝"
      case "room": return "🏫"
      case "survey": return "📊"
      default: return "🔔"
    }
  }

  const handleLogout = async () => {
    try {
      await http.post("/api/auth/logout", { method: "POST", credentials: "include" })
      localStorage.removeItem("currentUser")
      window.location.href = "/"
    } catch (error) {
      console.error("로그아웃 중 오류 발생:", error)
      alert("로그아웃 중 오류가 발생했습니다. 다시 시도해주세요.")
    }
  }

  // === 사용자 정보 조회 ===
  const fetchUserInfo = async () => {
    if (!currentUser) return
    setIsLoadingUserInfo(true)
    try {
      const { data } = await http.post(
        "/api/mypage/get-my-info",
        {
          name: currentUser.name,
          email: currentUser.email,
          educationId: currentUser.educationId
        },
        { withCredentials: true }
      )

      setUserInfo({
        name: data.name || "이름 없음",
        email: data.email || "이메일 없음",
        phone: data.phone || "전화번호 없음",
        educationName: data.educationName || "학원명 없음",
        role: data.role || "권한 없음"
      })
    } catch (error) {
      console.error("사용자 정보 조회 실패:", error)
      setUserInfo(null)
    } finally {
      setIsLoadingUserInfo(false)
    }
  }

  const handleDropdownToggle = () => setIsDropdownOpen(!isDropdownOpen)
  const handleOpenInfoModal = async () => { await fetchUserInfo(); setIsInfoModalOpen(true) }
  const handleCloseInfoModal = () => setIsInfoModalOpen(false)
  
  // === 수정 모달 관련 함수들 ===
  const handleOpenEditModal = () => {
    if (userInfo) {
      setEditFormData({
        name: userInfo.name,
        email: userInfo.email,
        phone: userInfo.phone,
        password: "",
        passwordConfirm: "",
        memberAddress: userInfo.memberAddress || ""
      })
    }
    setIsEditModalOpen(true)
  }
  
  const handleCloseEditModal = () => {
    setIsEditModalOpen(false)
    setEditFormData({
      name: "",
      email: "",
      phone: "",
      password: "",
      passwordConfirm: "",
      memberAddress: ""
    })
  }
  
  const handleUpdateUserInfo = async () => {
    if (!currentUser) return
    
    // 비밀번호 변경 시 확인
    if (editFormData.password && editFormData.password !== editFormData.passwordConfirm) {
      alert("비밀번호와 비밀번호 확인이 일치하지 않습니다.")
      return
    }
    
    setIsUpdating(true)
    
    try {
      const { data } = await http.post(
        "/api/mypage/update-my-info",
        {
          name: editFormData.name,
          email: editFormData.email,
          phone: editFormData.phone,
          password: editFormData.password || null,
          address: editFormData.memberAddress
        },
        { withCredentials: true }
      )
      
      // 업데이트된 정보로 userInfo 갱신
      setUserInfo({
        name: data.name || editFormData.name,
        email: data.email || editFormData.email,
        phone: data.phone || editFormData.phone,
        educationName: userInfo.educationName,
        role: userInfo.role,
        memberAddress: data.memberAddress || editFormData.memberAddress
      })
      
      alert("정보가 성공적으로 수정되었습니다!")
      handleCloseEditModal()
    } catch (error) {
      console.error("사용자 정보 수정 실패:", error)
      alert("정보 수정에 실패했습니다. 다시 시도해주세요.")
    } finally {
      setIsUpdating(false)
    }
  }

  // 계정 탈퇴 함수
  const handleDeleteAccount = async () => {
    if (!currentUser) return
    try {
      await http.post("/api/mypage/delete-my-info", {
        email: currentUser.email   // JWT에서 얻은 사용자 이메일
      }, { withCredentials: true })
      alert("계정이 성공적으로 탈퇴되었습니다.")
      localStorage.removeItem("currentUser")
      window.location.href = "/"
    } catch (error) {
      console.error("계정 탈퇴 실패:", error)
      alert("계정 탈퇴에 실패했습니다. 다시 시도해주세요.")
}
  }

  // props로 받은 역할 값을 표준 형태로 변환
  const normalizeRole = (role) => {
    if (!role) return role
    
    // 이미 ROLE_ 접두사가 있으면 그대로 반환
    if (role.startsWith('ROLE_')) return role
    
    // 소문자 역할명을 ROLE_ 접두사와 함께 대문자로 변환
    const roleMap = {
      'admin': 'ROLE_ADMIN',
      'director': 'ROLE_DIRECTOR', 
      'staff': 'ROLE_STAFF',
      'instructor': 'ROLE_INSTRUCTOR',
      'student': 'ROLE_STUDENT'
    }
    
    return roleMap[role.toLowerCase()] || role
  }

  const actualUserRole = normalizeRole(currentUser?.role || userRole)
  const actualUserName = currentUser?.name || userName
  
  const getRoleText = (role) => {
    switch (role) {
      case "ROLE_ADMIN": return "관리자권한"
      case "ROLE_DIRECTOR": return "학원장권한"
      case "ROLE_STAFF": return "일반직원권한"
      case "ROLE_INSTRUCTOR": return "강사권한"
      case "ROLE_STUDENT": return "학생권한"
      default: return "권한"
    }
  }

  const navItems =
    actualUserRole === "ROLE_ADMIN"
      ? [{ href: "/institution/register", label: "학원 추가", key: "institution" }]
      : actualUserRole === "ROLE_DIRECTOR" || actualUserRole === "ROLE_STAFF"
        ? [
            { href: "/account/individual", label: "계정 등록", key: "account" },
            { href: "/academic/students", label: "회원 정보", key: "academic" },
            { href: "/courses/course", label: "과정 관리", key: "courses" },
            { href: "/classroom/room", label: "강의실 관리", key: "classroom" },
            { href: "/evaluations", label: "설문 평가 관리", key: "evaluations" },
          { href: "/exam/courses", label: "시험 및 성적", key: "exam" },
            { href: "/permission", label: "권한 관리", key: "permission" },
          { href: "/notice", label: "공지사항", key: "notice" }
          ]
        : actualUserRole === "ROLE_INSTRUCTOR"
          ? [
              { href: "/instructor/courses/lectures", label: "과정 관리", key: "courses" },
              { href: "/instructor/academic", label: "회원 정보", key: "academic" },
              { href: "/instructor/exam/my-exams", label: "시험 및 성적", key: "exam" },
              { href: "/instructor/question-bank/all", label: "문제 은행", key: "question-bank" },
              { href: "/instructor/evaluation", label: "설문 평가", key: "evaluation" },
            { href: "/notice", label: "공지사항", key: "notice" }
            ]
          : actualUserRole === "ROLE_STUDENT"
            ? [
                { href: "/student/my-courses", label: "출석 관리", key: "my-courses" },
                { href: "/student/syllabus", label: "강의 계획서", key: "syllabus" },
                { href: "/student/assignments", label: "과제", key: "my-assignment" },
                { href: "/student/exams", label: "시험 및 성적", key: "my-exam" },
                { href: "/student/evaluation", label: "설문 평가", key: "evaluation" },
              { href: "/notice", label: "공지사항", key: "notice" }
            ]
            : []

  const permissionMap = { account: 1, academic: 2, courses: 3, classroom: 4, evaluations: 5, exam: 6, evaluation: 7 }

  return (
    <>
    <header className="px-6 py-3 flex items-center justify-between" style={{ backgroundColor: "#2C3E50" }}>
      <div className="flex items-center space-x-8">
        <Link to="/dashboard" className="flex items-center space-x-2">
          <img src="/LMS_logo.png" alt="LMSync Logo" className="h-12 w-auto" />
            <span className="text-xl font-bold" style={{ color: "#1ABC9C" }}>LMSync</span>
        </Link>
          <nav className="flex space-x-6 ml-30">
            {navItems.map(item => {
            const requiredPm = permissionMap[item.key]
            let allowed = true

            if (actualUserRole === "ROLE_STAFF") {
              if (!isPermissionsLoaded) {
                allowed = false
              } else {
                allowed = !requiredPm || grantedPmIds.includes(requiredPm)
              }
            }

            return (
              <Link
                key={item.key}
                to={allowed ? item.href : "#"}
                onClick={e => { if (!allowed) e.preventDefault() }}
                className={`text-white ${allowed ? "hover:opacity-80" : "opacity-50 pointer-events-none"} ${currentPage === item.key ? "px-3 py-1 rounded" : ""}`}
                style={{
                  backgroundColor: currentPage === item.key ? "#1ABC9C" : "transparent",
                  color: currentPage === item.key ? "white" : "#1ABC9C",
                  cursor: allowed ? "pointer" : "not-allowed"
                }}
              >
                {item.label}
              </Link>
            )
          })}
        </nav>
      </div>

      <div className="flex items-center space-x-4">
        

          {/* 채팅 */}
          <Link to="/chat"><MessageSquare className="w-5 h-5 text-white cursor-pointer hover:opacity-80" /></Link>

          {/* 사용자 드롭다운 */}
        <div className="relative" ref={dropdownRef}>
            <div className="flex items-center space-x-2 cursor-pointer hover:opacity-80 px-2 py-1 rounded" onClick={handleDropdownToggle}>
            <User className="w-5 h-5 text-white" />
            <span className="text-white text-sm">{actualUserName}</span>
            <span className="text-white text-xs opacity-70">{getRoleText(actualUserRole)}</span>
          </div>
          {isDropdownOpen && (
              <div className="absolute right-0 mt-2 w-80 bg-white rounded-md shadow-lg border z-50">
              <div className="py-1">
                  <button onClick={handleOpenInfoModal} className="block w-full text-left px-4 py-2 text-sm hover:bg-gray-100">내 정보 보기</button>
                  <button onClick={handleLogout} className="block w-full text-left px-4 py-2 text-sm hover:bg-gray-100" style={{ color: "#2C3E50" }}>로그아웃</button>
              </div>
            </div>
          )}
        </div>
      </div>   
    </header>

      {/* === 내 정보 모달 === */}
      {isInfoModalOpen && (
        <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: "rgba(0, 0, 0, 0.5)" }}>
          <div className="bg-white rounded-lg shadow-lg w-96 p-6 relative">
            <button onClick={handleCloseInfoModal} className="absolute top-3 right-3 text-gray-500 hover:text-gray-700">
              <X className="w-5 h-5" />
            </button>
            <h2 className="text-xl font-bold mb-4">내 정보</h2>
            {isLoadingUserInfo ? (
              <div className="text-gray-500">정보를 불러오는 중...</div>
            ) : userInfo ? (
              <>
                <div className="space-y-3 text-gray-700 mb-6">
                  <p><strong>이름:</strong> {userInfo.name}</p>
                  <p><strong>이메일:</strong> {userInfo.email}</p>
                  <p><strong>전화번호:</strong> {userInfo.phone}</p>
                  <p><strong>학원명:</strong> {userInfo.educationName}</p>
                  <p><strong>권한:</strong> {getRoleText(userInfo.role)}</p>
                </div>
                <div className="flex space-x-3">
                  <button 
                    onClick={handleOpenEditModal}
                    className="flex-1 py-2 px-4 rounded-md text-white  bg-[#1abc9c] font-medium hover:bg-[rgb(10,150,120)] transition-opacity"
                  >
                    수정하기
                  </button>
                  <button 
                    className="flex-1 py-2 px-4 rounded-md text-white bg-[#E74C3C] font-medium hover:bg-red-600 transition-opacity"
                    onClick={() => setIsDeleteModalOpen(true)}
                  >
                    계정 탈퇴
                  </button>
                </div>
              </>
            ) : (
              <div className="text-gray-500">사용자 정보를 불러올 수 없습니다.</div>
            )}
          </div>
        </div>
      )}
      {/* === 계정 탈퇴 확인 모달 === */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: "rgba(0, 0, 0, 0.5)" }}>
          <div className="bg-white rounded-lg shadow-lg w-80 p-6 relative">
            <h2 className="text-lg font-bold mb-4 text-center">계정 탈퇴하시겠습니까?</h2>
            <div className="flex space-x-3 mt-6">
              <button
                onClick={() => setIsDeleteModalOpen(false)}
                className="flex-1 py-2 px-4 rounded-md border border-gray-300 text-gray-700 font-medium hover:bg-gray-100 transition-colors"
              >
                취소
              </button>
              <button
                onClick={handleDeleteAccount}
                className="flex-1 py-2 px-4 rounded-md text-white font-medium hover:opacity-90 transition-opacity"
                style={{ backgroundColor: "#E74C3C" }}
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}

       {/* === 수정 모달 === */}
       {isEditModalOpen && (
         <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: "rgba(0, 0, 0, 0.5)" }}>
           <div className="bg-white rounded-lg shadow-lg w-96 p-6 relative">
             <button onClick={handleCloseEditModal} className="absolute top-3 right-3 text-gray-500 hover:text-gray-700">
               <X className="w-5 h-5" />
             </button>
             <h2 className="text-xl font-bold mb-4">정보 수정</h2>
             
             <div className="space-y-4">
               <div>
                 <label className="block text-sm font-medium text-gray-700 mb-1">이름</label>
                 <input
                   type="text"
                   value={editFormData.name}
                   onChange={(e) => setEditFormData({...editFormData, name: e.target.value})}
                   className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-blue-500"
                   placeholder="이름을 입력하세요"
                 />
               </div>
               
               <div>
                 <label className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
                 <input
                   type="email"
                   value={editFormData.email}
                   onChange={(e) => setEditFormData({...editFormData, email: e.target.value})}
                   className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-blue-500"
                   placeholder="이메일을 입력하세요"
                 />
               </div>
               
               <div>
                 <label className="block text-sm font-medium text-gray-700 mb-1">전화번호</label>
                 <input
                   type="tel"
                   value={editFormData.phone}
                   onChange={(e) => setEditFormData({...editFormData, phone: e.target.value})}
                   className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-blue-500"
                   placeholder="전화번호를 입력하세요"
                 />
               </div>
               
               <div>
                 <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호 (변경시에만 입력)</label>
                 <input
                   type="password"
                   value={editFormData.password}
                   onChange={(e) => setEditFormData({...editFormData, password: e.target.value})}
                   className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-blue-500"
                   placeholder="새 비밀번호를 입력하세요"
                 />
               </div>
               
               <div>
                 <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호 확인</label>
                 <input
                   type="password"
                   value={editFormData.passwordConfirm}
                   onChange={(e) => setEditFormData({...editFormData, passwordConfirm: e.target.value})}
                   className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-blue-500"
                   placeholder="새 비밀번호를 다시 입력하세요"
                 />
               </div>
               
                               <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">주소</label>
                  <div className="flex space-x-2">
                    <input
                      type="text"
                      value={editFormData.memberAddress}
                      onChange={(e) => setEditFormData({...editFormData, memberAddress: e.target.value})}
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-blue-500"
                      placeholder="주소를 입력하세요"
                      readOnly
                    />
                    <button
                      type="button"
                      onClick={() => {
                        if (window.daum && window.daum.Postcode) {
                          new window.daum.Postcode({
                            oncomplete: function(data) {
                              setEditFormData({
                                ...editFormData, 
                                memberAddress: data.address
                              })
                            }
                          }).open()
                        } else {
                          alert("우편번호 서비스를 불러올 수 없습니다.")
                        }
                      }}
                      className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 transition-colors text-sm"
                    >
                      주소 검색
                    </button>
                  </div>
                </div>
             </div>
             
             <div className="flex space-x-3 mt-6">
               <button
                 onClick={handleCloseEditModal}
                 className="flex-1 py-2 px-4 rounded-md border border-gray-300 text-gray-700 font-medium hover:bg-gray-100 transition-colors"
               >
                 취소
               </button>
               <button
                 onClick={handleUpdateUserInfo}
                 disabled={isUpdating}
                 className="flex-1 py-2 px-4 rounded-md text-white bg-[#1abc9c] font-medium hover:bg-[rgb(10,150,120)] transition-opacity disabled:opacity-50"
               >
                 {isUpdating ? "수정 중..." : "수정하기"}
               </button>
             </div>
           </div>
         </div>
       )}
     </>
   )
 }
