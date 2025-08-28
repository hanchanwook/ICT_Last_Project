import { useState, useEffect } from "react"
import { useParams, useNavigate } from "react-router-dom"
import { ArrowLeft, Download, FileText, Calendar, Users, CheckCircle, XCircle, Clock, Eye } from "lucide-react"
import PageLayout from "@/components/ui/page-layout"
import { Button } from "@/components/ui/button"
import { getAssignmentDetail, getAssignmentSubmissions, getAssignmentRubric, getCourseStudents, submitAssignmentGrading, getInstructorSubmissionFiles } from "@/api/sunghyun/assignmentApi"
import { getAssignmentMaterials, downloadAssignmentMaterialByKey } from "@/api/sunghyun/assignmentMaterialApi"
import { downloadFile } from "@/api/sunghyun/instructorCourseApi"
import { getInstructorLectures } from "@/api/sunghyun/instructorCourseApi"
import { getMenuItems } from "@/components/ui/menuConfig"

export default function AssignmentDetailPage() {
  const params = useParams()
  const navigate = useNavigate()
  const [assignment, setAssignment] = useState(null)
  const [submissions, setSubmissions] = useState([])
  const [loading, setLoading] = useState(true)
  const [gradingModal, setGradingModal] = useState({ isOpen: false, submission: null })
  const [currentUser, setCurrentUser] = useState(null)
  const [courseStudents, setCourseStudents] = useState([])
  const [courseName, setCourseName] = useState('')
  const [assignmentMaterials, setAssignmentMaterials] = useState([])
  const [rubricItems, setRubricItems] = useState([])
  
  // 코드 제출 보기 모드 상태 추가
  const [codeViewMode, setCodeViewMode] = useState("서술"); // "서술" 또는 "코드"
  const [parsedCodeData, setParsedCodeData] = useState({
    html: "",
    css: "",
    js: ""
  });

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

  // 과제 상세 정보 조회
  const fetchAssignmentDetail = async () => {
    setLoading(true)
    try {
      const assignmentId = params.id
      if (!assignmentId) {
        console.error('과제 ID가 없습니다.')
        setLoading(false)
        return
      }

      console.log('과제 상세 정보 조회 시작 - assignmentId:', assignmentId)
      
      // 과제 상세 정보 조회
      const assignmentResponse = await getAssignmentDetail(assignmentId)
      console.log('과제 상세 정보 응답:', assignmentResponse)
      const assignmentData = assignmentResponse.data || {}
      
      // 강의 이름 가져오기
      try {
        const coursesResponse = await getInstructorLectures()
        console.log('강의 목록 응답:', coursesResponse)
        
        let coursesData = []
        if (Array.isArray(coursesResponse)) {
          coursesData = coursesResponse
        } else if (coursesResponse.data && Array.isArray(coursesResponse.data)) {
          coursesData = coursesResponse.data
        } else if (coursesResponse.result && Array.isArray(coursesResponse.result)) {
          coursesData = coursesResponse.result
        }
        
        // 과제의 courseId와 매칭되는 강의 찾기
        const course = coursesData.find(c => c.courseId === assignmentData.courseId)
        const courseName = course ? course.courseName : '강의명 없음'
        setCourseName(courseName)
        console.log('매칭된 강의명:', courseName)
      } catch (courseError) {
        console.warn('강의 목록 조회 실패:', courseError)
        setCourseName('강의명 없음')
      }
      
      setAssignment({
        ...assignmentData,
        rubricitem: assignmentData.rubricitem || []
      })

      // 과제 제출 현황 조회
      try {
        const submissionsResponse = await getAssignmentSubmissions(assignmentId)
        console.log('과제 제출 현황 응답:', submissionsResponse)
        let submissionsData = []
        if (submissionsResponse && typeof submissionsResponse === 'object') {
          if (Array.isArray(submissionsResponse)) {
            submissionsData = submissionsResponse
          } else if (submissionsResponse.data && Array.isArray(submissionsResponse.data)) {
            submissionsData = submissionsResponse.data
          } else if (submissionsResponse.result && Array.isArray(submissionsResponse.result)) {
            submissionsData = submissionsResponse.result
          }
        }
        console.log('추출된 제출 현황 데이터:', submissionsData)
        
        // 해당 강의(courseId)에 등록된 학생들만 조회
        let courseStudentsList = []
        try {
          if (assignmentData.courseId) {
            console.log('=== 강의 학생 목록 조회 시작 ===')
            console.log('courseId:', assignmentData.courseId)
            console.log('assignmentData 전체:', assignmentData)
            
            const courseStudentsResponse = await getCourseStudents(assignmentData.courseId)
            console.log('=== getCourseStudents 응답 ===')
            console.log('원본 응답:', courseStudentsResponse)
            console.log('응답 타입:', typeof courseStudentsResponse)
            console.log('Array 여부:', Array.isArray(courseStudentsResponse))
            
            if (courseStudentsResponse && typeof courseStudentsResponse === 'object') {
              if (Array.isArray(courseStudentsResponse)) {
                courseStudentsList = courseStudentsResponse
                console.log('✅ 배열 형태로 학생 목록 설정:', courseStudentsList.length, '명')
              } else if (courseStudentsResponse.data && Array.isArray(courseStudentsResponse.data)) {
                courseStudentsList = courseStudentsResponse.data
                console.log('✅ data 속성에서 학생 목록 설정:', courseStudentsList.length, '명')
              } else if (courseStudentsResponse.students && Array.isArray(courseStudentsResponse.students)) {
                courseStudentsList = courseStudentsResponse.students
                console.log('✅ students 속성에서 학생 목록 설정:', courseStudentsList.length, '명')
              } else {
                console.warn('❌ 인식 가능한 학생 목록 형태가 아님:', Object.keys(courseStudentsResponse))
              }
            } else {
              console.warn('❌ 응답이 객체가 아니거나 null/undefined')
            }
            
            console.log('=== 최종 처리된 강의 학생 목록 ===')
            console.log('학생 수:', courseStudentsList.length)
            console.log('학생 목록 첫 3명 샘플:', courseStudentsList.slice(0, 3))
            console.log('학생 ID 목록:', courseStudentsList.map(s => s.memberId || s.id).slice(0, 10))
            console.log('학생 이름 목록:', courseStudentsList.map(s => s.memberName || s.name).slice(0, 10))
            
            if (courseStudentsList.length === 0) {
              console.warn('⚠️ 강의 학생 목록이 비어있습니다!')
            } else {
              console.log(`✅ 강의 "${assignmentData.courseId}"에 등록된 ${courseStudentsList.length}명의 학생을 조회했습니다.`)
            }
          } else {
            console.warn('❌ courseId가 없어 학생 목록을 조회할 수 없습니다.')
            console.log('assignmentData:', assignmentData)
          }

          // 학생 목록이 있으면 모든 학생 기준으로, 없으면 제출한 학생들만 처리
          let allSubmissionsWithStatus = []
          
          if (courseStudentsList.length > 0) {
            console.log('=== 강의 학생 목록 기준으로 제출 현황 생성 ===')
            // 모든 강의 학생에 대해 제출 현황 생성
            allSubmissionsWithStatus = courseStudentsList.map(student => {
              // 해당 학생의 제출 데이터 찾기
              const submission = submissionsData.find(sub => 
                sub.id === student.memberId || 
                sub.memberId === student.memberId ||
                sub.studentId === student.memberId ||
                sub.userId === student.memberId
              )
              
              // 제출 상태 계산
              let calculatedStatus = "not_submitted" // 기본값: 미제출
              if (submission && (submission.submissionDate || submission.submitDate || submission.createdAt)) {
                const submitTime = new Date(submission.submissionDate || submission.submitDate || submission.createdAt)
                const dueTime = new Date(assignmentData.dueDate)
                
                console.log(`[상태계산] 학생 ${student.memberName || student.memberId}:`, {
                  submitTime: submitTime.toISOString(),
                  dueTime: dueTime.toISOString(),
                  isLate: submitTime > dueTime
                })
                
                if (submitTime <= dueTime) {
                  calculatedStatus = "submitted" // 정시 제출
                } else {
                  calculatedStatus = "late" // 지각 제출
                }
              } else {
                console.log(`[상태계산] 학생 ${student.memberName || student.memberId}: 제출 데이터 없음`)
              }
              
              return {
                ...(submission || {}), // 제출 데이터가 있으면 포함, 없으면 빈 객체
                id: student.memberId,
                memberId: student.memberId,
                memberName: student.memberName || student.studentName || student.name,
                status: calculatedStatus,
                // 제출되지 않은 경우를 위한 기본값들
                submissionDate: submission?.submissionDate || submission?.submitDate || submission?.createdAt || null,
                score: submission?.score || null,
                feedback: submission?.feedback || null
              }
            })
          } else {
            console.log('=== 강의 학생 목록이 없음, 제출한 학생들만 처리 ===')
            // 강의 학생 목록을 가져오지 못한 경우, 제출한 학생들만 상태 계산
            allSubmissionsWithStatus = submissionsData.map(submission => {
              // 제출 상태 계산
              let calculatedStatus = "submitted" // 기본값: 제출 완료 (제출한 학생이니까)
              if (submission.submissionDate || submission.submitDate || submission.createdAt) {
                const submitTime = new Date(submission.submissionDate || submission.submitDate || submission.createdAt)
                const dueTime = new Date(assignmentData.dueDate)
                
                if (submitTime > dueTime) {
                  calculatedStatus = "late" // 지각 제출
                }
              }
              
              return {
                ...submission,
                status: calculatedStatus,
                memberName: submission.memberName || submission.studentName || submission.name || '알 수 없음'
              }
            })
          }
          
          console.log('=== 최종 제출 현황 데이터 ===')
          console.log('총 제출 현황 수:', allSubmissionsWithStatus.length)
          console.log('제출 현황 데이터:', allSubmissionsWithStatus)
          setSubmissions(allSubmissionsWithStatus)
        } catch (courseStudentsError) {
          console.error('강의 학생 목록 조회 실패:', courseStudentsError)
          console.log('=== 오류 발생으로 제출한 학생들만 상태 계산 ===')
          
          // 오류 발생 시 제출한 학생들만 상태 계산
          const basicSubmissions = submissionsData.map(submission => {
            let calculatedStatus = "submitted" // 기본값: 제출 완료
            
            if (submission.submissionDate || submission.submitDate || submission.createdAt) {
              const submitTime = new Date(submission.submissionDate || submission.submitDate || submission.createdAt)
              const dueTime = new Date(assignmentData.dueDate)
              
              console.log(`[오류처리-상태계산] 학생 ${submission.memberName || submission.id}:`, {
                submitTime: submitTime.toISOString(),
                dueTime: dueTime.toISOString(),
                isLate: submitTime > dueTime
              })
              
              if (submitTime > dueTime) {
                calculatedStatus = "late" // 지각 제출
              }
            }
            
            return {
              ...submission,
              status: calculatedStatus,
              memberName: submission.memberName || submission.studentName || submission.name || '알 수 없음'
            }
          })
          
          console.log('=== 오류 처리된 제출 현황 데이터 ===')
          console.log('제출 현황 수:', basicSubmissions.length)
          console.log('제출 현황 데이터:', basicSubmissions)
          setSubmissions(basicSubmissions)
        }
      } catch (submissionError) {
        console.warn('제출 현황 조회 실패 (과제 정보는 정상 표시):', submissionError)
        setSubmissions([])
      }

      // 루브릭 조회
      try {
        const rubricResponse = await getAssignmentRubric(assignmentId)
        console.log('루브릭 응답:', rubricResponse)
        let rubricData = []
        if (rubricResponse && typeof rubricResponse === 'object') {
          if (Array.isArray(rubricResponse)) {
            rubricData = rubricResponse
          } else if (rubricResponse.rubricitem && Array.isArray(rubricResponse.rubricitem)) {
            rubricData = rubricResponse.rubricitem
          } else if (rubricResponse.data && Array.isArray(rubricResponse.data)) {
            rubricData = rubricResponse.data
          } else if (rubricResponse.data && rubricResponse.data.rubricitem && Array.isArray(rubricResponse.data.rubricitem)) {
            rubricData = rubricResponse.data.rubricitem
          } else if (rubricResponse.result && Array.isArray(rubricResponse.result)) {
            rubricData = rubricResponse.result
          }
        }
        console.log('최종 추출된 루브릭 데이터:', rubricData)
        setRubricItems(rubricData)
        setAssignment(prev => ({
          ...prev,
          rubricitem: rubricData
        }))
      } catch (rubricError) {
        console.warn('루브릭 조회 실패:', rubricError)
        setRubricItems([])
      }
      
      // 과제 자료 조회
      try {
        const materialsResponse = await getAssignmentMaterials(assignmentId)
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
      
      // courseId를 기반으로 학생 목록 조회
      if (assignmentData.courseId) {
        try {
          console.log('학생 목록 조회 시작 - courseId:', assignmentData.courseId)
          const studentsResponse = await getCourseStudents(assignmentData.courseId)
          console.log('학생 목록 응답:', studentsResponse)
          let studentsData = []
          if (studentsResponse && typeof studentsResponse === 'object') {
            if (Array.isArray(studentsResponse)) {
              studentsData = studentsResponse
            } else if (studentsResponse.data && Array.isArray(studentsResponse.data)) {
              studentsData = studentsResponse.data
            } else if (studentsResponse.result && Array.isArray(studentsResponse.result)) {
              studentsData = studentsResponse.result
            }
          }
          
          // 중복 학생 제거 (memberId 또는 studentId 기준)
          const uniqueStudents = studentsData.filter((student, index, self) => {
            const studentId = student.memberId || student.studentId || student.id;
            return index === self.findIndex(s => (s.memberId || s.studentId || s.id) === studentId);
          });
          
          console.log('원본 학생 데이터:', studentsData);
          console.log('중복 제거된 학생 데이터:', uniqueStudents);
          
          setCourseStudents(uniqueStudents)
        } catch (studentsError) {
          console.warn('학생 목록 조회 실패:', studentsError)
          setCourseStudents([])
        }
      }
      
    } catch (error) {
      console.error('과제 상세 정보 조회 실패:', error)
      setAssignment(null)
      setSubmissions([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (params.id) {
      fetchAssignmentDetail()
    }
  }, [params.id])

  const handleOpenGradingModal = (submission) => {
    console.log('채점 모달 열기 - submission:', submission)
    
    // 코드 제출 데이터 파싱 (제출 타입이 "코드"인 경우)
    if (submission.submissionType === "코드") {
      const parsedData = parseCodeSubmission(submission.answerText);
      setParsedCodeData(parsedData);
      setCodeViewMode("서술"); // 기본값은 서술로 보기
    } else {
      setParsedCodeData({ html: "", css: "", js: "" });
      setCodeViewMode("서술");
    }
    
    setGradingModal({ isOpen: true, submission })
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

  const handleCloseGradingModal = () => {
    setGradingModal({ isOpen: false, submission: null })
    setCodeViewMode("서술") // 모달 닫을 때 보기 모드 초기화
  }

  const handleSubmitGrading = async (submissionId, score, feedback) => {
    try {
      console.log('채점 제출 시작:', { submissionId, score, feedback })
      
      const gradingData = {
        feedback: feedback || '',
        score: Number(score) || 0,
      }
      
      console.log('채점 데이터:', gradingData)
      
      await submitAssignmentGrading(submissionId, gradingData)
      
      console.log('채점 제출 성공')
      
      await fetchAssignmentDetail();
      handleCloseGradingModal();
    } catch (error) {
      console.error('채점 제출 오류:', error)
      alert(error.message || '저장에 실패했습니다.');
    }
  }

  // Helper functions
  const formatDate = (dateString) => {
    const date = new Date(dateString)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, "0")
    const day = String(date.getDate()).padStart(2, "0")
    const hours = String(date.getHours()).padStart(2, "0")
    const minutes = String(date.getMinutes()).padStart(2, "0")
    return `${year}-${month}-${day} ${hours}:${minutes}`
  }

  const getStatusBadge = (status) => {
    switch (status) {
      case "submitted":
        return (
          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-600">
            <CheckCircle className="w-3 h-3 mr-1" />
            제출 완료
          </span>
        )
      case "late":
        return (
          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-yellow-100 text-yellow-600">
            <Clock className="w-3 h-3 mr-1" />
            지각 제출
          </span>
        )
      case "not_submitted":
        return (
          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-600">
            <XCircle className="w-3 h-3 mr-1" />
            미제출
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-600">
            <XCircle className="w-3 h-3 mr-1" />
            알 수 없음
          </span>
        )
    }
  }

  const getFileIcon = (type) => {
    switch (type) {
      case "pdf":
        return <FileText className="w-4 h-4 text-red-500" />
      case "zip":
        return <FileText className="w-4 h-4 text-blue-500" />
      case "txt":
        return <FileText className="w-4 h-4 text-gray-500" />
      case "docx":
        return <FileText className="w-4 h-4 text-blue-500" />
      default:
        return <FileText className="w-4 h-4 text-gray-500" />
    }
  }

  const handleDownloadFile = async (fileId, fileName) => {
    try {
      console.log(`📥 파일 다운로드 시작: ${fileName} (ID: ${fileId})`)
      
      // 파일 ID 검증
      if (!fileId) {
        throw new Error('파일 ID가 없습니다.');
      }
      
      await downloadFile(fileId, fileName)
      console.log(`✅ 파일 다운로드 성공: ${fileName}`)
    } catch (error) {
      console.error('❌ 파일 다운로드 실패:', error)
      
      // 사용자에게 더 구체적인 에러 메시지 표시
      let errorMessage = '파일 다운로드에 실패했습니다.';
      
      if (error.message.includes('서버 내부 오류')) {
        errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
      } else if (error.message.includes('파일을 찾을 수 없습니다')) {
        errorMessage = '파일이 존재하지 않거나 삭제되었습니다.';
      } else if (error.message.includes('권한이 없습니다')) {
        errorMessage = '파일 다운로드 권한이 없습니다.';
      } else if (error.message.includes('네트워크 오류')) {
        errorMessage = '네트워크 연결을 확인해주세요.';
      } else if (error.message.includes('시간이 초과')) {
        errorMessage = '요청 시간이 초과되었습니다. 다시 시도해주세요.';
      }
      
      alert(errorMessage);
    }
  }

  const handleDownloadAssignmentMaterial = async (fileKey, fileName) => {
    try {
      console.log(`📥 과제 자료 다운로드 시작: ${fileName} (Key: ${fileKey})`)
      
      // 파일 키 검증
      if (!fileKey) {
        throw new Error('파일 키가 없습니다.');
      }
      
      await downloadFile(fileKey, fileName)
      console.log(`✅ 과제 자료 다운로드 성공: ${fileName}`)
    } catch (error) {
      console.error('❌ 과제 자료 다운로드 실패:', error)
      
      // 사용자에게 더 구체적인 에러 메시지 표시
      let errorMessage = '과제 자료 다운로드에 실패했습니다.';
      
      if (error.message.includes('서버 내부 오류')) {
        errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
      } else if (error.message.includes('파일을 찾을 수 없습니다')) {
        errorMessage = '파일이 존재하지 않거나 삭제되었습니다.';
      } else if (error.message.includes('권한이 없습니다')) {
        errorMessage = '파일 다운로드 권한이 없습니다.';
      } else if (error.message.includes('네트워크 오류')) {
        errorMessage = '네트워크 연결을 확인해주세요.';
      } else if (error.message.includes('시간이 초과')) {
        errorMessage = '요청 시간이 초과되었습니다. 다시 시도해주세요.';
      }
      
      alert(errorMessage);
    }
  }

  if (loading) {
    return (
      <PageLayout title="과제 상세보기" sidebarItems={sidebarItems} currentPath="/instructor/courses/assignments">
        <div className="flex items-center justify-center h-96">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">과제 정보를 불러오는 중...</p>
          </div>
        </div>
      </PageLayout>
    )
  }

  if (!assignment) {
    return (
      <PageLayout title="과제 상세보기" sidebarItems={sidebarItems} currentPath="/instructor/courses/assignments">
        <div className="flex items-center justify-center h-96">
          <div className="text-center">
            <FileText className="mx-auto h-12 w-12 text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">과제를 찾을 수 없습니다</h3>
            <p className="text-gray-500 mb-4">요청하신 과제 정보가 존재하지 않거나 삭제되었습니다.</p>
            <Button variant="outline" onClick={() => navigate(-1)} className="mt-2">
              뒤로가기
            </Button>
          </div>
        </div>
      </PageLayout>
    )
  }

  // 통계 계산
  const totalStudents = submissions.length
  const submittedCount = submissions.filter((s) => s.status === "submitted").length
  const lateSubmissionCount = submissions.filter((s) => s.status === "late").length
  const notSubmittedCount = submissions.filter((s) => s.status === "not_submitted").length

  return (
    <PageLayout title="과제 상세보기" sidebarItems={sidebarItems} currentPath="/instructor/courses/assignments">
      <div className="space-y-6">
        {/* 뒤로가기 버튼 */}
        <div className="flex items-center space-x-4">
          <Button variant="outline" onClick={() => navigate(-1)} className="flex items-center space-x-2">
            <ArrowLeft className="w-4 h-4" />
            <span>뒤로가기</span>
          </Button>
        </div>

        {/* 과제 기본 정보 */}
        <div className="bg-white rounded-lg shadow-sm border p-6">
          <div className="flex justify-between items-start mb-4">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 mb-2">{assignment.assignmentTitle || assignment.title || '-'}</h1>
              <div className="flex items-center space-x-4 text-sm text-gray-600">
                <span className="flex items-center">
                  <FileText className="w-4 h-4 mr-1" />
                  {courseName || assignment.courseId || assignment.courseCode || '-'}
                </span>
                <span className="flex items-center">
                  <Calendar className="w-4 h-4 mr-1" />
                  마감: {assignment.dueDate ? formatDate(assignment.dueDate) : '-'}
                </span>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <span className="px-2 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-600">
                {assignment.assignmentActive === 1 ? "삭제됨" : "활성"}
              </span>
            </div>
          </div>

          {/* 과제 설명 */}
          <div className="mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">과제 설명</h3>
            <div className="bg-gray-50 rounded-lg p-4">
              <pre className="whitespace-pre-wrap text-sm text-gray-700 font-sans">
                {assignment.assignmentContent || assignment.assignmentDesc || assignment.assignmentDescription || assignment.description || "과제 설명이 없습니다."}
              </pre>
            </div>
          </div>

          {/* 제출 요구사항 */}
          <div className="mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">제출 요구사항</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex items-center space-x-2">
                <span className={`w-3 h-3 rounded-full ${assignment.fileRequired ? 'bg-green-500' : 'bg-gray-300'}`}></span>
                <span className="text-sm text-gray-700">파일 제출 필수</span>
                <span className="text-sm font-medium">{assignment.fileRequired ? '예' : '아니오'}</span>
              </div>
              <div className="flex items-center space-x-2">
                <span className={`w-3 h-3 rounded-full ${assignment.codeRequired ? 'bg-green-500' : 'bg-gray-300'}`}></span>
                <span className="text-sm text-gray-700">코드 제출 필수</span>
                <span className="text-sm font-medium">{assignment.codeRequired ? '예' : '아니오'}</span>
              </div>
            </div>
          </div>

          {/* 루브릭 채점 기준 */}
          <div className="mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">루브릭 채점 기준</h3>
            {rubricItems && rubricItems.length > 0 ? (
              <div className="space-y-3">
                {rubricItems.map((item, index) => (
                  <div key={index} className="bg-gray-50 rounded-lg p-4 border-l-4 border-blue-500">
                    <div className="flex justify-between items-start mb-2">
                      <h4 className="font-medium text-gray-900">{item.itemTitle || `항목 ${index + 1}`}</h4>
                      <span className="text-sm font-semibold text-blue-600">{item.maxScore || 0}점</span>
                    </div>
                    <p className="text-sm text-gray-600">{item.description || '설명 없음'}</p>
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-gray-50 rounded-lg p-4 text-center">
                <p className="text-gray-500">등록된 루브릭(채점 기준)이 없습니다.</p>
              </div>
            )}
          </div>

          {/* 업로드된 과제 자료 */}
          <div className="mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">업로드된 과제 자료</h3>
            {assignmentMaterials && assignmentMaterials.length > 0 ? (
              <div className="space-y-2">
                {assignmentMaterials.map((material, index) => (
                  <div key={index} className="flex items-center justify-between bg-gray-50 rounded-lg p-3">
                    <div className="flex items-center space-x-3">
                      <FileText className="w-5 h-5 text-blue-500" />
                      <div>
                        <p className="text-sm font-medium text-gray-900">{material.fileName || material.name || '파일명 없음'}</p>
                        <p className="text-xs text-gray-500">
                          {material.fileSize ? `${(material.fileSize / 1024 / 1024).toFixed(2)} MB` : '크기 정보 없음'} • 
                          {material.fileType || material.type || '타입 정보 없음'}
                        </p>
                      </div>
                    </div>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        console.log('=== 과제 자료 다운로드 디버깅 ===');
                        console.log('material 객체:', material);
                        console.log('fileKey:', material?.fileKey);
                        console.log('fileName:', material?.fileName || material?.name);
                        
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
                        
                        // materialId를 우선적으로 사용하고, 없으면 다른 키들 시도 (강의 자료와 동일)
                        const downloadKey = material?.materialId || material?.fileKey || material?.fileId || material?.id;
                        const fileName = material?.fileName || material?.name || 'download';
                        
                        console.log('최종 선택된 다운로드 키:', downloadKey);
                        console.log('키 타입:', typeof downloadKey);
                        
                        if (downloadKey) {
                          handleDownloadAssignmentMaterial(downloadKey, fileName);
                        } else {
                          console.error('다운로드 키가 없습니다!');
                          alert('파일 다운로드 기능은 실제 업로드된 파일에서만 사용 가능합니다.');
                        }
                      }}
                      className="flex items-center space-x-1"
                    >
                      <Download className="w-4 h-4" />
                      <span>다운로드</span>
                    </Button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-gray-50 rounded-lg p-4 text-center">
                <p className="text-gray-500">업로드된 과제 자료가 없습니다.</p>
              </div>
            )}
          </div>
        </div>

        {/* 통계 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-lg shadow-sm border p-4">
            <div className="flex items-center">
              <CheckCircle className="w-8 h-8 text-green-500 mr-3" />
              <div>
                <p className="text-sm font-medium text-gray-600">총 제출</p>
                <p className="text-2xl font-bold text-gray-900">{submittedCount}</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow-sm border p-4">
            <div className="flex items-center">
              <XCircle className="w-8 h-8 text-red-500 mr-3" />
              <div>
                <p className="text-sm font-medium text-gray-600">미제출</p>
                <p className="text-2xl font-bold text-gray-900">{notSubmittedCount}</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow-sm border p-4">
            <div className="flex items-center">
              <Clock className="w-8 h-8 text-yellow-500 mr-3" />
              <div>
                <p className="text-sm font-medium text-gray-600">지각 제출</p>
                <p className="text-2xl font-bold text-gray-900">{lateSubmissionCount}</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow-sm border p-4">
            <div className="flex items-center">
              <FileText className="w-8 h-8 text-blue-500 mr-3" />
              <div>
                <p className="text-sm font-medium text-gray-600">평균 점수</p>
                <p className="text-2xl font-bold text-gray-900">0점</p>
              </div>
            </div>
          </div>
        </div>

        {/* 제출 현황 */}
        <div className="bg-white rounded-lg shadow-sm border p-6">
          <div className="mb-4">
            <h2 className="text-xl font-semibold text-gray-900">제출 현황 ({submissions.length}명)</h2>
          </div>

          {submissions.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      학생
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      상태
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      제출일
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      점수
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      관리
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {submissions.map((submission) => (
                    <tr key={submission.id}>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">
                          {submission.memberName || submission.studentName || submission.id || '알 수 없음'}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getStatusBadge(submission.status)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {submission.submittedAt ? formatDate(submission.submittedAt) : '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {submission.score || '-'}점
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <div className="flex space-x-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleOpenGradingModal(submission)}
                          >
                            채점
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="text-center py-12">
              <FileText className="mx-auto h-12 w-12 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">제출된 과제가 없습니다</h3>
              <p className="text-gray-500 mb-4">
                아직 학생들이 이 과제를 제출하지 않았습니다. 과제 마감일을 확인해보세요.
              </p>
              <div className="text-sm text-gray-400">
                <p>• 과제 마감일: {assignment.dueDate ? formatDate(assignment.dueDate) : '설정되지 않음'}</p>
                <p>• 제출 현황은 학생들이 과제를 제출한 후에 표시됩니다.</p>
              </div>
            </div>
          )}
        </div>

        {/* 과제 관리 안내 */}
        <div className="bg-blue-50 rounded-lg p-4">
          <h3 className="text-lg font-semibold text-blue-900 mb-2">과제 관리 안내</h3>
          <ul className="text-sm text-blue-800 space-y-1">
            <li>• 제출된 파일은 개별 다운로드 또는 일괄 다운로드가 가능합니다.</li>
            <li>• 채점이 완료되지 않은 과제는 채점 버튼을 통해 수동으로 채점할 수 있습니다.</li>
            <li>• 과제 마감일이 지나면 자동으로 상태가 변경됩니다.</li>
          </ul>
        </div>

        {/* 채점 모달 */}
        {gradingModal.isOpen && (
          <GradingModal
            submission={gradingModal.submission}
            assignment={assignment}
            onClose={handleCloseGradingModal}
            onSubmit={handleSubmitGrading}
            getStatusBadge={getStatusBadge}
            codeViewMode={codeViewMode}
            setCodeViewMode={setCodeViewMode}
            parsedCodeData={parsedCodeData}
          />
        )}
      </div>
    </PageLayout>
  )
}

// 채점 모달 컴포넌트
function GradingModal({ submission, assignment, onClose, onSubmit, getStatusBadge, codeViewMode, setCodeViewMode, parsedCodeData }) {
  const [score, setScore] = useState(submission.score || "")
  const [feedback, setFeedback] = useState(submission.feedback || "")
  const [loading, setLoading] = useState(false)
  // 학생 제출 파일 상태 추가
  const [submissionFiles, setSubmissionFiles] = useState([])
  const [filesLoading, setFilesLoading] = useState(true)

  useEffect(() => {
    async function fetchFiles() {
      // submission 객체 구조 확인을 위한 로그
      console.log('📋 submission 객체 전체:', submission)
      
      if (!submission) {
        console.log('❌ submission 객체가 없습니다')
        setSubmissionFiles([])
        setFilesLoading(false)
        return
      }
      
      // 학생 쪽과 동일한 방식으로 ID들 추출
      const submissionId = submission.submissionId || submission.id
      const assignmentId = submission.assignmentId || submission.assignment?.assignmentId
      const courseId = submission.courseId || submission.assignment?.courseId || assignment?.courseId
      const studentId = submission.memberId || submission.studentId
      
      console.log('📋 추출된 ID들:', { submissionId, assignmentId, courseId, studentId })
      
      if (!submissionId || !assignmentId) {
        console.log('❌ 필요한 ID가 부족합니다')
        setSubmissionFiles([])
        setFilesLoading(false)
        return
      }
      
      try {
        console.log('📁 강사용 채점 모달에서 파일 조회 시작:', {
          assignmentId: assignmentId,
          courseId: courseId,
          submissionId: submissionId,
          studentId: studentId
        })
        
        // 기존 강사용 함수 사용
        console.log('🔍 기존 강사용 파일 조회 시도:', { assignmentId, courseId, submissionId, studentId })
        const res = await getInstructorSubmissionFiles(assignmentId, courseId, submissionId, studentId)
        
        console.log('📁 강사용 채점 모달 파일 조회 결과:', res)
        
        // 새로운 API는 이미 배열 형태로 반환되므로 직접 사용
        const files = Array.isArray(res) ? res : []
        
        console.log('📁 강사용 처리된 파일 목록:', files)
        setSubmissionFiles(files)
        setFilesLoading(false)
      } catch (error) {
        console.error('❌ 강사용 채점 모달 파일 조회 실패:', error)
        setSubmissionFiles([])
        setFilesLoading(false)
      }
    }
    fetchFiles()
  }, [submission])

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (!score || score < 0 || score > (assignment ? assignment.maxScore : 100)) {
      alert(`점수는 0점에서 ${assignment ? assignment.maxScore : 100}점 사이여야 합니다.`)
      return
    }

    setLoading(true)
    try {
      await onSubmit(submission.submissionId, Number(score), feedback)
    } catch (error) {
      console.error('GradingModal에서 오류 발생:', error)
    } finally {
      setLoading(false)
    }
  }

  const formatDate = (dateString) => {
    const date = new Date(dateString)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, "0")
    const day = String(date.getDate()).padStart(2, "0")
    const hours = String(date.getHours()).padStart(2, "0")
    const minutes = String(date.getMinutes()).padStart(2, "0")
    return `${year}-${month}-${day} ${hours}:${minutes}`
  }

  const getFileIcon = (fileName) => {
    if (!fileName) return <FileText className="w-4 h-4 text-gray-500" />
    
    const extension = fileName.split('.').pop()?.toLowerCase()
    
    switch (extension) {
      case "pdf":
        return <FileText className="w-4 h-4 text-red-500" />
      case "zip":
      case "rar":
      case "7z":
        return <FileText className="w-4 h-4 text-blue-500" />
      case "txt":
        return <FileText className="w-4 h-4 text-gray-500" />
      case "doc":
      case "docx":
        return <FileText className="w-4 h-4 text-blue-500" />
      case "xls":
      case "xlsx":
        return <FileText className="w-4 h-4 text-green-500" />
      case "ppt":
      case "pptx":
        return <FileText className="w-4 h-4 text-orange-500" />
      case "jpg":
      case "jpeg":
      case "png":
      case "gif":
        return <FileText className="w-4 h-4 text-purple-500" />
      default:
        return <FileText className="w-4 h-4 text-gray-500" />
    }
  }

  // 파일 크기 포맷 함수 (학생 쪽과 동일)
  const formatFileSize = (bytes) => {
    if (!bytes) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const handleDownloadFile = async (fileKey, fileName) => {
    try {
      console.log(`📥 제출 파일 다운로드 시작: ${fileName} (Key: ${fileKey})`)
      
      // 파일 키 검증
      if (!fileKey) {
        throw new Error('파일 키가 없습니다.');
      }
      
      // 기존 다운로드 함수 사용
      await downloadFile(fileKey, fileName);
      
      console.log(`✅ 제출 파일 다운로드 성공: ${fileName}`)
    } catch (error) {
      console.error('❌ 제출 파일 다운로드 실패:', error)
      
      // 사용자에게 더 구체적인 에러 메시지 표시
      let errorMessage = '제출 파일 다운로드에 실패했습니다.';
      
      if (error.message.includes('서버 내부 오류')) {
        errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
      } else if (error.message.includes('파일을 찾을 수 없습니다')) {
        errorMessage = '파일이 존재하지 않거나 삭제되었습니다.';
      } else if (error.message.includes('권한이 없습니다')) {
        errorMessage = '파일 다운로드 권한이 없습니다.';
      } else if (error.message.includes('네트워크 오류')) {
        errorMessage = '네트워크 연결을 확인해주세요.';
      } else if (error.message.includes('시간이 초과')) {
        errorMessage = '요청 시간이 초과되었습니다. 다시 시도해주세요.';
      }
      
      alert(errorMessage);
    }
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-gray-200">
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-semibold text-gray-900">과제 채점</h2>
            <Button variant="outline" size="sm" onClick={onClose}>
              ✕
            </Button>
          </div>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* 제출 정보 */}
          <div>
            <h3 className="font-semibold text-gray-900 mb-2">제출 정보</h3>
            <div className="bg-gray-50 p-4 rounded-lg space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">학생:</span>
                <span className="text-sm font-medium text-gray-900">
                  {submission.memberName || submission.studentName || submission.id || '알 수 없음'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">제출일:</span>
                <span className="text-sm font-medium text-gray-900">
                  {submission.submittedAt ? formatDate(submission.submittedAt) : '-'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">상태:</span>
                <div>{getStatusBadge(submission.status)}</div>
              </div>
            </div>
          </div>

          {/* 제출 답안 */}
          {submission.answerText && (
            <div>
              <h3 className="font-semibold text-gray-900 mb-2">제출 답안</h3>
              
              {/* 코드 제출인 경우 드롭다운 추가 */}
              {submission.submissionType === "코드" && (
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
              
              <div className="bg-gray-50 p-4 rounded-lg">
                {submission.submissionType === "코드" && codeViewMode === "코드" ? (
                  <div className="space-y-3">
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
                  <pre className="whitespace-pre-wrap text-sm text-gray-700 font-sans">
                    {submission.answerText}
                  </pre>
                )}
              </div>
            </div>
          )}

          {/* 제출 파일 (학생 상세 모달과 동일하게) */}
          <div>
            <h3 className="font-semibold text-gray-900 mb-2">제출 파일</h3>
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
              {filesLoading ? (
                <div className="flex items-center justify-center py-8">
                  <div className="text-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-3"></div>
                    <p className="text-gray-500 text-sm">파일 목록을 불러오는 중...</p>
                  </div>
                </div>
              ) : submissionFiles && submissionFiles.length > 0 ? (
                <div className="space-y-3">
                  {submissionFiles.map((file, index) => (
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
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={() => handleDownloadFile(file.fileId || file.id || file.fileKey, file.fileName || file.name)}
                        className="text-blue-600 border-blue-600 hover:bg-blue-50"
                      >
                        <Download className="w-4 h-4" />
                        <span>다운로드</span>
                      </Button>
                    </div>
                  ))}
                </div>
              ) : (
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

          {/* 점수 입력 */}
          <div>
            <label htmlFor="score" className="block text-sm font-medium text-gray-700 mb-2">
              점수 <span className="text-red-500">*</span>
            </label>
            <div className="flex items-center space-x-2">
              <input
                type="number"
                id="score"
                min="0"
                max={assignment ? assignment.maxScore : 100}
                value={score}
                onChange={(e) => setScore(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="점수를 입력하세요"
                required
              />
              <span className="text-sm text-gray-600">/ {assignment ? assignment.maxScore : 100}점</span>
            </div>
          </div>

          {/* 피드백 입력 */}
          <div>
            <label htmlFor="feedback" className="block text-sm font-medium text-gray-700 mb-2">
              피드백
            </label>
            <textarea
              id="feedback"
              rows={4}
              value={feedback}
              onChange={(e) => setFeedback(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="학생에게 전달할 피드백을 작성하세요..."
            />
          </div>

          {/* 버튼 */}
          <div className="flex justify-end space-x-3 pt-4 border-t border-gray-200">
            <Button type="button" variant="outline" onClick={onClose} disabled={loading}>
              취소
            </Button>
            <Button type="submit" className="bg-green-600 hover:bg-green-700" disabled={loading}>
              {loading ? "저장 중..." : "채점 완료"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
