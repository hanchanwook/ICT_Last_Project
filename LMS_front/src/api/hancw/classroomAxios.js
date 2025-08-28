import { http } from '@/components/auth/http'

// classroomApi 인스턴스 제거, http 인스턴스만 사용

// 직원 - 1 - 모든 교실 조회
export const getAllClassrooms = async (educationId) => {
  try {
    const response = await http.get('/api/classroom/all', {
      params: { educationId }
    })
    return response.data
  } catch (error) {
    console.error('교실 목록 조회 실패:', error)
    throw error
  }
}

// 직원 - 2 - 교실 생성 
export const createClassroom = async (classroomData) => {
  try {
    const response = await http.post('/api/classroom/create', classroomData)
    return response.data
  } catch (error) {
    console.error('교실 생성 실패:', error)
    throw error
  }
}

// 직원 - 2-1. EducationId 조회
export const getEducationId = async (memberId) => {
  try {
    console.log('EducationId 조회 시작 - memberId:', memberId)
    console.log('http baseURL:', http.defaults?.baseURL)
    console.log('요청 URL:', `${http.defaults?.baseURL}/api/classroom/education-id?id=${memberId}`)
    
    const response = await http.get('/api/classroom/education-id', {
      params: { id: memberId }  // memberId를 id 파라미터로 전송
    })
    console.log('EducationId 조회 결과:', response.data)
    return response.data
  } catch (error) {
    console.error('EducationId 조회 실패:', error)
    console.error('요청 URL:', `${http.defaults?.baseURL}/api/classroom/education-id?id=${memberId}`)
    console.error('에러 상세:', error.response?.data)
    throw error
  }
}

// 직원 - 3 - 교실 수정
export const updateClassroom = async (classId, classroomData) => {
  try {
    const response = await http.put(`/api/classroom/update/${classId}`, classroomData)
    return response.data
  } catch (error) {
    console.error('교실 수정 실패:', error)
    throw error
  }
}

// 직원 - 6 - 강의실 파일 업로드 (CSV/Excel 통합)
export const uploadCsvFile = async (file, educationId) => {
  try {
    const formData = new FormData()
    
    // 파일 확장자 확인
    const fileExtension = file.name.toLowerCase().split('.').pop()
    
    // 지원하는 파일 형식 검증
    if (!['csv', 'xlsx', 'xls'].includes(fileExtension)) {
      throw new Error('지원하지 않는 파일 형식입니다. CSV 또는 Excel 파일을 업로드해주세요.')
    }
    
    // 통일된 엔드포인트 사용 (백엔드에서 파일 확장자에 따라 자동 처리)
    const endpoint = '/api/classroom/upload'
    const baseURL = http.defaults?.baseURL || 'http://localhost:19091'
    const fullURL = baseURL + endpoint
    
    // FormData 구성
    formData.append('file', file)
    formData.append('educationId', educationId)
    
    // 디버깅을 위한 콘솔 로그 추가
    console.log('=== 대량 강의실 등록 파일 업로드 요청 정보 ===')
    console.log('파일명:', file.name)
    console.log('파일 크기:', file.size, 'bytes')
    console.log('파일 타입:', file.type)
    console.log('파일 확장자:', fileExtension)
    console.log('요청 엔드포인트:', endpoint)
    console.log('전체 요청 URL:', fullURL)
    
    // FormData 내용 확인
    console.log('=== FormData 내용 ===')
    for (let [key, value] of formData.entries()) {
      if (value instanceof File) {
        console.log(`${key}:`, {
          name: value.name,
          size: value.size,
          type: value.type,
          lastModified: new Date(value.lastModified).toLocaleString()
        })
      } else {
        console.log(`${key}:`, value)
      }
    }
    console.log('================================')
    
    // fetch 사용 - Content-Type 문제 완전 해결
    const response = await fetch(fullURL, {
      method: 'POST',
      body: formData,
      credentials: 'include'  // 쿠키 포함
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    return data
  } catch (error) {
    console.error('대량 강의실 등록 실패:', error)
    throw error
  }
} 


// 직원 - 4 -  강의실 상세 정보 + 장비 목록 동시 로드
export const getClassroomWithEquipment = async (classId) => {
  try {
    console.log('강의실 상세 정보 + 장비 로드 시작 - classId:', classId)
    
    const [classroom, equipment] = await Promise.all([
      http.get(`/api/classroom/${classId}`).then(response => response.data),
      http.get(`/api/classroom-equipment/classroom/${classId}`).then(response => response.data).catch(() => [])
    ])
    
    console.log('강의실 데이터:', classroom)
    console.log('장비 데이터:', equipment)
    
    return {
      classroom,
      equipment: equipment || [] // 장비가 없으면 빈 배열 반환
    }
  } catch (error) {
    console.error('강의실 상세 정보 로드 실패:', error)
    // 장비 로드 실패 시에도 강의실 정보는 반환
    try {
      const classroom = await http.get(`/api/classroom/${classId}`).then(response => response.data)
      return {
        classroom,
        equipment: []
      }
    } catch (classroomError) {
      throw error
    }
  }
}

// 직원 - 16 - 검색 + 필터링 동시 적용 (클라이언트 사이드 처리)
export const searchAndFilterClassrooms = async (searchTerm, status) => {
  try {
    const allClassrooms = await getAllClassrooms()
    let filteredResults = allClassrooms
    if (searchTerm) {
      filteredResults = filteredResults.filter(classroom => 
        classroom.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        classroom.id?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        classroom.type?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        classroom.manager?.toLowerCase().includes(searchTerm.toLowerCase())
      )
    }
    if (status && status !== "all") {
      filteredResults = filteredResults.filter(classroom => classroom.status === status)
    }
    return filteredResults
  } catch (error) {
    console.error('검색 및 필터링 실패:', error)
    throw error
  }
}

// 직원 - 5 - 강의실 삭제 + 관련 장비 정리
export const deleteClassroomWithCleanup = async (classId) => {
  try {
    const response = await http.delete(`/api/classroom/delete/${classId}`)
    return response.data
  } catch (error) {
    console.error('강의실 삭제 및 정리 실패:', error)
    throw error
  }
}
