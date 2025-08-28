// src/api/http.js
import axios from "axios";

export const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:19091';

export const http = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true, // 쿠키 전송을 위해 활성화
});

// 요청 인터셉터: JWT Authorization 헤더 추가
http.interceptors.request.use(
  (config) => {
    // JWT 토큰을 여러 헤더에 추가 (백엔드 JWTFilter가 어떤 헤더를 찾는지 모르므로)
    const token = localStorage.getItem("token") || localStorage.getItem("accessToken")
    if (token) {
      // 여러 가능한 헤더명으로 토큰 전송
      config.headers.Authorization = `Bearer ${token}`
      config.headers['X-Auth-Token'] = token
      config.headers['Authentication'] = `Bearer ${token}`
      config.headers['Access-Token'] = token
      
      // 쿠키로도 토큰 전송 (백엔드가 쿠키에서 토큰을 찾을 수도 있음)
      document.cookie = `accessToken=${token}; path=/; max-age=3600`
      document.cookie = `jwt=${token}; path=/; max-age=3600`
      
      console.log("🔑 JWT 토큰 헤더들 설정됨:")
      console.log("- Authorization:", `Bearer ${token.substring(0, 20)}...`)
      console.log("- X-Auth-Token:", `${token.substring(0, 20)}...`)
      console.log("- Authentication:", `Bearer ${token.substring(0, 20)}...`)
      console.log("- Access-Token:", `${token.substring(0, 20)}...`)
      console.log("🍪 쿠키로도 토큰 전송: accessToken, jwt")
      console.log("📦 모든 요청 헤더:", Object.keys(config.headers))
    } else {
      console.log("❌ JWT 토큰이 없어서 인증 헤더 설정 안됨")
    }

    // 모든 POST, PUT, PATCH 요청에 userId 자동 추가 (특정 API 제외)
    const currentUser = JSON.parse(localStorage.getItem("currentUser") || "{}")
    const memberId = currentUser.memberId
    
    // userId 자동 추가를 제외할 API 경로들
    const excludePaths = [
      '/api/members/addcourse' // 과정 신청 API - 학생의 ID를 직접 전송해야 함
      
    ]
    
    const shouldExclude = excludePaths.some(path => config.url?.includes(path))
    
    if (memberId && config.method && ["post", "put", "patch"].includes(config.method) && !shouldExclude) {
      // 배열인지 체크해서 배열이면 그대로 둠
      if (Array.isArray(config.data)) {
        config.data = config.data.map(item => ({ ...item, userId: memberId }));
      } else if (typeof config.data === "object" && config.data !== null && !config.data.userId) {
        config.data = { ...config.data, userId: memberId };
      }
    }
    // GET 요청에도 userId를 쿼리 파라미터로 자동 추가 (단, 특정 경로 제외)
    if (memberId && config.method === "get") {
      // 학생 상세 정보 조회 API는 경로에 userId가 포함되므로 쿼리 파라미터 추가 제외
      const excludePaths = [
        '/api/instructor/students/', // 학생 상세 정보 조회
        '/api/instructor/attendance/students/' // 학생 출석 이력 조회
      ]
      
      const shouldExclude = excludePaths.some(path => config.url?.includes(path))
      
      if (!shouldExclude) {
        if (!config.params) config.params = {}
        if (!config.params.userId) config.params.userId = memberId
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);


// 쿠키에서 값을 가져오는 함수
export function getCookie(name) {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) return parts.pop().split(';').shift()
  return null
}


//  공통 응답/에러 처리 인터셉터
http.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // 토큰 만료 또는 인증 에러 처리 예시
    if (error.response?.status === 401) {
      console.warn("인증 만료: 다시 로그인 해주세요.");
      // 예: window.location.href = "/login"
    } else if (error.response?.status === 403) {
      console.warn("권한 없음");
    } else if (error.response?.status === 404) {
      // 404 에러는 리소스를 찾을 수 없는 경우이므로 조용히 처리
      // 각 컴포넌트에서 적절히 처리하도록 함
    } else {
      console.error("API 에러:", error.response?.data?.message || error.message);
    }
    return Promise.reject(error);
  }
);
