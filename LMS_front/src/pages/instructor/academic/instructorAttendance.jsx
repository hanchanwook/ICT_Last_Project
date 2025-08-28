import { useState, useEffect, useRef } from "react"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Search, Users, Calendar, CheckCircle, XCircle, Clock, Download, Edit, RefreshCw } from "lucide-react"
import { QRCodeSVG } from "qrcode.react"
import { getMenuItems } from "@/components/ui/menuConfig"
import { 
  getAttendanceStatus, 
  exportAttendanceData,
  createQRSession,
  getClassroomQRAttendance,
  getInstructorClassrooms
} from "@/api/hancw/instructorAcademicAxios"

export default function InstructorAttendancePage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedCourseFilter, setSelectedCourseFilter] = useState("all")
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [courses, setCourses] = useState([])
  const [attendanceData, setAttendanceData] = useState([])
  const [selectedCourseForStats, setSelectedCourseForStats] = useState(null)
  const [stats, setStats] = useState({
    totalRecords: 0,
    attendedCount: 0,
    lateCount: 0,
    absentCount: 0,
    attendanceRate: 0
  })

  // QR 관련 상태들
  const [qrCode, setQrCode] = useState("")
  const [currentTime, setCurrentTime] = useState(new Date())
  const [qrAttendanceData, setQrAttendanceData] = useState([])
  const [selectedClassroom, setSelectedClassroom] = useState(null)
  const [selectedCourse, setSelectedCourse] = useState(null)
  const [classrooms, setClassrooms] = useState([])
  const [showQRModal, setShowQRModal] = useState(false)
  const [qrSessionId, setQrSessionId] = useState(null)
  const [localIP, setLocalIP] = useState("")
  const [customIP, setCustomIP] = useState("")
  const [selectedCourseForQR, setSelectedCourseForQR] = useState(null)
  const qrRef = useRef(null)

  // 강사 전용 사이드바 메뉴
  const sidebarItems = getMenuItems('instructor-academic')

  // 현재 시간 업데이트 - 1초마다 업데이트 제거
  useEffect(() => {
    setCurrentTime(new Date())
  }, [])

  // IP 주소 자동 감지
  const getLocalIP = async () => {
    try {
      // WebRTC를 사용하여 로컬 IP 주소 감지
      const rtc = new RTCPeerConnection({ iceServers: [] })
      rtc.createDataChannel('')
      rtc.createOffer().then(offer => rtc.setLocalDescription(offer))
      
      rtc.onicecandidate = (event) => {
        if (event.candidate) {
          const ipMatch = event.candidate.candidate.match(/([0-9]{1,3}(\.[0-9]{1,3}){3})/)
          if (ipMatch && ipMatch[1]) {
            const ip = ipMatch[1]
            // 로컬 IP 주소만 필터링 (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
            if (ip.startsWith('192.168.') || ip.startsWith('10.') || /^172\.(1[6-9]|2[0-9]|3[0-1])\./.test(ip)) {
              setLocalIP(ip)
              console.log('📱 감지된 로컬 IP:', ip)
            }
          }
        }
      }
      
      // 3초 후 연결 종료
      setTimeout(() => {
        rtc.close()
      }, 3000)
    } catch (error) {
      console.error('IP 주소 감지 실패:', error)
    }
  }

  // 강의실과 과정 유효성 검증 함수
  const validateClassroomAndCourse = async (classroom, course) => {
    try {
      console.log('🔍 강의실과 과정 유효성 검증 시작:', { classroom, course })
      
      // 현재 로그인한 사용자 정보 가져오기
      const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')
      const memberId = currentUser.memberId
      
      if (!memberId) {
        throw new Error('사용자 정보를 찾을 수 없습니다.')
      }
      
      // 1. 강의실 유효성 검증
      if (!classroom || !classroom.classId) {
        throw new Error('유효하지 않은 강의실입니다.')
      }
      
      // 2. 과정 유효성 검증
      if (!course || !course.courseId) {
        throw new Error('유효하지 않은 과정입니다.')
      }
      
      // 3. 강사가 해당 과정을 담당할 수 있는지 확인
      const instructorCourses = await getAttendanceStatus(memberId)
      
      if (!instructorCourses || !instructorCourses.attendanceStatus) {
        throw new Error('강사 정보를 확인할 수 없습니다.')
      }
      
      const hasPermission = instructorCourses.attendanceStatus.some(
        record => record.courseId === course.courseId
      )
      
      if (!hasPermission) {
        throw new Error('해당 과정을 담당할 권한이 없습니다.')
      }
      
      // 4. 강의실과 과정이 연결되어 있는지 확인
      const isClassroomCourseValid = classroom.courseInfo && 
        (classroom.courseInfo.courseId === course.courseId || 
         classroom.courseInfo.courseld === course.courseId)
      
      if (!isClassroomCourseValid) {
        console.warn('⚠️ 강의실과 과정이 직접 연결되지 않았습니다. 계속 진행합니다.')
      }
      
      console.log('✅ 강의실과 과정 유효성 검증 완료')
      return true
      
    } catch (error) {
      console.error('❌ 강의실과 과정 유효성 검증 실패:', error)
      alert(error.message)
      return false
    }
  }

  // QR 출석 현황 조회 함수
  const fetchQRAttendanceData = async () => {
    if (!selectedClassroom?.classId) return
    
    try {
      console.log('📊 QR 출석 현황 조회:', selectedClassroom.classId)
      const data = await getClassroomQRAttendance(selectedClassroom.classId)
      console.log('📋 QR 출석 현황 데이터:', data)
      
      // 출석 데이터 설정
      if (Array.isArray(data)) {
        setQrAttendanceData(data)
      } else if (data && Array.isArray(data.content)) {
        // 페이지네이션 응답인 경우
        setQrAttendanceData(data.content)
      } else if (data && Array.isArray(data.attendanceList)) {
        // attendanceList 형태인 경우
        setQrAttendanceData(data.attendanceList)
      } else if (data && Array.isArray(data.attendanceRecords)) {
        // attendanceRecords 형태인 경우 (QR 출석 현황)
        console.log('📊 QR 출석 현황 데이터:', data.attendanceRecords)
        setQrAttendanceData(data.attendanceRecords)
      } else {
        console.warn('예상하지 못한 데이터 형태:', data)
        setQrAttendanceData([])
      }
      
    } catch (error) {
      console.error('QR 출석 현황 조회 실패:', error)
      setQrAttendanceData([])
    }
  }

  // QR 코드 생성 함수
  const generateNewQR = async () => {
    console.log('🎯 QR 생성 시도 - 선택된 과정:', selectedCourseForQR)
    
    if (!selectedCourseForQR) {
      console.log('❌ QR용 과정이 선택되지 않음')
      alert("QR 코드를 생성할 과정을 먼저 선택해주세요.")
      return
    }
    
    // 선택된 과정에 해당하는 강의실 찾기 - classroomId로 직접 매칭
    console.log('🔍 classrooms 배열:', classrooms)
    console.log('🔍 selectedCourseForQR.classroomId:', selectedCourseForQR.classroomId)
    
    const courseClassroom = classrooms.find(classroom => 
      classroom.classId === selectedCourseForQR.classroomId ||
      classroom.classroomId === selectedCourseForQR.classroomId
    )
    
    if (!courseClassroom) {
      console.log('❌ 선택된 과정에 해당하는 강의실을 찾을 수 없음')
      console.log('🔍 찾으려는 classroomId:', selectedCourseForQR.classroomId)
      console.log('🔍 사용 가능한 classrooms:', classrooms.map(c => ({ classId: c.classId, classroomId: c.classroomId })))
      alert("선택된 과정에 해당하는 강의실을 찾을 수 없습니다.")
      return
    }
    
    const classroom = courseClassroom
    const course = selectedCourseForQR
    
    // QR 생성 전 최종 유효성 검증
    const isValid = await validateClassroomAndCourse(classroom, course)
    if (!isValid) {
      console.log('❌ 유효성 검증 실패로 QR 생성 취소')
      return
    }

    try {
      // 현재 로그인한 사용자 정보 가져오기
      const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')
      const memberId = currentUser.memberId
      
      // 선택된 과정 정보 사용
      const matchingCourse = course
      
      console.log('🔍 선택된 강의실 정보:', {
        classId: classroom.classId,
        classCode: classroom.classCode,
        classNumber: classroom.classNumber,
        classCapacity: classroom.classCapacity
      })
      console.log('📚 선택된 과정:', matchingCourse)

      // 현재 시간 기준으로 강의 시간 설정 (실제 과정 시간 사용)
      const today = new Date()
      
      // 과정의 실제 수업 시간 사용
      let lectureStartTime, lectureEndTime
      
      if (matchingCourse.startTime && matchingCourse.endTime) {
        // 과정에 설정된 시간이 있는 경우
        const [startHour, startMinute] = matchingCourse.startTime.split(':').map(Number)
        const [endHour, endMinute] = matchingCourse.endTime.split(':').map(Number)
        
        lectureStartTime = new Date(today)
        lectureStartTime.setHours(startHour, startMinute, 0, 0)
        
        lectureEndTime = new Date(today)
        lectureEndTime.setHours(endHour, endMinute, 0, 0)
      } else {
        // 기본값 (09:00-18:00)
        lectureStartTime = new Date(today)
        lectureStartTime.setHours(9, 0, 0, 0)
        
        lectureEndTime = new Date(today)
        lectureEndTime.setHours(18, 0, 0, 0)
      }
      
      // QR 세션 만료 시간 (24시간 후)
      const expiryTime = new Date()
      expiryTime.setHours(expiryTime.getHours() + 24)

      // QR 세션 생성 - courseId 포함
      const sessionData = {
        classroomId: classroom.classId,
        classroomName: classroom.classCode,
        classroomNumber: classroom.classNumber,
        courseId: matchingCourse.courseId,
        courseName: matchingCourse.courseName,
        instructorId: memberId, // 실제 로그인된 강사 ID 사용
        lectureDate: today.toISOString().split('T')[0], // YYYY-MM-DD
        lectureStartTime: lectureStartTime.toISOString(),
        lectureEndTime: lectureEndTime.toISOString(),
        qrExpiryTime: expiryTime.toISOString(),
        createdAt: new Date().toISOString(),
        status: 'ACTIVE' // QR 세션 상태
      }
      
      console.log('📋 QR 세션 데이터:', sessionData)
      
      const sessionResponse = await createQRSession(sessionData)
      console.log('✅ QR 세션 생성 응답:', sessionResponse)
      
      setQrSessionId(sessionResponse.sessionId)
      
      // QR 코드에 포함할 정보를 JSON 형태로 구성
      const qrPayload = {
        sessionId: sessionResponse.sessionId,
        classroomId: classroom.classId,
        classroomName: classroom.classCode,
        classroomNumber: classroom.classNumber,
        courseId: matchingCourse.courseId,
        courseName: matchingCourse.courseName,
        lectureDate: sessionData.lectureDate,
        lectureStartTime: sessionData.lectureStartTime,
        lectureEndTime: sessionData.lectureEndTime,
        qrExpiryTime: sessionData.qrExpiryTime,
        type: 'ATTENDANCE_QR'
      }
      
      // QR 코드 URL 생성 - JSON 데이터를 base64로 인코딩
      const qrDataString = JSON.stringify(qrPayload)
      const qrDataEncoded = btoa(encodeURIComponent(qrDataString))
      
      // 모바일 접속을 위해 IP 주소 사용
      const currentHost = window.location.hostname
      const currentPort = window.location.port
      const protocol = window.location.protocol
      
      // localhost인 경우 사용자 입력 IP 또는 감지된 IP 사용
      let mobileHost = currentHost
      if (currentHost === 'localhost' || currentHost === '127.0.0.1') {
        if (customIP) {
          mobileHost = customIP
          console.log('📱 QR 코드에 사용할 IP (사용자 입력):', customIP)
        } else if (localIP) {
          mobileHost = localIP
          console.log('📱 QR 코드에 사용할 IP (감지됨):', localIP)
        } else {
          // IP가 없는 경우 사용자에게 안내
          alert('모바일 접속을 위해 IP 주소를 입력해주세요.\n\nIP 주소 확인 방법:\n1. 명령 프롬프트에서 "ipconfig" 실행\n2. Wi-Fi 어댑터의 IPv4 주소 확인\n3. 아래 입력창에 IP 주소 입력')
          return
        }
      }
      
      const qrData = `${protocol}//${mobileHost}:${currentPort}/mobile/login?data=${qrDataEncoded}`
      
      setQrCode(qrData)

      // 출석 데이터 초기화
      setQrAttendanceData(sessionResponse.initialAttendance || [])

      // 강의 시간 표시 형식 생성
      const timeDisplay = matchingCourse.startTime && matchingCourse.endTime 
        ? `${matchingCourse.startTime}-${matchingCourse.endTime}`
        : '09:00-18:00 (기본값)'
      
      alert(`${classroom.classCode} 강의실의 QR 코드가 생성되었습니다.\n\n📚 과정: ${matchingCourse.courseName}\n📅 강의일: ${sessionData.lectureDate}\n⏰ 강의시간: ${timeDisplay}\n⏳ 유효기간: 24시간\n\n학생들이 이 QR 코드를 스캔하면 출석 확인 페이지로 이동합니다.`)
    } catch (error) {
      console.error('QR 생성 실패:', error)
      alert('QR 코드 생성에 실패했습니다.')
    }
  }

  const downloadQRCode = () => {
    if (!qrRef.current) return

    try {
      // SVG 요소를 가져와서 캔버스로 변환
      const svg = qrRef.current.querySelector('svg')
      const svgData = new XMLSerializer().serializeToString(svg)
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      const img = new Image()

      // SVG를 이미지로 변환
      const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' })
      const url = URL.createObjectURL(svgBlob)

      img.onload = () => {
        canvas.width = 500
        canvas.height = 500
        ctx.fillStyle = 'white'
        ctx.fillRect(0, 0, canvas.width, canvas.height)
        ctx.drawImage(img, 0, 0, 500, 500)

        // 캔버스를 PNG로 다운로드
        canvas.toBlob((blob) => {
          const downloadUrl = URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = downloadUrl
          link.download = `QR_${selectedClassroom?.classCode || "attendance"}_${new Date().toISOString().split("T")[0]}.png`
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          URL.revokeObjectURL(downloadUrl)
          URL.revokeObjectURL(url)
        }, 'image/png')
      }

      img.src = url
      alert("QR 코드가 다운로드되었습니다.")
    } catch (error) {
      console.error("다운로드 실패:", error)
      alert("다운로드에 실패했습니다. 다시 시도해주세요.")
    }
  }

  // 데이터 로드
  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true)
        setError(null)
        
        // 현재 사용자 정보 가져오기
        const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}')
        const memberId = currentUser.memberId
        if (!memberId) {
          console.warn('사용자 정보가 없습니다.')
          setError('사용자 정보를 찾을 수 없습니다.')
          return
        }
        
        // IP 주소 감지 시작
        getLocalIP()
        
        // 담당 과정 현황 조회와 강사별 강의실 목록을 병렬로 로드
        const [attendanceData, classroomsData] = await Promise.all([
          getAttendanceStatus(memberId, { date: selectedDate }),
          getInstructorClassrooms(memberId)
        ])
        
                 // 디버깅을 위한 로그
         console.log('출석 현황 데이터:', attendanceData)
         console.log('attendanceStatus 배열:', attendanceData?.attendanceStatus)
         if (attendanceData?.attendanceStatus?.length > 0) {
           console.log('첫 번째 과정 데이터:', attendanceData.attendanceStatus[0])
           console.log('첫 번째 과정의 totalStudents:', attendanceData.attendanceStatus[0].totalStudents)
           console.log('첫 번째 과정의 presentCount:', attendanceData.attendanceStatus[0].presentCount)
           console.log('첫 번째 과정의 lateCount:', attendanceData.attendanceStatus[0].lateCount)
         }
        
        // 출석 데이터 구조 확인 및 매핑 (DB 스키마 기반)
        if (attendanceData && typeof attendanceData === 'object') {
          // 백엔드 응답 구조: {totalCount: 2, attendanceStatus: Array(2)}
          const attendanceRecords = Array.isArray(attendanceData.attendanceStatus) ? attendanceData.attendanceStatus : []
          const totalCount = attendanceData.totalCount || 0
          
          // 과정 목록은 attendanceStatus에서 추출 (과정별 출석 현황)
          // DB 스키마에 맞춰 필드명 매핑
          const mappedCourses = attendanceRecords.map(record => {
            // 강의실 정보 찾기
            let classroomInfo = null
            if (classroomsData && Array.isArray(classroomsData)) {
              // 더 자세한 디버깅을 위한 로그
              console.log('🔍 classroomsData 구조 확인:')
              classroomsData.forEach((classroom, index) => {
                console.log(`🔍 classroom ${index}:`, classroom)
                console.log(`🔍 classroom ${index}의 모든 키:`, Object.keys(classroom))
                if (classroom.courseInfo) {
                  console.log(`🔍 classroom ${index}의 courseInfo:`, classroom.courseInfo)
                  console.log(`🔍 classroom ${index}의 courseInfo.courseId:`, classroom.courseInfo.courseId)
                  console.log(`🔍 record.courseId:`, record.courseId)
                  console.log(`🔍 매칭 여부:`, classroom.courseInfo.courseId === record.courseId)
                }
              })
              
              classroomInfo = classroomsData.find(classroom => 
                classroom.courseInfo && classroom.courseInfo.courseId === record.courseId
              )
            }
            
            // 디버깅용 로그
            console.log('🔍 과정 매핑 - record.courseId:', record.courseId)
            console.log('🔍 찾은 classroomInfo:', classroomInfo)
            console.log('🔍 전체 classroomsData:', classroomsData)
            console.log('🔍 record 전체 데이터:', record)
            
            // classroomInfo의 모든 키 확인
            if (classroomInfo) {
              console.log('🔍 classroomInfo의 모든 키:', Object.keys(classroomInfo))
              console.log('🔍 classroomInfo.classNumber:', classroomInfo.classNumber)
              console.log('🔍 classroomInfo.classroomNumber:', classroomInfo.classroomNumber)
              console.log('🔍 classroomInfo.roomNumber:', classroomInfo.roomNumber)
            }
            
            // record의 모든 키 확인
            console.log('🔍 record의 모든 키:', Object.keys(record))
            console.log('🔍 record.classNumber:', record.classNumber)
            console.log('🔍 record.classroomNumber:', record.classroomNumber)
            console.log('🔍 record.roomNumber:', record.roomNumber)
            
                        const mappedCourse = {
              courseId: record.courseId,
              courseName: record.courseName,
              courseCode: record.courseCode,
              courseActive: record.courseActive,
              courseDays: record.courseDays,
              courseEndDay: record.courseEndDay,
              courseStartDay: record.courseStartDay,
              educationId: record.educationId,
              endTime: record.endTime,
              maxCapacity: record.maxCapacity,
              memberId: record.memberId,
              minCapacity: record.minCapacity,
              startTime: record.startTime,
              // 강의실 관련 필드
              classroomId: record.classroomId || record.classId,
              classroomName: classroomInfo ? `${classroomInfo.classCode} - ${classroomInfo.classNumber}호` : '미정',
              classroomCode: classroomInfo?.classCode || record.classCode || record.className || '미정',
              classroomNumber: classroomInfo?.classNumber || record.classNumber || '미정',
              
              // 디버깅: 최종 매핑 결과 확인
              _debug: {
                hasClassroomInfo: !!classroomInfo,
                classroomInfoKeys: classroomInfo ? Object.keys(classroomInfo) : [],
                recordClassroomKeys: Object.keys(record).filter(key => key.includes('class') || key.includes('room')),
                finalClassroomNumber: classroomInfo?.classNumber || record.classNumber || '미정'
              },
              // 출석 관련 필드
              totalStudents: record.totalStudents,
              presentCount: record.presentCount,
              lateCount: record.lateCount,
              absentCount: (record.totalStudents || 0) - (record.presentCount || 0) - (record.lateCount || 0),
              attendanceRate: record.attendanceRate
            }
            
            console.log('🎯 최종 매핑된 과정:', {
              courseName: mappedCourse.courseName,
              classroomCode: mappedCourse.classroomCode,
              classroomNumber: mappedCourse.classroomNumber,
              debug: mappedCourse._debug
            })
            
            return mappedCourse
          })
          
          setCourses(mappedCourses)
          setAttendanceData([]) // 개별 출석 기록은 별도 API 필요
          
                     // 통계 계산 - 과정별 통계를 합산
           console.log('🔍 각 과정별 데이터:')
           attendanceRecords.forEach((record, index) => {
             console.log(`과정 ${index + 1}:`, {
               courseName: record.courseName,
               totalStudents: record.totalStudents,
               presentCount: record.presentCount,
               lateCount: record.lateCount,
               absentCount: (record.totalStudents || 0) - (record.presentCount || 0) - (record.lateCount || 0)
             })
           })
           
           const totalStudents = attendanceRecords.reduce((sum, item) => sum + (item.totalStudents || 0), 0)
           console.log('📊 통계 계산 - totalCount:', totalCount, 'totalStudents:', totalStudents)
           
           const attendedCount = attendanceRecords.reduce((sum, item) => sum + (item.presentCount || 0), 0)
           const lateCount = attendanceRecords.reduce((sum, item) => sum + (item.lateCount || 0), 0)
           const absentCount = attendanceRecords.reduce((sum, item) => {
             const total = item.totalStudents || 0
             const present = item.presentCount || 0
             const late = item.lateCount || 0
             return sum + (total - present - late)
           }, 0)
           const attendanceRate = totalStudents > 0 ? Math.round((attendedCount / totalStudents) * 100) : 0
          
          console.log('📊 통계 계산 결과:', {
            totalStudents,
            attendedCount,
            lateCount,
            absentCount,
            attendanceRate
          })
          
          setStats({
            totalStudents,
            attendedCount,
            lateCount,
            absentCount,
            attendanceRate
          })
          
          // 강의실 데이터 처리
          console.log('🔍 로드된 강의실 데이터:', classroomsData)
          
          // 강의실 데이터가 배열인지 확인하고 안전하게 설정
          let classroomsArray = []
          if (Array.isArray(classroomsData)) {
            classroomsArray = classroomsData
          } else if (classroomsData && Array.isArray(classroomsData.content)) {
            // 페이지네이션 응답인 경우
            classroomsArray = classroomsData.content
          } else if (classroomsData && Array.isArray(classroomsData.classrooms)) {
            // classrooms 형태인 경우
            classroomsArray = classroomsData.classrooms
          } else if (classroomsData && classroomsData.classroomInfo) {
            // 단일 강의실 객체인 경우 (classroomInfo + courseInfo 구조)
            classroomsArray = [{
              classId: classroomsData.classroomInfo.classroomld || classroomsData.classroomInfo.classroomId,
              classCode: classroomsData.classroomInfo.classCode,
              classNumber: classroomsData.classroomInfo.classNumber,
              classCapacity: classroomsData.classroomInfo.classCapacity,
              courseInfo: classroomsData.courseInfo
            }]
          } else {
            console.warn('예상하지 못한 강의실 데이터 형태:', classroomsData)
            classroomsArray = []
          }
          
          setClassrooms(classroomsArray)
          
          // 첫 번째 강의실과 과정을 기본 선택
          if (classroomsArray.length > 0) {
            console.log('✅ 첫 번째 강의실을 기본 선택:', classroomsArray[0])
            setSelectedClassroom(classroomsArray[0])
          }
          
          if (mappedCourses.length > 0) {
            console.log('✅ 첫 번째 과정을 기본 선택:', mappedCourses[0])
            setSelectedCourse(mappedCourses[0])
          }
          
        } else {
          setCourses([])
          setAttendanceData([])
          setClassrooms([])
          setStats({
            totalStudents: 0,
            attendedCount: 0,
            lateCount: 0,
            absentCount: 0,
            attendanceRate: 0
          })
        }
      } catch (err) {
        console.error('데이터 로드 실패:', err)
        if (err.code === 'ERR_NETWORK') {
          setError('백엔드 서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.')
        } else {
          setError('데이터를 불러오는데 실패했습니다.')
        }
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [selectedDate]) // selectedDate가 변경될 때마다 데이터 다시 로드

  // 선택된 QR용 과정이 변경될 때마다 QR 출석 현황 조회
  useEffect(() => {
    if (selectedCourseForQR?.courseId) {
      const courseClassroom = classrooms.find(classroom => 
        classroom.courseInfo && classroom.courseInfo.courseId === selectedCourseForQR.courseId
      )
      if (courseClassroom) {
        setSelectedClassroom(courseClassroom)
        fetchQRAttendanceData()
      }
    }
  }, [selectedCourseForQR, classrooms])

  // 과정별 출석 현황 필터링
  const filteredCourses = (Array.isArray(courses) ? courses : []).filter((course) => {
    const courseName = course.courseName || ''
    const matchesSearch = courseName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (course.courseId || '').toLowerCase().includes(searchTerm.toLowerCase())
    const matchesCourse = selectedCourseFilter === "all" || course.courseId === selectedCourseFilter
    return matchesSearch && matchesCourse
  })

  // 요일 순서 정렬 함수
  const sortWeekdays = (weekdays) => {
    if (!weekdays) return '미정'
    
    const weekdayOrder = ['월', '화', '수', '목', '금', '토', '일']
    const days = weekdays.split(',').map(day => day.trim())
    
    return days
      .sort((a, b) => weekdayOrder.indexOf(a) - weekdayOrder.indexOf(b))
      .join(' ')
  }

  // QR 출석 현황 필터링 (오늘 날짜 기준)
  const safeQRAttendanceData = Array.isArray(qrAttendanceData) ? qrAttendanceData : []
  const today = new Date().toISOString().split('T')[0] // YYYY-MM-DD 형식
  
  const attendedStudents = safeQRAttendanceData.filter((attendance) => 
    attendance?.attendanceStatus === "출석" && 
    attendance?.lectureDate === today
  )

  const handleExportData = async () => {
    try {
      // 선택된 과정이 있으면 해당 과정의 courseId를, 없으면 전체 과정으로 설정
      const courseId = selectedCourseForStats?.courseId || (selectedCourseFilter !== "all" ? selectedCourseFilter : undefined)
      
      const params = {
        courseId: courseId,
        date: selectedDate,  // 선택된 날짜 추가
        searchTerm: searchTerm || undefined
      }
      
      console.log('📊 엑셀 다운로드 파라미터:', params)
      
      const blob = await exportAttendanceData(params)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      
      // 파일명에 과정명과 날짜 포함
      const courseName = selectedCourseForStats?.courseName || '전체'
      link.download = `출석현황_${courseName}_${selectedDate}.xlsx`
      
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error('엑셀 다운로드 실패:', error)
      alert('엑셀 다운로드에 실패했습니다.')
    }
  }



     const handleCourseSelect = (course) => {
     console.log('🎯 과정 선택됨:', course)
     setSelectedCourseForStats(course)
     setSelectedCourseForQR(course) // QR 코드 생성을 위한 과정 선택 추가
     
     // 선택된 과정의 출석 데이터로 통계 업데이트
     const courseStats = {
       totalStudents: course.totalStudents || 0,
       attendedCount: course.presentCount || 0,
       lateCount: course.lateCount || 0,
       absentCount: (course.totalStudents || 0) - (course.presentCount || 0) - (course.lateCount || 0),
       attendanceRate: course.attendanceRate || 0
     }
     console.log('📊 선택된 과정 통계:', courseStats)
     setStats(courseStats)
   }

     const handleCourseDeselect = () => {
     console.log('🔄 전체 과정 선택으로 변경')
     setSelectedCourseForStats(null)
     setSelectedCourseForQR(null) // QR 코드 생성용 선택도 해제
     // 전체 통계로 초기화
     const totalStudents = courses.reduce((sum, course) => sum + (course.totalStudents || 0), 0)
     const totalPresent = courses.reduce((sum, course) => sum + (course.presentCount || 0), 0)
     const totalLate = courses.reduce((sum, course) => sum + (course.lateCount || 0), 0)
     const totalAbsent = totalStudents - totalPresent - totalLate
     
     const overallStats = {
       totalStudents,
       attendedCount: totalPresent,
       lateCount: totalLate,
       absentCount: totalAbsent,
       attendanceRate: totalStudents > 0 ? Math.round((totalPresent / totalStudents) * 100) : 0
     }
     console.log('📊 전체 과정 통계:', overallStats)
     setStats(overallStats)
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
        <Sidebar title="회원 정보" menuItems={sidebarItems} currentPath="/instructor/academic/attendance" />

        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
                         {/* 페이지 헤더 */}
             <div className="mb-6">
               <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                 담당 과정 출석 관리
               </h1>
               <p className="text-gray-600">강사님이 담당하고 계신 과정별 학생들의 출석 현황과 QR 코드를 관리하세요.</p>
             </div>

             {/* 날짜 선택 및 엑셀 다운로드 */}
             <div className="flex justify-end items-center gap-4 mb-6">
               <div className="flex items-center gap-2">
                 <Label htmlFor="date-picker" className="text-sm font-medium text-gray-700">
                   날짜 선택
                 </Label>
                 <Input
                   id="date-picker"
                   type="date"
                   value={selectedDate}
                   onChange={(e) => setSelectedDate(e.target.value)}
                   max={new Date().toISOString().split('T')[0]}
                   className="w-40"
                 />
               </div>
               <Button onClick={handleExportData} 
               className="text-[#1abc9c] hover:bg-[#1abc9c] 
               border border-[#1abc9c] hover:text-white">
                 <Download className="w-4 h-4 mr-2" />
                 엑셀 다운로드
               </Button>
             </div>

             {/* 통계 카드 */}
             <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-m font-medium text-gray-600">
                        {selectedCourseForStats ? '선택된 과정 출석' : '출석'}
                      </p>
                      <p className="text-3xl font-bold" style={{ color: "#2ecc71" }}>
                        {selectedCourseForStats ? `${stats.attendedCount}명` : '-명'}
                      </p>
                    </div>
                    <div className="bg-[#e4f5eb] rounded-full p-3 mr-3">
                      <CheckCircle className="w-10 h-10" style={{ color: "#2ecc71" }} />
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-m font-medium text-gray-600">
                        {selectedCourseForStats ? '선택된 과정 지각' : '지각'}
                      </p>
                      <p className="text-3xl font-bold" style={{ color: "#3498db" }}>
                        {selectedCourseForStats ? `${stats.lateCount}명` : '-명'}
                      </p>
                    </div>
                    <div className="bg-[#EFF6FF] rounded-full p-3 mr-3">
                      <Clock className="w-10 h-10" style={{ color: "#3498db" }} />
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-m font-medium text-gray-600">
                        {selectedCourseForStats ? '선택된 과정 결석' : '결석'}
                      </p>
                      <p className="text-3xl font-bold" style={{ color: "#e74c3c" }}>
                        {selectedCourseForStats ? `${stats.absentCount}명` : '-명'}
                      </p>
                    </div>
                    <div className="bg-red-100 rounded-full p-3 mr-3">
                      <XCircle className="w-10 h-10" style={{ color: "#e74c3c" }} />
                    </div>
                  </div>
                </CardContent>
              </Card>
              </div>

            {/* 담당 과정 목록 */}
             <Card className="mb-6">
               <CardHeader>
                 <CardTitle style={{ color: "#2C3E50" }}>담당 과정 현황</CardTitle>
               </CardHeader>
               <CardContent>
                 <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                   {courses.map((course) => {
                     const isSelected = selectedCourseForStats?.courseId === course.courseId
                     const isQRSelected = selectedCourseForQR?.courseId === course.courseId
                     return (
                       <div 
                         key={course.courseId} 
                         className={`border rounded-lg p-4 transition-all duration-200 cursor-pointer transform hover:scale-105 hover:shadow-lg ${
                           isSelected 
                             ? 'border-blue-500 bg-blue-50 hover:bg-blue-100 hover:shadow-blue-200' 
                             : isQRSelected
                             ? 'border-green-500 bg-green-50 hover:bg-green-100 hover:shadow-green-200'
                             : 'hover:bg-gray-50 hover:border-gray-300'
                         }`}
                         onClick={() => {
                           if (isSelected) {
                             handleCourseDeselect()
                           } else {
                             handleCourseSelect(course)
                           }
                         }}

                       >
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex-1">
                            <h4 className="font-semibold text-gray-900">
                              {course.courseName || '과정 없음'}</h4>
                          </div>
                        </div>
                                                                         <div className="space-y-2 text-sm text-gray-600">
                          <div className="flex items-center gap-2">
                            <Users className="w-4 h-4" />
                            <span>
                              총 학생 : {course.totalStudents || course.maxCapacity || 0}명
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span>
                              출석 : {course.presentCount || 0}명
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span>
                              지각 : {course.lateCount || 0}명
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span>
                              결석 : {course.absentCount || 0}명
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span>
                              강의실명 : {course.classroomCode}
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span>
                             강의호실 : {course.classroomNumber}호
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span>
                              강의요일 : {sortWeekdays(course.courseDays)}
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span>
                              시간 : {course.startTime || '미정'} ~ {course.endTime || '미정'}
                            </span>
                          </div>
                           <div className="w-full bg-gray-200 rounded-full h-2">
                             <div
                               className="bg-[#1abc9c] h-2 rounded-full"
                               style={{ width: `${course.attendanceRate || 0}%` }}
                             ></div>
                           </div>
                           <div className="text-right text-s text-gray-900">
                             출석률 : {course.attendanceRate || 0}%
                           </div>
                           
                           {/* QR 선택 상태 표시 */}
                           {isQRSelected && (
                             <div className="mt-2 p-2 bg-green-100 rounded text-xs text-green-800 text-center">
                               ✓ QR 코드 생성용으로 선택됨
                             </div>
                           )}
                         </div>
                      </div>
                    )
                  })}
                </div>
              </CardContent>
            </Card>

            {/* QR 코드 생성 영역 */}
             <div className="mb-6">
               {/* QR 코드 생성 */}
               <Card>
                 <CardHeader>
                   <CardTitle style={{ color: "#2C3E50" }}>모바일 출석용 QR 코드 생성</CardTitle>
                 </CardHeader>
                 <CardContent>
                   <div className="space-y-4">
                     {/* QR용 과정 선택 안내 */}
                     <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                       <h3 className="text-sm font-medium text-blue-800 mb-2">QR 코드 생성 안내</h3>
                       <p className="text-sm text-blue-700">
                         위의 과정 목록에서 QR 코드를 생성할 과정을 선택하세요. 선택된 과정에 해당하는 강의실 정보가 자동으로 설정됩니다.
                       </p>
                     </div>

                     {/* 선택된 정보 표시 */}
                     {selectedCourseForQR && (
                       <div className="bg-gray-50 rounded-lg p-3 text-sm space-y-1">
                         <div><strong>선택된 과정:</strong> {selectedCourseForQR.courseName}</div>
                         {selectedClassroom && (
                           <>
                             <div><strong>강의실 : </strong> {selectedClassroom.classCode}</div>
                             <div><strong>강의 호실 : </strong> {selectedClassroom.classNumber}호</div>
                             <div><strong>수용 인원 : </strong> {selectedClassroom.classCapacity}명</div>
                           </>
                         )}
                         <div><strong>날짜 : </strong> {currentTime.toLocaleDateString("ko-KR")}</div>
                       </div>
                     )}

                    {/* IP 주소 입력 */}
                    <div className="bg-gray-50 rounded-lg p-3">
                      <h3 className="text-sm font-medium text-gray-900 mb-2">모바일 접속 IP 주소</h3>
                      <div className="space-y-2">
                        <div className="text-xs text-gray-500">
                          <p>• 모바일에서 QR 코드 스캔 시 접속할 컴퓨터의 IP 주소를 입력하세요</p>
                          <p>• IP 주소 확인: 명령 프롬프트에서 "ipconfig" 실행 후 Wi-Fi IPv4 주소 확인</p>
                        </div>
                        <div className="flex space-x-2">
                          <input
                            type="text"
                            value={customIP}
                            onChange={(e) => setCustomIP(e.target.value)}
                            placeholder="예: 192.168.1.100"
                            className="flex-1 px-3 py-2 text-sm border border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                          />
                          <button
                            onClick={() => {
                              if (customIP) {
                                alert(`IP 주소가 설정되었습니다: ${customIP}`)
                              } else {
                                alert('IP 주소를 입력해주세요.')
                              }
                            }}
                            className="px-3 py-2 text-sm text-gray-600 
                            rounded-md hover:bg-gray-100
                            border border-gray-600 transition-colors"
                          >
                            설정
                          </button>
                        </div>
                        {customIP && (
                          <div className="text-xs text-green-600">
                            ✓ 설정된 IP: {customIP}
                          </div>
                        )}
                        {localIP && !customIP && (
                          <div className="text-xs text-orange-600">
                            🔍 감지된 IP: {localIP} (자동 감지)
                          </div>
                        )}
                      </div>
                    </div>

                    {/* QR 코드 */}
                    {qrCode && (
                      <div
                        className="bg-gray-100 rounded-lg p-6 cursor-pointer hover:bg-gray-200 transition-colors"
                        onClick={() => setShowQRModal(true)}
                        ref={qrRef}
                      >
                        <div className="flex justify-center mb-4">
                          <QRCodeSVG
                            value={qrCode}
                            size={128}
                            level="M"
                            includeMargin={true}
                            bgColor="#ffffff"
                            fgColor="#000000"
                          />
                        </div>
                        <div className="mt-4 text-sm font-mono text-gray-600 break-all">{qrCode}</div>
                        <div className="mt-2 text-xs text-gray-500">클릭하여 크게 보기</div>
                        <div className="mt-2 text-xs text-blue-600 font-medium">학생들이 이 QR 코드를 스캔하면 출석 확인 페이지로 이동합니다</div>
                      </div>
                    )}

                                         <Button
                       onClick={() => generateNewQR()}
                       className={`flex items-center justify-center space-x-2 px-4 py-2 rounded-lg transition-colors mx-auto ${
                         selectedCourseForQR
                           ? "text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                           : "bg-gray-300 text-gray-500 cursor-not-allowed"
                       }`}
                       disabled={!selectedCourseForQR}
                     >
                       <span>QR 코드 생성</span>
                     </Button>

                     <div className="text-xs text-gray-500 text-center">
                       {selectedCourseForQR
                         ? `${selectedCourseForQR.courseName} 과정용 QR 코드를 생성합니다`
                         : "위의 과정 목록에서 QR 코드를 생성할 과정을 선택하세요"}
                     </div>
                  </div>
                </CardContent>
              </Card>
            </div>



                         {/* 강사 안내사항 */}
             <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
               <h3 className="text-sm font-medium text-blue-800 mb-2">출석 관리 안내사항</h3>
               <ul className="text-sm text-blue-700 space-y-1">
                 <li>• 담당하고 계신 과정의 출석 현황만 표시됩니다.</li>
                 <li>• 출석 상태는 실시간으로 업데이트됩니다.</li>
                 <li>• 지각/결석 학생의 비고란을 통해 사유를 확인하세요.</li>
                 <li>• 출석 수정이 필요한 경우 수정 버튼을 이용하세요.</li>
                 <li>• 과정 카드를 클릭하면 통계를 확인하고 QR 코드 생성용으로 선택됩니다.</li>
                 <li>• 엑셀 다운로드는 별도 버튼을 이용하세요.</li>
               </ul>
             </div>
          </div>
        </main>
      </div>

      {/* QR 코드 확대 모달 */}
      {showQRModal && qrCode && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-8 max-w-2xl w-full mx-4">
            <div className="text-center">
              <h3 className="text-xl font-semibold mb-4" style={{ color: "#2C3E50" }}>
                출석 링크 QR 코드
              </h3>

              {selectedCourseForQR && (
                <div className="mb-4 p-3 bg-blue-50 rounded-lg">
                  <div className="text-sm space-y-1">
                    <div>
                      <strong>강의실명 : </strong> {selectedCourseForQR.classroomCode}
                    </div>
                    <div>
                      <strong>강의호실 : </strong> {selectedCourseForQR.classroomNumber}호
                    </div>
                    <div>
                      <strong>수용인원 : </strong> {selectedCourseForQR.maxCapacity || '0'}명
                    </div>
                    <div>
                      <strong>날짜 : </strong> {selectedDate}
                    </div>
                  </div>
                </div>
              )}

              <div className="bg-gray-100 rounded-lg p-8 mb-4 flex justify-center">
                <QRCodeSVG
                  value={qrCode}
                  size={500}
                  level="M"
                  includeMargin={true}
                  bgColor="#ffffff"
                  fgColor="#000000"
                />
              </div>

              <div className="flex justify-center space-x-3">
                <Button
                  onClick={() => setShowQRModal(false)}
                  className="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
                >
                  닫기
                </Button>
                <Button
                  onClick={downloadQRCode}
                  className="px-4 py-2 bg-teal-600 text-white rounded-lg hover:bg-teal-700 transition-colors"
                >
                  다운로드
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

    </PageLayout>
  )
}
