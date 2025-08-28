import { useState } from "react"
import { Eye, EyeOff } from "lucide-react"
import { mobileStudentLogin } from "@/api/hancw/mobileStudentAxios"

export default function MobileLoginForm({ qrData }) {
  const [showPassword, setShowPassword] = useState(false)
  const [loginData, setLoginData] = useState({
    id: "",
    password: "",
  })
  const [isLoading, setIsLoading] = useState(false)

  // 학생 계정으로만 로그인 가능

  const handleLogin = async (e) => {
    e.preventDefault()
    setIsLoading(true)

    try {
      // 로컬 스토리지에서 출석 이력 데이터 가져오기
      const attendanceHistory = localStorage.getItem('attendanceHistory')
      const checkoutHistory = localStorage.getItem('checkoutHistory')
      
      // QR 데이터에서 sessionId 추출 (있는 경우)
      let sessionId = null
      if (qrData) {
        try {
          const decodedData = decodeURIComponent(atob(qrData))
          const qrPayload = JSON.parse(decodedData)
          sessionId = qrPayload.sessionId
        } catch (error) {
          console.warn('QR 데이터에서 sessionId 추출 실패:', error)
        }
      }
      
      // 로그인 요청 데이터에 출석 이력 포함
      const loginRequestData = {
        username: loginData.id,
        password: loginData.password,
        // 출석 이력 데이터 추가
        attendanceHistory: attendanceHistory ? JSON.parse(attendanceHistory) : null,
        checkoutHistory: checkoutHistory ? JSON.parse(checkoutHistory) : null,
        sessionId: sessionId
      }
      
      console.log('📱 로그인 요청 데이터:', {
        username: loginRequestData.username,
        hasAttendanceHistory: !!loginRequestData.attendanceHistory,
        hasCheckoutHistory: !!loginRequestData.checkoutHistory,
        sessionId: loginRequestData.sessionId
      })
      
      // DB 연동 로그인 시도
      const response = await mobileStudentLogin(loginRequestData)
      
      if (response.success && response.userInfo) {
        const userData = response.userInfo
        
        // 로그인 성공 - 사용자 정보를 sessionStorage에 저장
        sessionStorage.setItem(
          "currentUser",
          JSON.stringify({
            userInfo: {
              userId: userData.userId,
              userName: userData.userName,
              userEmail: userData.userEmail,
              userRole: userData.userRole
            },
            name: userData.userName,
            role: "student",
            userType: "student",
            memberId: userData.userId,
          })
        )

        console.log('📱 모바일 로그인 성공:', userData.name)
        
        // QR 데이터와 함께 출석 페이지로 리다이렉트
        if (qrData) {
          window.location.href = `/student/my-courses/mobileQR?data=${qrData}`
        } else {
          // QR 데이터가 없으면 기본 출석 페이지로
          window.location.href = '/student/my-courses/mobileQR'
        }
      } else {
        alert("로그인에 실패했습니다. 다시 시도해주세요.")
      }
    } catch (error) {
      console.error('📱 모바일 로그인 오류:', error)
      alert(error.message || "로그인 중 오류가 발생했습니다.")
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <form onSubmit={handleLogin} className="space-y-6">
      {/* 로그인 안내 */}
      <div className="p-4 rounded-xl bg-blue-50 border border-blue-200">
        <h3 className="text-sm font-medium text-blue-900 mb-2">로그인 안내</h3>
        <div className="space-y-1 text-xs text-blue-700">
          <p>• 학생 계정으로만 로그인 가능합니다</p>
          <p>• 모바일 출석을 위해 등록된 학생 계정을 사용해주세요</p>
        </div>
      </div>

      {/* 아이디 입력 */}
      <div className="space-y-2">
        <label htmlFor="mobileLoginId" className="text-sm font-medium text-gray-700">
          아이디
        </label>
        <input
          type="text"
          id="mobileLoginId"
          value={loginData.id}
          onChange={(e) => setLoginData({ ...loginData, id: e.target.value })}
          className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          placeholder="아이디를 입력하세요"
          required
        />
      </div>

      {/* 비밀번호 입력 */}
      <div className="space-y-2">
        <label htmlFor="mobileLoginPassword" className="text-sm font-medium text-gray-700">
          비밀번호
        </label>
        <div className="relative">
          <input
            type={showPassword ? "text" : "password"}
            id="mobileLoginPassword"
            value={loginData.password}
            onChange={(e) => setLoginData({ ...loginData, password: e.target.value })}
            className="w-full px-4 py-3 pr-12 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="비밀번호를 입력하세요"
            required
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 transform -translate-y-1/2 p-1"
          >
            {showPassword ? (
              <EyeOff className="w-5 h-5 text-gray-400" />
            ) : (
              <Eye className="w-5 h-5 text-gray-400" />
            )}
          </button>
        </div>
      </div>

      {/* 로그인 버튼 */}
      <button
        type="submit"
        disabled={isLoading}
        className="w-full px-6 py-4 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors disabled:opacity-50 text-lg font-medium"
      >
        {isLoading ? '로그인 중...' : '로그인'}
      </button>

      {/* 안내 메시지 */}
      <div className="text-center">
        <p className="text-xs text-gray-500">
          모바일 출석을 위해 학생 계정으로 로그인해주세요
        </p>
      </div>
    </form>
  )
} 