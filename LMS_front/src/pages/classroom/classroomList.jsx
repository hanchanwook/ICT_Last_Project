import { useState, useEffect, useRef } from "react"
import { Search, Filter, Plus, Edit, Trash2, Eye, MapPin, Settings, Check, X, Power, ChevronDown } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getMenuItems } from "@/components/ui/menuConfig"
import { jwtDecode } from "jwt-decode"

import { 
  getAllClassrooms, 
  searchAndFilterClassrooms, 
  deleteClassroomWithCleanup,
  updateClassroom,
  getClassroomWithEquipment 
} from "@/api/hancw/classroomAxios"
import {
  createClassroomEquipment,
  updateClassroomEquipment,
  deleteClassroomEquipment,
  updateEquipmentStatus,
  uploadEquipmentExcel,
  downloadEquipmentTemplate
} from "@/api/hancw/classroomEquipmentAxios"

export default function ClassroomRoomPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedStatus, setSelectedStatus] = useState("all")

  const [selectedRoom, setSelectedRoom] = useState(null)
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false)
  const [isEditMode, setIsEditMode] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [roomsData, setRoomsData] = useState([])
  const [filteredRooms, setFilteredRooms] = useState([])
  const [educationId, setEducationId] = useState(null)

  // 페이징 상태
  const [currentPage, setCurrentPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(20)
  const [isItemsPerPageOpen, setIsItemsPerPageOpen] = useState(false)
  const itemsPerPageRef = useRef(null)

  // 장비 관리 상태
  const [isEquipmentEditMode, setIsEquipmentEditMode] = useState(false)
  const [isExcelUploadMode, setIsExcelUploadMode] = useState(false)
  const [selectedExcelFile, setSelectedExcelFile] = useState(null)
  const [newEquipment, setNewEquipment] = useState({
    equipName: '',
    equipModel: '',
    equipNumber: 1,
    equipRent: 0,
    equipDescription: '',
    equipActive: 0
  })
  const [editingEquipment, setEditingEquipment] = useState(null)

  const sidebarMenuItems = getMenuItems('classroom')

  // JWT 토큰에서 educationId 추출
  const extractEducationIdFromToken = () => {
    try {
      const token = document.cookie
        .split('; ')
        .find(row => row.startsWith('refresh='))
        ?.split('=')[1]
      
      if (token) {
        const decoded = jwtDecode(token)
        console.log('JWT 토큰 디코딩 결과:', decoded)
        console.log('추출된 educationId:', decoded.educationId)
        return decoded.educationId
      } else {
        console.error('JWT 토큰을 쿠키에서 찾을 수 없습니다.')
        console.log('현재 쿠키들:', document.cookie)
      }
    } catch (error) {
      console.error('JWT 토큰 디코딩 오류:', error)
    }
    return null
  }

  // educationId 추출 및 설정
  useEffect(() => {
    const extractedEducationId = extractEducationIdFromToken()
    setEducationId(extractedEducationId)
  }, [])

  // 강의실 데이터 로드
  useEffect(() => {
    if (educationId) {
      loadClassrooms()
    }
  }, [educationId])

  // 검색 및 필터링 적용
  useEffect(() => {
    applyFilters()
  }, [searchTerm, selectedStatus, roomsData])

  // 필터링이 변경될 때 페이지를 1로 초기화
  useEffect(() => {
    setCurrentPage(1)
  }, [searchTerm, selectedStatus, itemsPerPage])

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (itemsPerPageRef.current && !itemsPerPageRef.current.contains(event.target)) {
        setIsItemsPerPageOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const loadClassrooms = async () => {
    try {
      setLoading(true)
      console.log('강의실 목록 API 호출 시작...')
      console.log('전송할 educationId:', educationId)
      const data = await getAllClassrooms(educationId)
      console.log('강의실 목록 받은 데이터:', data)
      console.log('강의실 개수:', data?.length)
      console.log('첫 번째 강의실 데이터:', data?.[0])
      setRoomsData(data)
      setError(null)
    } catch (err) {
      console.error('강의실 목록 로드 실패:', err)
      setError('강의실 목록을 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const applyFilters = async () => {
    try {
      console.log('필터 적용 - 검색어:', searchTerm, '상태:', selectedStatus)
      console.log('roomsData 개수:', roomsData.length)
      console.log('첫 번째 강의실 데이터:', roomsData[0])
      
      // 클라이언트 사이드 필터링 (백엔드 API 대신)
      let filteredData = roomsData
      
      // 검색어 필터링
      if (searchTerm && searchTerm.trim() !== '') {
        const searchLower = searchTerm.toLowerCase().trim()
        console.log('검색어 (소문자):', searchLower)
        
        filteredData = filteredData.filter(room => {
          const classCode = String(room.classCode || room.classroomName || room.name || '')
          const classNumber = String(room.classNumber || room.roomNumber || room.number || '')
          const classMemo = String(room.classMemo || room.memo || room.description || '')
          
          console.log('검색 대상:', {
            classCode: classCode,
            classNumber: classNumber,
            classMemo: classMemo,
            searchTerm: searchLower
          })
          
          const matches = 
            classCode.toLowerCase().includes(searchLower) ||
            classNumber.toLowerCase().includes(searchLower) ||
            classMemo.toLowerCase().includes(searchLower)
          
          console.log('매칭 결과:', matches)
          return matches
        })
        console.log('검색어 필터링 후 개수:', filteredData.length)
      }
      
      // 상태 필터링 (classRent + classActive 조합)
      if (selectedStatus !== "all") {
        const [rentStatus, activeStatus] = selectedStatus.split('-')
        
        filteredData = filteredData.filter(room => {
          // 사용 여부 필터링 (classRent)
          const rentMatch = rentStatus === "available" ? room.classRent === 0 : 
                           rentStatus === "inUse" ? room.classRent === 1 : true
          
          // 상태 필터링 (classActive)
          const activeMatch = activeStatus === "active" ? room.classActive === 0 : 
                             activeStatus === "inactive" ? room.classActive === 1 : true
          
          return rentMatch && activeMatch
        })
      }
      
      console.log('필터링된 데이터:', filteredData)
      setFilteredRooms(filteredData)
    } catch (err) {
      console.error('필터링 실패:', err)
      setFilteredRooms(roomsData)
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case "사용중":
        return "#e74c3c"
      case "사용가능":
        return "#1ABC9C"
      case "점검중":
        return "#95A5A6"
      default:
        return "#95A5A6"
    }
  }

  const getTypeColor = (type) => {
    switch (type) {
      case "일반강의실":
        return "#3498db"
      case "컴퓨터실":
        return "#9b59b6"
      case "세미나실":
        return "#1ABC9C"
      case "대강의실":
        return "#e67e22"
      case "실습실":
        return "#34495e"
      default:
        return "#95A5A6"
    }
  }

  // 강의실 통계 계산 (백엔드 필드명에 맞춰 수정)
  const stats = {
    total: roomsData.length,
    inUse: roomsData.filter((r) => r.classRent === 1).length, // 사용중인 강의실
    available: roomsData.filter((r) => r.classRent === 0 && r.classActive === 0).length, // 사용가능한 강의실
    inactive: roomsData.filter((r) => r.classActive === 1).length, // 점검 중인 강의실
    totalCapacity: roomsData.reduce((sum, room) => sum + (parseInt(room.classCapacity) || 0), 0),
    currentOccupancy: 0, // 현재 수강생 수는 별도 필드가 없으므로 0
  }
  
  console.log('강의실 통계:', stats)
  console.log('현재 roomsData:', roomsData)
  console.log('현재 filteredRooms:', filteredRooms)

  const handleView = async (roomId) => {
    try {
      console.log('강의실 상세 정보 조회 시작 - ID:', roomId)
      const roomData = await getClassroomWithEquipment(roomId)
      console.log('받은 강의실 상세 데이터:', roomData)
      setSelectedRoom(roomData)
      setIsDetailModalOpen(true)
      setIsEditMode(false)
    } catch (err) {
      console.error('강의실 상세 정보 로드 실패:', err)
      alert('강의실 상세 정보를 불러오는데 실패했습니다.')
    }
  }

  const handleCloseModal = () => {
    setIsDetailModalOpen(false)
    setSelectedRoom(null)
    setIsEditMode(false)
    setIsEquipmentEditMode(false)
    setIsExcelUploadMode(false)
    setSelectedExcelFile(null)
    setEditingEquipment(null)
    setNewEquipment({
      equipName: '',
      equipModel: '',
      equipNumber: 1,
      equipRent: 0,
      equipDescription: '',
      equipActive: 0
    })
  }

  const handleEditRoom = () => {
    setIsEditMode(true)
  }

  const handleSaveRoom = async () => {
    try {
      console.log('저장할 강의실 데이터:', selectedRoom.classroom)
      await updateClassroom(selectedRoom.classroom.classId, selectedRoom.classroom)
      alert("강의실 정보가 수정되었습니다.")
      setIsEditMode(false)
      loadClassrooms() // 목록 새로고침
    } catch (err) {
      console.error('강의실 수정 실패:', err)
      alert('강의실 정보 수정에 실패했습니다.')
    }
  }

  const handleDeleteRoom = async () => {
    if (confirm("정말로 이 강의실을 삭제하시겠습니까?\n(관련된 장비 정보도 함께 삭제됩니다.)")) {
      try {
        await deleteClassroomWithCleanup(selectedRoom.classroom.classId)
        alert(`${selectedRoom.classroom.classCode} 강의실이 삭제되었습니다.`)
        handleCloseModal()
        loadClassrooms() // 목록 새로고침
      } catch (err) {
        console.error('강의실 삭제 실패:', err)
        alert('강의실 삭제에 실패했습니다.')
      }
    }
  }

  const handleRoomFieldChange = (field, value) => {
    console.log('필드 변경:', field, value)
    setSelectedRoom((prev) => ({
      ...prev,
      classroom: {
        ...prev.classroom,
        [field]: value,
      }
    }))
  }

  // 장비 관리 함수들
  const handleAddEquipment = async () => {
    try {
      if (!newEquipment.equipName.trim()) {
        alert('장비명을 입력해주세요.')
        return
      }

      if (!newEquipment.equipModel.trim()) {
        alert('장비 모델을 입력해주세요.')
        return
      }

      const equipmentData = {
        ...newEquipment,
        classId: selectedRoom.classroom.classId
      }

      console.log('장비 추가 시작:', equipmentData)
      await createClassroomEquipment(equipmentData)
      
      // 강의실 정보 새로고침
      const updatedRoomData = await getClassroomWithEquipment(selectedRoom.classroom.classId)
      setSelectedRoom(updatedRoomData)
      
      // 새 장비 폼 초기화
      setNewEquipment({
        equipName: '',
        equipModel: '',
        equipNumber: 1,
        equipRent: 0,
        equipDescription: '',
        equipActive: 0
      })
      
      alert('장비가 추가되었습니다.')
    } catch (error) {
      console.error('장비 추가 실패:', error)
      alert('장비 추가에 실패했습니다.')
    }
  }

  const handleEditEquipment = (equipment) => {
    setEditingEquipment({
      classEquipId: equipment.classEquipId || equipment.id,
      equipName: equipment.equipName || equipment.equipmentName || equipment.name || '',
      equipModel: equipment.equipModel || equipment.model || '',
      equipNumber: equipment.equipNumber || equipment.number || 1,
      equipRent: equipment.equipRent || equipment.rent || 0,
      equipDescription: equipment.equipDescription || equipment.description || ''
    })
    setIsEquipmentEditMode(true)
  }

  const handleSaveEquipmentEdit = async () => {
    try {
      if (!editingEquipment.equipName.trim()) {
        alert('장비명을 입력해주세요.')
        return
      }

      if (!editingEquipment.equipModel.trim()) {
        alert('장비 모델을 입력해주세요.')
        return
      }

      console.log('장비 수정 시작:', editingEquipment)
      await updateClassroomEquipment(editingEquipment.classEquipId, editingEquipment)
      
      // 강의실 정보 새로고침
      const updatedRoomData = await getClassroomWithEquipment(selectedRoom.classroom.classId)
      setSelectedRoom(updatedRoomData)
      
      setEditingEquipment(null)
      setIsEquipmentEditMode(false)
      
      alert('장비가 수정되었습니다.')
    } catch (error) {
      console.error('장비 수정 실패:', error)
      alert('장비 수정에 실패했습니다.')
    }
  }

  const handleDeleteEquipment = async (equipmentId) => {
    if (confirm('정말로 이 장비를 삭제하시겠습니까?')) {
      try {
        console.log('장비 삭제 시작 - ID:', equipmentId)
        await deleteClassroomEquipment(equipmentId)
        
        // 강의실 정보 새로고침
        const updatedRoomData = await getClassroomWithEquipment(selectedRoom.classroom.classId)
        setSelectedRoom(updatedRoomData)
        
        alert('장비가 삭제되었습니다.')
      } catch (error) {
        console.error('장비 삭제 실패:', error)
        alert('장비 삭제에 실패했습니다.')
      }
    }
  }

  const handleEquipmentStatusChange = async (equipmentId, newStatus) => {
    try {
      console.log('장비 상태 변경 시작 - ID:', equipmentId, '상태:', newStatus)
      await updateEquipmentStatus(equipmentId, newStatus)
      
      // 강의실 정보 새로고침
      const updatedRoomData = await getClassroomWithEquipment(selectedRoom.classroom.classId)
      setSelectedRoom(updatedRoomData)
      
      alert('장비 상태가 변경되었습니다.')
    } catch (error) {
      console.error('장비 상태 변경 실패:', error)
      alert('장비 상태 변경에 실패했습니다.')
    }
  }

  const handleCancelEquipmentEdit = () => {
    setEditingEquipment(null)
    setIsEquipmentEditMode(false)
  }

  // Excel 업로드 관련 함수들
  const handleExcelFileSelect = (event) => {
    const file = event.target.files[0]
    if (file) {
      if (file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
          file.type === 'application/vnd.ms-excel') {
        setSelectedExcelFile(file)
      } else {
        alert('Excel 파일(.xlsx, .xls)만 업로드 가능합니다.')
        event.target.value = null
      }
    }
  }

  const handleExcelUpload = async () => {
    if (!selectedExcelFile) {
      alert('업로드할 Excel 파일을 선택해주세요.')
      return
    }

    if (!selectedRoom?.classroom?.classId) {
      alert('강의실 정보가 없습니다.')
      return
    }

    try {
      console.log('Excel 파일 업로드 시작:', selectedExcelFile.name)
      console.log('현재 선택된 강의실 ID:', selectedRoom.classroom.classId)
      await uploadEquipmentExcel(selectedExcelFile, selectedRoom.classroom.classId)
      
      // 강의실 정보 새로고침
      const updatedRoomData = await getClassroomWithEquipment(selectedRoom.classroom.classId)
      setSelectedRoom(updatedRoomData)
      
      setSelectedExcelFile(null)
      setIsExcelUploadMode(false)
      
      alert('Excel 파일 업로드가 완료되었습니다.')
    } catch (error) {
      console.error('Excel 업로드 실패:', error)
      alert('Excel 파일 업로드에 실패했습니다.')
    }
  }

  const handleDownloadTemplate = async () => {
    try {
      console.log('장비 템플릿 다운로드 시작')
      
      const response = await downloadEquipmentTemplate()
      
      // Blob 생성 및 다운로드
      const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = '강의실장비_템플릿.xlsx'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      
      alert('강의실 장비 템플릿이 다운로드되었습니다.')
    } catch (error) {
      console.error('템플릿 다운로드 실패:', error)
      alert('템플릿 다운로드에 실패했습니다.')
    }
  }

  const handleEdit = async (roomId) => {
    try {
      console.log("강의실 수정 시작 - ID:", roomId)
      const roomData = await getClassroomWithEquipment(roomId)
      console.log('수정할 강의실 데이터:', roomData)
      setSelectedRoom(roomData)
      setIsDetailModalOpen(true)
      setIsEditMode(true) // 수정 모드로 설정
      setEditingEquipment(null) // 편집 중인 장비 초기화
    } catch (err) {
      console.error('강의실 수정 데이터 로드 실패:', err)
      alert('강의실 정보를 불러오는데 실패했습니다.')
    }
  }

  const handleRoomStatusToggle = async (roomId, currentStatus) => {
    const newStatus = currentStatus === 0 ? 1 : 0
    const statusText = newStatus === 0 ? "사용 가능" : "점검 중"
    const confirmText = currentStatus === 0 ? "점검 중으로 변경" : "사용 가능으로 변경"
    
    if (confirm(`정말로 이 강의실을 ${confirmText}하시겠습니까?`)) {
      try {
        console.log('강의실 상태 변경 시작 - ID:', roomId, '현재 상태:', currentStatus, '새 상태:', newStatus)
        
        // 강의실 상태 업데이트를 위한 데이터 준비
        const updateData = {
          classActive: newStatus
        }
        
        await updateClassroom(roomId, updateData)
        alert(`강의실이 ${statusText}로 변경되었습니다.`)
        loadClassrooms() // 목록 새로고침
      } catch (err) {
        console.error('강의실 상태 변경 실패:', err)
        alert('강의실 상태 변경에 실패했습니다.')
      }
    }
  }

  const handleDelete = async (roomId) => {
    if (confirm("정말로 이 강의실을 삭제하시겠습니까?\n(관련된 장비 정보도 함께 삭제됩니다.)")) {
      try {
        await deleteClassroomWithCleanup(roomId)
        alert(`${roomId} 강의실이 삭제되었습니다.`)
        loadClassrooms() // 목록 새로고침
      } catch (err) {
        console.error('강의실 삭제 실패:', err)
        alert('강의실 삭제에 실패했습니다.')
      }
    }
  }

  // 페이징된 데이터 계산
  const getPaginatedData = () => {
    const startIndex = (currentPage - 1) * itemsPerPage
    const endIndex = startIndex + itemsPerPage
    return filteredRooms.slice(startIndex, endIndex)
  }

  // 총 페이지 수 계산
  const getTotalPages = () => {
    return Math.ceil(filteredRooms.length / itemsPerPage)
  }

  // 페이지 변경 핸들러
  const handlePageChange = (page) => {
    setCurrentPage(page)
  }

  if (loading) {
    return (
      <PageLayout currentPage="classroom">
        <div className="flex">
          <Sidebar title="강의실 관리" menuItems={sidebarMenuItems} currentPath="/classroom/room" />
          <main className="flex-1 p-8">
            <div className="max-w-7xl mx-auto">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
                <p className="mt-4 text-gray-600">데이터를 불러오는 중...</p>
              </div>
            </div>
          </main>
        </div>
      </PageLayout>
    )
  }

  if (error) {
    return (
      <PageLayout currentPage="classroom">
        <div className="flex">
          <Sidebar title="강의실 관리" menuItems={sidebarMenuItems} currentPath="/classroom/room" />
          <main className="flex-1 p-8">
            <div className="max-w-7xl mx-auto">
              <div className="text-center">
                <p className="text-red-500 mb-4">{error}</p>
                <Button onClick={loadClassrooms}>다시 시도</Button>
              </div>
            </div>
          </main>
        </div>
      </PageLayout>
    )
  }

  return (
    <PageLayout currentPage="classroom" userRole="staff">
      <div className="flex">
        <Sidebar title="강의실 관리" menuItems={sidebarMenuItems} currentPath="/classroom/room" />

        <main className="flex-1 p-8">
          <div className="max-w-7xl mx-auto">
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                강의실 목록
              </h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                등록된 모든 강의실의 정보를 조회하고 관리할 수 있습니다.
              </p>
            </div>

            {/* 검색 및 필터 */}
            <Card className="mb-6">
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>검색 및 필터</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-col md:flex-row gap-4">
                  <div className="flex-1">
                    <div className="relative">
                      <Search
                        className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4"
                        style={{ color: "#95A5A6" }}
                      />
                      <Input
                        placeholder="강의실명, 강의호실, 메모로 검색..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="pl-10"
                      />
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <select
                      value={selectedStatus}
                      onChange={(e) => setSelectedStatus(e.target.value)}
                      className="px-3 py-2 border rounded-md"
                      style={{ 
                        borderColor: "#95A5A6",
                        borderTopWidth: "2px",
                        borderBottomWidth: "2px"
                      }}
                    >
                      <option value="all">전체</option>
                      <option value="available-active">사용 가능</option>
                      <option value="available-inactive">점검 중</option>
                      <option value="inUse-active">사용 중</option>
                    </select>

                    {/* 페이지당 항목 수 선택 */}
                    <div className="relative" ref={itemsPerPageRef}>
                      <button
                        onClick={() => setIsItemsPerPageOpen(!isItemsPerPageOpen)}
                        className="flex items-center justify-between px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white min-w-[120px]"
                        style={{ 
                          borderColor: "#95A5A6",
                          borderTopWidth: "2px",
                          borderBottomWidth: "2px"
                        }}
                      >
                        <span className="text-gray-700">
                          {itemsPerPage}개씩
                        </span>
                        <ChevronDown className={`w-4 h-4 text-gray-600 transition-transform ${isItemsPerPageOpen ? 'rotate-180' : ''}`} />
                      </button>
                      
                      {isItemsPerPageOpen && (
                        <div className="absolute top-full left-0 right-0 mt-1 bg-white border-2 border-gray-300 rounded-md shadow-lg z-10">
                          {[20, 50, 100].map((size, index) => (
                            <div 
                              key={size}
                              className={`px-3 py-2 hover:bg-blue-50 cursor-pointer ${index < 2 ? 'border-b border-gray-200' : ''}`}
                              onClick={() => {
                                setItemsPerPage(size)
                                setIsItemsPerPageOpen(false)
                              }}
                            >
                              {size}개씩
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 통계 카드 */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#3498db" }}>
                    {stats.total}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    전체 강의실
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#1ABC9C" }}>
                    {stats.available}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    사용가능
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#b0c4de" }}>
                    {stats.inUse}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    사용중
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <div className="text-2xl font-bold" style={{ color: "#95A5A6" }}>
                    {stats.inactive}
                  </div>
                  <div className="text-sm" style={{ color: "#95A5A6" }}>
                    점검 중
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* 강의실 목록 테이블 */}
            <Card>
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>강의실 목록({filteredRooms.length}개)</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b" style={{ borderColor: "#95A5A6" }}>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          강의실명
                        </th>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          강의호실
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          수용인원
                        </th>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          학원명
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          강의실 사용
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          강의실 상태
                        </th>
                        <th className="text-center py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          관리자 작업
                        </th>
                        <th className="text-left py-3 px-4 font-medium" style={{ color: "#2C3E50" }}>
                          비고
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {getPaginatedData().map((room) => (
                        <tr key={room.classId} className="border-b" style={{ borderColor: "#f1f2f6" }}>
                          <td className="py-3 px-4">
                            <span className="font-mono text-sm" style={{ color: "#2C3E50" }}>
                              {room.classCode}
                            </span>
                          </td>
                          <td className="py-3 px-4">
                            <div className="flex items-center space-x-2">
                              <MapPin className="w-4 h-4" style={{ color: "#3498db" }} />
                              <span className="font-medium" style={{ color: "#2C3E50" }}>
                                {room.classNumber}
                              </span>
                            </div>
                          </td>
                          <td className="py-3 px-4 text-center">
                            <div className="flex flex-col items-center">
                              <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                {room.classCapacity}명
                              </span>
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <span className="text-sm" style={{ color: "#2C3E50" }}>
                              {room.educationName || `${room.educationId}`}
                            </span>
                          </td>
                          <td className="py-3 px-4 text-center">
                            <Badge
                              className="text-xs text-white"
                              style={{ backgroundColor: room.classRent ? "#e74c3c" : "#1ABC9C" }}
                            >
                              {room.classRent ? "사용 중" : "사용 가능"}
                            </Badge>
                          </td>
                          <td className="py-3 px-4 text-center">
                            <Badge
                              className="text-xs text-white"
                              style={{ backgroundColor: room.classActive === 0 ? "#1ABC9C" : "#95A5A6" }}
                            >
                              {room.classActive === 0 ? "사용 가능" : "점검 중"}
                            </Badge>
                          </td>
                          <td className="py-3 px-4 text-center">
                            <div className="flex justify-center space-x-2">
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleView(room.classId)}
                                className="p-1 hover:bg-blue-50 hover:scale-110 transition-all duration-200"
                                title="상세보기"
                              >
                                <Eye className="w-4 h-4" style={{ color: "#1abc9c" }} />
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleEdit(room.classId)}
                                className="p-1 hover:bg-blue-50 hover:scale-110 transition-all duration-200"
                                title="수정"
                              >
                                <Edit className="w-4 h-4" style={{ color: "#b0c4de" }} />
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleRoomStatusToggle(room.classId, room.classActive)}
                                className="p-1 hover:bg-red-50 hover:scale-110 transition-all duration-200"
                                title={room.classActive === 0 ? "점검 중으로 변경" : "사용 가능으로 변경"}
                              >
                                <Power className="w-4 h-4" style={{ color: room.classActive === 0 ?"#e74c3c" : "#95A5A6" }} />
                              </Button>
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <div>
                              <p className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                {room.classMemo || "-"}
                              </p>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>

            {/* 페이징 컴포넌트 */}
            {getTotalPages() > 1 && (
              <div className="flex justify-center items-center mt-6 space-x-2">
                {/* 이전 페이지 버튼 */}
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 1}
                  className="px-3 py-1 transition-colors duration-200"
                  style={{ 
                    borderColor: "#1ABC9C", 
                    color: "#1ABC9C",
                    opacity: currentPage === 1 ? 0.5 : 1
                  }}
                  onMouseEnter={e => {
                    if (currentPage !== 1) {
                      e.currentTarget.style.backgroundColor = '#1ABC9C';
                      e.currentTarget.style.color = '#fff';
                    }
                  }}
                  onMouseLeave={e => {
                    if (currentPage !== 1) {
                      e.currentTarget.style.backgroundColor = 'transparent';
                      e.currentTarget.style.color = '#1ABC9C';
                    }
                  }}
                >
                  이전
                </Button>

                {/* 페이지 번호들 */}
                {Array.from({ length: getTotalPages() }, (_, i) => i + 1).map((page) => (
                  <Button
                    key={page}
                    variant={currentPage === page ? "default" : "outline"}
                    size="sm"
                    onClick={() => handlePageChange(page)}
                    className="px-3 py-1 transition-colors duration-200"
                    style={{
                      backgroundColor: currentPage === page ? "#1ABC9C" : "transparent",
                      borderColor: "#1ABC9C",
                      color: currentPage === page ? "white" : "#1ABC9C"
                    }}
                    onMouseEnter={e => {
                      if (currentPage !== page) {
                        e.currentTarget.style.backgroundColor = '#1ABC9C';
                        e.currentTarget.style.color = '#fff';
                      }
                    }}
                    onMouseLeave={e => {
                      if (currentPage !== page) {
                        e.currentTarget.style.backgroundColor = 'transparent';
                        e.currentTarget.style.color = '#1ABC9C';
                      }
                    }}
                  >
                    {page}
                  </Button>
                ))}

                {/* 다음 페이지 버튼 */}
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage === getTotalPages()}
                  className="px-3 py-1 transition-colors duration-200"
                  style={{ 
                    borderColor: "#1ABC9C", 
                    color: "#1ABC9C",
                    opacity: currentPage === getTotalPages() ? 0.5 : 1
                  }}
                  onMouseEnter={e => {
                    if (currentPage !== getTotalPages()) {
                      e.currentTarget.style.backgroundColor = '#1ABC9C';
                      e.currentTarget.style.color = '#fff';
                    }
                  }}
                  onMouseLeave={e => {
                    if (currentPage !== getTotalPages()) {
                      e.currentTarget.style.backgroundColor = 'transparent';
                      e.currentTarget.style.color = '#1ABC9C';
                    }
                  }}
                >
                  다음
                </Button>
              </div>
            )}

            {/* 페이지 정보 표시 */}
            {filteredRooms.length > 0 && (
              <div className="text-center mt-4">
                <p style={{ color: "#95A5A6" }}>
                  {((currentPage - 1) * itemsPerPage) + 1} - {Math.min(currentPage * itemsPerPage, filteredRooms.length)} / {filteredRooms.length}개
                </p>
              </div>
            )}
          </div>
        </main>
      </div>
      {/* 강의실 상세정보 모달 */}
      {isDetailModalOpen && selectedRoom && (
        <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              {/* 모달 헤더 */}
              <div className="flex justify-between items-center mb-6">
                <div className="flex items-center space-x-3">
                  <MapPin className="w-6 h-6" style={{ color: "#3498db" }} />
                  <h2 className="text-2xl font-bold" style={{ color: "#2C3E50" }}>
                    {isEditMode ? "강의실 정보 수정" : `강의실 상세정보 - ${selectedRoom.classroom?.classCode || ''}`}
                  </h2>
                </div>
                <button 
                  onClick={handleCloseModal} 
                  className="text-gray-400 hover:text-gray-600 hover:bg-gray-100 hover:scale-110 transition-all duration-200 text-2xl w-8 h-8 rounded-full flex items-center justify-center"
                >
                  ×
                </button>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* 기본 정보 */}
                <Card>
                  <CardHeader>
                    <CardTitle style={{ color: "#2C3E50" }}>기본 정보</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium mb-1" style={{ color: "#2C3E50" }}>
                          강의실명
                        </label>
                        {isEditMode ? (
                          <Input
                            value={selectedRoom.classroom?.classCode || ''}
                            onChange={(e) => handleRoomFieldChange("classCode", e.target.value)}
                          />
                        ) : (
                          <p className="text-sm bg-gray-100 p-2 rounded">{selectedRoom.classroom?.classCode || '-'}</p>
                        )}
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-1" style={{ color: "#2C3E50" }}>
                          강의호실
                        </label>
                        {isEditMode ? (
                          <Input
                            value={selectedRoom.classroom?.classNumber || ''}
                            onChange={(e) => handleRoomFieldChange("classNumber", e.target.value)}
                          />
                        ) : (
                          <p className="text-sm font-mono bg-gray-100 p-2 rounded">{selectedRoom.classroom?.classNumber || '-'}</p>
                        )}
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium mb-1" style={{ color: "#2C3E50" }}>
                          수용 인원
                        </label>
                        {isEditMode ? (
                          <Input
                            type="number"
                            value={selectedRoom.classroom?.classCapacity || ''}
                            onChange={(e) => handleRoomFieldChange("classCapacity", parseInt(e.target.value))}
                          />
                        ) : (
                          <p className="text-sm bg-gray-100 p-2 rounded">{selectedRoom.classroom?.classCapacity || '-'}명</p>
                        )}
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-1" style={{ color: "#2C3E50" }}>
                          강의실 면적
                        </label>
                        {isEditMode ? (
                          <Input
                            type="number"
                            value={selectedRoom.classroom?.classArea || ''}
                            onChange={(e) => handleRoomFieldChange("classArea", parseInt(e.target.value))}
                            placeholder="면적을 입력하세요"
                          />
                        ) : (
                          <p className="text-sm bg-gray-100 p-2 rounded">{selectedRoom.classroom?.classArea || '-'}㎡</p>
                        )}
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-1" style={{ color: "#2C3E50" }}>
                          1인당면적
                        </label>
                        <p className="text-sm bg-gray-100 p-2 rounded">{selectedRoom.classroom?.classPersonArea || '-'}㎡/인</p>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium mb-1" style={{ color: "#2C3E50" }}>
                          강의실 상태
                        </label>
                        {isEditMode ? (
                          <select
                            value={selectedRoom.classroom?.classActive === 0 ? "사용 가능" : "점검 중"}
                            onChange={(e) => handleRoomFieldChange("classActive", e.target.value === "사용 가능" ? 0 : 1)}
                            className="w-full px-3 py-2 border rounded-md"
                          >
                            <option value="사용 가능">사용 가능</option>
                            <option value="점검 중">점검 중</option>
                          </select>
                        ) : (
                          <Badge
                            className="text-sm text-white"
                            style={{ backgroundColor: selectedRoom.classroom?.classActive === 0 ? "#1ABC9C" : "#95A5A6" }}
                          >
                            {selectedRoom.classroom?.classActive === 0 ? "사용 가능" : "점검 중"}
                          </Badge>
                        )}
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-1" style={{ color: "#2C3E50" }}>
                          강의실 사용 여부
                        </label>
                        {isEditMode ? (
                          <select
                            value={selectedRoom.classroom?.classRent === 0 ? "사용 가능" : "사용 중"}
                            onChange={(e) => handleRoomFieldChange("classRent", e.target.value === "사용 가능" ? 0 : 1)}
                            className="w-full px-3 py-2 border rounded-md"
                          >
                            <option value="사용 가능">사용 가능</option>
                            <option value="사용 중">사용 중</option>
                          </select>
                        ) : (
                          <Badge
                            className="text-sm text-white"
                            style={{ backgroundColor: selectedRoom.classroom?.classRent === 0 ? "#1ABC9C" : "#e74c3c" }}
                          >
                            {selectedRoom.classroom?.classRent === 0 ? "사용 가능" : "사용 중"}
                          </Badge>
                        )}
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium mb-1" style={{ color: "#2C3E50" }}>
                        설명
                      </label>
                      {isEditMode ? (
                        <textarea
                          value={selectedRoom.classroom?.classMemo || ""}
                          onChange={(e) => handleRoomFieldChange("classMemo", e.target.value)}
                          className="w-full px-3 py-2 border rounded-md"
                          rows="3"
                        />
                      ) : (
                        <p className="text-sm bg-gray-100 p-2 rounded">
                          {selectedRoom.classroom?.classMemo || "설명 없음"}
                        </p>
                      )}
                    </div>
                  </CardContent>
                </Card>

                {/* 장비 정보 */}
                <Card>
                  <CardHeader>
                    <CardTitle style={{ color: "#2C3E50" }}>장비 정보</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {isEditMode && (
                      <div className="flex gap-2 mb-4">
                        <Button
                          size="sm"
                          onClick={handleDownloadTemplate}
                          className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                        >
                          템플릿 다운로드
                        </Button>
                        <Button
                          size="sm"
                          onClick={() => setIsExcelUploadMode(!isExcelUploadMode)}
                          className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                        >
                          Excel 업로드
                        </Button>
                        <Button
                          size="sm"
                          onClick={() => setIsEquipmentEditMode(true)}
                          className="text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
                        >
                          <Plus className="w-4 h-4 mr-1" />
                          장비 추가
                        </Button>
                      </div>
                    )}
                    {/* Excel 업로드 폼 */}
                    {isExcelUploadMode && (
                      <div className="mb-4 p-4 border rounded-lg bg-purple-50">
                        <h4 className="font-medium mb-3" style={{ color: "#2C3E50" }}>Excel 파일 업로드</h4>
                        <div className="space-y-3">
                          <div>
                            <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                              Excel 파일 선택
                            </label>
                            <input
                              type="file"
                              accept=".xlsx,.xls"
                              onChange={handleExcelFileSelect}
                              className="w-full px-2 py-1 text-sm border rounded"
                            />
                            {selectedExcelFile && (
                              <p className="text-xs text-green-600 mt-1">
                                선택된 파일: {selectedExcelFile.name}
                              </p>
                            )}
                          </div>
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              onClick={handleExcelUpload}
                              disabled={!selectedExcelFile}
                              style={{ backgroundColor: "#9b59b6" }}
                              className="hover:bg-purple-600 hover:scale-105 transition-all duration-200 disabled:opacity-50"
                            >
                              업로드
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => {
                                setIsExcelUploadMode(false)
                                setSelectedExcelFile(null)
                              }}
                              className="hover:bg-gray-100 hover:scale-105 transition-all duration-200"
                            >
                              취소
                            </Button>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* 새 장비 추가 폼 */}
                    {isEquipmentEditMode && !editingEquipment && (
                      <div className="mb-4 p-4 border rounded-lg bg-blue-50">
                        <h4 className="font-medium mb-3" style={{ color: "#2C3E50" }}>새 장비 추가</h4>
                        <div className="grid grid-cols-2 gap-3">
                          <div>
                            <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                              장비명 *
                            </label>
                            <Input
                              size="sm"
                              value={newEquipment.equipName}
                              onChange={(e) => setNewEquipment(prev => ({ ...prev, equipName: e.target.value }))}
                              placeholder="장비명 입력"
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                              장비 모델 *
                            </label>
                            <Input
                              size="sm"
                              value={newEquipment.equipModel}
                              onChange={(e) => setNewEquipment(prev => ({ ...prev, equipModel: e.target.value }))}
                              placeholder="모델명 입력"
                            />
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-3 mt-3">
                          <div>
                            <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                              장비 수량
                            </label>
                                                         <Input
                               size="sm"
                               type="number"
                               value={newEquipment.equipNumber}
                               onChange={(e) => setNewEquipment(prev => ({ ...prev, equipNumber: parseInt(e.target.value) || 1 }))}
                               placeholder="1"
                               min="1"
                             />
                          </div>
                          <div>
                            <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                              대여 여부
                            </label>
                            <select
                              value={newEquipment.equipRent === 0 ? "구매" : "대여"}
                              onChange={(e) => setNewEquipment(prev => ({ 
                                ...prev, 
                                equipRent: e.target.value === "구매" ? 0 : 1 
                              }))}
                              className="w-full px-2 py-1 text-sm border rounded"
                            >
                              <option value="구매">구매</option>
                              <option value="대여">대여</option>
                            </select>
                          </div>
                        </div>
                        <div className="mt-3">
                          <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                            설명 (구매처/구매링크)
                          </label>
                          <textarea
                            value={newEquipment.equipDescription}
                            onChange={(e) => setNewEquipment(prev => ({ ...prev, equipDescription: e.target.value }))}
                            className="w-full px-2 py-1 text-sm border rounded"
                            rows="2"
                            placeholder="구매처나 구매링크를 입력하세요"
                          />
                        </div>
                        <div className="flex gap-2 mt-3">
                          <Button
                            size="sm"
                            onClick={handleAddEquipment}
                            style={{ backgroundColor: "#1ABC9C" }}
                            className="hover:bg-green-600 hover:scale-105 transition-all duration-200"
                          >
                            <Plus className="w-3 h-3 mr-1" />
                            추가
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setIsEquipmentEditMode(false)}
                            className="hover:bg-gray-100 hover:scale-105 transition-all duration-200"
                          >
                            취소
                          </Button>
                        </div>
                      </div>
                    )}

                    {/* 장비 목록 */}
                    {(() => {
                      console.log('장비 정보 렌더링 - selectedRoom:', selectedRoom)
                      console.log('장비 데이터:', selectedRoom.equipment)
                      console.log('장비 개수:', selectedRoom.equipment?.length)
                      
                      // 장비 데이터가 없거나 빈 배열인 경우 처리
                      const equipmentList = selectedRoom.equipment || []
                      const hasEquipment = Array.isArray(equipmentList) && equipmentList.length > 0
                      
                      if (hasEquipment) {
                        return (
                          <div className="space-y-2">
                            {equipmentList.map((equipment, index) => {
                              console.log(`장비 ${index}:`, equipment)
                              const equipmentId = equipment.classEquipId || equipment.id
                              const equipmentName = equipment.equipName || equipment.equipmentName || equipment.name || '이름 없음'
                              const equipmentModel = equipment.equipModel || equipment.model || '모델 없음'
                              const equipmentQuantity = equipment.equipNumber || equipment.number || 1
                              const equipmentRent = equipment.equipRent || equipment.rent || 0
                              const equipmentDescription = equipment.equipDescription || equipment.description || ''
                              const equipActive = equipment.equipActive !== undefined ? equipment.equipActive : (equipment.active !== undefined ? equipment.active : 1)
                              console.log('장비 상태 디버깅:', {
                                equipmentId: equipmentId,
                                equipActive: equipActive,
                                equipment: equipment
                              })
                              
                              // 장비 수정 모드인지 확인
                              const isEditingThisEquipment = editingEquipment && editingEquipment.classEquipId === equipmentId
                              
                              return (
                                <div key={index} className="p-3 bg-gray-50 rounded-lg border">
                                  {isEditingThisEquipment ? (
                                    // 장비 수정 폼
                                    <div className="space-y-2">
                                      <div className="grid grid-cols-2 gap-2">
                                        <div>
                                          <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                                            장비명 *
                                          </label>
                                          <Input
                                            size="sm"
                                            value={editingEquipment.equipName}
                                            onChange={(e) => setEditingEquipment(prev => ({ ...prev, equipName: e.target.value }))}
                                          />
                                        </div>
                                        <div>
                                          <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                                            장비 모델 *
                                          </label>
                                          <Input
                                            size="sm"
                                            value={editingEquipment.equipModel}
                                            onChange={(e) => setEditingEquipment(prev => ({ ...prev, equipModel: e.target.value }))}
                                          />
                                        </div>
                                      </div>
                                      <div className="grid grid-cols-2 gap-2">
                                        <div>
                                          <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                                            장비 번호
                                          </label>
                                                                                     <Input
                                             size="sm"
                                             type="number"
                                             value={editingEquipment.equipNumber}
                                             onChange={(e) => setEditingEquipment(prev => ({ ...prev, equipNumber: parseInt(e.target.value) || 1 }))}
                                             min="1"
                                           />
                                        </div>
                                        <div>
                                          <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                                            대여 여부
                                          </label>
                                          <select
                                            value={editingEquipment.equipRent === 0 ? "구매" : "대여"}
                                            onChange={(e) => setEditingEquipment(prev => ({ 
                                              ...prev, 
                                              equipRent: e.target.value === "구매" ? 0 : 1 
                                            }))}
                                            className="w-full px-2 py-1 text-sm border rounded"
                                          >
                                            <option value="구매">구매</option>
                                            <option value="대여">대여</option>
                                          </select>
                                        </div>
                                      </div>
                                      <div>
                                        <label className="block text-xs font-medium mb-1" style={{ color: "#2C3E50" }}>
                                          설명 (구매처/구매링크)
                                        </label>
                                        <textarea
                                          value={editingEquipment.equipDescription}
                                          onChange={(e) => setEditingEquipment(prev => ({ ...prev, equipDescription: e.target.value }))}
                                          className="w-full px-2 py-1 text-sm border rounded"
                                          rows="2"
                                        />
                                      </div>
                                      <div className="flex gap-2">
                                        <Button
                                          size="sm"
                                          onClick={handleSaveEquipmentEdit}
                                          style={{ backgroundColor: "#1ABC9C" }}
                                          className="hover:bg-green-600 hover:scale-105 transition-all duration-200"
                                        >
                                          <Check className="w-3 h-3 mr-1" />
                                          저장
                                        </Button>
                                        <Button
                                          size="sm"
                                          variant="outline"
                                          onClick={handleCancelEquipmentEdit}
                                          className="hover:bg-gray-100 hover:scale-105 transition-all duration-200"
                                        >
                                          <X className="w-3 h-3 mr-1" />
                                          취소
                                        </Button>
                                      </div>
                                    </div>
                                  ) : (
                                    // 장비 정보 표시
                                    <div className="flex items-center justify-between">
                                                                             <div className="flex-1">
                                         <div className="flex items-center space-x-2">
                                           <span className="text-sm font-medium" style={{ color: "#2C3E50" }}>
                                             {equipmentName}
                                           </span>
                                           <Badge
                                             className="text-xs"
                                             style={{
                                               backgroundColor: "#3498db20",
                                               color: "#3498db",
                                             }}
                                           >
                                             {equipmentModel}
                                           </Badge>
                                         </div>
                                         <div className="flex items-center space-x-2 mt-1">
                                           <Badge
                                             className="text-xs"
                                             style={{
                                               backgroundColor: (equipActive === 0) ? "#1ABC9C20" : "#e74c3c20",
                                               color: (equipActive === 0) ? "#1ABC9C" : "#e74c3c",
                                             }}
                                           >
                                             {equipActive === 0 ? "활성" : "비활성"}
                                           </Badge>
                                           <span className="text-xs text-gray-500">
                                             수량: {equipmentQuantity}개
                                           </span>
                                           <Badge
                                             className="text-xs"
                                             style={{
                                               backgroundColor: equipmentRent === 0 ? "#1ABC9C20" : "#f39c1220",
                                               color: equipmentRent === 0 ? "#1ABC9C" : "#f39c12",
                                             }}
                                           >
                                             {equipmentRent === 0 ? "구매" : "대여"}
                                           </Badge>
                                         </div>
                                         {equipmentDescription && (
                                           <div className="mt-1">
                                             <span className="text-xs text-gray-500">
                                               {equipmentDescription}
                                             </span>
                                           </div>
                                         )}
                                       </div>
                                      
                                      {isEditMode && (
                                        <div className="flex items-center space-x-1">
                                          <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => handleEditEquipment(equipment)}
                                            className="p-1 hover:bg-blue-50 hover:scale-110 transition-all duration-200"
                                            title="수정"
                                          >
                                            <Edit className="w-3 h-3" style={{ color: "#1abc9c" }} />
                                          </Button>
                                          <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => handleEquipmentStatusChange(equipmentId, equipActive === 0 ? 1 : 0)}
                                            className="p-1 hover:bg-gray-100 hover:scale-110 transition-all duration-200"
                                            title={equipActive === 0 ? "비활성화" : "활성화"}
                                          >
                                            <Settings className="w-3 h-3" style={{ color: equipActive === 0 ? "#95A5A6" : "#b0c4de" }} />
                                          </Button>
                                          <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => handleDeleteEquipment(equipmentId)}
                                            className="p-1 hover:bg-red-50 hover:scale-110 transition-all duration-200"
                                            title="삭제"
                                          >
                                            <Trash2 className="w-3 h-3" style={{ color: "#e74c3c" }} />
                                          </Button>
                                        </div>
                                      )}
                                    </div>
                                  )}
                                </div>
                              )
                            })}
                          </div>
                        )
                      } else {
                        return (
                          <div className="text-center py-4">
                            <p className="text-sm text-gray-500 mb-2">등록된 장비가 없습니다.</p>
                          </div>
                        )
                      }
                    })()}
                  </CardContent>
                </Card>
              </div>
              {/* 모달 하단 버튼 */}
              <div className="flex justify-end space-x-3 mt-6 pt-6 border-t">
                {isEditMode ? (
                  <>
                    <Button 
                      variant="outline" 
                      onClick={() => {
                        setIsEditMode(false)
                        setIsEquipmentEditMode(false)
                        setIsExcelUploadMode(false)
                        setSelectedExcelFile(null)
                        setEditingEquipment(null)
                        setNewEquipment({
                          equipName: '',
                          equipModel: '',
                          equipNumber: 1,
                          equipRent: 0,
                          equipDescription: '',
                          equipActive: 0
                        })
                      }}
                      className="hover:bg-gray-100 "
                    >
                      취소
                    </Button>
                    <Button 
                      onClick={handleSaveRoom} 
                      className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                    >
                      저장
                    </Button>
                  </>
                ) : (
                  <>
                    <Button 
                      variant="outline" 
                      onClick={handleCloseModal}
                      className="hover:bg-gray-100"
                    >
                      닫기
                    </Button>
                    <Button 
                      onClick={handleEditRoom} 
                      className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                    >
                      수정
                    </Button>
                    <Button 
                      onClick={handleDeleteRoom} 
                      className="bg-[#e74c3c] hover:bg-red-700"
                    >
                      삭제
                    </Button>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </PageLayout>
  )
}
