import { useState, useEffect } from "react"
import Header from "@/components/layout/header"
import Sidebar from "@/components/layout/sidebar"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Search, Calendar, Clock, Users, MapPin, BookOpen, CheckCircle } from "lucide-react"
import { Link } from "react-router-dom"
import { getInstructorLectures, getLectureStats, getCourseMaterials, getClassInfo } from "@/api/sunghyun/instructorCourseApi"
import { getMenuItems } from "@/components/ui/menuConfig"

export default function InstructorLecturesPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedPeriod, setSelectedPeriod] = useState("all")
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [lectures, setLectures] = useState([])
  const [stats, setStats] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [currentUser, setCurrentUser] = useState(null)
  const [retryCount, setRetryCount] = useState(0)
  const [isRetrying, setIsRetrying] = useState(false)

  // 사이드바 메뉴 구성
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

  // API에서 데이터 가져오기
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true)
        setError(null)
        setIsRetrying(false)
        
        console.log('🚀 API 호출 시작 - 쿠키 기반 인증')
        console.log('👤 현재 사용자:', currentUser)
        
        // 사용자 인증 상태 확인
        if (!currentUser || !currentUser.memberId) {
          throw new Error('사용자 인증 정보가 없습니다. 다시 로그인해주세요.');
        }
        
        const [lecturesData, statsData] = await Promise.all([
          getInstructorLectures(),
          getLectureStats()
        ])
        
        console.log('강의 데이터 응답:', lecturesData)
        if (Array.isArray(lecturesData) && lecturesData.length === 0) {
          console.warn('경고: 강의 데이터가 빈 배열입니다. (DB에 데이터가 없거나, 쿼리 조건이 맞지 않을 수 있음)');
        }
        console.log('통계 데이터 응답:', statsData)
        
        // courseActive가 1인 강의는 제외
        const filteredLecturesData = lecturesData.filter(lecture => {
          const isActive = lecture.courseActive === 1 || lecture.courseActive === true;
          if (isActive) {
            console.log(`강의 ${lecture.courseId} (${lecture.courseName}) - courseActive: ${lecture.courseActive} - 제외됨`);
          }
          return !isActive;
        });
        
        console.log('필터링된 강의 데이터:', filteredLecturesData);
        
        // 각 강의의 자료 개수와 강의실 정보 가져오기
        const lecturesWithDetails = await Promise.all(
          filteredLecturesData.map(async (lecture) => {
            try {
              // 강의 자료 개수 가져오기
              let materialsCount = 0
              try {
                const materialsResponse = await getCourseMaterials(lecture.courseId)
                console.log(`강의 ${lecture.courseId} 자료 응답:`, materialsResponse)
                
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
                console.warn(`강의 ${lecture.courseId} 자료 조회 실패:`, materialsError)
                materialsCount = 0
              }
              
              // 강의실 정보 가져오기
              let classInfo = null
              if (lecture.classId) {
                try {
                  console.log(`강의실 정보 조회 시작: ${lecture.classId}`)
                  classInfo = await getClassInfo(lecture.classId)
                  console.log(`강의실 ${lecture.classId} 정보:`, classInfo)
                  
                  // classInfo 구조 확인
                  if (classInfo && typeof classInfo === 'object') {
                    console.log('classInfo 구조:', {
                      classId: classInfo.classId,
                      classCode: classInfo.classCode,
                      className: classInfo.className
                    })
                  }
                } catch (classError) {
                  console.warn(`강의실 ${lecture.classId} 정보 조회 실패:`, classError)
                  console.warn('에러 상세:', classError.response?.data)
                  
                  // UUID인 경우 기본값 설정
                  const isUUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(lecture.classId)
                  classInfo = {
                    classId: lecture.classId,
                    classCode: isUUID ? '미정' : `강의실${lecture.classId}`,
                    className: isUUID ? '강의실 미정' : `강의실 ${lecture.classId}`
                  }
                }
              }
              
              return {
                ...lecture,
                materialsCount: materialsCount,
                classInfo: classInfo
              }
            } catch (error) {
              console.warn(`강의 ${lecture.courseId} 상세 정보 조회 실패:`, error)
              
              // UUID인 경우 기본값 설정
              const isUUID = lecture.classId ? /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(lecture.classId) : false
              
              return {
                ...lecture,
                materialsCount: 0,
                classInfo: lecture.classId ? {
                  classId: lecture.classId,
                  classCode: isUUID ? '미정' : `강의실${lecture.classId}`,
                  className: isUUID ? '강의실 미정' : `강의실 ${lecture.classId}`
                } : null
              }
            }
          })
        )
        
        console.log('상세 정보가 포함된 강의 데이터:', lecturesWithDetails)
        
        setLectures(lecturesWithDetails)
        setStats(statsData)
        setRetryCount(0) // 성공 시 재시도 카운트 리셋
      } catch (err) {
        console.error('❌ 데이터 로딩 오류:', err);
        
        // 에러 응답 상세 정보 로깅
        if (err.response) {
          console.error('🔍 에러 응답 전체:', err.response);
          console.error('🔍 에러 응답 데이터:', err.response.data);
          console.error('🔍 에러 상태 코드:', err.response.status);
        }
        
        // 사용자 친화적인 오류 메시지 설정
        let errorMessage = err.message;
        if (err.response?.status === 401) {
          errorMessage = '인증이 만료되었습니다. 다시 로그인해주세요.';
        } else if (err.response?.status === 403) {
          errorMessage = '접근 권한이 없습니다.';
        } else if (err.response?.status === 404) {
          errorMessage = '요청한 데이터를 찾을 수 없습니다.';
        } else if (err.response?.status === 500) {
          errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        } else if (err.code === 'NETWORK_ERROR') {
          errorMessage = '네트워크 연결을 확인해주세요.';
        }
        
        setError(errorMessage)
      } finally {
        setLoading(false)
      }
    }

    if (currentUser) {
      fetchData()
    }
  }, [currentUser, retryCount])

  // 재시도 함수
  const handleRetry = () => {
    setRetryCount(prev => prev + 1)
    setIsRetrying(true)
  }

  // 강의 상태를 판단하는 함수
  const getLectureStatus = (lecture) => {
    const now = new Date()
    const startDate = lecture.courseStartDay ? new Date(lecture.courseStartDay) : null
    const endDate = lecture.courseEndDay ? new Date(lecture.courseEndDay) : null
    
    if (!startDate) return '예정'
    
    if (now < startDate) {
      return '예정'
    } else if (now >= startDate && now <= endDate) {
      return '진행중'
    } else {
      return '완료'
    }
  }

  const filteredLectures = lectures.filter((lecture) => {
    const matchesSearch =
      lecture.courseName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      lecture.courseCode?.toLowerCase().includes(searchTerm.toLowerCase())

    const matchesPeriod = selectedPeriod === "all" || 
      (lecture.courseStartDay && new Date(lecture.courseStartDay).getFullYear().toString() === selectedPeriod)
    
    const lectureStatus = getLectureStatus(lecture)
    const matchesStatus = selectedStatus === "all" || lectureStatus === selectedStatus

    return matchesSearch && matchesPeriod && matchesStatus
  })

  const getStatusColor = (status) => {
    switch (status) {
      case "완료":
        return "bg-[#e4f5eb] text-[#1abc9c]"
      case "예정":
        return "bg-[#EFF6FF] text-[#3498db]"
      case "진행중":
        return "bg-[#EFF6FF] text-[#b0c4de]"
      case "취소":
        return "bg-red-100 text-red-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }



  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header currentPage="courses" userRole="instructor" userName={currentUser?.name || "강사"} />

        <div className="flex">
          <Sidebar title="과정 관리" menuItems={sidebarItems} currentPath="/instructor/courses/lectures" />
          <main className="flex-1 p-6">
            <div className="max-w-7xl mx-auto">
              <div className="flex flex-col items-center justify-center h-64 space-y-4">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                <div className="text-lg text-gray-600">
                  {isRetrying ? '데이터를 다시 불러오는 중...' : '데이터를 불러오는 중...'}
                </div>
                {isRetrying && (
                  <div className="text-sm text-gray-500">
                    재시도 횟수: {retryCount}회
                  </div>
                )}
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
              <div className="flex flex-col items-center justify-center h-64 space-y-6">
                <div className="text-center">
                  <div className="text-6xl mb-4">⚠️</div>
                  <h2 className="text-xl font-semibold text-gray-800 mb-2">데이터 로드 실패</h2>
                  <p className="text-red-600 mb-4">{error}</p>
                  
                  {/* 재시도 횟수 표시 */}
                  {retryCount > 0 && (
                    <p className="text-sm text-gray-500 mb-4">
                      재시도 횟수: {retryCount}회
                    </p>
                  )}
                </div>
                
                <div className="flex space-x-4">
                  {/* 재시도 버튼 */}
                  <Button 
                    onClick={handleRetry}
                    disabled={isRetrying}
                    className="bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400"
                  >
                    {isRetrying ? '재시도 중...' : '다시 시도'}
                  </Button>
                  
                  {/* 인증 관련 오류인 경우 로그인 페이지로 이동 */}
                  {(error.includes('인증') || error.includes('로그인') || error.includes('권한')) && (
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
                
                {/* 디버깅 정보 (개발 모드에서만 표시) */}
                {import.meta.env.DEV && (
                  <details className="mt-4 text-left">
                    <summary className="cursor-pointer text-sm text-gray-500 hover:text-gray-700">
                      디버깅 정보 보기
                    </summary>
                    <div className="mt-2 p-3 bg-gray-100 rounded text-xs">
                      <p><strong>사용자 ID:</strong> {currentUser?.memberId || '없음'}</p>
                      <p><strong>재시도 횟수:</strong> {retryCount}</p>
                      <p><strong>오류 메시지:</strong> {error}</p>
                    </div>
                  </details>
                )}
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
            {/* 페이지 헤더 */}
            <div className="mb-6">
              <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                담당 강의 목록
              </h1>
              <p className="text-gray-600">과거부터 현재까지 담당하신 모든 강의를 확인하세요.</p>
            </div>

            {/* 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600">총 강의 수</p>
                      <p className="text-3xl font-bold" style={{ color: "#3498db" }}>
                        {lectures.length}회
                      </p>
                    </div>
                    <div className="bg-[#EFF6FF] rounded-full p-3 mr-3">
                      <BookOpen className="w-10 h-10" style={{ color: "#3498db" }} />
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600">완료된 강의</p>
                      <p className="text-3xl font-bold" style={{ color: "#1abc9c" }}>
                        {lectures.filter((l) => getLectureStatus(l) === '완료').length}회
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
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600">진행중인 강의</p>
                      <p className="text-3xl font-bold" style={{ color: "#b0c4de" }}>
                        {lectures.filter((l) => getLectureStatus(l) === '진행중').length}회
                      </p>
                    </div>
                    <div className="bg-[#EFF6FF] rounded-full p-3 mr-3">
                      <Calendar className="w-10 h-10" style={{ color: "#b0c4de" }} />
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="ml-4">
                      <p className="text-m font-medium text-gray-600">예정된 강의</p>
                      <p className="text-3xl font-bold" style={{ color: "#415e7a" }}>
                        {lectures.filter((l) => getLectureStatus(l) === '예정').length}회
                      </p>
                    </div>
                    <div className="bg-[#e8eef3] rounded-full p-3 mr-3">
                      <Users className="w-10 h-10" style={{ color: "#415e7a" }} />
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* 검색 및 필터 */}
            <div className="bg-white p-4 rounded-lg shadow-sm border mb-6">
              <div className="flex flex-col md:flex-row gap-4">
                <div className="flex-1 relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                  <input
                    type="text"
                    placeholder="과정명, 과정코드, 강의명으로 검색..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <select
                  value={selectedPeriod}
                  onChange={(e) => setSelectedPeriod(e.target.value)}
                  className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="all">전체 연도</option>
                  {Array.from(new Set(lectures.map(l => l.courseStartDay && new Date(l.courseStartDay).getFullYear()))).filter(year => year).map(year => (
                    <option key={year} value={year}>{year}년</option>
                  ))}
                </select>
                <select
                  value={selectedStatus}
                  onChange={(e) => setSelectedStatus(e.target.value)}
                  className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
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
              {lectures.length === 0 ? (
                <div className="p-8 text-center">
                  <div className="text-gray-500 text-lg mb-2">등록된 강의가 없습니다.</div>
                  <div className="text-gray-400 text-sm">담당하신 강의가 등록되면 여기에 표시됩니다.</div>
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
                      {filteredLectures.map((lecture) => (
                        <tr key={lecture.courseId} className="hover:bg-gray-50">
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div>
                              <div className="text-sm font-medium text-gray-900">{lecture.courseName}</div>
                              <div className="text-sm text-gray-500">
                                {lecture.courseCode}
                              </div>
                              <div className="text-sm text-gray-500">
                                {lecture.courseStartDay && new Date(lecture.courseStartDay).toLocaleDateString()} ~ {lecture.courseEndDay && new Date(lecture.courseEndDay).toLocaleDateString()}
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center mb-1">
                              <Calendar className="w-4 h-4 text-gray-400 mr-2" />
                              <span className="text-sm text-gray-900">
                                {lecture.courseStartDay && new Date(lecture.courseStartDay).toLocaleDateString()}
                              </span>
                            </div>
                            <div className="flex items-center mb-1">
                              <Clock className="w-4 h-4 text-gray-400 mr-2" />
                              <span className="text-sm text-gray-900">
                                {lecture.courseDays ? (
                                  `${lecture.courseDays} ${lecture.startTime || '00:00'}~${lecture.endTime || '00:00'}`
                                ) : (
                                  '시간 미정'
                                )}
                              </span>
                            </div>
                            <div className="flex items-center">
                              <MapPin className="w-4 h-4 text-gray-400 mr-2" />
                              <span className="text-sm text-gray-900">
                                {lecture.classInfo ? lecture.classInfo.classCode : (lecture.classId || '미정')}
                              </span>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm text-gray-900">
                              {lecture.materialsCount > 0 ? (
                                <span className="text-blue-600 font-medium">
                                  {lecture.materialsCount}개
                                </span>
                              ) : (
                                <span className="text-gray-500">없음</span>
                              )}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <span
                              className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(getLectureStatus(lecture))}`}
                            >
                              {getLectureStatus(lecture)}
                            </span>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex space-x-2">
                              {(() => {
                                const lectureId = lecture.courseId
                                console.log('강의 ID 확인:', { 
                                  courseId: lecture.courseId,
                                  finalId: lectureId 
                                })
                                
                                if (lectureId) {
                                  return (
                                    <Link to={`/instructor/courses/lectures/${lectureId}`}>
                                      <Button size="sm" variant="outline" 
                                      className="text-[#1abc9c] border border-[#1abc9c]
                                      hover:bg-[#1abc9c] hover:text-white">
                                        상세보기
                                      </Button>
                                    </Link>
                                  )
                                } else {
                                  return (
                                    <Button 
                                      size="sm" 
                                      variant="outline" 
                                      className="text-[#1abc9c] border border-[#1abc9c]
                                      hover:bg-[#1abc9c] hover:text-white"
                                      onClick={() => {
                                        console.log('강의 데이터:', lecture)
                                        alert('강의 ID가 없습니다.')
                                      }}
                                    >
                                      상세보기
                                    </Button>
                                  )
                                }
                              })()}
                              {lecture.status === "예정" && (
                                <Button size="sm" variant="outline" className="text-xs bg-transparent">
                                  수정
                                </Button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* 강의 노트 섹션 */}
            {lectures.filter((l) => l.notes && l.status === "완료").length > 0 && (
              <div className="mt-6 bg-white rounded-lg shadow-sm border p-6">
                <h3 className="text-lg font-semibold mb-4" style={{ color: "#2C3E50" }}>
                  최근 강의 노트
                </h3>
                <div className="space-y-4">
                  {lectures
                    .filter((l) => l.notes && l.status === "완료")
                    .slice(0, 3)
                    .map((lecture) => (
                      <div key={lecture.courseId} className="border-l-4 border-blue-500 pl-4 py-2">
                        <div className="flex justify-between items-start">
                          <div>
                            <h4 className="font-medium text-gray-900">{lecture.lectureTitle}</h4>
                            <p className="text-sm text-gray-600">
                              {lecture.courseName} | {lecture.date}
                            </p>
                            <p className="text-sm text-gray-700 mt-1">{lecture.notes}</p>
                          </div>
                        </div>
                      </div>
                    ))}
                </div>
              </div>
            )}

            {/* 안내사항 */}
            <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
              <h3 className="text-sm font-medium text-blue-800 mb-2">강의 관리 안내</h3>
              <ul className="text-sm text-blue-700 space-y-1">
                <li>• 강의 완료 후에는 출석 체크와 강의 노트를 작성해주세요.</li>
                <li>• 강의 자료는 사전에 업로드하여 학생들이 미리 확인할 수 있도록 해주세요.</li>
                <li>• 예정된 강의는 수정이 가능하며, 변경사항은 즉시 학생들에게 알림이 발송됩니다.</li>
              </ul>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
