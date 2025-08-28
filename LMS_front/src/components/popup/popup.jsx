import { useState, useEffect } from "react";
import { http, getCookie } from "@/components/auth/http";
import { jwtDecode } from "jwt-decode";
import { X } from "lucide-react";

export default function Popup({ educationId }) {
  const [activePopups, setActivePopups] = useState([]);
  const [closedPopups, setClosedPopups] = useState([]);
  const [currentPopupIndex, setCurrentPopupIndex] = useState(0);
  const [showPopup, setShowPopup] = useState(true);

  // 오늘 보지 않기 체크
  const checkTodayDismissed = () => {
    const today = new Date().toDateString();
    const dismissed = localStorage.getItem('popupDismissed');
    return dismissed === today;
  };

  // 오늘 보지 않기 설정
  const setTodayDismissed = () => {
    const today = new Date().toDateString();
    localStorage.setItem('popupDismissed', today);
  };

  // 토큰에서 educationId 추출
  const getEducationIdFromToken = () => {
    try {
      const token = getCookie("refresh");
      if (token) {
        const decoded = jwtDecode(token);
        console.log("JWT 토큰 디코딩 결과:", decoded);
        return decoded.educationId;
      }
    } catch (error) {
      console.error("토큰 디코딩 실패:", error);
    }
    return null;
  };

  // 활성 팝업 조회
  const fetchActivePopups = async () => {
    console.log("팝업 컴포넌트 마운트됨");
    
    try {
      // 토큰에서 educationId 추출
      const tokenEducationId = getEducationIdFromToken();
      console.log("토큰에서 추출한 educationId:", tokenEducationId);
      
      // props로 받은 educationId 또는 토큰에서 추출한 educationId 사용
      const finalEducationId = educationId || tokenEducationId;
      console.log("최종 사용할 educationId:", finalEducationId);
      
      console.log("팝업 API 호출 시작");
      const response = await http.get("/api/popup/list", {
        params: { educationId: finalEducationId }
      });
      
      console.log("팝업 API 응답:", response.data);
      console.log("응답 데이터 타입:", typeof response.data);
      console.log("응답 데이터 길이:", response.data ? response.data.length : 0);
      
      // content가 있는 팝업만 필터링
      const popupsWithContent = (response.data || []).filter(popup => {
        console.log("팝업 데이터:", popup);
        console.log("content 존재 여부:", !!popup.content);
        return popup.content;
      });
      
      console.log("content가 있는 팝업:", popupsWithContent);
      console.log("content가 있는 팝업 개수:", popupsWithContent.length);
      
      // API에서 데이터가 없으면 테스트용 팝업 추가
      if (popupsWithContent.length === 0) {
        console.log("API에서 팝업이 없어서 테스트용 팝업 추가");
        const testPopup = {
          id: "test-popup",
          content: `테스트 팝업입니다. educationId: ${finalEducationId || '없음'}, API에서 데이터를 가져왔지만 content가 없거나 빈 배열입니다.`
        };
        popupsWithContent.push(testPopup);
      }
      
      setActivePopups(popupsWithContent);
    } catch (error) {
      console.error("팝업 조회 실패:", error);
      setActivePopups([
        {
          id: "error-popup",
          content: `팝업 API 호출 오류: ${error.message}`,
        },
      ]);
    }
  };

  // 팝업 닫기
  const closePopup = (popupId) => {
    console.log("팝업 닫기:", popupId);
    setClosedPopups(prev => [...prev, popupId]);
    // 다음 팝업으로 이동
    setCurrentPopupIndex(prev => prev + 1);
  };

  // 오늘 보지 않기 클릭
  const handleTodayDismiss = () => {
    setTodayDismissed();
    setShowPopup(false);
  };

  useEffect(() => {
    console.log("팝업 useEffect 실행");
    // 오늘 보지 않기 체크
    if (checkTodayDismissed()) {
      setShowPopup(false);
      return;
    }
    fetchActivePopups();
  }, [educationId]);

  // 닫힌 팝업 제외하고 표시할 팝업들
  const visiblePopups = activePopups.filter(popup => !closedPopups.includes(popup.id));
  
  console.log("activePopups:", activePopups);
  console.log("closedPopups:", closedPopups);
  console.log("visiblePopups:", visiblePopups);
  console.log("currentPopupIndex:", currentPopupIndex);
  
  // 현재 표시할 팝업 (하나씩만)
  const currentPopup = visiblePopups[currentPopupIndex];
  
  console.log("currentPopup:", currentPopup);

  if (!showPopup || !currentPopup) {
    console.log("표시할 팝업이 없습니다.");
    return null;
  }

  console.log("팝업 렌더링:", currentPopup.content);

  return (
    <div className="fixed bottom-4 left-4 z-50">
      <div
        className="bg-white rounded-lg shadow-lg border border-gray-200 overflow-hidden relative hover:shadow-xl transition-all duration-300 max-w-sm w-80"
      >
        {/* 닫기 버튼 */}
        <button
          onClick={() => closePopup(currentPopup.id)}
          className="absolute top-3 right-3 text-gray-500 hover:text-gray-700 z-10 bg-white rounded-full p-1 shadow-sm hover:bg-gray-100 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>

        {/* 내용 */}
        <div className="p-4">
          {/* 텍스트 내용 */}
          {currentPopup.content && (
            <div 
              className="text-gray-700 leading-relaxed text-sm mb-4"
              dangerouslySetInnerHTML={{ __html: currentPopup.content }}
            />
          )}

          {/* 오늘 보지 않기 버튼 */}
          <div className="flex items-center justify-between pt-3 border-t border-gray-100">
            <label className="flex items-center text-xs text-gray-500 cursor-pointer">
              <input
                type="checkbox"
                className="mr-2"
                onChange={handleTodayDismiss}
              />
              오늘 보지 않기
            </label>
          </div>
        </div>
      </div>
    </div>
  );
}
