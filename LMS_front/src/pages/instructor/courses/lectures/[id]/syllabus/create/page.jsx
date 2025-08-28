import { useState, useEffect } from "react"
import { useParams, useNavigate } from "react-router-dom"
import { Link } from "react-router-dom"
import { ArrowLeft, Save, X, Plus, Minus, AlertCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import Header from "@/components/layout/header"
import Sidebar from "@/components/layout/sidebar"
import { 
  createLecturePlan, 
  updateLecturePlan, 
  getLecturePlan, 
  getWeeklyplan,
  deleteLecturePlan,
  getAllSubjects,
  getAllSubjectDetails
} from "@/api/sunghyun/instructorCourseApi"
import { getMenuItems } from "@/components/ui/menuConfig"

export default function SyllabusCreatePage() {
  const { id: lectureId } = useParams()
  const navigate = useNavigate()

  // lectureplan 테이블에 저장할 데이터
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

  // weeklyPlan 테이블에 저장할 데이터
  const [weeklyplan, setWeeklyplan] = useState([])
  
  // 선택된 과목들을 관리하는 상태
  const [selectedSubjects, setSelectedSubjects] = useState({})
  const [selectedSubjectDetails, setSelectedSubjectDetails] = useState({})

  const [isLoading, setIsLoading] = useState(false)
  const [errors, setErrors] = useState({})
  const [existingPlanId, setExistingPlanId] = useState(null) // 기존 계획서가 있는지 확인
  
  // 과목 및 세부과목 데이터
  const [subjects, setSubjects] = useState([])
  const [subjectDetails, setSubjectDetails] = useState([])
  const [isLoadingSubjects, setIsLoadingSubjects] = useState(false)

  // 사이드바 메뉴 구성
  const sidebarItems = getMenuItems('instructor-courses')

  // 강의 정보 (실제로는 API에서 가져올 데이터)
  const [lecture, setLecture] = useState({
    courseName: "",
    courseCode: "",
    lectureTitle: "",
    instructor: "",
  })

  // 과목 및 세부과목 데이터 불러오기
  useEffect(() => {
    const loadSubjects = async () => {
      try {
        setIsLoadingSubjects(true)
        const [subjectsData, detailsData] = await Promise.all([
          getAllSubjects(),
          getAllSubjectDetails()
        ])
        
        // 과목 중복 제거: subjectId를 기준으로 중복 제거
        const uniqueSubjects = subjectsData.filter((subject, index, self) => 
          index === self.findIndex(s => s.subjectId === subject.subjectId)
        )
        
        setSubjects(uniqueSubjects)
        setSubjectDetails(detailsData)
        
        console.log('원본 과목 데이터:', subjectsData.length, '개')
        console.log('중복 제거 후 과목 데이터:', uniqueSubjects.length, '개')
        console.log('세부과목 데이터 샘플:', detailsData.slice(0, 3))
        console.log('과목 데이터 샘플:', uniqueSubjects.slice(0, 3))
      } catch (error) {
        console.error('과목 데이터 로딩 오류:', error)
        // 에러가 발생해도 계속 진행
      } finally {
        setIsLoadingSubjects(false)
      }
    }
    
    loadSubjects()
  }, [])

  // 기존 강의 계획서가 있는지 확인
  useEffect(() => {
    const checkExistingLectureplan = async () => {
      try {
        const existingLectureplan = await getLecturePlan(lectureId)
        if (existingLectureplan && existingLectureplan.planId) {
          setExistingPlanId(existingLectureplan.planId)
          // 기존 데이터로 폼 초기화
          setLectureplanData({
            planTitle: existingLectureplan.planTitle || "",
            planContent: existingLectureplan.planContent || "",
            courseGoal: existingLectureplan.courseGoal || "",
            learningMethod: existingLectureplan.learningMethod || "",
            evaluationMethod: existingLectureplan.evaluationMethod || "",
            textbook: existingLectureplan.textbook || "",
            weekCount: existingLectureplan.weekCount || 15,
            assignmentPolicy: existingLectureplan.assignmentPolicy || "",
            latePolicy: existingLectureplan.latePolicy || "",
            etcNote: existingLectureplan.etcNote || "",
          })
          
          // 주차별 계획 별도 조회
          try {
            const weeklyplanData = await getWeeklyplan(existingLectureplan.planId)
            if (weeklyplanData && weeklyplanData.length > 0) {
              // 기존 주차별 계획 데이터로 설정
              setWeeklyplan(weeklyplanData)
            } else {
              // 주차별 계획이 없으면 주차 수에 맞게 빈 데이터로 초기화
              const initialWeeklyplan = Array.from({ length: existingLectureplan.weekCount || 15 }, (_, index) => ({
                weekNumber: index + 1,
                weekTitle: "",
                weekContent: "",
                subjectIds: [], // 배열로 변경
                subDetailIds: [], // 배열로 변경
              }))
              setWeeklyplan(initialWeeklyplan)
            }
          } catch (weeklyplanError) {
            console.warn("주차별 계획 조회 중 오류:", weeklyplanError)
            // 주차별 계획 조회 실패 시 빈 데이터로 초기화
            const initialWeeklyplan = Array.from({ length: existingLectureplan.weekCount || 15 }, (_, index) => ({
              weekNumber: index + 1,
              weekTitle: "",
              weekContent: "",
              subjectIds: [], // 배열로 변경
              subDetailIds: [], // 배열로 변경
            }))
            setWeeklyplan(initialWeeklyplan)
          }
        }
      } catch (error) {
        // 404 에러는 기존 계획서가 없는 것으로 처리
        if (error.response?.status === 404) {
          console.log("기존 강의 계획서가 없습니다. 새로 작성합니다.")
        } else {
          // 다른 에러는 콘솔에 로그 출력
          console.error("강의 계획서 조회 중 오류 발생:", error)
        }
      }
    }

    checkExistingLectureplan()
  }, [lectureId])

  useEffect(() => {
    // 주차별 계획 초기화 - 기존 데이터가 있으면 유지, 없으면 새로 생성
    if (weeklyplan.length === 0) {
      const initialWeeklyplan = Array.from({ length: lectureplanData.weekCount }, (_, index) => ({
        weekNumber: index + 1,
        weekTitle: "",
        weekContent: "",
        subjectIds: [], // 배열로 변경
        subDetailIds: [], // 배열로 변경
      }))
      setWeeklyplan(initialWeeklyplan)
    }
  }, [lectureplanData.weekCount, weeklyplan.length])

  const handleInputChange = (field, value) => {
    setLectureplanData((prev) => ({ ...prev, [field]: value }))
    // 에러 제거
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: "" }))
    }
  }

  const handleWeeklyplanChange = (weekIndex, field, value) => {
    const updatedWeeklyplan = [...weeklyplan]
    updatedWeeklyplan[weekIndex] = { ...updatedWeeklyplan[weekIndex], [field]: value }
    setWeeklyplan(updatedWeeklyplan)
  }

  const handleWeekCountChange = (weeks) => {
    const newWeeks = Math.max(1, Math.min(20, weeks))
    const currentWeekCount = weeklyplan.length
    
    if (newWeeks > currentWeekCount) {
      // 주차 수가 늘어나면 새로운 주차 추가
      const additionalWeeks = Array.from({ length: newWeeks - currentWeekCount }, (_, index) => ({
        weekNumber: currentWeekCount + index + 1,
        weekTitle: "",
        weekContent: "",
        subjectIds: [], // 배열로 변경
        subDetailIds: [], // 배열로 변경
      }))
      setWeeklyplan([...weeklyplan, ...additionalWeeks])
    } else if (newWeeks < currentWeekCount) {
      // 주차 수가 줄어들면 뒤에서부터 제거
      setWeeklyplan(weeklyplan.slice(0, newWeeks))
    }
    
    setLectureplanData((prev) => ({ ...prev, weekCount: newWeeks }))
  }

  // 과목 선택 처리
  const handleSubjectSelection = (weekIndex, subjectId, isChecked) => {
    const updatedWeeklyplan = [...weeklyplan]
    const currentWeek = updatedWeeklyplan[weekIndex]
    
    // subjectIds와 subDetailIds가 undefined인 경우 빈 배열로 초기화
    if (!currentWeek.subjectIds) {
      currentWeek.subjectIds = []
    }
    if (!currentWeek.subDetailIds) {
      currentWeek.subDetailIds = []
    }
    
    if (isChecked) {
      // 과목 추가
      if (!currentWeek.subjectIds.includes(subjectId)) {
        currentWeek.subjectIds = [...currentWeek.subjectIds, subjectId]
      }
    } else {
      // 과목 제거
      currentWeek.subjectIds = currentWeek.subjectIds.filter(id => id !== subjectId)
      // 해당 과목의 세부과목들도 함께 제거
      const selectedSubject = subjects.find(s => s.subjectId === subjectId)
      if (selectedSubject) {
        const subjectDetailsToRemove = subjectDetails.filter(d => 
          d.subjectId === subjectId || d.subject_id === subjectId
        )
        subjectDetailsToRemove.forEach(detail => {
          const detailId = detail.subDetailld || detail.subDetailId
          currentWeek.subDetailIds = currentWeek.subDetailIds.filter(id => id !== detailId)
        })
      }
    }
    
    // 선택된 과목들의 이름을 제목에 추가
    const selectedSubjectNames = currentWeek.subjectIds
      .map(id => subjects.find(s => s.subjectId === id)?.subjectName)
      .filter(name => name)
      .join(', ')
    
    currentWeek.weekTitle = selectedSubjectNames
    
    // 선택된 세부과목들의 이름을 내용에 추가
    const selectedDetailNames = currentWeek.subDetailIds
      .map(id => subjectDetails.find(d => 
        (d.subDetailld === id || d.subDetailId === id)
      )?.subDetailName)
      .filter(name => name)
      .join(', ')
    
    currentWeek.weekContent = selectedDetailNames
    
    setWeeklyplan(updatedWeeklyplan)
  }

  // 세부과목 선택 처리
  const handleSubjectDetailSelection = (weekIndex, subDetailId, isChecked) => {
    const updatedWeeklyplan = [...weeklyplan]
    const currentWeek = updatedWeeklyplan[weekIndex]
    
    // subDetailIds가 undefined인 경우 빈 배열로 초기화
    if (!currentWeek.subDetailIds) {
      currentWeek.subDetailIds = []
    }
    
    if (isChecked) {
      // 세부과목 추가
      if (!currentWeek.subDetailIds.includes(subDetailId)) {
        currentWeek.subDetailIds = [...currentWeek.subDetailIds, subDetailId]
      }
    } else {
      // 세부과목 제거
      currentWeek.subDetailIds = currentWeek.subDetailIds.filter(id => id !== subDetailId)
    }
    
    // 선택된 세부과목들의 이름을 내용에 추가
    const selectedDetailNames = currentWeek.subDetailIds
      .map(id => subjectDetails.find(d => 
        (d.subDetailld === id || d.subDetailId === id)
      )?.subDetailName)
      .filter(name => name)
      .join(', ')
    
    currentWeek.weekContent = selectedDetailNames
    
    setWeeklyplan(updatedWeeklyplan)
  }



  const validateForm = () => {
    const newErrors = {}

    // 필수 항목 검증 - null, undefined, 빈 문자열 모두 체크
    if (!lectureplanData.planTitle || !lectureplanData.planTitle.trim()) {
      newErrors.planTitle = "강의계획서 제목을 입력해주세요."
    }
    if (!lectureplanData.planContent || !lectureplanData.planContent.trim()) {
      newErrors.planContent = "강의계획서 내용을 입력해주세요."
    }
    if (!lectureplanData.courseGoal || !lectureplanData.courseGoal.trim()) {
      newErrors.courseGoal = "전체 수업 목표를 입력해주세요."
    }

    // 주차별 계획 검증 - 모든 주차가 입력되어야 함
    const weekCount = lectureplanData.weekCount
    const filledWeeks = weeklyplan.filter(week => 
      week.weekTitle && week.weekTitle.trim() && week.weekContent && week.weekContent.trim()
    ).length

    if (filledWeeks !== weekCount) {
      newErrors.weeklyplan = `모든 주차를 입력해주세요. (${filledWeeks}/${weekCount}주차 입력됨)`
    }

    // 각 주차별로 제목과 내용이 모두 있는지 확인
    const incompleteWeeks = weeklyplan
      .map((week, index) => ({ week, index }))
      .filter(({ week }) => {
        const hasTitle = week.weekTitle && week.weekTitle.trim()
        const hasContent = week.weekContent && week.weekContent.trim()
        return (hasTitle && !hasContent) || (!hasTitle && hasContent)
      })

    if (incompleteWeeks.length > 0) {
      const weekNumbers = incompleteWeeks.map(({ week }) => week.weekNumber).join(', ')
      newErrors.weeklyplan = `다음 주차의 제목과 내용을 모두 입력해주세요: ${weekNumbers}주차`
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSave = async () => {
    if (!validateForm()) {
      // 에러 메시지 표시
      if (errors.weeklyplan) {
        alert(errors.weeklyplan)
      } else if (errors.planTitle || errors.planContent || errors.courseGoal) {
        const errorMessages = []
        if (errors.planTitle) errorMessages.push(errors.planTitle)
        if (errors.planContent) errorMessages.push(errors.planContent)
        if (errors.courseGoal) errorMessages.push(errors.courseGoal)
        alert("다음 필수 항목을 입력해주세요:\n" + errorMessages.join('\n'))
      } else {
        alert("필수 항목을 모두 입력해주세요.")
      }
      return
    }

    setIsLoading(true)
    try {
      // 주차별 계획 데이터 준비 - 모든 주차 데이터 포함
      const weeklyplanData = weeklyplan.map((week, index) => {
        // weekNumber가 없거나 잘못된 경우 인덱스 기반으로 설정
        let weekNumber = week.weekNumber || index + 1;
        
        // weekNumber를 명시적으로 숫자로 변환
        weekNumber = parseInt(weekNumber, 10);
        if (isNaN(weekNumber) || weekNumber <= 0) {
          weekNumber = index + 1;
        }
        
        // 주차 번호가 1 이상인지 확인
        if (weekNumber < 1) {
          weekNumber = index + 1;
        }
        
        return {
          weekNumber: weekNumber,
          weekTitle: week.weekTitle?.trim() || "", // 빈 문자열로 변환
          title: week.weekTitle?.trim() || "", // 백엔드에서 기대할 수 있는 필드명 추가
          weekContent: week.weekContent?.trim() || "", // 빈 문자열로 변환
          content: week.weekContent?.trim() || "", // 백엔드에서 기대할 수 있는 필드명 추가
          subjectIds: Array.isArray(week.subjectIds) ? week.subjectIds : [],
          subDetailIds: Array.isArray(week.subDetailIds) ? week.subDetailIds : [],
        };
      }).filter(week => week.weekNumber > 0); // 유효한 주차 번호만 필터링

      console.log('페이지에서 보내는 주차별 계획 데이터:', weeklyplanData);
      console.log('강의 계획서 데이터:', lectureplanData);
      console.log('주차별 계획 데이터 검증:', weeklyplanData.map(week => ({
        weekNumber: week.weekNumber,
        weekNumberType: typeof week.weekNumber,
        hasWeekNumber: typeof week.weekNumber === 'number' && week.weekNumber > 0,
        weekTitle: week.weekTitle,
        weekContent: week.weekContent
      })));
      console.log('원본 weeklyplan 상태:', weeklyplan);

      // 주차별 계획 데이터가 비어있는지 확인
      if (!weeklyplanData || weeklyplanData.length === 0) {
        console.warn('주차별 계획 데이터가 비어있습니다.');
      }

      // courseId를 lectureplanData에 넣지 않고, createSyllabus의 첫 번째 인자로만 전달
      let result
      
      if (existingPlanId) {
        // 기존 계획서 수정
        result = await updateLecturePlan(existingPlanId, lectureplanData, weeklyplanData)
        alert("강의계획서가 성공적으로 수정되었습니다.")
      } else {
        // 새 계획서 등록
        result = await createLecturePlan(lectureId, lectureplanData, weeklyplanData)
        alert("강의계획서가 성공적으로 등록되었습니다.")
      }
      
      console.log("저장 결과:", result)
      navigate(`/instructor/courses/lectures/${lectureId}`)
    } catch (error) {
      console.error("저장 중 오류:", error)
      console.error("오류 응답 데이터:", error.response?.data)
      console.error("오류 상태:", error.response?.status)
      console.error("오류 메시지:", error.message)
      
      // 더 구체적인 에러 메시지 제공
      let errorMessage = "저장 중 오류가 발생했습니다."
      
      if (error.response) {
        // 서버에서 응답이 온 경우
        const status = error.response.status
        if (status === 500) {
          errorMessage = "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        } else if (status === 400) {
          errorMessage = "잘못된 요청입니다. 입력 데이터를 확인해주세요."
        } else if (status === 401) {
          errorMessage = "인증이 필요합니다. 다시 로그인해주세요."
        } else if (status === 403) {
          errorMessage = "권한이 없습니다."
        } else {
          errorMessage = `서버 오류 (${status}): ${error.response.data?.message || error.message}`
        }
      } else if (error.request) {
        // 요청은 보냈지만 응답을 받지 못한 경우
        errorMessage = "서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요."
      } else {
        // 기타 오류
        errorMessage = error.message || "알 수 없는 오류가 발생했습니다."
      }
      
      alert(errorMessage)
    } finally {
      setIsLoading(false)
    }
  }



  return (
    <div className="min-h-screen bg-gray-50">
      <Header currentPage="courses" userRole="instructor" userName="김강사" />

      <div className="flex">
        <Sidebar title="과정 관리" menuItems={sidebarItems} currentPath="/instructor/courses/lectures" />

        <main className="flex-1 p-6">
          <div className="max-w-4xl mx-auto">
            {/* 헤더 */}
            <div className="mb-6">
              <Link to={`/instructor/courses/lectures/${lectureId}`}>
                <Button variant="outline" className="mb-4 bg-transparent">
                  <ArrowLeft className="w-4 h-4 mr-2" />
                  강의 상세로 돌아가기
                </Button>
              </Link>

              <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                {existingPlanId ? "강의계획서 수정" : "강의계획서 작성"}
              </h1>
              <p className="text-gray-600">
                {lecture.courseName} ({lecture.courseCode}) - {lecture.lectureTitle}
              </p>
            </div>



            {/* 기본 정보 */}
            <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
              <h3 className="text-lg font-semibold mb-4" style={{ color: "#2C3E50" }}>
                기본 정보
              </h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    강의계획서 제목 <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={lectureplanData.planTitle}
                    onChange={(e) => handleInputChange("planTitle", e.target.value)}
                    className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      errors.planTitle ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="강의계획서 제목을 입력하세요"
                  />
                  {errors.planTitle && <p className="text-red-500 text-sm mt-1">{errors.planTitle}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    강의계획서 내용 <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    value={lectureplanData.planContent}
                    onChange={(e) => handleInputChange("planContent", e.target.value)}
                    className={`w-full h-24 px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      errors.planContent ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="강의계획서의 전반적인 내용을 작성하세요"
                  />
                  {errors.planContent && <p className="text-red-500 text-sm mt-1">{errors.planContent}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    전체 수업 목표 <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    value={lectureplanData.courseGoal}
                    onChange={(e) => handleInputChange("courseGoal", e.target.value)}
                    className={`w-full h-24 px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      errors.courseGoal ? "border-red-500" : "border-gray-300"
                    }`}
                    placeholder="학습 목표와 성취하고자 하는 역량을 작성하세요"
                  />
                  {errors.courseGoal && <p className="text-red-500 text-sm mt-1">{errors.courseGoal}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    학습 방법
                  </label>
                  <textarea
                    value={lectureplanData.learningMethod}
                    onChange={(e) => handleInputChange("learningMethod", e.target.value)}
                    className="w-full h-24 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="강의, 실습, 프로젝트 등 학습 방법을 작성하세요"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    평가 방법
                  </label>
                  <input
                    type="text"
                    value={lectureplanData.evaluationMethod}
                    onChange={(e) => handleInputChange("evaluationMethod", e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="출석 10%, 과제 40%, 시험 50% 등"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">교재 정보</label>
                  <input
                    type="text"
                    value={lectureplanData.textbook}
                    onChange={(e) => handleInputChange("textbook", e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="주교재, 부교재 등 (선택사항)"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">주차 수</label>
                  <input
                    type="number"
                    min="1"
                    max="20"
                    value={lectureplanData.weekCount}
                    onChange={(e) => handleWeekCountChange(Number.parseInt(e.target.value) || 1)}
                    className="w-32 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <span className="text-sm text-gray-500 ml-2">주 (기본 15주)</span>
                </div>
              </div>
            </div>

            {/* 정책 및 기타 정보 */}
            <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
              <h3 className="text-lg font-semibold mb-4" style={{ color: "#2C3E50" }}>
                정책 및 기타 정보
              </h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">과제 정책</label>
                  <textarea
                    value={lectureplanData.assignmentPolicy}
                    onChange={(e) => handleInputChange("assignmentPolicy", e.target.value)}
                    className="w-full h-24 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="과제 제출 방법, 평가 기준 등을 작성하세요"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">지각 처리 정책</label>
                  <textarea
                    value={lectureplanData.latePolicy}
                    onChange={(e) => handleInputChange("latePolicy", e.target.value)}
                    className="w-full h-24 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="지각, 결석 처리 방법 등을 작성하세요"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">기타 참고사항</label>
                  <textarea
                    value={lectureplanData.etcNote}
                    onChange={(e) => handleInputChange("etcNote", e.target.value)}
                    className="w-full h-24 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="기타 참고사항이나 특이사항을 작성하세요"
                  />
                </div>
              </div>
            </div>

            {/* 주차별 계획 */}
            <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-semibold" style={{ color: "#2C3E50" }}>
                  주차별 계획
                </h3>
                <div className="flex items-center space-x-2">
                  <label className="text-sm font-medium text-gray-700">전체 주차 수:</label>
                  <input
                    type="number"
                    min="1"
                    max="20"
                    value={lectureplanData.weekCount}
                    onChange={(e) => handleWeekCountChange(Number.parseInt(e.target.value) || 1)}
                    className="w-16 px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <span className="text-sm text-gray-500">주</span>
                </div>
              </div>

              <div className="space-y-4 max-h-96 overflow-y-auto">
                {weeklyplan.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    <p>주차별 계획을 작성해주세요.</p>
                    <p className="text-sm">주차 수를 설정하면 자동으로 주차별 계획 입력 폼이 생성됩니다.</p>
                  </div>
                ) : (
                  weeklyplan.map((week, index) => (
                    <div key={index} className="border border-gray-200 rounded-lg p-4 hover:border-gray-300 transition-colors">
                      <div className="flex items-center mb-3">
                        <div className="w-8 h-8 bg-gray-100 text-gray-700 rounded-full flex items-center justify-center text-sm font-semibold mr-3">
                          {week.weekNumber}
                        </div>
                        <h4 className="font-medium text-gray-900">{week.weekNumber}주차</h4>
                                                 {(week.weekTitle && week.weekTitle.trim() && week.weekContent && week.weekContent.trim()) ? (
                           <span className="ml-2 px-2 py-1 bg-green-100 text-green-800 text-xs rounded-full">
                             완료
                           </span>
                         ) : (
                           <span className="ml-2 px-2 py-1 bg-red-100 text-red-800 text-xs rounded-full">
                             미완료
                           </span>
                         )}
                      </div>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">
                            주차 제목 <span className="text-red-500">*</span>
                          </label>
                          <div className="space-y-2">
                            <input
                              type="text"
                              value={week.weekTitle}
                              onChange={(e) => handleWeeklyplanChange(index, "weekTitle", e.target.value)}
                              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                              placeholder="예: OT 및 환경 설정"
                            />
                                                         <div>
                               <label className="block text-xs font-medium text-gray-600 mb-1">
                                 과목 선택 (다중 선택 가능)
                               </label>
                               <div className="max-h-32 overflow-y-auto border border-gray-300 rounded-md p-2">
                                 {isLoadingSubjects ? (
                                   <div className="text-sm text-gray-500">로딩 중...</div>
                                 ) : subjects.length > 0 ? (
                                   <div className="space-y-1">
                                                                           {subjects.map((subject, subjectIndex) => (
                                        <label key={`week-${week.weekNumber}-subject-${subjectIndex}-${subject.subjectId}`} className="flex items-center space-x-2 text-sm">
                                         <input
                                           type="checkbox"
                                           checked={week.subjectIds?.includes(subject.subjectId) || false}
                                           onChange={(e) => handleSubjectSelection(index, subject.subjectId, e.target.checked)}
                                           className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                         />
                                         <span className="text-gray-700">{subject.subjectName}</span>
                                       </label>
                                     ))}
                                   </div>
                                 ) : (
                                   <div className="text-sm text-gray-500">과목이 없습니다.</div>
                                 )}
                               </div>
                               {week.subjectIds && week.subjectIds.length > 0 && (
                                 <div className="mt-2 p-2 bg-blue-50 border border-blue-200 rounded-md">
                                   <div className="text-xs font-medium text-blue-800 mb-1">선택된 과목:</div>
                                   <div className="text-xs text-blue-700">
                                     {week.subjectIds
                                       .map(id => subjects.find(s => s.subjectId === id)?.subjectName)
                                       .filter(name => name)
                                       .join(', ')}
                                   </div>
                                 </div>
                               )}
                             </div>
                          </div>
                        </div>
                                                 <div>
                           <label className="block text-sm font-medium text-gray-700 mb-1">
                             상세 커리큘럼 설명 <span className="text-red-500">*</span>
                           </label>
                           <div className="space-y-2">
                             <textarea
                               value={week.weekContent}
                               onChange={(e) => handleWeeklyplanChange(index, "weekContent", e.target.value)}
                               className="w-full h-20 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                               placeholder="해당 주차의 상세 내용을 작성하세요"
                             />
                             
                                                           {/* 선택된 과목별 세부과목 표시 */}
                              {week.subjectIds && week.subjectIds.length > 0 && (
                                <div className="space-y-3">
                                  {week.subjectIds.map(subjectId => {
                                    const selectedSubject = subjects.find(s => s.subjectId === subjectId)
                                    // 세부과목 필터링 - subjectId 또는 subject_id 필드로 매칭
                                    const subjectDetailsForSubject = subjectDetails.filter(d => 
                                      d.subjectId === subjectId || d.subject_id === subjectId
                                    )
                                    
                                    return (
                                      <div key={subjectId} className="border border-gray-200 rounded-md p-3">
                                        <div className="text-sm font-medium text-gray-800 mb-2">
                                          {selectedSubject?.subjectName} - 세부과목
                                        </div>
                                        {subjectDetailsForSubject.length > 0 ? (
                                          <div className="space-y-1">
                                            {subjectDetailsForSubject.map((detail, detailIndex) => (
                                              <label key={`week-${week.weekNumber}-detail-${detailIndex}-${detail.subDetailld || detail.subDetailId || 'no-id'}`} className="flex items-center space-x-2 text-sm">
                                                <input
                                                  type="checkbox"
                                                  checked={week.subDetailIds?.includes(detail.subDetailld || detail.subDetailId) || false}
                                                  onChange={(e) => handleSubjectDetailSelection(index, detail.subDetailld || detail.subDetailId, e.target.checked)}
                                                  className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                                />
                                                <span className="text-gray-700">{detail.subDetailName}</span>
                                              </label>
                                            ))}
                                          </div>
                                        ) : (
                                          <div className="text-sm text-gray-500">해당 과목의 세부과목이 없습니다.</div>
                                        )}
                                      </div>
                                    )
                                  })}
                                </div>
                              )}
                             
                             {week.subDetailIds && week.subDetailIds.length > 0 && (
                               <div className="mt-2 p-2 bg-green-50 border border-green-200 rounded-md">
                                 <div className="text-xs font-medium text-green-800 mb-1">선택된 세부과목:</div>
                                 <div className="text-xs text-green-700">
                                   {week.subDetailIds
                                     .map(id => subjectDetails.find(d => 
                                       (d.subDetailld === id || d.subDetailId === id)
                                     )?.subDetailName)
                                     .filter(name => name)
                                     .join(', ')}
                                 </div>
                               </div>
                             )}
                           </div>
                         </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
              
              {weeklyplan.length > 0 && (
                <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                                     <p className="text-sm text-blue-800">
                     <strong>안내:</strong> 모든 주차의 제목과 내용을 입력해야 등록이 가능합니다. 
                     현재 <strong>{weeklyplan.filter(week => week.weekTitle && week.weekTitle.trim() && week.weekContent && week.weekContent.trim()).length}/{weeklyplan.length}주차</strong>가 완료되었습니다.
                   </p>
                  {errors.weeklyplan && (
                    <p className="text-sm text-red-600 mt-2">
                      <strong>오류:</strong> {errors.weeklyplan}
                    </p>
                  )}
                </div>
              )}
            </div>

            {/* 저장/취소/삭제 버튼 */}
            <div className="flex justify-end space-x-4">
              <Link to={`/instructor/courses/lectures/${lectureId}`}>
                <Button variant="outline" className="hover:bg-gray-200">
                  취소
                </Button>
              </Link>
              {existingPlanId && (
                <Button
                  className="bg-red-600 text-white hover:bg-red-700"
                  onClick={async () => {
                    if (window.confirm("정말로 강의계획서를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) {
                      try {
                        await deleteLecturePlan(existingPlanId)
                        alert("강의계획서가 삭제되었습니다.")
                        navigate(`/instructor/courses/lectures/${lectureId}`)
                      } catch (error) {
                        alert("삭제 중 오류가 발생했습니다: " + error.message)
                      }
                    }
                  }}
                >
                  삭제
                </Button>
              )}
              <Button onClick={handleSave} disabled={isLoading} className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]">
                {isLoading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
                    저장 중...
                  </>
                ) : (
                  <>
                    <Save className="w-4 h-4 mr-2" />
                    {existingPlanId ? "수정" : "저장"}
                  </>
                )}
              </Button>
            </div>

            {/* 안내사항 */}
            <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-start">
                <AlertCircle className="w-5 h-5 text-blue-600 mr-3 mt-0.5" />
                <div className="text-sm text-blue-800">
                  <h4 className="font-medium mb-1">작성 안내</h4>
                  <ul className="space-y-1 text-blue-700">
                    <li>• 필수 항목(*)은 반드시 입력해주세요.</li>
                    <li>• 모든 주차의 제목과 내용을 입력해야 등록이 가능합니다.</li>
                    <li>• 주차별 계획은 전체 주차 수에 따라 자동으로 생성됩니다.</li>
                                         <li>• 과목을 선택하면 해당 과목의 세부과목들이 나타나며, 세부과목을 선택하면 자동으로 제목과 내용이 입력됩니다.</li>
                    <li>• 평가 방법은 구체적인 비율로 작성해주세요. (예: 출석 10%, 과제 40%, 시험 50%)</li>
                    <li>• 저장 후에도 언제든지 수정할 수 있습니다.</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
