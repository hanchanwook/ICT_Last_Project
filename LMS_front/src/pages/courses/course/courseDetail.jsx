import { useState, useEffect } from "react"
import { ArrowLeft, BookOpen, Users, Calendar, Clock, DollarSign, User, Edit, UserPlus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { useParams, useNavigate } from "react-router-dom"
import { getAllCourse, getAdminStudentsByCourse } from "@/api/suhyeon/courseApi"
import { coursesMenuItems } from "@/components/ui/menuConfig"

/**
 * 과정 상세 정보 페이지
 * 선택된 과정의 상세 정보와 수강생 목록을 표시
 */
export default function CourseDetailPage() {
  const { id } = useParams() // URL 파라미터에서 과정 ID 추출
  const navigate = useNavigate()

  // 상태 관리
  const [course, setCourse] = useState(null); // 선택된 과정 정보
  const [formData, setFormData] = useState(null); // 편집 모드용 데이터
  const [students, setStudents] = useState([]); // 수강생 목록
  const [loadingStudents, setLoadingStudents] = useState(false); // 수강생 데이터 로딩 상태
  const [isEditing, setIsEditing] = useState(false) // 편집 모드 상태

  // 과정 정보 불러오기
  useEffect(() => {
    const fetchCourse = async () => {
      const allCourses = await getAllCourse();
      const found = allCourses.find(c => String(c.courseId) === String(id));
      setCourse(found);
      setFormData(found);
    }
    fetchCourse();
  }, [id]);

  // 해당 강의를 수강하는 학생 목록 조회
  useEffect(() => {
    const fetchStudents = async () => {
      if (!id) return;
      
      try {
        setLoadingStudents(true);
        const studentsData = await getAdminStudentsByCourse(id);
        setStudents(studentsData || []);
      } catch (error) {
        setStudents([]);
      } finally {
        setLoadingStudents(false);
      }
    };

    fetchStudents();
  }, [id]);

  /**
   * 과정 상태에 따른 색상 반환
   * @param {string} status - 과정 상태
   * @returns {string} 색상 코드
   */
  const getStatusColor = (status) => {
    switch (status) {
      case "진행중":
        return "#1ABC9C"
      case "모집중":
        return "#3498db"
      case "마감":
        return "#f39c12"
      case "완료":
        return "#95A5A6"
      default:
        return "#95A5A6"
    }
  }

  /**
   * 과정 수정 페이지로 이동
   */
  const handleEdit = () => {
    // 수정 페이지로 이동 (courseId 쿼리스트링으로 전달)
    navigate(`/courses/course/register?courseId=${id}`)
  }

  /**
   * 편집 모드 종료
   */
  const handleSave = () => {
    setIsEditing(false)
  }

  /**
   * 폼 데이터 변경 처리
   * @param {string} field - 변경할 필드명
   * @param {any} value - 변경할 값
   */
  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  return (
    <PageLayout currentPage="courses">
      <div className="flex">
        <Sidebar title="과정 관리" menuItems={coursesMenuItems} currentPath="/courses/course" />
        <main className="flex-1 p-8">
          <div className="max-w-7xl mx-auto">
            {/* 헤더 */}
            <div className="mb-8">
              <div className="flex items-center justify-between">
                <div className="flex-col items-center space-x-4">
                  {/* 뒤로가기 버튼 */}
                  <Button variant="ghost" size="sm" className="flex items-center space-x-2"
                  onClick={() => navigate("/courses/course")}>
                    <ArrowLeft className="w-4 h-4" />
                    <span>목록으로</span>
                  </Button>
                  <div>
                    <h1 className="text-2xl font-bold mt-2" style={{ color: "#2C3E50" }}>
                      과정 상세 정보
                    </h1>
                    <p className="text-lg" style={{ color: "#95A5A6" }}>
                      {course?.courseName} 과정의 상세 정보입니다.
                    </p>
                  </div>
                </div>
                {/* 액션 버튼 영역 */}
                <div className="flex items-center space-x-2">
                  {isEditing ? (
                    <>
                      <Button
                        onClick={handleEdit}
                        variant="outline"
                        className="flex items-center space-x-2 bg-transparent"
                        style={{ borderColor: "#95A5A6", color: "#95A5A6" }}
                      >
                        <span>취소</span>
                      </Button>
                      <Button
                        onClick={handleSave}
                        className="text-white font-medium flex items-center space-x-2"
                        style={{ backgroundColor: "#1ABC9C" }}
                      >
                        <span>저장</span>
                      </Button>
                    </>
                  ) : (
                    <>
                      <Button
                        onClick={handleEdit}
                        className="text-[#1ABC9C] border border-[#1ABC9C] font-medium flex items-center space-x-2 hover:bg-[#1ABC9C] hover:text-white"
                      >
                        <Edit className="w-4 h-4" />
                        <span>정보 수정</span>
                      </Button>
                    </>
                  )}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* 기본 정보 카드 */}
              <Card className="lg:col-span-1">
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50", fontSize: "1.2rem", fontWeight: "bold" }}>기본 정보</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  {/* 과정 아이콘 및 제목 */}
                  <div className="text-center mb-6">
                    <div
                      className="w-20 h-20 rounded-full mx-auto mb-4 flex items-center justify-center text-2xl font-bold text-white"
                      style={{ backgroundColor: "#1ABC9C" }}
                    >
                      <BookOpen className="w-8 h-8" />
                    </div>
                    {/* 편집 모드일 때는 입력 필드, 아닐 때는 텍스트 표시 */}
                    {isEditing ? (
                      <input
                        type="text"
                        value={formData?.courseName}
                        onChange={(e) => handleInputChange("name", e.target.value)}
                        className="text-xl font-bold text-center bg-transparent border-b-2 border-emerald-500 focus:outline-none mb-2"
                        style={{ color: "#2C3E50" }}
                      />
                    ) : (
                      <h3 className="text-xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                        {formData?.courseName}
                      </h3>
                    )}
                    <p className="text-sm mb-2" style={{ color: "#95A5A6" }}>
                      {course?.courseCode}
                    </p>
                    {/* 과정 상태 배지 */}
                    <Badge className="text-white" style={{ backgroundColor: getStatusColor(course?.status) }}>
                      {course?.status}
                    </Badge>
                  </div>

                                      {/* 과정 상세 정보 */}
                    <div className="space-y-3">
                      {/* 담당 강사 정보 */}
                      <div className="flex items-center space-x-3">
                        <User className="w-4 h-4" style={{ color: "#95A5A6" }} />
                        <div>
                          <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            담당 강사
                          </p>
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            {course?.memberName}
                          </p>
                        </div>
                      </div>

                      {/* 총 과정 시간 정보 */}
                      <div className="flex items-center space-x-3">
                        <DollarSign className="w-4 h-4" style={{ color: "#95A5A6" }} />
                        <div>
                          <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            총 과정 시간
                          </p>
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            {(() => {
                              if (course?.subjects && Array.isArray(course.subjects)) {
                                const totalHours = course.subjects.reduce((sum, subject) => 
                                  sum + (parseInt(subject.subjectTime) || 0), 0
                                )
                                return `${totalHours}시간`
                              }
                              return "0시간"
                            })()}
                          </p>
                        </div>
                      </div>

                      {/* 과정 기간 정보 */}
                      <div className="flex items-center space-x-3">
                        <Clock className="w-4 h-4" style={{ color: "#95A5A6" }} />
                        <div>
                          <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            과정 기간
                          </p>
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            {course?.courseStartDay} - {course?.courseEndDay}
                          </p>
                        </div>
                      </div>

                      {/* 수업 일정 정보 */}
                      <div className="flex items-center space-x-3">
                        <Calendar className="w-4 h-4" style={{ color: "#95A5A6" }} />
                        <div>
                          <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            수업 일정
                          </p>
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            요일 : {course?.courseDays} 
                          </p>
                          <p className="text-sm" style={{ color: "#95A5A6" }}>
                            시간 : {course?.startTime} - {course?.endTime}
                          </p>
                        </div>
                      </div>
                    </div>
                </CardContent>
              </Card>

              {/* 과정 설명 및 목표 */}
              <Card className="lg:col-span-2">
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50", fontSize: "1.2rem", fontWeight: "bold" }}>과정 소개</CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                  {/* 과목 정보 섹션 */}
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <h4 className="font-semibold" style={{ color: "#2C3E50" }}>
                        과목 정보
                      </h4>
                                             {/* 총 과목 시간 요약 */}
                       {Array.isArray(formData?.subjects) && formData.subjects.length > 0 && (
                         <div className="flex items-center space-x-2">
                           <Clock className="w-4 h-4" style={{ color: "#2C3E50" }} />
                           <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                             총 {formData.subjects.reduce((sum, subject) => 
                               sum + (parseInt(subject.subjectTime) || 0), 0
                             )}시간
                           </span>
                         </div>
                       )}
                    </div>
                    {/* 과목 목록 표시 */}
                    {Array.isArray(formData?.subjects) && formData.subjects.length > 0 ? (
                      <div className="max-h-80 overflow-y-auto">
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                          {formData.subjects.map((subject, index) => (
                            <div key={index} className="flex flex-col items-center justify-center p-6 bg-white rounded-xl shadow-lg transition-shadow duration-200">
                              <BookOpen className="w-8 h-8 mb-2" style={{ color: "#1ABC9C" }} />
                              <div className="text-xl font-bold mb-1" style={{ color: "#1ABC9C" }}>
                                {subject.subjectName}
                              </div>
                              <div className="text-sm text-center mb-2" style={{ color: "#95A5A6" }}>
                                {subject.subjectInfo}
                              </div>
                                                       {/* 과목 시간 정보 */}
                               <div className="flex items-center space-x-1">
                                 <Clock className="w-4 h-4" style={{ color: "#2C3E50" }} />
                                 <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                   {subject.subjectTime || 0}시간
                                 </span>
                               </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ) : (
                      <div className="text-center text-base py-8" style={{ color: "#95A5A6" }}>
                        과목 정보가 없습니다.
                      </div>
                    )}
                  </div>

                  {/* 수강생 정보 섹션 */}
                  <h4 className="font-semibold mb-3" style={{ color: "#2C3E50" }}>
                    수강생 정보
                  </h4>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {/* 현재 수강생 수 */}
                    <div className="flex flex-col items-center justify-center p-6 bg-white rounded-xl shadow-lg transition-shadow duration-200">
                      <Users className="w-8 h-8 mb-2" style={{ color: "#1ABC9C" }} />
                      <div className="text-3xl font-bold mb-1" style={{ color: "#1ABC9C" }}>
                        {students.length}
                      </div>
                      <div className="text-base" style={{ color: "#95A5A6" }}>
                        현재 수강생
                      </div>
                    </div>
                    {/* 최대 수강생 수 */}
                    <div className="flex flex-col items-center justify-center p-6 bg-white rounded-xl shadow-lg transition-shadow duration-200">
                      <Users className="w-8 h-8 mb-2" style={{ color: "#3498db" }} />
                      <div className="text-3xl font-bold mb-1" style={{ color: "#3498db" }}>
                        {course?.maxCapacity ?? 0}
                      </div>
                      <div className="text-base" style={{ color: "#95A5A6" }}>
                        최대 수강생
                      </div>
                    </div>
                    {/* 수강률 */}
                    <div className="flex flex-col items-center justify-center p-6 bg-white rounded-xl shadow-lg transition-shadow duration-200">
                      <Users className="w-8 h-8 mb-2" style={{ color: "#1ABC9C" }} />
                      <div className="text-3xl font-bold mb-1" style={{ color: "#1ABC9C" }}>
                        {course?.maxCapacity
                          ? `${Math.round((students.length / course?.maxCapacity) * 100)}%`
                          : "0%"}
                      </div>
                      <div className="text-base" style={{ color: "#95A5A6" }}>
                        수강률
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* 수강생 목록 */}
              <Card className="lg:col-span-3">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle style={{ color: "#2C3E50" }}>
                      수강생 목록 ({students.length}명)
                    </CardTitle>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b" style={{ borderColor: "#95A5A6" }}>
                          <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                            NO
                          </th>
                          <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                            이름
                          </th>
                          <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                            이메일
                          </th>
                          <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                            연락처
                          </th>
                          <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                            등록일
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {/* 로딩 상태 표시 */}
                        {loadingStudents ? (
                          <tr>
                            <td colSpan="5" className="text-center py-8">
                              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-4"></div>
                              <p style={{ color: "#95A5A6" }}>학생 목록을 불러오는 중...</p>
                            </td>
                          </tr>
                        ) : students.length > 0 ? (
                          // 수강생 목록 표시
                          students.map((student, index) => (
                            <tr key={student.memberId} className="border-b hover:bg-gray-50" style={{ borderColor: "#f1f2f6" }}>
                              <td className="py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                                {index + 1}
                              </td>
                              <td className="py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                                {student.memberName}
                              </td>
                              <td className="py-3 px-4" style={{ color: "#95A5A6" }}>
                                {student.memberEmail}
                              </td>
                              <td className="py-3 px-4" style={{ color: "#95A5A6" }}>
                                {student.memberPhone}
                              </td>
                              <td className="py-3 px-4 text-center text-sm" style={{ color: "#95A5A6" }}>
                                {student.createdAt}
                              </td>
                            </tr>
                          ))
                        ) : (
                          // 수강생이 없을 때 표시
                          <tr>
                            <td colSpan="5" className="text-center py-8">
                              <Users className="w-16 h-16 mx-auto mb-4" style={{ color: "#95A5A6" }} />
                              <h3 className="text-xl font-semibold mb-2" style={{ color: "#2C3E50" }}>
                                등록된 수강생이 없습니다
                              </h3>
                              <p style={{ color: "#95A5A6" }}>수강생을 추가해보세요.</p>
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </main>
      </div>
    </PageLayout>
  )
} 