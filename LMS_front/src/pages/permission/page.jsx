import { useState, useEffect } from "react"
import Header from "@/components/layout/header"
import { Users, Settings, Plus, Edit, Trash2, Check, X, ChevronDown, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import Sidebar from "@/components/layout/sidebar"
import axios from "axios"

const sidebarMenuItems = [
]


// 학원장 화면 - 직원 목록 카드
const DirectorView = ({ staffList, onEdit }) => (
  <div className="space-y-6 max-w-7xl mx-auto">
    <div className="flex justify-between items-center">
      <div>
        <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
          직원 관리
        </h1>
        <p className="text-gray-600">직원들의 권한을 관리하고 역할을 설정할 수 있습니다.</p>
      </div>
    </div>

    

    {/* 강사 섹션 */}
    {staffList.filter(staff => staff.role === "ROLE_INSTRUCTOR").length > 0 && (
      <div className="mb-8">
        <div className="flex items-center mb-4">
          <h2 className="text-2xl font-semibold text-[#2c3e50]">강사</h2>
          <span className="ml-2 px-2 py-1 bg-[#e8f8f5] text-[#1abc9c] text-sm rounded-full">
            {staffList.filter(staff => staff.role === "ROLE_INSTRUCTOR").length}명
          </span>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {staffList.filter(staff => staff.role === "ROLE_INSTRUCTOR").map((staff) => (
            <Card key={staff.id} className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-center mb-4">
                  <div className="ml-4 flex-1">
                    <h3 className="text-lg font-semibold text-gray-900">{staff.name}</h3>
                    <p className="text-sm text-gray-600">{staff.department}</p>
                  </div>
                </div>

                <div className="space-y-2 mb-4">
                  <div className="flex items-center text-sm text-gray-600">
                    <span className="font-medium">이메일:</span>
                    <span className="ml-2">{staff.email}</span>
                  </div>
                  <div className="flex items-center text-sm text-gray-600">
                    <span className="font-medium">연락처:</span>
                    <span className="ml-2">{staff.phone}</span>
                  </div>
                  <div className="flex items-center text-sm text-gray-600">
                    <span className="font-medium">입사일:</span>
                    <span className="ml-2">{staff.joinDate}</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    )}

    {/* 직원 섹션 */}
    {staffList.filter(staff => staff.role === "ROLE_STAFF").length > 0 && (
      <div className="mb-8">
        <div className="flex items-center mb-4">
          <h2 className="text-xl font-semibold text-[#2c3e50]">직원</h2>
          <span className="ml-2 px-2 py-1 bg-[#ebf3fd] text-[#3498db] text-sm rounded-full">
            {staffList.filter(staff => staff.role === "ROLE_STAFF").length}명
          </span>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {staffList.filter(staff => staff.role === "ROLE_STAFF").map((staff) => (
        <Card key={staff.id} className="hover:shadow-md transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center mb-4">
              <div className="ml-4 flex-1">
                <h3 className="text-lg font-semibold text-gray-900">{staff.name}</h3>
                <p className="text-sm text-gray-600">{staff.department}</p>
              </div>
            </div>

            <div className="space-y-2 mb-4">
              <div className="flex items-center text-sm text-gray-600">
                <span className="font-medium">이메일:</span>
                <span className="ml-2">{staff.email}</span>
              </div>
              <div className="flex items-center text-sm text-gray-600">
                <span className="font-medium">연락처:</span>
                <span className="ml-2">{staff.phone}</span>
              </div>
              <div className="flex items-center text-sm text-gray-600">
                <span className="font-medium">입사일:</span>
                <span className="ml-2">{staff.joinDate}</span>
              </div>
            </div>

            <div className="flex justify-end space-x-2">
            { staff.role !== "ROLE_INSTRUCTOR" && (
              <Button size="sm" variant="ghost" onClick={() => onEdit(staff)} 
              className="flex-1 hover:bg-gray-200">
                <Edit className="w-4 h-4 mr-1" />
                권한수정
              </Button>
            )}
            </div>
          </CardContent>
        </Card>
      ))}
        </div>
      </div>
    )}
  </div>
)

// 일반직원 화면 - 자신의 권한 목록
const StaffView = ({ currentUserPermissions, permissionLoading }) => {
  // 로딩 중인 경우
  if (permissionLoading) {
    return (
      <div className="space-y-6 max-w-4xl mx-auto">
        <div>
          <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
            내 권한 현황
          </h1>
          <p className="text-gray-600">현재 계정에 부여된 권한을 확인할 수 있습니다.</p>
        </div>
        <div className="bg-white p-8 rounded-lg shadow-sm border text-center">
          <p className="text-gray-500">권한 정보를 불러오는 중입니다...</p>
        </div>
      </div>
    )
  }

  // 배열이 비어있거나 잘못된 형태인 경우 처리
  if (!Array.isArray(currentUserPermissions) || currentUserPermissions.length === 0) {
    return (
      <div className="space-y-6 max-w-4xl mx-auto">
        <div>
          <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
            내 권한 현황
          </h1>
          <p className="text-gray-600">현재 계정에 부여된 권한을 확인할 수 있습니다.</p>
        </div>
        <div className="bg-white p-8 rounded-lg shadow-sm border text-center">
          <p className="text-gray-500">부여된 권한이 없습니다.</p>
        </div>
      </div>
    )
  }

  // 권한 통계 계산
  const grantedCount = currentUserPermissions.filter(p => p.isGranted).length
  const totalCount = currentUserPermissions.length
  const deniedCount = totalCount - grantedCount

  // 타입별로 권한 그룹화
  const groupedPermissions = currentUserPermissions.reduce((acc, permission) => {
    const category = permission.type === "1" ? "메인 메뉴 권한" : "하위 메뉴 권한"
    if (!acc[category]) {
      acc[category] = []
    }
    acc[category].push(permission)
    return acc
  }, {})

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold mb-2" style={{ color: "#2C3E50" }}>
          내 권한 현황
        </h1>
        <p className="text-gray-600">현재 계정에 부여된 권한을 확인할 수 있습니다.</p>
      </div>

      {/* 권한 요약 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div className="ml-4">
                <p className="text-m font-medium text-gray-600">전체 권한</p>
                <p className="text-3xl font-bold" style={{ color: "#3498db" }}>
                  {totalCount}개
                </p>
              </div>
              <div className="p-3 rounded-full" style={{ backgroundColor: "rgba(52, 152, 219, 0.1)" }}>
                <Users className="w-10 h-10" style={{ color: "#3498db" }} />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div className="ml-4">
                <p className="text-m font-medium text-gray-600">허용된 권한</p>
                <p className="text-3xl font-bold" style={{ color: "#2ecc71" }}>
                  {grantedCount}개
                </p>
              </div>
              <div className="p-3 rounded-full" style={{ backgroundColor: "rgba(46, 204, 113, 0.1)" }}>
                <Check className="w-10 h-10" style={{ color: "#2ecc71" }} />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div className="ml-4">
                <p className="text-m font-medium text-gray-600">제한된 권한</p>
                <p className="text-3xl font-bold" style={{ color: "#e74c3c" }}>
                  {deniedCount}개
                </p>
              </div>
              <div className="p-3 rounded-full" style={{ backgroundColor: "rgba(231, 76, 60, 0.1)" }}>
                <X className="w-10 h-10" style={{ color: "#e74c3c" }} />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 권한 상세 목록 - 메인 권한별 섹션으로 그룹화 */}
      <div className="space-y-6">
        {(() => {
          // 메인 권한들(type="1")을 섹션 헤더로 사용
          const mainPermissions = currentUserPermissions.filter(p => p.type === "1");
          const subPermissions = currentUserPermissions.filter(p => p.type === "2");
          
          
          return mainPermissions.map((mainPerm) => {
            // 해당 메인 권한의 하위 권한들 찾기
            const childPermissions = subPermissions.filter(subPerm => {
              const mainPmId = Number(mainPerm.pmId);
              
              // selfFK 또는 selfFk 필드 확인 (백엔드 응답에 따라)
              const selfFKField = subPerm.selfFK || subPerm.selfFk;
              
              // selfFK가 null/undefined이거나 0인 경우 제외 (숫자 타입이므로)
              if (selfFKField === null || selfFKField === undefined || selfFKField === 0) {
                return false;
              }
              
              const selfFKValue = Number(selfFKField);
              
              return selfFKValue === mainPmId;
            });
            
            return (
              <div key={mainPerm.pmId} className="bg-white rounded-lg shadow-sm border">
                <div className="p-6">
                  {/* 메인 권한 섹션 헤더 */}
                  <div className="mb-6 pb-4 border-b">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center mb-2">
                          <div
                            className={`w-6 h-6 rounded-full flex items-center justify-center mr-3 ${
                              mainPerm.isGranted ? "bg-green-100" : "bg-red-100"
                            }`}
                          >
                            {mainPerm.isGranted ? (
                              <Check className="w-4 h-4 text-green-600" />
                            ) : (
                              <X className="w-4 h-4 text-red-600" />
                            )}
                          </div>
                          <h3 className="text-lg font-semibold text-gray-900">{mainPerm.permissionName}</h3>
                        </div>
                        <p className="text-sm text-gray-600 ml-9">{mainPerm.permissionText}</p>
                      </div>
                      <span
                        className={`px-3 py-1 rounded-full text-sm font-medium ${
                          mainPerm.isGranted ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"
                        }`}
                      >
                        {mainPerm.isGranted ? "허용" : "제한"}
                      </span>
                    </div>
                  </div>
                  
                  {/* 하위 권한들 */}
                  {childPermissions.length > 0 && (
                    <div>
                      <h4 className="text-md font-medium text-gray-700 mb-3">하위 권한</h4>
                      <div className="space-y-3">
                        {childPermissions.map((childPerm) => (
                          <div key={childPerm.pmId} className="flex items-start justify-between p-3 rounded-lg bg-gray-50 border">
                            <div className="flex-1">
                              <div className="flex items-center mb-1">
                                <div
                                  className={`w-4 h-4 rounded-full flex items-center justify-center mr-3 ${
                                    childPerm.isGranted ? "bg-green-100" : "bg-red-100"
                                  }`}
                                >
                                  {childPerm.isGranted ? (
                                    <Check className="w-2 h-2 text-green-600" />
                                  ) : (
                                    <X className="w-2 h-2 text-red-600" />
                                  )}
                                </div>
                                <h5 className="font-medium text-gray-800">{childPerm.permissionName}</h5>
                              </div>
                              <p className="text-sm text-gray-600 ml-7">{childPerm.permissionText}</p>
                            </div>
                            <span
                              className={`px-2 py-1 rounded-full text-xs font-medium ${
                                childPerm.isGranted ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"
                              }`}
                            >
                              {childPerm.isGranted ? "허용" : "제한"}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                  
                  {/* 하위 권한이 없는 경우 */}
                  {childPermissions.length === 0 && (
                    <div className="text-center py-4">
                      <p className="text-sm text-gray-500">하위 권한이 없습니다.</p>
                    </div>
                  )}
                </div>
              </div>
            );
          });
        })()}
      </div>
    </div>
  )
}
export default function PermissionPage() {
  const [selectedStaff, setSelectedStaff] = useState(null)
  const [isEditModalOpen, setIsEditModalOpen] = useState(false)
  const [staffList, setStaffList] = useState([])
  const [loading, setLoading] = useState(true)
  const [permissionLoading, setPermissionLoading] = useState(false)
  const [currentUserPermissions, setCurrentUserPermissions] = useState([])
  const [mainPermissions, setMainPermissions] = useState([]) // type=1 목록
  const [subPermissions,  setSubPermissions]  = useState([])   // type=2 목록

  const [expandedCategories, setExpandedCategories] = useState({});

  const [userRole] = useState(() => {
    try {
      const u = localStorage.getItem("currentUser")
      const parsed = u ? JSON.parse(u) : null
      const role = parsed?.role || "staff"
      return role
    } catch(err){
      return "staff"
    }
  })

  // 현재 로그인한 사용자의 educationId 가져오기
  const [currentUserEducationId] = useState(() => {
    try {
      const u = localStorage.getItem("currentUser")
      const parsed = u ? JSON.parse(u) : null
      const educationId = parsed?.educationId
      
      // educationId가 없다면 JWT 토큰에서도 확인해보기
      if (!educationId) {
        try {
          const token = document.cookie
            .split('; ')
            .find(row => row.startsWith('refresh='))
            ?.split('=')[1];
          
          if (token) {
            // JWT 디코딩 (단순 Base64 디코딩, 서명 검증은 하지 않음)
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.educationId || null;
          }
        } catch (jwtErr) {
        }
      }
      
      return educationId
    } catch(err){
      return null
    }
  })

  useEffect(() => {
    async function fetchStaff() {
      try {
        // 현재 사용자의 educationId가 없으면 요청하지 않음
        if (!currentUserEducationId) {
          setLoading(false)
          return
        }
  
        // 백엔드에서 SecurityContext를 통해 educationId를 확인하므로 쿼리 파라미터 제거
        const res = await axios.get("/api/staff/members", {
          withCredentials: true
        })
  
        const members = Array.isArray(res.data) ? res.data : [res.data]
  
        // educationId가 일치하는 직원들만 필터링
        const filteredMembers = members.filter(
          (m) => m.educationId === currentUserEducationId
        )
  
        const mapped = filteredMembers.map((m) => ({
          id: m.memberId,
          name: m.memberName,
          email: m.memberEmail,
          phone: m.memberPhone,
          role: m.memberRole,
          joinDate: m.createdAt?.slice(0, 10),
          status: m.memberStatus,
          department: m.memberDepartment,
          avatar: m.memberAvatar,
          permissions: m.memberPermissions,
          educationId: m.educationId,
        }))
  
        setStaffList(mapped)
      } catch (err) {
        // 디버깅 실패 시 전체 목록 조회 시도
        try {
          const res = await axios.get("/api/staff/members/all", {
            withCredentials: true,
          })
  
          if (Array.isArray(res.data)) {
            const filteredMembers = res.data.filter(
              (m) => m.educationId === currentUserEducationId
            )
  
            const mapped = filteredMembers.map((m) => ({
              id: m.memberId,
              name: m.memberName,
              email: m.memberEmail,
              phone: m.memberPhone,
              role: m.memberRole,
              joinDate: m.createdAt?.slice(0, 10),
              status: m.memberStatus,
              department: m.memberDepartment,
              avatar: m.memberAvatar,
              permissions: m.memberPermissions,
              educationId: m.educationId,
            }))
  
            setStaffList(mapped)
          }
        } catch (debugErr) {
          alert("서버와의 연결에 문제가 있습니다. 백엔드 서버가 실행 중인지 확인해주세요.")
        }
      } finally {
        setLoading(false)
      }
    }
    fetchStaff()
  }, [currentUserEducationId])
  

  // StaffView용 권한 조회 (로그인한 사용자가 staff인 경우)
  useEffect(() => {
    if (userRole !== "staff") return;
    setPermissionLoading(true);
    
    (async () => {
      try {
        const res = await axios.get("/api/staff/my-permissions");
        
        
        if (res.data && res.data.success) {
          // 백엔드 응답 구조에 맞춰 권한 데이터 추출
          const { mainPermissions, subPermissions } = res.data.data;
          const allPermissions = [...(mainPermissions || []), ...(subPermissions || [])];
          
          allPermissions.forEach((perm, index) => {
          });
          
          setCurrentUserPermissions(allPermissions);
        } else {
          setCurrentUserPermissions([]);
        }
      } catch (err) {
        setCurrentUserPermissions([]);
      } finally {
        setPermissionLoading(false);
      }
    })();
  }, [userRole]);

  const fetchMainPermissions = async () => {
    try {
      const res = await axios.get("/api/staff/permissions/main")
      setMainPermissions(res.data)
    } catch (err) {
    }
  }

  const fetchSubPermissions = async () => {
    try {
      const res = await axios.get("/api/staff/permissions/sub")
      setSubPermissions(res.data)   // [{ pmId, permissionName, … }, …]
    } catch (err) {
    }
  }

  const toggleCategory = (pmId) => {
    setExpandedCategories(prev => ({
      ...prev,
      [pmId]: !prev[pmId],
    }));
  };

  // 폼 입력값 변경 핸들러
  const handleFormChange = (field, value) => {
    setCreatePermissionForm(prev => ({
      ...prev,
      [field]: value,
      // 타입이 메인메뉴로 변경되면 상위 권한 ID 초기화
      ...(field === "type" && value === "1" ? { parentPmId: "" } : {})
    }))
  }

  // 로딩 상태 처리 - 역할에 따라 다르게 처리
  if (userRole === "director" && loading) {
    return <p className="p-8 text-center">직원 목록을 불러오는 중...</p>
  }

  // “권한 수정” 버튼 클릭 핸들러
  const handleEditPermissions = async (staff) => {
    try {
      // 해당 직원의 권한 정보를 가져온다 (이미 모든 권한 포함)
      const res = await axios.get(`/api/staff/members/${staff.id}/permissions`);
      
      if (res.data.success) {
        const { permissions } = res.data.data;
        
        // 부여된 권한 ID 목록
        const grantedPmIds = permissions
          .filter(p => p.isGranted)
          .map(p => p.pmId);
        
        // 전체 권한 목록을 올바른 형태로 변환
        const allPermissions = permissions.map(p => ({
          id: p.pmId,
          name: p.permissionName,
          description: p.permissionText,
          type: p.type,
          selfFK: p.selfFk  // 백엔드에서는 selfFk로 반환됨
        }));
        

  
        // selectedStaff 에 두 가지를 함께 저장
        setSelectedStaff({
          ...staff,
          allPermissions,   
          grantedPmIds      
        });
        setIsEditModalOpen(true);
      } else {
        alert(res.data.message || "권한 정보를 가져올 수 없습니다.");
      }
    } catch (err) {
      alert("권한 정보를 가져오는 중 오류가 발생했습니다.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Header currentPage="permission" userRole={userRole} />
      <div className="flex">
        <Sidebar title="권한 관리" menuItems={sidebarMenuItems} currentPath="" />
        <main className="flex-1 p-8">
        {userRole === "director"
          ? <DirectorView
              staffList={staffList}
              onEdit={handleEditPermissions} 
            />
          : <StaffView 
              currentUserPermissions={currentUserPermissions} 
              permissionLoading={permissionLoading}
            />
          }
        </main> 
      </div>

      {/* 권한 수정 모달 */}
      {isEditModalOpen && selectedStaff && (
        <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            {/* 헤더 */}
            <div className="flex justify-between items-center mb-6">
              <div>
                <h3 className="text-xl font-semibold text-gray-900">{selectedStaff.name} 권한 관리</h3>
                <p className="text-sm text-gray-600 mt-1">
                  {selectedStaff.department} · {selectedStaff.email}
                </p>
              </div>
              <Button variant="ghost" onClick={() => setIsEditModalOpen(false)} className="p-2">
                <X className="w-5 h-5" />
              </Button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* 좌측: 직원 정보 */}
              <div className="lg:col-span-1">
                <div className="bg-gray-50 rounded-lg p-4 mb-4">
                  <div className="flex items-center mb-4">
                    <img
                      src={selectedStaff.avatar || "/placeholder.svg"}
                      alt={selectedStaff.name}
                      className="w-16 h-16 rounded-full object-cover"
                    />
                    <div className="ml-4">
                      <h4 className="font-semibold text-gray-900">{selectedStaff.name}</h4>
                      <p className="text-sm text-gray-600">{selectedStaff.department}</p>
                      <span
                        className={`inline-block px-2 py-1 rounded-full text-xs font-medium mt-1 ${
                          selectedStaff.status === "활성" ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"
                        }`}
                      >
                        {selectedStaff.status}
                      </span>
                    </div>
                  </div>

                  <div className="space-y-2 text-sm">
                    <div>
                      <span className="font-medium text-gray-700">이메일:</span>
                      <span className="ml-2 text-gray-600">{selectedStaff.email}</span>
                    </div>
                    <div>
                      <span className="font-medium text-gray-700">연락처:</span>
                      <span className="ml-2 text-gray-600">{selectedStaff.phone}</span>
                    </div>
                    <div>
                      <span className="font-medium text-gray-700">입사일:</span>
                      <span className="ml-2 text-gray-600">{selectedStaff.joinDate}</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* 우측: 권한 설정 */}
              <div className="lg:col-span-2">
                <div className="mb-4">
                  <h4 className="text-lg font-semibold text-gray-900 mb-2">시스템 권한</h4>
                  <p className="text-sm text-gray-600">각 모듈별로 세부 권한을 설정할 수 있습니다.</p>
                  

                </div>

                <div className="space-y-4">
                  {(() => {
                    // 권한을 type에 따라 그룹화 (숫자와 문자열 모두 처리)
                    const mainPermissions = selectedStaff.allPermissions.filter(perm => 
                      perm.type === "1" || perm.type === 1
                    );
                    const subPermissions = selectedStaff.allPermissions.filter(perm => 
                      perm.type === "2" || perm.type === 2
                    );
                    
                    // type=1 권한들을 카테고리로 표시
                    return mainPermissions.map((mainPerm) => {
                      const categoryId = mainPerm.id;
                      const isExpanded = expandedCategories[categoryId] || false;
                      
                      // 해당 메인 권한의 하위 권한들 찾기 (selfFK로 연결된 것들)
                      const childPermissions = subPermissions.filter(subPerm => {
                        // pmId와 selfFK 모두 INT이므로 숫자로 직접 비교
                        const mainPermId = Number(mainPerm.id);
                        const selfFKValue = Number(subPerm.selfFK);

                        
                        // NULL이나 NaN이면 매칭하지 않음
                        if (isNaN(selfFKValue) || selfFKValue === 0) {
                          return false;
                        }
                        
                        return selfFKValue === mainPermId;
                      });
                      // 허용된 하위 권한 개수 계산
                      const allowedCount = childPermissions.filter(cp =>
                        selectedStaff.grantedPmIds.includes(cp.id)
                      ).length;
                      

                      
                      return (
                        <div key={mainPerm.id} className="border rounded-lg">
                          {/* 메인 카테고리 (type=1) */}
                          <div className={`flex items-center justify-between p-4 bg-gray-50 ${isExpanded ? 'rounded-t-lg' : 'rounded-lg'}`}>
                            <div className="flex items-center space-x-3">
                            <input
                              type="checkbox"
                              checked={selectedStaff.grantedPmIds.includes(mainPerm.id)}
                              onChange={(e) => {
                                const isChecked = e.target.checked;
                                let next;
                                
                                if (isChecked) {
                                  // 메인 권한 체크 시: 메인 권한 + 모든 하위 권한 추가
                                  const childIds = childPermissions.map(child => child.id);
                                  const newIds = [mainPerm.id, ...childIds];
                                  next = [...new Set([...selectedStaff.grantedPmIds, ...newIds])];
                                } else {
                                  // 메인 권한 해제 시: 메인 권한 + 모든 하위 권한 제거
                                  const childIds = childPermissions.map(child => child.id);
                                  const idsToRemove = [mainPerm.id, ...childIds];
                                  next = selectedStaff.grantedPmIds.filter(id => !idsToRemove.includes(id));
                                }
                                
                                setSelectedStaff({ ...selectedStaff, grantedPmIds: next });
                              }}
                              className="w-5 h-5 text-green-600 border-gray-300 rounded"
                            />
                              <div>
                                <h4 className="font-medium text-gray-900">
                                  {mainPerm.name}
                                  <span className="ml-2 text-xs text-gray-500">
                                    (허용된 하위: {allowedCount} / 전체: {childPermissions.length})
                                  </span>
                                </h4>
                                <p className="text-sm text-gray-600">{mainPerm.description}</p>
                              </div>
                            </div>
                              <button
                                onClick={() => {
                                  setExpandedCategories(prev => ({
                                    ...prev,
                                    [categoryId]: !prev[categoryId]
                                  }));
                                }}
                                className="p-1 hover:bg-gray-200 rounded"
                              >
                                {isExpanded ? (
                                  <ChevronDown className="w-4 h-4 text-[#2c3e50]" />
                                ) : (
                                  <ChevronRight className="w-4 h-4 text-[#2c3e50]" />
                                )}
                              </button>
                          </div>
                          
                          {/* 하위 권한들 (type=2, selfFK로 연결된 것들) */}
                          {isExpanded && (
                            <div className="border-t bg-white rounded-b-lg">
                              {childPermissions.length > 0 ? (
                                childPermissions.map((childPerm, index) => (
                                  <label
                                    key={childPerm.id}
                                    className={`flex items-center justify-between p-4 pl-12 border-b last:border-b-0 hover:bg-gray-100 ${
                                      index === childPermissions.length - 1 ? 'hover:rounded-b-lg' : ''
                                    }`}
                                  >
                                    <div>
                                      <h4 className="font-medium text-gray-700">{childPerm.name}</h4>
                                      <p className="text-sm text-gray-500">{childPerm.description}</p>
                                    </div>
                                    <input
                                      type="checkbox"
                                      checked={selectedStaff.grantedPmIds.includes(childPerm.id)}
                                      onChange={(e) => {
                                        const nextIds = e.target.checked
                                          ? [...selectedStaff.grantedPmIds, childPerm.id]
                                          : selectedStaff.grantedPmIds.filter((id) => id !== childPerm.id);
                                        setSelectedStaff({
                                          ...selectedStaff,
                                          grantedPmIds: nextIds,
                                        });
                                      }}
                                      className="w-4 h-4"
                                    />
                                  </label>
                                ))
                              ) : (
                                <div className="p-4 pl-12 text-sm text-gray-500">
                                  하위 권한이 없습니다.
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    });
                  })()}
                </div>
              </div>
            </div>

            <div className="flex justify-between items-center mt-8 pt-6 border-t">
              <div className="text-sm text-gray-500">마지막 수정: {new Date().toLocaleDateString()}</div>
              <div className="flex space-x-3">
                <Button variant="ghost" onClick={() => setIsEditModalOpen(false)}
                className="hover:bg-gray-100 text-gray-700 px-6">
                  취소
                </Button>
                <Button
                  onClick={async () => {
                    try {
                      const res = await axios.put(
                        `/api/staff/members/${selectedStaff.id}/permissions`,
                        { grantedPmIds: selectedStaff.grantedPmIds },
                        {headers: {'Content-Type': 'application/json'}}
                      )

                      if (res.data.success) {
                        alert("권한이 성공적으로 수정되었습니다!")
                        setIsEditModalOpen(false)
                        // 직원 목록 새로고침하여 변경사항 반영
                        window.location.reload()
                      } else {
                        alert(res.data.message || "권한 수정에 실패했습니다.")
                      }
                    } catch (err) {
                      alert(err.response?.data?.message || "권한 수정 중 오류가 발생했습니다.")
                    }
                  }}
                  className="bg-[#1abc9c] hover:bg-[rgb(10,150,120)] text-white px-6"
                >
                  권한 저장
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}