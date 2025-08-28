import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import * as XLSX from "xlsx"
import {
  Plus,
  Upload,
  Download,
  FileSpreadsheet,
  X,
} from "lucide-react"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getMenuItems } from "@/components/ui/menuConfig"
import { 
  createClassroom, 
  uploadCsvFile,
  getEducationId
} from "@/api/hancw/classroomAxios"

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

export default function ClassroomRegister() {
  const [formData, setFormData] = useState({
    classCode: "",
    classNumber: "",
    capacity: "",
    classActive: "0", // 0: 사용 가능, 1: 점검 중
    classRent: "0",   // 0: 공실, 1: 사용중
    classArea: "0",
    classMemo: "",
  })

  const [currentUser, setCurrentUser] = useState(null)
  const [educationId, setEducationId] = useState(null)
  const [educationName, setEducationName] = useState(null)
  const [isLoadingEducationId, setIsLoadingEducationId] = useState(true)
  const [educationIdError, setEducationIdError] = useState(null)

  const sidebarItems = getMenuItems('classroom')

  const [uploadedFile, setUploadedFile] = useState(null)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadResults, setUploadResults] = useState(null)

  // localStorage에서 사용자 정보 가져오기
  useEffect(() => {
    const userInfo = localStorage.getItem('currentUser')
    if (userInfo) {
      const parsedUser = JSON.parse(userInfo)
      setCurrentUser(parsedUser)
      console.log('현재 사용자 정보:', parsedUser)
    }
  }, [])

  // memberId로 educationId 조회
  useEffect(() => {
    const fetchEducationId = async () => {
      if (!currentUser?.memberId) {
        console.log('사용자 ID가 없습니다.')
        setIsLoadingEducationId(false)
        return
      }

      try {
        setIsLoadingEducationId(true)
        setEducationIdError(null)
        console.log('educationId 조회 시작 - 사용자 ID:', currentUser.memberId)
        
        const response = await getEducationId(currentUser.memberId)
        console.log('educationId 조회 응답:', response)
        
        if (response.success && response.educationId) {
          setEducationId(response.educationId)
          setEducationName(response.educationName)
          console.log('설정된 educationId:', response.educationId)
          console.log('설정된 educationName:', response.educationName)
        } else {
          throw new Error(response.message || '사용자의 교육기관 정보를 찾을 수 없습니다.')
        }
      } catch (error) {
        console.error('educationId 조회 실패:', error)
        setEducationIdError(error.message || '사용자 정보 조회에 실패했습니다.')
        setEducationId(null)
      } finally {
        setIsLoadingEducationId(false)
      }
    }

    if (currentUser) {
      fetchEducationId()
    }
  }, [currentUser])

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (!formData.classCode || !formData.classNumber || !formData.capacity) {
      alert("필수 항목을 모두 입력해주세요.")
      return
    }

    if (isLoadingEducationId) {
      alert("사용자 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.")
      return
    }

    if (educationIdError) {
      alert(`사용자 정보 조회 실패: ${educationIdError}`)
      return
    }

    if (!educationName) {
      alert("사용자의 교육기관 정보를 찾을 수 없습니다. 관리자에게 문의해주세요.")
      return
    }



    try {
      console.log('강의실 등록 시작 - 폼 데이터:', formData)
      console.log('사용할 educationId:', educationId)
      
      const classroomData = {
        classId: formData.classCode,
        classCode: formData.classCode,
        classNumber: parseInt(formData.classNumber),
        classCapacity: parseInt(formData.capacity),
        classActive: parseInt(formData.classActive),
        classRent: parseInt(formData.classRent),
        classArea: parseInt(formData.classArea),
        classPersonArea: 0,
        classMemo: formData.classMemo,
        educationId: educationId, // 백엔드 API에서 조회한 educationId 사용
        educationName: educationName // 백엔드 API에서 조회한 educationName 사용
      }

      console.log('전송할 강의실 데이터:', classroomData)
      const result = await createClassroom(classroomData)
      console.log('강의실 등록 결과:', result)
      
      alert("강의실이 성공적으로 등록되었습니다.")
      
      // 폼 초기화
      setFormData({
        classCode: "",
        classNumber: "",
        capacity: "",
        classActive: "0", // 0: 사용 가능, 1: 점검 중
        classRent: "0",   // 0: 공실, 1: 사용중
        classArea: "0",
        classMemo: "",
      })
    } catch (error) {
      console.error('강의실 등록 실패:', error)
      alert('강의실 등록에 실패했습니다.')
    }
  }

  const downloadTemplate = async () => {
    try {
      // 워크북 생성
      const workbook = XLSX.utils.book_new()
      
      // 데이터 템플릿 시트 생성 (실제 입력용)
      const templateData = [
        ['강의실명*', '강의호실*', '수용인원*', '강의실상태(0:사용가능, 1:점검중)', '사용여부(0:공실, 1:사용중)', '면적(㎡)', '1인당면적(㎡)', '비고'],
        ['A101', '101', '30', '0', '0', '45.50', '', '1층 일반강의실'],
        ['B203', '203', '50', '0', '0', '75.00', '', '컴퓨터 25대 설치'],
        ['C301', '301', '100', '0', '1', '150.00', '', '대형 강의실'],
        ['D401', '401', '20', '1', '0', '30.00', '', '소규모 세미나용 (점검중)'],
        ['E501', '501', '40', '0', '0', '60.00', '', '실습 장비 설치'],
        ['', '', '', '', '', '', '', ''],
        ['', '', '', '', '', '', '', ''],
        ['', '', '', '', '', '', '', ''],
        ['', '', '', '', '', '', '', ''],
        ['', '', '', '', '', '', '', ''],
        ['', '', '', '', '', '', '', ''],
        ['', '', '', '', '', '', '', ''],
        ['', '', '', '', '', '', '', ''],
        ['', '', '', '', '', '', '', ''],
        ['', '', '', '', '', '', '', '']
      ]
      
      const templateSheet = XLSX.utils.aoa_to_sheet(templateData)
      
      // 수식 추가 (1인당면적 = 면적/수용인원, 소수점 2자리까지 표시)
      templateSheet['G2'] = { f: 'ROUND(F2/C2, 2)' }  // A101: 45.50/30 = 1.52
      templateSheet['G3'] = { f: 'ROUND(F3/C3, 2)' }  // B203: 75.00/50 = 1.50
      templateSheet['G4'] = { f: 'ROUND(F4/C4, 2)' }  // C301: 150.00/100 = 1.50
      templateSheet['G5'] = { f: 'ROUND(F5/C5, 2)' }  // D401: 30.00/20 = 1.50
      templateSheet['G6'] = { f: 'ROUND(F6/C6, 2)' }  // E501: 60.00/40 = 1.50
      
      // 열 너비 설정
      const colWidths = [
        { wch: 12 }, // 강의실명
        { wch: 10 }, // 강의실호실
        { wch: 10 }, // 수용인원
        { wch: 10 }, // 강의실상태
        { wch: 10 }, // 사용여부
        { wch: 8 },  // 면적
        { wch: 10 }, // 1인당면적
        { wch: 30 }  // 비고
      ]
      templateSheet['!cols'] = colWidths
      
      XLSX.utils.book_append_sheet(workbook, templateSheet, '강의실_등록')
      
      // Excel 파일 생성 및 다운로드
      const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
      const blob = new Blob([excelBuffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = '강의실_등록_템플릿.xlsx'
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      alert('강의실 등록 템플릿이 다운로드되었습니다.')
    } catch (error) {
      console.error('템플릿 다운로드 실패:', error)
      alert('템플릿 다운로드에 실패했습니다.')
    }
  }

  const handleFileUpload = (event) => {
    const file = event.target.files[0]
    if (file) {
      setUploadedFile(file)
      setUploadResults(null)
    }
  }

  const handleBulkUpload = async () => {
    if (!uploadedFile) {
      alert("업로드할 파일을 선택해주세요.")
      return
    }

    if (isLoadingEducationId) {
      alert("사용자 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.")
      return
    }

    if (educationIdError) {
      alert(`사용자 정보 조회 실패: ${educationIdError}`)
      return
    }

    if (!educationId || !educationName) {
      alert("사용자의 교육기관 정보를 찾을 수 없습니다. 관리자에게 문의해주세요.")
      return
    }

    try {
      setIsUploading(true)
      console.log('엑셀 업로드 시작 - educationId:', educationId, 'educationName:', educationName)
      const result = await uploadCsvFile(uploadedFile, educationId, educationName)
      setUploadResults(result)
      alert("엑셀 파일 업로드가 완료되었습니다.")
    } catch (error) {
      console.error('대량 업로드 실패:', error)
      alert('엑셀 파일 업로드에 실패했습니다.')
    } finally {
      setIsUploading(false)
    }
  }

  const removeUploadedFile = () => {
    setUploadedFile(null)
    setUploadResults(null)
  }

  return (
    <PageLayout currentPage="classroom" userRole="staff">
      <div className="flex">
        <Sidebar title="강의실 관리" menuItems={sidebarItems} currentPath="/classroom/register" />

        <main className="flex-1 p-6">
          <div className="max-w-4xl mx-auto">
            <div className="mb-6">
              <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
                강의실 등록
              </h1>
              <p className="text-gray-600">새로운 강의실 정보를 입력하거나 엑셀 파일로 일괄 등록하세요.</p>
            </div>

            {/* 엑셀 업로드 섹션 */}
            <Card className="mb-6">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Upload className="w-5 h-5" style={{ color: "#1abc9c" }} />
                  파일 일괄 등록
                </CardTitle>
                <CardDescription>Excel 또는 CSV 파일을 업로드하여 여러 강의실을 한번에 등록할 수 있습니다.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center gap-4">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={downloadTemplate}
                    className="flex items-center gap-2 bg-transparent hover:bg-[#1abc9c]
                    text-[#1abc9c] hover:text-white border border-[#1abc9c]"
                  >
                    <Download className="w-4 h-4" />
                    템플릿 다운로드
                  </Button>
                  <span className="text-sm text-gray-500">템플릿을 다운로드하여 데이터를 입력한 후 업로드하세요.</span>
                </div>

                <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center">
                  {uploadedFile ? (
                    <div className="space-y-4">
                      <div className="flex items-center justify-center gap-2">
                        <FileSpreadsheet className="w-8 h-8" style={{ color: "#3498db" }} />
                        <span className="font-medium">{uploadedFile.name}</span>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={removeUploadedFile}
                          className="p-1"
                        >
                          <X className="w-4 h-4" />
                        </Button>
                      </div>
                      <Button
                        onClick={handleBulkUpload}
                        disabled={isUploading}
                        className="w-full"
                        style={{ backgroundColor: "#3498db" }}
                      >
                        {isUploading ? "업로드 중..." : "업로드 실행"}
                      </Button>
                    </div>
                  ) : (
                    <div>
                      <Upload className="w-12 h-12 mx-auto mb-4" style={{ color: "#95A5A6" }} />
                      <p className="text-lg font-medium mb-2" style={{ color: "#2C3E50" }}>
                        파일을 업로드하세요
                      </p>
                      <p className="text-sm mb-4" style={{ color: "#95A5A6" }}>
                        .xlsx, .xls, .csv 파일을 지원합니다.
                      </p>
                      <input
                        type="file"
                        accept=".xlsx,.xls,.csv"
                        onChange={handleFileUpload}
                        className="hidden"
                        id="file-upload"
                      />
                      <label
                        htmlFor="file-upload"
                        className="cursor-pointer inline-flex items-center 
                        px-4 py-2 border border-[#1abc9c] text-sm font-medium 
                        rounded-md text-[#1abc9c] bg-white hover:bg-[#1abc9c] hover:text-white"
                      >
                        파일 선택
                      </label>
                    </div>
                  )}
                </div>

                
              </CardContent>
            </Card>

            {/* 개별 등록 폼 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Plus className="w-5 h-5" style={{ color: "#1abc9c" }} />
                  개별 강의실 등록
                </CardTitle>
                <CardDescription>새로운 강의실 정보를 직접 입력하여 등록할 수 있습니다.</CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* 기본 정보 */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* 왼쪽 열: 강의실명, 강의호실, 수용인원, 면적 */}
                    <div className="space-y-4">
                      <div>
                        <Label htmlFor="classCode" className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                          강의실명 *
                        </Label>
                        <Input
                          id="classCode"
                          value={formData.classCode}
                          onChange={(e) => handleInputChange("classCode", e.target.value)}
                          placeholder="예: R001"
                          required
                        />
                      </div>
                      
                      <div>
                        <Label htmlFor="classNumber" className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                          강의호실 *
                        </Label>
                        <Input
                          id="classNumber"
                          value={formData.classNumber}
                          onChange={(e) => handleInputChange("classNumber", e.target.value)}
                          placeholder="예: A-101"
                          required
                        />
                      </div>
                      
                      <div>
                        <Label htmlFor="capacity" className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                          수용 인원
                        </Label>
                        <Input
                          id="capacity"
                          type="number"
                          value={formData.capacity}
                          onChange={(e) => handleInputChange("capacity", e.target.value)}
                          placeholder="예: 30"
                          required
                        />
                      </div>
                      
                      <div>
                        <Label htmlFor="classArea" className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                          면적 (㎡)
                        </Label>
                        <Input
                          id="classArea"
                          type="number"
                          value={formData.classArea}
                          onChange={(e) => handleInputChange("classArea", e.target.value)}
                          placeholder="예: 50"
                        />
                      </div>
                    </div>

                    {/* 오른쪽 열: 강의실 상태, 사용 여부, 설명 */}
                    <div className="space-y-4">
                      {/* 강의실 상태와 사용 여부를 가로로 배치 */}
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <Label htmlFor="classActive" className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            강의실 상태
                          </Label>
                          <Select
                            value={formData.classActive}
                            onValueChange={(value) => handleInputChange("classActive", value)}
                          >
                            <SelectTrigger className="bg-white border border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-200">
                              <SelectValue placeholder="강의실 상태를 선택하세요" />
                            </SelectTrigger>
                            <SelectContent className="bg-white border border-gray-200 shadow-lg">
                              <SelectItem value="0">사용 가능</SelectItem>
                              <SelectItem value="1">점검 중</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                        
                        <div>
                          <Label htmlFor="classRent" className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                            강의실 사용 
                          </Label>
                          <Select
                            value={formData.classRent}
                            onValueChange={(value) => handleInputChange("classRent", value)}
                          >
                            <SelectTrigger className="bg-white border border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-200">
                              <SelectValue placeholder="강의실 사용 상태를 선택하세요" />
                            </SelectTrigger>
                            <SelectContent className="bg-white border border-gray-200 shadow-lg">
                              <SelectItem value="0">사용 가능</SelectItem>
                              <SelectItem value="1">사용 중</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                      </div>
                      
                      <div>
                        <Label htmlFor="classMemo" className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                          설명
                        </Label>
                        <Textarea
                          id="classMemo"
                          value={formData.classMemo}
                          onChange={(e) => handleInputChange("classMemo", e.target.value)}
                          placeholder="강의실에 대한 추가 설명을 입력하세요."
                          rows="20"
                          className="bg-white border border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 min-h-[150px]"
                          style={{ height: '150px' }}
                        />
                      </div>
                    </div>
                  </div>

                  {/* 등록 버튼 */}
                  <div className="flex justify-end">
                    <Button
                      type="submit"
                      className="px-8 text-[#1abc9c] bg-white border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                                             disabled={isLoadingEducationId || educationIdError || !educationId || !educationName}
                    >
                                             {isLoadingEducationId ? "사용자 정보 로딩 중..." : 
                        educationIdError ? "사용자 정보 오류" :
                        !educationId || !educationName ? "교육기관 정보 없음" : "강의실 등록"}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
    </PageLayout>
  )
}
