import { useState, useEffect } from "react"
import { CheckCircle, XCircle, Clock, User, MapPin, Calendar, ArrowLeft, LogOut } from "lucide-react"
import { validateQRData, submitAttendance, checkQRSessionStatus, getTodayAttendanceStatus, submitCheckOut } from "@/api/hancw/mobileStudentAxios"

// 쿠키에서 userId 추출하는 함수
const getUserIdFromCookie = () => {
  try {
    // refresh 쿠키에서 JWT 토큰 가져오기
    const cookies = document.cookie.split(';')
    const refreshCookie = cookies.find(cookie => cookie.trim().startsWith('refresh='))
    
    if (refreshCookie) {
      const token = refreshCookie.split('=')[1]
      // JWT 토큰 디코딩 (Base64)
      const payload = JSON.parse(atob(token.split('.')[1]))
      return payload.userId || payload.userId
    }
  } catch (error) {
    console.error('쿠키에서 userId 추출 실패:', error)
  }
  return null
}

export default function MobileAttendancePage() {
  const [attendanceData, setAttendanceData] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [attendanceStatus, setAttendanceStatus] = useState(null)
  const [todayAttendanceInfo, setTodayAttendanceInfo] = useState(null)
  const [currentAction, setCurrentAction] = useState(null) // 'checkin' | 'checkout' | 'already_checked_out'

  // currentAction 상태 변화 추적
  useEffect(() => {
    console.log('📱 currentAction 상태 변화:', currentAction)
  }, [currentAction])

  // 로그인 상태 확인
  useEffect(() => {
    const currentUser = sessionStorage.getItem('currentUser')
    if (!currentUser) {
      // 로그인되지 않은 경우 모바일 로그인 페이지로 리다이렉트
      const urlParams = new URLSearchParams(window.location.search)
      const qrData = urlParams.get('data')
      const redirectUrl = qrData ? `/mobile/login?data=${qrData}` : '/mobile/login'
      window.location.href = redirectUrl
      return
    }

    // 로그인된 경우 바로 오늘 출석 상태 확인
    checkTodayAttendanceStatusOnLogin()

    // URL 파라미터에서 QR 코드 데이터를 가져옴
    const urlParams = new URLSearchParams(window.location.search)
    const qrData = urlParams.get('data')
    
    if (qrData) {
      // QR 코드 데이터를 파싱하여 출석 정보 추출
      parseQRData(qrData)
    } else {
      // QR 코드 데이터가 없어도 출석 상태는 확인 가능
      console.log('📱 QR 데이터 없음, 출석 상태만 확인')
      // QR 데이터가 없으면 attendanceData는 null로 유지
      // 하지만 출석 상태는 이미 checkTodayAttendanceStatusOnLogin에서 확인됨
    }
  }, [])

  const parseQRData = async (qrData) => {
    try {
      // QR 코드 데이터 형식: base64 인코딩된 JSON 데이터
      const decodedData = decodeURIComponent(atob(qrData))
      const qrPayload = JSON.parse(decodedData)
      
      console.log('📱 QR 데이터 파싱 결과:', qrPayload)
      
      // QR 데이터 유효성 검사
      if (qrPayload.type === 'ATTENDANCE_QR' && qrPayload.sessionId) {
        
        // 백엔드에서 QR 데이터 검증 (실패해도 계속 진행)
        try {
          const validationResult = await validateQRData(qrData)
          console.log('📱 QR 데이터 검증 결과:', validationResult)
          
          // QR 세션 상태 확인
          const sessionStatus = await checkQRSessionStatus(qrPayload.sessionId)
          console.log('📱 QR 세션 상태:', sessionStatus)
          
          if (validationResult.success && sessionStatus.success) {
            setAttendanceData({
              sessionId: qrPayload.sessionId,
              classroomId: qrPayload.classroomId,
              classroomName: qrPayload.classroomName,
              classroomNumber: qrPayload.classroomNumber,
              lectureDate: qrPayload.lectureDate,
              lectureStartTime: qrPayload.lectureStartTime,
              lectureEndTime: qrPayload.lectureEndTime,
              qrExpiryTime: qrPayload.qrExpiryTime,
              timestamp: new Date().toISOString()
            })
            
            // 출석 상태 확인
            await checkTodayAttendanceStatus()
          } else {
            // API 검증 실패 시에도 로컬 데이터로 진행
            console.warn('API 검증 실패, 로컬 데이터로 진행')
            setAttendanceData({
              sessionId: qrPayload.sessionId,
              classroomId: qrPayload.classroomId,
              classroomName: qrPayload.classroomName,
              classroomNumber: qrPayload.classroomNumber,
              lectureDate: qrPayload.lectureDate,
              lectureStartTime: qrPayload.lectureStartTime,
              lectureEndTime: qrPayload.lectureEndTime,
              qrExpiryTime: qrPayload.qrExpiryTime,
              timestamp: new Date().toISOString()
            })
            await checkTodayAttendanceStatus()
          }
        } catch (apiError) {
          console.error('API 검증 실패, 로컬 데이터로 진행:', apiError)
          // API 호출 실패 시에도 로컬 데이터로 진행
          setAttendanceData({
            sessionId: qrPayload.sessionId,
            classroomId: qrPayload.classroomId,
            classroomName: qrPayload.classroomName,
            classroomNumber: qrPayload.classroomNumber,
            lectureDate: qrPayload.lectureDate,
            lectureStartTime: qrPayload.lectureStartTime,
            lectureEndTime: qrPayload.lectureEndTime,
            qrExpiryTime: qrPayload.qrExpiryTime,
            timestamp: new Date().toISOString()
          })
          await checkTodayAttendanceStatus()
        }
      } else {
        throw new Error('Invalid QR data format')
      }
    } catch (error) {
      console.error('QR 데이터 파싱 오류:', error)
      setAttendanceStatus('error')
      setIsLoading(false)
    }
  }

  // 로그인 후 바로 출석 상태 확인 
  const checkTodayAttendanceStatusOnLogin = async () => {
    try {
      const currentUser = JSON.parse(sessionStorage.getItem('currentUser') || '{}')
      const userId = getUserIdFromCookie() || currentUser.userInfo?.userId || currentUser.userId
      
      console.log('📱 로그인 후 출석 상태 확인:', userId)
      
      const statusResult = await getTodayAttendanceStatus(userId)
      console.log('📱 로그인 후 출석 상태 확인 결과:', statusResult)
      
      if (statusResult.success) {
        setTodayAttendanceInfo(statusResult)
        
        // 디버깅 로그 추가
        console.log('📱 [로그인] action 값:', statusResult.action)
        console.log('📱 [로그인] hasAttendance 값:', statusResult.hasAttendance)
        console.log('📱 [로그인] 전체 statusResult:', statusResult)
        
        // 출석 상태에 따라 액션 결정
        if (statusResult.action === 'CHECK_IN') {
          console.log('📱 [로그인] CHECK_IN 조건 만족 - checkin으로 설정')
          setCurrentAction('checkin')
        } else if (statusResult.action === 'CHECK_OUT') {
          console.log('📱 [로그인] CHECK_OUT 조건 만족 - checkout으로 설정')
          setCurrentAction('checkout')
        } else if (statusResult.action === 'ALREADY_CHECKED_OUT') {
          console.log('📱 [로그인] ALREADY_CHECKED_OUT 조건 만족 - already_checked_out으로 설정')
          setCurrentAction('already_checked_out')
        } else {
          console.log('📱 [로그인] 예상치 못한 action 값:', statusResult.action)
          setCurrentAction('checkin')
        }
      } else {
        // 출석 기록이 없으면 출석 화면
        console.log('📱 [로그인] success가 false - checkin으로 설정')
        setCurrentAction('checkin')
      }
    } catch (error) {
      console.error('로그인 후 출석 상태 확인 실패:', error)
      // 에러 발생 시 출석 화면으로 설정
      setCurrentAction('checkin')
    } finally {
      setIsLoading(false)
    }
  }

  // 오늘 출석 상태 확인 (QR 데이터와 함께)
  const checkTodayAttendanceStatus = async () => {
    try {
      const currentUser = JSON.parse(sessionStorage.getItem('currentUser') || '{}')
      const userId = getUserIdFromCookie() || currentUser.userInfo?.userId || currentUser.userId
      
      console.log('📱 오늘 출석 상태 확인:', userId)
      
      const statusResult = await getTodayAttendanceStatus(userId)
      console.log('📱 출석 상태 확인 결과:', statusResult)
      
      if (statusResult.success) {
        setTodayAttendanceInfo(statusResult)
        
        // 디버깅 로그 추가
        console.log('📱 [QR] action 값:', statusResult.action)
        console.log('📱 [QR] hasAttendance 값:', statusResult.hasAttendance)
        console.log('📱 [QR] 전체 statusResult:', statusResult)
        
        // 출석 상태에 따라 액션 결정
        if (statusResult.action === 'CHECK_IN') {
          console.log('📱 [QR] CHECK_IN 조건 만족 - checkin으로 설정')
          setCurrentAction('checkin')
        } else if (statusResult.action === 'CHECK_OUT') {
          console.log('📱 [QR] CHECK_OUT 조건 만족 - checkout으로 설정')
          setCurrentAction('checkout')
        } else if (statusResult.action === 'ALREADY_CHECKED_OUT') {
          console.log('📱 [QR] ALREADY_CHECKED_OUT 조건 만족 - already_checked_out으로 설정')
          setCurrentAction('already_checked_out')
        } else {
          console.log('📱 [QR] 예상치 못한 action 값:', statusResult.action)
          setCurrentAction('checkin')
        }
      } else {
        // 출석 기록이 없으면 출석 화면
        console.log('📱 [QR] success가 false - checkin으로 설정')
        setCurrentAction('checkin')
      }
    } catch (error) {
      console.error('출석 상태 확인 실패:', error)
      // 에러 발생 시 출석 화면으로 설정
      setCurrentAction('checkin')
    } finally {
      setIsLoading(false)
    }
  }

  const handleAttendance = async () => {
    setIsLoading(true)
    
    try {
      // 출석 버튼 클릭 시 현재 상태를 다시 확인
      console.log('📱 출석 버튼 클릭 - 현재 상태 재확인')
      const currentUser = JSON.parse(sessionStorage.getItem('currentUser') || '{}')
      const userId = getUserIdFromCookie() || currentUser.userInfo?.userId || currentUser.userId
      
      // 현재 출석 상태를 다시 확인
      const currentStatus = await getTodayAttendanceStatus(userId)
      console.log('📱 출석 버튼 클릭 시 현재 상태:', currentStatus)
      
      // 이미 완료된 경우 처리
      if (currentStatus.success && currentStatus.action === 'ALREADY_CHECKED_OUT') {
        console.log('📱 이미 출석 및 퇴실이 완료됨 - 상태 업데이트')
        setTodayAttendanceInfo(currentStatus)
        setCurrentAction('already_checked_out')
        setIsLoading(false)
        return
      }
      
      // 로그인된 사용자 정보 가져오기
      
      // 실제 서버에 출석 요청
      const currentTime = new Date()
      const attendancePayload = {
        sessionId: attendanceData?.sessionId || null,
        classroomId: attendanceData?.classroomId || todayAttendanceInfo?.classId,
        timestamp: currentTime.toISOString(),
        checkInTime: currentTime.toISOString(), // 시분초까지 포함된 출석 시간
        userInfo: {
          userId: getUserIdFromCookie() || currentUser.userInfo?.userId || currentUser.userId,
          name: currentUser.userInfo?.userName || currentUser.name,
          role: currentUser.userInfo?.userRole || currentUser.role,
          userType: currentUser.userType
        },
        deviceInfo: {
          userAgent: navigator.userAgent,
          platform: navigator.platform,
          language: navigator.language
        }
      }
      
      console.log('📱 출석 요청 데이터:', attendancePayload)
      
      try {
        const result = await submitAttendance(attendancePayload)
        console.log('📱 출석 처리 결과:', result)
        
        if (result.success) {
          setAttendanceStatus('success')
          setCurrentAction('checkout') // 출석 완료 후 퇴실 화면으로 변경
        } else {
          throw new Error(result.message || '출석 처리에 실패했습니다.')
        }
      } catch (apiError) {
        console.error('API 출석 처리 실패, 로컬 처리로 진행:', apiError)
        // API 호출 실패 시 로컬 출석 처리
        const localAttendance = {
          ...attendancePayload,
          status: 'success',
          message: '로컬 출석 처리 완료 (서버 연결 불가)',
          timestamp: new Date().toISOString()
        }
        
        // 로컬 스토리지에 출석 기록 저장
        const attendanceHistory = JSON.parse(localStorage.getItem('attendanceHistory') || '[]')
        attendanceHistory.push(localAttendance)
        localStorage.setItem('attendanceHistory', JSON.stringify(attendanceHistory))
        
        setAttendanceStatus('success')
        setCurrentAction('checkout')
        alert('출석이 완료되었습니다. (서버 연결 불가로 로컬 처리)')
      }
      
    } catch (error) {
      console.error('출석 처리 오류:', error)
      setAttendanceStatus('error')
    } finally {
      setIsLoading(false)
    }
  }

  const handleCheckOut = async () => {
    setIsLoading(true)
    
    try {
      // 퇴실 버튼 클릭 시 현재 상태를 다시 확인
      console.log('📱 퇴실 버튼 클릭 - 현재 상태 재확인')
      const currentUser = JSON.parse(sessionStorage.getItem('currentUser') || '{}')
      const userId = getUserIdFromCookie() || currentUser.userInfo?.userId || currentUser.userId
      
      // 현재 출석 상태를 다시 확인
      const currentStatus = await getTodayAttendanceStatus(userId)
      console.log('📱 퇴실 버튼 클릭 시 현재 상태:', currentStatus)
      
      // 이미 완료된 경우 처리
      if (currentStatus.success && currentStatus.action === 'ALREADY_CHECKED_OUT') {
        console.log('📱 이미 출석 및 퇴실이 완료됨 - 상태 업데이트')
        setTodayAttendanceInfo(currentStatus)
        setCurrentAction('already_checked_out')
        setIsLoading(false)
        return
      }
      
      // 로그인된 사용자 정보 가져오기
      
      // 퇴실 요청 데이터
      const checkoutPayload = {
        sessionId: attendanceData?.sessionId || null,
        classroomId: attendanceData?.classroomId || todayAttendanceInfo?.classId,
        userInfo: {
          userId: getUserIdFromCookie() || currentUser.userInfo?.userId || currentUser.userId,
          memberName: currentUser.userInfo?.userName || currentUser.name || currentUser.memberName,
          memberEmail: currentUser.userInfo?.userEmail || currentUser.email || currentUser.memberEmail
        }
      }
      
      console.log('📱 퇴실 요청 데이터:', checkoutPayload)
      
      try {
        const result = await submitCheckOut(checkoutPayload)
        console.log('📱 퇴실 처리 결과:', result)
        
        if (result.success) {
          setAttendanceStatus('checkout_success')
          setCurrentAction('already_checked_out')
        } else {
          throw new Error(result.message || '퇴실 처리에 실패했습니다.')
        }
      } catch (apiError) {
        console.error('API 퇴실 처리 실패, 로컬 처리로 진행:', apiError)
        // API 호출 실패 시 로컬 퇴실 처리
        const localCheckout = {
          ...checkoutPayload,
          status: 'success',
          message: '로컬 퇴실 처리 완료 (서버 연결 불가)',
          timestamp: new Date().toISOString()
        }
        
        // 로컬 스토리지에 퇴실 기록 저장
        const checkoutHistory = JSON.parse(localStorage.getItem('checkoutHistory') || '[]')
        checkoutHistory.push(localCheckout)
        localStorage.setItem('checkoutHistory', JSON.stringify(checkoutHistory))
        
        setAttendanceStatus('checkout_success')
        setCurrentAction('already_checked_out')
        alert('퇴실이 완료되었습니다. (서버 연결 불가로 로컬 처리)')
      }
      
    } catch (error) {
      console.error('퇴실 처리 오류:', error)
      setAttendanceStatus('error')
    } finally {
      setIsLoading(false)
    }
  }

  const handleCancel = () => {
    if (attendanceStatus === 'success' || attendanceStatus === 'checkout_success') {
      // 출석/퇴실 완료 후에는 창 닫기 시도
      try {
        // 현재 창이 JavaScript로 열린 창인 경우에만 작동
        if (window.opener) {
          window.close()
        } else {
          // 일반적인 경우 사용자에게 안내
          alert('출석이 완료되었습니다. 브라우저 창을 닫아주세요.')
        }
      } catch (error) {
        // 창 닫기가 실패한 경우 안내
        alert('출석이 완료되었습니다. 브라우저 창을 닫아주세요.')
      }
    } else {
      // 로그아웃 시 모바일 로그인 페이지로 이동
      window.location.href = '/mobile/login'
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-xl shadow-lg p-8 text-center w-full max-w-sm">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto mb-6"></div>
          <p className="text-gray-600 text-lg">출석 정보를 확인하는 중...</p>
        </div>
      </div>
    )
  }

  if (attendanceStatus === 'error') {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-xl shadow-lg p-8 text-center w-full max-w-sm">
          <XCircle className="w-20 h-20 text-red-500 mx-auto mb-6" />
          <h2 className="text-2xl font-bold text-gray-900 mb-4">QR 코드 오류</h2>
          <p className="text-gray-600 mb-8 text-lg">유효하지 않은 QR 코드입니다.</p>
          <button
            onClick={handleCancel}
            className="w-full px-6 py-4 bg-gray-600 text-white rounded-xl hover:bg-gray-700 transition-colors text-lg font-medium"
          >
            돌아가기
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">

      <div className="p-4">
        <div className="bg-white rounded-xl shadow-lg p-6">
          <div className="text-center mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              {(() => {
                console.log('📱 UI 렌더링 - currentAction:', currentAction)
                if (currentAction === 'checkin') return '출석 확인'
                if (currentAction === 'checkout') return '퇴실 확인'
                return '출석 완료'
              })()}
            </h2>
            <p className="text-gray-600 text-lg">
              {(() => {
                if (currentAction === 'checkin') return '아래 정보를 확인하고 출석을 완료하세요.'
                if (currentAction === 'checkout') return '아래 정보를 확인하고 퇴실을 완료하세요.'
                return '오늘의 출석이 완료되었습니다.'
              })()}
            </p>
          </div>

          {/* 강의실 정보 */}
          {attendanceData && (
            <div className="bg-blue-50 rounded-xl p-6 mb-8">
              <h3 className="text-xl font-semibold text-blue-900 mb-6">{attendanceData.classroomName} - {attendanceData.classroomNumber}호</h3>
              
              <div className="space-y-4 text-base text-blue-700">
                <div className="flex items-center space-x-3">
                  <MapPin className="w-5 h-5 flex-shrink-0" />
                  <span>강의실: {attendanceData.classroomName} {attendanceData.classroomNumber}호</span>
                </div>
                <div className="flex items-center space-x-3">
                  <Calendar className="w-5 h-5 flex-shrink-0" />
                  <span>강의일: {attendanceData.lectureDate}</span>
                </div>
                <div className="flex items-center space-x-3">
                  <Clock className="w-5 h-5 flex-shrink-0" />
                  <span>강의시간: {new Date(attendanceData.lectureStartTime).toLocaleTimeString('ko-KR', {hour: '2-digit', minute: '2-digit'})} - {new Date(attendanceData.lectureEndTime).toLocaleTimeString('ko-KR', {hour: '2-digit', minute: '2-digit'})}</span>
                </div>
                <div className="flex items-center space-x-3">
                  <User className="w-5 h-5 flex-shrink-0" />
                  <span>QR 세션: {attendanceData.sessionId}</span>
                </div>
              </div>
            </div>
          )}

          {/* 출석 상태 정보 (QR 데이터가 없어도 표시) */}
          {!attendanceData && todayAttendanceInfo && (
            <div className="bg-blue-50 rounded-xl p-6 mb-8">
              <h3 className="text-xl font-semibold text-blue-900 mb-6">오늘의 출석 상태</h3>
              
              <div className="space-y-4 text-base text-blue-700">
                <div className="flex items-center space-x-3">
                  <Calendar className="w-5 h-5 flex-shrink-0" />
                  <span>날짜: {new Date().toLocaleDateString('ko-KR')}</span>
                </div>
                <div className="flex items-center space-x-3">
                  <Clock className="w-5 h-5 flex-shrink-0" />
                  <span>현재 시간: {new Date().toLocaleTimeString('ko-KR')}</span>
                </div>
                {todayAttendanceInfo.classroomName && (
                  <div className="flex items-center space-x-3">
                    <MapPin className="w-5 h-5 flex-shrink-0" />
                    <span>강의실: {todayAttendanceInfo.classroomName} {todayAttendanceInfo.classroomNumber}호</span>
                  </div>
                )}
                {todayAttendanceInfo.checkInTime && (
                  <div className="flex items-center space-x-3">
                    <CheckCircle className="w-5 h-5 flex-shrink-0" />
                    <span>출석 시간: {new Date(todayAttendanceInfo.checkInTime).toLocaleTimeString('ko-KR')}</span>
                  </div>
                )}
                {todayAttendanceInfo.checkOutTime && (
                  <div className="flex items-center space-x-3">
                    <LogOut className="w-5 h-5 flex-shrink-0" />
                    <span>퇴실 시간: {new Date(todayAttendanceInfo.checkOutTime).toLocaleTimeString('ko-KR')}</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 출석 완료 상태 표시 */}
          {attendanceStatus === 'success' && (
            <div className="bg-green-50 border border-green-200 rounded-xl p-6 mb-8">
              <div className="flex items-center space-x-4">
                <CheckCircle className="w-8 h-8 text-green-600 flex-shrink-0" />
                <div>
                  <h3 className="text-lg font-medium text-green-900">출석 완료</h3>
                  <p className="text-base text-green-700">
                    {new Date().toLocaleTimeString("ko-KR")}에 출석이 완료되었습니다.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* 퇴실 완료 상태 표시 */}
          {attendanceStatus === 'checkout_success' && (
            <div className="bg-blue-50 border border-blue-200 rounded-xl p-6 mb-8">
              <div className="flex items-center space-x-4">
                <LogOut className="w-8 h-8 text-blue-600 flex-shrink-0" />
                <div>
                  <h3 className="text-lg font-medium text-blue-900">퇴실 완료</h3>
                  <p className="text-base text-blue-700">
                    {new Date().toLocaleTimeString("ko-KR")}에 퇴실이 완료되었습니다.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* 버튼 */}
          <div className="space-y-4">
            {currentAction === 'checkin' && !attendanceStatus && (
              <div>
                {!attendanceData && (
                  <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4 mb-4">
                    <p className="text-yellow-800 text-sm">
                      💡 QR 코드를 스캔하지 않고 출석합니다. 강의실 정보가 표시되지 않을 수 있습니다.
                    </p>
                  </div>
                )}
                <button
                  onClick={handleAttendance}
                  disabled={isLoading}
                  className="w-full px-6 py-4 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors disabled:opacity-50 text-lg font-medium"
                >
                  {isLoading ? '출석 처리 중...' : '출석하기'}
                </button>
              </div>
            )}
            
            {currentAction === 'checkout' && attendanceStatus !== 'checkout_success' && (
              <button
                onClick={handleCheckOut}
                disabled={isLoading}
                className="w-full px-6 py-4 bg-orange-600 text-white rounded-xl hover:bg-orange-700 transition-colors disabled:opacity-50 text-lg font-medium"
              >
                {isLoading ? '퇴실 처리 중...' : '퇴실하기'}
              </button>
            )}

            {currentAction === 'already_checked_out' && (
              <div className="bg-green-50 border border-green-200 rounded-xl p-6 mb-4">
                <div className="flex items-center space-x-4">
                  <CheckCircle className="w-8 h-8 text-green-600 flex-shrink-0" />
                  <div>
                    <h3 className="text-lg font-medium text-green-900">출석 완료</h3>
                    <p className="text-base text-green-700">
                      오늘의 출석과 퇴실이 모두 완료되었습니다.
                    </p>
                  </div>
                </div>
              </div>
            )}
            
            <button
              onClick={handleCancel}
              className="w-full px-6 py-4 bg-gray-600 text-white rounded-xl hover:bg-gray-700 transition-colors text-lg font-medium"
            >
              {attendanceStatus === 'success' || attendanceStatus === 'checkout_success' || currentAction === 'already_checked_out' ? '화면 종료' : '로그아웃'}
            </button>
          </div>

          {/* 주의사항 */}
          <div className="mt-8 p-6 bg-yellow-50 rounded-xl">
            <h4 className="text-base font-medium text-yellow-900 mb-3">주의사항</h4>
            <ul className="text-sm text-yellow-700 space-y-2">
              {currentAction === 'checkin' ? (
                <>
                  <li>• 출석은 한 번만 가능합니다</li>
                  <li>• 출석 후에는 수정할 수 없습니다</li>
                  {!attendanceData && (
                    <li>• QR 코드를 스캔하여 출석할 수 있습니다</li>
                  )}
                </>
              ) : currentAction === 'checkout' ? (
                <>
                  <li>• 퇴실은 한 번만 가능합니다</li>
                  <li>• 퇴실 후에는 수정할 수 없습니다</li>
                  <li>• 강의 종료 후 퇴실하는 것을 권장합니다</li>
                </>
              ) : (
                <>
                  <li>• 오늘의 출석과 퇴실이 모두 완료되었습니다</li>
                  <li>• 출석 기록은 수정할 수 없습니다</li>
                </>
              )}
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
} 