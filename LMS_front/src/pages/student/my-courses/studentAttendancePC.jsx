import { useState, useEffect } from "react"
import { CheckCircle, XCircle, Clock, User, MapPin, Calendar, BookOpen, ChevronDown, LogIn, LogOut } from "lucide-react"
import { getStudentSubjectsAndClassrooms, submitPCAttendance, submitPCCheckOut, getTodayAttendanceStatus } from "@/api/hancw/studentCourseAxios"

export default function StudentAttendancePCPage() {
  const [isLoading, setIsLoading] = useState(true)
  const [attendanceStatus, setAttendanceStatus] = useState(null)
  const [selectedCourse, setSelectedCourse] = useState("")
  const [selectedClassroom, setSelectedClassroom] = useState("")
  const [courses, setCourses] = useState([])
  const [classrooms, setClassrooms] = useState([])
  const [isCourseOpen, setIsCourseOpen] = useState(false)
  const [isClassroomOpen, setIsClassroomOpen] = useState(false)
  const [isCheckOutMode, setIsCheckOutMode] = useState(false) // 퇴실 모드 여부
  const [currentAttendanceInfo, setCurrentAttendanceInfo] = useState(null) // 현재 출석 정보
  const [courseAttendanceStatuses, setCourseAttendanceStatuses] = useState({}) // 각 과정별 출석 상태

  // 로그인 상태 확인 및 데이터 로드
  useEffect(() => {
    const currentUser = localStorage.getItem('currentUser') || sessionStorage.getItem('currentUser')
    if (!currentUser) {
      // 로그인되지 않은 경우 학생 로그인 페이지로 리다이렉트
      window.location.href = '/student/login'
      return
    }

    // 학생의 수강 과정 목록 가져오기 후 출석 상태 확인
    loadStudentCoursesAndCheckStatus()
  }, [])

  const loadStudentCoursesAndCheckStatus = async () => {
    try {
      const currentUser = JSON.parse(localStorage.getItem('currentUser') || sessionStorage.getItem('currentUser') || '{}')
      const userId = currentUser.memberId || currentUser.userInfo?.userId || currentUser.userId
      
      if (!userId) {
        console.error('사용자 ID를 찾을 수 없습니다.')
        setCourses([])
        return
      }
      
      console.log('수강 과정 및 강의실 정보 로드 시작, userId:', userId)
      const coursesData = await getStudentSubjectsAndClassrooms(userId)
      setCourses(coursesData)
      console.log('수강 과정 및 강의실 정보 로드 완료:', coursesData)
      
      // 수강 과정 로드 완료 후 출석 상태 확인
      await checkTodayAttendanceStatus(coursesData)
    } catch (error) {
      console.error('수강 과정 목록 로드 오류:', error)
      setCourses([])
    } finally {
      setIsLoading(false)
    }
  }

  // 오늘 출석 상태 확인 (과정별 개별 관리)
  const checkTodayAttendanceStatus = async (coursesData = courses) => {
    try {
      const currentUser = JSON.parse(localStorage.getItem('currentUser') || sessionStorage.getItem('currentUser') || '{}')
      const userId = currentUser.memberId || currentUser.userInfo?.userId || currentUser.userId
      
      if (!userId) return
      
      // 각 과정별로 출석 상태 확인
      const attendanceStatuses = {}
      
      for (const course of coursesData) {
        try {
          // 각 과정별로 memberId와 courseId를 함께 전송하여 출석 상태 확인
          const statusData = await getTodayAttendanceStatus(userId, course.courseId, course.memberId)
          console.log(`🔍 과정 "${course.courseName}" 출석 상태:`, statusData)
          
          // 백엔드에서 받은 데이터를 정확히 해석
          const hasAttendance = statusData?.hasAttendance || false
          const hasCheckIn = statusData?.checkInTime ? true : false
          const hasCheckOut = statusData?.checkOutTime ? true : false
          
          console.log(`🔍 과정 "${course.courseName}" 상세 상태:`, {
            hasAttendance,
            hasCheckIn,
            hasCheckOut,
            checkInTime: statusData?.checkInTime,
            checkOutTime: statusData?.checkOutTime
          })
          
          // 상태 결정 로직 개선
          let isCompleted = false
          let isCheckOutMode = false
          let canAttend = true
          
          if (hasAttendance && hasCheckIn) {
            if (hasCheckOut) {
              // 입실과 퇴실 모두 완료
              isCompleted = true
              isCheckOutMode = false
              canAttend = false
            } else {
              // 입실만 완료, 퇴실 대기
              isCompleted = false
              isCheckOutMode = true
              canAttend = false
            }
          } else {
            // 출석 기록 없음
            isCompleted = false
            isCheckOutMode = false
            canAttend = true
          }
          
          attendanceStatuses[course.courseId] = {
            courseId: course.courseId,
            courseName: course.courseName,
            memberId: course.memberId,
            statusData: statusData,
            hasAttendance: hasAttendance,
            hasCheckIn: hasCheckIn,
            hasCheckOut: hasCheckOut,
            isCompleted: isCompleted,
            isCheckOutMode: isCheckOutMode,
            canAttend: canAttend
          }
          
          console.log(`🔍 과정 "${course.courseName}" 최종 상태:`, {
            isCompleted,
            isCheckOutMode,
            canAttend
          })
          
        } catch (error) {
          console.error(`과정 "${course.courseName}" 출석 상태 확인 실패:`, error)
          // 에러 시 기본값 설정
          attendanceStatuses[course.courseId] = {
            courseId: course.courseId,
            courseName: course.courseName,
            memberId: course.memberId,
            statusData: null,
            hasAttendance: false,
            hasCheckIn: false,
            hasCheckOut: false,
            isCompleted: false,
            isCheckOutMode: false,
            canAttend: true
          }
        }
      }
      
      console.log('🔍 전체 출석 상태:', attendanceStatuses)
      setCourseAttendanceStatuses(attendanceStatuses)
      
      // 선택된 과정이 있으면 해당 과정의 상태로 UI 업데이트
      if (selectedCourse && attendanceStatuses[selectedCourse.courseId]) {
        const selectedStatus = attendanceStatuses[selectedCourse.courseId]
        setIsCheckOutMode(selectedStatus.isCheckOutMode)
        setCurrentAttendanceInfo(selectedStatus.statusData)
      }
      
    } catch (error) {
      console.error('출석 상태 확인 오류:', error)
      setIsCheckOutMode(false)
      setCurrentAttendanceInfo(null)
    }
  }

  const handleCourseSelect = async (course) => {
    console.log('🔍 선택된 과정 정보:', course)
    console.log('  - courseId:', course?.courseId)
    console.log('  - courseName:', course?.courseName)
    console.log('  - classrooms:', course?.classrooms)
    
    // 선택된 과정의 출석 상태 확인
    const courseStatus = courseAttendanceStatuses[course.courseId]
    console.log('🔍 선택된 과정의 출석 상태:', courseStatus)
    
    if (courseStatus?.isCompleted) {
      // 완료된 과정인 경우 알림
      alert(`"${course.courseName}" 과정은 오늘 이미 출석/퇴실이 완료되었습니다.`)
      return
    }
    
    setSelectedCourse(course)
    setSelectedClassroom("") // 과정 변경 시 강의실 초기화
    setIsCourseOpen(false)
    
    // 퇴실 대기 상태라면 자동으로 퇴실 모드로 변경
    if (courseStatus?.isCheckOutMode) {
      setIsCheckOutMode(true)
      setCurrentAttendanceInfo(courseStatus.statusData)
      console.log('🔍 퇴실 대기 상태로 자동 변경됨')
    } else {
      setIsCheckOutMode(false)
      setCurrentAttendanceInfo(null)
      console.log('🔍 출석 모드로 설정됨')
    }
    
    // 선택된 과정의 출석 기록을 DB에서 다시 조회
    try {
      const currentUser = JSON.parse(localStorage.getItem('currentUser') || sessionStorage.getItem('currentUser') || '{}')
      const userId = currentUser.memberId || currentUser.userInfo?.userId || currentUser.userId
      
      console.log('🔍 선택된 과정의 출석 기록 조회 시작:', {
        userId,
        courseId: course.courseId,
        memberId: course.memberId
      })
      
      // DB에서 해당 과정의 출석 기록 조회 (checkIn 컬럼 기준)
      const statusData = await getTodayAttendanceStatus(userId, course.courseId, course.memberId)
      console.log('🔍 DB에서 조회한 출석 기록:', statusData)
      
      // 백엔드에서 받은 데이터를 정확히 해석
      const hasAttendance = statusData?.hasAttendance || false
      const hasCheckIn = statusData?.checkInTime ? true : false
      const hasCheckOut = statusData?.checkOutTime ? true : false
      
      console.log('🔍 선택된 과정 상세 상태:', {
        hasAttendance,
        hasCheckIn,
        hasCheckOut,
        checkInTime: statusData?.checkInTime,
        checkOutTime: statusData?.checkOutTime
      })
      
      // 상태 결정 로직
      let isCompleted = false
      let isCheckOutMode = false
      let canAttend = true
      
      if (hasAttendance && hasCheckIn) {
        if (hasCheckOut) {
          // 입실과 퇴실 모두 완료
          isCompleted = true
          isCheckOutMode = false
          canAttend = false
        } else {
          // 입실만 완료, 퇴실 대기
          isCompleted = false
          isCheckOutMode = true
          canAttend = false
        }
      } else {
        // 출석 기록 없음
        isCompleted = false
        isCheckOutMode = false
        canAttend = true
      }
      
      // 선택된 과정의 상태 업데이트
      setCourseAttendanceStatuses(prev => ({
        ...prev,
        [course.courseId]: {
          courseId: course.courseId,
          courseName: course.courseName,
          memberId: course.memberId,
          statusData: statusData,
          hasAttendance: hasAttendance,
          hasCheckIn: hasCheckIn,
          hasCheckOut: hasCheckOut,
          isCompleted: isCompleted,
          isCheckOutMode: isCheckOutMode,
          canAttend: canAttend
        }
      }))
      
      // UI 상태 업데이트
      setIsCheckOutMode(isCheckOutMode)
      setCurrentAttendanceInfo(statusData)
      
      console.log('🔍 선택된 과정 최종 상태:', {
        isCompleted,
        isCheckOutMode,
        canAttend
      })
      
    } catch (error) {
      console.error('선택된 과정 출석 기록 조회 실패:', error)
      // 에러 시 기존 상태 유지
      if (courseStatus) {
        setIsCheckOutMode(courseStatus.isCheckOutMode)
        setCurrentAttendanceInfo(courseStatus.statusData)
      } else {
        setIsCheckOutMode(false)
        setCurrentAttendanceInfo(null)
      }
    }
    
    // 선택된 과정의 강의실 목록 설정
    if (course && course.classrooms) {
      setClassrooms(course.classrooms)
      console.log('🔍 설정된 강의실 목록:', course.classrooms)
    } else {
      setClassrooms([])
      console.log('🔍 강의실 목록이 없습니다.')
    }
  }

  const handleClassroomSelect = (classroom) => {
    console.log('🔍 선택된 강의실 정보:', classroom)
    console.log('  - classroomId:', classroom?.classroomId)
    console.log('  - classroomName:', classroom?.classroomName)
    
    setSelectedClassroom(classroom)
    setIsClassroomOpen(false)
  }

  const handleAttendance = async () => {
    if (!selectedCourse || !selectedClassroom) {
      alert("과정과 강의실을 모두 선택해주세요.")
      return
    }

    setIsLoading(true)
    
    try {
      const currentUser = JSON.parse(localStorage.getItem('currentUser') || sessionStorage.getItem('currentUser') || '{}')
      const currentTime = new Date()
      
      if (isCheckOutMode) {
        // 퇴실 처리
        const checkOutPayload = {
          courseId: selectedCourse.courseId,
          courseName: selectedCourse.courseName,
          classroomId: selectedClassroom.classroomId,
          classroomName: selectedClassroom.classroomName,
          userId: currentUser.memberId || currentUser.userInfo?.userId || currentUser.userId,
          memberId: selectedCourse.memberId, // 선택된 과정의 memberId 추가
          timestamp: currentTime.toISOString()
        }
        
        console.log('💻 PC 퇴실 요청 데이터:', checkOutPayload)
        const result = await submitPCCheckOut(checkOutPayload)
        console.log('PC 퇴실 제출 결과:', result)
        
        // 퇴실 완료 후 해당 과정의 출석 상태를 다시 확인
        try {
          const updatedStatusData = await getTodayAttendanceStatus(
            currentUser.memberId || currentUser.userInfo?.userId || currentUser.userId,
            selectedCourse.courseId,
            selectedCourse.memberId
          )
          
          console.log('🔍 퇴실 후 업데이트된 출석 상태:', updatedStatusData)
          
          // 백엔드에서 받은 실제 상태로 업데이트
          setCourseAttendanceStatuses(prev => {
            // 백엔드 데이터를 정확히 해석
            const hasAttendance = updatedStatusData?.hasAttendance || false
            const hasCheckIn = updatedStatusData?.checkInTime ? true : false
            const hasCheckOut = updatedStatusData?.checkOutTime ? true : false
            
            let isCompleted = false
            let isCheckOutMode = false
            let canAttend = true
            
            if (hasAttendance && hasCheckIn) {
              if (hasCheckOut) {
                // 입실과 퇴실 모두 완료
                isCompleted = true
                isCheckOutMode = false
                canAttend = false
              } else {
                // 입실만 완료, 퇴실 대기
                isCompleted = false
                isCheckOutMode = true
                canAttend = false
              }
            } else {
              // 출석 기록 없음
              isCompleted = false
              isCheckOutMode = false
              canAttend = true
            }
            
            return {
              ...prev,
              [selectedCourse.courseId]: {
                courseId: selectedCourse.courseId,
                courseName: selectedCourse.courseName,
                memberId: selectedCourse.memberId,
                statusData: updatedStatusData,
                hasAttendance: hasAttendance,
                hasCheckIn: hasCheckIn,
                hasCheckOut: hasCheckOut,
                isCompleted: isCompleted,
                isCheckOutMode: isCheckOutMode,
                canAttend: canAttend
              }
            }
          })
          
          // UI 상태도 업데이트
          if (updatedStatusData?.hasAttendance && updatedStatusData?.checkOutTime) {
            setIsCheckOutMode(false)
            setCurrentAttendanceInfo(updatedStatusData)
          }
          
        } catch (statusError) {
          console.error('퇴실 후 상태 확인 실패:', statusError)
                     // 상태 확인 실패 시 로컬 상태로 업데이트
           setCourseAttendanceStatuses(prev => ({
             ...prev,
             [selectedCourse.courseId]: {
               ...prev[selectedCourse.courseId],
               isCompleted: true,
               isCheckOutMode: false,
               hasAttendance: true,
               hasCheckIn: true,
               hasCheckOut: true,
               canAttend: false,
               statusData: {
                 ...prev[selectedCourse.courseId]?.statusData,
                 checkOutTime: currentTime.toISOString()
               }
             }
           }))
          setIsCheckOutMode(false)
        }
        
        setAttendanceStatus('checkout_success')
      } else {
        // 출석 처리
        const attendancePayload = {
          courseId: selectedCourse.courseId,
          courseName: selectedCourse.courseName,
          classroomId: selectedClassroom.classroomId,
          classroomName: selectedClassroom.classroomName,
          userId: currentUser.memberId || currentUser.userInfo?.userId || currentUser.userId,
          memberId: selectedCourse.memberId, // 선택된 과정의 memberId 추가
          timestamp: currentTime.toISOString()
        }
        
        console.log('💻 PC 출석 요청 데이터:', attendancePayload)
        const result = await submitPCAttendance(attendancePayload)
        console.log('PC 출석 제출 결과:', result)
        
        // 출석 완료 후 해당 과정의 출석 상태를 다시 확인
        try {
          const updatedStatusData = await getTodayAttendanceStatus(
            currentUser.memberId || currentUser.userInfo?.userId || currentUser.userId,
            selectedCourse.courseId,
            selectedCourse.memberId
          )
          
          console.log('🔍 출석 후 업데이트된 출석 상태:', updatedStatusData)
          
          // 백엔드에서 받은 실제 상태로 업데이트
          setCourseAttendanceStatuses(prev => {
            // 백엔드 데이터를 정확히 해석
            const hasAttendance = updatedStatusData?.hasAttendance || false
            const hasCheckIn = updatedStatusData?.checkInTime ? true : false
            const hasCheckOut = updatedStatusData?.checkOutTime ? true : false
            
            let isCompleted = false
            let isCheckOutMode = false
            let canAttend = true
            
            if (hasAttendance && hasCheckIn) {
              if (hasCheckOut) {
                // 입실과 퇴실 모두 완료
                isCompleted = true
                isCheckOutMode = false
                canAttend = false
              } else {
                // 입실만 완료, 퇴실 대기
                isCompleted = false
                isCheckOutMode = true
                canAttend = false
              }
            } else {
              // 출석 기록 없음
              isCompleted = false
              isCheckOutMode = false
              canAttend = true
            }
            
            return {
              ...prev,
              [selectedCourse.courseId]: {
                courseId: selectedCourse.courseId,
                courseName: selectedCourse.courseName,
                memberId: selectedCourse.memberId,
                statusData: updatedStatusData,
                hasAttendance: hasAttendance,
                hasCheckIn: hasCheckIn,
                hasCheckOut: hasCheckOut,
                isCompleted: isCompleted,
                isCheckOutMode: isCheckOutMode,
                canAttend: canAttend
              }
            }
          })
          
          // UI 상태도 업데이트
          if (updatedStatusData?.hasAttendance && !updatedStatusData?.checkOutTime) {
            setIsCheckOutMode(true)
            setCurrentAttendanceInfo(updatedStatusData)
          }
          
        } catch (statusError) {
          console.error('출석 후 상태 확인 실패:', statusError)
                     // 상태 확인 실패 시 로컬 상태로 업데이트
           setCourseAttendanceStatuses(prev => ({
             ...prev,
             [selectedCourse.courseId]: {
               ...prev[selectedCourse.courseId],
               hasAttendance: true,
               hasCheckIn: true,
               hasCheckOut: false,
               isCompleted: false,
               isCheckOutMode: true,
               canAttend: false,
               statusData: {
                 checkInTime: currentTime.toISOString(),
                 courseName: selectedCourse.courseName,
                 classroomName: selectedClassroom.classroomName
               }
             }
           }))
          setIsCheckOutMode(true)
        }
        
        setAttendanceStatus('success')
      }
      
      setIsLoading(false)
      
    } catch (error) {
      console.error('처리 오류:', error)
      setAttendanceStatus('error')
      setIsLoading(false)
    }
  }

  const handleCancel = () => {
    if (attendanceStatus === 'success' || attendanceStatus === 'checkout_success') {
      // 출석/퇴실 완료 후 출석 기록 페이지로 이동
      window.location.href = '/student/my-courses'
    } else {
      // 취소 시 출석 기록 페이지로 이동
      window.location.href = '/student/my-courses'
    }
  }

  if (isLoading && !attendanceStatus) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-xl shadow-lg p-8 text-center w-full max-w-sm">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto mb-6"></div>
          <p className="text-gray-600 text-lg">수강 과목을 불러오는 중...</p>
        </div>
      </div>
    )
  }

  if (attendanceStatus === 'error') {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-xl shadow-lg p-8 text-center w-full max-w-sm">
          <XCircle className="w-20 h-20 text-red-500 mx-auto mb-6" />
          <h2 className="text-2xl font-bold text-gray-900 mb-4">처리 오류</h2>
          <p className="text-gray-600 mb-8 text-lg">처리 중 오류가 발생했습니다.</p>
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

  if (attendanceStatus === 'success') {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-xl shadow-lg p-8 text-center w-full max-w-sm">
          <CheckCircle className="w-20 h-20 text-green-500 mx-auto mb-6" />
          <h2 className="text-2xl font-bold text-gray-900 mb-4">출석 완료</h2>
          <p className="text-gray-600 mb-8 text-lg">
            {new Date().toLocaleTimeString("ko-KR")}에 출석이 완료되었습니다.
          </p>
          <div className="space-y-4">
            <button
              onClick={() => {
                setAttendanceStatus(null)
                setSelectedCourse("")
                setSelectedClassroom("")
                setIsCheckOutMode(false)
                setCurrentAttendanceInfo(null)
              }}
              className="w-full px-6 py-4 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors text-lg font-medium"
            >
              다른 과정 출석하기
            </button>
            <button
              onClick={handleCancel}
              className="w-full px-6 py-4 bg-gray-600 text-white rounded-xl hover:bg-gray-700 transition-colors text-lg font-medium"
            >
              출석 기록으로 돌아가기
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (attendanceStatus === 'checkout_success') {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-xl shadow-lg p-8 text-center w-full max-w-sm">
          <CheckCircle className="w-20 h-20 text-green-500 mx-auto mb-6" />
          <h2 className="text-2xl font-bold text-gray-900 mb-4">퇴실 완료</h2>
          <p className="text-gray-600 mb-8 text-lg">
            {new Date().toLocaleTimeString("ko-KR")}에 퇴실이 완료되었습니다.
          </p>
          <div className="space-y-4">
            <button
              onClick={() => {
                setAttendanceStatus(null)
                setSelectedCourse("")
                setSelectedClassroom("")
                setIsCheckOutMode(false)
                setCurrentAttendanceInfo(null)
              }}
              className="w-full px-6 py-4 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors text-lg font-medium"
            >
              다른 과정 출석하기
            </button>
            <button
              onClick={handleCancel}
              className="w-full px-6 py-4 bg-gray-600 text-white rounded-xl hover:bg-gray-700 transition-colors text-lg font-medium"
            >
              출석 기록으로 돌아가기
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="p-4">
        <div className="bg-white rounded-xl shadow-lg p-6 max-w-2xl mx-auto">
          <div className="text-center mb-8">
            <div className={`inline-flex items-center justify-center w-16 h-16 rounded-full mb-4 ${
              selectedCourse && courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted ? 'bg-gray-600' : 
              isCheckOutMode ? 'bg-red-600' : 'bg-blue-600'
            }`}>
              {selectedCourse && courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted ? (
                <CheckCircle className="w-8 h-8 text-white" />
              ) : isCheckOutMode ? (
                <LogOut className="w-8 h-8 text-white" />
              ) : (
                <LogIn className="w-8 h-8 text-white" />
              )}
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">PC 출석/퇴실</h2>
            <p className="text-gray-600 text-lg">
              {selectedCourse && courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted 
                ? '오늘 출석/퇴실이 완료되었습니다.'
                : isCheckOutMode 
                  ? '수강 중인 과정과 강의실을 선택하여 퇴실하세요.' 
                  : '수강 중인 과정과 강의실을 선택하여 출석하세요.'
              }
            </p>
          </div>

          {/* 이미 완료된 상태 표시 */}
          {selectedCourse && courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted && currentAttendanceInfo && (
            <div className="bg-green-50 rounded-xl p-6 mb-6">
              <h3 className="text-lg font-semibold text-green-900 mb-4">이 과정 출석/퇴실 완료</h3>
              <div className="space-y-3 text-green-700">
                <div className="flex items-center space-x-3">
                  <CheckCircle className="w-5 h-5 flex-shrink-0" />
                  <span>상태: {currentAttendanceInfo.attendanceStatus || '출석/퇴실 완료'}</span>
                </div>
                <div className="flex items-center space-x-3">
                  <Clock className="w-5 h-5 flex-shrink-0" />
                  <span>출석 시간: {new Date(currentAttendanceInfo.checkInTime).toLocaleString("ko-KR")}</span>
                </div>
                <div className="flex items-center space-x-3">
                  <Clock className="w-5 h-5 flex-shrink-0" />
                  <span>퇴실 시간: {new Date(currentAttendanceInfo.checkOutTime).toLocaleString("ko-KR")}</span>
                </div>
                <div className="flex items-center space-x-3">
                  <Calendar className="w-5 h-5 flex-shrink-0" />
                  <span>강의 날짜: {currentAttendanceInfo.lectureDate || new Date().toISOString().split('T')[0]}</span>
                </div>
              </div>
            </div>
          )}

          {/* 현재 출석 상태 표시 (퇴실 모드일 때만) */}
          {isCheckOutMode && currentAttendanceInfo && selectedCourse && !courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted && (
            <div className="bg-blue-50 rounded-xl p-6 mb-6">
              <h3 className="text-lg font-semibold text-blue-900 mb-4">현재 출석 정보</h3>
              <div className="space-y-3 text-blue-700">
                <div className="flex items-center space-x-3">
                  <BookOpen className="w-5 h-5 flex-shrink-0" />
                  <span>과정: {currentAttendanceInfo.courseName || selectedCourse.courseName}</span>
                </div>
                <div className="flex items-center space-x-3">
                  <MapPin className="w-5 h-5 flex-shrink-0" />
                  <span>강의실: {currentAttendanceInfo.classroomName || selectedClassroom?.classroomName}</span>
                </div>
                <div className="flex items-center space-x-3">
                  <Clock className="w-5 h-5 flex-shrink-0" />
                  <span>출석 시간: {new Date(currentAttendanceInfo.checkInTime).toLocaleString("ko-KR")}</span>
                </div>
              </div>
            </div>
          )}

          {/* 과목 선택 */}
          <div className="space-y-2 mb-6">
            <label className="text-sm font-medium text-gray-700">수강 과정</label>
            <div className="relative">
              <button
                type="button"
                onClick={() => setIsCourseOpen(!isCourseOpen)}
                className="w-full flex items-center justify-between px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
              >
                <span className={selectedCourse ? "text-gray-900" : "text-gray-500"}>
                  {selectedCourse ? selectedCourse.courseName : "과정을 선택하세요"}
                </span>
                <ChevronDown className={`w-5 h-5 text-gray-400 transition-transform ${isCourseOpen ? 'rotate-180' : ''}`} />
              </button>
              
              {isCourseOpen && (
                <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-xl shadow-lg max-h-60 overflow-y-auto">
                  {courses.length > 0 ? (
                    courses.map((course, index) => {
                      const courseStatus = courseAttendanceStatuses[course.courseId]
                      const isCompleted = courseStatus?.isCompleted
                      const isCheckOutMode = courseStatus?.isCheckOutMode
                      
                      return (
                        <button
                          key={index}
                          onClick={() => handleCourseSelect(course)}
                          disabled={isCompleted}
                          className={`w-full px-4 py-3 text-left border-b border-gray-100 last:border-b-0 transition-colors ${
                            isCompleted 
                              ? 'bg-gray-100 text-gray-400 cursor-not-allowed' 
                              : 'hover:bg-gray-50'
                          }`}
                        >
                          <div className="flex items-center justify-between">
                            <div className="font-medium text-gray-900">{course.courseName}</div>
                            <div className="flex items-center space-x-2">
                              {isCompleted && (
                                <div className="flex items-center space-x-1 text-green-600">
                                  <CheckCircle className="w-4 h-4" />
                                  <span className="text-xs">완료</span>
                                </div>
                              )}
                              {isCheckOutMode && !isCompleted && (
                                <div className="flex items-center space-x-1 text-red-600">
                                  <LogOut className="w-4 h-4" />
                                  <span className="text-xs">퇴실 대기</span>
                                </div>
                              )}
                              {!courseStatus?.hasAttendance && (
                                <div className="flex items-center space-x-1 text-blue-600">
                                  <LogIn className="w-4 h-4" />
                                  <span className="text-xs">출석 가능</span>
                                </div>
                              )}
                            </div>
                          </div>
                        </button>
                      )
                    })
                  ) : (
                    <div className="px-4 py-3 text-gray-500">수강 중인 과정이 없습니다.</div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* 강의실 선택 */}
          <div className="space-y-2 mb-8">
            <label className="text-sm font-medium text-gray-700">강의실</label>
            <div className="relative">
              <button
                type="button"
                onClick={() => setIsClassroomOpen(!isClassroomOpen)}
                disabled={!selectedCourse || (selectedCourse && courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted)}
                className="w-full flex items-center justify-between px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors disabled:bg-gray-100 disabled:cursor-not-allowed"
              >
                <span className={selectedClassroom ? "text-gray-900" : "text-gray-500"}>
                  {selectedClassroom ? `${selectedClassroom.classroomName} ${selectedClassroom.classroomNumber}호` : "강의실을 선택하세요"}
                </span>
                <ChevronDown className={`w-5 h-5 text-gray-400 transition-transform ${isClassroomOpen ? 'rotate-180' : ''}`} />
              </button>
              
              {isClassroomOpen && selectedCourse && !courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted && (
                <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-xl shadow-lg max-h-60 overflow-y-auto">
                  {classrooms.length > 0 ? (
                    classrooms.map((classroom, index) => (
                      <button
                        key={index}
                        onClick={() => handleClassroomSelect(classroom)}
                        className="w-full px-4 py-3 text-left hover:bg-gray-50 border-b border-gray-100 last:border-b-0"
                      >
                        <div className="font-medium text-gray-900">{classroom.classroomName} {classroom.classroomNumber}호</div>
                      </button>
                    ))
                  ) : (
                    <div className="px-4 py-3 text-gray-500">사용 가능한 강의실이 없습니다.</div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* 선택된 정보 표시 */}
          {selectedCourse && (
            <div className="bg-blue-50 rounded-xl p-6 mb-8">
              <h3 className="text-lg font-semibold text-blue-900 mb-4">선택된 정보</h3>
              <div className="space-y-3 text-blue-700">
                <div className="flex items-center space-x-3">
                  <BookOpen className="w-5 h-5 flex-shrink-0" />
                  <span>과정: {selectedCourse.courseName}</span>
                </div>
                {selectedClassroom && (
                  <div className="flex items-center space-x-3">
                    <MapPin className="w-5 h-5 flex-shrink-0" />
                    <span>강의실: {selectedClassroom.classroomName} {selectedClassroom.classroomNumber}호</span>
                  </div>
                )}
                <div className="flex items-center space-x-3">
                  <Clock className="w-5 h-5 flex-shrink-0" />
                  <span>{isCheckOutMode ? '퇴실' : '출석'} 시간: {new Date().toLocaleString("ko-KR")}</span>
                </div>
                {/* 과정별 상태 표시 */}
                {courseAttendanceStatuses[selectedCourse.courseId] && (
                  <div className="flex items-center space-x-3">
                    <div className="w-5 h-5 flex-shrink-0" />
                    <span className="text-sm">
                      상태: {
                        courseAttendanceStatuses[selectedCourse.courseId].isCompleted ? '출석/퇴실 완료' :
                        courseAttendanceStatuses[selectedCourse.courseId].isCheckOutMode ? '출석 완료 (퇴실 대기)' :
                        '출석 가능'
                      }
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 버튼 */}
          <div className="space-y-4">
            {selectedCourse && courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted ? (
              <div className="text-center">
                <div className="bg-gray-100 rounded-xl p-6">
                  <CheckCircle className="w-12 h-12 text-green-600 mx-auto mb-4" />
                  <p className="text-gray-700 font-medium">이 과정은 오늘 출석/퇴실이 완료되었습니다</p>
                  <p className="text-gray-500 text-sm mt-2">다른 과정을 선택하거나 내일 다시 시도해주세요</p>
                </div>
              </div>
            ) : (
              <button
                onClick={handleAttendance}
                disabled={isLoading || !selectedCourse || !selectedClassroom || (selectedCourse && courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted)}
                className={`w-full px-6 py-4 text-white rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-lg font-medium flex items-center justify-center space-x-2 ${
                  isCheckOutMode 
                    ? 'bg-[#1abc9c] hover:bg-[rgb(10,150,120)]' 
                    : 'bg-[#1abc9c] hover:bg-[rgb(10,150,120)]'
                }`}
              >
                {isLoading ? (
                  <>
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                    <span>{isCheckOutMode ? '퇴실 처리 중...' : '출석 처리 중...'}</span>
                  </>
                ) : (
                  <span>{isCheckOutMode ? '퇴실하기' : '출석하기'}</span>
                )}
              </button>
            )}
            
            <button
              onClick={handleCancel}
              className="w-full px-6 py-4 bg-gray-600 text-white rounded-xl hover:bg-gray-700 transition-colors text-lg font-medium"
            >
              돌아가기
            </button>
          </div>

          {/* 주의사항 */}
          <div className="mt-8 p-6 bg-yellow-50 rounded-xl">
            <h4 className="text-base font-medium text-yellow-900 mb-3">주의사항</h4>
            <ul className="text-sm text-yellow-700 space-y-2">
              {selectedCourse && courseAttendanceStatuses[selectedCourse.courseId]?.isCompleted ? (
                <>
                  <li>• 이 과정은 오늘 출석/퇴실이 이미 완료되었습니다</li>
                  <li>• 각 과정별로 하루에 한 번만 출석/퇴실이 가능합니다</li>
                  <li>• 출석/퇴실 후에는 수정할 수 없습니다</li>
                  <li>• 다른 과정을 선택하거나 내일 다시 시도해주세요</li>
                </>
              ) : isCheckOutMode ? (
                <>
                  <li>• 퇴실은 한 번만 가능합니다</li>
                  <li>• 퇴실 후에는 수정할 수 없습니다</li>
                  <li>• 정확한 과정과 강의실을 선택해주세요</li>
                </>
              ) : (
                <>
                  <li>• 각 과정별로 출석은 한 번만 가능합니다</li>
                  <li>• 출석 후에는 수정할 수 없습니다</li>
                  <li>• 정확한 과정과 강의실을 선택해주세요</li>
                  <li>• 여러 과정을 듣는 경우 각 과정별로 개별 출석/퇴실이 필요합니다</li>
                </>
              )}
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
} 