import { useState, useEffect } from "react"
import { Search, Eye, BarChart3, Users, Calendar, Star, Send, X, GraduationCap, BarChart2, CheckCircle, CalendarCheck2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent } from "@/components/ui/card"
import Header from "@/components/layout/header"
import Sidebar from "@/components/layout/sidebar"
import { getMenuItems } from "@/components/ui/menuConfig"
import { getStudentCourseList, getCourseEvaluationQuestions, submitEvaluationResponse, getStudentEvaluationResponse } from "@/api/suhyeon/evaluationApi"

export default function EvaluationResponse() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [selectedPeriod, setSelectedPeriod] = useState("all")
  
  // 사용자 정보 가져오기
  const userName = localStorage.getItem('userName') || "학생"
  
  // 모달 상태
  const [showEvaluationModal, setShowEvaluationModal] = useState(false)
  const [showResponseModal, setShowResponseModal] = useState(false)
  const [selectedCourse, setSelectedCourse] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // 설문 응답 상태
  const [questionResponses, setQuestionResponses] = useState({})

  // 데이터 상태
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [evaluationQuestions, setEvaluationQuestions] = useState([])
  const [studentResponses, setStudentResponses] = useState([])
  const [selectedTemplateIndex, setSelectedTemplateIndex] = useState(0)

  // 사이드바 메뉴 구성
  const sidebarMenuItems = getMenuItems('student-evaluation')

  // 학생의 수강 과정 목록 불러오기
  useEffect(() => {
    const fetchCourses = async () => {
      try {
        setLoading(true)
        const response = await getStudentCourseList()
        
        if (!response || !Array.isArray(response)) {
          throw new Error("API 응답이 비어있거나 배열이 아닙니다.")
        }
        
        // 개별 템플릿의 상태를 결정하는 헬퍼 함수
        const getTemplateStatus = (template, courseResponse) => {
          const currentDate = new Date()
          const today = currentDate.toISOString().split('T')[0] // YYYY-MM-DD 형식
          
          if (!template.openDate || !template.closeDate) {
            return "설문일정이 없음"
          }
          
          if (today >= template.openDate && today <= template.closeDate) {
            // 평가기간 안에 오늘이 들어감
            return template.response === true ? "응답 완료" : "응답 가능"
          } else if (today > template.closeDate) {
            // 기간이 지남
            return template.response === true ? "응답 완료" : "설문평가 종료"
          } else if (today < template.openDate) {
            // 기간 전
            return "예정"
          }
          
          return "설문일정이 없음"
        }
        
        // API 응답 데이터를 컴포넌트에서 사용하는 형식으로 변환
        const formattedCourses = response.map((course) => {
          // templateList 배열이 있는지 확인
          const hasTemplates = course.templateList && course.templateList.length > 0
          
          // 현재 날짜
          const currentDate = new Date()
          const today = currentDate.toISOString().split('T')[0] // YYYY-MM-DD 형식
          
          // 템플릿이 있는 경우 상태 판단
          let status = "설문일정이 없음"
          let surveyActive = false
          let firstTemplate = null
          
          if (hasTemplates) {
            // 첫 번째 템플릿을 기준으로 상태 결정
            firstTemplate = course.templateList[0]
            
            if (firstTemplate.openDate && firstTemplate.closeDate) {
              if (today >= firstTemplate.openDate && today <= firstTemplate.closeDate) {
                // 평가기간 안에 오늘이 들어감
                if (course.response === true) {
                  status = "응답 완료"
                } else {
                  status = "응답 가능"
                }
                surveyActive = true
              } else if (today > firstTemplate.closeDate) {
                // 기간이 지남
                if (course.response === true) {
                  status = "응답 완료"
                } else {
                  status = "설문평가 종료"
                }
                surveyActive = false
              } else if (today < firstTemplate.openDate) {
                // 기간 전
                status = "예정"
                surveyActive = false
              }
            } else {
              // 날짜 정보가 없음
              status = "설문일정이 없음"
              surveyActive = false
            }
          } else {
            // 템플릿이 없음
            status = "설문일정이 없음"
            surveyActive = false
          }
          
          // 각 템플릿에 개별 상태 추가
          const templateListWithStatus = course.templateList ? course.templateList.map(template => ({
            ...template,
            templateStatus: getTemplateStatus(template, course.response)
          })) : []
          
                     return {
             id: course.courseId || "UNKNOWN",
             code: course.courseCode || "UNKNOWN",
            name: course.courseName || "과정명 없음",
            instructor: course.memberName || "강사명 없음",
            period: course.courseStartDay && course.courseEndDay 
              ? `${course.courseStartDay} ~ ${course.courseEndDay}` 
              : "날짜 정보 없음",
            time: course.startTime && course.endTime 
              ? `${course.startTime} ~ ${course.endTime}`
              : "시간 정보 없음",
            surveyActive: surveyActive,
            surveyStartDate: hasTemplates ? firstTemplate?.openDate : "",
            surveyEndDate: hasTemplates ? firstTemplate?.closeDate : "",
            status: status,
            category: "설문 평가",
            schedule: course.courseDays || "설문 평가",
            templateGroupId: course.templateGroupId || "",
            studentCount: course.studentCount || 0,
            maxCapacity: course.maxCapacity || 0,
            // 새로운 구조: templateList 배열에 개별 상태 추가
            templateList: templateListWithStatus,
          }
        })
        
        setCourses(formattedCourses)
      } catch (err) {
        console.error("학생 수강 과정 목록 불러오기 실패:", err)
        setError("과정 목록을 불러오는데 실패했습니다.")
      } finally {
        setLoading(false)
      }
    }

    fetchCourses()
  }, [])

  // 평가 항목 정의 - API에서 받아온 실제 설문 문항 사용

  // 설문 응답 시작
  const handleStartEvaluation = async (course, templateIndex = 0) => {
    try {
      setSelectedCourse(course)
      setSelectedTemplateIndex(templateIndex)
      setShowEvaluationModal(true)
      
      // 선택된 템플릿의 상태 확인
      const selectedTemplate = course.templateList[templateIndex]
      if (selectedTemplate && selectedTemplate.templateStatus === "응답 완료") {
        // 응답 완료 상태인 경우 기존 응답 데이터를 불러옴
        await loadStudentResponse(course, templateIndex)
      } else {
        // 선택된 템플릿의 설문 문항 불러오기
        await loadTemplateQuestions(course, templateIndex)
      }
      
    } catch (err) {
      console.error("설문 문항 불러오기 실패:", err)
      alert("설문 문항을 불러오는데 실패했습니다.")
      // 에러 발생 시 모달 닫기
      setShowEvaluationModal(false)
      setSelectedCourse(null)
    }
  }

  // 템플릿 문항 로드 함수 (새로운 응답용)
  const loadTemplateQuestions = async (course, templateIndex) => {
    let questionsData = []
    
    try {
      if (course.templateList && course.templateList.length > 0) {
        // templateList가 있는 경우 선택된 템플릿의 templateGroupId 사용
        const selectedTemplate = course.templateList[templateIndex]
        
        if (selectedTemplate && selectedTemplate.templateGroupId) {
          questionsData = await getCourseEvaluationQuestions(selectedTemplate.templateGroupId)
        }
      } else {
        // templateList가 없는 경우 course.templateGroupId 사용
        if (course.templateGroupId) {
          questionsData = await getCourseEvaluationQuestions(course.templateGroupId)
        } else {
          questionsData = []
        }
      }
      
      // API 응답에서 data 필드 추출 - 안전한 처리
      let templateData = null
      if (questionsData) {
        if (typeof questionsData === 'object' && questionsData !== null) {
          templateData = questionsData.data || questionsData
        } else {
          templateData = null
        }
      }
      
      // templateData가 유효한 경우에만 설정하고 questionNum으로 정렬
      if (templateData && typeof templateData === 'object' && templateData !== null) {
        // questionList가 있는 경우 중복 제거 및 questionNum으로 정렬
        if (templateData.questionList && Array.isArray(templateData.questionList)) {
          // 깊은 복사로 원본 데이터 보호
          const templateDataCopy = JSON.parse(JSON.stringify(templateData));
          
          // 중복 제거 (evalQuestionId 기준)
          const uniqueQuestions = templateDataCopy.questionList.reduce((acc, question) => {
            const existingQuestion = acc.find(q => q.evalQuestionId === question.evalQuestionId);
            if (!existingQuestion) {
              acc.push(question);
            }
            return acc;
          }, []);
          
          // questionNum으로 정렬
          const sortedQuestions = uniqueQuestions.sort((a, b) => a.questionNum - b.questionNum);
          
          // 정렬된 questionList로 templateData 업데이트
          const processedTemplateData = {
            ...templateDataCopy,
            questionList: sortedQuestions
          };
          
          setEvaluationQuestions([processedTemplateData])
        } else {
          setEvaluationQuestions([templateData])
        }
      } else {
        setEvaluationQuestions([])
      }
      
      // 설문 문항별 응답 초기화 - 안전한 처리
      const initialResponses = {}
      if (templateData && templateData.questionList && Array.isArray(templateData.questionList)) {
        // questionNum으로 정렬된 질문 목록 사용
        const sortedQuestions = templateData.questionList.sort((a, b) => a.questionNum - b.questionNum);
        sortedQuestions.forEach(question => {
          if (question && question.evalQuestionId) {
            if (question.evalQuestionType === 0) {
              initialResponses[question.evalQuestionId] = 0
            } else if (question.evalQuestionType === 1) {
              initialResponses[question.evalQuestionId] = ""
            }
          }
        })
      }
      setQuestionResponses(initialResponses)
      
    } catch (error) {
      console.error("템플릿 문항 로드 중 오류:", error)
      throw error
    }
  }

  // 학생 응답 데이터 로드 함수 (응답 완료 상태용)
  const loadStudentResponse = async (course, templateIndex) => {
    let questionsData = []
    let responsesData = []
    
    try {
      if (course.templateList && course.templateList.length > 0) {
        // templateList가 있는 경우 선택된 템플릿의 templateGroupId 사용
        const selectedTemplate = course.templateList[templateIndex]
        
        if (selectedTemplate && selectedTemplate.templateGroupId) {
          try {
            const [questionsResult, responsesResult] = await Promise.all([
              getCourseEvaluationQuestions(selectedTemplate.templateGroupId),
              getStudentEvaluationResponse(selectedTemplate.templateGroupId)
            ])
            questionsData = questionsResult || []
            responsesData = responsesResult || []
          } catch (apiError) {
            console.error("API 호출 중 오류:", apiError)
            questionsData = []
            responsesData = []
          }
        }
      } else {
        // templateList가 없는 경우 course.templateGroupId 사용
        if (course.templateGroupId) {
          try {
            const [questionsResult, responsesResult] = await Promise.all([
              getCourseEvaluationQuestions(course.templateGroupId),
              getStudentEvaluationResponse(course.templateGroupId)
            ])
            questionsData = questionsResult || []
            responsesData = responsesResult || []
          } catch (apiError) {
            console.error("API 호출 중 오류:", apiError)
            questionsData = []
            responsesData = []
          }
        } else {
          questionsData = []
          responsesData = []
        }
      }
      
      // API 응답에서 data 필드 추출 - 더 안전한 처리
      let templateData = null
      if (questionsData) {
        if (typeof questionsData === 'object' && questionsData !== null) {
          templateData = questionsData.data || questionsData
        } else {
          templateData = null
        }
      }
      
      // templateData가 유효한 경우에만 설정하고 questionNum으로 정렬
      if (templateData && typeof templateData === 'object' && templateData !== null) {
        // questionList가 있는 경우 중복 제거 및 questionNum으로 정렬
        if (templateData.questionList && Array.isArray(templateData.questionList)) {
          // 깊은 복사로 원본 데이터 보호
          const templateDataCopy = JSON.parse(JSON.stringify(templateData));
          
          // 중복 제거 (evalQuestionId 기준)
          const uniqueQuestions = templateDataCopy.questionList.reduce((acc, question) => {
            const existingQuestion = acc.find(q => q.evalQuestionId === question.evalQuestionId);
            if (!existingQuestion) {
              acc.push(question);
            }
            return acc;
          }, []);
          
          // questionNum으로 정렬
          const sortedQuestions = uniqueQuestions.sort((a, b) => a.questionNum - b.questionNum);
          
          // 정렬된 questionList로 templateData 업데이트
          const processedTemplateData = {
            ...templateDataCopy,
            questionList: sortedQuestions
          };
          
          setEvaluationQuestions([processedTemplateData])
        } else {
          setEvaluationQuestions([templateData])
        }
      } else {
        setEvaluationQuestions([])
      }
      
      // API에서 이미 올바른 형식으로 반환되므로 직접 사용
      let actualResponsesData = responsesData || []
      setStudentResponses(actualResponsesData)
      
      // 학생 응답 데이터를 questionResponses에 설정
      if (actualResponsesData && Array.isArray(actualResponsesData) && actualResponsesData.length > 0) {
        // 설문 문항별 응답 설정 - 안전한 초기화
        const newQuestionResponses = {}
        
        try {
          actualResponsesData.forEach((response) => {
            // response가 유효한 객체인지 확인
            if (response && typeof response === 'object' && response !== null && response.evalQuestionId) {
              // DTO에 따르면 questionType이 response 객체에 직접 포함되어 있음
              const questionType = response.questionType
              
              // 백엔드 응답 형식에 맞춰 필드명 변경
              // questionType: 0 = 객관식(5점 척도), 1 = 주관식
              let value
              if (questionType === 0) { // 객관식 (5점 척도)
                value = response.score || 0
              } else if (questionType === 1) { // 주관식
                value = response.answerText || ""
              } else {
                value = null // 또는 적절한 기본값
              }
              
              if (value !== null) { // 유효한 value가 있는 경우에만 추가
                newQuestionResponses[response.evalQuestionId] = value
              }
            }
          })
          
        } catch (forEachError) {
          console.error("응답 데이터 처리 중 오류:", forEachError)
          // 오류가 발생해도 빈 객체로 설정
          setQuestionResponses({})
          return
        }
        
        // newQuestionResponses가 유효한 객체인지 확인 후 설정
        try {
          if (newQuestionResponses && typeof newQuestionResponses === 'object') {
            setQuestionResponses(newQuestionResponses)
          } else {
            setQuestionResponses({})
          }
        } catch (setError) {
          console.error("상태 설정 중 오류:", setError)
          // 오류가 발생해도 빈 객체로 설정
          setQuestionResponses({})
        }
      } else {
        setQuestionResponses({})
      }
      
    } catch (error) {
      console.error("학생 응답 데이터 로드 중 오류:", error)
      throw error
    }
  }

  // 별점 변경
  // 설문 문항별 응답 변경
  const handleQuestionResponseChange = (questionId, value) => {
    setQuestionResponses((prev) => ({
      ...prev,
      [questionId]: value,
    }))
  }

  // 설문 제출
  const handleSubmit = async () => {
    // 설문 문항이 있는 경우와 없는 경우를 구분하여 검증
    let isValid = true
    let errorMessage = ""
    
    if (evaluationQuestions.length > 0) {
      // 실제 설문 문항이 있는 경우
      for (const template of evaluationQuestions) {
        for (const question of template.questionList || []) {
          const response = questionResponses[question.evalQuestionId]
          
          if (question.evalQuestionType === 0) {
            // 객관식(5점 척도) 문항 검증
            if (!response || response === 0) {
              errorMessage = `"${question.evalQuestionText}" 문항을 평가해주세요.`
              isValid = false
              break
            }
          } else if (question.evalQuestionType === 1) {
            // 주관식(서술형) 문항 검증
            if (!response || response.trim() === "") {
              errorMessage = `"${question.evalQuestionText}" 문항에 답변을 입력해주세요.`
              isValid = false
              break
            }
          }
        }
        if (!isValid) break
      }
    } else {
      // 설문 문항이 없는 경우
      errorMessage = "설문 문항이 없습니다."
      isValid = false
    }

    if (!isValid) {
      alert(errorMessage)
      return
    }

    setIsSubmitting(true)

    try {
      // API로 전달할 데이터 구성
      const answerList = []
      
      // 설문 문항이 있는 경우
      if (evaluationQuestions.length > 0) {
        evaluationQuestions.forEach(template => {
          template.questionList?.forEach(question => {
            const response = questionResponses[question.evalQuestionId]
            const answer = {
              evalQuestionId: question.evalQuestionId,
              score: question.evalQuestionType === 0 ? (response || 0) : 0,
              answerText: question.evalQuestionType === 1 ? (response || "") : ""
            }
            answerList.push(answer)
          })
        })
      } else {
        // 설문 문항이 없는 경우
        alert("설문 문항이 없습니다.")
        return
      }
      
      // 선택된 템플릿의 templateGroupId 사용
      let templateGroupId = selectedCourse.templateGroupId
      if (selectedCourse.templateList && selectedCourse.templateList.length > 0) {
        const selectedTemplate = selectedCourse.templateList[selectedTemplateIndex]
        templateGroupId = selectedTemplate?.templateGroupId || selectedCourse.templateGroupId
      }
      
             const responseData = {
         courseId: selectedCourse.id,
         templateGroupId: templateGroupId,
         answerList: answerList
       }
      
      await submitEvaluationResponse(responseData)

      // 과정 상태 업데이트
      setCourses(prev => prev.map(course => 
        course.id === selectedCourse.id 
          ? { ...course, status: "응답 완료" }
          : course
      ))

      alert("설문 평가가 완료되었습니다. 소중한 의견 감사합니다!")
      closeModal()
      // 페이지 새로고침
      window.location.reload()
    } catch (error) {
      console.error("설문 제출 중 오류:", error)
      alert("설문 제출 중 오류가 발생했습니다.")
    } finally {
      setIsSubmitting(false)
    }
  }

  // 설문 응답 확인
  const handleViewResponse = async (course, templateIndex = 0) => {
    try {
      setSelectedCourse(course)
      setSelectedTemplateIndex(templateIndex)
      setShowResponseModal(true)
      
      // 응답 완료 상태이므로 getStudentEvaluationResponse 호출
      await loadStudentResponse(course, templateIndex)
      
    } catch (err) {
      console.error("설문 응답 조회 실패:", err)
      alert("설문 응답을 불러오는데 실패했습니다.")
    }
  }

  // 모달 닫기
  const closeModal = () => {
    setShowEvaluationModal(false)
    setSelectedCourse(null)
    setSelectedTemplateIndex(0)
    setIsSubmitting(false)
    setEvaluationQuestions([])
    setQuestionResponses({})
  }

  // 응답 확인 모달 닫기
  const closeResponseModal = () => {
    setShowResponseModal(false)
    setSelectedCourse(null)
    setSelectedTemplateIndex(0)
    setEvaluationQuestions([])
    setStudentResponses([])
  }

  // 별점 컴포넌트
  const StarRating = ({ rating, onRatingChange, disabled = false }) => {
    return (
      <div className="flex gap-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            onClick={() => !disabled && onRatingChange(star)}
            className={`p-1 transition-colors ${disabled ? "cursor-not-allowed" : "cursor-pointer hover:scale-110"}`}
            disabled={disabled}
          >
            <Star className={`w-6 h-6 ${star <= rating ? "fill-yellow-400 text-yellow-400" : "text-gray-300"}`} />
          </button>
        ))}
      </div>
    )
  }

  // 필터링된 과정 목록
  const filteredCourses = courses.filter((course) => {
    const matchesSearch =
      course.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      course.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
      course.instructor.toLowerCase().includes(searchTerm.toLowerCase()) ||
      course.category.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = selectedStatus === "all" || course.status === selectedStatus
    const matchesPeriod = selectedPeriod === "all" || course.period === selectedPeriod

    return matchesSearch && matchesStatus && matchesPeriod
  })

        // 통계 계산 - 각 과정의 templateList 내 개별 템플릿 상태를 기준으로 계산
  const totalCourses = courses.length
  
  // 모든 템플릿의 상태를 추출하여 통계 계산
  let completedSurveys = 0
  let availableSurveys = 0
  let endedSurveys = 0
  let scheduledSurveys = 0
  
  courses.forEach(course => {
    if (course.templateList && course.templateList.length > 0) {
      // 템플릿이 있는 경우 각 템플릿의 상태를 개별적으로 계산
      course.templateList.forEach(template => {
        switch (template.templateStatus) {
          case "응답 완료":
            completedSurveys++
            break
          case "응답 가능":
            availableSurveys++
            break
          case "설문평가 종료":
            endedSurveys++
            break
          case "예정":
            scheduledSurveys++
            break
          default:
            break
        }
      })
    } else {
      // 템플릿이 없는 경우 과정의 전체 상태를 확인
      // 과정이 끝났거나 설문평가 종료 상태인 경우
      if (course.status === "설문평가 종료" || course.status === "응답 완료") {
        endedSurveys++
      }
    }
  })

     const getStatusColor = (status) => {
     switch (status) {
       case "응답 가능":
         return "bg-green-100 text-green-800"
       case "응답 완료":
         return "bg-blue-100 text-blue-800"
       case "설문평가 종료":
         return "bg-red-100 text-red-800"
       case "예정":
         return "bg-yellow-100 text-yellow-800"
       case "설문일정이 없음":
         return "bg-gray-100 text-gray-800"
       default:
         return "bg-gray-100 text-gray-800"
     }
   }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header currentPage="evaluation" userRole="student" userName={userName} />
      <div className="flex">
        <Sidebar title="설문 평가" menuItems={sidebarMenuItems} currentPath="/student/evaluation" />

        <main className="flex-1 p-6 ">
          <div className="max-w-7xl mx-auto">
          {/* 페이지 헤더 */}
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900 mb-2">설문 평가</h1>
            <p className="text-gray-600">수강 중인 과정 설문 평가를 진행하세요.</p>
          </div>

                                         {/* 통계 카드 */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
            <Card>
              <CardContent className="p-6 flex items-center justify-between">
                <div>
                  <p className="text-base font-medium text-gray-600">총 수강 과정</p>
                  <p className="text-3xl font-bold text-[#3498db]">{totalCourses}개</p>
                </div>
                <div className="bg-[#EFF6FF] rounded-full p-3 ">
                  <GraduationCap className="w-10 h-10 text-[#3498db]" />
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6 flex items-center justify-between">
                <div >
                  <p className="text-base font-medium text-gray-600">응답 가능</p>
                  <p className="text-3xl font-bold text-[#1abc9c]">{availableSurveys}개</p>
                </div>
                <div className="bg-[#e4f5eb] rounded-full p-3 ">
                  <BarChart2 className="w-10 h-10 text-[#1abc9c]" />
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6 flex items-center justify-between">
                <div >
                  <p className="text-base font-medium text-gray-600">응답 완료</p>
                  <p className="text-3xl font-bold text-[#b0c4de]">{completedSurveys}개</p>
                </div>
                <div className="bg-[#eff6ff] rounded-full p-3 ">
                  <CheckCircle className="w-10 h-10 text-[#b0c4de]" />
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6 flex items-center justify-between">
                <div >
                  <p className="text-base font-medium text-gray-600">설문평가 종료</p>
                  <p className="text-3xl font-bold text-[#3f5c77]">{endedSurveys}개</p>
                </div>
                <div className="bg-[#eff6ff] rounded-full p-3 ">
                  <CalendarCheck2 className="w-10 h-10 text-[#3f5c77]" />
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6 flex items-center justify-between">
                <div >
                  <p className="text-base font-medium text-gray-600">예정</p>
                  <p className="text-3xl font-bold text-[#b0c4de]">{scheduledSurveys}개</p>
                </div>
                <div className="bg-[#eff6ff] rounded-full p-3 ">
                  <Calendar className="w-10 h-10 text-[#b0c4de]" />
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
                  placeholder="과정, 강사명으으로 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-teal-500"
                />
              </div>
                                                         <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-teal-500"
              >
                <option value="all">전체 상태</option>
                <option value="응답 가능">응답 가능</option>
                <option value="응답 완료">응답 완료</option>
                <option value="설문평가 종료">설문평가 종료</option>
                <option value="예정">예정</option>
              </select>
            </div>
          </div>

          {/* 로딩 상태 */}
          {loading && (
            <div className="bg-white rounded-lg shadow-sm p-8">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-500 mx-auto mb-4"></div>
                <p className="text-gray-600">수강 과정 목록을 불러오는 중...</p>
              </div>
            </div>
          )}

          {/* 에러 상태 */}
          {error && (
            <div className="bg-white rounded-lg shadow-sm p-8">
              <div className="text-center">
                <p className="text-red-600 mb-4">{error}</p>
                <Button 
                  onClick={() => window.location.reload()} 
                  className="bg-teal-500 hover:bg-teal-600 text-white"
                >
                  다시 시도
                </Button>
              </div>
            </div>
          )}

          {/* 과정 목록 */}
          {!loading && !error && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {filteredCourses.map((course) => (
                <div key={course.id} className="bg-white p-4 rounded-lg shadow-sm border hover:shadow-md transition-shadow">
                  <div>
                    {/* 과정 기본 정보 */}
                    <div className="flex justify-between items-start mb-4">
                      <div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-1">{course.name}</h3>
                        <p className="text-sm text-gray-600">
                          {course.code} • {course.category}
                        </p>
                        <p className="text-sm text-gray-500">{course.instructor}</p>
                      </div>
                    </div>

                    {/* 과정 세부 정보 */}
                    <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
                      <div>
                        <p className="text-gray-600">
                          수강 기간 : <br/>
                          <span className="font-medium">{course.period}</span>
                        </p>
                        <p className="text-gray-600">
                          수업 일정: <br/>
                          <span className="font-medium">{course.schedule} {course.time}</span>
                        </p>
                      </div>
                      <div>
                        <p className="text-gray-600">
                          수강생 수: <span className="font-medium">{course.studentCount}/{course.maxCapacity}</span>
                        </p>
                      </div>
                    </div>

                                         {/* 설문 정보 */}
                     <div className="bg-gray-50 p-4 rounded-lg mb-4">
                       <div className="flex justify-between items-center mb-2">
                         <h4 className="font-medium text-gray-900">설문 평가 정보</h4>
                       </div>

                                               <div className="text-sm text-gray-600">
                          {course.templateList && course.templateList.length > 0 ? (
                            <div>
                              <p className="font-medium mb-2">평가설문 목록:</p>
                              {course.templateList.map((template, index) => (
                                <div key={template.templateGroupId} className="mb-2">
                                  <div className={`flex items-center justify-between p-3 bg-white rounded-lg border transition-colors ${
                                    template.templateStatus === "응답 가능" 
                                      ? "border-gray-200 hover:border-teal-300 cursor-pointer" 
                                      : template.templateStatus === "응답 완료"
                                      ? "border-gray-200 hover:border-blue-300 cursor-pointer"
                                      : "border-gray-200 cursor-not-allowed opacity-75"
                                  }`}
                                       onClick={() => {
                                         if (template.templateStatus === "응답 가능") {
                                           handleStartEvaluation(course, index)
                                         } else if (template.templateStatus === "응답 완료") {
                                           handleViewResponse(course, index)
                                         } else if (template.templateStatus === "설문평가 종료") {
                                           alert("이미 설문평가가 종료되었습니다.")
                                         } else if (template.templateStatus === "예정") {
                                           alert("아직 설문평가가 시작되지 않았습니다.")
                                         } else {
                                           alert("설문평가를 진행할 수 없습니다.")
                                         }
                                       }}>
                                    <div className="flex-1">
                                      <div className="font-medium text-gray-900 text-sm">
                                        {template.questionTemplateName}
                                      </div>
                                      <div className="text-xs text-gray-500 mt-1">
                                        {template.openDate} ~ {template.closeDate}
                                      </div>
                                    </div>
                                    <div className="flex items-center gap-2">
                                      {template.templateStatus === "응답 가능" ? (
                                        <Button
                                          size="sm"
                                          className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                                          onClick={(e) => {
                                            e.stopPropagation()
                                            handleStartEvaluation(course, index)
                                          }}
                                        >
                                          응답
                                        </Button>
                                      ) : template.templateStatus === "응답 완료" ? (
                                        <Button
                                          size="sm"
                                          className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                                          onClick={(e) => {
                                            e.stopPropagation()
                                            handleViewResponse(course, index)
                                          }}
                                        >
                                          확인
                                        </Button>
                                      ) : template.templateStatus === "설문평가 종료" ? (
                                        <Badge className="bg-red-100 text-red-600 text-xs font-medium">
                                          평가 불가
                                        </Badge>
                                      ) : template.templateStatus === "예정" ? (
                                        <Badge className="bg-orange-100 text-orange-600 text-xs font-medium">
                                          설문 예정
                                        </Badge>
                                      ) : (
                                        <Badge className="bg-gray-100 text-gray-600 text-xs font-medium">
                                          평가 불가
                                        </Badge>
                                      )}
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          ) : course.status === "응답 완료" ? (
                            <p className="text-green-600 font-medium">✓ 설문 응답 완료</p>
                          ) : course.status === "설문평가 종료" ? (
                            <p className="text-red-600 font-medium">설문평가가 종료되었습니다.</p>
                          ) : course.status === "예정" ? (
                            <p className="text-orange-600 font-medium">설문평가 예정입니다.</p>
                          ) : (
                            <p className="text-gray-600 font-medium">설문일정이 없습니다.</p>
                          )}
                        </div>
                     </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* 결과가 없을 때 */}
          {!loading && !error && filteredCourses.length === 0 && (
            <div className="bg-white p-8 rounded-lg shadow-sm border text-center">
              <BarChart3 className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">검색 결과가 없습니다</h3>
              <p className="text-gray-600">검색 조건을 변경해보세요.</p>
            </div>
          )}

          {/* 설문 평가 모달 */}
          {showEvaluationModal && selectedCourse && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" style={{backgroundColor: "rgba(0, 0, 0, 0.3)"}}> 
              <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
                {/* 모달 헤더 */}
                <div className="flex justify-between items-center p-6 border-b">
                  <div>
                    <h2 className="text-xl font-bold text-gray-900">{selectedCourse.name} 설문 평가</h2>
                    <p className="text-gray-600 mt-1">
                      {selectedCourse.code} • {selectedCourse.instructor}
                    </p>
                  </div>
                  <Button variant="ghost" onClick={closeModal} className="p-2">
                    <X className="w-5 h-5" />
                  </Button>
                </div>

                {/* 모달 내용 */}
                <div className="p-6">
                  {/* 선택된 템플릿 정보 표시 */}
                  {selectedCourse.templateList && selectedCourse.templateList.length > 0 && (
                    <div className="mb-6">
                      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                        <div className="flex justify-between items-start">
                          <div>
                            <h3 className="text-lg font-semibold text-blue-900 mb-2">
                              {selectedCourse.templateList[selectedTemplateIndex]?.questionTemplateName || `평가 ${selectedTemplateIndex + 1}`}
                            </h3>
                            <p className="text-sm text-blue-700 mb-1">
                              평가 기간: {selectedCourse.templateList[selectedTemplateIndex]?.openDate} ~ {selectedCourse.templateList[selectedTemplateIndex]?.closeDate}
                            </p>
                          </div>
                          <Badge className="bg-blue-200 text-blue-800">
                            평가 {selectedTemplateIndex + 1}
                          </Badge>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* 과정 정보 */}
                  <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-3">{selectedCourse.name}</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600">
                      <div className="flex items-center gap-2">
                        <Users className="w-4 h-4" />
                        <span>{selectedCourse.instructor}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Calendar className="w-4 h-4" />
                        <span>{selectedCourse.period}</span>
                      </div>
                    </div>
                  </div>

                  {/* 평가 항목들 */}
                  <div className="mb-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">
                      {selectedCourse.templateList && selectedCourse.templateList[selectedTemplateIndex]?.templateStatus === "응답 완료" ? "과정 평가 결과" : "과정 평가 항목"}
                    </h3>
                    <p className="text-sm text-gray-600 mb-6">
                      {selectedCourse.templateList && selectedCourse.templateList[selectedTemplateIndex]?.templateStatus === "응답 완료"
                        ? "이미 완료된 설문 평가 결과입니다."
                        : "각 항목을 평가해주세요"
                      }
                    </p>
                    
                    {/* 실제 설문 문항이 있는 경우 */}
                    {evaluationQuestions.length > 0 ? (
                      <div className="space-y-8">
                        {evaluationQuestions.map((template, templateIndex) => (
                          <div key={template.questionTemplateNum || templateIndex} className="border border-gray-200 rounded-lg p-6">
                            <h4 className="text-lg font-medium text-gray-900 mb-4">
                              {template.questionTemplateName}
                            </h4>
                            <div className="space-y-6">
                              {template.questionList?.map((question) => (
                                <div key={question.evalQuestionId} className="border-b border-gray-100 pb-6 last:border-b-0">
                                  <div className="flex justify-between items-start mb-3">
                                    <div className="flex-1">
                                      <h5 className="font-medium text-gray-900 mb-2">
                                        {question.questionNum}. {question.evalQuestionText}
                                      </h5>
                                      <p className="text-sm text-gray-600">
                                        {question.evalQuestionType === 0 ? "5점 척도로 평가해주세요" : "자유롭게 답변해주세요"}
                                      </p>
                                    </div>
                                  </div>
                                  
                                  {/* 객관식(5점 척도) 문항 */}
                                  {question.evalQuestionType === 0 && (
                                    <div className="ml-6">
                                      <StarRating
                                        rating={questionResponses[question.evalQuestionId] || 0}
                                        onRatingChange={(rating) => handleQuestionResponseChange(question.evalQuestionId, rating)}
                                        disabled={selectedCourse.templateList && selectedCourse.templateList[selectedTemplateIndex]?.templateStatus === "응답 완료"}
                                      />
                                    </div>
                                  )}
                                  
                                  {/* 서술형(주관식) 문항 */}
                                  {question.evalQuestionType === 1 && (
                                    <div className="ml-6">
                                      <Textarea
                                        placeholder="답변을 입력해주세요..."
                                        value={questionResponses[question.evalQuestionId] || ""}
                                        onChange={(e) => handleQuestionResponseChange(question.evalQuestionId, e.target.value)}
                                        rows={3}
                                        className="w-full"
                                        disabled={selectedCourse.templateList && selectedCourse.templateList[selectedTemplateIndex]?.templateStatus === "응답 완료"}
                                      />
                                    </div>
                                  )}
                                </div>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      /* 설문 문항이 없는 경우 */
                      <div className="text-center py-8">
                        <p className="text-gray-500">설문 문항이 없습니다.</p>
                      </div>
                    )}
                  </div>

                  {/* 안내사항 */}
                  <div className="bg-gray-50 rounded-lg p-4 mb-6">
                    <div className="text-sm text-gray-600 space-y-2">
                      <p>• 설문은 익명으로 처리되며, 과정 개선을 위한 목적으로만 사용됩니다.</p>
                      <p>• 한 번 제출한 설문은 수정할 수 없으니 신중하게 평가해주세요.</p>
                      <p>• 성의있는 평가와 피드백은 더 나은 과정를 만드는 데 큰 도움이 됩니다.</p>
                    </div>
                  </div>
                </div>

                {/* 모달 푸터 */}
                <div className="border-t border-gray-200 flex justify-end gap-3 p-6">
                  <Button 
                    className="border hover:bg-gray-200"
                    onClick={closeModal}
                  >
                    {selectedCourse.status === "응답 완료" ? "닫기" : "취소"}
                  </Button>
                  {selectedCourse.templateList && selectedCourse.templateList[selectedTemplateIndex]?.templateStatus !== "응답 완료" && (
                    <Button
                      onClick={handleSubmit}
                      disabled={isSubmitting || (() => {
                        // 모든 필수 문항이 답변되었는지 확인
                        if (evaluationQuestions.length > 0) {
                          for (const template of evaluationQuestions) {
                            for (const question of template.questionList || []) {
                              const response = questionResponses[question.evalQuestionId]
                              
                              if (question.evalQuestionType === 0 && (!response || response === 0)) {
                                return true // disabled - 객관식(5점 척도) 미답변
                              }
                              if (question.evalQuestionType === 1 && (!response || response.trim() === "")) {
                                return true // disabled - 서술형(주관식) 미답변
                              }
                            }
                          }
                          return false // enabled - 모든 문항 답변 완료
                        } else {
                          // 설문 문항이 없는 경우
                          return true // disabled
                        }
                      })()}
                      className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                    >
                      {isSubmitting ? (
                        <>
                          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                          제출 중...
                        </>
                      ) : (
                        <>
                          설문 제출
                        </>
                      )}
                    </Button>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* 응답 확인 모달 */}
          {showResponseModal && selectedCourse && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" style={{backgroundColor: "rgba(0, 0, 0, 0.3)"}}> 
              <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
                {/* 모달 헤더 */}
                <div className="flex justify-between items-center p-6 border-b">
                  <div>
                    <h2 className="text-xl font-bold text-gray-900">{selectedCourse.name} 설문 응답 확인</h2>
                    <p className="text-gray-600 mt-1">
                      {selectedCourse.code} • {selectedCourse.instructor}
                    </p>
                  </div>
                  <Button variant="ghost" onClick={closeResponseModal} className="p-2">
                    <X className="w-5 h-5" />
                  </Button>
                </div>

                {/* 모달 내용 */}
                <div className="p-6">
                  {/* 선택된 템플릿 정보 표시 */}
                  {selectedCourse.templateList && selectedCourse.templateList.length > 0 && (
                    <div className="mb-6">
                      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                        <div className="flex justify-between items-start">
                          <div>
                            <h3 className="text-lg font-semibold text-blue-900 mb-2">
                              {selectedCourse.templateList[selectedTemplateIndex]?.questionTemplateName || `평가 ${selectedTemplateIndex + 1}`}
                            </h3>
                            <p className="text-sm text-blue-700 mb-1">
                              평가 기간: {selectedCourse.templateList[selectedTemplateIndex]?.openDate} ~ {selectedCourse.templateList[selectedTemplateIndex]?.closeDate}
                            </p>
                          </div>
                          <Badge className="bg-blue-200 text-blue-800">
                            평가 {selectedTemplateIndex + 1}
                          </Badge>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* 과정 정보 */}
                  <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-3">{selectedCourse.name}</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600">
                      <div className="flex items-center gap-2">
                        <Users className="w-4 h-4" />
                        <span>{selectedCourse.instructor}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Calendar className="w-4 h-4" />
                        <span>{selectedCourse.period}</span>
                      </div>
                    </div>
                  </div>

                  {/* 응답 내용 */}
                  <div className="mb-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">내 설문 응답</h3>
                    
                    {/* 실제 설문 문항이 있는 경우 */}
                    {evaluationQuestions.length > 0 ? (
                      <div className="space-y-8">
                        {evaluationQuestions.map((template, templateIndex) => (
                          <div key={template.questionTemplateNum || templateIndex} className="border border-gray-200 rounded-lg p-6">
                            <h4 className="text-lg font-medium text-gray-900 mb-4">
                              {template.questionTemplateName}
                            </h4>
                            <div className="space-y-6">
                              {template.questionList?.map((question) => {
                                // 해당 문항의 학생 응답 찾기
                                const studentResponse = studentResponses.find(
                                  response => response.evalQuestionId === question.evalQuestionId
                                )
                                

                                
                                // 백엔드 응답 형식에 맞춰 questionType 확인
                                // 백엔드에서 questionType: 0 = 객관식(5점 척도), 1 = 주관식
                                const isObjectiveQuestion = studentResponse?.questionType === 0
                                
                                return (
                                  <div key={question.evalQuestionId} className="border-b border-gray-100 pb-6 last:border-b-0">
                                    <div className="mb-3">
                                      <h5 className="font-medium text-gray-900 mb-2">
                                        {question.questionNum}. {question.evalQuestionText}
                                      </h5>
                                    </div>
                                    
                                    {/* 객관식(5점 척도) 문항 응답 */}
                                    {isObjectiveQuestion && (
                                      <div className="ml-6">
                                        <div className="flex items-center gap-2 mb-2">
                                          <span className="text-sm text-gray-600">내 평가:</span>
                                          <div className="flex gap-1">
                                            {[1, 2, 3, 4, 5].map((star) => (
                                              <Star 
                                                key={star}
                                                className={`w-5 h-5 ${star <= (studentResponse?.score || 0) ? "fill-yellow-400 text-yellow-400" : "text-gray-300"}`} 
                                              />
                                            ))}
                                          </div>
                                          <span className="text-sm font-medium text-gray-900">
                                            {studentResponse?.score || 0}점
                                          </span>
                                        </div>
                                      </div>
                                    )}
                                    
                                    {/* 서술형(주관식) 문항 응답 */}
                                    {!isObjectiveQuestion && (
                                      <div className="ml-6">
                                        <div className="mb-2">
                                          <span className="text-sm text-gray-600">내 답변:</span>
                                        </div>
                                        <div className="bg-gray-50 border border-gray-200 rounded-lg p-3">
                                          <p className="text-gray-900 whitespace-pre-wrap">
                                            {studentResponse?.answerText || "답변 없음"}
                                          </p>
                                        </div>
                                      </div>
                                    )}
                                  </div>
                                )
                              })}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      /* 설문 문항이 없는 경우 */
                      <div className="text-center py-8">
                        <p className="text-gray-500">설문 문항이 없습니다.</p>
                      </div>
                    )}
                  </div>

                  {/* 안내사항 */}
                  <div className="bg-gray-50 rounded-lg p-4 mb-6">
                    <div className="text-sm text-gray-600 space-y-2">
                      <p>• 설문은 익명으로 처리되며, 과정 개선을 위한 목적으로만 사용됩니다.</p>
                      <p>• 한 번 제출한 설문은 수정할 수 없습니다.</p>
                      <p>• 소중한 의견 감사합니다.</p>
                    </div>
                  </div>
                </div>

                {/* 모달 푸터 */}
                <div className="border-t border-gray-200 flex justify-end gap-3 p-6">
                  <Button 
                    className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                    onClick={closeResponseModal}
                  >
                    닫기
                  </Button>
                </div>
              </div>
            </div>
          )}
          </div>
        </main>
      </div>
    </div>
  )
}