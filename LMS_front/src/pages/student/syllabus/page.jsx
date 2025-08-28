import { useState, useEffect } from "react"
import { Search, BookOpen, Calendar, Clock, User, MapPin, FileText, Download, Eye } from "lucide-react"
import Header from "@/components/layout/header"
import Sidebar from "@/components/layout/sidebar"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { getStudentCoursesByMemberId, getStudentSyllabus } from "@/api/sunghyun/studentCourseApi"
import { getClassroomDetail } from "@/api/sunghyun/classroomApi"
import { getCourseMaterials, downloadFile } from "@/api/sunghyun/instructorCourseApi"

export default function StudentSyllabusPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [selectedPeriod, setSelectedPeriod] = useState("all")
  const [isLoading, setIsLoading] = useState(true)
  const [courses, setCourses] = useState([])
  const [error, setError] = useState(null)
  const [currentUser, setCurrentUser] = useState(null)

  // localStorage에서 사용자 정보 가져오기
  useEffect(() => {
    const userInfo = localStorage.getItem('currentUser')
    if (userInfo) {
      setCurrentUser(JSON.parse(userInfo))
    }
  }, [])

  const sidebarMenuItems = [
    {
      label: "강의 리스트",
      href: "/student/syllabus",
      icon: "BookOpen"
    }
  ]

  // 강의 상태를 기간 기준으로 계산하는 함수 (강사 입장과 동일)
  const getCourseStatus = (course) => {
    const now = new Date()
    const startDate = course.courseStartDay ? new Date(course.courseStartDay) : null
    const endDate = course.courseEndDay ? new Date(course.courseEndDay) : null
    
    if (!startDate) return '예정'
    
    if (now < startDate) {
      return '예정'
    } else if (now >= startDate && now <= endDate) {
      return '진행중'
    } else {
      return '완료'
    }
  }

  // 모든 강의 리스트 불러오기
  useEffect(() => {
    const fetchCourses = async () => {
      try {
        setIsLoading(true)
        setError(null)
        console.log('현재 사용자 정보:', currentUser)
        
        console.log('API 호출 시작 - 학생 수강 강의 목록 조회')
        
        const data = await getStudentCoursesByMemberId()
        console.log('API 응답 데이터:', data)
        
        let coursesData = []
        if (Array.isArray(data)) {
          coursesData = data
        } else if (data.data && Array.isArray(data.data)) {
          coursesData = data.data
        } else if (data.result && Array.isArray(data.result)) {
          coursesData = data.result
        }
        
        console.log('처리된 강의 데이터:', coursesData)
        
        if (coursesData.length === 0) {
          console.warn('수강 강의가 없습니다. 수강 신청을 먼저 해주세요.')
        }

        // 각 강의에 대해 강의실 정보와 자료 개수 가져오기 (강사 입장과 동일)
        const coursesWithDetails = await Promise.all(
          coursesData.map(async (course) => {
            try {
              // 강의 자료 개수 가져오기
              let materialsCount = 0
              try {
                const materialsResponse = await getCourseMaterials(course.courseId)
                console.log(`강의 ${course.courseId} 자료 응답:`, materialsResponse)
                
                if (materialsResponse && typeof materialsResponse === 'object') {
                  if (Array.isArray(materialsResponse)) {
                    materialsCount = materialsResponse.length
                  } else if (materialsResponse.data && Array.isArray(materialsResponse.data)) {
                    materialsCount = materialsResponse.data.length
                  } else if (materialsResponse.result && Array.isArray(materialsResponse.result)) {
                    materialsCount = materialsResponse.result.length
                  }
                }
              } catch (materialsError) {
                console.warn(`강의 ${course.courseId} 자료 조회 실패:`, materialsError)
                materialsCount = 0
              }
              
              // 강의실 정보 가져오기
              let classInfo = null
              if (course.classId) {
                try {
                  console.log(`강의실 정보 조회 시작: ${course.classId}`)
                  classInfo = await getClassroomDetail(course.classId)
                  console.log(`강의실 ${course.classId} 정보:`, classInfo)
                  
                  // classInfo 구조 확인
                  if (classInfo && typeof classInfo === 'object') {
                    console.log('classInfo 구조:', {
                      classId: classInfo.classId,
                      classCode: classInfo.classCode,
                      className: classInfo.className
                    })
                  }
                } catch (classError) {
                  console.warn(`강의실 ${course.classId} 정보 조회 실패:`, classError)
                  console.warn('에러 상세:', classError.response?.data)
                  
                  // UUID인 경우 기본값 설정
                  const isUUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(course.classId)
                  classInfo = {
                    classId: course.classId,
                    classCode: isUUID ? '미정' : `강의실${course.classId}`,
                    className: isUUID ? '강의실 미정' : `강의실 ${course.classId}`
                  }
                }
              }
              
              return {
                ...course,
                materialsCount: materialsCount,
                classInfo: classInfo
              }
            } catch (error) {
              console.warn(`강의 ${course.courseId} 상세 정보 조회 실패:`, error)
              
              // UUID인 경우 기본값 설정
              const isUUID = course.classId ? /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(course.classId) : false
              
              return {
                ...course,
                materialsCount: 0,
                classInfo: course.classId ? {
                  classId: course.classId,
                  classCode: isUUID ? '미정' : `강의실${course.classId}`,
                  className: isUUID ? '강의실 미정' : `강의실 ${course.classId}`
                } : null
              }
            }
          })
        )
        
        console.log('상세 정보가 포함된 강의 데이터:', coursesWithDetails)
        setCourses(coursesWithDetails)
      } catch (err) {
        console.error('강의 데이터 로딩 오류:', err)
        setError(err.message)
      } finally {
        setIsLoading(false)
      }
    }
    if (currentUser) fetchCourses()
  }, [currentUser])

  const [selectedCourse, setSelectedCourse] = useState(null)

  // 필터링된 강의 목록 (강사 입장과 동일한 로직)
  const filteredCourses = courses.filter((course) => {
    const matchesSearch =
      course.courseName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      course.courseCode?.toLowerCase().includes(searchTerm.toLowerCase())

    const matchesPeriod = selectedPeriod === "all" || 
      (course.courseStartDay && new Date(course.courseStartDay).getFullYear().toString() === selectedPeriod)
    
    const courseStatus = getCourseStatus(course)
    const matchesStatus = selectedStatus === "all" || courseStatus === selectedStatus

    return matchesSearch && matchesPeriod && matchesStatus
  })

  const handleViewSyllabus = async (course) => {
    try {
      console.log('강의계획서 조회 시작 - courseId:', course.courseId)
      const syllabusData = await getStudentSyllabus(course.courseId)
      console.log('강의계획서 데이터:', syllabusData)
      
      // 강의 자료 목록 조회
      let materialsData = []
      try {
        const materialsResponse = await getCourseMaterials(course.courseId)
        console.log('강의 자료 목록 응답:', materialsResponse)
        
        if (materialsResponse && typeof materialsResponse === 'object') {
          if (Array.isArray(materialsResponse)) {
            materialsData = materialsResponse
          } else if (materialsResponse.data && Array.isArray(materialsResponse.data)) {
            materialsData = materialsResponse.data
          } else if (materialsResponse.result && Array.isArray(materialsResponse.result)) {
            materialsData = materialsResponse.result
          }
        }
        console.log('처리된 강의 자료 목록:', materialsData)
      } catch (materialsError) {
        console.warn('강의 자료 목록 조회 실패:', materialsError)
        materialsData = []
      }
      
      // 강의 정보와 강의계획서 정보, 자료 목록을 합쳐서 저장
      setSelectedCourse({
        ...course,
        syllabus: syllabusData,
        materials: materialsData
      })
    } catch (error) {
      console.error('강의계획서 조회 오류:', error)
      alert('강의계획서를 불러오는데 실패했습니다.')
    }
  }

  const handleDownloadSyllabus = (course) => {
    // 강의계획서 다운로드 로직
    console.log(`${course.courseName} 강의계획서 다운로드`)
  }

  const handleDownloadMaterial = async (material) => {
    try {
      console.log('=== 강의 자료 다운로드 디버깅 ===')
      console.log('material 객체:', material)
      console.log('각 키 값:', {
        materialId: material?.materialId,
        fileKey: material?.fileKey,
        fileId: material?.fileId,
        id: material?.id
      })
      
      // materialId를 우선적으로 사용하고, 없으면 다른 키들 시도 (강의 자료와 동일)
      const downloadKey = material?.materialId || material?.fileKey || material?.fileId || material?.id
      const fileName = material?.fileName || material?.name || 'download'
      
      console.log('최종 선택된 다운로드 키:', downloadKey)
      console.log('파일명:', fileName)
      
      if (downloadKey) {
        await downloadFile(downloadKey, fileName)
      } else {
        console.error('다운로드 키가 없습니다!')
        alert('파일 다운로드 기능은 실제 업로드된 파일에서만 사용 가능합니다.')
      }
    } catch (error) {
      console.error('강의 자료 다운로드 실패:', error)
      alert('파일 다운로드에 실패했습니다.')
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case "완료":
        return "bg-green-100 text-green-800"
      case "예정":
        return "bg-blue-100 text-blue-800"
      case "진행중":
        return "bg-yellow-100 text-yellow-800"
      case "취소":
        return "bg-red-100 text-red-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  // 파일 아이콘 함수 (강의 자료와 동일)
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

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header currentPage="syllabus" userRole="student" userName="김학생" />
        <div className="flex items-center justify-center h-96">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-600">강의계획서를 불러오는 중...</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header currentPage="syllabus" userRole="student" userName="김학생" />

      <div className="flex">
        <Sidebar title="강의계획서" menuItems={sidebarMenuItems} currentPath="/student/syllabus" />
        <main className="flex-1 p-6">
        <div className="max-w-7xl mx-auto">
          {/* 페이지 헤더 */}
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900">강의계획서</h1>
            <p className="text-gray-600 mt-1">수강중인 강의들의 강의계획서를 확인할 수 있습니다.</p>
          </div>

          {/* 통계 카드 */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            <Card>
              <CardContent className="p-6 flex items-center justify-between">
                <div className="ml-4">
                  <p className="text-base font-medium text-gray-600">총 수강 과목</p>
                  <p className="text-3xl font-bold text-[#3498db]">{courses.length}회</p>
                </div>
                <div className="bg-[#EFF6FF] rounded-full p-3 mr-3">
                  <BookOpen className="w-10 h-10 text-[#3498db]" />
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6 flex items-center justify-between">
                <div className="ml-4">
                  <p className="text-base font-medium text-gray-600">완료된 과목</p>
                  <p className="text-3xl font-bold text-[#1abc9c]">{courses.filter((c) => getCourseStatus(c) === '완료').length}회</p>
                </div>
                <div className="bg-[#e4f5eb] rounded-full p-3 mr-3">
                  <Clock className="w-10 h-10 text-[#1abc9c]" />
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6 flex items-center justify-between">
                <div>
                  <p className="text-base font-medium text-gray-600">진행중인 과목</p>
                  <p className="text-3xl font-bold text-[#F1C40F]">{courses.filter((c) => getCourseStatus(c) === '진행중').length}회</p>
                </div>
                <div className="bg-yellow-50 rounded-full p-3 mr-3">
                  <Calendar className="w-10 h-10 text-[#F1C40F]" />
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6 flex items-center justify-between">
                <div className="ml-4">
                  <p className="text-base font-medium text-gray-600">예정된 과목</p>
                  <p className="text-3xl font-bold text-[#b0c4de]">{courses.filter((c) => getCourseStatus(c) === '예정').length}회</p>
                </div>
                <div className="bg-[#eff6ff] rounded-full p-3 mr-3">
                  <User className="w-10 h-10 text-[#b0c4de]" />
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 검색 및 필터 (강사 입장과 동일한 구조) */}
          <div className="bg-white p-4 rounded-lg shadow-sm border mb-6">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                <input
                  type="text"
                  placeholder="과목명, 과목코드, 강사명으로 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <select
                value={selectedPeriod}
                onChange={(e) => setSelectedPeriod(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="all">전체 연도</option>
                {Array.from(new Set(courses.map(c => c.courseStartDay && new Date(c.courseStartDay).getFullYear()))).filter(year => year).map(year => (
                  <option key={year} value={year}>{year}년</option>
                ))}
              </select>
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="all">전체 상태</option>
                <option value="예정">예정</option>
                <option value="진행중">진행중</option>
                <option value="완료">완료</option>
              </select>
            </div>
          </div>

          {/* 강의 목록 */}
          <div className="bg-white rounded-lg shadow-sm border overflow-hidden">
            {filteredCourses.length === 0 ? (
              <div className="p-8 text-center">
                <div className="text-gray-500 text-lg mb-2">등록된 강의가 없습니다.</div>
                <div className="text-gray-400 text-sm">수강 신청을 먼저 해주세요.</div>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        강의 정보
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        일시/장소
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        강의 자료
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        상태
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        관리
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {filteredCourses.map((course) => {
                      const courseStatus = getCourseStatus(course)
                      return (
                        <tr key={course.courseId} className="hover:bg-gray-50">
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div>
                              <div className="text-sm font-medium text-gray-900">{course.courseName}</div>
                              <div className="text-sm text-gray-500">
                                {course.courseCode}
                              </div>
                              <div className="text-sm text-gray-500">
                                {course.courseStartDay && new Date(course.courseStartDay).toLocaleDateString()} ~ {course.courseEndDay && new Date(course.courseEndDay).toLocaleDateString()}
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center mb-1">
                              <Calendar className="w-4 h-4 text-gray-400 mr-2" />
                              <span className="text-sm text-gray-900">
                                {course.courseStartDay && new Date(course.courseStartDay).toLocaleDateString()}
                              </span>
                            </div>
                            <div className="flex items-center mb-1">
                              <Clock className="w-4 h-4 text-gray-400 mr-2" />
                              <span className="text-sm text-gray-900">
                                {course.courseDays ? (
                                  `${course.courseDays} ${course.startTime || '00:00'}~${course.endTime || '00:00'}`
                                ) : (
                                  '시간 미정'
                                )}
                              </span>
                            </div>
                            <div className="flex items-center">
                              <MapPin className="w-4 h-4 text-gray-400 mr-2" />
                              <span className="text-sm text-gray-900">
                                {course.classInfo ? course.classInfo.classCode : (course.classId || '미정')}
                              </span>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm text-gray-900">
                              {course.materialsCount > 0 ? (
                                <span className="text-blue-600 font-medium">
                                  {course.materialsCount}개
                                </span>
                              ) : (
                                <span className="text-gray-500">없음</span>
                              )}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <span
                              className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(courseStatus)}`}
                            >
                              {courseStatus}
                            </span>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex space-x-2">
                                                             <Button
                                 size="sm"
                                 onClick={() => handleViewSyllabus(course)}
                                 className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                               >
                                 상세보기
                               </Button>
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
        </main>
      </div>

      {/* 강의계획서 상세 모달 */}
      {selectedCourse && (
        <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b">
              <div className="flex justify-between items-center">
                <h2 className="text-xl font-bold text-gray-900">{selectedCourse.courseName} 강의계획서</h2>
                <button onClick={() => setSelectedCourse(null)} className="text-gray-400 hover:text-gray-600">
                  ✕
                </button>
              </div>
            </div>

            <div className="p-6 space-y-6">
              {/* 기본 정보 */}
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-3">기본 정보</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="font-medium">과목코드:</span> {selectedCourse.courseCode}
                  </div>
                  <div>
                    <span className="font-medium">강의명:</span> {selectedCourse.courseName}
                  </div>
                  <div>
                    <span className="font-medium">강의시간:</span> {selectedCourse.startTime && selectedCourse.endTime ? `${selectedCourse.startTime} ~ ${selectedCourse.endTime}` : '미정'}
                  </div>
                  <div>
                    <span className="font-medium">강의실:</span> {selectedCourse.classInfo ? selectedCourse.classInfo.classCode : (selectedCourse.classId || '미정')}
                  </div>
                  <div>
                    <span className="font-medium">수업요일:</span> {selectedCourse.courseDays || '미정'}
                  </div>
                  <div>
                    <span className="font-medium">최대 수강인원:</span> {selectedCourse.maxCapacity || '미정'}
                  </div>
                  <div>
                    <span className="font-medium">최소 수강인원:</span> {selectedCourse.minCapacity || '미정'}
                  </div>
                  <div>
                    <span className="font-medium">개강일:</span> {selectedCourse.courseStartDay || '미정'}
                  </div>
                  <div>
                    <span className="font-medium">종강일:</span> {selectedCourse.courseEndDay || '미정'}
                  </div>
                </div>
              </div>

              {/* 강의 목표 */}
              {selectedCourse.syllabus?.courseGoal && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">강의 목표</h3>
                  <p className="text-sm text-gray-700 whitespace-pre-line">{selectedCourse.syllabus.courseGoal}</p>
                </div>
              )}

              {/* 학습 방법 */}
              {selectedCourse.syllabus?.learningMethod && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">학습 방법</h3>
                  <p className="text-sm text-gray-700 whitespace-pre-line">{selectedCourse.syllabus.learningMethod}</p>
                </div>
              )}

              {/* 평가 방법 */}
              {selectedCourse.syllabus?.evaluationMethod && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">평가 방법</h3>
                  <p className="text-sm text-gray-700">{selectedCourse.syllabus.evaluationMethod}</p>
                </div>
              )}

              {/* 교재 */}
              {selectedCourse.syllabus?.textbook && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">교재</h3>
                  <p className="text-sm text-gray-700">{selectedCourse.syllabus.textbook}</p>
                </div>
              )}

              {/* 과제 정책 */}
              {selectedCourse.syllabus?.assignmentPolicy && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">과제 정책</h3>
                  <p className="text-sm text-gray-700 whitespace-pre-line">{selectedCourse.syllabus.assignmentPolicy}</p>
                </div>
              )}

              {/* 지각 정책 */}
              {selectedCourse.syllabus?.latePolicy && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">지각 정책</h3>
                  <p className="text-sm text-gray-700 whitespace-pre-line">{selectedCourse.syllabus.latePolicy}</p>
                </div>
              )}

              {/* 기타 참고사항 */}
              {selectedCourse.syllabus?.etcNote && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">기타 참고사항</h3>
                  <p className="text-sm text-gray-700 whitespace-pre-line">{selectedCourse.syllabus.etcNote}</p>
                </div>
              )}

              {/* 주차별 계획 */}
              {selectedCourse.syllabus?.weeklyPlan && selectedCourse.syllabus.weeklyPlan.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">주차별 강의 계획</h3>
                  <div className="space-y-3">
                    {selectedCourse.syllabus.weeklyPlan.map((week) => (
                      <div key={week.weekNumber} className="border rounded-lg p-4">
                        <div className="flex items-center justify-between mb-2">
                          <h4 className="font-medium text-gray-900">
                            {week.weekNumber}주차: {week.weekTitle}
                          </h4>
                        </div>
                        <p className="text-sm text-gray-600">{week.weekContent}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* 강의 자료 */}
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-3">강의 자료</h3>
                {selectedCourse.materials && selectedCourse.materials.length > 0 ? (
                  <div className="space-y-3">
                    {selectedCourse.materials.map((material, index) => (
                      <div key={material.id || material.fileId || `material-${index}`} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg border">
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
                           size="sm"
                           onClick={() => handleDownloadMaterial(material)}
                           className="flex items-center space-x-1 text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                         >
                           <Download className="w-4 h-4" />
                           <span>다운로드</span>
                         </Button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="bg-gray-50 rounded-lg p-4 text-center border">
                    <FileText className="w-8 h-8 mx-auto mb-2 text-gray-400" />
                    <p className="text-gray-500">업로드된 강의 자료가 없습니다.</p>
                  </div>
                )}
              </div>
            </div>

            <div className="p-6 border-t bg-gray-50 flex justify-end gap-2">
              <Button onClick={() => setSelectedCourse(null)} 
                className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]">
                 닫기
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}