import { useState, useEffect } from "react"
import { useParams, Link } from "react-router-dom"
import Header from "@/components/layout/header"
import Sidebar from "@/components/layout/sidebar"
import { Button } from "@/components/ui/button"
import { 
  getLectureDetail, 
  deleteCourseMaterial,
  getCourseMaterials,
  getLecturePlan,
  downloadFile,
  getClassInfo
} from "@/api/sunghyun/instructorCourseApi"
import { uploadCourseMaterial } from "@/api/sunghyun/instructorMaterialApi"
import {
  ArrowLeft,
  Calendar,
  Clock,
  MapPin,
  BookOpen,
  FileText,
  Download,
  Edit,
  Save,
  X,
  Plus,
  Trash2,
  Users,
  Target,
} from "lucide-react"

import { http } from "@/components/auth/http"
import { getMenuItems } from "@/components/ui/menuConfig"


export default function LectureDetailPage() {
  const { id: lectureId } = useParams()
  const [currentUser, setCurrentUser] = useState(null)

  const [isEditingMaterials, setIsEditingMaterials] = useState(false)
  const [selectedFile, setSelectedFile] = useState(null)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [isUploading, setIsUploading] = useState(false)
  const [isEditingSyllabus, setIsEditingSyllabus] = useState(false)
  
  // 강의 계획서 데이터 (DB 스키마에 맞게 수정)
  const [lectureplanData, setLectureplanData] = useState({
    planTitle: "",
    planContent: "",
    courseGoal: "",
    learningMethod: "",
    evaluationMethod: "",
    textbook: "",
    weekCount: 15,
    assignmentPolicy: "",
    latePolicy: "",
    etcNote: "",
  })
  
  // 주차별 계획 데이터
  const [weeklyplan, setWeeklyplan] = useState([])
  const [hasLectureplan, setHasLectureplan] = useState(false)

  // 사이드바 메뉴 구성
  const sidebarItems = getMenuItems('instructor-courses')

  // 강의 데이터
  const [lecture, setLecture] = useState(null)
  const [materials, setMaterials] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // localStorage에서 사용자 정보 가져오기
  useEffect(() => {
    const userInfo = localStorage.getItem('currentUser')
    if (userInfo) {
      setCurrentUser(JSON.parse(userInfo))
    }
  }, [])

  useEffect(() => {
    console.log('강의 상세 정보 로딩 시작 - lectureId:', lectureId)
    
    if (!lectureId) {
      console.error('강의 ID가 없습니다.')
      setError('유효하지 않은 강의 ID입니다.')
      setLoading(false)
      return
    }

    const fetchData = async () => {
      try {
        setLoading(true)
        setError(null)
        
        console.log('API 호출 시작 - courseId:', lectureId)
        
        // 강의 상세 정보 조회
        let lectureData
        
        try {
          lectureData = await getLectureDetail(lectureId)
          console.log('강의 상세 데이터 응답:', lectureData)
        } catch (lectureError) {
          console.error('강의 상세 정보 조회 실패:', lectureError)
          throw lectureError
        }
        
        // 강의실 정보 가져오기
        let classInfo = null
        if (lectureData.classId) {
          try {
            console.log(`강의실 정보 조회 시작: ${lectureData.classId}`)
            classInfo = await getClassInfo(lectureData.classId)
            console.log(`강의실 ${lectureData.classId} 정보:`, classInfo)
          } catch (classError) {
            console.warn(`강의실 ${lectureData.classId} 정보 조회 실패:`, classError)
            
            // UUID인 경우 기본값 설정
            const isUUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(lectureData.classId)
            classInfo = {
              classId: lectureData.classId,
              classCode: isUUID ? '미정' : `강의실${lectureData.classId}`,
              className: isUUID ? '강의실 미정' : `강의실 ${lectureData.classId}`
            }
          }
        }
        
        // 강의 데이터에 강의실 정보 추가
        const lectureWithClassInfo = {
          ...lectureData,
          classInfo: classInfo
        }
        
        setLecture(lectureWithClassInfo)
        
        // 강의 자료 목록 조회
        try {
          const materialsData = await getCourseMaterials(lectureId)
          console.log('강의 자료 목록:', materialsData)
          setMaterials(materialsData.data || materialsData || [])
        } catch (error) {
          console.warn('강의 자료 목록 조회 실패:', error)
          setMaterials([])
        }
        
      } catch (err) {
        console.error('데이터 로딩 오류:', err)
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [lectureId])

  // 강의 계획서 데이터 가져오기 (courseId → planId → 상세/주차별 계획)
  useEffect(() => {
    const fetchLectureplan = async () => {
      try {
        console.log('강의 계획서 조회 시작 - lectureId:', lectureId)
        
        // 1. courseId로 planId 조회
        const planIdRes = await http.get(`/api/instructor/lectureplan/course/${lectureId}`)
        console.log('planId 조회 응답:', planIdRes.data)
        
        const planId = planIdRes.data.data?.planId
        if (!planId) {
          console.log('planId가 없습니다.')
          setHasLectureplan(false)
          return
        }
        
        console.log('planId:', planId)
        
        // 2. planId로 강의계획서 상세 조회
        const planDetailRes = await http.get(`/api/instructor/lectureplan/plan/${planId}`)
        console.log('강의계획서 상세 조회 응답:', planDetailRes.data)
        
        const lectureplan = planDetailRes.data.data
        setLectureplanData({
          planTitle: lectureplan.planTitle || "",
          planContent: lectureplan.planContent || "",
          courseGoal: lectureplan.courseGoal || "",
          learningMethod: lectureplan.learningMethod || "",
          evaluationMethod: lectureplan.evaluationMethod || "",
          textbook: lectureplan.textbook || "",
          weekCount: lectureplan.weekCount || 15,
          assignmentPolicy: lectureplan.assignmentPolicy || "",
          latePolicy: lectureplan.latePolicy || "",
          etcNote: lectureplan.etcNote || "",
        })
        
        // 3. planId로 주차별 계획 조회
        try {
          const weeklyplanRes = await http.get(`/api/instructor/lectureplan/${planId}/weeklyplan`)
          console.log('주차별 계획 조회 응답:', weeklyplanRes.data)
          
          const weeklyplanData = weeklyplanRes.data.data || weeklyplanRes.data || []
          console.log('설정할 주차별 계획:', weeklyplanData)
          
          setWeeklyplan(weeklyplanData)
        } catch (weeklyplanError) {
          console.warn('주차별 계획 조회 실패:', weeklyplanError)
          setWeeklyplan([])
        }
        
        setHasLectureplan(true)
        console.log('강의 계획서 조회 완료')
      } catch (error) {
        // 404 에러는 강의 계획서가 없는 것으로 처리
        if (error.response?.status === 404) {
          console.log("강의 계획서가 없습니다.")
          setHasLectureplan(false)
        } else {
          // 다른 에러는 콘솔에 로그 출력
          console.error("강의 계획서 조회 중 오류 발생:", error)
          setHasLectureplan(false)
        }
      }
    }
    if (lectureId) {
      fetchLectureplan()
    }
  }, [lectureId])

  const handleFileSelect = (event) => {
    const file = event.target.files[0]
    if (file) {
      console.log('선택된 파일 정보:', {
        name: file.name,
        size: file.size,
        type: file.type,
        lastModified: file.lastModified
      })
      
      // 파일이 비어있는지 확인
      if (file.size === 0) {
        alert('빈 파일은 업로드할 수 없습니다.')
        return
      }
      
      // 파일 크기 제한 (10MB)
      if (file.size > 10 * 1024 * 1024) {
        alert('파일 크기는 10MB를 초과할 수 없습니다.')
        return
      }
      
      // 허용된 파일 타입 체크
      const allowedTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'text/plain',
        'image/jpeg',
        'image/png',
        'image/gif',
        'application/zip',
        'application/x-rar-compressed'
      ]
      
      if (!allowedTypes.includes(file.type)) {
        alert('지원하지 않는 파일 형식입니다. PDF, Word, Excel, PowerPoint, 이미지, 압축파일만 업로드 가능합니다.')
        return
      }
      
      // 파일명에 특수문자나 공백이 있는지 확인
      const fileName = file.name
      if (/[<>:"/\\|?*]/.test(fileName)) {
        alert('파일명에 특수문자 (< > : " / \\ | ? *)가 포함되어 있습니다. 파일명을 변경해주세요.')
        return
      }
      
      setSelectedFile(file)
    }
  }

  const handleUploadMaterial = async () => {
    if (!selectedFile) {
      alert('파일을 선택해주세요.')
      return
    }

    setIsUploading(true)
    setUploadProgress(0)

    try {
      // 업로드 전 파일 정보 확인
      console.log('업로드할 파일 정보:', {
        name: selectedFile.name,
        size: selectedFile.size,
        type: selectedFile.type,
        lastModified: selectedFile.lastModified
      })
      
      // 업로드 진행률 시뮬레이션
      const progressInterval = setInterval(() => {
        setUploadProgress(prev => {
          if (prev >= 90) {
            clearInterval(progressInterval)
            return 90
          }
          return prev + 10
        })
      }, 100)

      // FormData 생성
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('title', selectedFile.name);
      
      // Presigned URL 방식으로 파일 업로드
      const response = await uploadCourseMaterial(lectureId, formData)

      clearInterval(progressInterval)
      setUploadProgress(100)

      // 서버 응답 확인
      console.log('파일 업로드 응답:', response)
      
      // 응답 데이터 검증
      if (!response) {
        throw new Error('서버에서 올바른 응답을 받지 못했습니다.')
      }

      // 업로드된 파일 정보를 materials에 추가
      const newMaterialObj = {
        id: response.data?.id || response.id || Date.now(),
        name: selectedFile.name,
        type: selectedFile.type.split('/')[1].toUpperCase(),
        size: formatFileSize(selectedFile.size),
        uploadDate: new Date().toLocaleDateString(),
        file: selectedFile,
        url: response.data?.url || response.url || '' // 서버에서 반환한 파일 URL
      }

      // 성공 메시지
      alert('파일이 성공적으로 업로드되었습니다.')
      
      // 강의 자료 목록 새로고침
      try {
        const materialsData = await getCourseMaterials(lectureId)
        console.log('업로드 후 강의 자료 목록:', materialsData)
        setMaterials(materialsData.data || materialsData || [])
      } catch (error) {
        console.warn('강의 자료 목록 새로고침 실패:', error)
        // 로컬에 추가
        setMaterials([...materials, newMaterialObj])
      }
      
      setSelectedFile(null)
      setIsEditingMaterials(false)
      setTimeout(() => {
        setUploadProgress(0)
        setIsUploading(false)
      }, 500)

    } catch (error) {
      console.error('파일 업로드 오류:', error)
      
      // 더 구체적인 에러 메시지 제공
      let errorMessage = '파일 업로드에 실패했습니다.'
      if (error.response?.status === 400) {
        errorMessage = '잘못된 요청입니다. 파일 형식이나 크기를 확인해주세요.'
      } else if (error.response?.status === 413) {
        errorMessage = '파일이 너무 큽니다. 파일 크기를 줄여주세요.'
      } else if (error.response?.status === 401) {
        errorMessage = '인증이 만료되었습니다. 다시 로그인해주세요.'
      } else if (error.response?.status === 403) {
        errorMessage = '파일 업로드 권한이 없습니다.'
      }
      
      alert(errorMessage)
      setIsUploading(false)
      setUploadProgress(0)
    }
  }

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const handleDragOver = (e) => {
    e.preventDefault()
    e.currentTarget.classList.add('border-blue-400', 'bg-blue-50')
  }

  const handleDragLeave = (e) => {
    e.preventDefault()
    e.currentTarget.classList.remove('border-blue-400', 'bg-blue-50')
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.currentTarget.classList.remove('border-blue-400', 'bg-blue-50')
    
    const files = e.dataTransfer.files
    if (files.length > 0) {
      handleFileSelect({ target: { files: [files[0]] } })
    }
  }

  const handleDeleteMaterial = async (materialId, material) => {
    // 디버깅: materialId 확인
    console.log('=== 삭제 요청 디버깅 ===');
    console.log('전달받은 materialId:', materialId);
    console.log('전체 material 객체:', material);
    console.log('materialId 타입:', typeof materialId);
    console.log('materialId가 undefined인가?', materialId === undefined);
    console.log('materialId가 null인가?', materialId === null);
    console.log('materialId가 빈 문자열인가?', materialId === '');
    
    if (!materialId) {
      console.error('❌ materialId가 유효하지 않습니다!');
      alert('파일 ID가 유효하지 않습니다. 페이지를 새로고침하고 다시 시도해주세요.');
      return;
    }
    
    if (window.confirm('정말로 이 파일을 삭제하시겠습니까?')) {
      try {
        console.log('🗑️ 파일 삭제 시작:', materialId);
        await deleteCourseMaterial(materialId)
        
        // 강의 자료 목록 새로고침
        try {
          const materialsData = await getCourseMaterials(lectureId)
          console.log('삭제 후 강의 자료 목록:', materialsData)
          setMaterials(materialsData.data || materialsData || [])
        } catch (error) {
          console.warn('강의 자료 목록 새로고침 실패:', error)
          // 로컬에서 제거 - materialId와 fileKey 모두 확인
          setMaterials(materials.filter((m) => {
            const shouldKeep = m.materialId !== materialId && 
                             m.fileKey !== materialId && 
                             m.id !== material?.id;
            console.log(`필터링: ${m.fileName} - materialId: ${m.materialId}, fileKey: ${m.fileKey}, id: ${m.id}, 유지: ${shouldKeep}`);
            return shouldKeep;
          }))
        }
        
        alert('파일이 성공적으로 삭제되었습니다.')
      } catch (error) {
        console.error('파일 삭제 오류:', error)
        alert('파일 삭제에 실패했습니다.')
      }
    }
  }

  const getAttendanceColor = (attendance) => {
    switch (attendance) {
      case "출석":
        return "text-green-600 bg-green-100"
      case "지각":
        return "text-yellow-600 bg-yellow-100"
      case "결석":
        return "text-red-600 bg-red-100"
      default:
        return "text-gray-600 bg-gray-100"
    }
  }

  const getParticipationColor = (participation) => {
    switch (participation) {
      case "적극적":
        return "text-blue-600 bg-blue-100"
      case "보통":
        return "text-gray-600 bg-gray-100"
      case "소극적":
        return "text-orange-600 bg-orange-100"
      default:
        return "text-gray-600 bg-gray-100"
    }
  }

  const getFileIcon = (fileName) => {
    if (!fileName) return '📄'; // 기본 파일 아이콘
    
    const extension = fileName.toLowerCase().split('.').pop();
    
    switch (extension) {
      case 'pdf':
        return '📄';
      case 'doc':
      case 'docx':
        return '📝';
      case 'ppt':
      case 'pptx':
        return '📊';
      case 'xls':
      case 'xlsx':
        return '📈';
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
        return '🖼️';
      case 'mp4':
      case 'avi':
      case 'mov':
        return '🎥';
      case 'mp3':
      case 'wav':
        return '🎵';
      case 'zip':
      case 'rar':
        return '📦';
      case 'txt':
        return '📄';
      default:
        return '📄';
    }
  }

  // 썸네일 이미지 URL 생성 함수
  const getThumbnailUrl = (material) => {
    if (!material) return null;
    
    // 먼저 material.thumbnailUrl이 있는지 확인
    if (material.thumbnailUrl && material.thumbnailUrl !== "") {
      return material.thumbnailUrl;
    }
    
    // thumbnailUrl이 없으면 fileKey로 생성
    const fileName = material.fileName || material.name;
    if (!fileName) return null;
    
    const extension = fileName.toLowerCase().split('.').pop();
    const isImage = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(extension);
    
    if (isImage && material.materialId) {
      // 이미지 파일인 경우 썸네일 URL 생성
      // 백엔드에서 새로운 API가 구현될 때까지 기존 API 사용
      // TODO: 백엔드에서 /api/instructor/file/download/filekey/{fileKey} 구현 후 변경
      return `http://localhost:19091/api/file/download/${material.materialId}`;
    }
    
    return null;
  }

  // 파일 타입 확인 함수
  const isImageFile = (fileName) => {
    if (!fileName) return false;
    const extension = fileName.toLowerCase().split('.').pop();
    return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(extension);
  }

  const handleSaveSyllabus = () => {
    setLecture((prev) => ({
      ...prev,
      syllabus: lectureplanData,
    }))
    setIsEditingSyllabus(false)
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header currentPage="courses" userRole="instructor" userName={currentUser?.name || "강사"} />
        <div className="flex">
          <Sidebar title="과정 관리" menuItems={sidebarItems} currentPath="/instructor/courses/lectures" />
          <main className="flex-1 p-6">
            <div className="max-w-7xl mx-auto">
              <div className="flex items-center justify-center h-64">
                <div className="text-lg text-gray-600">강의 정보를 불러오는 중...</div>
              </div>
            </div>
          </main>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header currentPage="courses" userRole="instructor" userName={currentUser?.name || "강사"} />
        <div className="flex">
          <Sidebar title="과정 관리" menuItems={sidebarItems} currentPath="/instructor/courses/lectures" />
          <main className="flex-1 p-6">
            <div className="max-w-7xl mx-auto">
              <div className="flex flex-col items-center justify-center h-64 space-y-4">
                <div className="text-lg text-red-600">오류: {error}</div>
                <div className="flex space-x-4">
                  <Button 
                    onClick={() => window.location.reload()}
                    className="bg-blue-600 hover:bg-blue-700"
                  >
                    다시 시도
                  </Button>
                  <Link to="/instructor/courses/lectures">
                    <Button variant="outline" className="bg-transparent">
                      강의 목록으로 돌아가기
                    </Button>
                  </Link>
                </div>
                {(error.includes('토큰') || error.includes('인증') || error.includes('로그인')) && (
                  <Button 
                    onClick={() => {
                      localStorage.removeItem('currentUser')
                      window.location.href = '/'
                    }}
                    className="bg-red-600 hover:bg-red-700"
                  >
                    로그인 페이지로 이동
                  </Button>
                )}
              </div>
            </div>
          </main>
        </div>
      </div>
    )
  }

  if (!lecture) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header currentPage="courses" userRole="instructor" userName={currentUser?.name || "강사"} />
        <div className="flex">
          <Sidebar title="과정 관리" menuItems={sidebarItems} currentPath="/instructor/courses/lectures" />
          <main className="flex-1 p-6">
            <div className="max-w-7xl mx-auto">
              <div className="flex items-center justify-center h-64">
                <div className="text-lg text-gray-600">강의 정보를 찾을 수 없습니다.</div>
              </div>
            </div>
          </main>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header currentPage="courses" userRole="instructor" userName={currentUser?.name || "강사"} />

      <div className="flex">
        <Sidebar title="과정 관리" menuItems={sidebarItems} currentPath="/instructor/courses/lectures" />

        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto">
            {/* 뒤로가기 버튼 */}
            <div className="mb-6">
              <Link to="/instructor/courses/lectures">
                <Button variant="outline" className="mb-4 bg-transparent">
                  <ArrowLeft className="w-4 h-4 mr-2" />
                  강의 목록으로 돌아가기
                </Button>
              </Link>

              <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                강의 상세 정보
              </h1>
              <p className="text-gray-600">강의의 상세 정보와 출석 현황을 확인하세요.</p>
            </div>

            {/* 강의 기본 정보 */}
            <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div>
                  <h2 className="text-xl font-semibold mb-4" style={{ color: "#2C3E50" }}>
                    {lecture.courseName}
                  </h2>
                  <div className="space-y-3">
                    <div className="flex items-center">
                      <BookOpen className="w-5 h-5 text-gray-400 mr-3" />
                      <span className="text-gray-600">과정 코드:</span>
                      <span className="ml-2 font-medium">{lecture.courseCode}</span>
                    </div>
                    <div className="flex items-center">
                      <Calendar className="w-5 h-5 text-gray-400 mr-3" />
                      <span className="text-gray-600">시작일:</span>
                      <span className="ml-2 font-medium">
                        {lecture.courseStartDay ? new Date(lecture.courseStartDay).toLocaleDateString() : '미정'}
                      </span>
                    </div>
                    <div className="flex items-center">
                      <Calendar className="w-5 h-5 text-gray-400 mr-3" />
                      <span className="text-gray-600">종료일:</span>
                      <span className="ml-2 font-medium">
                        {lecture.courseEndDay ? new Date(lecture.courseEndDay).toLocaleDateString() : '미정'}
                      </span>
                    </div>
                    <div className="flex items-center">
                      <Clock className="w-5 h-5 text-gray-400 mr-3" />
                      <span className="text-gray-600">강의 시간:</span>
                      <span className="ml-2 font-medium">
                        {lecture.startTime && lecture.endTime ? `${lecture.startTime} - ${lecture.endTime}` : '미정'}
                      </span>
                    </div>
                    <div className="flex items-center">
                      <MapPin className="w-5 h-5 text-gray-400 mr-3" />
                      <span className="text-gray-600">강의실:</span>
                      <span className="ml-2 font-medium">
                        {lecture.classInfo ? lecture.classInfo.classCode : (lecture.classId || '미정')}
                      </span>
                    </div>
                    <div className="flex items-center">
                      <Users className="w-5 h-5 text-gray-400 mr-3" />
                      <span className="text-gray-600">수용 인원:</span>
                      <span className="ml-2 font-medium">
                        {lecture.minCapacity ? `${lecture.minCapacity}명 - ` : ''}{lecture.maxCapacity}명
                      </span>
                    </div>
                    <div className="flex items-center">
                      <Calendar className="w-5 h-5 text-gray-400 mr-3" />
                      <span className="text-gray-600">강의 요일:</span>
                      <span className="ml-2 font-medium">{lecture.courseDays || '미정'}</span>
                    </div>
                  </div>
                </div>

                <div>
                  <h3 className="text-lg font-semibold mb-4" style={{ color: "#2C3E50" }}>
                    내 강의 상태
                  </h3>
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-gray-600">최소 수용 인원:</span>
                      <span className="font-semibold">{lecture.minCapacity}명</span>
                    </div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-gray-600">최대 수용 인원:</span>
                      <span className="font-semibold">{lecture.maxCapacity}명</span>
                    </div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-gray-600">수강률:</span>
                      <span className="font-semibold text-blue-600">
                        {(() => {
                          const now = new Date()
                          const startDate = lecture.courseStartDay ? new Date(lecture.courseStartDay) : null
                          const endDate = lecture.courseEndDay ? new Date(lecture.courseEndDay) : null
                          
                          if (!startDate || !endDate) return 0
                          
                          const totalDuration = endDate.getTime() - startDate.getTime()
                          const elapsed = now.getTime() - startDate.getTime()
                          
                          if (elapsed <= 0) return 0 // 아직 시작하지 않음
                          if (elapsed >= totalDuration) return 100 // 이미 종료됨
                          
                          return Math.round((elapsed / totalDuration) * 100)
                        })()}%
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2 mt-3">
                      <div
                        className="bg-blue-600 h-2 rounded-full"
                        style={{ 
                          width: `${(() => {
                            const now = new Date()
                            const startDate = lecture.courseStartDay ? new Date(lecture.courseStartDay) : null
                            const endDate = lecture.courseEndDay ? new Date(lecture.courseEndDay) : null
                            
                            if (!startDate || !endDate) return 0
                            
                            const totalDuration = endDate.getTime() - startDate.getTime()
                            const elapsed = now.getTime() - startDate.getTime()
                            
                            if (elapsed <= 0) return 0 // 아직 시작하지 않음
                            if (elapsed >= totalDuration) return 100 // 이미 종료됨
                            
                            return (elapsed / totalDuration) * 100
                          })()}%` 
                        }}
                      ></div>
                    </div>
                    <div className="mt-3 text-sm text-gray-500">
                      <div className="flex items-center justify-between">
                        <span>강의 상태:</span>
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                          (() => {
                            const now = new Date()
                            const startDate = lecture.courseStartDay ? new Date(lecture.courseStartDay) : null
                            const endDate = lecture.courseEndDay ? new Date(lecture.courseEndDay) : null
                            
                            if (!startDate) return 'bg-gray-100 text-gray-800' // 시작일이 없으면 비활성
                            
                            if (now < startDate) {
                              return 'bg-blue-100 text-blue-800' // 예정
                            } else if (now >= startDate && now <= endDate) {
                              return 'bg-green-100 text-green-800' // 진행중
                            } else {
                              return 'bg-gray-100 text-gray-800' // 종료됨
                            }
                          })()
                        }`}>
                          {(() => {
                            const now = new Date()
                            const startDate = lecture.courseStartDay ? new Date(lecture.courseStartDay) : null
                            const endDate = lecture.courseEndDay ? new Date(lecture.courseEndDay) : null
                            
                            if (!startDate) return '비활성'
                            
                            if (now < startDate) {
                              return '예정'
                            } else if (now >= startDate && now <= endDate) {
                              return '진행중'
                            } else {
                              return '종료됨'
                            }
                          })()}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* 강의 자료 */}
            <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-semibold" style={{ color: "#2C3E50" }}>
                  강의 자료
                </h3>
                <Button onClick={() => setIsEditingMaterials(true)} size="sm" 
                className="hover:bg-[#1abc9c] text-[#1abc9c] 
                border border-[#1abc9c] hover:text-white">
                  <Plus className="w-4 h-4 mr-2" />
                  자료 추가
                </Button>
              </div>

              {isEditingMaterials && (
                <div className="mb-4 p-4 bg-gray-50 rounded-lg">
                  <div className="space-y-4">
                    {/* 파일 선택 영역 */}
                    <div 
                      className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-blue-400 transition-colors"
                      onDragOver={handleDragOver}
                      onDragLeave={handleDragLeave}
                      onDrop={handleDrop}
                    >
                      <input
                        type="file"
                        onChange={handleFileSelect}
                        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.jpg,.jpeg,.png,.gif,.zip,.rar"
                        className="hidden"
                        id="file-upload"
                        disabled={isUploading}
                      />
                      <label htmlFor="file-upload" className="cursor-pointer">
                        <div className="flex flex-col items-center">
                          <FileText className="w-12 h-12 text-gray-400 mb-2" />
                          <p className="text-lg font-medium text-gray-700 mb-1">
                            {selectedFile ? selectedFile.name : '파일을 선택하거나 여기에 드래그하세요'}
                          </p>
                          <p className="text-sm text-gray-500">
                            PDF, Word, Excel, PowerPoint, 이미지, 압축파일 (최대 10MB)
                          </p>
                        </div>
                      </label>
                    </div>

                    {/* 선택된 파일 정보 */}
                    {selectedFile && (
                      <div className="bg-white p-3 rounded-lg border">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center">
                            <FileText className="w-5 h-5 mr-2" />
                            <div>
                              <p className="font-medium text-gray-900">{selectedFile.name}</p>
                              <p className="text-sm text-gray-500">
                                {formatFileSize(selectedFile.size)} • {selectedFile.type}
                              </p>
                            </div>
                          </div>
                          <Button
                            onClick={() => setSelectedFile(null)}
                            variant="outline"
                            size="sm"
                            className="text-red-600 hover:text-red-700"
                          >
                            <X className="w-4 h-4" />
                          </Button>
                        </div>
                      </div>
                    )}

                    {/* 업로드 진행률 */}
                    {isUploading && (
                      <div className="bg-white p-3 rounded-lg border">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-sm font-medium text-gray-700">업로드 중...</span>
                          <span className="text-sm text-gray-500">{uploadProgress}%</span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                            style={{ width: `${uploadProgress}%` }}
                          ></div>
                        </div>
                      </div>
                    )}

                    {/* 버튼 영역 */}
                    <div className="flex gap-2">
                      <Button 
                        onClick={handleUploadMaterial} 
                        size="sm"
                        disabled={!selectedFile || isUploading}
                        className="text-[#1abc9c] border border-[#1abc9c]
                        hover:bg-[#1abc9c] hover:text-white"
                      >
                        {isUploading ? (
                          <>
                            <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
                            업로드 중...
                          </>
                        ) : (
                          <>
                            <FileText className="w-4 h-4 mr-2" />
                            업로드
                          </>
                        )}
                      </Button>
                      <Button 
                        onClick={() => setIsEditingMaterials(false)} 
                        variant="outline" 
                        size="sm"
                        className="hover:bg-gray-200"
                      >
                        취소
                      </Button>
                    </div>
                  </div>
                </div>
              )}

              {/* 업로드된 자료 목록 */}
              <div className="space-y-3">
                {materials.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    <FileText className="w-12 h-12 mx-auto mb-2 text-gray-300" />
                    <p>업로드된 자료가 없습니다.</p>
                  </div>
                ) : (
                  materials.map((material, index) => (
                    <div key={material.id || material.fileId || `material-${index}`} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center">
                        <div className="p-2 bg-blue-100 rounded-lg mr-3 w-12 h-12 flex items-center justify-center">
                          {isImageFile(material?.fileName || material?.name) && getThumbnailUrl(material) ? (
                            <img 
                              src={getThumbnailUrl(material)} 
                              alt={material?.fileName || material?.name || '이미지'}
                              className="w-8 h-8 object-cover rounded"
                              onError={(e) => {
                                // 이미지 로드 실패 시 기본 아이콘 표시
                                e.target.style.display = 'none';
                                e.target.nextSibling.style.display = 'block';
                              }}
                            />
                          ) : null}
                          <span className={isImageFile(material?.fileName || material?.name) && getThumbnailUrl(material) ? 'hidden' : 'block'}>
                            {getFileIcon(material?.fileName || material?.name || '')}
                          </span>
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">
                            {material?.title || material?.name || 'Unknown File'}
                          </p>
                          <p className="text-sm text-gray-500">
                            {material?.size || material?.fileSize || '0 Bytes'} • {material?.uploadDate || '날짜 없음'}
                          </p>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          className="p-1 text-[#1abc9c] border border-[#1abc9c]
                          hover:bg-[#1abc9c] hover:text-white"
                          onClick={async () => {
                            console.log('=== 파일 다운로드 디버깅 ===');
                            console.log('material 객체:', material);
                            console.log('fileKey:', material?.fileKey);
                            console.log('fileName:', material?.fileName || material?.name);
                            
                            // 가능한 모든 다운로드 키 확인
                            const possibleKeys = [
                              material?.fileKey,
                              material?.materialId,
                              material?.fileId,
                              material?.id
                            ].filter(key => key != null && key !== '');
                            
                            console.log('가능한 다운로드 키들:', possibleKeys);
                            
                            // materialId를 우선적으로 사용하고, 없으면 다른 키들 시도
                            const downloadKey = material?.materialId || material?.fileKey || material?.fileId || material?.id;
                            
                            if (downloadKey) {
                              try {
                                await downloadFile(downloadKey, material?.fileName || material?.name);
                                console.log('다운로드 성공');
                              } catch (error) {
                                console.error('다운로드 실패:', error);
                                alert(error.message || '파일 다운로드에 실패했습니다.');
                              }
                            } else {
                              console.error('다운로드 키가 없습니다!');
                              alert('파일 다운로드 기능은 실제 업로드된 파일에서만 사용 가능합니다.');
                            }
                          }}
                        >
                          <Download className="w-4 h-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          className="p-1 text-red-600 hover:text-red-700 bg-transparent"
                          onClick={() => {
                            // 디버깅: material 객체 확인
                            console.log('=== Material 객체 디버깅 ===');
                            console.log('전체 material 객체:', material);
                            console.log('material.id:', material?.id);
                            console.log('material.fileId:', material?.fileId);
                            console.log('material.materialId:', material?.materialId);
                            console.log('material.file_id:', material?.file_id);
                            console.log('material.material_id:', material?.material_id);
                            
                            // 가능한 모든 ID 필드 확인
                            const possibleIds = [
                              material?.id,
                              material?.fileId,
                              material?.materialId,
                              material?.file_id,
                              material?.material_id
                            ].filter(id => id != null && id !== '');
                            
                            console.log('가능한 ID들:', possibleIds);
                            
                            // LMS API는 숫자 ID를 사용하므로 material.id 사용
                            const materialId = material?.id;
                            
                            if (!materialId) {
                              console.error('❌ material 객체에서 유효한 ID를 찾을 수 없습니다!');
                              alert('파일 정보를 찾을 수 없습니다. 페이지를 새로고침하고 다시 시도해주세요.');
                              return;
                            }
                            
                            console.log('✅ 사용할 materialId:', materialId);
                            handleDeleteMaterial(materialId, material);
                          }}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* 과제 정보 */}
            {lecture.homework && (
              <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
                <h3 className="text-lg font-semibold mb-4" style={{ color: "#2C3E50" }}>
                  과제 정보
                </h3>
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                  <div className="flex items-start">
                    <FileText className="w-5 h-5 text-yellow-600 mr-3 mt-0.5" />
                    <div>
                      <h4 className="font-medium text-yellow-800 mb-1">{lecture.homework}</h4>
                      <p className="text-yellow-700 text-sm">마감일: {lecture.homeworkDueDate}</p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 강의 계획서 */}
            <div className="bg-white rounded-lg shadow-sm border overflow-hidden">
              <div className="p-6 border-b border-gray-200">
                <div className="flex justify-between items-center">
                  <div className="flex items-center">
                    <div className="p-2 rounded-lg mr-3">
                      <FileText className="w-6 h-6" />
                    </div>
                    <h3 className="text-xl font-semibold">
                      강의 계획서
                    </h3>
                  </div>
                  {!hasLectureplan && (
                    <Link to={`/instructor/courses/lectures/${lectureId}/lectureplan/create`}>
                      <Button>
                        <Plus className="w-4 h-4 mr-2" />
                        강의계획서 작성하기
                      </Button>
                    </Link>
                  )}
                  {hasLectureplan && (
                    <Link to={`/instructor/courses/lectures/${lectureId}/lectureplan/create`}>
                      <Button variant="outline"
                        className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white">
                        <Edit className="w-4 h-4 mr-2" />
                        수정
                      </Button>
                    </Link>
                  )}
                </div>
              </div>

              <div className="p-6">
                {hasLectureplan ? (
                  <div className="space-y-8">
                    {/* 기본 정보 카드 */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-3">
                          <h4 className="font-semibold">강의계획서 제목</h4>
                        </div>
                        <p className="font-medium">{lectureplanData.planTitle}</p>
                      </div>
                      
                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-3">
                          <h4 className="font-semibold">총 주차 수</h4>
                        </div>
                        <p className="font-medium text-lg">{lectureplanData.weekCount}주</p>
                      </div>
                    </div>

                    {/* 주요 내용 카드들 */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-3">
                          <h4 className="font-semibold">강의계획서 내용</h4>
                        </div>
                        <p className="whitespace-pre-wrap">{lectureplanData.planContent || '내용이 없습니다.'}</p>
                      </div>

                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-3">
                          <h4 className="font-semibold">전체 수업 목표</h4>
                        </div>
                        <p className="whitespace-pre-wrap">{lectureplanData.courseGoal || '목표가 설정되지 않았습니다.'}</p>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-3">
                          <h4 className="font-semibold">학습 방법</h4>
                        </div>
                        <p className="whitespace-pre-wrap">{lectureplanData.learningMethod}</p>
                      </div>

                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-3">
                          <h4 className="font-semibold">평가 방법</h4>
                        </div>
                        <p className="whitespace-pre-wrap">{lectureplanData.evaluationMethod}</p>
                      </div>
                    </div>

                    <div className="rounded-xl p-6 border bg-white">
                      <div className="flex items-center mb-3">
                        <h4 className="font-semibold">교재 정보</h4>
                      </div>
                      <p className="whitespace-pre-wrap">{lectureplanData.textbook}</p>
                    </div>

                    {/* 정책 정보 카드들 */}
                    {lectureplanData.assignmentPolicy && (
                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-3">
                          <h4 className="font-semibold">과제 정책</h4>
                        </div>
                        <p className="whitespace-pre-wrap">{lectureplanData.assignmentPolicy}</p>
                      </div>
                    )}

                    {lectureplanData.latePolicy && (
                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-3">
                          <h4 className="font-semibold">지각 처리 정책</h4>
                        </div>
                        <p className="whitespace-pre-wrap">{lectureplanData.latePolicy}</p>
                      </div>
                    )}

                    {lectureplanData.etcNote && (
                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-3">
                          <h4 className="font-semibold">기타 참고사항</h4>
                        </div>
                        <p className="whitespace-pre-wrap">{lectureplanData.etcNote}</p>
                      </div>
                    )}

                    {/* 주차별 계획 */}
                    {(() => {
                      console.log('주차별 계획 렌더링 조건 확인:', {
                        weeklyplan,
                        weeklyplanLength: weeklyplan?.length,
                        hasLectureplan
                      });
                      return weeklyplan && weeklyplan.length > 0;
                    })() && (
                      <div className="rounded-xl p-6 border bg-white">
                        <div className="flex items-center mb-6">
                          <div className="p-2 rounded-lg mr-3 border bg-white">
                            <Calendar className="w-5 h-5" />
                          </div>
                          <h4 className="font-semibold text-lg">주차별 계획</h4>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                          {weeklyplan.map((week, index) => (
                            <div key={week.weekNumber || week.id || `week-${index}`} className="bg-white rounded-lg p-4 shadow-sm border">
                              <div className="flex items-center mb-3">
                                <div className="w-8 h-8 bg-[#b0c4de] text-gray-700 rounded-full flex items-center justify-center text-sm font-semibold mr-3">
                                  {week.weekNumber}
                                </div>
                                <h5 className="font-medium">주차</h5>
                              </div>
                              {week.weekTitle && (
                                <p className="font-medium mb-2">{week.weekTitle}</p>
                              )}
                              {week.weekContent && (
                                <p className="text-sm whitespace-pre-wrap">{week.weekContent}</p>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="text-center py-12">
                    <div className="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-4 border bg-white">
                      <FileText className="w-10 h-10" />
                    </div>
                    <h4 className="text-xl font-medium mb-2">강의계획서가 없습니다</h4>
                    <p className="mb-6">강의계획서를 작성하여 체계적인 강의를 진행하세요.</p>
                    <Link to={`/instructor/courses/lectures/${lectureId}/lectureplan/create`}>
                      <Button>
                        <Plus className="w-4 h-4 mr-2" />
                        강의계획서 작성하기
                      </Button>
                    </Link>
                  </div>
                )}
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
