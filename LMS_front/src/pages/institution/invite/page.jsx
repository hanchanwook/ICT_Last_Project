import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import PageLayout from "@/components/ui/page-layout";
import Sidebar from "@/components/layout/sidebar";
import { getMenuItems } from "@/components/ui/menuConfig";
import { http } from "@/components/auth/http";

export default function InstitutionInvitePage() {
  const [inviteEmail, setInviteEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [registeredInstitutions, setRegisteredInstitutions] = useState([]);
  const [isLoadingInstitutions, setIsLoadingInstitutions] = useState(true);
  
  // 수정 모달 관련 state
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingInstitution, setEditingInstitution] = useState(null);
  const [editFormData, setEditFormData] = useState({
    educationId: "",
    educationName: "",
    memberEmail: "",
    memberName: "",
    businessNumber: "",
    description: "",
    memberAddress: "",
    educationAddress: "",
    createdAt: ""
  });

  const sidebarMenuItems = getMenuItems('institution');

  // 다음 우편번호 API 스크립트 로드
  useEffect(() => {
    const script = document.createElement('script');
    script.src = "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    script.async = true;
    document.head.appendChild(script);

    return () => {
      document.head.removeChild(script);
    };
  }, []);

  // 등록된 학원 목록 불러오기
  useEffect(() => {
    fetchRegisteredInstitutions();
  }, []);

  const fetchRegisteredInstitutions = async () => {
    try {
      const response = await http.get("/api/education/list", {
        withCredentials: true
      });
      
      setRegisteredInstitutions(response.data);
      setIsLoadingInstitutions(false);
      
    } catch (error) {
      setIsLoadingInstitutions(false);
    }
  };

  const handleSendInvite = async () => {
    if (!inviteEmail.trim()) {
      alert("이메일을 입력해주세요.");
      return;
    }

    // 이메일 형식 검증
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(inviteEmail)) {
      alert("올바른 이메일 형식을 입력해주세요.");
      return;
    }

    setIsLoading(true);
    try {
      const response = await http.post("/api/education/invite", {
        email: inviteEmail
      });

      alert(`${inviteEmail}로 초대장이 성공적으로 전송되었습니다.`);
      setInviteEmail("");
      setIsLoading(false);
      
    } catch (error) {
      alert("초대장 전송에 실패했습니다. 다시 시도해주세요.");
      setIsLoading(false);
    }
  };

  const handleEmailChange = (e) => {
    setInviteEmail(e.target.value);
  };

  // 수정 모달 열기
  const handleEditClick = (institution) => {
    setEditingInstitution(institution);
    setEditFormData({
      educationId: institution.educationId,
      educationName: institution.educationName,
      memberEmail: institution.memberEmail,
      memberName: institution.memberName,
      businessNumber: institution.businessNumber,
      description: institution.description,
      memberAddress: institution.memberAddress,
      educationAddress: institution.educationAddress,
      createdAt: institution.createdAt
    });
    setIsEditModalOpen(true);
  };

  // 수정 폼 데이터 변경
  const handleEditFormChange = (field, value) => {
    setEditFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // 주소 검색 함수
  const handleAddressSearch = (addressType) => {
    if (window.daum && window.daum.Postcode) {
      new window.daum.Postcode({
        oncomplete: function(data) {
          const fullAddress = data.address + (data.buildingName ? ' ' + data.buildingName : '');
          handleEditFormChange(addressType, fullAddress);
        }
      }).open();
    } else {
      alert("주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
    }
  };

  // 수정 저장
  const handleSaveEdit = async () => {
    try {
      const response = await http.put(`/api/education/update/${editFormData.educationId}`, editFormData);
      
      // 로컬 상태 업데이트
      setRegisteredInstitutions(prev => 
        prev.map(inst => 
          inst.educationId === editFormData.educationId 
            ? { ...inst, ...editFormData }
            : inst
        )
      );
      
      alert("학원 정보가 성공적으로 수정되었습니다.");
      setIsEditModalOpen(false);
      setEditingInstitution(null);
      setEditFormData({
        educationId: "",
        educationName: "",
        memberEmail: "",
        memberName: "",
        businessNumber: "",
        description: "",
        memberAddress: "",
        educationAddress: "",
        createdAt: ""
      });
      
    } catch (error) {
      alert("학원 정보 수정에 실패했습니다. 다시 시도해주세요.");
    }
  };

  // 수정 모달 닫기
  const handleCloseEditModal = () => {
    setIsEditModalOpen(false);
    setEditingInstitution(null);
    setEditFormData({
      educationId: "",
      educationName: "",
      memberEmail: "",
      memberName: "",
      businessNumber: "",
      description: "",
      memberAddress: "",
      educationAddress: "",
      createdAt: ""
    });
  };

  return (
    <PageLayout currentPage="institution" userRole="admin">
      <div className="flex">
        <Sidebar title="학원 관리" menuItems={sidebarMenuItems} currentPath="/institution/invite" />
        <main className="flex-1 p-8">
          <div className="max-w-6xl">
          <div className="mb-8">
            <h1 className="text-3xl font-bold mb-4" style={{ color: "#2C3E50" }}>
              학원 초대장 관리
            </h1>
            <p className="text-lg" style={{ color: "#95A5A6" }}>
              새로운 학원장에게 초대장을 보내고 등록된 학원을 관리합니다.
            </p>
          </div>

          {/* 초대장 전송 섹션 */}
          <Card className="mb-8">
            <CardHeader>
              <CardTitle style={{ color: "#2C3E50" }}>초대장 전송</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="inviteEmail" className="block text-sm font-medium text-gray-700 mb-2">
                    학원장 이메일 주소 <span className="text-red-500">*</span>
                  </Label>
                  <div className="flex gap-2">
                    <Input
                      id="inviteEmail"
                      type="email"
                      placeholder="example@email.com"
                      value={inviteEmail}
                      onChange={handleEmailChange}
                      className="flex-1 placeholder:text-gray-400"
                    />
                    <Button
                      type="button"
                      onClick={handleSendInvite}
                      disabled={isLoading}
                      className="bg-emerald-500 text-white hover:bg-emerald-600 disabled:opacity-50"
                    >
                      {isLoading ? "전송 중..." : "초대장 전송"}
                    </Button>
                  </div>
                  <p className="text-sm text-gray-500 mt-2">
                    입력한 이메일 주소로 학원 등록 초대장이 전송됩니다.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 등록된 학원 목록 섹션 */}
          <Card>
            <CardHeader>
              <CardTitle style={{ color: "#2C3E50" }}>등록된 학원 목록</CardTitle>
            </CardHeader>
            <CardContent>
              {isLoadingInstitutions ? (
                <div className="flex justify-center items-center py-8">
                  <div className="text-gray-500">학원 목록을 불러오는 중...</div>
                </div>
              ) : registeredInstitutions.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  등록된 학원이 없습니다.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b border-gray-200">
                        <th className="text-left py-3 px-4 font-medium text-gray-700">학원명</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-700">이메일</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-700">등록일</th>
                        <th className="text-left py-3 px-4 font-medium text-gray-700">관리</th>
                      </tr>
                    </thead>
                    <tbody>
                      {registeredInstitutions.map((institution) => (
                        <tr key={institution.id} className="border-b border-gray-100 hover:bg-gray-50">
                          <td className="py-3 px-4 font-medium text-gray-900">
                            {institution.educationName}
                          </td>
                          <td className="py-3 px-4 text-gray-600">
                            {institution.memberEmail}
                          </td>
                          <td className="py-3 px-4 text-gray-600">
                            {institution.createdAt ? new Date(institution.createdAt).toLocaleDateString('ko-KR') : '-'}
                          </td>
                          <td className="py-3 px-4">
                            <div className="flex gap-2">
                              <Button
                                size="sm"
                                variant="outline"
                                className="text-xs text-emerald-600 border-emerald-600 hover:bg-emerald-50"
                                onClick={() => handleEditClick(institution)}
                              >
                                수정
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>

          {/* 안내사항 */}
          <Card className="mt-6" style={{ borderColor: "#1ABC9C", borderWidth: "1px" }}>
            <CardContent className="pt-6">
              <h3 className="font-semibold mb-2" style={{ color: "#2C3E50" }}>
                초대장 관리 안내사항
              </h3>
              <ul className="space-y-1 text-sm" style={{ color: "#95A5A6" }}>
                <li>• 초대장을 받은 학원장은 이메일의 링크를 통해 가입할 수 있습니다.</li>
                <li>• 초대장은 24시간 동안 유효합니다.</li>
                <li>• 등록된 학원의 상태를 관리할 수 있습니다.</li>
                <li>• 비활성화된 학원은 시스템 접근이 제한됩니다.</li>
                <li>• 문의사항이 있으시면 관리자에게 연락해주세요.</li>
              </ul>
            </CardContent>
          </Card>
        </div>
      </main>
      </div>

      {/* 수정 모달 */}
      <Dialog open={isEditModalOpen} onOpenChange={setIsEditModalOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto bg-white">
          <DialogHeader>
            <DialogTitle style={{ color: "#2C3E50" }}>
              학원 정보 수정
            </DialogTitle>
          </DialogHeader>
          
          <div className="space-y-6">
            {/* 학원 기본 정보 */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="editName" className="block text-sm font-medium text-gray-700 mb-2">
                  학원명 <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="editName"
                  value={editFormData.educationName}
                  onChange={(e) => handleEditFormChange('educationName', e.target.value)}
                  placeholder="학원명을 입력하세요"
                />
              </div>
              
              <div>
                <Label htmlFor="editEmail" className="block text-sm font-medium text-gray-700 mb-2">
                  이메일 <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="editEmail"
                  type="email"
                  value={editFormData.memberEmail}
                  onChange={(e) => handleEditFormChange('memberEmail', e.target.value)}
                  placeholder="example@email.com"
                />
              </div>
            </div>

            {/* 학원장 정보 */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="editDirectorName" className="block text-sm font-medium text-gray-700 mb-2">
                  학원장 이름 <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="editDirectorName"
                  value={editFormData.memberName}
                  onChange={(e) => handleEditFormChange('memberName', e.target.value)}
                  placeholder="학원장 이름을 입력하세요"
                />
              </div>
              
              <div>
                <Label htmlFor="editBusinessNumber" className="block text-sm font-medium text-gray-700 mb-2">
                  사업자 번호 <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="editBusinessNumber"
                  value={editFormData.businessNumber}
                  onChange={(e) => handleEditFormChange('businessNumber', e.target.value)}
                  placeholder="123-45-67890"
                />
              </div>
            </div>

            {/* 학원 설명 */}
            <div>
              <Label htmlFor="editDescription" className="block text-sm font-medium text-gray-700 mb-2">
                학원 설명
              </Label>
              <Textarea
                id="editDescription"
                value={editFormData.description}
                onChange={(e) => handleEditFormChange('description', e.target.value)}
                placeholder="학원에 대한 설명을 입력하세요"
                rows={3}
              />
            </div>

            {/* 주소 정보 */}
            <div className="space-y-4">
              <div>
                <Label htmlFor="editDirectorAddress" className="block text-sm font-medium text-gray-700 mb-2">
                  학원장 주소
                </Label>
                <div className="flex gap-2">
                  <Input
                    id="editDirectorAddress"
                    value={editFormData.memberAddress}
                    onChange={(e) => handleEditFormChange('memberAddress', e.target.value)}
                    placeholder="학원장 주소를 입력하세요"
                    className="flex-1"
                  />
                  <Button
                    type="button"
                    onClick={() => handleAddressSearch('memberAddress')}
                    className="bg-blue-500 text-white hover:bg-blue-600 disabled:opacity-50"
                  >
                    주소 검색
                  </Button>
                </div>
              </div>
              
              <div>
                <Label htmlFor="editInstitutionAddress" className="block text-sm font-medium text-gray-700 mb-2">
                  학원 주소 <span className="text-red-500">*</span>
                </Label>
                <div className="flex gap-2">
                  <Input
                    id="editInstitutionAddress"
                    value={editFormData.educationAddress}
                    onChange={(e) => handleEditFormChange('educationAddress', e.target.value)}
                    placeholder="학원 주소를 입력하세요"
                    className="flex-1"
                  />
                  <Button
                    type="button"
                    onClick={() => handleAddressSearch('educationAddress')}
                    className="bg-blue-500 text-white hover:bg-blue-600 disabled:opacity-50"
                  >
                    주소 검색
                  </Button>
                </div>
              </div>
            </div>

            {/* 버튼 */}
            <div className="flex justify-end gap-3 pt-4">
              <Button
                variant="outline"
                onClick={handleCloseEditModal}
              >
                취소
              </Button>
              <Button
                onClick={handleSaveEdit}
                className="bg-emerald-500 text-white hover:bg-emerald-600"
              >
                저장
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </PageLayout>
  );
}
