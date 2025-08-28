import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import PageLayout from "@/components/ui/page-layout"
import Sidebar from "@/components/layout/sidebar"
import { getMenuItems } from "@/components/ui/menuConfig"
import { jwtDecode } from 'jwt-decode';
import { http } from "@/components/auth/http"
import {getAllCourse} from "@/api/suhyeon/courseApi"

// --- AddressSearch 컴포넌트 (컨트롤드로 변경) ---
function AddressSearch({ value, onSelect }) {
  useEffect(() => {
    if (!window.daum || !window.daum.Postcode) {
      const script = document.createElement("script");
      script.src = "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
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
        name="memberAddress"
        value={value}
        placeholder="주소를 입력하세요"
        readOnly
        className="text-gray-500 placeholder:text-gray-400"
      />
      <Button
        type="button"
        className="bg-white text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white"
        onClick={openDaumPostcode}
      >
        주소 찾기
      </Button>
    </div>
  );
}

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
}

export default function IndividualRegisterPage() {
  const [formData, setFormData] = useState({
    memberRole: "ROLE_STUDENT",
    memberName: "",
    password: "",
    confirmPassword: "",
    memberEmail: "",
    memberPhone: "",
    memberBirthday: "",
    memberAddress: "",
    courseId: "",
    educationId: "",
    userId: "",
    memberId: "",
  });

  const [courseList, setCourseList] = useState([]);
  const sidebarMenuItems = getMenuItems('account');

  const [userRole, setUserRole] = useState(null);
  const [grantedPmIds, setGrantedPmIds] = useState([]);

  // 중복 체크 상태 관리
  const [duplicateChecks, setDuplicateChecks] = useState({
    email: false
  });
  const [isChecking, setIsChecking] = useState({
    email: false
  });

  // 중복 체크 함수들
  const checkEmailDuplicate = async () => {
    if (!formData.memberEmail) {
      alert("이메일을 먼저 입력해주세요.");
      return;
    }

    setIsChecking(prev => ({ ...prev, email: true }));
    try {
      const response = await http.post("/api/registration/check-email", {
        memberEmail: formData.memberEmail
      });
      
      if (response.data.isDuplicate) {
        alert("이미 사용 중인 이메일입니다.");
        setDuplicateChecks(prev => ({ ...prev, email: false }));
        // 이메일 입력 데이터 초기화
        setFormData(prev => ({ ...prev, memberEmail: "" }));
      } else {
        alert("사용 가능한 이메일입니다.");
        setDuplicateChecks(prev => ({ ...prev, email: true }));
      }
    } catch (error) {
      alert("이메일 중복 체크 중 오류가 발생했습니다.");
    } finally {
      setIsChecking(prev => ({ ...prev, email: false }));
    }
  };

  useEffect(() => {
    const token = getCookie("refresh");
    if (!token) return;
  
    const decoded = jwtDecode(token);
    const role = decoded.role;      // 토큰 페이로드 안에 role 필드가 있다고 가정
    setUserRole(role);
  
    if (role === "ROLE_STAFF") {
      http.get("/api/staff/my-permissions", { withCredentials: true })
        .then(res => {
          const { mainPermissions = [], subPermissions = [] } = res.data.data;
          const all = [...mainPermissions, ...subPermissions];
          // 성공적으로 userRole, grantedPmIds 세팅
          setGrantedPmIds(
            all.filter(p => p.isGranted)
              .map(p => String(p.pmId))
          );
        })
        .catch(err => {
        });
    }
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    // 중복 체크 확인
    if (!duplicateChecks.email) {
      alert("이메일 중복 검사를 완료해주세요.");
      return;
    }

    // 생년월일 유효성 검사
    if (formData.memberBirthday) {
      const birthDate = new Date(formData.memberBirthday);
      const currentDate = new Date();
      
      if (birthDate > currentDate) {
        alert("생년월일은 현재 날짜보다 이전이어야 합니다. 다시 입력해주세요.");
        return;
      }
    }

    try {
      const response = await http.post("/api/registration/registerEach", formData);
      
      // 전화번호 중복 체크 응답 처리
      if (response.data === "등록된 전화번호입니다. 다시 입력해주세요.") {
        alert("등록된 전화번호입니다. 다시 입력해주세요.");
        // 전화번호 입력 필드 초기화
        setFormData(prev => ({ ...prev, memberPhone: "" }));
        return;
      }
      
      alert(`${formData.memberName}님의 ${getUserTypeLabel(formData.memberRole)} 계정이 성공적으로 등록되었습니다!`);
      
      // 성공 시 폼 초기화 (educationId는 유지)
      const currentEducationId = formData.educationId;
      const currentUserId = formData.userId;
      const currentMemberId = formData.memberId;
      
      setFormData({
        memberRole: "ROLE_STAFF",
        memberName: "",
        password: "",
        confirmPassword: "",
        memberEmail: "",
        memberPhone: "",
        memberBirthday: "",
        memberAddress: "",
        courseId: "",
        educationId: currentEducationId,
        userId: currentUserId,
        memberId: currentMemberId,
      });
      
      // 중복 체크 상태도 초기화
      setDuplicateChecks({
        email: false
      });
    } catch (error) {
      alert("계정 등록 중 오류가 발생했습니다. 다시 시도해주세요.");
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }))
    
    // 이메일이나 전화번호가 변경되면 중복 체크 상태 리셋
    if (name === 'memberEmail' && duplicateChecks.email) {
      setDuplicateChecks(prev => ({ ...prev, email: false }));
    }
  };

  const handleUserTypeChange = (value) => {
    setFormData((prev) => ({
      ...prev,
      memberRole: value,
    }))
  };

  const handleReset = () => {
    // JWT 토큰에서 educationId, userId, memberId 다시 가져오기
    const token = getCookie("refresh");
    let educationId = "";
    let userId = "";
    let memberId = "";
    
    if (token) {
      try {
        const decodedToken = jwtDecode(token);
        educationId = decodedToken.educationId || "";
        userId = decodedToken.userId || "";
        memberId = decodedToken.userId || "";
      } catch (error) {
      }
    }

    setFormData({
      memberRole: "ROLE_STAFF",
      memberName: "",
      password: "",
      confirmPassword: "",
      memberEmail: "",
      memberPhone: "",
      memberBirthday: "",
      memberAddress: "",
      courseId: "",
      educationId: educationId,
      userId: userId,
      memberId: memberId,
    })
    
    // 중복 체크 상태도 초기화
    setDuplicateChecks({
      email: false
    });
  };

  const handleBirthDateChange = (e) => {
    let value = e.target.value.replace(/[^0-9]/g, '');
    if (value.length > 4) value = value.slice(0, 4) + '-' + value.slice(4);
    if (value.length > 7) value = value.slice(0, 7) + '-' + value.slice(7);
    if (value.length > 10) value = value.slice(0, 10);
    setFormData((prev) => ({ ...prev, memberBirthday: value }));
  };

  // 연락처 자동 하이픈 처리 및 마스킹
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

  const getUserTypeLabel = (type) => {
    switch (type) {
      case "ROLE_STAFF": return "일반직원"
      case "ROLE_INSTRUCTOR": return "강사"
      case "ROLE_STUDENT": return "학생"
      default: return "사용자"
    }
  };

  const userTypes = [
    { value: "ROLE_STAFF", label: "일반직원", id: "staff" },
    { value: "ROLE_INSTRUCTOR", label: "강사", id: "instructor" },
    { value: "ROLE_STUDENT", label: "학생", id: "student" },
  ];

  // 과정 목록 불러오기 (userId 세팅된 후)
  useEffect(() => {
    if (!formData.userId) return;
  
    const fetchCourses = async () => {
      try {
        const res = await http.get("/api/getCourse/list", {
          params: { educationId: formData.educationId },
        });

        const studentCountData = await getAllCourse();

        // res.data가 바로 배열이므로 그대로 사용
        if (Array.isArray(res.data)) {
          // getAllCourse()에서 가져온 studentCount와 maxCapacity 정보를 병합
          const mergedCourseList = res.data.map(course => {
            // studentCountData에서 같은 courseId를 가진 데이터 찾기
            const matchingCourse = studentCountData.find(sc => sc.courseId === course.courseId);
            if (matchingCourse) {
              return {
                ...course,
                studentCount: matchingCourse.studentCount,
                maxCapacity: matchingCourse.maxCapacity
              };
            }
            // 매칭되는 데이터가 없으면 기본값 설정
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
  
    fetchCourses();
  }, [formData.userId]);

  // JWT 토큰에서 userId, educationId 추출
  useEffect(() => {
    const token = getCookie("refresh");
    if (token) {
      const decodedToken = jwtDecode(token);
      setFormData((prev) => ({
        ...prev,
        educationId: decodedToken.educationId,
        userId: decodedToken.userId,
        memberId: decodedToken.userId,
      }));
    }
  }, []);

  return (
    <PageLayout currentPage="account">
      <div className="flex">
        <Sidebar title="계정 등록" menuItems={sidebarMenuItems} currentPath="/account/individual" actualUserRole={userRole} grantedPmIds={grantedPmIds} />

        <main className="flex-1 p-8">
          <div className="max-w-5xl mx-auto">
            <div className="mb-8">
              <h1 className="text-2xl font-bold mb-4" style={{ color: "#2C3E50" }}>
                개별 계정 등록
              </h1>
              <p className="text-lg" style={{ color: "#95A5A6" }}>
                새로운 사용자 계정을 개별적으로 등록합니다.
              </p>
            </div>

            <Card>
              <CardHeader>
                <CardTitle style={{ color: "#2C3E50" }}>계정 정보 입력</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* 사용자 유형 선택 */}
                  <div className="space-y-3">
                    <Label>
                      사용자 유형 <span className="text-red-500">*</span>
                    </Label>
                    <RadioGroup
                      value={formData.memberRole}
                      onValueChange={handleUserTypeChange}
                      className="flex flex-wrap gap-4"
                    >
                      {userTypes.map((type) => (
                        <div key={type.value} className="flex items-center space-x-2">
                          <RadioGroupItem value={type.value} id={type.id} />
                          <Label htmlFor={type.id} className="cursor-pointer">
                            {type.label}
                          </Label>
                        </div>
                      ))}
                    </RadioGroup>
                  </div>

                  {/* 기본 정보 */}
                  <div className="grid md:grid-cols-2 gap-6">
                    <div className="space-y-2">
                      <Label htmlFor="memberName">이름 <span className="text-red-500">*</span></Label>
                      <Input
                        id="memberName"
                        name="memberName"
                        placeholder="이름을 입력하세요"
                        value={formData.memberName}
                        onChange={handleInputChange}
                        required
                        className="placeholder:text-gray-400"
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="memberEmail">이메일 <span className="text-red-500">*</span></Label>
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
                        <Button
                          type="button"
                          onClick={checkEmailDuplicate}
                          disabled={isChecking.email}
                          className="bg-white text-[#1abc9c] border border-[#1abc9c] hover:bg-[#1abc9c] hover:text-white disabled:opacity-50"
                        >
                          {isChecking.email ? "검사 중..." : "중복 검사"}
                        </Button>
                      </div>
                      {duplicateChecks.email && (
                        <p className="text-sm text-green-600">✓ 사용 가능한 이메일입니다.</p>
                      )}
                    </div>
                  </div>

                  <div className="grid md:grid-cols-2 gap-6">
                    <div className="space-y-2">
                      <Label htmlFor="birth">생년월일</Label>
                      <Input
                        id="memberBirthday"
                        name="memberBirthday"
                        placeholder="YYYY-MM-DD"
                        value={formData.memberBirthday}
                        onChange={handleBirthDateChange}
                        className="placeholder:text-gray-400"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="memberPhone">연락처 <span className="text-red-500">*</span></Label>
                      <div className="flex gap-2">
                        <Input
                          id="memberPhone"
                          name="memberPhone"
                          placeholder="010-0000-0000"
                          value={maskPhoneNumber(formData.memberPhone)}
                          onChange={handlePhoneNumberChange}
                          required
                          className="flex-1 placeholder:text-gray-400"
                        />
                      </div>
                    </div>
                  </div>

                  {/* 비밀번호 */}
                  <div className="grid md:grid-cols-2 gap-6">
                    <div className="space-y-2">
                      <Label htmlFor="password">비밀번호 <span className="text-red-500">*</span></Label>
                      <Input
                        id="password"
                        name="password"
                        type="password"
                        placeholder="비밀번호를 입력하세요"
                        value={formData.password}
                        onChange={handleInputChange}
                        required
                        className="placeholder:text-gray-400"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="confirmPassword">비밀번호 확인 <span className="text-red-500">*</span></Label>
                      <Input
                        id="confirmPassword"
                        name="confirmPassword"
                        type="password"
                        placeholder="비밀번호를 다시 입력하세요"
                        value={formData.confirmPassword}
                        onChange={handleInputChange}
                        required
                        className="placeholder:text-gray-400"
                      />
                    </div>
                  </div>

                  {/* 주소 정보 */}
                  <div className="space-y-2">
                    <Label htmlFor="memberAddress">주소</Label>
                    <AddressSearch
                      value={formData.memberAddress}
                      onSelect={(address) => setFormData((prev) => ({ ...prev, memberAddress: address }))}
                    />
                  </div>

                  {(formData.memberRole === "ROLE_STUDENT") && (
                    <div className="space-y-2">
                      <Label htmlFor="course">과정명</Label>
                      {courseList.length > 0 ? (
                        <select
                          id="courseId"
                          name="courseId"
                          value={formData.courseId}
                          className="border border-black rounded-md w-70 h-9 pl-2"
                          onChange={(e) => setFormData({ ...formData, courseId: e.target.value })}
                        >
                          <option value="" >과정명을 선택하세요</option>
                          {courseList
                            .filter(course => course.studentCount < course.maxCapacity && course.courseActive === 0)
                            .map(course => (
                              <option key={course.courseId} value={course.courseId}>
                                {course.courseName}
                              </option>
                            ))}
                        </select>
                      ) : (
                        <select disabled>
                          <option>과정 불러오는 중...</option>
                        </select>
                      )}
                    </div>
                  )}

                  {/* 버튼 그룹 */}
                  <div className="flex justify-end space-x-4 pt-6">
                    <Button type="button" variant="outline" onClick={handleReset}
                    className="hover:bg-gray-100">초기화</Button>
                    <Button 
                      type="submit" 
                      className="text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                      style={{ backgroundColor: "#1ABC9C" }}
                      disabled={!isEmailDomainSelected()}
                    >
                      계정 등록
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>

            {/* 등록 안내 */}
            <Card className="mt-6" style={{ borderColor: "#1ABC9C", borderWidth: "1px" }}>
              <CardContent>
                <h3 className="font-semibold mb-2" style={{ color: "#2C3E50" }}>개별 등록 안내사항</h3>
                <ul className="space-y-1 text-sm" style={{ color: "#95A5A6" }}>
                  <li>• 필수 항목(*)은 반드시 입력해주세요.</li>
                  <li>• 아이디는 중복될 수 없으며, 영문과 숫자 조합을 권장합니다.</li>
                  <li>• 비밀번호는 8자 이상으로 설정해주세요.</li>
                  <li>• 등록된 계정은 즉시 시스템에서 사용 가능합니다.</li>
                </ul>
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
    </PageLayout>
  )
}
