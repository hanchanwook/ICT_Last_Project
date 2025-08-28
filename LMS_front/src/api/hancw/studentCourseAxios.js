import { baseURL } from "@/components/auth/http"

// 백엔드 데이터를 프론트엔드 형식으로 변환하는 함수
const transformBackendData = (backendCourses) => {
  console.log('🔄 백엔드 데이터 변환 시작:', backendCourses)
  
  // 과정별로 그룹화
  const courseMap = new Map()
  
  backendCourses.forEach(course => {
         console.log('🔄 개별 과정 데이터:', course)
     console.log('  - courseName:', course.courseName)
     console.log('  - courseld:', course.courseld)
     console.log('  - courseId:', course.courseId)
     console.log('  - memberId:', course.memberId) // memberId 확인
     console.log('  - classroomName:', course.classroomName)
     console.log('  - classroomld:', course.classroomld)
     console.log('  - classroomId:', course.classroomId)
     console.log('  - 모든 필드:', Object.keys(course))
     console.log('  - 모든 값:', Object.entries(course))
    
    const courseKey = course.courseName
    
         if (!courseMap.has(courseKey)) {
       const mappedCourseId = course.courseld || course.courseId
       console.log('🔄 과정 ID 매핑:', {
         courseld: course.courseld,
         courseId: course.courseId,
         mappedCourseId: mappedCourseId
       })
       
       courseMap.set(courseKey, {
         courseId: mappedCourseId,
         courseName: course.courseName,
         memberId: course.memberId, // memberId 추가
         instructorName: course.instructorName || "미지정",
         schedule: course.schedule || "강의 시간 정보",
         classrooms: []
       })
     }
    
    const existingCourse = courseMap.get(courseKey)
    
    // 강의실 정보 추가 (중복 방지)
    const classroomExists = existingCourse.classrooms.some(c => 
      c.classroomName === course.classroomName
    )
    
         if (!classroomExists) {
       const mappedClassroomId = course.classroomld || course.classroomId
       console.log('🔄 강의실 ID 매핑:', {
         classroomld: course.classroomld,
         classroomId: course.classroomId,
         mappedClassroomId: mappedClassroomId
       })
       
       existingCourse.classrooms.push({
         classroomId: mappedClassroomId,
         classroomName: course.classroomName,
         classroomNumber: course.classroomName.replace(/[^0-9]/g, '') || "1",
         location: course.location || "강의실 위치"
       })
     }
  })
  
  const transformedCourses = Array.from(courseMap.values())
  console.log('🔄 변환 완료:', transformedCourses)
  
  // 변환된 각 과정의 ID 확인
  transformedCourses.forEach((course, index) => {
    console.log(`🔄 변환된 과정 [${index}]:`, {
      courseId: course.courseId,
      courseName: course.courseName,
      memberId: course.memberId, // memberId 확인
      classrooms: course.classrooms.map(c => ({
        classroomId: c.classroomId,
        classroomName: c.classroomName
      }))
    })
  })
  
  return transformedCourses
}

// 학생 본인 출석 기록 조회 (학생용)
export const getStudentAttendanceRecords = async (studentId, filters = {}) => {
  try {
    console.log('학생 본인 출석 기록 조회 요청:', studentId)
    
    const response = await fetch(`${baseURL}/api/attendance/my-attendances`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // 쿠키 포함
    })
    
    if (!response.ok) {
      throw new Error('출석 기록 조회 실패')
    }
    
    const data = await response.json()
    console.log('🔍 출석 기록 API 응답 데이터:')
    console.log('  - 전체 응답:', data)
    console.log('  - 응답 타입:', typeof data)
    console.log('  - attendances 필드:', data.attendances)
    if (data.attendances && Array.isArray(data.attendances)) {
      console.log('  - 출석 기록 개수:', data.attendances.length)
      data.attendances.forEach((record, index) => {
        console.log(`  - [${index}] 출석 기록:`, record)
      })
    }
    return data
  } catch (error) {
    console.error('출석 기록 조회 오류:', error)
    throw error
  }
}

// 출석 상태에 따른 아이콘 반환
export const getAttendanceStatusIcon = (status) => {
  switch (status) {
    case "출석":
      return "check-circle"
    case "지각":
      return "alert-circle"
    case "결석":
      return "x-circle"
    default:
      return "clock"
  }
}

// 출석 상태에 따른 색상 반환
export const getAttendanceStatusColor = (status) => {
  switch (status) {
    case "출석":
      return "text-green-600 bg-green-50"
    case "지각":
      return "text-yellow-600 bg-yellow-50"
    case "결석":
      return "text-red-600 bg-red-50"
    default:
      return "text-gray-600 bg-gray-50"
  }
}

// 학생 수강 과목 및 강의실 정보 조회 (PC 출석용) - 실제 ID 조회
export const getStudentSubjectsAndClassrooms = async (studentId) => {
  try {
    console.log('학생 수강 과목 및 강의실 정보 조회 요청:', studentId)
    
    // 새로운 API: 실제 courseId와 classroomId 조회 (JWT에서 사용자 정보 추출)
    const response = await fetch(`${baseURL}/api/attendance/courses-and-classrooms`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    })
    
    if (!response.ok) {
      throw new Error('수강 과목 및 강의실 정보 조회 실패')
    }
    
    const data = await response.json()
    console.log('🔍 백엔드에서 받은 데이터:')
    console.log('  - 전체 응답:', data)
    console.log('  - 응답 타입:', typeof data)
    console.log('  - courses 필드:', data.courses)
    console.log('  - success:', data.success)
    console.log('  - totalCount:', data.totalCount)
    
    if (data.courses && Array.isArray(data.courses)) {
      console.log('  - courses 배열 길이:', data.courses.length)
      data.courses.forEach((course, index) => {
        console.log(`  - [${index}] 과정 정보:`, course)
        console.log(`    - courseName: ${course.courseName}`)
        console.log(`    - courseld: ${course.courseld}`)
        console.log(`    - classroomName: ${course.classroomName}`)
        console.log(`    - classroomld: ${course.classroomld}`)
      })
    }
    
    // 백엔드 응답 구조를 프론트엔드에서 사용할 수 있는 형태로 변환
    const transformedData = transformBackendData(data.courses || [])
    console.log('🔄 변환된 데이터:', transformedData)
    return transformedData
  } catch (error) {
    console.error('수강 과목 및 강의실 정보 조회 오류:', error)
    
    // API가 없으면 기존 방식으로 fallback
    console.log('새 API 없음, 기존 방식으로 fallback')
    return await getStudentSubjectsAndClassroomsFallback(studentId)
  }
}

// 기존 방식 (fallback용)
const getStudentSubjectsAndClassroomsFallback = async (studentId) => {
  try {
    console.log('기존 방식으로 수강 과정 및 강의실 정보 조회')
    
    // 기존 출석 기록 API를 활용하여 과정과 강의실 정보 추출 (JWT에서 사용자 정보 추출)
    const attendanceData = await getStudentAttendanceRecords()
    
    // 출석 기록에서 고유한 과정과 강의실 정보 추출
    const attendanceArray = attendanceData.attendances || attendanceData || []
    
    // 과정별로 그룹화하여 강의실 정보 수집
    const coursesMap = new Map()
    
    attendanceArray.forEach(record => {
      if (record.courseName && record.classroomName) {
        const courseKey = record.courseName
        
        if (!coursesMap.has(courseKey)) {
          coursesMap.set(courseKey, {
            courseId: record.courseId || `course_${record.courseName}_${Date.now()}`, // 실제 ID가 있으면 사용
            courseName: record.courseName,
            memberId: record.memberId || record.userId, // memberId 추가 (fallback으로 userId 사용)
            instructorName: record.instructorName || "미지정",
            schedule: "강의 시간 정보",
            classrooms: []
          })
        }
        
        const course = coursesMap.get(courseKey)
        const classroomExists = course.classrooms.some(c => c.classroomName === record.classroomName)
        
        if (!classroomExists) {
          course.classrooms.push({
            classroomId: record.classroomId || record.classId || `classroom_${record.classroomName}_${Date.now()}`, // 실제 ID가 있으면 사용
            classroomName: record.classroomName,
            classroomNumber: record.classroomName.replace(/[^0-9]/g, '') || "1",
            location: "강의실 위치"
          })
        }
      }
    })
    
    const courses = Array.from(coursesMap.values())
    console.log('🔍 Fallback 방식으로 추출된 데이터:')
    console.log('  - 전체 데이터:', courses)
    console.log('  - 데이터 타입:', typeof courses)
    console.log('  - 배열 길이:', courses.length)
    courses.forEach((course, index) => {
      console.log(`  - [${index}] 과정 정보:`, course)
      console.log(`    - memberId: ${course.memberId}`) // memberId 확인
      if (course.classrooms && course.classrooms.length > 0) {
        console.log(`    강의실 목록:`, course.classrooms)
      }
    })
    
    return courses
  } catch (error) {
    console.error('기존 방식 수강 과정 및 강의실 정보 조회 오류:', error)
    throw error
  }
}

// PC 출석 제출
export const submitPCAttendance = async (attendanceData) => {
  try {
    console.log('PC 출석 제출 요청:', attendanceData)
    
    // PC 출석 데이터 구조 (memberId 포함)
    const pcAttendanceData = {
      ...attendanceData,
      userId: attendanceData.userInfo?.userId || attendanceData.userId,
      memberId: attendanceData.memberId // memberId 추가
    }
    
    console.log('🔍 백엔드로 전송할 데이터 구조:')
    console.log('  - courseId:', pcAttendanceData.courseId)
    console.log('  - courseName:', pcAttendanceData.courseName)
    console.log('  - classroomId:', pcAttendanceData.classroomId)
    console.log('  - classroomName:', pcAttendanceData.classroomName)
    console.log('  - userId:', pcAttendanceData.userId)
    console.log('  - memberId:', pcAttendanceData.memberId)
    console.log('  - 전체 데이터:', JSON.stringify(pcAttendanceData, null, 2))
    
    const response = await fetch(`${baseURL}/api/attendance/submit-pc`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(pcAttendanceData)
    })
    
    if (!response.ok) {
      throw new Error('PC 출석 제출 실패')
    }
    
    const data = await response.json()
    console.log('PC 출석 제출 성공:', data)
    return data
  } catch (error) {
    console.error('PC 출석 제출 오류:', error)
    throw error
  }
}

// PC 퇴실 제출
export const submitPCCheckOut = async (checkOutData) => {
  try {
    console.log('PC 퇴실 제출 요청:', checkOutData)
    
    // memberId가 포함된 데이터 구조 확인
    console.log('🔍 백엔드로 전송할 퇴실 데이터:')
    console.log('  - courseId:', checkOutData.courseId)
    console.log('  - courseName:', checkOutData.courseName)
    console.log('  - classroomId:', checkOutData.classroomId)
    console.log('  - classroomName:', checkOutData.classroomName)
    console.log('  - userId:', checkOutData.userId)
    console.log('  - memberId:', checkOutData.memberId)
    console.log('  - 전체 데이터:', JSON.stringify(checkOutData, null, 2))
    
    const response = await fetch(`${baseURL}/api/attendance/checkout-pc`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(checkOutData)
    })
    
    if (!response.ok) {
      throw new Error('PC 퇴실 제출 실패')
    }
    
    const data = await response.json()
    console.log('PC 퇴실 제출 성공:', data)
    return data
  } catch (error) {
    console.error('PC 퇴실 제출 오류:', error)
    throw error
  }
}

// 오늘 출석 상태 확인 (PC용)
export const getTodayAttendanceStatus = async (userId, courseId = null, memberId = null) => {
  try {
    console.log('오늘 출석 상태 확인 요청:', { userId, courseId, memberId })
    
    let url = `${baseURL}/api/attendance/status-pc/${userId}`
    const params = new URLSearchParams()
    
    if (courseId) {
      params.append('courseId', courseId)
    }
    if (memberId) {
      params.append('memberId', memberId)
    }
    
    if (params.toString()) {
      url += `?${params.toString()}`
    }
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    })
    
    if (!response.ok) {
      throw new Error('오늘 출석 상태 확인 실패')
    }
    
    const data = await response.json()
    console.log('오늘 출석 상태 확인 성공:', data)
    return data
  } catch (error) {
    console.error('오늘 출석 상태 확인 오류:', error)
    throw error
  }
}

// 특정 과정의 출석 가능 여부 확인
export const checkAttendanceAvailability = async (userId, courseId, classroomId) => {
  try {
    console.log('출석 가능 여부 확인 요청:', { userId, courseId, classroomId })
    
    const response = await fetch(`${baseURL}/api/attendance/check-availability`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        userId,
        courseId,
        classroomId,
        currentTime: new Date().toISOString()
      })
    })
    
    if (!response.ok) {
      throw new Error('출석 가능 여부 확인 실패')
    }
    
    const data = await response.json()
    console.log('출석 가능 여부 확인 성공:', data)
    return data
  } catch (error) {
    console.error('출석 가능 여부 확인 오류:', error)
    throw error
  }
}
