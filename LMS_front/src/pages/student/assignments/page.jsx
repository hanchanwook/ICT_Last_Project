import { useState, useEffect } from "react"
import { Search, Calendar, Clock, FileText, Download, Upload, CheckCircle, AlertCircle, XCircle } from "lucide-react"
import Header from "@/components/layout/header"
import Sidebar from "@/components/layout/sidebar"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { getStudentAssignments, createStudentAssignmentSubmission, updateStudentAssignmentSubmission, deleteStudentAssignmentSubmission, getMyAssignmentSubmissions, getStudentAssignmentDetail, getStudentAssignmentRubric, getStudentSubmissionDetail } from "@/api/sunghyun/studentAssignmentApi"
import { getAssignmentMaterials, downloadAssignmentMaterialByKey } from "@/api/sunghyun/assignmentMaterialApi"
import { 
  uploadStudentAssignmentFile, 
  deleteStudentSubmissionFile,
  downloadStudentSubmissionFile,
  useFileUploadService,
  getStudentSubmissionFilesForDetail
} from "@/api/sunghyun/studentAssignmentMaterialApi"
import {
  getAssignmentSubmissionFiles,
  uploadAssignmentSubmissionFile,
  deleteAssignmentSubmissionFile,
  getAssignmentSubmission,
  submitAssignment,
  updateAssignmentSubmission,
  connectSubmissionFiles,
  updateSubmissionFileIds,
  downloadFile,
  handleApiResponse,
  handleApiError,
  downloadAssignmentMaterial
} from "@/api/sunghyun/studentAssignmentMaterialApiView"
import MonacoEditor from "@monaco-editor/react";
import { getStudentCoursesByMemberId } from "@/api/sunghyun/studentCourseApi";
import { getInstructors } from "@/api/kayoung/questionBankApi";

export default function StudentAssignmentsPage() {
  const [assignments, setAssignments] = useState([])
  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [subjectFilter, setSubjectFilter] = useState("all")
  const [isLoading, setIsLoading] = useState(true)
  const [selectedAssignment, setSelectedAssignment] = useState(null)
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false)
  const [error, setError] = useState(null)
  // 실제 로그인된 학생의 id를 localStorage에서 가져와야 함
  const [currentUser, setCurrentUser] = useState(null)
  const [showSubmitModal, setShowSubmitModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [targetAssignment, setTargetAssignment] = useState(null)
  const [answerText, setAnswerText] = useState(""); // HTML
  const [answerCss, setAnswerCss] = useState(""); // CSS
  const [answerJs, setAnswerJs] = useState(""); // JS
  const [file, setFile] = useState(null)
  const [submissionType, setSubmissionType] = useState("서술")
  const [editSubmission, setEditSubmission] = useState(null)
  const [editAnswerText, setEditAnswerText] = useState("")
  const [editAnswerCss, setEditAnswerCss] = useState("") // CSS for edit modal
  const [editAnswerJs, setEditAnswerJs] = useState("") // JS for edit modal
  const [editSubmissionType, setEditSubmissionType] = useState("서술")
  const [submissions, setSubmissions] = useState([])
  const [editFile, setEditFile] = useState(null)
  // 코드 타입 상태 추가
  const [codeType, setCodeType] = useState("html");
  const [courses, setCourses] = useState([]);
  const [instructors, setInstructors] = useState([]);
  const [assignmentMaterials, setAssignmentMaterials] = useState([]);
  // 학생 제출 파일 관련 상태
  const [submissionFiles, setSubmissionFiles] = useState([]);
  const [selectedSubmissionFile, setSelectedSubmissionFile] = useState(null);
  const [isUploadingSubmissionFile, setIsUploadingSubmissionFile] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  
  // 제출 상세 정보 관련 상태
  const [submissionDetail, setSubmissionDetail] = useState(null);
  const [submissionDetailFiles, setSubmissionDetailFiles] = useState([]);
  
  // 코드 제출 보기 모드 상태 추가
  const [codeViewMode, setCodeViewMode] = useState("서술"); // "서술" 또는 "코드"
  const [parsedCodeData, setParsedCodeData] = useState({
    html: "",
    css: "",
    js: ""
  });
  
  const sidebarMenuItems = [
    {
      label: "과제 리스트",
      href: "/student/assignments",
      icon: "FileText"
    }
  ]
  

  
  // 파일 업로드 서비스 훅 사용
  const { uploadFileWithPresignedUrl, uploadMultipleFiles, getImageUrl } = useFileUploadService();

  useEffect(() => {
    const userInfo = localStorage.getItem('currentUser')
    if (userInfo) {
      setCurrentUser(JSON.parse(userInfo))
    }
  }, [])

  const fetchAll = async () => {
    try {
      setIsLoading(true)
      const assignmentsData = await getStudentAssignments()
      const submissionsData = await getMyAssignmentSubmissions()
      const coursesData = await getStudentCoursesByMemberId()
      const instructorsData = await getInstructors()
      
      // submissionsData가 { data: [...] } 형태라면 아래처럼!
      const submissionList = Array.isArray(submissionsData)
        ? submissionsData
        : (Array.isArray(submissionsData?.data) ? submissionsData.data : []);
      
      // 과제 데이터에 courseName과 instructorName 추가
      const assignmentsWithCourseName = await Promise.all(assignmentsData.map(async (assignment) => {
        const course = coursesData.find(course => course.courseId === assignment.courseId)
        
        // 강사가 등록한 과제 상세 정보 가져오기
        let assignmentDetail = null
        let rubricData = []
        let instructorName = `강사 ${assignment.memberId?.substring(0, 8)}...`
        
        try {
          // 강사 과제 상세 정보 조회
          console.log(`과제 ${assignment.assignmentId} 상세 정보 조회 시작`)
          const detailResponse = await getStudentAssignmentDetail(assignment.assignmentId)
          console.log(`과제 ${assignment.assignmentId} 상세 정보 응답:`, detailResponse)
          
          if (detailResponse && detailResponse.data) {
            assignmentDetail = detailResponse.data
            // 강사 이름이 있으면 사용
            if (assignmentDetail.memberName) {
              instructorName = assignmentDetail.memberName
            }
            console.log(`과제 ${assignment.assignmentId} 상세 정보 파싱 완료:`, assignmentDetail)
          }
        } catch (error) {
          console.warn(`과제 ${assignment.assignmentId} 상세 정보 조회 실패:`, error)
        }
        
        // 루브릭 정보 가져오기
        try {
          const rubricResponse = await getStudentAssignmentRubric(assignment.assignmentId)
          
          if (rubricResponse && rubricResponse.rubricitem) {
            rubricData = rubricResponse.rubricitem
          } else if (Array.isArray(rubricResponse)) {
            rubricData = rubricResponse
          } else if (rubricResponse && rubricResponse.data && rubricResponse.data.rubricitem) {
            rubricData = rubricResponse.data.rubricitem
          }
        } catch (error) {
          // 루브릭 조회 실패 시 빈 배열 사용
        }
        
        return {
          ...assignment,
          ...assignmentDetail, // 강사가 등록한 상세 정보로 덮어쓰기
          courseName: course ? course.courseName : `과정 ${assignment.courseId?.substring(0, 8)}...`,
          instructorName: instructorName,
          rubric: rubricData
        }
      }))
      
      setAssignments(assignmentsWithCourseName || [])
      setSubmissions(submissionList)
      setCourses(coursesData)
      setInstructors(instructorsData)

    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    if (currentUser) fetchAll()
  }, [currentUser])

  // 검색 및 필터링
  useEffect(() => {
    let filtered = assignments;

    if (searchTerm) {
      filtered = filtered.filter(
        (assignment) =>
          (assignment.assignmentTitle && assignment.assignmentTitle.toLowerCase().includes(searchTerm.toLowerCase())) ||
          (assignment.courseName && assignment.courseName.toLowerCase().includes(searchTerm.toLowerCase())) ||
          (assignment.memberId && assignment.memberId.toLowerCase().includes(searchTerm.toLowerCase())),
      );
    }

    if (statusFilter !== "all") {
      filtered = filtered.filter((assignment) => assignment.status === statusFilter);
    }

    if (subjectFilter !== "all") {
      filtered = filtered.filter((assignment) => assignment.courseId === subjectFilter);
    }

    // setFilteredAssignments(filtered); // 삭제
  }, [searchTerm, statusFilter, subjectFilter, assignments]);

  const getStatusBadge = (status) => {
    switch (status) {
      case "submitted":
        return (
          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
            <CheckCircle className="w-3 h-3 mr-1" />
            제출완료
          </span>
        )
      case "submitted_overdue":
        return (
          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-orange-100 text-orange-800">
            <CheckCircle className="w-3 h-3 mr-1" />
            제출완료(기한초과)
          </span>
        )
      case "pending":
        return (
          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
            <Clock className="w-3 h-3 mr-1" />
            제출대기
          </span>
        )
      case "overdue":
        return (
          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">
            <XCircle className="w-3 h-3 mr-1" />
            기한초과
          </span>
        )
      default:
        return null
    }
  }

  const getDaysUntilDue = (dueDate) => {
    const today = new Date()
    const due = new Date(dueDate)
    const diffTime = due - today
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
    return diffDays
  }

  const getUniqueSubjects = () => {
    // courseId와 courseName을 함께 가져와서 courseName을 우선 표시
    const subjects = assignmentsWithStatus.map((assignment) => ({
      courseId: assignment.courseId,
      courseName: assignment.courseName || `과정 ${assignment.courseId?.substring(0, 8)}...`
    }));
    
    // 중복 제거 (courseId 기준)
    const uniqueSubjects = subjects.filter((subject, index, self) => 
      index === self.findIndex(s => s.courseId === subject.courseId)
    );
    
    return uniqueSubjects;
  }

  const handleFileUpload = (assignmentId) => {
    // 파일 업로드 로직
    console.log("파일 업로드:", assignmentId)
  }

  const handleDownloadAttachment = (filename) => {
    // 첨부파일 다운로드 로직
    console.log("첨부파일 다운로드:", filename)
  }

  const handleViewAssignmentDetail = async (assignment) => {
    setSelectedAssignment(assignment)
    setIsDetailModalOpen(true)
    
    // 코드 제출 데이터 파싱 (제출 타입이 "코드"인 경우)
    if (assignment.mySubmission?.submissionType === "코드") {
      const parsedData = parseCodeSubmission(assignment.mySubmission.answerText);
      setParsedCodeData(parsedData);
      setCodeViewMode("서술"); // 기본값은 서술로 보기
    } else {
      setParsedCodeData({ html: "", css: "", js: "" });
      setCodeViewMode("서술");
    }
    
    // 제출 상세 정보 가져오기 (기존 코드 유지)
    try {
      console.log('제출 상세 정보 조회 시작:', assignment.assignmentId)
      const detail = await getStudentSubmissionDetail(assignment.assignmentId)
      console.log('제출 상세 정보:', detail)
      setSubmissionDetail(detail)
    } catch (error) {
      console.warn('제출 상세 정보 조회 실패:', error)
      setSubmissionDetail(null)
    }
    
    // 과제 제출 모달과 동일한 방식으로 제출 파일 목록 가져오기
    try {
      // mySubmission에서 submissionId를 가져옴 (파일 조회용)
      const submissionId = assignment.mySubmission?.submissionId
      
      if (!submissionId) {
        setSubmissionDetailFiles([])
        return
      }
      
      const submissionFilesResponse = await getStudentSubmissionFilesForDetail(assignment.assignmentId, assignment.courseId, submissionId)
      
      // 과제 제출 모달과 동일한 응답 처리 방식 사용
      let submissionFilesData = []
      if (submissionFilesResponse && typeof submissionFilesResponse === 'object') {
        if (Array.isArray(submissionFilesResponse)) {
          submissionFilesData = submissionFilesResponse
        } else if (submissionFilesResponse.data && Array.isArray(submissionFilesResponse.data)) {
          submissionFilesData = submissionFilesResponse.data
        } else if (submissionFilesResponse.result && Array.isArray(submissionFilesResponse.result)) {
          submissionFilesData = submissionFilesResponse.result
        }
      }
      setSubmissionDetailFiles(submissionFilesData)
    } catch (error) {
      setSubmissionDetailFiles([])
    }
  }

  const closeDetailModal = () => {
    setIsDetailModalOpen(false)
    setSelectedAssignment(null)
    setSubmissionDetail(null)
    setSubmissionDetailFiles([])
    setCodeViewMode("서술") // 모달 닫을 때 보기 모드 초기화
  }

  // 코드 제출 데이터 파싱 함수 추가
  const parseCodeSubmission = (answerText) => {
    if (!answerText) return { html: "", css: "", js: "" };
    
    try {
      // HTML 태그에서 CSS와 JS 추출
      const htmlMatch = answerText.match(/<html[^>]*>([\s\S]*?)<\/html>/i);
      if (!htmlMatch) return { html: answerText, css: "", js: "" };
      
      const htmlContent = htmlMatch[1];
      
      // CSS 추출
      const cssMatch = htmlContent.match(/<style[^>]*>([\s\S]*?)<\/style>/i);
      const css = cssMatch ? cssMatch[1].trim() : "";
      
      // JS 추출
      const jsMatch = htmlContent.match(/<script[^>]*>([\s\S]*?)<\/script>/i);
      const js = jsMatch ? jsMatch[1].trim() : "";
      
      // HTML body 내용 추출
      const bodyMatch = htmlContent.match(/<body[^>]*>([\s\S]*?)<\/body>/i);
      const html = bodyMatch ? bodyMatch[1].trim() : htmlContent;
      
      return { html, css, js };
    } catch (error) {
      console.error('코드 파싱 오류:', error);
      return { html: answerText, css: "", js: "" };
    }
  };

  const openSubmitModal = async (assignment) => {
    setTargetAssignment(assignment)
    
    // 과제 자료 목록 조회
    try {
      const materialsResponse = await getAssignmentMaterials(assignment.assignmentId)
      
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
      setAssignmentMaterials(materialsData)
    } catch (error) {
      setAssignmentMaterials([])
    }
    
    // 학생 제출 파일 목록 조회
    try {
      // mySubmission에서 submissionId를 가져옴 (파일 조회용)
      const submissionId = assignment.mySubmission?.submissionId
      
      const submissionFilesResponse = await getStudentSubmissionFilesForDetail(assignment.assignmentId, assignment.courseId, submissionId)
      
      let submissionFilesData = []
      if (submissionFilesResponse && typeof submissionFilesResponse === 'object') {
        if (Array.isArray(submissionFilesResponse)) {
          submissionFilesData = submissionFilesResponse
        } else if (submissionFilesResponse.data && Array.isArray(submissionFilesResponse.data)) {
          submissionFilesData = submissionFilesResponse.data
        } else if (submissionFilesResponse.result && Array.isArray(submissionFilesResponse.result)) {
          submissionFilesData = submissionFilesResponse.result
        }
      }
      setSubmissionFiles(submissionFilesData)
    } catch (error) {
      setSubmissionFiles([])
    }
    
    setShowSubmitModal(true)
  }
  const openEditModal = (submission) => {
    setEditSubmission(submission)
    setEditAnswerText(submission.answerText || "")
    setEditAnswerCss(submission.answerCss || "") // Initialize CSS content
    setEditAnswerJs(submission.answerJs || "") // Initialize JS content
    setEditSubmissionType(submission.submissionType || "서술")
    setShowEditModal(true)
  }
  const openDeleteModal = (assignment) => {
    setTargetAssignment(assignment)
    setShowDeleteModal(true)
  }

  // 과제 자료 다운로드 함수
  const handleDownloadAssignmentMaterial = async (material) => {
    try {
      console.log('=== 과제 자료 다운로드 디버깅 시작 ===');
      console.log('material 객체:', material);
      console.log('currentUser:', currentUser);
      
      // 가능한 모든 다운로드 키 확인
      const possibleKeys = [
        material?.materialId,
        material?.fileKey,
        material?.fileId,
        material?.id
      ].filter(key => key != null && key !== '');
      
      console.log('가능한 다운로드 키들:', possibleKeys);
      console.log('각 키 값:', {
        materialId: material?.materialId,
        fileKey: material?.fileKey,
        fileId: material?.fileId,
        id: material?.id
      });
      
      // materialId를 우선적으로 사용하고, 없으면 다른 키들 시도
      const downloadKey = material?.materialId || material?.fileKey || material?.fileId || material?.id;
      const fileName = material?.fileName || material?.name || 'download';
      
      console.log('최종 선택된 다운로드 키:', downloadKey);
      console.log('키 타입:', typeof downloadKey);
      console.log('파일명:', fileName);
      
      if (downloadKey) {
        // 여러 다운로드 방식을 순차적으로 시도
        let downloadSuccess = false;
        
        // 방법 1: assignmentMaterialApi의 downloadAssignmentMaterialByKey 사용
        try {
          console.log('🔍 방법 1 시도: downloadAssignmentMaterialByKey');
          console.log('🔍 전달할 파라미터:', { downloadKey, fileName });
          await downloadAssignmentMaterialByKey(downloadKey, fileName);
          console.log('✅ 방법 1 성공: downloadAssignmentMaterialByKey');
          downloadSuccess = true;
        } catch (error1) {
          console.error('❌ 방법 1 실패:', error1);
          console.error('❌ 방법 1 에러 상세:', {
            message: error1.message,
            status: error1.response?.status,
            statusText: error1.response?.statusText,
            data: error1.response?.data
          });
          
          // 방법 2: studentAssignmentMaterialApiView의 downloadAssignmentMaterial 사용
          try {
            console.log('🔍 방법 2 시도: downloadAssignmentMaterial');
            console.log('🔍 전달할 파라미터:', { downloadKey, memberId: currentUser?.memberId });
            const downloadInfo = await downloadAssignmentMaterial(downloadKey, currentUser?.memberId);
            console.log('🔍 downloadInfo 응답:', downloadInfo);
            
            if (downloadInfo && downloadInfo.data && downloadInfo.data.downloadUrl) {
              console.log('🔍 downloadUrl 발견:', downloadInfo.data.downloadUrl);
              await downloadFile(downloadInfo.data.downloadUrl, fileName);
              console.log('✅ 방법 2 성공: downloadAssignmentMaterial');
              downloadSuccess = true;
            } else {
              console.error('❌ downloadUrl이 없습니다. downloadInfo 구조:', downloadInfo);
              throw new Error('downloadUrl이 없습니다.');
            }
          } catch (error2) {
            console.error('❌ 방법 2 실패:', error2);
            console.error('❌ 방법 2 에러 상세:', {
              message: error2.message,
              status: error2.response?.status,
              statusText: error2.response?.statusText,
              data: error2.response?.data
            });
            
            // 방법 3: studentAssignmentMaterialApi의 downloadStudentSubmissionFile 사용
            try {
              console.log('🔍 방법 3 시도: downloadStudentSubmissionFile');
              console.log('🔍 전달할 파라미터:', { downloadKey, fileName });
              await downloadStudentSubmissionFile(downloadKey, fileName);
              console.log('✅ 방법 3 성공: downloadStudentSubmissionFile');
              downloadSuccess = true;
            } catch (error3) {
              console.error('❌ 방법 3 실패:', error3);
              console.error('❌ 방법 3 에러 상세:', {
                message: error3.message,
                status: error3.response?.status,
                statusText: error3.response?.statusText,
                data: error3.response?.data
              });
              throw error3; // 모든 방법이 실패
            }
          }
        }
        
        if (!downloadSuccess) {
          throw new Error('모든 다운로드 방법이 실패했습니다.');
        }
      } else {
        console.error('❌ 다운로드 키가 없습니다.');
        alert('파일 다운로드 기능은 실제 업로드된 파일에서만 사용 가능합니다.');
      }
    } catch (error) {
      console.error('❌ 과제 자료 다운로드 최종 실패:', error);
      console.error('❌ 최종 에러 상세:', {
        message: error.message,
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        stack: error.stack
      });
      
      // 더 구체적인 에러 메시지 제공
      let errorMessage = '파일 다운로드에 실패했습니다.';
      if (error.message.includes('404')) {
        errorMessage = '파일을 찾을 수 없습니다. 파일이 삭제되었거나 경로가 잘못되었을 수 있습니다.';
      } else if (error.message.includes('403')) {
        errorMessage = '파일 다운로드 권한이 없습니다.';
      } else if (error.message.includes('401')) {
        errorMessage = '로그인이 필요합니다.';
      } else if (error.message.includes('네트워크')) {
        errorMessage = '네트워크 오류가 발생했습니다. 백엔드 서버가 실행 중인지 확인해주세요.';
      }
      
      alert(errorMessage);
    }
  }

  // 파일 아이콘 함수
  const getFileIcon = (fileName) => {
    if (!fileName) return <FileText className="w-4 h-4 text-gray-500" />
    
    const extension = fileName.toLowerCase().split('.').pop()
    switch (extension) {
      case 'pdf':
        return <FileText className="w-4 h-4 text-red-500" />
      case 'doc':
      case 'docx':
        return <FileText className="w-4 h-4 text-blue-500" />
      case 'xls':
      case 'xlsx':
        return <FileText className="w-4 h-4 text-green-500" />
      case 'ppt':
      case 'pptx':
        return <FileText className="w-4 h-4 text-orange-500" />
      case 'zip':
      case 'rar':
      case '7z':
        return <FileText className="w-4 h-4 text-purple-500" />
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
      case 'bmp':
        return <FileText className="w-4 h-4 text-pink-500" />
      case 'mp4':
      case 'avi':
      case 'mov':
      case 'wmv':
        return <FileText className="w-4 h-4 text-indigo-500" />
      case 'mp3':
      case 'wav':
      case 'aac':
        return <FileText className="w-4 h-4 text-yellow-500" />
      default:
        return <FileText className="w-4 h-4 text-gray-500" />
    }
  }

  // 파일 크기 포맷 함수
  const formatFileSize = (bytes) => {
    if (!bytes) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  // 학생 제출 파일 선택 핸들러
  const handleSubmissionFileSelect = (event) => {
    const file = event.target.files[0]
    if (file) {
      console.log('선택된 제출 파일 정보:', {
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
      
      // 허용된 파일 타입 체크 (API에서 지원하는 타입)
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
        'image/bmp',
        'application/zip',
        'application/x-rar-compressed',
        'video/mp4',
        'video/avi',
        'video/mov',
        'video/wmv',
        'audio/mpeg',
        'audio/wav',
        'audio/aac'
      ]
      
      if (!allowedTypes.includes(file.type)) {
        alert('지원하지 않는 파일 형식입니다. PDF, Word, Excel, PowerPoint, 이미지, 비디오, 오디오, 압축파일만 업로드 가능합니다.')
        return
      }
      
      // 파일명에 특수문자나 공백이 있는지 확인
      const fileName = file.name
      if (/[<>:"/\\|?*]/.test(fileName)) {
        alert('파일명에 특수문자 (< > : " / \\ | ? *)가 포함되어 있습니다. 파일명을 변경해주세요.')
        return
      }
      
      setSelectedSubmissionFile(file)
    }
  }



  // 학생 제출 파일 업로드
  const handleUploadSubmissionFile = async () => {
    if (!selectedSubmissionFile) {
      alert('파일을 선택해주세요.')
      return
    }

    if (!targetAssignment?.assignmentId) {
      alert('과제 정보가 없습니다.')
      return
    }

    if (!currentUser?.memberId) {
      alert('사용자 정보가 없습니다. 다시 로그인해주세요.')
      return
    }

    setIsUploadingSubmissionFile(true)
    setUploadProgress(0)

    try {
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

      // submissionId가 없어도 파일 업로드 시 assignment_submission_file 테이블에 저장 (submission_id는 NULL)
      let submissionId = targetAssignment.mySubmission?.submissionId

      // Presigned URL 방식으로 파일 업로드
      const uploadOptions = {
        ownerId: currentUser.memberId,
        memberType: 'USER',
        assignmentInfo: {
          assignmentId: targetAssignment.assignmentId,
          courseId: targetAssignment.courseId,
          studentId: currentUser.memberId,
          submissionId: submissionId
        }
      }

      const uploadedFile = await uploadFileWithPresignedUrl(selectedSubmissionFile, uploadOptions)

      clearInterval(progressInterval)
      setUploadProgress(100)

      if (!uploadedFile) {
        throw new Error('파일 업로드에 실패했습니다.')
      }

      // 성공 메시지
      alert('파일이 성공적으로 업로드되었습니다.')
      
      // 업로드된 파일을 즉시 목록에 추가
      const newUploadedFile = {
        id: uploadedFile.fileId || uploadedFile.id || Date.now().toString(),
        fileName: selectedSubmissionFile.name,
        fileSize: selectedSubmissionFile.size,
        uploadDate: new Date().toISOString(),
        isLocalFile: true // 로컬에서 추가된 파일임을 표시
      }
      
      // 기존 파일 목록에 새 파일 추가
      setSubmissionFiles(prevFiles => {
        const updatedFiles = [...prevFiles, newUploadedFile]
        return updatedFiles
      })
      
      // 서버에서 최신 목록 가져오기 (선택사항)
      try {
        const serverFiles = await getStudentSubmissionFilesForDetail(targetAssignment.assignmentId, targetAssignment.courseId, currentUser.memberId)
        
        if (serverFiles && Array.isArray(serverFiles) && serverFiles.length > 0) {
          // 서버 파일과 로컬 파일을 합침
          setSubmissionFiles(prevFiles => {
            const localFiles = prevFiles.filter(file => file.isLocalFile)
            const combinedFiles = [...serverFiles, ...localFiles]
            return combinedFiles
          })
        }
      } catch (error) {
        // 에러가 발생해도 alert을 표시하지 않음
      }
      
      setSelectedSubmissionFile(null)
      setTimeout(() => {
        setUploadProgress(0)
        setIsUploadingSubmissionFile(false)
      }, 500)

    } catch (error) {
      console.error('학생 제출 파일 업로드 오류:', error)
      
      // 더 구체적인 에러 메시지 제공
      let errorMessage = '파일 업로드에 실패했습니다.'
      if (error.message.includes('크기')) {
        errorMessage = '파일이 너무 큽니다. 파일 크기를 줄여주세요.'
      } else if (error.message.includes('권한')) {
        errorMessage = '파일 업로드 권한이 없습니다.'
      } else if (error.message.includes('인증')) {
        errorMessage = '인증이 만료되었습니다. 다시 로그인해주세요.'
      }
      
      alert(errorMessage)
      setIsUploadingSubmissionFile(false)
      setUploadProgress(0)
    }
  }

  // 학생 제출 파일 삭제
  const handleDeleteSubmissionFile = async (fileId, fileName) => {
    if (!window.confirm(`'${fileName}' 파일을 삭제하시겠습니까?`)) {
      return
    }

    try {
      await deleteStudentSubmissionFile(targetAssignment.assignmentId, fileId)
      
      // 제출 파일 목록 새로고침
      try {
        const submissionFilesData = await getStudentSubmissionFilesForDetail(targetAssignment.assignmentId, targetAssignment.courseId, currentUser.memberId)
        setSubmissionFiles(submissionFilesData)
      } catch (error) {
        // 로컬에서 제거
        setSubmissionFiles(prevFiles => prevFiles.filter(file => file.id !== fileId))
      }
      
      alert('파일이 성공적으로 삭제되었습니다.')
    } catch (error) {
      alert('파일 삭제에 실패했습니다.')
    }
  }



  // 드래그 앤 드롭 핸들러
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
      handleSubmissionFileSelect({ target: { files: [files[0]] } })
    }
  }
  // 모달 닫기 핸들러
  const closeAllModals = () => {
    setShowSubmitModal(false)
    setShowEditModal(false)
    setShowDeleteModal(false)
    setTargetAssignment(null)
  }
  const closeEditModal = () => {
    setShowEditModal(false)
    setEditSubmission(null)
    setEditAnswerText("")
    setEditSubmissionType("서술")
  }
  // assignmentsWithStatus 계산 (상태로 분리하지 않고 map으로 계산)
  const assignmentsWithStatus = assignments.map(assignment => {
    const safeSubmissions = Array.isArray(submissions) ? submissions : []
    
    // 제출 데이터 구조 디버깅
    console.log('제출 데이터 확인:', {
      assignmentId: assignment.assignmentId,
      currentUser: currentUser,
      submissions: safeSubmissions,
      firstSubmission: safeSubmissions[0]
    });
    
    // 다양한 필드명으로 mySubmission 찾기
    const mySubmission = safeSubmissions.find(sub => {
      const assignmentMatch = sub.assignmentId === assignment.assignmentId;
      const memberMatch = sub.id === currentUser?.memberId || 
                         sub.memberId === currentUser?.memberId ||
                         sub.studentId === currentUser?.memberId;
      
      console.log('제출 매칭 확인:', {
        assignmentId: sub.assignmentId,
        assignmentMatch,
        memberId: sub.id || sub.memberId || sub.studentId,
        currentUserMemberId: currentUser?.memberId,
        memberMatch
      });
      
      return assignmentMatch && memberMatch;
    });
    
    console.log('찾은 mySubmission:', mySubmission);
    
    const now = new Date();
    const due = new Date(assignment.dueDate);
    let status = "";
    
    if (mySubmission) {
      // 제출 완료된 경우, 제출 시점이 기한을 넘었는지 확인
      const submittedAt = new Date(mySubmission.submittedAt || mySubmission.submittedDate || mySubmission.createdAt);
      if (submittedAt > due) {
        status = "submitted_overdue"; // 제출 완료이지만 기한 초과
      } else {
        status = "submitted"; // 제출 완료
      }
    } else {
      // 미제출인 경우, 현재가 기한을 넘었는지 확인
      if (now > due) {
        status = "overdue"; // 기한 초과 (하지만 제출 가능)
      } else {
        status = "pending"; // 제출 대기
      }
    }
    
    const result = { ...assignment, mySubmission, status };
    // 각 과제별 상태 로그
    console.log('assignmentWithStatus:', result);
    return result;
  });

  // 상태별 필터 및 검색도 assignmentsWithStatus 기준으로
  const filteredAssignments = assignmentsWithStatus.filter((assignment) => {
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      if (!(
        (assignment.assignmentTitle && assignment.assignmentTitle.toLowerCase().includes(term)) ||
        (assignment.courseName && assignment.courseName.toLowerCase().includes(term)) ||
        (assignment.memberId && assignment.memberId.toLowerCase().includes(term))
      )) {
        return false;
      }
    }
    if (statusFilter !== "all" && assignment.status !== statusFilter) return false;
    if (subjectFilter !== "all" && assignment.courseId !== subjectFilter) return false;
    return true;
  });

  // 통계 카드도 assignmentsWithStatus 기준으로
  const totalAssignments = assignmentsWithStatus.length;
  const submittedAssignments = assignmentsWithStatus.filter(a => a.status === "submitted" || a.status === "submitted_overdue").length;
  const pendingAssignments = assignmentsWithStatus.filter(a => a.status === "pending").length;
  const overdueAssignments = assignmentsWithStatus.filter(a => a.status === "overdue").length;
  const submittedOverdueAssignments = assignmentsWithStatus.filter(a => a.status === "submitted_overdue").length;
  const averageScore =
    assignmentsWithStatus.filter((a) => a.mySubmission && a.mySubmission.score !== null).reduce((sum, a) => sum + a.mySubmission.score, 0) /
      assignmentsWithStatus.filter((a) => a.mySubmission && a.mySubmission.score !== null).length || 0;

  const handleFileChange = (e) => {
    setFile(e.target.files[0] || null)
  }

  const handleAssignmentSubmit = async (e) => {
    e.preventDefault();
    try {
      // 필수값 체크
      if (!targetAssignment.assignmentId) {
        alert('과제 ID가 없습니다.');
        return;
      }
      if (!currentUser?.memberId) {
        alert('사용자 정보가 없습니다. 다시 로그인 해주세요.');
        return;
      }
      if (!answerText || answerText.trim() === "") {
        alert('답변 내용을 입력하세요.');
        return;
      }
      if (!submissionType) {
        alert('제출 타입을 선택하세요.');
        return;
      }
      
      // 디버깅을 위한 로그
      console.log('=== 과제 제출 검증 디버깅 ===');
      console.log('targetAssignment:', targetAssignment);
      console.log('fileRequired:', targetAssignment.fileRequired);
      console.log('file_required:', targetAssignment.file_required);
      console.log('codeRequired:', targetAssignment.codeRequired);
      console.log('code_required:', targetAssignment.code_required);
      console.log('submissionType:', submissionType);
      console.log('file:', file);
      console.log('submissionFiles:', submissionFiles);
      console.log('submissionFiles.length:', submissionFiles?.length);
      
      // 파일 제출 필수 검증
      if (targetAssignment.fileRequired === true || targetAssignment.file_required === true) {
        const hasUploadedFiles = (file !== null) || 
          (submissionFiles && submissionFiles.length > 0);
        
        console.log('파일 제출 필수 확인:', {
          fileRequired: targetAssignment.fileRequired,
          file_required: targetAssignment.file_required,
          hasUploadedFiles: hasUploadedFiles,
          file: file !== null,
          submissionFilesCount: submissionFiles?.length || 0
        });
        
        if (!hasUploadedFiles) {
          alert('파일 제출이 필수입니다. 파일을 업로드해주세요.');
          return;
        }
      }
      
      // 코드 제출 필수 검증
      if (targetAssignment.codeRequired === true || targetAssignment.code_required === true) {
        console.log('코드 제출 필수 확인:', {
          codeRequired: targetAssignment.codeRequired,
          code_required: targetAssignment.code_required,
          submissionType: submissionType
        });
        
        if (submissionType !== "코드") {
          alert('코드 제출이 필수입니다. 제출 타입을 "코드"로 선택해주세요.');
          return;
        }
      }
      // 코드 제출일 때 answerText에 HTML+CSS+JS를 모두 합쳐서 저장
      let answerTextToSend = answerText;
      if (submissionType === "코드") {
        answerTextToSend = `\n<html>\n  <head>\n    <style>\n${answerCss}\n    </style>\n  </head>\n  <body>\n${answerText}\n    <script>\n${answerJs}\n    <\/script>\n  </body>\n</html>\n`;
      }
      // 업로드된 모든 파일들을 FormData에 추가
      const formData = new FormData();
      formData.append('assignmentId', targetAssignment.assignmentId);
      formData.append('answerText', answerTextToSend);
      formData.append('submissionType', submissionType);
      
      // 기존 단일 파일이 있으면 추가
      if (file) {
        formData.append('file', file);
      }
      
      // 제출 모달에서 업로드된 파일들 추가
      if (submissionFiles && submissionFiles.length > 0) {
        submissionFiles.forEach((submissionFile, index) => {
          // 로컬 파일인 경우 File 객체가 없으므로 건너뛰기
          if (submissionFile.isLocalFile) {
            console.log('로컬 파일은 제출에서 제외:', submissionFile.fileName);
            return;
          }
          // 실제 파일 객체가 있는 경우만 추가
          if (submissionFile.file) {
            formData.append(`submissionFiles`, submissionFile.file);
          }
        });
      }
      

      const result = await createStudentAssignmentSubmission(formData);
      
      if (result && typeof result.resultCode === 'string' && result.resultCode.trim().toUpperCase() === "SUCCESS") {
        // 제출 성공 후 업로드된 파일들을 assignment_submission_file 테이블에 연결
        const submissionId = result.data?.submissionId || result.submissionId || result.data?.id || result.id;
        
        if (submissionId && submissionFiles && submissionFiles.length > 0) {
          // 업로드된 파일들의 submission_id를 업데이트
          try {
            // 업데이트할 파일들의 ID 목록
            const fileIdsToUpdate = submissionFiles
              .filter(file => !file.isLocalFile)
              .map(file => file.id || file.fileId);
            
            if (fileIdsToUpdate.length > 0) {
              await updateSubmissionFileIds(fileIdsToUpdate, submissionId);
            }
          } catch (error) {
            // 업데이트 실패해도 제출은 성공으로 처리
          }
        }
        
        alert("과제가 성공적으로 제출되었습니다!\n" + (result.resultMessage || ""));
        await fetchAll(); // 반드시 await로 목록 재조회 (상태 최신화)
        setFile(null);
        closeAllModals();
      } else {
        alert("제출 실패: " + (result?.resultMessage || "오류가 발생했습니다."));
      }
    } catch (err) {
      alert("과제 제출에 실패했습니다: " + err.message);
    }
  };

  const handleEditFileChange = (e) => {
    setEditFile(e.target.files[0] || null)
  }

  const handleEditSubmit = async (e) => {
    e.preventDefault()
    try {
      const formData = new FormData();
      formData.append("answerText", editAnswerText);
      formData.append("submissionType", editSubmissionType);
      
      // 코드 타입인 경우 CSS와 JS 내용도 추가
      if (editSubmissionType === "코드") {
        formData.append("answerCss", editAnswerCss);
        formData.append("answerJs", editAnswerJs);
      }
      
      if (editFile) {
        formData.append("file", editFile);
      }
      const response = await fetch(`/api/student/assignmentsubmission/${editSubmission.submissionId}`, {
        method: "PUT",
        body: formData,
      });
      if (!response.ok) {
        // 네트워크 에러(400, 500 등) 처리
        const errorText = await response.text();
        alert("수정 실패(네트워크): " + response.status + " " + errorText);
        return;
      }
      const result = await response.json();
      if (result.resultCode === "SUCCESS") {
        alert("수정 완료!\n" + (result.resultMessage || ""));
        await fetchAll(); // 수정 후 상태 최신화
        setEditFile(null);
        closeEditModal();
        setSelectedAssignment(null); // 상세 모달 닫기
        setIsDetailModalOpen(false);
      } else {
        alert("수정 실패: " + (result.resultMessage || "오류가 발생했습니다."));
      }
    } catch (err) {
      alert("수정 실패(예외): " + err.message);
    }
  }

  const handleDeleteSubmission = async (submission) => {
    if (!window.confirm("정말 삭제하시겠습니까?")) return
    try {
      await deleteStudentAssignmentSubmission(submission.submissionId); // 실제 삭제 API 호출
      alert("삭제 완료!")
      closeEditModal()
      setSelectedAssignment(null); // 상세 모달 닫기
      setIsDetailModalOpen(false);
      await fetchAll(); // 상태 최신화
    } catch (err) {
      alert("삭제 실패: " + err.message)
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header currentPage="my-assignment" userRole="student" userName="김학생" />
        <div className="flex items-center justify-center h-96">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">과제 목록을 불러오는 중...</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header currentPage="my-assignment" userRole="student" userName="김학생" />
      <div className="flex">
        <Sidebar title="과제" menuItems={sidebarMenuItems} currentPath="/student/assignments" />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 min-w-7xl lg:px-8 py-8">
        {/* 페이지 헤더 */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">과제</h1>
          <p className="text-gray-600 mt-2">수강중인 강의의 과제를 확인하고 제출하세요.</p>
        </div>

        {/* 상단 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <Card>
                <CardContent className="p-6 flex items-center justify-between">
                  <div>
                    <p className="text-base font-medium text-gray-600">전체 과제</p>
                    <p className="text-3xl font-bold text-[#3498db]">{totalAssignments}개</p>
                  </div>
                  <div className="bg-[#EFF6FF] rounded-full p-3">
                    <FileText className="w-10 h-10 text-[#3498db]" />
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-6 flex items-center justify-between">
                  <div>
                    <p className="text-base font-medium text-gray-600">제출완료</p>
                    <p className="text-3xl font-bold text-[#1abc9c]">{submittedAssignments}개</p>
                  </div>
                  <div className="bg-[#e4f5eb] rounded-full p-3">
                    <CheckCircle className="w-10 h-10 text-[#1abc9c]" />
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-6 flex items-center justify-between">
                  <div>
                    <p className="text-base font-medium text-gray-600">제출대기</p>
                    <p className="text-3xl font-bold text-[#b0c4de]">{pendingAssignments}개</p>
                  </div>
                  <div className="bg-[#eff6ff] rounded-full p-3">
                    <Clock className="w-10 h-10 text-[#b0c4de]" />
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-6 flex items-center justify-between">
                  <div>
                    <p className="text-base font-medium text-gray-600">기한초과 제출</p>
                    <p className="text-3xl font-bold text-[#e74c3c]">{submittedOverdueAssignments}개</p>
                  </div>
                  <div className="bg-red-100 rounded-full p-3">
                    <CheckCircle className="w-10 h-10 text-[#e74c3c]" />
                  </div>
                </CardContent>
              </Card>
            </div>

        {/* 검색/필터 바 */}
        <div className="bg-white p-4 rounded-lg shadow-sm border mb-6">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <input
                type="text"
                placeholder="과제명, 과목명, 강사명으로 검색..."
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <select
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">모든 상태</option>
              <option value="pending">제출대기</option>
              <option value="submitted">제출완료</option>
              <option value="overdue">기한초과</option>
            </select>
            <select
              value={subjectFilter}
              onChange={e => setSubjectFilter(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">모든 과목</option>
              {getUniqueSubjects().map((subject, idx) => (
                <option key={subject.courseId || idx} value={subject.courseId}>{subject.courseName}</option>
              ))}
            </select>
          </div>
        </div>

        {/* 과제 리스트 */}
        <div className="space-y-6">
          {filteredAssignments.length === 0 ? (
            <div className="bg-white p-12 rounded-lg shadow-sm border text-center">
              <FileText className="w-16 h-16 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">과제가 없습니다</h3>
              <p className="text-gray-600">검색 조건에 맞는 과제를 찾을 수 없습니다.</p>
            </div>
          ) : (
            filteredAssignments.map((assignment) => {
              const isSubmitted = assignment.status === "submitted" || assignment.status === "submitted_overdue";
              const isSubmittedOverdue = assignment.status === "submitted_overdue";
              const isOverdue = assignment.status === "overdue";
              const isPending = assignment.status === "pending";
              return (
                <div key={assignment.assignmentId} className="bg-white rounded-lg shadow-sm border p-6">
                  <div className="flex justify-between items-center mb-2">
                    <div>
                      <h2 className="text-lg font-bold inline-block align-middle">{assignment.assignmentTitle}</h2>
                      {isSubmitted && !isSubmittedOverdue && (
                        <span className="inline-flex items-center px-2 py-1 ml-2 rounded-full text-xs font-medium bg-green-100 text-green-800 align-middle">제출완료</span>
                      )}
                      {isSubmittedOverdue && (
                        <span className="inline-flex items-center px-2 py-1 ml-2 rounded-full text-xs font-medium bg-orange-100 text-orange-800 align-middle">제출완료(기한초과)</span>
                      )}
                      {isOverdue && (
                        <span className="inline-flex items-center px-2 py-1 ml-2 rounded-full text-xs font-medium bg-red-100 text-red-800 align-middle">기한초과</span>
                      )}
                      {isPending && (
                        <span className="inline-flex items-center px-2 py-1 ml-2 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800 align-middle">제출대기</span>
                      )}
                    </div>
                    <div>
                      {isSubmitted && (
                        <Button className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white ml-2" onClick={() => handleViewAssignmentDetail(assignment)}>
                          상세보기
                        </Button>
                      )}
                      {(isPending || isOverdue) && (
                        <Button className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white ml-2" onClick={() => openSubmitModal(assignment)}>
                          과제 제출
                        </Button>
                      )}
                    </div>
                  </div>
                  <div className="text-gray-600 mb-2">
                    <span className="font-medium">과목:</span> {assignment.courseName || `과정 ${assignment.courseId?.substring(0, 8)}...`}
                  </div>
                  <div className="text-gray-700 mb-2">{assignment.assignmentContent}</div>
                  <div className="flex flex-wrap gap-2 mb-2">
                    {(assignment.attachments || []).map((file, i) => (
                      <span key={i} className="inline-flex items-center bg-gray-100 px-2 py-1 rounded text-sm">
                        <FileText className="w-4 h-4 mr-1" /> {file}
                        <button className="ml-2 text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white px-2 py-1 rounded">다운로드</button>
                      </span>
                    ))}
                  </div>
                  <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500 mb-2">
                    <span>마감: {assignment.dueDate}</span>
                    {assignment.mySubmission && assignment.mySubmission.submittedAt && <span>제출: {assignment.mySubmission.submittedAt}</span>}
                    {assignment.mySubmission && assignment.mySubmission.score !== null && <span>점수: {assignment.mySubmission.score}/{assignment.maxScore}</span>}
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* 상세 모달 */}
        {isDetailModalOpen && selectedAssignment && (
          <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center z-50">
            <div className="bg-white rounded-lg max-w-2xl w-full p-8 shadow-lg relative overflow-y-auto max-h-[90vh]">
              <button onClick={closeDetailModal} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600">
                <XCircle className="w-6 h-6" />
              </button>
              <h2 className="text-2xl font-bold mb-4">과제 상세정보</h2>
              
              {/* 디버깅 로그 */}
              {console.log('과제 상세 모달 데이터:', {
                selectedAssignment,
                courseName: selectedAssignment.courseName,
                fileRequired: selectedAssignment.fileRequired,
                codeRequired: selectedAssignment.codeRequired,
                rubric: selectedAssignment.rubric,
                rubricitem: selectedAssignment.rubricitem
              })}
              
              {/* assignment 테이블 정보 */}
              <div className="bg-gray-50 rounded-lg p-6 mb-6">
                <div className="flex flex-wrap justify-between items-center mb-2">
                  <div className="text-xl font-bold">{selectedAssignment.assignmentTitle}</div>
                  <div className="flex gap-2">
                    {selectedAssignment.status === 'submitted' && (
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">제출완료</span>
                    )}
                    {selectedAssignment.status === 'submitted_overdue' && (
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-orange-100 text-orange-800">제출완료(기한초과)</span>
                    )}
                    {selectedAssignment.status === 'pending' && (
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">제출대기</span>
                    )}
                    {selectedAssignment.status === 'overdue' && (
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">기한초과</span>
                    )}
                  </div>
                </div>
                <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-2">
                  <span>과목: {selectedAssignment.courseName || `과정 ${selectedAssignment.courseId?.substring(0, 8)}...`}</span>
                  <span>마감일: {selectedAssignment.dueDate}</span>
                  <span>배점: {selectedAssignment.maxScore || selectedAssignment.assignmentAcc || 0}점</span>
                </div>
                
                {/* 제출 필수 요건 표시 */}
                <div className="flex flex-wrap gap-2 mb-3">
                  {selectedAssignment.fileRequired === true || selectedAssignment.file_required === true ? (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      파일 제출 필수
                    </span>
                  ) : null}
                  {selectedAssignment.codeRequired === true || selectedAssignment.code_required === true ? (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                      코드 제출 필수
                    </span>
                  ) : null}
                  {(!selectedAssignment.fileRequired && !selectedAssignment.file_required && 
                    !selectedAssignment.codeRequired && !selectedAssignment.code_required) && (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
                      제출 요건: 없음
                    </span>
                  )}
                </div>
                
                <div className="text-sm text-gray-600 mb-2">설명: {selectedAssignment.assignmentContent}</div>
                
                {/* 루브릭 정보 표시 */}
                <div className="mt-4">
                  <h4 className="text-base font-semibold mb-2">루브릭 채점 기준</h4>
                  {(Array.isArray(selectedAssignment.rubric) && selectedAssignment.rubric.length > 0) || 
                   (Array.isArray(selectedAssignment.rubricitem) && selectedAssignment.rubricitem.length > 0) ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {(selectedAssignment.rubricitem || selectedAssignment.rubric || []).map((item, idx) => (
                        <div key={idx} className="bg-white border rounded-lg p-4">
                          <div className="font-medium text-gray-900 mb-2">{item.title || item.name || item.itemTitle || `항목${idx+1}`}</div>
                          <div className="text-sm text-gray-600 mb-1">{item.description || '-'}</div>
                          <div className="text-sm font-medium text-blue-600">배점: {item.score || item.point || item.maxScore || 0}점</div>
                          <div className="text-xs text-gray-500">항목 순서: {item.itemOrder || idx + 1}</div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="bg-gray-50 border rounded-lg p-4">
                      <span className="text-gray-500">루브릭 채점 기준: 없음</span>
                    </div>
                  )}
                </div>
                
                {selectedAssignment.mySubmission && selectedAssignment.mySubmission.score !== null && (
                  <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg">
                    <div className="text-sm text-gray-600 mb-2">
                      점수: <span className={selectedAssignment.mySubmission.score >= 80 ? 'text-green-600' : selectedAssignment.mySubmission.score >= 60 ? 'text-yellow-600' : 'text-red-600'}>{selectedAssignment.mySubmission.score}/{selectedAssignment.maxScore}</span>
                    </div>
                    {selectedAssignment.mySubmission.feedback && (
                      <div className="text-sm text-gray-600">
                        <div className="font-medium mb-1">피드백:</div>
                        <div className="bg-white p-2 rounded border text-gray-700">
                          {selectedAssignment.mySubmission.feedback}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
              
              {/* 내가 제출한 과제 내용 표시 */}
              {selectedAssignment.mySubmission && (
                <div className="mb-6">
                  <h4 className="text-lg font-semibold text-gray-900 mb-3">내 제출 내용</h4>
                  <div className="bg-gray-100  rounded-lg p-4">
                    <div className="mb-2"><b>제출 타입:</b> {selectedAssignment.mySubmission.submissionType || <span className="text-gray-400">(없음)</span>}</div>
                    
                    {/* 코드 제출인 경우 드롭다운 추가 */}
                    {selectedAssignment.mySubmission.submissionType === "코드" && (
                      <div className="mb-3">
                        <label className="block text-sm font-medium text-gray-700 mb-2">보기 모드:</label>
                        <select
                          value={codeViewMode}
                          onChange={(e) => setCodeViewMode(e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        >
                          <option value="서술">서술로 보기</option>
                          <option value="코드">코드로 보기</option>
                        </select>
                      </div>
                    )}
                    
                    {/* 답변 내용 표시 */}
                    <div className="mb-2">
                      <b>답변 내용:</b>
                      {selectedAssignment.mySubmission.submissionType === "코드" && codeViewMode === "코드" ? (
                        <div className="mt-2 space-y-3">
                          {/* HTML 코드 */}
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">HTML:</label>
                            <div className="bg-gray-900 text-green-400 p-3 rounded-md font-mono text-sm overflow-x-auto">
                              <pre>{parsedCodeData.html || <span className="text-gray-400">(HTML 코드 없음)</span>}</pre>
                            </div>
                          </div>
                          
                          {/* CSS 코드 */}
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">CSS:</label>
                            <div className="bg-gray-900 text-blue-400 p-3 rounded-md font-mono text-sm overflow-x-auto">
                              <pre>{parsedCodeData.css || <span className="text-gray-400">(CSS 코드 없음)</span>}</pre>
                            </div>
                          </div>
                          
                          {/* JavaScript 코드 */}
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">JavaScript:</label>
                            <div className="bg-gray-900 text-yellow-400 p-3 rounded-md font-mono text-sm overflow-x-auto">
                              <pre>{parsedCodeData.js || <span className="text-gray-400">(JavaScript 코드 없음)</span>}</pre>
                            </div>
                          </div>
                          
                          {/* 코드 미리보기 */}
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">미리보기:</label>
                            <div className="border border-gray-300 rounded-md overflow-hidden">
                              <iframe
                                srcDoc={`<html><head><style>${parsedCodeData.css}</style></head><body>${parsedCodeData.html}<script>${parsedCodeData.js}</script></body></html>`}
                                className="w-full h-64 border-0"
                                title="코드 미리보기"
                              />
                            </div>
                          </div>
                        </div>
                      ) : (
                        <div className="mt-2 bg-white p-3 rounded border text-gray-700 whitespace-pre-wrap">
                          {selectedAssignment.mySubmission.answerText || <span className="text-gray-400">(없음)</span>}
                        </div>
                      )}
                    </div>
                    
                    {selectedAssignment.mySubmission.fileName && (
                      <div className="mb-2"><b>제출 파일:</b> {selectedAssignment.mySubmission.fileName}</div>
                    )}
                    {selectedAssignment.mySubmission.submittedAt && (
                      <div className="mb-2"><b>제출일:</b> {selectedAssignment.mySubmission.submittedAt}</div>
                    )}
                    <div className="flex gap-2 mt-4">
                      <Button className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white" onClick={() => openEditModal(selectedAssignment.mySubmission)}>수정</Button>
                      <Button className="text-[#e74c3c] border border-[#e74c3c] hover:bg-[#e74c3c] hover:text-white" onClick={() => handleDeleteSubmission(selectedAssignment.mySubmission)}>삭제</Button>
                    </div>
                  </div>
                </div>
              )}
              

              

              


              {/* 제출된 파일 목록 표시 */}
              <div className="mb-6">
                <h4 className="text-lg font-semibold text-gray-900 mb-3">제출한 파일</h4>
                <div className="bg-gray-100 rounded-lg p-4">
                  {submissionDetailFiles && submissionDetailFiles.length > 0 ? (
                    <div className="space-y-3">
                      {submissionDetailFiles.map((file, index) => (
                        <div key={index} className="flex items-center justify-between p-3 bg-white rounded-lg border">
                          <div className="flex items-center">
                            {getFileIcon(file.fileName || file.name)}
                            <div className="ml-3">
                              <div className="font-medium text-gray-900">
                                {file.fileName || file.name || `파일 ${index + 1}`}
                              </div>
                              <div className="text-sm text-gray-500">
                                {file.fileSize ? formatFileSize(file.fileSize) : ''}
                                {file.uploadDate && ` • ${new Date(file.uploadDate).toLocaleDateString()}`}
                              </div>
                            </div>
                          </div>
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={async () => {
                                try {
                                  const fileKey = file.fileKey || file.id || file.fileId
                                  const fileName = file.fileName || file.name || 'download'
                                  if (fileKey) {
                                    const response = await fetch('/api/v2/file/download', {
                                      method: 'POST',
                                      headers: {
                                        'Content-Type': 'application/json'
                                      },
                                      credentials: 'include',
                                      body: JSON.stringify({
                                        key: fileKey,
                                        name: fileName
                                      })
                                    });
                                    
                                    if (response.ok) {
                                      const blob = await response.blob();
                                      const url = window.URL.createObjectURL(blob);
                                      const link = document.createElement('a');
                                      link.href = url;
                                      link.download = fileName;
                                      document.body.appendChild(link);
                                      link.click();
                                      document.body.removeChild(link);
                                      window.URL.revokeObjectURL(url);
                                    } else {
                                      alert('파일 다운로드에 실패했습니다.')
                                    }
                                  } else {
                                    alert('다운로드할 수 없습니다.')
                                  }
                                } catch (error) {
                                  alert('파일 다운로드에 실패했습니다.')
                                }
                              }}
                              className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                            >
                              <Download className="w-4 h-4" />
                            </Button>

                          </div>
                        </div>
                      ))}
                    </div>
                  ) : selectedAssignment.submissionFile ? (
                    // 기존 단일 파일 표시 (하위 호환성)
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <CheckCircle className="w-5 h-5 text-green-600 mr-2" />
                        <span className="text-green-900 font-medium">{selectedAssignment.submissionFile}</span>
                      </div>
                      <span className="text-sm text-green-600">제출완료</span>
                    </div>
                  ) : (
                    // 제출된 파일이 없는 경우
                    <div className="flex items-center justify-center py-8">
                      <div className="text-center">
                        <FileText className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                        <p className="text-gray-500 text-sm">제출된 자료가 없습니다</p>
                        <p className="text-gray-400 text-xs mt-1">과제 제출 시 업로드한 파일들이 여기에 표시됩니다</p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
              
              <div className="flex justify-end mt-6">
                <Button onClick={closeDetailModal} className="px-6 text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white">닫기</Button>
              </div>
            </div>
          </div>
        )}

        {/* 과제 제출 모달 */}
        {showSubmitModal && targetAssignment && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-lg max-w-2xl w-full p-8 shadow-lg relative overflow-y-auto max-h-[90vh]">
              <button onClick={closeAllModals} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600">
                <XCircle className="w-6 h-6" />
              </button>
              <h2 className="text-2xl font-bold mb-4">과제 제출</h2>
              {/* assignment 테이블 정보 */}
              <div className="bg-gray-50 rounded-lg p-6 mb-6">
                <div className="font-bold text-lg mb-2">{targetAssignment.assignmentTitle}</div>
                <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-2">
                  <span>과목: {targetAssignment.courseName || `과정 ${targetAssignment.courseId?.substring(0, 8)}...`}</span>
                  <span>마감일: {targetAssignment.dueDate}</span>
                  <span>배점: {targetAssignment.maxScore || targetAssignment.assignmentAcc || 0}점</span>
                </div>
                
                {/* 제출 필수 요건 표시 */}
                <div className="flex flex-wrap gap-2 mb-3">
                  {targetAssignment.fileRequired === true || targetAssignment.file_required === true ? (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      파일 제출 필수
                    </span>
                  ) : null}
                  {targetAssignment.codeRequired === true || targetAssignment.code_required === true ? (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                      코드 제출 필수
                    </span>
                  ) : null}
                  {(!targetAssignment.fileRequired && !targetAssignment.file_required && 
                    !targetAssignment.codeRequired && !targetAssignment.code_required) && (
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
                      제출 요건: 없음
                    </span>
                  )}
                </div>
                
                <div className="text-sm text-gray-600 mb-2">설명: {targetAssignment.assignmentContent}</div>
                
                {/* 루브릭 정보 표시 */}
                <div className="mt-4">
                  <h4 className="text-base font-semibold mb-2">루브릭 채점 기준</h4>
                  {(Array.isArray(targetAssignment.rubric) && targetAssignment.rubric.length > 0) || 
                   (Array.isArray(targetAssignment.rubricitem) && targetAssignment.rubricitem.length > 0) ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {(targetAssignment.rubricitem || targetAssignment.rubric || []).map((item, idx) => (
                        <div key={idx} className="bg-white border rounded-lg p-4">
                          <div className="font-medium text-gray-900 mb-2">{item.title || item.name || item.itemTitle || `항목${idx+1}`}</div>
                          <div className="text-sm text-gray-600 mb-1">{item.description || '-'}</div>
                          <div className="text-sm font-medium text-blue-600">배점: {item.score || item.point || item.maxScore || 0}점</div>
                          <div className="text-xs text-gray-500">항목 순서: {item.itemOrder || idx + 1}</div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="bg-gray-50 border rounded-lg p-4">
                      <span className="text-gray-500">루브릭 채점 기준: 없음</span>
                    </div>
                  )}
                </div>

                {/* 과제 자료 표시 */}
                <div className="mt-4">
                  <h4 className="text-base font-semibold mb-2">과제 자료</h4>
                  {assignmentMaterials && assignmentMaterials.length > 0 ? (
                    <div className="space-y-2">
                      {assignmentMaterials.map((material, index) => (
                        <div key={material.id || material.fileId || `material-${index}`} className="flex items-center justify-between p-3 bg-white border rounded-lg">
                          <div className="flex items-center space-x-3">
                            <div className="p-2 bg-blue-100 rounded-lg">
                              {getFileIcon(material?.fileName || material?.name)}
                            </div>
                            <div>
                              <p className="font-medium text-gray-900">
                                {material?.title || material?.fileName || material?.name || 'Unknown File'}
                              </p>
                              <p className="text-sm text-gray-500">
                                {material?.fileSize ? formatFileSize(material.fileSize) : (material?.size || '0 Bytes')} • 
                                {material?.uploadDate || '날짜 없음'}
                              </p>
                            </div>
                          </div>
                          <Button
                            type="button"
                            size="sm"
                            onClick={() => handleDownloadAssignmentMaterial(material)}
                            className="flex items-center space-x-1 text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                          >
                            <Download className="w-4 h-4" />
                            <span>다운로드</span>
                          </Button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="bg-gray-50 border rounded-lg p-4">
                      <div className="flex items-center justify-center">
                        <FileText className="w-6 h-6 text-gray-400 mr-2" />
                        <span className="text-gray-500">업로드된 과제 자료가 없습니다.</span>
                      </div>
                    </div>
                  )}
                </div>

                {/* 내 제출 파일 관리 */}
                <div className="mt-4">
                  <h4 className="text-base font-semibold mb-2">내 제출 파일</h4>
                  
                  {/* 파일 업로드 영역 */}
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
                          onChange={handleSubmissionFileSelect}
                          accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.jpg,.jpeg,.png,.gif,.zip,.rar"
                          className="hidden"
                          id="submission-file-upload"
                          disabled={isUploadingSubmissionFile}
                        />
                        <label htmlFor="submission-file-upload" className="cursor-pointer">
                          <div className="flex flex-col items-center">
                            <Upload className="w-12 h-12 text-gray-400 mb-2" />
                            <p className="text-lg font-medium text-gray-700 mb-1">
                              {selectedSubmissionFile ? selectedSubmissionFile.name : '제출할 파일을 선택하거나 여기에 드래그하세요'}
                            </p>
                                                  <p className="text-sm text-gray-500">
                        PDF, Word, Excel, PowerPoint, 이미지, 비디오, 오디오, 압축파일 (최대 10MB)
                      </p>
                          </div>
                        </label>
                      </div>

                      {/* 선택된 파일 정보 */}
                      {selectedSubmissionFile && (
                        <div className="bg-white p-3 rounded-lg border">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center">
                              <FileText className="w-5 h-5 text-blue-500 mr-2" />
                              <div>
                                <p className="font-medium text-gray-900">{selectedSubmissionFile.name}</p>
                                <p className="text-sm text-gray-500">
                                  {formatFileSize(selectedSubmissionFile.size)} • {selectedSubmissionFile.type}
                                </p>
                              </div>
                            </div>
                            <Button
                              onClick={() => setSelectedSubmissionFile(null)}
                              variant="outline"
                              size="sm"
                              className="text-red-600 hover:text-red-700"
                            >
                              <XCircle className="w-4 h-4" />
                            </Button>
                          </div>
                        </div>
                      )}

                      {/* 업로드 진행률 */}
                      {isUploadingSubmissionFile && (
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

                      {/* 업로드 버튼 */}
                      <div className="flex gap-2">
                        <Button 
                          onClick={handleUploadSubmissionFile} 
                          size="sm"
                          disabled={!selectedSubmissionFile || isUploadingSubmissionFile}
                          className="bg-[#1abc9c] text-white hover:bg-[rgb(10,150,120)]"
                        >
                          {isUploadingSubmissionFile ? (
                            <>
                              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
                              업로드 중...
                            </>
                          ) : (
                            <>
                              <Upload className="w-4 h-4 mr-2" />
                              파일 업로드
                            </>
                          )}
                        </Button>
                      </div>
                    </div>
                  </div>

                  {/* 업로드된 제출 파일 목록 */}
                  <div className="space-y-2">
                    {submissionFiles.length === 0 ? (
                      <div className="bg-gray-50 border rounded-lg p-4 text-center">
                        <FileText className="w-8 h-8 mx-auto mb-2 text-gray-300" />
                        <p className="text-gray-500">업로드된 제출 파일이 없습니다.</p>
                      </div>
                    ) : (
                      submissionFiles.map((file, index) => (
                        <div key={file.id || file.fileId || `submission-file-${index}`} className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg">
                          <div className="flex items-center space-x-3">
                            <div className="p-2 bg-green-100 rounded-lg">
                              {getFileIcon(file?.fileName || file?.name)}
                            </div>
                            <div>
                              <p className="font-medium text-gray-900">
                                {file?.title || file?.fileName || file?.name || 'Unknown File'}
                              </p>
                              <p className="text-sm text-gray-500">
                                {file?.fileSize ? formatFileSize(file.fileSize) : (file?.size || '0 Bytes')} • 
                                {file?.uploadDate || '업로드됨'}
                              </p>
                            </div>
                          </div>
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              onClick={async () => {
                                try {
                                  const downloadKey = file?.materialId || file?.fileKey || file?.fileId || file?.id
                                  const fileName = file?.fileName || file?.name || 'download'
                                  if (downloadKey) {
                                    await downloadStudentSubmissionFile(downloadKey, fileName)
                                    console.log('제출 파일 다운로드 완료:', fileName)
                                  } else {
                                    alert('다운로드할 수 없습니다.')
                                  }
                                } catch (error) {
                                  console.error('제출 파일 다운로드 실패:', error)
                                  alert('파일 다운로드에 실패했습니다.')
                                }
                              }}
                              className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                            >
                              <Download className="w-4 h-4" />
                            </Button>

                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
              {/* assignmentsubmission 테이블 컬럼에 맞는 제출 폼 */}
              <form onSubmit={handleAssignmentSubmit}>
                {submissionType === "코드" ? (
                  <>
                    <div>
                      <label className="block text-sm font-medium mb-1">HTML</label>
                      <MonacoEditor
                        height="200px"
                        language="html"
                        theme="vs-dark"
                        value={answerText}
                        onChange={setAnswerText}
                        options={{
                          fontSize: 14,
                          minimap: { enabled: false },
                          suggestOnTriggerCharacters: true,
                          quickSuggestions: true,
                          wordBasedSuggestions: true,
                          tabCompletion: "on",
                        }}
                      />
                    </div>
                    <div className="mt-2">
                      <label className="block text-sm font-medium mb-1">CSS</label>
                      <MonacoEditor
                        height="200px"
                        language="css"
                        theme="vs-dark"
                        value={answerCss}
                        onChange={setAnswerCss}
                        options={{
                          fontSize: 14,
                          minimap: { enabled: false },
                          suggestOnTriggerCharacters: true,
                          quickSuggestions: true,
                          wordBasedSuggestions: true,
                          tabCompletion: "on",
                        }}
                      />
                    </div>
                    <div className="mt-2">
                      <label className="block text-sm font-medium mb-1">JavaScript</label>
                      <MonacoEditor
                        height="200px"
                        language="javascript"
                        theme="vs-dark"
                        value={answerJs}
                        onChange={setAnswerJs}
                        options={{
                          fontSize: 14,
                          minimap: { enabled: false },
                          suggestOnTriggerCharacters: true,
                          quickSuggestions: true,
                          wordBasedSuggestions: true,
                          tabCompletion: "on",
                        }}
                      />
                    </div>
                    <div className="mb-2 mt-2">
                      <label className="block text-xs font-medium mb-1">코드 실행 미리보기 (iframe)</label>
                      <div className="border rounded overflow-hidden" style={{height: 200}}>
                        <iframe
                          title="code-preview"
                          style={{ width: "100%", height: "100%", border: 0 }}
                          srcDoc={`<html><head><style>${answerCss}</style></head><body>${answerText}<script>${answerJs}\/script></body></html>`}
                        />
                      </div>
                    </div>
                  </>
                ) : (
                  <div>
                    <label className="block text-sm font-medium mb-1">답변 내용(answerText)</label>
                    <textarea className="w-full border rounded p-2" rows={5} value={answerText} onChange={e => setAnswerText(e.target.value)} />
                  </div>
                )}
                <div>
                  <label className="block text-sm font-medium mb-1">제출 타입(submissionType)</label>
                  <select className="w-full border rounded p-2" value={submissionType} onChange={e => setSubmissionType(e.target.value)}>
                    <option value="서술">서술</option>
                    <option value="코드">코드</option>
                  </select>
                </div>

                <div className="flex justify-end gap-2 mt-6">
                  <Button onClick={closeAllModals} className="hover:bg-gray-200 border">취소</Button>
                  <Button type="submit" className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white">제출</Button>
                </div>
              </form>
            </div>
          </div>
        )}
        {/* 수정 모달 */}
        {showEditModal && editSubmission && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-lg max-w-2xl w-full p-8 shadow-lg relative overflow-y-auto max-h-[90vh]">
              <button onClick={closeEditModal} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600">
                <XCircle className="w-6 h-6" />
              </button>
              <h2 className="text-2xl font-bold mb-4">과제 제출 수정</h2>
              <div className="bg-gray-50 rounded-lg p-6 mb-6">
                <div className="font-bold text-lg mb-2">{editSubmission.assignmentTitle}</div>
                <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-2">
                  <span>과목ID: {editSubmission.courseId}</span>
                  <span>작성자ID: {editSubmission.memberId}</span>
                  <span>제출일: {editSubmission.submittedAt}</span>
                </div>
              </div>
              <form className="space-y-4" onSubmit={handleEditSubmit}>
                {editSubmissionType === "코드" ? (
                  <>
                    <div>
                      <label className="block text-sm font-medium mb-1">HTML</label>
                      <MonacoEditor
                        height="200px"
                        language="html"
                        theme="vs-dark"
                        value={editAnswerText}
                        onChange={setEditAnswerText}
                        options={{
                          fontSize: 14,
                          minimap: { enabled: false },
                          suggestOnTriggerCharacters: true,
                          quickSuggestions: true,
                          wordBasedSuggestions: true,
                          tabCompletion: "on",
                        }}
                      />
                    </div>
                    <div className="mt-2">
                      <label className="block text-sm font-medium mb-1">CSS</label>
                      <MonacoEditor
                        height="200px"
                        language="css"
                        theme="vs-dark"
                        value={editAnswerCss}
                        onChange={setEditAnswerCss}
                        options={{
                          fontSize: 14,
                          minimap: { enabled: false },
                          suggestOnTriggerCharacters: true,
                          quickSuggestions: true,
                          wordBasedSuggestions: true,
                          tabCompletion: "on",
                        }}
                      />
                    </div>
                    <div className="mt-2">
                      <label className="block text-sm font-medium mb-1">JavaScript</label>
                      <MonacoEditor
                        height="200px"
                        language="javascript"
                        theme="vs-dark"
                        value={editAnswerJs}
                        onChange={setEditAnswerJs}
                        options={{
                          fontSize: 14,
                          minimap: { enabled: false },
                          suggestOnTriggerCharacters: true,
                          quickSuggestions: true,
                          wordBasedSuggestions: true,
                          tabCompletion: "on",
                        }}
                      />
                    </div>
                    <div className="mb-2 mt-2">
                      <label className="block text-xs font-medium mb-1">코드 실행 미리보기 (iframe)</label>
                      <div className="border rounded overflow-hidden" style={{height: 200}}>
                        <iframe
                          title="code-preview"
                          style={{ width: "100%", height: "100%", border: 0 }}
                          srcDoc={`<html><head><style>${editAnswerCss}</style></head><body>${editAnswerText}<script>${editAnswerJs}\/script></body></html>`}
                        />
                      </div>
                    </div>
                  </>
                ) : (
                  <div>
                    <label className="block text-sm font-medium mb-1">답변 내용(answerText)</label>
                    <textarea className="w-full border rounded p-2" rows={3} value={editAnswerText} onChange={e => setEditAnswerText(e.target.value)} />
                  </div>
                )}
                <div>
                  <label className="block text-sm font-medium mb-1">제출 타입(submissionType)</label>
                  <select className="w-full border rounded p-2" value={editSubmissionType} onChange={e => setEditSubmissionType(e.target.value)}>
                    <option value="서술">서술</option>
                    <option value="코드">코드</option>
                  </select>
                </div>
                <div className="flex justify-end gap-2 mt-6">
                  <Button onClick={closeEditModal} className="hover:bg-gray-200 border">취소</Button>
                  <Button type="submit" className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white">수정</Button>
                </div>
              </form>
            </div>
          </div>
        )}
        {/* 제출 취소 모달 */}
        {showDeleteModal && targetAssignment && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-lg max-w-md w-full p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold">제출 취소</h2>
                <button onClick={closeAllModals} className="text-gray-400 hover:text-gray-600 transition-colors">
                  <XCircle className="w-6 h-6" />
                </button>
              </div>
              <div className="mb-4">{targetAssignment.assignmentTitle} 과제의 제출을 취소하시겠습니까?</div>
              {/* 실제 제출 취소 로직은 여기에 구현 */}
              <div className="flex justify-end gap-2">
                <Button onClick={closeAllModals} className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white">취소</Button>
                <Button className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white">제출 취소</Button>
              </div>
            </div>
          </div>
        )}
      </main>
      </div>
    </div>
  )
}