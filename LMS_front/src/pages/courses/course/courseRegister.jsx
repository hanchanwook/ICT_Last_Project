import { useState, useEffect, useCallback } from "react"
import { Save, Plus, Minus, RotateCcw, Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getAllSubject, getCourseClassroom, createCourse, getAllCourse, updateCourse } from "@/api/suhyeon/courseApi"
import { getAllEduInstructor } from "@/api/suhyeon/courseApi"
import { getAllClassrooms } from "@/api/hancw/classroomAxios"
import { coursesMenuItems, createDynamicMenuItems } from "@/components/ui/menuConfig"
import { useNavigate, useLocation } from "react-router-dom"

export default function CourseRegisterPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const queryParams = new URLSearchParams(location.search)
  const courseId = queryParams.get("courseId")

  const initialFormData = {
    courseName: "",
    memberId: "",
    courseStartDay: "",
    courseEndDay: "",
    classId: "",
    courseDays: "",
    startTime: "",
    endTime: "",
    minCapacity: 1,
    maxCapacity: "",
    curriculum: [{ id: Date.now(), week: "", topic: "", description: "", subjectTime: "" }],
  }

  const [formData, setFormData] = useState(initialFormData)
  const [isSubjectModalOpen, setIsSubjectModalOpen] = useState(false)
  const [searchTerm, setSearchTerm] = useState("")
  const [availableSubjects, setAvailableSubjects] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedCurriculumIndex, setSelectedCurriculumIndex] = useState(null)
  const [roomsData, setRoomsData] = useState([])
  const [error, setError] = useState(null)
  const [availableTimes, setAvailableTimes] = useState([])
  const [instructorList, setInstructorList] = useState([])
  const [selectedClassCapacity, setSelectedClassCapacity] = useState(null)
  const [totalCourseHours, setTotalCourseHours] = useState(0)
  const [subjectTimes, setSubjectTimes] = useState({}) // 과목별 시간을 별도로 관리
  const [selectedSubjects, setSelectedSubjects] = useState([]) // 선택된 과목들을 관리

  // 강의 총 시간 계산 함수
  const calculateTotalCourseHours = () => {
    if (!formData.courseStartDay || !formData.courseEndDay || !formData.startTime || !formData.endTime || !formData.courseDays || formData.courseDays.length === 0) {
      return 0
    }

    const startDate = new Date(formData.courseStartDay)
    const endDate = new Date(formData.courseEndDay)
    const startTime = new Date(`2000-01-01T${formData.startTime}`)
    const endTime = new Date(`2000-01-01T${formData.endTime}`)
    
    // 하루 수업 시간 (시간 단위)
    const dailyHours = (endTime - startTime) / (1000 * 60 * 60)
    
    // 총 수업일수 계산
    let totalDays = 0
    const currentDate = new Date(startDate)
    
    while (currentDate <= endDate) {
      const dayOfWeek = currentDate.getDay()
      const dayNames = ['일', '월', '화', '수', '목', '금', '토']
      const currentDayName = dayNames[dayOfWeek]
      
      if (formData.courseDays.includes(currentDayName)) {
        totalDays++
      }
      
      currentDate.setDate(currentDate.getDate() + 1)
    }
    
    return Math.round(dailyHours * totalDays * 10) / 10 // 소수점 첫째자리까지
  }

  // 강의실 목록 불러오기
  useEffect(() => {
    const fetchClassrooms = async () => {
      try {
        setLoading(true)
        const data = await getAllClassrooms()
        setRoomsData(data)
        setError(null)
      } catch (err) {
        setRoomsData([])
        setError('강의실 목록을 불러오는데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }
    fetchClassrooms()
  }, [])

  // 강사 목록 불러오기
  useEffect(() => {
    const fetchInstructors = async () => {
      try {
        const data = await getAllEduInstructor()
        setInstructorList(data)
      } catch (err) {
        setInstructorList([])
      }
    }
    fetchInstructors()
  }, [])

  // API에서 과목 데이터 가져오기
  useEffect(() => {
    const fetchSubDetails = async () => {
      try {
        setLoading(true)
        const data = await getAllSubject()
        if (data && Array.isArray(data) && data.length > 0) {
          // 각 과목의 id 확인
          data.forEach((subject, index) => {
          
          })
          setAvailableSubjects(data)
        } else {
          setAvailableSubjects([])
        }
      } catch (error) {
        console.error('과목 데이터 가져오기 실패:', error)
        setAvailableSubjects([])
      } finally {
        setLoading(false)
      }
    }
    fetchSubDetails()
  }, [])

     // 강의 총 시간 계산
   useEffect(() => {
     const totalHours = calculateTotalCourseHours()
     setTotalCourseHours(totalHours)
   }, [formData.courseStartDay, formData.courseEndDay, formData.startTime, formData.endTime, formData.courseDays])

   // 과목 시간 변경 시 총 시간 재계산 (디버깅용)
   useEffect(() => {
     const totalSubjectHours = calculateTotalSubjectHours()
   }, [subjectTimes, formData.curriculum])

  // 수정 모드: courseId가 있으면 기존 정보 불러오기
  useEffect(() => {
    if (courseId && roomsData.length > 0) {
      const fetchCourse = async () => {
        try {
          const allCourses = await getAllCourse()
          const found = allCourses.find(c => String(c.courseId) === String(courseId))
          
          if (found) {
            // memberName으로 강사 찾기
            let foundMemberId = found.memberId || ""
            if (found.memberName && instructorList.length > 0) {
              const matchingInstructor = instructorList.find(instructor => 
                instructor.memberName === found.memberName
              )
              
              if (matchingInstructor) {
                foundMemberId = matchingInstructor.memberId
              }
            }
            
            const newCurriculum = found.subjects
              ? found.subjects.map(sub => ({
                  id: Date.now() + Math.random(),
                  week: sub.duration || "",
                  topic: sub.subjectName || "",
                  description: sub.subjectInfo || "",
                  subjectId: sub.id || sub.subjectId || "",
                  subjectTime: sub.subjectTime || "",
                }))
              : [{ id: Date.now(), week: "", topic: "", description: "", subjectTime: "" }]

            setFormData({
              courseName: found.courseName || "",
              memberId: foundMemberId,
              courseStartDay: found.courseStartDay || "",
              courseEndDay: found.courseEndDay || "",
              classId: found.classId || "",
              courseDays: Array.isArray(found.courseDays)
                ? found.courseDays
                : (typeof found.courseDays === "string" && found.courseDays)
                  ? found.courseDays.split(",")
                  : [],
              startTime: found.startTime || "",
              endTime: found.endTime || "",
              minCapacity: found.minCapacity || "",
              maxCapacity: found.maxCapacity || "",
              curriculum: newCurriculum,
            })

            // subjectTimes 상태도 초기화
            const initialSubjectTimes = {}
            newCurriculum.forEach((item, index) => {
              if (item.subjectTime) {
                initialSubjectTimes[index] = item.subjectTime
              }
            })
            setSubjectTimes(initialSubjectTimes)
          }
        } catch (error) {
          console.error("과정 데이터 불러오기 실패:", error)
        }
      }
      fetchCourse()
    }
  }, [courseId, roomsData, instructorList])

  const handleInputChange = (field, value) => {
    // 강의실 선택 시 classCapacity도 갱신
    if (field === "classId") {
      const selectedRoom = roomsData.find(room => room.classId === value)
      setSelectedClassCapacity(selectedRoom ? selectedRoom.classCapacity : null)
      // 강의실이 바뀌면 최대인원, 최소인원, 시간, 예약가능시간도 초기화
      setAvailableTimes([])
      setFormData((prev) => ({
        ...prev,
        classId: value,
        maxCapacity: selectedRoom ? selectedRoom.classCapacity : "",
        minCapacity: 1,
        startTime: "",
        endTime: "",
      }))
      return
    }
    // maxStudents 입력 시 classCapacity 초과 제한 및 경고
    if (field === "maxCapacity" && selectedClassCapacity) {
      if (Number(value) > selectedClassCapacity) {
        alert(`최대 수강생은 강의실 최대 정원(${selectedClassCapacity}명)을 초과할 수 없습니다.`)
        value = selectedClassCapacity
      }
    }
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  const addCurriculumItem = () => {
    // 수정 모드에서는 기존 선택된 과목들을 미리 선택
    if (courseId && formData.curriculum && formData.curriculum.length > 0) {
      const existingSubjects = formData.curriculum
        .filter(item => item.subjectId) // subjectId가 있는 항목만
        .map(item => ({
          id: item.subjectId,
          subjectId: item.subjectId,
          subjectName: item.topic,
          subjectInfo: item.description,
          duration: item.week,
          subDetails: item.subDetails || []
        }))
      setSelectedSubjects(existingSubjects)
    } else {
      setSelectedSubjects([]) // 새로 등록하는 경우 선택된 과목 초기화
    }
    setIsSubjectModalOpen(true)
  }

  const handleSelectSubject = (subject) => {
    
    setSelectedSubjects(prev => {
      // subjectId를 우선 사용하고, 없으면 id 사용
      const subjectIdentifier = subject.subjectId || subject.id
      const isAlreadySelected = prev.some(selected => {
        const selectedIdentifier = selected.subjectId || selected.id
        return selectedIdentifier === subjectIdentifier
      })
      
      if (isAlreadySelected) {
        const filtered = prev.filter(selected => {
          const selectedIdentifier = selected.subjectId || selected.id
          return selectedIdentifier !== subjectIdentifier
        })
        return filtered
      } else {
        const newSelected = [...prev, subject]
        return newSelected
      }
    })
  }

  const handleConfirmSubjectSelection = () => {
    if (selectedSubjects.length === 0) {
      alert("최소 1개 이상의 과목을 선택해주세요.")
      return
    }

    setFormData((prev) => {
      const newCurriculum = selectedSubjects.map(subject => ({
        id: Date.now() + Math.random(),
        week: subject.duration || "",
        topic: subject.subjectName || "",
        description: subject.subjectInfo || "",
        subDetails: subject.subDetails || [],
        subjectId: subject.id || subject.subjectId || "",
        subjectTime: "",
      }))
      return { ...prev, curriculum: newCurriculum }
    })
    
    setIsSubjectModalOpen(false)
    setSelectedSubjects([])
  }

  const handleCloseSubjectModal = () => {
    setIsSubjectModalOpen(false)
    setSelectedSubjects([])
  }

  const removeCurriculumItem = (index) => {
    const newCurriculum = formData.curriculum.filter((_, i) => i !== index)
    setFormData((prev) => ({
      ...prev,
      curriculum: newCurriculum.length > 0
        ? newCurriculum
        : [{ id: Date.now(), week: "", topic: "", description: "", subjectTime: "", subDetails: [] }],
    }))
  }

     // 새로운 과목 시간 입력 처리 함수
   const handleSubjectTimeInput = useCallback((curriculumIndex, value) => {
     
     // 숫자만 허용
     const numericValue = value.replace(/[^0-9]/g, '')
     
           // 빈 값이거나 0이면 빈 문자열로 설정 (0시간 입력 방지)
      if (numericValue === '' || numericValue === '0') {
        setSubjectTimes(prev => {
          const newTimes = { ...prev }
          newTimes[curriculumIndex] = ''
          return newTimes
        })
        
        // formData.curriculum의 subjectTime도 동기화
        setFormData(prev => {
          const newCurriculum = [...prev.curriculum]
          newCurriculum[curriculumIndex] = {
            ...newCurriculum[curriculumIndex],
            subjectTime: ''
          }
          return { ...prev, curriculum: newCurriculum }
        })
        return
      }
     
     // 숫자로 변환하고 최대값 제한
     const parsedValue = parseInt(numericValue)
     const timeValue = Math.min(999, parsedValue)
     
     // 강의 총 시간 초과 검증
     if (totalCourseHours > 0) {
       // 현재 입력된 총 시간 계산 (현재 입력 중인 과목 제외) - 실제 과목이 있는 항목만 계산
       const validCurriculumItems = formData.curriculum.filter(item => 
         item.week || item.topic || item.description
       )
       const currentTotal = validCurriculumItems.reduce((sum, item, index) => {
         if (index === curriculumIndex) return sum // 현재 입력 중인 과목은 제외
         const currentTime = subjectTimes[index] || item.subjectTime || ''
         return sum + (parseInt(currentTime) || 0)
       }, 0)
       
       // 새로운 총 시간이 강의 총 시간을 초과하는지 확인
       const newTotal = currentTotal + timeValue
       
       if (newTotal > totalCourseHours) {
         alert(`강의 총 시간(${totalCourseHours}시간)을 초과할 수 없습니다.\n현재 입력 가능한 최대 시간: ${totalCourseHours - currentTotal}시간`)
         return
       }
     }
     
           setSubjectTimes(prev => {
        const newTimes = { ...prev }
        newTimes[curriculumIndex] = timeValue.toString()
        return newTimes
      })
      
      // formData.curriculum의 subjectTime도 동기화
      setFormData(prev => {
        const newCurriculum = [...prev.curriculum]
        newCurriculum[curriculumIndex] = {
          ...newCurriculum[curriculumIndex],
          subjectTime: timeValue.toString()
        }
        return { ...prev, curriculum: newCurriculum }
      })
   }, [totalCourseHours, formData.curriculum, subjectTimes])

  // 총 과목 시간 계산 함수
  const calculateTotalSubjectHours = () => {
    
    // UI와 동일한 필터링 적용 - 실제 과목이 있는 항목만 계산
    const validCurriculumItems = formData.curriculum.filter(item => 
      item.week || item.topic || item.description
    )
    
    const total = validCurriculumItems.reduce((sum, item, index) => {
      // 새로운 입력 시간이 있으면 그것을 사용, 없으면 기존 시간 사용
      const currentTime = subjectTimes[index] || item.subjectTime || ''
      const timeValue = parseInt(currentTime) || 0
      
      return sum + timeValue
    }, 0)
    
    return total
  }

  // 남은 시간 계산 함수
  const calculateRemainingHours = () => {
    const totalSubjectHours = calculateTotalSubjectHours()
    return Math.max(0, totalCourseHours - totalSubjectHours)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (Number(formData.minCapacity) > Number(formData.maxCapacity)) {
      alert("최소 수강생은 최대 수강생보다 클 수 없습니다.")
      return
    }
    if (formData.startTime && formData.endTime && formData.startTime >= formData.endTime) {
      alert("수업 시작시간은 종료시간보다 빠르거나 같아야 합니다.")
      return
    }

    // 과목 필수 검증
    const validSubjects = formData.curriculum.filter(item => 
      item.subjectId && item.subjectId !== "" && item.topic && item.topic.trim() !== ""
    )
    if (validSubjects.length === 0) {
      alert("최소 1개 이상의 과목을 선택해주세요.")
      return
    }

         // 과목 시간 검증
     const totalSubjectHours = calculateTotalSubjectHours()
     if (totalSubjectHours === 0) {
       alert("모든 과목의 시간을 입력해주세요.")
       return
     }
     
     // 개별 과목 시간 검증 (0시간 입력 방지) - 실제 과목이 있는 항목만 검증
     const validCurriculumItems = formData.curriculum.filter(item => 
       item.week || item.topic || item.description
     )
     
     const invalidSubjects = validCurriculumItems.filter((item, index) => {
       const currentTime = subjectTimes[index] || item.subjectTime || ''
       return item.subjectId && item.subjectId !== "" && item.topic && item.topic.trim() !== "" && 
              (currentTime === '' || currentTime === '0' || parseInt(currentTime) === 0)
     })
     
     if (invalidSubjects.length > 0) {
       const subjectNames = invalidSubjects.map(item => item.topic).join(', ')
       alert(`다음 과목들의 시간을 입력해주세요: ${subjectNames}`)
       return
     }
         // 총 과목 시간이 강의 총 시간과 정확히 일치하는지 검증
     if (totalSubjectHours !== totalCourseHours) {
       if (totalSubjectHours > totalCourseHours) {
         alert(`총 과목 시간(${totalSubjectHours}시간)이 과정 총 시간(${totalCourseHours}시간)을 초과할 수 없습니다.\n\n현재 입력된 총 시간: ${totalSubjectHours}시간\n과정 총 시간: ${totalCourseHours}시간\n초과된 시간: ${totalSubjectHours - totalCourseHours}시간`)
         return
       } else {
         alert(`총 과목 시간이 과정 총 시간과 일치하지 않습니다.\n\n현재 입력된 총 시간: ${totalSubjectHours}시간\n과정 총 시간: ${totalCourseHours}시간\n부족한 시간: ${totalCourseHours - totalSubjectHours}시간\n\n모든 과목의 시간을 입력하여 과정 총 시간과 정확히 일치시켜주세요.`)
         return
       }
     }

    // 과목별 시간 정보를 포함한 데이터 구성 - 실제 과목이 있는 항목만 포함
    const subjectsWithTime = validSubjects.map((item, index) => {
      const currentTime = subjectTimes[index] || item.subjectTime || ''
      return {
        subjectId: item.subjectId,
        subjectTime: parseInt(currentTime) || 0
      }
    })
    
    // 필수 필드 검증
    if (!formData.startTime || formData.startTime.trim() === "") {
      alert("수업 시작시간을 선택해주세요.")
      return
    }

    // API 전달용 데이터 구성
    const courseData = {
      courseName: formData.courseName,
      memberId: formData.memberId,
      courseStartDay: formData.courseStartDay,
      courseEndDay: formData.courseEndDay,
      classId: formData.classId,
      courseDays: formData.courseDays,
      startTime: formData.startTime,
      endTime: formData.endTime,
      minCapacity: Number(formData.minCapacity),
      maxCapacity: Number(formData.maxCapacity),
      subjects: subjectsWithTime  // 과목별 시간 정보만 전송
    }

    try {
      if (courseId) {
        await updateCourse(courseId, courseData)
        alert(`${formData.courseName} 과정이 성공적으로 수정되었습니다!`)
      } else {
        await createCourse(courseData)
        alert(`${formData.courseName} 과정이 성공적으로 등록되었습니다!`)
      }
      navigate("/courses/course")
    } catch (error) {
      console.error("과정 등록/수정 실패:", error)
      alert("과정 등록/수정에 실패했습니다. 다시 시도해주세요.")
    }
  }

  const handleReset = () => {
    setFormData(initialFormData)
  }

  const selectedSubjectNames = formData.curriculum.map(item => item.topic)
  const selectedSubjectIds = formData.curriculum.map(item => item.subjectId)

  // 오늘 날짜 구하기
  const today = new Date().toISOString().split('T')[0]

  return (
    <PageLayout currentPage="courses">
      <div className="flex">
        <Sidebar title="과정 관리" menuItems={createDynamicMenuItems(coursesMenuItems, courseId, null, "/courses/course/register")} currentPath="/courses/course/register" />

        <main className="flex-1 p-8">
          <div className="max-w-6xl mx-auto">
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                {courseId ? "과정 수정" : "새 과정 등록"}
              </h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                {courseId ? "기존 교육 과정을 수정합니다." : "새로운 교육 과정을 등록합니다."}
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* 기본 정보 */}
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50", fontSize: "1.2rem", fontWeight: "bold" }}>기본 정보</CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-2">
                      <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                        과정명 <span className="text-red-500">*</span> (최대 30자)
                      </label>
                      <Input
                        placeholder="과정명을 입력하세요"
                        value={formData.courseName}
                        onChange={(e) => {
                          if (e.target.value.length <= 30) {
                            handleInputChange("courseName", e.target.value)
                          }
                        }}
                        maxLength={30}
                        required
                      />
                      <div className="text-right">
                        <span className="text-xs" style={{ color: "#95A5A6" }}>
                          {formData.courseName.length}/30
                        </span>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                       담당 강사 <span className="text-red-500">*</span>
                      </label>
                      <select
                        value={formData.memberId}
                        onChange={(e) => handleInputChange("memberId", e.target.value)}
                        className="w-full px-3 py-2 border rounded-md"
                        style={{ borderColor: "#95A5A6" }}
                        required
                      >
                        <option value="">강사를 선택하세요</option>
                        {instructorList && instructorList.map((instructor) => (
                          <option key={instructor.memberId} value={instructor.memberId}>
                            {instructor.memberName || instructor.memberId}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-2">
                      <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                        시작일 <span className="text-red-500">*</span>
                      </label>
                      <Input
                        type="date"
                        value={formData.courseStartDay}
                        onChange={(e) => {
                          const selectedStartDate = e.target.value
                          if (formData.courseEndDay && selectedStartDate >= formData.courseEndDay) {
                            alert("시작일은 종료일 이전으로 설정해주세요.")
                            return
                          }
                          handleInputChange("courseStartDay", selectedStartDate)
                        }}
                        required
                        min={today}
                        max={formData.courseEndDay ? new Date(new Date(formData.courseEndDay).getTime() - 24 * 60 * 60 * 1000).toISOString().split('T')[0] : undefined}
                      />
                    </div>

                    <div className="space-y-2">
                      <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                        종료일 <span className="text-red-500">*</span>
                      </label>
                      <Input
                        type="date"
                        value={formData.courseEndDay}
                        onChange={(e) => {
                          const selectedEndDate = e.target.value
                          if (formData.courseStartDay && selectedEndDate <= formData.courseStartDay) {
                            alert("종료일은 시작일 이후로 설정해주세요.")
                            return
                          }
                          handleInputChange("courseEndDay", selectedEndDate)
                          
                          // 종료일이 설정되면 시작일을 하루 전으로 자동 설정
                          if (selectedEndDate) {
                            const endDate = new Date(selectedEndDate)
                            const startDate = new Date(endDate)
                            startDate.setDate(endDate.getDate() - 1)
                            const startDateString = startDate.toISOString().split('T')[0]
                            
                            if (!formData.courseStartDay || formData.courseStartDay >= selectedEndDate) {
                              handleInputChange("courseStartDay", startDateString)
                            }
                          }
                        }}
                        required
                        min={formData.courseStartDay || today}
                      />
                    </div>
                  </div>
                  <div className="grid grid-cols-4 md:grid-cols-4 gap-6">                    
                    <div className="space-y-2">
                        <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                          강의실
                        </label>
                        <select
                          value={formData.classId ? String(formData.classId).trim() : ""}
                          onChange={(e) => handleInputChange("classId", e.target.value)}
                          className="w-full px-3 py-2 border rounded-md"
                          style={{ borderColor: "#95A5A6" }}
                          required
                        >
                          <option value="">강의실을 선택하세요</option>
                          {roomsData && roomsData.map((room) => (
                            <option key={room.classId} value={String(room.classId).trim()}>
                              {room.className || room.classNumber || room.classId}
                            </option>
                          ))}
                          {formData.classId &&
                            !roomsData.some(r => String(r.classId).trim() === String(formData.classId).trim()) && (
                              <option value={String(formData.classId).trim()}>{formData.classId}</option>
                            )}
                        </select>
                      </div>                    
                      <div className="space-y-2">                        
                          <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            수업 요일 <span className="text-red-500">*</span>
                          </label>
                          <div className="flex gap-4 items-center">
                            {['월', '화', '수', '목', '금'].map((day) => (
                              <label key={day} className="flex items-center gap-1">
                                <input
                                  type="checkbox"
                                  name="courseDays"
                                  value={day}
                                  checked={Array.isArray(formData.courseDays) && formData.courseDays.includes(day)}
                                  onChange={(e) => {
                                    const checked = e.target.checked
                                    const prev = Array.isArray(formData.courseDays) ? formData.courseDays : []
                                    handleInputChange(
                                      "courseDays",
                                      checked ? [...prev, day] : prev.filter((d) => d !== day)
                                    )
                                  }}
                                />
                                {day}
                              </label>
                            ))}
                            <button
                              type="button"
                              className="ml-4 px-3 py-1 border rounded text-sm"
                              style={{
                                borderColor: '#1ABC9C',
                                color: '#1ABC9C',
                                height: '32px',
                                writingMode: 'horizontal-tb',
                                minWidth: '120px',
                                whiteSpace: 'nowrap',
                                display: 'inline-block',
                              }}
                              onClick={async () => {
                                if (!formData.classId || !formData.courseStartDay || !formData.courseEndDay || !formData.courseDays || formData.courseDays.length === 0) {
                                  alert('강의실, 시작일, 종료일, 수업 요일을 모두 선택하세요.')
                                  return
                                }
                                try {
                                  setLoading(true)
                                  const data = await getCourseClassroom(
                                    formData.classId,
                                    formData.courseStartDay,
                                    formData.courseEndDay,
                                    formData.courseDays
                                  )
                                  setAvailableTimes(data.availableSlots)
                                  
                                  // 예약 가능한 시간이 없는 경우 알림
                                  if (!data.availableSlots || data.availableSlots.length === 0) {
                                    alert('선택한 기간과 요일에 예약 가능한 시간이 없습니다.\n다른 강의실, 기간 또는 요일을 선택해주세요.')
                                  }
                                  
                                  setFormData((prev) => ({
                                    ...prev,
                                    startTime: "",
                                    endTime: "",
                                  }))
                                } catch (err) {
                                  setAvailableTimes([])
                                  alert('시간 조회에 실패했습니다.')
                                } finally {
                                  setLoading(false)
                                }
                              }}
                              disabled={loading}
                            >
                              {loading ? '조회 중...' : '시간 조회'}
                            </button>
                            {loading && (
                              <span
                                className="ml-2 text-emerald-600 text-sm"
                                style={{
                                  writingMode: 'horizontal-tb',
                                  whiteSpace: 'nowrap',
                                  display: 'inline-block',
                                }}
                              >
                                시간을 조회 중입니다...
                              </span>
                            )}
                          </div>
                        </div>  
                      </div>
                  <div className="grid grid-cols-4 md:grid-cols-3 gap-6">         
                  <div className="space-y-2">
                      <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                        수업 시작시간 <span className="text-red-500">*</span>
                      </label>
                      <select
                        value={formData.startTime || ""}
                        onChange={(e) => handleInputChange("startTime", e.target.value)}
                        className="w-full px-3 py-2 border rounded-md"
                        style={{ borderColor: "#95A5A6" }}
                        required
                        disabled={!availableTimes || availableTimes.length === 0}
                      >
                        <option value="">시간을 선택하세요</option>
                        {availableTimes && availableTimes.length > 0 && availableTimes.map((time) => (
                          <option key={time} value={time}>{time}</option>
                        ))}
                        {formData.startTime && !availableTimes.includes(formData.startTime) && (
                          <option value={formData.startTime}>{formData.startTime}</option>
                        )}
                      </select>
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                        수업 종료시간 <span className="text-red-500">*</span>
                      </label>
                      <select
                        value={formData.endTime || ""}
                        onChange={(e) => handleInputChange("endTime", e.target.value)}
                        className="w-full px-3 py-2 border rounded-md"
                        style={{ borderColor: "#95A5A6" }}
                        required
                        disabled={!availableTimes || availableTimes.length === 0}
                      >
                        <option value="">시간을 선택하세요</option>
                        {availableTimes && availableTimes.length > 0 && availableTimes
                          .filter((time) => !formData.startTime || time > formData.startTime)
                          .map((time) => (
                            <option key={time} value={time}>{time}</option>
                          ))}
                        {formData.endTime && !availableTimes.includes(formData.endTime) && (
                          <option value={formData.endTime}>{formData.endTime}</option>
                        )}
                      </select>
                    </div>
                    </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                      <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                        최소 수강생 <span className="text-red-500">*</span>
                      </label>
                      <Input
                        type="number"
                        placeholder="30"
                        value={formData.minCapacity}
                        min={1}
                        max={formData.maxCapacity || undefined}
                        onChange={(e) => handleInputChange("minCapacity", e.target.value)}
                        required
                      />
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                        최대 수강생 <span className="text-red-500">*</span>
                      </label>
                      <Input
                        type="number"
                        placeholder="30"
                        value={formData.maxCapacity}
                        min={formData.minCapacity || 1}
                        onChange={(e) => handleInputChange("maxCapacity", e.target.value)}
                        required
                        max={selectedClassCapacity || undefined}
                      />
                    </div>
                  </div>
                  
                  {/* 강의 총 시간 표시 */}
                  {totalCourseHours > 0 && (
                    <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                        과정 총 시간
                        </span>
                        <span className="text-lg font-bold" style={{ color: "#1ABC9C" }}>
                          {totalCourseHours}시간
                        </span>
                      </div>
                      <p className="text-xs mt-1" style={{ color: "#95A5A6" }}>
                        (시작일: {formData.courseStartDay}, 종료일: {formData.courseEndDay}, 
                        요일: {Array.isArray(formData.courseDays) ? formData.courseDays.join(', ') : formData.courseDays}, 
                        시간: {formData.startTime} ~ {formData.endTime})
                      </p>
                    </div>
                  )}
                </CardContent>
              </Card>
              {/* 과목선택 */}
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50", fontSize: "1.2rem", fontWeight: "bold" }}>과목 <span className="text-red-500">*</span></CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  {formData.curriculum.length === 0 ||
                    !formData.curriculum.some(item => item.week || item.topic || item.description) ? (
                    <div className="text-center py-8">
                      <div className="text-lg mb-4" style={{ color: "#95A5A6" }}>
                        등록된 과목이 없습니다
                      </div>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={addCurriculumItem}
                        className="bg-transparent text-[#1abc9c] border border-[#1abc9c] hover:!bg-[#1abc9c] hover:!text-white"
                      >
                        <Plus className="w-4 h-4 mr-2" />
                        과목 추가하기
                      </Button>
                    </div>
                  ) : (
                    <>
                      {formData.curriculum
                        .filter(item => item.week || item.topic || item.description)
                        .map((item, index) => (
                          <div
                            key={item.id || index}
                            className="p-4 border rounded-lg space-y-4"
                            style={{ borderColor: "#e0e0e0", backgroundColor: "#f8f9fa" }}
                          >
                            <div className="flex items-center justify-between">
                              <div>
                                <p style={{ color: "#95A5A6" }}>
                                  과목 {index + 1}
                                </p>
                                <h4 className="font-medium" style={{ color: "#2C3E50", fontWeight: "bold" }}>{item.topic}</h4>
                              </div>
                              <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={() => removeCurriculumItem(index)}
                                className="bg-transparent"
                                style={{ borderColor: "#e74c3c", color: "#e74c3c" }}
                              >
                                <Minus className="w-4 h-4" />
                              </Button>
                            </div>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                              <div className="space-y-2">
                                <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                  과목 설명
                                </label>
                                <div className="p-3 border rounded-md" style={{ borderColor: "#e0e0e0" }}>
                                  <div className="flex items-center justify-between">
                                    <div>
                                      <p className="text-sm">{item.description}</p>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <div className="space-y-2">
                                <label className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                  과목 시간 (시간) <span className="text-red-500">*</span>
                                </label>
                                <div className="flex items-center space-x-2">
                                                                     <Input
                                     type="text"
                                     placeholder="예: 10"
                                     value={subjectTimes[index] || item.subjectTime || ""}
                                     onChange={(e) => handleSubjectTimeInput(index, e.target.value)}
                                     className="flex-1"
                                     maxLength={3}
                                     required
                                     title={totalCourseHours > 0 ? `최대 ${(() => {
                                       const validCurriculumItems = formData.curriculum.filter(item => 
                                         item.week || item.topic || item.description
                                       )
                                       const currentTotal = validCurriculumItems.reduce((sum, item, idx) => {
                                         if (idx === index) return sum
                                         const currentTime = subjectTimes[idx] || item.subjectTime || ''
                                         return sum + (parseInt(currentTime) || 0)
                                       }, 0)
                                       return totalCourseHours - currentTotal
                                     })()}시간까지 입력 가능` : "시간을 입력하세요"}
                                   />
                                  <span className="text-sm" style={{ color: "#95A5A6" }}>
                                    시간
                                  </span>
                                </div>
                                  <div className="flex justify-between items-center">
                                   <p className="text-xs" style={{ color: "#95A5A6" }}>
                                   과정 총 시간: {totalCourseHours}시간
                                   </p>
                                 </div>
                                 <div className="flex justify-between items-center mt-1">
                                   <p className="text-xs font-medium" style={{ color: "#27AE60" }}>
                                     현재 입력된 총 시간: {calculateTotalSubjectHours()}시간
                                   </p>
                                   {totalCourseHours > 0 && (
                                     <p className="text-xs" style={{ color: calculateTotalSubjectHours() > totalCourseHours ? "#e74c3c" : "#27AE60" }}>
                                       남은 시간: {calculateRemainingHours()}시간
                                     </p>
                                   )}
                                 </div>
                                 {/* 입력 가능한 최대 시간 표시 */}
                                 {totalCourseHours > 0 && (() => {
                                   const validCurriculumItems = formData.curriculum.filter(item => 
                                     item.week || item.topic || item.description
                                   )
                                   const currentTotal = validCurriculumItems.reduce((sum, item, idx) => {
                                     if (idx === index) return sum // 현재 과목 제외
                                     const currentTime = subjectTimes[idx] || item.subjectTime || ''
                                     return sum + (parseInt(currentTime) || 0)
                                   }, 0)
                                   const maxAvailable = totalCourseHours - currentTotal
                                   return maxAvailable > 0 && (
                                     <p className="text-xs" style={{ color: "#1ABC9C" }}>
                                       이 과목에 입력 가능한 최대 시간: {maxAvailable}시간
                                     </p>
                                   )
                                 })()}
                                 {/* 0시간 입력 경고 */}
                                 {(subjectTimes[index] === '0' || item.subjectTime === '0') && (
                                   <p className="text-xs text-red-500 mt-1">
                                     ⚠️ 0시간은 입력할 수 없습니다. 1시간 이상 입력해주세요.
                                   </p>
                                 )}
                                                                 {(subjectTimes[index] || item.subjectTime) && Number(subjectTimes[index] || item.subjectTime) > 0 && (
                                   <p className="text-xs" style={{ color: "#27AE60" }}>
                                     ✓ {subjectTimes[index] || item.subjectTime}시간 입력됨
                                   </p>
                                 )}
                              </div>
                            </div>
                          </div>
                        ))}
                      
                                             {/* 총 과목 시간 표시 */}
                       {(formData.curriculum.some(item => item.subjectTime && Number(item.subjectTime) > 0) || 
                         Object.values(subjectTimes).some(time => time && Number(time) > 0)) && (
                        <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
                          <div className="flex items-center justify-between">
                            <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                              총 과목 시간
                            </span>
                            <span className="text-lg font-bold" style={{ color: "#27AE60" }}>
                              {calculateTotalSubjectHours()}시간
                            </span>
                          </div>
                          {totalCourseHours > 0 && (
                            <div className="mt-2 space-y-1">
                                                             <p className="text-xs" style={{ color: "#95A5A6" }}>
                                 남은 시간: {calculateRemainingHours()}시간
                               </p>
                               {/* 총 시간 일치 여부 표시 */}
                               {(() => {
                                 const totalSubject = calculateTotalSubjectHours()
                                 const totalCourse = totalCourseHours
                                 
                                 return totalSubject === totalCourse ? (
                                   <p className="text-xs font-medium" style={{ color: "#27AE60" }}>
                                     ✓ 총 과목 시간이 과정 총 시간과 일치합니다
                                   </p>
                                 ) : totalSubject > totalCourse ? (
                                   <p className="text-xs font-medium" style={{ color: "#e74c3c" }}>
                                     ⚠️ 총 과목 시간이 과정 총 시간을 초과했습니다
                                   </p>
                                 ) : (
                                   <p className="text-xs font-medium" style={{ color: "#f39c12" }}>
                                     ⚠️ 총 과목 시간이 과정 총 시간보다 부족합니다
                                   </p>
                                 )
                               })()}
                                                             <div className="w-full bg-gray-200 rounded-full h-2">
                                 <div 
                                   className={`h-2 rounded-full transition-all duration-300 ${
                                     calculateTotalSubjectHours() === totalCourseHours 
                                       ? 'bg-green-500' 
                                       : calculateTotalSubjectHours() > totalCourseHours 
                                         ? 'bg-red-500' 
                                         : 'bg-yellow-500'
                                   }`}
                                   style={{ 
                                     width: `${Math.min(100, (calculateTotalSubjectHours() / totalCourseHours) * 100)}%` 
                                   }}
                                 ></div>
                               </div>
                                  {calculateTotalSubjectHours() > totalCourseHours && (
                                 <p className="text-xs text-red-500 mt-1">
                                   ⚠️ 총 과목 시간이 과정 총 시간을 초과했습니다
                                 </p>
                               )}
                               {/* 총 시간 초과 시 상세 정보 */}
                               {calculateTotalSubjectHours() > totalCourseHours && (
                                 <p className="text-xs text-red-500 mt-1">
                                   초과된 시간: {calculateTotalSubjectHours() - totalCourseHours}시간
                                 </p>
                               )}
                               {/* 0시간 과목 경고 */}
                               {formData.curriculum.some((item, index) => {
                                 const currentTime = subjectTimes[index] || item.subjectTime || ''
                                 return item.subjectId && item.subjectId !== "" && item.topic && item.topic.trim() !== "" && 
                                        (currentTime === '' || currentTime === '0' || parseInt(currentTime) === 0)
                               }) && (
                                 <p className="text-xs text-red-500 mt-1">
                                   ⚠️ 모든 과목의 시간을 1시간 이상 입력해주세요
                                 </p>
                               )}
                            </div>
                          )}
                        </div>
                      )}
                      
                      <Button
                        type="button"
                        size="sm"
                        onClick={addCurriculumItem}
                        className="bg-transparent text-[#1ABC9C] border border-[#1ABC9C] hover:!bg-[#1ABC9C] hover:!text-white"
                      >
                        <Plus className="w-4 h-4 mr-2" />
                        과목 추가하기
                      </Button>
                    </>
                  )}
                </CardContent>
              </Card>

              {/* 버튼 그룹 */}
              <div className="flex justify-end space-x-4 pt-6">
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleReset}
                  className="flex items-center space-x-2 bg-transparent
                  text-[#95A5A5] border border-[#95A5A5] hover:!bg-gray-100
                  hover:text-[#95A5A5]"
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>초기화</span>
                </Button>
                <Button
                  type="submit"
                  className="text-white font-medium flex items-center space-x-2
                  bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                >
                  <Save className="w-4 h-4" />
                  <span>{courseId ? "과정 수정" : "과정 등록"}</span>
                </Button>
              </div>
            </form>

            {/* 등록 안내 */}
            <Card className="mt-6" style={{ borderColor: "#1ABC9C", borderWidth: "1px" }}>
              <CardContent >
                <h3 className="font-semibold mb-2" style={{ color: "#2C3E50" }}>
                  {courseId ? "과정 수정" : "과정 등록"} 안내사항
                </h3>
                <ul className="space-y-1 text-sm" style={{ color: "#95A5A6" }}>
                  <li>• 필수 항목(*)은 반드시 입력해주세요.</li>
                  <li>• 과정명은 중복될 수 없습니다.</li>
                  <li>• 시작일은 종료일보다 이전이어야 합니다.</li>
                  <li>• 최소 1개 이상의 과목을 선택해야 합니다.</li>
                  <li>• 각 과목의 시간을 입력해야 하며, 총 과목 시간은 과정 총 시간과 정확히 일치해야 합니다.</li>
                  {courseId ? (
                    <>
                      <li>• 수정된 과정 정보는 즉시 반영됩니다.</li>
                      <li>• 수정 후 과정 정보는 다시 변경할 수 있습니다.</li>
                    </>
                  ) : (
                    <>
                      <li>• 등록된 과정은 즉시 과정 목록에 표시됩니다.</li>
                      <li>• 등록 후 과정 정보는 수정할 수 있습니다.</li>
                    </>
                  )}
                </ul>
              </CardContent>
            </Card>
          </div>
        </main>
      </div>

      {/* 과목 선택 모달 */}
      {isSubjectModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}>
          <Card className="w-full max-w-6xl mx-4 max-h-[80vh] overflow-y-auto">
            <CardHeader>
              <div className="flex items-center justify-between mb-4">
                <CardTitle style={{ color: "#2C3E50" }}>과목 선택</CardTitle>
                <Button variant="ghost" size="sm" onClick={handleCloseSubjectModal} style={{ color: "#95A5A6" }}>
                  ✕
                </Button>
              </div>
              <div className="flex-1">
                <div className="relative">
                  <Search
                    className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4"
                    style={{ color: "#95A5A6" }}
                  />
                  <Input
                    placeholder="과목명으로 검색..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
              </div>
              <div className="flex items-center justify-between mt-4">
                <div className="flex items-center space-x-4">
                  <p className="text-sm" style={{ color: "#95A5A6" }}>
                    총 과목: {availableSubjects.length}개
                  </p>
                  <p className="text-sm font-medium" style={{ color: "#1ABC9C" }}>
                    선택된 과목: {selectedSubjects.length}개
                  </p>
                </div>
                <div className="flex space-x-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      // 편집 모드에서는 기존 curriculum의 과목들을 selectedSubjects에 설정
                      if (courseId && formData.curriculum && formData.curriculum.length > 0) {
                        const existingSubjects = formData.curriculum
                          .filter(item => item.subjectId) // subjectId가 있는 항목만
                          .map(item => ({
                            id: item.subjectId,
                            subjectId: item.subjectId,
                            subjectName: item.topic,
                            subjectInfo: item.description,
                            duration: item.week,
                            subDetails: item.subDetails || []
                          }))
                        setSelectedSubjects(existingSubjects)
                      } else {
                        setSelectedSubjects([]) // 새로 등록하는 경우 선택된 과목 초기화
                      }
                    }}
                    className="text-[#95A5A6] border-[#95A5A6] hover:bg-gray-100"
                  >
                    선택 취소
                  </Button>
                  <Button
                    size="sm"
                    onClick={handleConfirmSubjectSelection}
                    className="text-white"
                    style={{ backgroundColor: "#1ABC9C" }}
                    disabled={selectedSubjects.length === 0}
                  >
                    선택 완료 ({selectedSubjects.length}개)
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {availableSubjects
                  .filter(subject => 
                    subject.subjectName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                    (subject.subjectInfo && subject.subjectInfo.toLowerCase().includes(searchTerm.toLowerCase()))
                  )
                  .map((subject) => {
                    const isSelected = selectedSubjects.some(selected => {
                      const selectedIdentifier = selected.subjectId || selected.id
                      const subjectIdentifier = subject.subjectId || subject.id
                      return selectedIdentifier === subjectIdentifier
                    });
                    return (
                      <div
                        key={subject.id || subject.subjectId || `subject-${Math.random()}`}
                        className={`p-4 border rounded-lg cursor-pointer transition-all duration-200 ${
                          isSelected 
                            ? 'border-[#1ABC9C] bg-[#1ABC9C]/5' 
                            : 'border-[#e0e0e0] hover:border-[#1ABC9C] hover:bg-gray-50'
                        }`}
                        onClick={() => handleSelectSubject(subject)}
                      >
                        <div className="flex items-start justify-between mb-3">
                          <h3 className="font-semibold text-sm" style={{ color: "#2C3E50" }}>
                            {subject.subjectName}
                          </h3>
                          <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                            isSelected 
                              ? 'border-[#1ABC9C] bg-[#1ABC9C]' 
                              : 'border-gray-300'
                          }`}>
                            {isSelected && (
                              <div className="w-2 h-2 rounded-full bg-white"></div>
                            )}
                          </div>
                        </div>
                        <div className="flex flex-wrap gap-1 mb-2">
                          {subject.subDetails && subject.subDetails.length > 0
                            ? subject.subDetails.slice(0, 3).map((subDetail) => (
                                <span
                                  key={subDetail.subDetailId || `subdetail-${Math.random()}`}
                                  className="px-2 py-1 rounded bg-emerald-50 text-emerald-700 text-xs font-medium border border-emerald-200"
                                >
                                  {subDetail.subDetailName}
                                </span>
                              ))
                            : <span className="px-2 py-1 rounded bg-gray-50 text-gray-500 text-xs font-medium border border-gray-200">세부과목 없음</span>
                          }
                          {subject.subDetails && subject.subDetails.length > 3 && (
                            <span className="px-2 py-1 rounded bg-gray-50 text-gray-500 text-xs font-medium border border-gray-200">
                              +{subject.subDetails.length - 3}개 더
                            </span>
                          )}
                        </div>
                        <p className="text-xs" style={{ color: "#95A5A6" }}>
                          {subject.subjectInfo || "설명 없음"}
                        </p>
                      </div>
                    );
                  })}
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </PageLayout>
  )
}
