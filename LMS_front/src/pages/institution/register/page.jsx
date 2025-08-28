import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import PageLayout from "@/components/ui/page-layout";
import Sidebar from "@/components/layout/sidebar";
import { getMenuItems } from "@/components/ui/menuConfig";
import { http } from "@/components/auth/http";

// --- AddressSearch 컴포넌트 (다음 우편번호 API) ---
function AddressSearch({ value, onSelect }) {
  useEffect(() => {
    if (!window.daum || !window.daum.Postcode) {
      const script = document.createElement("script");
      script.src =
        "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
      script.async = true;
      document.body.appendChild(script);
    }
  }, []);

  const openDaumPostcode = () => {
    if (!window.daum || !window.daum.Postcode) {
      alert("주소 검색 서비스를 불러오지 못했습니다. 다시 시도해주세요.");
      return;
    }

    new window.daum.Postcode({
      oncomplete: (data) => {
        const fullAddress = data.address;
        onSelect(fullAddress);
      },
    }).open();
  };

  return (
    <div className="flex gap-2">
      <Input
        value={value}
        placeholder="주소를 입력하세요"
        readOnly
        className="text-gray-500 flex-1"
      />
      <Button
        type="button"
        className="bg-emerald-500 text-white"
        onClick={openDaumPostcode}
      >
        주소 찾기
      </Button>
    </div>
  );
}

export default function InstitutionRegisterPage() {
  const [formData, setFormData] = useState({
    educationName: "",
    businessNumber: "",
    memberName: "",
    memberPhone: "",
    memberEmail: "",
    password: "",
    educationAddress: "", // 학원 주소
    educationDetailAddress: "", // 학원 상세주소
    description: "",
  });

  const sidebarMenuItems = getMenuItems('institution');

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    
    if (name === 'memberEmail') {
      if (selectedDomain === '직접 입력') {
        // 직접 입력 선택 시 완전한 이메일 주소 입력 가능
        setFormData((prev) => ({
          ...prev,
          [name]: value,
        }));
      } else {
        // 드롭다운 선택 시 기존 도메인 유지
        const [, domain] = formData.memberEmail.split('@');
        const newEmail = domain ? `${value}@${domain}` : value;
        setFormData((prev) => ({
          ...prev,
          [name]: newEmail,
        }));
      }
    } else {
      setFormData((prev) => ({
        ...prev,
        [name]: value,
      }));
    }
  };

  // 연락처 자동 하이픈 처리
  const handlePhoneNumberChange = (e) => {
    // 마스킹된 값에서 실제 전화번호를 추출하는 로직
    let inputValue = e.target.value;
    
    // **** 부분을 실제 입력된 숫자로 교체
    if (inputValue.includes('****')) {
      const parts = inputValue.split('-');
      if (parts.length === 3) {
        // 현재 formData의 실제 전화번호에서 중간 부분을 가져옴
        const currentParts = formData.memberPhone.split('-');
        if (currentParts.length === 3) {
          inputValue = `${parts[0]}-${currentParts[1]}-${parts[2]}`;
        }
      }
    }
    
    // 숫자만 추출하여 포맷팅
    let value = inputValue.replace(/[^0-9]/g, "");
    if (value.length > 3) value = value.slice(0, 3) + "-" + value.slice(3);
    if (value.length > 8) value = value.slice(0, 8) + "-" + value.slice(8);
    if (value.length > 13) value = value.slice(0, 13);
    setFormData((prev) => ({ ...prev, memberPhone: value }));
  };

  // 전화번호 마스킹 함수
  const maskPhoneNumber = (phoneNumber) => {
    if (!phoneNumber) return '';
    const parts = phoneNumber.split('-');
    if (parts.length === 3) {
      return `${parts[0]}-****-${parts[2]}`;
    }
    return phoneNumber;
  };

  // 사업자 등록번호 자동 하이픈 처리 (000-00-00000)
  const handleBusinessNumberChange = (e) => {
    let value = e.target.value.replace(/[^0-9]/g, "");
    if (value.length > 3) value = value.slice(0, 3) + "-" + value.slice(3);
    if (value.length > 6) value = value.slice(0, 6) + "-" + value.slice(6);
    if (value.length > 12) value = value.slice(0, 12);
    setFormData((prev) => ({ ...prev, businessNumber: value }));
  };

  const emailDomains = ["gmail.com", "naver.com", "daum.net", "직접 입력"];
  const [selectedDomain, setSelectedDomain] = useState('');
  
  const handleEmailDomainChange = (e) => {
    const domain = e.target.value;
    setSelectedDomain(domain);
    
    if (!domain) return; // 빈 값이면 아무것도 하지 않음
    
    if (domain === '직접 입력') {
      // 직접 입력 선택 시 이메일을 로컬 파트만 남기고 초기화
      setFormData((prev) => {
        const [localPart] = prev.memberEmail.split('@');
        return { ...prev, memberEmail: localPart || '' };
      });
    } else {
      setFormData((prev) => {
        const [localPart] = prev.memberEmail.split("@");
        // 로컬 파트가 없으면 빈 문자열로 처리
        const emailLocalPart = localPart || '';
        return { ...prev, memberEmail: `${emailLocalPart}@${domain}` };
      });
    }
  };

  // 화면에 표시할 이메일 값
  const getDisplayEmail = () => {
    if (selectedDomain === '직접 입력') {
      // 직접 입력 선택 시 완전한 이메일 주소 표시
      return formData.memberEmail;
    } else {
      // 드롭다운 선택 시 로컬 파트만 표시
      const [localPart] = formData.memberEmail.split('@');
      return localPart || '';
    }
  };

  // 이메일 도메인 선택 여부 확인
  const isEmailDomainSelected = () => {
    // 이메일 선택 옵션이 선택된 경우 false 반환
    if (!selectedDomain || selectedDomain === '') {
      return false;
    }
    
    if (selectedDomain === '직접 입력') {
      // 직접 입력 선택 시 @가 포함된 완전한 이메일 주소인지 확인
      return formData.memberEmail.includes('@') && formData.memberEmail.split('@')[1]?.length > 0;
    } else {
      // 드롭다운 선택 시 @가 포함되어 있는지 확인
      return formData.memberEmail.includes('@');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const payload = {
      education: {
        educationName: formData.educationName,
        businessNumber: formData.businessNumber,
        educationAddress: formData.educationAddress,
        educationDetailAddress: formData.educationDetailAddress,
        description: formData.description,
      },
      member: {
        memberName: formData.memberName,
        memberPhone: formData.memberPhone,
        memberEmail: formData.memberEmail,
        password: formData.password,
      },
      user: {
        name: formData.memberName,
        phone: formData.memberPhone,
        email: formData.memberEmail,
        password: formData.password, // 비밀번호 전달
      },
    };

    try {
      await http.post("/api/education/register", payload);
      alert("학원이 성공적으로 등록되었습니다!");
      handleReset();
    } catch (error) {
      alert("등록 중 오류가 발생했습니다.");
    }
  };

  const handleReset = () => {
    setFormData({
      educationName: "",
      businessNumber: "",
      memberName: "",
      memberPhone: "",
      memberEmail: "",
      password: "",
      educationAddress: "",
      educationDetailAddress: "",
      description: "",
    });
    
    // 선택된 도메인도 초기화
    setSelectedDomain('');
  };

  const inputTextColor = (value) => ({
    color: value ? "#000000" : "#95A5A6",
  });

  return (
    <PageLayout currentPage="institution" userRole="admin">
      <div className="flex">
        <Sidebar title="학원 관리" menuItems={sidebarMenuItems} currentPath="/institution/register" />
        <main className="flex-1 p-8">
          <div className="max-w-4xl">
          <div className="mb-8">
            <h1 className="text-3xl font-bold mb-4" style={{ color: "#2C3E50" }}>
              학원 등록
            </h1>
            <p className="text-lg" style={{ color: "#95A5A6" }}>
              새로운 학원을 시스템에 등록합니다.
            </p>
          </div>

          <Card>
            <CardHeader>
              <CardTitle style={{ color: "#2C3E50" }}>학원 정보 입력</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit} className="space-y-6">
                {/* 학원명 / 사업자등록번호 */}
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <Label htmlFor="educationName">
                      학원명 <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="educationName"
                      name="educationName"
                      placeholder="학원명을 입력하세요"
                      value={formData.educationName}
                      onChange={handleInputChange}
                      required
                      style={inputTextColor(formData.educationName)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="businessNumber">
                      사업자등록번호 <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="businessNumber"
                      name="businessNumber"
                      placeholder="000-00-00000"
                      value={formData.businessNumber}
                      onChange={handleBusinessNumberChange}
                      required
                      style={inputTextColor(formData.businessNumber)}
                    />
                  </div>
                </div>

                {/* 대표자명 / 연락처 */}
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <Label htmlFor="memberName">
                      학원장 성명 <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="memberName"
                      name="memberName"
                      placeholder="대표자명을 입력하세요"
                      value={formData.memberName}
                      onChange={handleInputChange}
                      required
                      style={inputTextColor(formData.memberName)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="memberPhone">
                      연락처 <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="memberPhone"
                      name="memberPhone"
                      placeholder="010-0000-0000"
                      value={maskPhoneNumber(formData.memberPhone)}
                      onChange={handlePhoneNumberChange}
                      required
                      style={inputTextColor(formData.memberPhone)}
                    />
                  </div>
                </div>

                {/* 이메일 + 비밀번호 */}
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <Label htmlFor="memberEmail">
                      이메일 <span className="text-red-500">*</span>
                    </Label>
                    <div className="flex gap-2">
                      <Input
                        id="memberEmail"
                        name="memberEmail"
                        type="text"
                        placeholder="이메일 주소"
                        value={getDisplayEmail()}
                        onChange={handleInputChange}
                        required
                        className="flex-1 placeholder:text-gray-400"
                      />
                      <select
                        onChange={handleEmailDomainChange}
                        className="border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 px-3 py-2"
                      >
                        <option value="">이메일 선택</option>
                        {emailDomains.map((domain) => (
                          <option key={domain} value={domain}>
                            {domain}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="password">
                      비밀번호 <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="password"
                      name="password"
                      type="password"
                      placeholder="비밀번호 입력"
                      value={formData.password}
                      onChange={handleInputChange}
                      required
                      style={inputTextColor(formData.password)}
                    />
                  </div>
                </div>

                {/* 학원 주소 */}
                <div className="space-y-2">
                  <Label htmlFor="educationAddress">
                    학원 주소 <span className="text-red-500">*</span>
                  </Label>
                  <AddressSearch
                    value={formData.educationAddress}
                    onSelect={(address) =>
                      setFormData((prev) => ({ ...prev, educationAddress: address }))
                    }
                  />
                </div>

                {/* 학원 상세주소 */}
                <div className="space-y-2">
                  <Label htmlFor="educationDetailAddress">
                    상세주소
                  </Label>
                  <Input
                    id="educationDetailAddress"
                    name="educationDetailAddress"
                    placeholder="상세주소를 입력하세요 (예: 101호, 2층)"
                    value={formData.educationDetailAddress}
                    onChange={handleInputChange}
                    style={inputTextColor(formData.educationDetailAddress)}
                  />
                </div>



                {/* 학원 설명 */}
                <div className="space-y-2">
                  <Label htmlFor="description">학원 설명</Label>
                  <Textarea
                    id="description"
                    name="description"
                    placeholder="학원에 대한 간단한 설명을 입력하세요"
                    value={formData.description}
                    onChange={handleInputChange}
                    rows={4}
                    style={inputTextColor(formData.description)}
                  />
                </div>

                {/* 버튼 그룹 */}
                <div className="flex justify-end space-x-4 pt-6">
                  <Button type="button" variant="outline" onClick={handleReset}>
                    초기화
                  </Button>
                  <Button
                    type="submit"
                    className="text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                    style={{ backgroundColor: "#1ABC9C" }}
                    disabled={!isEmailDomainSelected()}
                  >
                    학원 등록
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          {/* 안내사항 */}
          <Card className="mt-6" style={{ borderColor: "#1ABC9C", borderWidth: "1px" }}>
            <CardContent className="pt-6">
              <h3 className="font-semibold mb-2" style={{ color: "#2C3E50" }}>
                학원 등록 안내사항
              </h3>
              <ul className="space-y-1 text-sm" style={{ color: "#95A5A6" }}>
                <li>• 필수 항목(*)은 반드시 입력해주세요.</li>
                <li>• 사업자등록번호는 정확히 입력해주세요.</li>
                <li>• 등록 후 승인 과정을 거쳐 시스템 이용이 가능합니다.</li>
                <li>• 문의사항이 있으시면 관리자에게 연락해주세요.</li>
              </ul>
            </CardContent>
          </Card>
        </div>
      </main>
      </div>
    </PageLayout>
  );
}
