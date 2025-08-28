import { useState, useEffect, useRef } from "react";
import { http, getCookie } from "@/components/auth/http";  // getCookie 추가
import { jwtDecode } from "jwt-decode";                    // jwtDecode 추가
import PageLayout from "@/components/ui/page-layout";
import { Button } from "@/components/ui/button";
import { Plus, Search, Pencil, Trash } from "lucide-react";
import Sidebar from "@/components/layout/sidebar";
import { getMenuItems } from "@/components/ui/menuConfig";
import TextEditor from "@/components/ui/text-editor";

export default function PopupPage() {
  const [currentUser, setCurrentUser] = useState(null);
  const [educationId, setEducationId] = useState(null);   // educationId 상태
  const [sidebarMenuItems, setSidebarMenuItems] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);

  const [popups, setPopups] = useState([]);

  const [newPopup, setNewPopup] = useState({
    id: null,
    content: "",
    previewImage: "",
    imageUrl: "",
    validFrom: "",
    validTo: "",
  });

  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef(null);
  const [isUploading, setIsUploading] = useState(false);

  const changeRole = (currentUser) => {
    if (currentUser?.role === "student") return "ROLE_STUDENT";
    if (currentUser?.role === "instructor") return "ROLE_INSTRUCTOR";
    if (currentUser?.role === "admin") return "ROLE_ADMIN";
    if (currentUser?.role === "staff") return "ROLE_STAFF";
    if (currentUser?.role === "director") return "ROLE_DIRECTOR";
  };

  // === educationId가 세팅되면 리스트 호출 ===
  useEffect(() => {
    if (educationId) {
      fetchPopups(educationId);
    }
  }, [educationId]);

  // === 초기 로드 시 userInfo, educationId 추출 ===
  useEffect(() => {
    const userInfo = localStorage.getItem("currentUser");
    if (userInfo) {
      const user = JSON.parse(userInfo);
      setCurrentUser(user);

      // refreshToken decode → educationId 추출
      const refreshToken = getCookie("refresh");
      if (refreshToken) {
        try {
          const decoded = jwtDecode(refreshToken);
          if (decoded.educationId) setEducationId(decoded.educationId);
        } catch (e) {
          console.error("refreshToken decode 실패", e);
        }
      }

      // 메뉴 구성
      let menuItems;
      if (["ROLE_INSTRUCTOR", "ROLE_STUDENT"].includes(changeRole(user))) {
        menuItems = getMenuItems("notice");
      } else {
        menuItems = getMenuItems("notice-manage");
        if (changeRole(user) !== "ROLE_STAFF" && changeRole(user) !== "ROLE_DIRECTOR") {
          menuItems = menuItems.filter((item) => item.key !== "popup-manage");
        }
      }

      const updatedMenuItems = menuItems.map((item) => {
        if (item.key === "popup-manage") return { ...item, label: "팝업 관리" };
        if (["ROLE_ADMIN", "ROLE_STAFF", "ROLE_DIRECTOR"].includes(changeRole(user)))
          return { ...item, label: "공지사항 관리" };
        return { ...item, label: "공지사항" };
      });
      setSidebarMenuItems(updatedMenuItems);
    }
  }, []);

  // === 팝업 리스트 조회 ===
  const fetchPopups = async (eduId) => {
    try {
      const res = await http.get("/api/popup/list", {
        params: { educationId: eduId }
      });
      setPopups(res.data || []);
    } catch (err) {
      console.error(err);
      setPopups([]);
    }
  };

  const handleFileUpload = async (file) => {
    if (!file) return;
    if (!file.type.startsWith("image/")) return alert("이미지 파일만 업로드 가능합니다.");
    if (file.size > 5 * 1024 * 1024) return alert("파일 크기는 5MB 이하여야 합니다.");

    try {
      setIsUploading(true);
      const reader = new FileReader();
      reader.onload = (e) => setNewPopup((prev) => ({ 
        ...prev, 
        previewImage: e.target.result,
        imageUrl: e.target.result  // base64 데이터를 imageUrl에도 저장
      }));
      reader.readAsDataURL(file);
    } catch (error) {
      console.error(error);
      alert("이미지 업로드에 실패했습니다.");
    } finally {
      setIsUploading(false);
    }
  };

  const handleDragOver = (e) => { e.preventDefault(); setIsDragOver(true); };
  const handleDragLeave = (e) => { e.preventDefault(); setIsDragOver(false); };
  const handleDrop = (e) => { e.preventDefault(); setIsDragOver(false); if (e.dataTransfer.files.length > 0) handleFileUpload(e.dataTransfer.files[0]); };
  const handleFileSelect = (e) => handleFileUpload(e.target.files[0]);
  const handleRemoveImage = () => setNewPopup((prev) => ({ ...prev, previewImage: "", imageUrl: "" }));

  const handleCreateOrUpdate = async () => {
    const userName = currentUser?.name || "알 수 없음";
    try {
      if (isEditMode && newPopup.id) {
        await http.put("/api/popup/update", {
          id: newPopup.id,
          content: newPopup.content,
          imageUrl: newPopup.imageUrl,
          validFrom: newPopup.validFrom,
          validTo: newPopup.validTo,
          updatedBy: userName,
          educationId: educationId,
        });
        alert("팝업이 수정되었습니다.");
      } else {
        await http.post("/api/popup/create", {
          content: newPopup.content,
          imageUrl: newPopup.imageUrl,
          validFrom: newPopup.validFrom,
          validTo: newPopup.validTo,
          createdBy: userName,
          educationId: educationId,
        });
        alert("팝업이 생성되었습니다.");
      }
      setIsCreateModalOpen(false);
      setNewPopup({ id: null, content: "", previewImage: "", imageUrl: "", validFrom: "", validTo: "" });
      setIsEditMode(false);
      fetchPopups(educationId);
    } catch (error) {
      console.error(error);
      alert("팝업 저장 중 오류 발생");
    }
  };

  const handleEditPopup = (popup) => {
    setNewPopup({
      id: popup.id,
      content: popup.content,
      previewImage: popup.imageUrl,
      imageUrl: popup.imageUrl,
      validFrom: popup.validFrom,
      validTo: popup.validTo,
    });
    setIsEditMode(true);
    setIsCreateModalOpen(true);
  };

  const handleDeletePopup = async (id) => {
    if (!window.confirm("정말 삭제하시겠습니까?")) return;
    try {
      await http.delete(`/api/popup/delete?id=${id}`);
      alert("삭제 완료");
      fetchPopups(educationId);
    } catch (err) {
      console.error(err);
      alert("삭제 실패");
    }
  };

  return (
    <PageLayout currentPage="notice" userRole={currentUser?.role || "student"} userName={currentUser?.name || "사용자"}>
      <div className="flex">
        <Sidebar title="공지사항" menuItems={sidebarMenuItems} currentPath="/notice/popup" />
        <div className="flex-1 p-8">
          <div className="max-w-6xl mx-auto">
            <div className="flex justify-between items-center mb-8">
              <div>
                <h1 className="text-3xl font-bold text-gray-900">팝업 관리</h1>
                <p className="text-gray-600 mt-2">팝업을 관리합니다.</p>
              </div>
            </div>

            <div className="mb-10 flex gap-4 items-center">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="text"
                  placeholder="팝업 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <Button className="p-2 text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]"
                onClick={() => { setIsCreateModalOpen(true); setIsEditMode(false); }}>
                <Plus className="w-4 h-4" /> 새 팝업 생성
              </Button>
            </div>

            {/* 목록 테이블 */}
            <div className="bg-white rounded-lg shadow p-4">
              {popups.length > 0 ? (
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b">
                      <th className="py-2 px-4 text-left">내용</th>
                      <th className="py-2 px-4 text-left">작성자</th>
                      <th className="py-2 px-4 text-left">수정자</th>
                      <th className="py-2 px-4 text-left">작성일</th>
                      <th className="py-2 px-4 text-left">수정일</th>
                      <th className="py-2 px-4 text-center">액션</th>
                    </tr>
                  </thead>
                  <tbody>
                    {popups.map((popup) => (
                      <tr key={popup.id} className="border-b hover:bg-gray-50">
                        <td className="py-2 px-4" dangerouslySetInnerHTML={{ __html: popup.content }} />
                        <td className="py-2 px-4">{popup.createdBy}</td>
                        <td className="py-2 px-4">{popup.updatedBy || "없음"}</td>
                        <td className="py-2 px-4">{new Date(popup.createdAt).toLocaleString()}</td>
                        <td className="py-2 px-4">{popup.updatedAt ? new Date(popup.updatedAt).toLocaleString() : "없음"}</td>
                        <td className="py-2 px-4 text-center space-x-2">
                          <Button size="sm" onClick={() => handleEditPopup(popup)}><Pencil className="w-4 h-4" /></Button>
                          <Button size="sm" onClick={() => handleDeletePopup(popup.id)} className="bg-red-500 hover:bg-red-600"><Trash className="w-4 h-4" /></Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <div className="text-center py-12">
                  <p className="text-gray-500">데이터가 없습니다.</p>
                </div>
              )}
            </div>
          </div>

          {/* 생성/수정 모달 */}
          {isCreateModalOpen && (
            <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center z-50">
              <div className="bg-white rounded-lg p-6 max-w-2xl w-full mx-4 max-h-[80vh] overflow-y-auto">
                <div className="flex justify-between items-start mb-4">
                  <h2 className="text-2xl font-bold text-gray-900">{isEditMode ? "팝업 수정" : "팝업 생성"}</h2>
                  <Button size="sm" onClick={() => setIsCreateModalOpen(false)}>✕</Button>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">내용</label>
                    <TextEditor value={newPopup.content} onChange={(value) => setNewPopup((prev) => ({ ...prev, content: value }))} />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">이미지 업로드 (선택사항)</label>
                    <div
                      className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${isDragOver ? "border-blue-500 bg-blue-50" : "border-gray-300 hover:border-gray-400"}`}
                      onDragOver={handleDragOver}
                      onDragLeave={handleDragLeave}
                      onDrop={handleDrop}
                      onClick={() => !isUploading && fileInputRef.current?.click()}
                      style={{ cursor: isUploading ? "not-allowed" : "pointer" }}
                    >
                      {isUploading ? (
                        <div>
                          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-2"></div>
                          <p className="text-sm text-gray-600">이미지 업로드 중...</p>
                        </div>
                      ) : newPopup.previewImage ? (
                        <div>
                          <img src={newPopup.previewImage} alt="업로드된 이미지" className="max-w-full h-32 mx-auto mb-2 rounded" />
                          <div className="flex justify-center gap-2 mt-2">
                            <p className="text-sm text-gray-600">클릭하여 이미지 변경</p>
                            <button type="button" onClick={(e) => { e.stopPropagation(); handleRemoveImage(); }} className="text-sm text-red-600 hover:text-red-700 font-medium">제거</button>
                          </div>
                        </div>
                      ) : (
                        <div>
                          <p className="text-sm text-gray-600">이미지를 드래그하여 놓거나 클릭하여 선택하세요</p>
                          <p className="text-xs text-gray-500 mt-1">PNG, JPG, GIF (최대 5MB)</p>
                        </div>
                      )}
                      <input ref={fileInputRef} type="file" accept="image/*" onChange={handleFileSelect} className="hidden" />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">유효기간 시작일</label>
                      <input
                        type="datetime-local"
                        value={newPopup.validFrom}
                        onChange={(e) => setNewPopup((prev) => ({ ...prev, validFrom: e.target.value }))}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">유효기간 종료일</label>
                      <input
                        type="datetime-local"
                        value={newPopup.validTo}
                        onChange={(e) => setNewPopup((prev) => ({ ...prev, validTo: e.target.value }))}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      />
                    </div>
                  </div>
                </div>

                <div className="flex justify-end gap-3 mt-6">
                  <Button className="border border-gray-400 text-gray-400 hover:bg-gray-200" onClick={() => setIsCreateModalOpen(false)}>취소</Button>
                  <Button onClick={handleCreateOrUpdate} disabled={!newPopup.content} className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]">
                    {isEditMode ? "수정" : "생성"}
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </PageLayout>
  );
}
