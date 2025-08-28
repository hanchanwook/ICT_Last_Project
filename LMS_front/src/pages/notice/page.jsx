import { useState, useEffect, useRef } from "react";
import { http, getCookie } from "@/components/auth/http";
import { jwtDecode } from "jwt-decode";
import PageLayout from "@/components/ui/page-layout";
import { Button } from "@/components/ui/button";
import { Plus, Search, Eye, Edit, Trash2, Calendar, User } from "lucide-react";
import Sidebar from "@/components/layout/sidebar";
import { getMenuItems } from "@/components/ui/menuConfig";
import { useNavigate } from "react-router-dom";

export default function NoticePage() {
  const [notices, setNotices] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedNotice, setSelectedNotice] = useState(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [sidebarMenuItems, setSidebarMenuItems] = useState([]);
  const navigate = useNavigate();
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef(null);
  const [isUploading, setIsUploading] = useState(false);

  const [newNotice, setNewNotice] = useState({
    title: "",
    content: "",
    previewImage: "",
    imageUrl: "",
    isActive: 1
  });

  const [editNotice, setEditNotice] = useState({
    id: null,
    title: "",
    content: "",
    previewImage: "",
    imageUrl: "",
    isActive: 1
  });

  const changeRole = (currentUser) => {
    if (currentUser?.role === "student") return "ROLE_STUDENT";
    if (currentUser?.role === "instructor") return "ROLE_INSTRUCTOR";
    if (currentUser?.role === "admin") return "ROLE_ADMIN";
    if (currentUser?.role === "staff") return "ROLE_STAFF";
    if (currentUser?.role === "director") return "ROLE_DIRECTOR";
  };

  const getUserName = () => {
    try {
      const refreshToken = getCookie("refresh");
      if (refreshToken) {
        const decoded = jwtDecode(refreshToken);
        return decoded.name || decoded.memberName || null;
      }
      const accessToken = getCookie("token");
      if (accessToken) {
        const decoded = jwtDecode(accessToken);
        return decoded.name || decoded.memberName || null;
      }
      return null;
    } catch (error) {
      return null;
    }
  };

  const getEducationId = () => {
    try {
      const refreshToken = getCookie("refresh");
      if (refreshToken) {
        const decoded = jwtDecode(refreshToken);
        return decoded.educationId || null;
      }
      return null;
    } catch (error) {
      return null;
    }
  };

  useEffect(() => {
    const userInfo = localStorage.getItem("currentUser");
    if (userInfo) {
      const user = JSON.parse(userInfo);
      setCurrentUser(user);
      let menuItems;
      if (changeRole(user) === "ROLE_INSTRUCTOR" || changeRole(user) === "ROLE_STUDENT") {
        menuItems = getMenuItems("notice");
      } else if (
        changeRole(user) === "ROLE_ADMIN" ||
        changeRole(user) === "ROLE_STAFF" ||
        changeRole(user) === "ROLE_DIRECTOR"
      ) {
        menuItems = getMenuItems("notice-manage");
        menuItems = menuItems.filter((item) => item.key !== "popup-manage");
      } else {
        menuItems = getMenuItems("notice");
      }
      const updatedMenuItems = menuItems.map((item) => {
        if (
          changeRole(user) === "ROLE_ADMIN" ||
          changeRole(user) === "ROLE_STAFF" ||
          changeRole(user) === "ROLE_DIRECTOR"
        ) {
          return { ...item, label: "공지사항 관리" };
        } else {
          return { ...item, label: "공지사항" };
        }
      });
      setSidebarMenuItems(updatedMenuItems);
    }
    fetchNotices();
  }, []);

  const fetchNotices = async () => {
    try {
      setIsLoading(true);
      const educationId = getEducationId();
      const response = await http.get("/api/notice/list", { params: { educationId: educationId } });
      setNotices(response.data || []);
    } catch (error) {
      setNotices([]);
    } finally {
      setIsLoading(false);
    }
  };

  const filteredNotices = notices.filter((notice) => {
    const matchesSearch =
      notice.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      notice.content.toLowerCase().includes(searchTerm.toLowerCase());
    if (!matchesSearch) return false;
    const userRole = changeRole(currentUser);
    if (userRole === "ROLE_ADMIN" || userRole === "ROLE_DIRECTOR" || userRole === "ROLE_STAFF") return true;
    if (userRole === "ROLE_STUDENT" || userRole === "ROLE_INSTRUCTOR") return notice.isActive === 1;
    return false;
  });

  const formatDate = (dateString) => {
    if (!dateString) return "날짜 없음";
    try {
      return new Date(dateString).toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
      });
    } catch (error) {
      console.error("날짜 포맷팅 오류:", error);
      return "날짜 오류";
    }
  };

  const handleCreateNotice = async () => {
    try {
      const userName = getUserName();
      const noticeData = {
        title: newNotice.title,
        content: newNotice.content,
        active: newNotice.isActive,
        educationId: getEducationId(),
        createdBy: userName || currentUser?.name || "작성자",
        updatedBy: null,
        imageUrl: newNotice.imageUrl || ""
      };
      await http.post("/api/notice/create", noticeData);
      alert("공지사항이 등록되었습니다.");
      setIsCreateModalOpen(false);
      setNewNotice({ title: "", content: "", previewImage: "", imageUrl: "", isActive: 1 });
      fetchNotices();
    } catch (error) {
      alert("공지사항 등록에 실패했습니다.");
    }
  };

  const handleEditNotice = async () => {
    try {
      const userName = getUserName();
      const noticeData = {
        title: editNotice.title,
        content: editNotice.content,
        active: editNotice.isActive,
        educationId: getEducationId(),
        createdBy: editNotice.createdBy || "작성자",
        updatedBy: userName || currentUser?.name || "수정자",
        imageUrl: editNotice.imageUrl || ""
      };
      await http.put(`/api/notice/${editNotice.id}`, noticeData);
      alert("공지사항이 수정되었습니다.");
      setIsEditModalOpen(false);
      setEditNotice({ id: null, title: "", content: "", previewImage: "", imageUrl: "", isActive: 1 });
      fetchNotices();
    } catch (error) {
      alert("공지사항 수정에 실패했습니다.");
    }
  };

  const handleDeleteNotice = async (id) => {
    if (!confirm("정말로 이 공지사항을 삭제하시겠습니까?")) return;
    try {
      await http.delete(`/api/notice/${id}`);
      alert("공지사항이 삭제되었습니다.");
      fetchNotices();
    } catch (error) {
      alert("공지사항 삭제에 실패했습니다.");
    }
  };

  const handleToggleVisibility = async (id, currentStatus) => {
    try {
      const newStatus = currentStatus === 1 ? false : true;
      await http.patch(`/api/notice/${id}/toggle-visibility`, newStatus, {
        headers: { "Content-Type": "application/json" }
      });
      alert("공개 상태가 변경되었습니다.");
      fetchNotices();
    } catch (error) {
      alert("공개 상태 변경에 실패했습니다.");
    }
  };

  const openEditModal = (notice) => {
    setEditNotice({
      id: notice.id,
      title: notice.title,
      content: notice.content,
      previewImage: notice.imageUrl || "",
      imageUrl: notice.imageUrl || "",
      isActive: notice.isActive,
      createdBy: notice.createdBy || notice.created_by || notice.creatorName || "작성자"
    });
    setIsEditModalOpen(true);
  };

  const handleFileUpload = async (file) => {
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      alert("이미지 파일만 업로드 가능합니다.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      alert("파일 크기는 5MB 이하여야 합니다.");
      return;
    }
    try {
      setIsUploading(true);
      const reader = new FileReader();
      reader.onload = (e) => {
        const base64Data = e.target.result;
        if (isCreateModalOpen) {
          setNewNotice({ ...newNotice, previewImage: base64Data, imageUrl: base64Data });
        } else if (isEditModalOpen) {
          setEditNotice({ ...editNotice, previewImage: base64Data, imageUrl: base64Data });
        }
      };
      reader.onerror = () => { throw new Error("파일 읽기에 실패했습니다."); };
      reader.readAsDataURL(file);
    } catch (error) {
      alert("이미지 업로드에 실패했습니다.");
    } finally {
      setIsUploading(false);
    }
  };

  const handleDragOver = (e) => { e.preventDefault(); setIsDragOver(true); };
  const handleDragLeave = (e) => { e.preventDefault(); setIsDragOver(false); };
  const handleDrop = (e) => { e.preventDefault(); setIsDragOver(false); handleFileUpload(e.dataTransfer.files[0]); };
  const handleFileSelect = (e) => { handleFileUpload(e.target.files[0]); };
  const handleRemoveImage = () => {
    if (isCreateModalOpen) setNewNotice({ ...newNotice, previewImage: "", imageUrl: "" });
    else if (isEditModalOpen) setEditNotice({ ...editNotice, previewImage: "", imageUrl: "" });
  };

  return (
    <PageLayout currentPage="notice" userRole={currentUser?.role || "student"} userName={currentUser?.name || "사용자"}>
      <div className="flex">
        <Sidebar title="공지사항" menuItems={sidebarMenuItems} currentPath="/notice" />
        <div className="flex-1 p-8">
          <div className="max-w-6xl mx-auto">
            {/* 검색창 + 새 공지 작성 버튼 */}
            <div className="mb-10 flex gap-4 items-center">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <input
                  type="text"
                  placeholder="공지사항 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              {(changeRole(currentUser) !== "ROLE_STUDENT" && changeRole(currentUser) !== "ROLE_INSTRUCTOR") && (
                <Button className="p-2 text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]" onClick={() => setIsCreateModalOpen(true)}>
                  <Plus className="w-4 h-4" /> 새 공지 작성
                </Button>
              )}
            </div>

            {/* 공지 목록 */}
            {isLoading ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
                <p className="mt-2 text-gray-600">로딩 중...</p>
              </div>
            ) : (
              <div className="bg-white rounded-lg shadow ">
                {filteredNotices.length === 0 ? (
                  <div className="text-center py-12">
                    <p className="text-gray-500">등록된 공지사항이 없습니다.</p>
                  </div>
                ) : (
                  <div className="divide-y divide-gray-200">
                    {filteredNotices.map((notice) => (
                      <div key={notice.id} className="p-6 transition-colors ">
                        <div className="flex justify-between items-start">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                              <h3 className="text-lg font-semibold text-gray-900">{notice.title}</h3>
                              {notice.isActive === 1 ? (
                                <span className="px-2 py-1 text-xs font-medium bg-green-100 text-green-800 rounded-full">공개</span>
                              ) : (
                                <span className="px-2 py-1 text-xs font-medium bg-gray-100 text-gray-800 rounded-full">비공개</span>
                              )}
                            </div>
                            <p className="text-gray-600 line-clamp-2 mb-3">{notice.content}</p>
                            <div className="flex items-center gap-4 text-sm text-gray-500">
                              {(notice.updatedAt || notice.updated_at) && (notice.updatedBy || notice.updated_by || notice.modifierName) ? (
                                <>
                                  <div className="flex items-center gap-1"><Calendar className="w-4 h-4" />수정: {formatDate(notice.updatedAt || notice.updated_at)}</div>
                                  <div className="flex items-center gap-1"><User className="w-4 h-4" />{notice.updatedBy || notice.updated_by || notice.modifierName}</div>
                                </>
                              ) : (
                                <>
                                  <div className="flex items-center gap-1"><Calendar className="w-4 h-4" />{formatDate(notice.createdAt || notice.created_at)}</div>
                                  <div className="flex items-center gap-1"><User className="w-4 h-4" />{notice.createdBy || notice.created_by || notice.creatorName || "작성자"}</div>
                                </>
                              )}
                            </div>
                          </div>
                          <div className="flex items-center gap-2 ml-4">
                            <Button size="sm" onClick={() => { setSelectedNotice(notice); setIsDetailModalOpen(true); }} className="hover:bg-green-100 hover:scale-110">
                              <Eye className="w-4 h-4 text-[#1abc9c]" />
                            </Button>
                            {(changeRole(currentUser) === "ROLE_ADMIN" || changeRole(currentUser) === "ROLE_STAFF" || changeRole(currentUser) === "ROLE_DIRECTOR") && (
                              <>
                                <Button size="sm" onClick={() => openEditModal(notice)} className="hover:bg-gray-100 hover:scale-110">
                                  <Edit className="w-4 h-4 text-[#b0c4de]" />
                                </Button>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => handleToggleVisibility(notice.id, notice.isActive)}
                                  className={notice.isActive === 1 ? "text-gray-600 hover:text-gray-700" : "text-white hover:opacity-90"}
                                  style={notice.isActive === 1 ? {} : { backgroundColor: "#1ABC9C" }}
                                >
                                  {notice.isActive === 1 ? "비공개" : "공개"}
                                </Button>
                                <Button size="sm" onClick={() => handleDeleteNotice(notice.id)} className="hover:bg-red-100 hover:scale-110">
                                  <Trash2 className="w-4 h-4 text-[#e74c3c]" />
                                </Button>
                              </>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* 상세 모달 */}
          {isDetailModalOpen && selectedNotice && (
            <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center z-50">
              <div className="bg-white rounded-lg p-6 max-w-2xl w-full mx-4 max-h-[80vh] overflow-y-auto">
                <div className="flex justify-between items-start mb-4">
                  <h2 className="text-2xl font-bold text-gray-900">{selectedNotice.title}</h2>
                  <Button size="sm" onClick={() => setIsDetailModalOpen(false)}>✕</Button>
                </div>
                <div className="mb-4">
                  <div className="flex items-center gap-4 text-sm text-gray-500 mb-3">
                    {(selectedNotice.updatedAt || selectedNotice.updated_at) && (selectedNotice.updatedBy || selectedNotice.updated_by || selectedNotice.modifierName) ? (
                      <>
                        <div className="flex items-center gap-1"><Calendar className="w-4 h-4" />수정: {formatDate(selectedNotice.updatedAt || selectedNotice.updated_at)}</div>
                        <div className="flex items-center gap-1"><User className="w-4 h-4" />{selectedNotice.updatedBy || selectedNotice.updated_by || selectedNotice.modifierName}</div>
                      </>
                    ) : (
                      <>
                        <div className="flex items-center gap-1"><Calendar className="w-4 h-4" />{formatDate(selectedNotice.createdAt || selectedNotice.created_at)}</div>
                        <div className="flex items-center gap-1"><User className="w-4 h-4" />{selectedNotice.createdBy || selectedNotice.created_by || selectedNotice.creatorName || "작성자"}</div>
                      </>
                    )}
                  </div>
                  {selectedNotice.imageUrl && (
                    <div className="mb-4">
                      <img src={selectedNotice.imageUrl} alt="공지사항 이미지" className="max-w-full h-auto rounded-lg" />
                    </div>
                  )}
                  <div className="prose max-w-none">
                    <p className="text-gray-800 whitespace-pre-line">{selectedNotice.content}</p>
                  </div>
                </div>
                <div className="flex justify-end">
                  <Button onClick={() => setIsDetailModalOpen(false)} className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]">닫기</Button>
                </div>
              </div>
            </div>
          )}

          {/* 작성 모달 */}
          {isCreateModalOpen && (
            <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center z-50">
              <div className="bg-white rounded-lg p-6 max-w-2xl w-full mx-4 max-h-[80vh] overflow-y-auto">
                <div className="flex justify-between items-start mb-6">
                  <h2 className="text-2xl font-bold text-gray-900">공지사항 작성</h2>
                  <Button size="sm" onClick={() => setIsCreateModalOpen(false)}>✕</Button>
                </div>
                <div className="space-y-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">제목 *</label>
                    <input
                      type="text"
                      value={newNotice.title}
                      onChange={(e) => setNewNotice({ ...newNotice, title: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      placeholder="공지사항 제목을 입력하세요"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">이미지 업로드 (선택)</label>
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
                      ) : newNotice.previewImage ? (
                        <div>
                          <img src={newNotice.previewImage} alt="업로드된 이미지" className="max-w-full h-32 mx-auto mb-2 rounded" />
                          <div className="flex justify-center gap-2 mt-2">
                            <p className="text-sm text-gray-600">클릭하여 이미지 변경</p>
                            <button type="button" onClick={(e) => { e.stopPropagation(); handleRemoveImage(); }} className="text-sm text-red-600 hover:text-red-700 font-medium">
                              제거
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div>
                          <div className="text-gray-400 mb-2">
                            <svg className="mx-auto h-12 w-12" stroke="currentColor" fill="none" viewBox="0 0 48 48">
                              <path d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                          </div>
                          <p className="text-sm text-gray-600">이미지를 드래그하여 놓거나 클릭하여 선택하세요</p>
                          <p className="text-xs text-gray-500 mt-1">PNG, JPG, GIF (최대 5MB)</p>
                        </div>
                      )}
                      <input ref={fileInputRef} type="file" accept="image/*" onChange={handleFileSelect} className="hidden" />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">내용 *</label>
                    <textarea
                      value={newNotice.content}
                      onChange={(e) => setNewNotice({ ...newNotice, content: e.target.value })}
                      rows={6}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      placeholder="공지사항 내용을 입력하세요"
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    <input type="checkbox" id="isActive" checked={newNotice.isActive === 1} onChange={(e) => setNewNotice({ ...newNotice, isActive: e.target.checked ? 1 : 0 })} className="rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
                    <label htmlFor="isActive" className="text-sm text-gray-700">즉시 공개</label>
                  </div>
                </div>
                <div className="flex justify-end gap-3 mt-8">
                  <Button className="border border-gray-400 text-gray-400 hover:bg-gray-200" onClick={() => setIsCreateModalOpen(false)}>취소</Button>
                  <Button onClick={handleCreateNotice} disabled={!newNotice.title || !newNotice.content} className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]">등록</Button>
                </div>
              </div>
            </div>
          )}

          {/* 수정 모달 */}
          {isEditModalOpen && (
            <div className="fixed inset-0 bg-[rgba(0,0,0,0.5)] flex items-center justify-center z-50">
              <div className="bg-white rounded-lg p-6 max-w-2xl w-full mx-4 max-h-[80vh] overflow-y-auto">
                <div className="flex justify-between items-start mb-6">
                  <h2 className="text-2xl font-bold text-gray-900">공지사항 수정</h2>
                  <Button size="sm" onClick={() => setIsEditModalOpen(false)}>✕</Button>
                </div>
                <div className="space-y-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">제목 *</label>
                    <input type="text" value={editNotice.title} onChange={(e) => setEditNotice({ ...editNotice, title: e.target.value })} className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" placeholder="공지사항 제목을 입력하세요" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">이미지 업로드 (선택)</label>
                    <div className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${isDragOver ? "border-blue-500 bg-blue-50" : "border-gray-300 hover:border-gray-400"}`} onDragOver={handleDragOver} onDragLeave={handleDragLeave} onDrop={handleDrop} onClick={() => !isUploading && fileInputRef.current?.click()} style={{ cursor: isUploading ? "not-allowed" : "pointer" }}>
                      {isUploading ? (
                        <div>
                          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-2"></div>
                          <p className="text-sm text-gray-600">이미지 업로드 중...</p>
                        </div>
                      ) : editNotice.previewImage ? (
                        <div>
                          <img src={editNotice.previewImage} alt="업로드된 이미지" className="max-w-full h-32 mx-auto mb-2 rounded" />
                          <div className="flex justify-center gap-2 mt-2">
                            <p className="text-sm text-gray-600">클릭하여 이미지 변경</p>
                            <button type="button" onClick={(e) => { e.stopPropagation(); handleRemoveImage(); }} className="text-sm text-red-600 hover:text-red-700 font-medium">제거</button>
                          </div>
                        </div>
                      ) : (
                        <div>
                          <div className="text-gray-400 mb-2">
                            <svg className="mx-auto h-12 w-12" stroke="currentColor" fill="none" viewBox="0 0 48 48">
                              <path d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                          </div>
                          <p className="text-sm text-gray-600">이미지를 드래그하여 놓거나 클릭하여 선택하세요</p>
                          <p className="text-xs text-gray-500 mt-1">PNG, JPG, GIF (최대 5MB)</p>
                        </div>
                      )}
                      <input ref={fileInputRef} type="file" accept="image/*" onChange={handleFileSelect} className="hidden" />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">내용 *</label>
                    <textarea value={editNotice.content} onChange={(e) => setEditNotice({ ...editNotice, content: e.target.value })} rows={6} className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" placeholder="공지사항 내용을 입력하세요" />
                  </div>
                  <div className="flex items-center gap-2">
                    <input type="checkbox" id="edit_isActive" checked={editNotice.isActive === 1} onChange={(e) => setEditNotice({ ...editNotice, isActive: e.target.checked ? 1 : 0 })} className="rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
                    <label htmlFor="edit_isActive" className="text-sm text-gray-700">공개</label>
                  </div>
                </div>
                <div className="flex justify-end gap-3 mt-8">
                  <Button className="border border-gray-400 text-gray-400 hover:bg-gray-200" onClick={() => setIsEditModalOpen(false)}>취소</Button>
                  <Button onClick={handleEditNotice} disabled={!editNotice.title || !editNotice.content} className="text-white bg-[#1abc9c] hover:bg-[rgb(10,150,120)]">수정</Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </PageLayout>
  );
}
