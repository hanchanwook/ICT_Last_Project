import { http } from '@/components/auth/http'



// 직원 - 4 - 강의실 장비 생성
export const createClassroomEquipment = async (equipmentData) => {
  try {
    console.log('장비 생성 시작:', equipmentData)
    const response = await http.post('/api/classroom-equipment', equipmentData)
    console.log('장비 생성 결과:', response.data)
    return response.data
  } catch (error) {
    console.error('장비 생성 실패:', error)
    throw error
  }
}

// 직원 - 9 - 강의실 장비 수정
export const updateClassroomEquipment = async (equipmentId, equipmentData) => {
  try {
    console.log('장비 수정 시작 - 장비 ID:', equipmentId, '데이터:', equipmentData)
    const response = await http.put(`/api/classroom-equipment/${equipmentId}`, equipmentData)
    console.log('장비 수정 결과:', response.data)
    return response.data
  } catch (error) {
    console.error('장비 수정 실패:', error)
    throw error
  }
}

// 직원 - 10 - 강의실 장비 삭제 (실제 DB 삭제)
export const deleteClassroomEquipment = async (equipmentId) => {
  try {
    console.log('장비 삭제 시작 - 장비 ID:', equipmentId)
    const response = await http.delete(`/api/classroom-equipment/${equipmentId}`)
    console.log('장비 삭제 결과:', response.data)
    return response.data
  } catch (error) {
    console.error('장비 삭제 실패:', error)
    throw error
  }
}

// 직원 - 7 - 강의실 장비 상태 토글
export const updateEquipmentStatus = async (equipmentId, status) => {
  try {
    console.log('장비 상태 변경 시작 - 장비 ID:', equipmentId, '상태:', status)
    const response = await http.put(`/api/classroom-equipment/${equipmentId}/toggle`, { status })
    console.log('장비 상태 변경 결과:', response.data)
    return response.data
  } catch (error) {
    console.error('장비 상태 변경 실패:', error)
    throw error
  }
}

  // 직원 - 11 - 장비 엑셀 파일 업로드
export const uploadEquipmentExcel = async (file, classId) => {
  try {
    // 파일 유효성 검사
    if (!file) {
      throw new Error('파일이 선택되지 않았습니다.')
    }
    
    if (file.size === 0) {
      throw new Error('빈 파일입니다.')
    }
    
    // 파일 확장자 검증 (백엔드에서 .xlsx만 허용)
    const fileExtension = file.name.toLowerCase().split('.').pop()
    if (fileExtension !== 'xlsx') {
      throw new Error('Excel 파일(.xlsx)만 업로드 가능합니다.')
    }
    
    console.log('장비 엑셀 파일 업로드 시작 - 파일명:', file.name)
    console.log('강의실 ID:', classId)
    console.log('파일 크기:', file.size, 'bytes')
    console.log('파일 타입:', file.type)
    console.log('파일 마지막 수정일:', file.lastModified)
    console.log('파일 객체:', file)
    
    const formData = new FormData()
    
    // 백엔드에서 확인하는 필드명으로만 파일 추가
    formData.append('excelFile', file)  // 백엔드에서 확인하는 필드
    formData.append('file', file)       // 백엔드에서 확인하는 필드
    formData.append('classId', classId) // 강의실 ID를 FormData에 추가
    
    // 백엔드에서 @RequestPart를 사용할 경우를 대비
    formData.append('multipartFile', file)
    
    // 추가 필드명들 (백엔드 호환성)
    formData.append('uploadFile', file)
    formData.append('equipmentFile', file)
    
    // Content-Type 헤더 제거 - 브라우저가 자동으로 boundary를 포함한 Content-Type 설정
    
    // FormData 내용 확인
    console.log('=== FormData 내용 상세 확인 ===')
    console.log('FormData entries 개수:', formData.entries().length)
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
    console.log('FormData 전체 객체:', formData)
    console.log('================================')
    
    // classroomAxios.js와 동일한 방식으로 fetch 사용
    const endpoint = '/api/classroom-equipment/excel-upload'
    const baseURL = http.defaults?.baseURL || 'http://localhost:19091'
    const fullURL = `${baseURL}${endpoint}?classId=${classId}`
    
    console.log('백엔드로 전송할 API 엔드포인트:', endpoint)
    console.log('전체 요청 URL:', fullURL)
    console.log('fetch 사용 - Content-Type 문제 완전 해결')
    
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
    
    console.log('장비 엑셀 파일 업로드 결과:', data)
    
    return data
  } catch (error) {
    console.error('장비 엑셀 파일 업로드 실패:', error)
    console.error('에러 응답 데이터:', error.response?.data)
    console.error('에러 상태 코드:', error.response?.status)
    console.error('에러 메시지:', error.message)
    throw error
  }
}

// 직원 - 12 - 장비 엑셀 템플릿 다운로드
export const downloadEquipmentTemplate = async () => {
  try {
    console.log('장비 템플릿 다운로드 시작')
    const response = await http.get('/api/classroom-equipment/template/download', {
      responseType: 'blob'
    })
    console.log('장비 템플릿 다운로드 완료 - 파일 크기:', response.data.size)
    return response.data
  } catch (error) {
    console.error('장비 템플릿 다운로드 실패:', error)
    throw error
  }
}


