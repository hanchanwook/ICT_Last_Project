import { useState, useEffect } from "react"
import { Search, Calendar, FileText, Clock, CheckCircle, AlertCircle, Plus, Download, Trash2, X, Edit, Eye } from "lucide-react"
import Header from "@/components/layout/header"
import Sidebar from "@/components/layout/sidebar"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Link } from "react-router-dom"
import { getInstructorAssignments, getAssignmentStats, createAssignment, saveAssignmentRubric, updateAssignment, deleteAssignment, getAssignmentIdByUserInfo, getAssignmentDetail, getAssignmentRubric, getAssignmentSubmissions } from "@/api/sunghyun/assignmentApi"
import { getInstructorLectures } from "@/api/sunghyun/instructorCourseApi"
import { getMenuItems } from "@/components/ui/menuConfig"
import { uploadAssignmentMaterial, getAssignmentMaterials, deleteAssignmentMaterial, downloadAssignmentMaterial } from "@/api/sunghyun/assignmentMaterialApi"
import { Card, CardContent } from "@/components/ui/card"

export default function InstructorAssignmentsPage() {
  const [assignments, setAssignments] = useState([])
  const [courses, setCourses] = useState([])
  const [searchTerm, setSearchTerm] = useState("")
  const [filterStatus, setFilterStatus] = useState("all")
  const [filterCourse, setFilterCourse] = useState("all")
  const [loading, setLoading] = useState(true)
  const [showSubmissionModal, setShowSubmissionModal] = useState(false)
  const [isEditMode, setIsEditMode] = useState(false)
  const [editingAssignment, setEditingAssignment] = useState(null)
  const [submissionForm, setSubmissionForm] = useState({
    assignmentTitle: "",
    assignmentContent: "",
    dueDate: "",
    courseId: "",
    fileRequired: false,
    codeRequired: false,
  })
  
  // 과제 자료 업로드 관련 상태
  const [isEditingMaterials, setIsEditingMaterials] = useState(false)
  const [selectedFile, setSelectedFile] = useState(null)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [isUploading, setIsUploading] = useState(false)
  const [assignmentMaterials, setAssignmentMaterials] = useState([])
  const [currentAssignmentId, setCurrentAssignmentId] = useState(null) // 현재 과제 ID 저장
  
  // 루브릭 관련 상태
  const [rubricitem, setRubricitem] = useState([
    { itemTitle: "", maxScore: 10, description: "", itemOrder: 1 }
  ])
  const [useRubric, setUseRubric] = useState(false)
  const [stats, setStats] = useState({
    total: 0,
    active: 0,
    completed: 0,
  })
  const [currentUser, setCurrentUser] = useState(null)

  // 사이드바 메뉴 항목
  const sidebarItems = getMenuItems('instructor-courses')

  // localStorage에서 사용자 정보 가져오기
  useEffect(() => {
    const userInfo = localStorage.getItem('currentUser')
    if (userInfo) {
      const parsedUser = JSON.parse(userInfo)
      console.log('저장된 사용자 정보:', parsedUser)
      setCurrentUser(parsedUser)
    }
  }, [])

  // 폼 상태 변경 모니터링
  useEffect(() => {
    console.log('submissionForm 상태 변경:', submissionForm)
  }, [submissionForm])

  // 루브릭 상태 변경 모니터링
  useEffect(() => {
    console.log('useRubric 상태 변경:', useRubric)
    console.log('rubricitem 상태 변경:', rubricitem)
  }, [useRubric, rubricitem])

  // 과제 목록 및 통계 조회
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      try {
        console.log('[과제등록] API 호출 시작 - 쿠키 기반 인증')
        
        // 과제 목록 조회
        const assignmentsResponse = await getInstructorAssignments()
        console.log('[과제등록] 과제 목록 응답:', assignmentsResponse)
        let assignmentsData = assignmentsResponse.data || assignmentsResponse || []
        
        // 강의 목록 조회 (과제 등록 시 사용)
        console.log('[과제등록] getInstructorLectures 호출 URL:', getInstructorLectures.toString())
        const coursesResponse = await getInstructorLectures()
        console.log('[과제등록] 강의 목록 응답:', coursesResponse)
        
        // 응답 구조에 따라 데이터 추출
        let coursesData = []
        if (Array.isArray(coursesResponse)) {
          coursesData = coursesResponse
        } else if (coursesResponse.data && Array.isArray(coursesResponse.data)) {
          coursesData = coursesResponse.data
        } else if (coursesResponse.result && Array.isArray(coursesResponse.result)) {
          coursesData = coursesResponse.result
        }
        
        console.log('[과제등록] 추출된 강의 목록:', coursesData);
        console.log('[과제등록] 현재 사용자 memberId:', currentUser?.memberId);
        
        // 현재 사용자가 담당하는 강의만 필터링
        const filteredCourses = coursesData.filter(course => {
          console.log(`강의 ${course.courseName} (${course.courseId}) - 담당 강사: ${course.memberId}, 현재 사용자: ${currentUser?.memberId}`);
          return course.memberId === currentUser?.memberId;
        });
        
        // 필터링된 강의가 없으면 모든 강의 표시 (임시 해결책)
        const finalCourses = filteredCourses.length > 0 ? filteredCourses : coursesData;
        console.log('[과제등록] 최종 사용할 강의 목록:', finalCourses);
        
        console.log('[과제등록] 필터링된 강의 목록:', filteredCourses);
        
        // 과제 데이터에 강의 이름 추가
        assignmentsData = assignmentsData.map(assignment => {
          const course = coursesData.find(c => c.courseId === assignment.courseId)
          return {
            ...assignment,
            courseName: course ? course.courseName : '강의명 없음'
          }
        })
        
        setAssignments(assignmentsData)
        setCourses(finalCourses)
        
        // 클라이언트 사이드에서 통계 계산
        const now = new Date()
        const totalAssignments = assignmentsData.length
        const activeAssignments = assignmentsData.filter(assignment => {
          const dueDate = new Date(assignment.dueDate)
          return assignment.assignmentActive !== 1 && dueDate >= now
        }).length
        const completedAssignments = assignmentsData.filter(assignment => {
          const dueDate = new Date(assignment.dueDate)
          return assignment.assignmentActive !== 1 && dueDate < now
        }).length
        
        console.log('[과제등록] 계산된 통계:', {
          total: totalAssignments,
          active: activeAssignments,
          completed: completedAssignments
        })
        
        setStats({
          total: totalAssignments,
          active: activeAssignments,
          completed: completedAssignments,
        })
        
      } catch (error) {
        console.error('[과제등록] 데이터 조회 실패:', error)
        alert('[과제등록] 데이터 조회 실패: ' + (error?.message || error))
        // 에러 발생 시 빈 배열로 설정
        setAssignments([])
        setCourses([])
        setStats({
          total: 0,
          active: 0,
          completed: 0,
        })
      } finally {
        setLoading(false)
      }
    }

    if (currentUser) {
      fetchData()
    }
  }, [currentUser])

  // 과제 상태 판단 함수
  const getAssignmentStatus = (assignment) => {
    const now = new Date()
    const dueDate = new Date(assignment.dueDate)
    
    if (assignment.assignmentActive === 1) {
      return "deleted"
    } else if (dueDate < now) {
      return "overdue"
    } else {
      return "active"
    }
  }

  // 필터링된 과제 목록
  const filteredAssignments = assignments.filter((assignment) => {
    const matchesSearch =
      assignment.assignmentTitle?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      assignment.assignmentContent?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      assignment.courseName?.toLowerCase().includes(searchTerm.toLowerCase())

    const matchesStatus = filterStatus === "all" || getAssignmentStatus(assignment) === filterStatus
    const matchesCourse = filterCourse === "all" || assignment.courseId === filterCourse

    return matchesSearch && matchesStatus && matchesCourse
  })

  const getStatusBadge = (assignment) => {
    const status = getAssignmentStatus(assignment)
    switch (status) {
      case "active":
        return <Badge className="bg-green-100 text-green-800 border-green-200">진행중</Badge>
      case "overdue":
        return <Badge className="bg-red-100 text-red-800 border-red-200">마감됨</Badge>
      case "deleted":
        return <Badge className="bg-gray-100 text-gray-800 border-gray-200">삭제됨</Badge>
      default:
        return <Badge className="bg-gray-100 text-gray-800 border-gray-200">알 수 없음</Badge>
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return "-"
    const date = new Date(dateString)
    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  // 루브릭 관련 함수들
  const addRubricItem = () => {
    setRubricitem(prev => {
      const nextOrder = prev.length + 1
      return [...prev, { itemTitle: "", maxScore: 10, description: "", itemOrder: nextOrder }]
    })
  }

  const removeRubricItem = (index) => {
    if (rubricitem.length > 1) {
      setRubricitem(rubricitem.filter((_, i) => i !== index))
    }
  }

  const updateRubricItem = (index, field, value) => {
    const updatedItems = [...rubricitem]
    updatedItems[index] = { ...updatedItems[index], [field]: value }
    setRubricitem(updatedItems)
  }

  // 과제 자료 업로드 관련 함수들
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

    // 과제 ID 확인
    const assignmentId = currentAssignmentId || editingAssignment?.assignmentId
    
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

      if (assignmentId) {
        // 기존 과제 수정 모드: 즉시 업로드
        const formData = new FormData();
        formData.append('file', selectedFile);
        formData.append('title', selectedFile.name);
        
        // Presigned URL 방식으로 파일 업로드 (강의 자료와 동일한 방식)
        const response = await uploadAssignmentMaterial(assignmentId, formData)

        clearInterval(progressInterval)
        setUploadProgress(100)

        // 서버 응답 확인
        console.log('파일 업로드 응답:', response)
        
        // 응답 데이터 검증
        if (!response) {
          throw new Error('서버에서 올바른 응답을 받지 못했습니다.')
        }

        // 성공 메시지
        alert('파일이 성공적으로 업로드되었습니다.')
        
        // 과제 자료 목록 새로고침
        try {
          const materialsData = await getAssignmentMaterials(assignmentId)
          console.log('업로드 후 과제 자료 목록:', materialsData)
          setAssignmentMaterials(materialsData.data || materialsData || [])
        } catch (error) {
          console.warn('과제 자료 목록 새로고침 실패:', error)
        }
      } else {
        // 신규 등록 모드: 로컬에만 추가 (과제 등록 시 함께 업로드)
        const newMaterialObj = {
          id: Date.now(), // 임시 ID
          name: selectedFile.name,
          fileName: selectedFile.name,
          type: selectedFile.type.split('/')[1].toUpperCase(),
          fileType: selectedFile.type.split('/')[1].toUpperCase(),
          size: formatFileSize(selectedFile.size),
          fileSize: selectedFile.size,
          uploadDate: new Date().toLocaleDateString(),
          file: selectedFile,
          title: selectedFile.name,
        }

        clearInterval(progressInterval)
        setUploadProgress(100)

        // 성공 메시지
        alert('파일이 추가되었습니다. 과제 등록 시 함께 업로드됩니다.')
        
        // 로컬에 추가
        setAssignmentMaterials([...assignmentMaterials, newMaterialObj])
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
    // 과제 ID 확인
    const assignmentId = currentAssignmentId || editingAssignment?.assignmentId
    
    // 디버깅: materialId 확인
    console.log('=== 삭제 요청 디버깅 ===');
    console.log('전달받은 assignmentId:', assignmentId);
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
        if (assignmentId) {
          // 기존 과제 수정 모드: 실제 API 호출
          console.log('🗑️ 파일 삭제 시작:', assignmentId, materialId);
          await deleteAssignmentMaterial(assignmentId, materialId)
          
          // 과제 자료 목록 새로고침
          try {
            const materialsData = await getAssignmentMaterials(assignmentId)
            console.log('삭제 후 과제 자료 목록:', materialsData)
            setAssignmentMaterials(materialsData.data || materialsData || [])
          } catch (error) {
            console.warn('과제 자료 목록 새로고침 실패:', error)
            // 로컬에서 제거
            setAssignmentMaterials(assignmentMaterials.filter((m) => m.id !== materialId))
          }
        } else {
          // 신규 등록 모드: 로컬에서만 제거
          setAssignmentMaterials(assignmentMaterials.filter((m) => m.id !== materialId))
        }
        
        alert('파일이 성공적으로 삭제되었습니다.')
      } catch (error) {
        console.error('파일 삭제 오류:', error)
        alert('파일 삭제에 실패했습니다: ' + error.message)
      }
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

  // 과제 수정 모드 시작
  const handleEditAssignment = async (assignment) => {
    console.log('수정할 과제:', assignment)
    console.log('원본 과제의 fileRequired:', assignment.fileRequired, '타입:', typeof assignment.fileRequired)
    console.log('원본 과제의 codeRequired:', assignment.codeRequired, '타입:', typeof assignment.codeRequired)
    
    try {
      // 과제 제출 현황 조회하여 제출 완료한 학생이 있는지 확인
      try {
        const submissionsResponse = await getAssignmentSubmissions(assignment.assignmentId)
        console.log('과제 제출 현황 응답:', submissionsResponse)
        
        const submissions = submissionsResponse.data || submissionsResponse || []
        console.log('제출 현황 목록:', submissions)
        
        // 제출 완료한 학생이 있는지 확인
        const submittedStudents = submissions.filter(submission => {
          // 제출 상태 확인 (다양한 필드명 고려)
          const status = submission.status || submission.submissionStatus || submission.state
          const hasSubmissionDate = submission.submissionDate || submission.submitDate || submission.createdAt
          
          return status === 'submitted' || status === '제출완료' || hasSubmissionDate
        })
        
        console.log('제출 완료한 학생 수:', submittedStudents.length)
        console.log('제출 완료한 학생들:', submittedStudents)
        
        if (submittedStudents.length > 0) {
          alert('이미 제출 학생이 있습니다.')
          return // 수정 모달을 열지 않고 함수 종료
        }
      } catch (submissionsError) {
        console.warn('과제 제출 현황 조회 실패:', submissionsError)
        // 제출 현황 조회에 실패해도 수정은 허용 (기존 동작 유지)
      }

      // 마감된 과제인지 확인
      const dueDate = assignment.dueDate || assignmentDetail?.dueDate
      if (dueDate) {
        const now = new Date()
        const deadline = new Date(dueDate)
        console.log('현재 시간:', now)
        console.log('마감 시간:', deadline)
        
        if (now > deadline) {
          alert('마감된 과제입니다')
          return // 수정 모달을 열지 않고 함수 종료
        }
      }

      // 과제 상세 정보 조회 (루브릭과 자료 포함)
      const assignmentDetailResponse = await getAssignmentDetail(assignment.assignmentId)
      console.log('과제 상세 정보 응답:', assignmentDetailResponse)
      const assignmentDetail = assignmentDetailResponse.data || assignmentDetailResponse || assignment
      console.log('추출된 과제 상세 정보:', assignmentDetail)
      console.log('상세 정보의 모든 키:', Object.keys(assignmentDetail))
      console.log('상세 정보의 fileRequired:', assignmentDetail.fileRequired, '타입:', typeof assignmentDetail.fileRequired)
      console.log('상세 정보의 codeRequired:', assignmentDetail.codeRequired, '타입:', typeof assignmentDetail.codeRequired)
      
      // 가능한 다른 필드명들도 확인
      console.log('file_required:', assignmentDetail.file_required)
      console.log('code_required:', assignmentDetail.code_required)
      console.log('isFileRequired:', assignmentDetail.isFileRequired)
      console.log('isCodeRequired:', assignmentDetail.isCodeRequired)
      
      // 과제 데이터를 폼에 설정 (다양한 필드명 시도)
      const formData = {
        assignmentTitle: assignmentDetail.assignmentTitle || assignment.assignmentTitle || "",
        assignmentContent: assignmentDetail.assignmentContent || assignment.assignmentContent || "",
        dueDate: assignmentDetail.dueDate ? new Date(assignmentDetail.dueDate).toISOString().slice(0, 16) : 
                assignment.dueDate ? new Date(assignment.dueDate).toISOString().slice(0, 16) : "",
        courseId: assignmentDetail.courseId || assignment.courseId || "",
        fileRequired: Boolean(
          assignmentDetail.fileRequired || 
          assignmentDetail.file_required || 
          assignmentDetail.isFileRequired || 
          assignment.fileRequired || 
          false
        ),
        codeRequired: Boolean(
          assignmentDetail.codeRequired || 
          assignmentDetail.code_required || 
          assignmentDetail.isCodeRequired || 
          assignment.codeRequired || 
          false
        ),
      }
      console.log('설정할 폼 데이터:', formData)
      setSubmissionForm(formData)
      
      // 루브릭 조회
      try {
        const rubricResponse = await getAssignmentRubric(assignment.assignmentId)
        console.log('루브릭 응답:', rubricResponse)
        console.log('루브릭 응답 타입:', typeof rubricResponse)
        console.log('루브릭 응답의 모든 키:', rubricResponse ? Object.keys(rubricResponse) : 'null/undefined')
        
        let rubricData = []
        if (rubricResponse && typeof rubricResponse === 'object') {
          if (Array.isArray(rubricResponse)) {
            rubricData = rubricResponse
            console.log('루브릭이 배열로 반환됨')
          } else if (rubricResponse.rubricitem && Array.isArray(rubricResponse.rubricitem)) {
            rubricData = rubricResponse.rubricitem
            console.log('루브릭이 rubricResponse.rubricitem으로 반환됨')
          } else if (rubricResponse.data && Array.isArray(rubricResponse.data)) {
            rubricData = rubricResponse.data
            console.log('루브릭이 rubricResponse.data로 반환됨')
          } else if (rubricResponse.data && rubricResponse.data.rubricitem && Array.isArray(rubricResponse.data.rubricitem)) {
            rubricData = rubricResponse.data.rubricitem
            console.log('루브릭이 rubricResponse.data.rubricitem으로 반환됨')
          } else if (rubricResponse.result && Array.isArray(rubricResponse.result)) {
            rubricData = rubricResponse.result
            console.log('루브릭이 rubricResponse.result로 반환됨')
          } else {
            console.log('루브릭 데이터를 찾을 수 없음, 전체 응답:', rubricResponse)
          }
        }
        console.log('최종 추출된 루브릭 데이터:', rubricData)
        
        if (rubricData && rubricData.length > 0) {
          console.log('루브릭 데이터가 있음, 설정:', rubricData)
          setRubricitem(rubricData)
          setUseRubric(true)
        } else {
          console.log('루브릭 데이터가 없음, 기본값 설정')
          setRubricitem([{ itemTitle: "", maxScore: 10, description: "", itemOrder: 1 }])
          setUseRubric(false)
        }
      } catch (rubricError) {
        console.warn('루브릭 조회 실패:', rubricError)
        setRubricitem([{ itemTitle: "", maxScore: 10, description: "", itemOrder: 1 }])
        setUseRubric(false)
      }
      
      setEditingAssignment(assignmentDetail)
      setIsEditMode(true)
      setShowSubmissionModal(true)
      
      // 과제 ID 설정
      setCurrentAssignmentId(assignment.assignmentId)
      
      // 과제 자료 목록 조회
      try {
        const materialsResponse = await getAssignmentMaterials(assignment.assignmentId)
        console.log('과제 자료 응답:', materialsResponse)
        let materialsData = []
        if (materialsResponse && typeof materialsResponse === 'object') {
          if (Array.isArray(materialsResponse)) {
            materialsData = materialsResponse
          } else if (materialsResponse.data && Array.isArray(materialsResponse.data)) {
            materialsData = materialsResponse.data
          } else if (materialsResponse.result && Array.isArray(materialsResponse.result)) {
            materialsData = materialsResponse.result
          }
        }
        console.log('최종 추출된 과제 자료 데이터:', materialsData)
        setAssignmentMaterials(materialsData)
      } catch (materialsError) {
        console.warn('과제 자료 조회 실패:', materialsError)
        setAssignmentMaterials([])
      }
      
    } catch (error) {
      console.error('과제 상세 정보 조회 실패:', error)
      // 실패 시 기본 데이터로 설정
      setSubmissionForm({
        assignmentTitle: assignment.assignmentTitle || "",
        assignmentContent: assignment.assignmentContent || "",
        dueDate: assignment.dueDate ? new Date(assignment.dueDate).toISOString().slice(0, 16) : "",
        courseId: assignment.courseId || "",
        fileRequired: Boolean(assignment.fileRequired),
        codeRequired: Boolean(assignment.codeRequired),
      })
      
      setRubricitem([{ itemTitle: "", maxScore: 10, description: "", itemOrder: 1 }])
      setUseRubric(false)
      setEditingAssignment(assignment)
      setIsEditMode(true)
      setShowSubmissionModal(true)
      setCurrentAssignmentId(assignment.assignmentId)
      setAssignmentMaterials([])
    }
  }

  // 과제 등록 처리
  const handleCreateAssignment = async (e) => {
    e.preventDefault()
    
    // 필수 필드 검증
    if (!submissionForm.assignmentTitle.trim()) {
      alert('과제명을 입력해주세요.')
      return
    }
    
    if (!submissionForm.courseId) {
      alert('과정을 선택해주세요.')
      return
    }
    
    if (!submissionForm.assignmentContent.trim()) {
      alert('과제 설명을 입력해주세요.')
      return
    }
    
    if (!submissionForm.dueDate) {
      alert('제출 마감일을 선택해주세요.')
      return
    }
    
    try {
      // localStorage에서 id 가져오기
      const memberId = currentUser?.memberId
      if (!memberId) {
        alert('사용자 정보가 없습니다. 다시 로그인해주세요.')
        return
      }
      
      const assignmentData = {
        ...submissionForm,
        memberId,
      }
      
      // rubricitem에 itemOrder 자동 부여
      const rubricitemWithOrder = rubricitem.map((item, idx) => ({ ...item, itemOrder: idx + 1 }))
      
      // assignmentData와 rubricitem을 하나의 객체로 펼쳐서 전송
      const payload = {
        ...assignmentData,
        rubricitem: useRubric ? rubricitemWithOrder : []
      }
      
      console.log('=== 과제 등록 디버깅 ===');
      console.log('현재 사용자 정보:', currentUser);
      console.log('선택한 과정 ID:', submissionForm.courseId);
      console.log('memberId:', memberId);
      console.log('assignmentData:', assignmentData);
      console.log('rubricitemWithOrder:', rubricitemWithOrder);
      console.log('useRubric:', useRubric);
      console.log('최종 payload:', payload);
      console.log('payload JSON:', JSON.stringify(payload, null, 2));
      
      // 과제 등록
      const createdAssignment = await createAssignment(payload)
      
      // 생성된 과제 ID 저장
      const assignmentId = createdAssignment.data?.assignmentId || createdAssignment.assignmentId || createdAssignment.id
      if (assignmentId) {
        setCurrentAssignmentId(assignmentId)
        console.log('생성된 과제 ID:', assignmentId)
        
        // 과제 등록 후 자료 업로드 (로컬에 저장된 자료들)
        if (assignmentMaterials.length > 0) {
          console.log('로컬에 저장된 자료들 업로드 시작:', assignmentMaterials.length, '개')
          
          for (const material of assignmentMaterials) {
            try {
              if (material.file) {
                const formData = new FormData();
                formData.append('file', material.file);
                formData.append('title', material.title || material.name);
                
                console.log('자료 업로드 중:', material.name)
                await uploadAssignmentMaterial(assignmentId, formData)
                console.log('자료 업로드 성공:', material.name)
              }
            } catch (uploadError) {
              console.error('자료 업로드 실패:', material.name, uploadError)
              // 개별 자료 업로드 실패는 전체 실패로 처리하지 않음
            }
          }
        }
      }
      
      // 루브릭이 설정된 경우 별도로 저장
      if (useRubric && rubricitemWithOrder.length > 0) {
        try {
          if (assignmentId) {
            await saveAssignmentRubric(assignmentId, rubricitemWithOrder)
            console.log('루브릭 저장 완료')
          }
        } catch (rubricError) {
          console.error('루브릭 저장 실패:', rubricError)
          // 루브릭 저장 실패해도 과제 등록은 성공으로 처리
          alert('루브릭 저장에 실패했습니다. 잠시 후 다시 시도해 주세요.')
        }
      }
      
      // 성공 시 모달 닫기 및 폼 초기화
      setShowSubmissionModal(false)
      setIsEditMode(false)
      setEditingAssignment(null)
      setSubmissionForm({
        assignmentTitle: "",
        assignmentContent: "",
        dueDate: "",
        courseId: "",
        fileRequired: false,
        codeRequired: false,
      })
      setRubricitem([{ itemTitle: "", maxScore: 10, description: "", itemOrder: 1 }])
      setUseRubric(false)
      // 자료 업로드 관련 상태 초기화
      setIsEditingMaterials(false)
      setSelectedFile(null)
      setUploadProgress(0)
      setIsUploading(false)
      setAssignmentMaterials([])
      setCurrentAssignmentId(null)
      
      // 성공 메시지
      alert('과제와 자료가 성공적으로 등록되었습니다.')
      
      // 페이지 새로고침
      window.location.reload()
      
    } catch (error) {
      console.error('과제 등록 실패:', error)
      alert('과제 등록에 실패했습니다: ' + error.message)
    }
  }

  // 과제 수정 처리
  const handleUpdateAssignment = async (e) => {
    e.preventDefault()
    
    // 필수 필드 검증
    if (!submissionForm.assignmentTitle.trim()) {
      alert('과제명을 입력해주세요.')
      return
    }
    
    if (!submissionForm.courseId) {
      alert('과정을 선택해주세요.')
      return
    }
    
    if (!submissionForm.assignmentContent.trim()) {
      alert('과제 설명을 입력해주세요.')
      return
    }
    
    if (!submissionForm.dueDate) {
      alert('제출 마감일을 선택해주세요.')
      return
    }
    
    try {
      // localStorage에서 id 가져오기
      const memberId = currentUser?.memberId
      if (!memberId) {
        alert('사용자 정보가 없습니다. 다시 로그인해주세요.')
        return
      }
      
      if (!editingAssignment) {
        alert('수정할 과제 정보가 없습니다.')
        return
      }
      
      const updatedAssignmentData = {
        assignmentId: editingAssignment.assignmentId,
        courseId: submissionForm.courseId,
        memberId,
        assignmentTitle: submissionForm.assignmentTitle,
        assignmentContent: submissionForm.assignmentContent,
        dueDate: submissionForm.dueDate,
        fileRequired: submissionForm.fileRequired,
        codeRequired: submissionForm.codeRequired,
      }
      
      // rubricitem에 itemOrder 자동 부여
      const rubricitemWithOrder = rubricitem.map((item, idx) => ({ ...item, itemOrder: idx + 1 }))
      
      // assignmentData와 rubricitem을 하나의 객체로 펼쳐서 전송 (자료는 별도 업로드)
      const payload = {
        ...updatedAssignmentData,
        rubricitem: useRubric ? rubricitemWithOrder : [],
      }
      // 과제 수정
      await updateAssignment(editingAssignment.assignmentId, payload)
      
      // 루브릭이 설정된 경우 별도로 저장
      if (useRubric && rubricitem.length > 0) {
        try {
          const rubricitemWithOrder = rubricitem.map((item, idx) => ({ ...item, itemOrder: idx + 1 }))
          await saveAssignmentRubric(editingAssignment.assignmentId, rubricitemWithOrder)
          console.log('루브릭 수정 완료')
        } catch (rubricError) {
          console.error('루브릭 수정 실패:', rubricError)
          alert('루브릭 수정에 실패했습니다. 잠시 후 다시 시도해 주세요.')
        }
      }
      
      // 성공 시 모달 닫기 및 폼 초기화
      setShowSubmissionModal(false)
      setIsEditMode(false)
      setEditingAssignment(null)
      setSubmissionForm({
        assignmentTitle: "",
        assignmentContent: "",
        dueDate: "",
        courseId: "",
        fileRequired: false,
        codeRequired: false,
      })
      setRubricitem([{ itemTitle: "", maxScore: 10, description: "", itemOrder: 1 }])
      setUseRubric(false)
      // 자료 업로드 관련 상태 초기화
      setIsEditingMaterials(false)
      setSelectedFile(null)
      setUploadProgress(0)
      setIsUploading(false)
      setAssignmentMaterials([])
      setCurrentAssignmentId(null)
      
      // 페이지 새로고침 (실제로는 상태 업데이트가 더 효율적)
      window.location.reload()
      
    } catch (error) {
      console.error('과제 수정 실패:', error)
      alert('과제 수정에 실패했습니다: ' + error.message)
    }
  }

  // 과제 삭제 처리
  const handleDeleteAssignment = async (assignmentId) => {
    try {
      // localStorage에서 id 가져오기
      const memberId = currentUser?.memberId
      if (!memberId) {
        alert('사용자 정보가 없습니다. 다시 로그인해주세요.')
        return
      }
      
      // 과제 삭제
      await deleteAssignment(assignmentId)
      
      // 페이지 새로고침 (실제로는 상태 업데이트가 더 효율적)
      window.location.reload()
      
    } catch (error) {
      console.error('과제 삭제 실패:', error)
      alert('과제 삭제에 실패했습니다: ' + error.message)
    }
  }

  // 수정 모달에서 과제 삭제 처리
  const handleDeleteFromModal = async () => {
    if (!editingAssignment) {
      alert('삭제할 과제 정보가 없습니다.')
      return
    }

    const confirmDelete = window.confirm(
      `"${editingAssignment.assignmentTitle}" 과제를 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`
    )

    if (confirmDelete) {
      try {
        await handleDeleteAssignment(editingAssignment.assignmentId)
        // 삭제 성공 시 모달 닫기
        setShowSubmissionModal(false)
        setIsEditMode(false)
        setEditingAssignment(null)
        // 자료 업로드 관련 상태 초기화
        setIsEditingMaterials(false)
        setSelectedFile(null)
        setUploadProgress(0)
        setIsUploading(false)
        setAssignmentMaterials([])
        setCurrentAssignmentId(null)
      } catch (error) {
        // 에러는 handleDeleteAssignment에서 처리됨
      }
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header />
        <div className="flex">
          <Sidebar title="과제 관리" menuItems={sidebarItems} currentPath="/instructor/courses/assignments" />
          <main className="flex-1 p-6">
            <div className="flex items-center justify-center h-96">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                <p className="mt-4 text-gray-600">과제 목록을 불러오는 중...</p>
              </div>
            </div>
          </main>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <div className="flex">
        <Sidebar title="과제 관리" menuItems={sidebarItems} currentPath="/instructor/courses/assignments" />
        <main className="flex-1 p-6">
          <div className="mb-6">
            <h1 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>
              과제 리스트
            </h1>
          </div>
          <div className="space-y-6">
            {/* 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600">총 과제 수</p>
                      <p className="text-3xl font-bold" style={{ color: "#3498db" }}>
                        {stats.total}
                      </p>
                    </div>
                    <div className="bg-[#EFF6FF] rounded-full p-3 mr-3"> 
                      <FileText className="h-10 w-10 text-[#3498db]" />
                    </div>
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600">진행중</p>
                      <p className="text-3xl font-bold" style={{ color: "#2ECC71" }}>{stats.active}</p>
                    </div>
                    <div className="bg-[#e4f5eb] rounded-full p-3 mr-3"> 
                      <CheckCircle className="h-10 w-10 text-[#2ECC71]" />
                    </div>
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-m font-medium text-gray-600">마감됨</p>
                      <p className="text-3xl font-bold" style={{ color: "#E74C3C" }}>{stats.completed}</p>
                    </div>
                    <div className="bg-red-100 rounded-full p-3 mr-3"> 
                      <Clock className="h-10 w-10 text-[#E74C3C]" />
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* 검색 및 필터 */}
            <div className="bg-white p-6 rounded-lg shadow-sm border">
              <div className="flex flex-col md:flex-row gap-4">
                <div className="flex-1">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
                    <input
                      type="text"
                      placeholder="과제명, 설명, 과정명으로 검색..."
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                    />
                  </div>
                </div>
                <div className="flex gap-2">
                  <select
                    className="px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                  >
                    <option value="all">모든 상태</option>
                    <option value="active">진행중</option>
                    <option value="overdue">마감됨</option>
                    <option value="deleted">삭제됨</option>
                  </select>
                  <select
                    className="px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    value={filterCourse}
                    onChange={(e) => setFilterCourse(e.target.value)}
                  >
                    <option value="all">모든 과정</option>
                    {courses.map((course) => (
                      <option key={course.courseId} value={course.courseId}>
                        {course.courseName}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* 과제 목록 */}
            <div className="bg-white rounded-lg shadow-sm border overflow-hidden">
              <div className="p-6 border-b border-gray-200">
                <div className="flex justify-between items-center">
                  <h3 className="text-lg font-semibold" style={{ color: "#2C3E50" }}>
                    과제 목록 ({filteredAssignments.length}개)
                  </h3>
                  <Button className=" hover:bg-[#1abc9c] hover:text-white
                  text-[#1abc9c] border border-[#1abc9c]" onClick={() => setShowSubmissionModal(true)}>
                    <Plus className="w-4 h-4 mr-2" />새 과제 등록
                  </Button>
                </div>
              </div>

              {filteredAssignments.length === 0 ? (
                <div className="p-12 text-center">
                  <FileText className="mx-auto h-12 w-12 text-gray-400 mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">과제가 없습니다</h3>
                  <p className="text-gray-500">검색 조건을 변경하거나 새로운 과제를 등록해보세요.</p>
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          과제 정보
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          과정
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          제출 마감일
                        </th>

                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          상태
                        </th>
                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                          관리
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {filteredAssignments.map((assignment) => (
                        <tr key={assignment.assignmentId} className="hover:bg-gray-50">
                          <td className="px-6 py-4">
                            <div className="text-sm font-medium text-gray-900">{assignment.assignmentTitle}</div>
                          </td>
                          <td className="px-6 py-4">
                            <div className="text-sm text-gray-900">{assignment.courseName}</div>
                          </td>
                          <td className="px-6 py-4">
                            <div className="flex items-center text-sm text-gray-900">
                              <Calendar className="w-4 h-4 mr-2 text-gray-400" />
                              {formatDate(assignment.dueDate)}
                            </div>
                          </td>
                          
                          <td className="px-6 py-4">{getStatusBadge(assignment)}</td>
                          <td className="px-6 py-4 flex justify-end">
                            <div className="flex space-x-2">
                              {getAssignmentStatus(assignment) === "active" && (
                                <Button 
                                  size="sm" 
                                  className="text-[#95A5A6] hover:bg-blue-50 hover:scale-105 bg-transparent"
                                  onClick={() => handleEditAssignment(assignment)}
                                >
                                  <Edit className="w-4 h-4" />
                                </Button>
                              )}
                              <Link to={`/instructor/courses/assignments/detail/${assignment.assignmentId}`}>
                                <Button
                                  size="sm"
                                  className="text-[#1abc9c] hover:bg-blue-50 hover:scale-105 bg-transparent"
                                >
                                  <Eye className="w-4 h-4" />
                                </Button>
                              </Link>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* 안내사항 */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex">
                <div className="flex-shrink-0">
                  <AlertCircle className="h-5 w-5 text-blue-400" />
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-blue-800">과제 관리 안내</h3>
                  <div className="mt-2 text-sm text-blue-700">
                    <ul className="list-disc list-inside space-y-1">
                      <li>과제 제출 마감일이 지나면 자동으로 상태가 변경됩니다.</li>
                      <li>학생들의 과제 제출 현황을 실시간으로 확인할 수 있습니다.</li>

                      <li>파일 필수 또는 코드 필수 옵션을 설정하여 제출 요구사항을 지정할 수 있습니다.</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
      
      {/* 과제 등록 모달 */}
      {showSubmissionModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl mx-4 max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-gray-200">
              <div className="flex justify-between items-center">
                <h2 className="text-xl font-semibold" style={{ color: "#2C3E50" }}>
                  {isEditMode ? "과제 수정" : "새 과제 등록"}
                </h2>
                <button onClick={() => {
                  setShowSubmissionModal(false)
                  setIsEditMode(false)
                  setEditingAssignment(null)
                  setSubmissionForm({
                    assignmentTitle: "",
                    assignmentContent: "",
                    dueDate: "",
                    courseId: "",
                    fileRequired: false,
                    codeRequired: false,
                  })
                  setRubricitem([{ itemTitle: "", maxScore: 10, description: "", itemOrder: 1 }])
                  setUseRubric(false)
                  // 자료 업로드 관련 상태 초기화
                  setIsEditingMaterials(false)
                  setSelectedFile(null)
                  setUploadProgress(0)
                  setIsUploading(false)
                  setAssignmentMaterials([])
                  setCurrentAssignmentId(null)
                }} className="text-gray-400 hover:text-gray-600">
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>

            <form className="p-6 space-y-6" onSubmit={isEditMode ? handleUpdateAssignment : handleCreateAssignment}>
              {/* 과제명 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  과제명 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="과제명을 입력하세요"
                  value={submissionForm.assignmentTitle}
                  onChange={(e) => setSubmissionForm({ ...submissionForm, assignmentTitle: e.target.value })}
                  required
                />
              </div>

              {/* 과정 선택 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  과정 선택 <span className="text-red-500">*</span>
                </label>
                <select
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  value={submissionForm.courseId}
                  onChange={(e) => setSubmissionForm({ ...submissionForm, courseId: e.target.value })}
                  required
                >
                  <option value="">과정을 선택하세요</option>
                  {Array.isArray(courses) && courses.length > 0 ? (
                    courses.map((course, index) => {
                      // 필드명 안전하게 처리
                      const id = course.courseId || course.id || course.code || course.lectureId || index;
                      const name = course.courseName || course.name || course.title || course.lectureName || `강의 ${index + 1}`;
                      if (!id || !name) {
                        console.warn('과정 데이터에 courseId/courseName이 없습니다:', course);
                      }
                      return (
                        <option key={id} value={id}>
                          {name}
                        </option>
                      );
                    })
                  ) : (
                    <option disabled>등록된 과정이 없습니다.</option>
                  )}
                </select>
                {(!Array.isArray(courses) || courses.length === 0) && (
                  <p className="text-sm text-gray-500 mt-1">담당 강의가 없습니다.</p>
                )}
              </div>

              {/* 과제 설명 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  과제 설명 <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={4}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="과제에 대한 상세한 설명을 입력하세요"
                  value={submissionForm.assignmentContent}
                  onChange={(e) => setSubmissionForm({ ...submissionForm, assignmentContent: e.target.value })}
                  required
                />
              </div>

              {/* 과제 자료 업로드 */}
              <div>
                <div className="flex justify-between items-center mb-4">
                  <label className="block text-sm font-medium text-gray-700">
                    과제 자료 (선택사항)
                  </label>
                  <Button 
                    type="button"
                    onClick={() => setIsEditingMaterials(true)} 
                    size="sm" 
                    className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                  >
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
                          id="assignment-file-upload"
                          disabled={isUploading}
                        />
                        <label htmlFor="assignment-file-upload" className="cursor-pointer">
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
                              <FileText className="w-5 h-5 text-blue-500 mr-2" />
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
                              className="text-red-600 hover:bg-red-200"
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
                          type="button"
                          onClick={handleUploadMaterial} 
                          size="sm"
                          disabled={!selectedFile || isUploading}
                          className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
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
                          type="button"
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
                  {assignmentMaterials.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">
                      <FileText className="w-12 h-12 mx-auto mb-2 text-gray-300" />
                      <p>업로드된 자료가 없습니다.</p>
                    </div>
                  ) : (
                    assignmentMaterials.map((material, index) => (
                      <div key={material.id || material.fileId || `material-${index}`} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                        <div className="flex items-center">
                          <div className="p-2 bg-blue-100 rounded-lg mr-3 w-12 h-12 flex items-center justify-center">
                            <span>{getFileIcon(material?.fileName || material?.name || '')}</span>
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
                            className="p-1 text-blue-600 hover:text-blue-700 bg-transparent"
                            onClick={async () => {
                              console.log('=== 과제 파일 다운로드 디버깅 ===');
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
                                  await downloadAssignmentMaterial(downloadKey, material?.fileName || material?.name);
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

              {/* 제출 마감일 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  제출 마감일 <span className="text-red-500">*</span>
                </label>
                <input
                  type="datetime-local"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  value={submissionForm.dueDate}
                  onChange={(e) => setSubmissionForm({ ...submissionForm, dueDate: e.target.value })}
                  required
                />
              </div>

              {/* 추가 옵션 */}
              <div className="space-y-4">
                <div className="flex items-center">
                  <input
                    id="file-required"
                    type="checkbox"
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    checked={submissionForm.fileRequired}
                    onChange={(e) => setSubmissionForm({ ...submissionForm, fileRequired: e.target.checked })}
                  />
                  <label htmlFor="file-required" className="ml-2 block text-sm text-gray-700">
                    파일 제출 필수
                  </label>
                </div>
                <div className="flex items-center">
                  <input
                    id="code-required"
                    type="checkbox"
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    checked={submissionForm.codeRequired}
                    onChange={(e) => setSubmissionForm({ ...submissionForm, codeRequired: e.target.checked })}
                  />
                  <label htmlFor="code-required" className="ml-2 block text-sm text-gray-700">
                    코드 제출 필수
                  </label>
                </div>
              </div>

              {/* 루브릭 설정 */}
              <div className="border-t pt-6">
                <div className="flex items-center mb-4">
                  <input
                    id="use-rubric"
                    type="checkbox"
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    checked={useRubric}
                    onChange={(e) => setUseRubric(e.target.checked)}
                  />
                  <label htmlFor="use-rubric" className="ml-2 block text-sm font-medium text-gray-700">
                    채점 기준(루브릭) 설정
                  </label>
                </div>

                {useRubric && (
                  <div className="space-y-4">
                    <div className="bg-gray-50 p-4 rounded-lg">
                      <p className="text-sm text-gray-600 mb-3">
                        채점 기준을 설정하면 학생들이 평가 기준을 미리 확인할 수 있고, 
                        채점 시 일관성 있는 평가가 가능합니다.
                      </p>
                    </div>

                    {rubricitem.map((item, index) => (
                      <div key={index} className="border border-gray-200 rounded-lg p-4 space-y-3">
                        <div className="flex justify-between items-center">
                          <h4 className="text-sm font-medium text-gray-700">채점 항목 {index + 1}</h4>
                          {rubricitem.length > 1 && (
                            <button
                              type="button"
                              onClick={() => removeRubricItem(index)}
                              className="text-red-500 hover:text-red-700 text-sm"
                            >
                              삭제
                            </button>
                          )}
                        </div>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                          <div>
                            <label className="block text-xs font-medium text-gray-600 mb-1">
                              항목명 <span className="text-red-500">*</span>
                            </label>
                            <input
                              type="text"
                              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                              placeholder="예: 정확성, 표현력, 창의성"
                              value={item.itemTitle}
                              onChange={(e) => updateRubricItem(index, 'itemTitle', e.target.value)}
                              required={useRubric}
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-600 mb-1">
                              배점 <span className="text-red-500">*</span>
                            </label>
                            <input
                              type="number"
                              min="1"
                              max="100"
                              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                              placeholder="10"
                              value={item.maxScore}
                              onChange={(e) => updateRubricItem(index, 'maxScore', parseInt(e.target.value) || 0)}
                              required={useRubric}
                            />
                          </div>
                        </div>
                        
                        <div>
                          <label className="block text-xs font-medium text-gray-600 mb-1">
                            상세 기준
                          </label>
                          <textarea
                            rows={2}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                            placeholder="이 항목에 대한 상세한 채점 기준을 설명하세요"
                            value={item.description}
                            onChange={(e) => updateRubricItem(index, 'description', e.target.value)}
                          />
                        </div>
                      </div>
                    ))}

                    <button
                      type="button"
                      onClick={addRubricItem}
                      className="w-full py-2 px-4 border-2 border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-gray-400 hover:text-gray-600 transition-colors"
                    >
                      + 채점 항목 추가
                    </button>
                  </div>
                )}
              </div>
            </form>

            <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 flex justify-between items-center">
              {/* 왼쪽: 삭제 버튼 (수정 모드일 때만 표시) */}
              {isEditMode && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleDeleteFromModal}
                  className="bg-white border-red-300 text-red-600 hover:bg-red-50 hover:border-red-400"
                >
                  과제 삭제
                </Button>
              )}
              
              {/* 오른쪽: 취소 및 저장 버튼 */}
              <div className="flex space-x-3">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setShowSubmissionModal(false)
                    setIsEditMode(false)
                    setEditingAssignment(null)
                    setSubmissionForm({
                      assignmentTitle: "",
                      assignmentContent: "",
                      dueDate: "",
                      courseId: "",
                      fileRequired: false,
                      codeRequired: false,
                    })
                    setRubricitem([{ itemTitle: "", maxScore: 10, description: "", itemOrder: 1 }])
                    setUseRubric(false)
                    // 자료 업로드 관련 상태 초기화
                    setIsEditingMaterials(false)
                    setSelectedFile(null)
                    setUploadProgress(0)
                    setIsUploading(false)
                    setAssignmentMaterials([])
                    setCurrentAssignmentId(null)
                  }}
                  className="bg-white border-gray-300 text-gray-700 hover:bg-gray-50"
                >
                  취소
                </Button>
                <Button
                  type="submit"
                  className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                  onClick={isEditMode ? handleUpdateAssignment : handleCreateAssignment}
                >
                  {isEditMode ? "과제 수정" : "과제 등록"}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
