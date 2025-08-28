import { Link } from "react-router-dom";
import React, { useState, useEffect } from "react";
import axios from "axios";

const permissionMap = {
  //계정 등록은 개별/일괄 어차피 등록이니까 헤더에서 체크한걸로 끝 사이드는 제약 필요 없을 듯듯
  
  //회원 정보
  "academic-register": "10",
  "academic-students": "2",

  //과정 관리
  "course-list": "14",
  "course-register": "7",
  "subject-list": "13",
  "subject-register": "15",
  "subject-detail": "16",

  //강의실
  "classroom-list": "17",
  "classroom-register": "18",

  //시험 및 성적
  "exam-courses": "20",
  "courses-list": "25",
  "question-list": "26",

  //설문
  "survey-items": "22",
  "survey-templates": "23",
  "survey-lectures": "24",

}

export default function Sidebar({ title, subtitle, menuItems = [], currentPath = "" }) {
  // 메뉴 설정을 중앙화했으므로 자동 추가 로직 제거
  const enhancedMenuItems = menuItems;

  const [userRole, setUserRole] = useState("")
  const [grantedPmIds, setGrantedPmIds] = useState([])

  // 마운트 시 현재 유저 정보(로컬스토리지)와 권한 API 호출
  useEffect(() => {
    const raw = localStorage.getItem("currentUser");
    if (!raw) return;

    const user = JSON.parse(raw);
    setUserRole(user.role); // e.g. "staff", "admin"

    const isStaff = user.role === "staff" || user.role === "ROLE_STAFF";
    if (!isStaff) return;

    // Vite proxy로 "/api" → 백엔드 URL
    axios
      .get("/api/staff/my-permissions", { withCredentials: true })
      .then(res => {
        const { mainPermissions, subPermissions } = res.data.data;
        const all = [...mainPermissions, ...subPermissions];
        setGrantedPmIds(
          all
            .filter(p => p.isGranted)
            .map(p => String(p.pmId))
        );
      })
      .catch(console.error);
  }, []);

  const isStaff = userRole === "staff" || userRole === "ROLE_STAFF"

  return (
    <aside className="w-64 min-h-screen p-4" style={{ backgroundColor: "#f1f2f6" }}>
      <div className="mb-6">
        <h2 className="text-lg font-semibold mb-2" style={{ color: "#2C3E50" }}>
          {title}
        </h2>
        {subtitle && (
          <p className="text-sm" style={{ color: "#95A5A6" }}>
            {subtitle}
          </p>
        )}
      </div>
      <nav className="space-y-2">
      {menuItems.map(item => {
          const requiredPm = permissionMap[item.key]
          const allowed    = !isStaff || !requiredPm || grantedPmIds.includes(requiredPm)
          const isActive   = currentPath === item.href
        

          const baseClass = `
            block w-full text-left px-4 py-2 rounded transition-colors
            ${isActive ? "bg-gray-200 font-semibold text-gray-900" : "hover:bg-gray-200 text-gray-800"}
          `

          return allowed
            ? (
              <Link
                key={item.key}
                to={item.href}
                className={baseClass}
              >
                {item.label}
              </Link>
            )
            : (
              <button
                key={item.key}
                disabled
                className={`
                  ${baseClass}
                  text-gray-400 opacity-50 pointer-events-none cursor-not-allowed
                `}
              >
                {item.label}
              </button>
            )
        })}
      </nav>
    </aside>
  )
}
