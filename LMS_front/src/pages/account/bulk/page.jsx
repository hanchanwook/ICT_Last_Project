import { useState, useRef, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Upload, Download, RefreshCw, X } from "lucide-react"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getMenuItems } from "@/components/ui/menuConfig"
import * as XLSX from "xlsx"
import { jwtDecode } from "jwt-decode"
import { getAllCourse } from "@/api/suhyeon/courseApi"
import { http } from "@/components/auth/http"

const mapRoleToEnum = (role) => {
  switch (role?.trim()) {
    case "직원": return "ROLE_STAFF"
    case "강사": return "ROLE_INSTRUCTOR"
    case "학생": return "ROLE_STUDENT"
    default: return null
  }
}

// === 엑셀 헤더 키 정규화 및 null 처리 ===
const normalizeKeys = (row, educationId) => {
  const normalizedRow = {}
  Object.keys(row).forEach(key => {
    const cleanKey = key.replace(/\s+/g, "")
    normalizedRow[cleanKey] = row[key]
  })

  const courseKey = Object.keys(normalizedRow).find(k => k.includes("과정") || k.includes("과목"))
  const rawCourse = courseKey ? normalizedRow[courseKey] : null

  const courseName = (!rawCourse || rawCourse.toString().trim().toLowerCase() === "null")
    ? null
    : rawCourse.toString().trim()

  return {
    memberRole: mapRoleToEnum(normalizedRow["사용자유형"]),
    memberName: normalizedRow["이름"],
    password: normalizedRow["비밀번호"],
    memberEmail: normalizedRow["이메일"],
    memberPhone: normalizedRow["연락처"],
    memberBirthday: normalizedRow["생년월일"],
    courseName,
    memberAddress: normalizedRow["거주지"],
    educationId
  }
}

// === 중복 체크 ===
const checkDuplicateMembers = async (data) => {
  const seenEmails = new Set()
  const seenPhones = new Set()
  
  for (const item of data) {
    // 이메일 중복 체크
    if (seenEmails.has(item.memberEmail)) {
      return { hasDuplicate: true, type: 'email', value: item.memberEmail, message: '파일 내 이메일 중복' }
    }
    seenEmails.add(item.memberEmail)
    
    // 전화번호 중복 체크
    if (seenPhones.has(item.memberPhone)) {
      return { hasDuplicate: true, type: 'phone', value: item.memberPhone, message: '파일 내 전화번호 중복' }
    }
    seenPhones.add(item.memberPhone)
  }
  
  // DB와의 중복 체크
  for (const item of data) {
    try {
      // 이메일 중복 체크
      const emailResponse = await http.post("/api/registration/check-email", {
        memberEmail: item.memberEmail
      });
      
      if (emailResponse.data.isDuplicate) {
        return { hasDuplicate: true, type: 'email', value: item.memberEmail, message: 'DB에 이미 존재하는 이메일' }
      }
      
      // 전화번호 중복 체크
      const phoneResponse = await http.post("/api/registration/check-phone", {
        memberPhone: item.memberPhone
      });
      
      if (phoneResponse.data.isDuplicate) {
        return { hasDuplicate: true, type: 'phone', value: item.memberPhone, message: 'DB에 이미 존재하는 전화번호' }
      }
    } catch (error) {
      return { hasDuplicate: true, type: 'error', value: '', message: '중복 체크 중 오류가 발생했습니다.' }
    }
  }
  
  return { hasDuplicate: false }
}

export default function BulkRegisterPage() {
  const [uploadedFiles, setUploadedFiles] = useState([])
  const [fileDataMap, setFileDataMap] = useState({})
  const [previewData, setPreviewData] = useState([])
  const [courseList, setCourseList] = useState([])
  const sidebarMenuItems = getMenuItems("account")
  const fileInputRef = useRef(null)

  // JWT 토큰에서 educationId 추출
  const token = document.cookie.split('; ').find(c => c.startsWith('refresh='))?.split('=')[1]
  const educationId = token ? jwtDecode(token).educationId : null

  useEffect(() => {
    const fetchCourses = async () => {
      try {
        const res = await http.get("/api/getCourse/list", {
          params: { educationId: educationId },
        });

        const studentCountData = await getAllCourse();

        if (Array.isArray(res.data)) {
          const mergedCourseList = res.data.map(course => {
            const matchingCourse = studentCountData.find(sc => sc.courseId === course.courseId);
            if (matchingCourse) {
              return {
                ...course,
                studentCount: matchingCourse.studentCount,
                maxCapacity: matchingCourse.maxCapacity
              };
            }
            return {
              ...course,
              studentCount: 0,
              maxCapacity: 0
            };
          });
          setCourseList(mergedCourseList);
        } else {
          setCourseList([]);
        }
      } catch (err) {
        setCourseList([]);
      }
    };

    if (educationId) {
      fetchCourses();
    }
  }, [educationId])

  const getAvailableCourses = () => {
    return courseList
      .filter(course => course.studentCount < course.maxCapacity && course.courseActive === 0)
      .map(course => course.courseName)
  }

  const updatePreviewData = (dataMap) => {
    const merged = Object.values(dataMap).flat()
    setPreviewData(merged)
  }

  const parseExcelFile = async (file) => {
    const data = await file.arrayBuffer()
    const workbook = XLSX.read(data, { type: "array" })
    const sheetName = workbook.SheetNames[0]
    const worksheet = workbook.Sheets[sheetName]
    const jsonData = XLSX.utils.sheet_to_json(worksheet)

    const accountsWithEducationId = jsonData.map((row) =>
      normalizeKeys(row, educationId)
    )

    if (accountsWithEducationId.some((acc) => !acc.memberRole)) {
      throw new Error(`${file.name}: 사용자유형이 잘못 입력되었습니다.`)
    }
    if (accountsWithEducationId.some((acc) => !acc.password)) {
      throw new Error(`${file.name}: 비밀번호가 없는 행이 있습니다.`)
    }
    if (accountsWithEducationId.some((acc) =>
      acc.memberRole === "ROLE_STUDENT" && (acc.courseName == null)
    )) {
      throw new Error(`${file.name}: 학생은 반드시 과정명을 넣어야됩니다.`)
    }

    const availableCourses = getAvailableCourses()
    
    const invalidStudents = accountsWithEducationId.filter((acc) =>
      acc.memberRole === "ROLE_STUDENT" && !availableCourses.includes(acc.courseName)
    );
    
    if (invalidStudents.length > 0) {
      throw new Error(`${file.name}: 학생 수강인원 초과된 과정이 존재합니다. 다시 확인해주세요.`)
    }

    return accountsWithEducationId
  }

  const handleFileUpload = async (e) => {
    const files = Array.from(e.target.files)
    const validFiles = files.filter((file) => file.name.endsWith(".xlsx"))
    if (!validFiles.length) {
      alert("엑셀 파일만 선택해주세요.")
      return
    }

    const existingNames = uploadedFiles.map((f) => f.name)
    const newFiles = validFiles.filter((f) => !existingNames.includes(f.name))
    if (newFiles.length === 0) return

    let tempMap = { ...fileDataMap }
    const successFiles = []

    for (const file of newFiles) {
      try {
        const parsedData = await parseExcelFile(file)
        const testMap = { ...tempMap, [file.name]: parsedData }
        const merged = Object.values(testMap).flat()

        const duplicateCheckResult = await checkDuplicateMembers(merged);
        if (duplicateCheckResult.hasDuplicate) {
          alert(`${file.name}: ${duplicateCheckResult.message}`);
          continue;
        }

        tempMap[file.name] = parsedData
        successFiles.push(file)
      } catch (err) {
        alert(err.message)
      }
    }

    if (successFiles.length > 0) {
      setUploadedFiles((prev) => [...prev, ...successFiles])
      setFileDataMap(tempMap)
      updatePreviewData(tempMap)
    }
  }

  const handleRemoveFile = (fileName) => {
    setUploadedFiles((prev) => prev.filter((file) => file.name !== fileName))
    setFileDataMap((prev) => {
      const newMap = { ...prev }
      delete newMap[fileName]
      updatePreviewData(newMap)
      return newMap
    })
  }

  const handleUpload = async () => {
    try {
      const data = Array.isArray(previewData) ? previewData : [previewData];
      await http.post("/api/registration/registerAll", data);
      alert(`${data.length}개의 계정이 성공적으로 등록되었습니다!`);
      handleReset();
    } catch (error) {
      alert("업로드 중 오류가 발생했습니다.");
    }
  };

  const handleReset = () => {
    setUploadedFiles([])
    setFileDataMap({})
    setPreviewData([])
    if (fileInputRef.current) fileInputRef.current.value = ""
  }

  const downloadTemplate = () => {
    const templateData = [
      ["사용자유형", "이름", "비밀번호", "이메일", "연락처", "생년월일", "과정명(직원, 강사는 null로 할 것)", "거주지"],
      ["직원", "김직원", "password123", "employee001@example.com", "010-1234-5678", "1990-01-01", "null", "서울"],
      ["강사", "박강사", "password123", "teacher001@example.com", "010-2345-6789", "1985-05-15", "null", "서울"],
      ["학생", "이학생", "password123", "student001@example.com", "010-3456-7890", "2000-12-25", "과정 2", "서울"]
    ]
    const worksheet = XLSX.utils.aoa_to_sheet(templateData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, "Template")
    XLSX.writeFile(workbook, "계정등록_템플릿.xlsx")
  }

  return (
    <PageLayout currentPage="account">
      <div className="flex">
        <Sidebar title="계정 등록" menuItems={sidebarMenuItems} currentPath="/account/bulk" />
        <main className="flex-1 p-8">
          <div className="max-w-5xl mx-auto">
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>일괄 계정 등록</h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                엑셀 파일을 업로드하면 즉시 데이터를 합쳐서 미리보고, 바로 업로드할 수 있습니다.
              </p>
            </div>

            <Card className="mb-6">
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>템플릿 다운로드</CardTitle>
              </CardHeader>
              <CardContent className="flex items-center justify-between">
                <p className="text-sm" style={{ color: "#95A5A6" }}>엑셀 템플릿을 다운로드하여 데이터를 입력하세요.</p>
                <Button onClick={downloadTemplate} variant="outline" 
                className="text-[#1abc9c] bg-white hover:bg-[#1abc9c] hover:text-white">
                  <Download className="w-4 h-4 mr-2" /> 템플릿 다운로드
                </Button>
              </CardContent>
            </Card>

            <Card className="mb-6">
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>파일 업로드</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <input type="file" accept=".xlsx" multiple onChange={handleFileUpload} ref={fileInputRef} className="hidden" />
                  <div className="flex items-center space-x-4">
                    <Button type="button" variant="outline" 
                    onClick={() => fileInputRef.current.click()}
                    className="hover:bg-gray-100">
                      파일 선택
                    </Button>
                    {previewData.length > 0 && (
                      <>
                        <Button onClick={handleUpload} className="text-white font-medium flex items-center space-x-2"
                          style={{ backgroundColor: "#1ABC9C" }}>
                          <Upload className="w-4 h-4" /> <span>업로드</span>
                        </Button>
                        <Button type="button" variant="outline" onClick={handleReset} className="flex items-center space-x-2">
                          <RefreshCw className="w-4 h-4" /> <span>초기화</span>
                        </Button>
                      </>
                    )}
                  </div>

                  {uploadedFiles.length > 0 && (
                    <ul className="text-sm space-y-1" style={{ color: "#1ABC9C" }}>
                      {uploadedFiles.map((file) => (
                        <li key={file.name} className="flex items-center justify-between border-b py-1">
                          <span>{file.name}</span>
                          <button type="button" onClick={() => handleRemoveFile(file.name)} className="text-red-500 hover:text-red-700">
                            <X className="w-4 h-4" />
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </CardContent>
            </Card>

            {previewData.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle style={{ color: "#2C3E50" }}>데이터 미리보기</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="overflow-x-auto">
                    <table className="min-w-full border border-gray-200 text-sm">
                      <thead className="bg-gray-100">
                        <tr>
                          <th className="border px-4 py-2">역할</th>
                          <th className="border px-4 py-2">이름</th>
                          <th className="border px-4 py-2">이메일</th>
                          <th className="border px-4 py-2">연락처</th>
                          <th className="border px-4 py-2">생년월일</th>
                          <th className="border px-4 py-2">과정명</th>
                          <th className="border px-4 py-2">거주지</th>
                        </tr>
                      </thead>
                      <tbody>
                        {previewData.map((row, i) => (
                          <tr key={i}>
                            <td className="border px-4 py-2">{row.memberRole}</td>
                            <td className="border px-4 py-2">{row.memberName}</td>
                            <td className="border px-4 py-2">{row.memberEmail}</td>
                            <td className="border px-4 py-2">{row.memberPhone}</td>
                            <td className="border px-4 py-2">{row.memberBirthday || '-'}</td>
                            <td className="border px-4 py-2">{row.courseName}</td>
                            <td className="border px-4 py-2">{row.memberAddress}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </CardContent>
              </Card>
            )}
          </div>
        </main>
      </div>
    </PageLayout>
  )
}
