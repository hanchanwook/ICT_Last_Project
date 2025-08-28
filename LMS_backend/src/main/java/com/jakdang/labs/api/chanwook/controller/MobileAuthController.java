package com.jakdang.labs.api.chanwook.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;
import com.jakdang.labs.api.chanwook.service.MobileAuthService;
import jakarta.servlet.http.HttpSession;

// 모바일 로그인 관리 컨트롤러

@RestController
@RequestMapping("/api/auth/mobile")
@Slf4j
@RequiredArgsConstructor
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    // 1. 모바일 학생 로그인
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> mobileStudentLogin(@RequestBody Map<String, Object> loginData, HttpSession session) {
        log.info("=== 모바일 학생 로그인 요청 ===");
        log.info("로그인 데이터: {}", loginData);
        
        try {
            String username = (String) loginData.get("username");
            String password = (String) loginData.get("password");
            String sessionId = (String) loginData.get("sessionId"); // QR 코드에서 추출한 sessionId (선택사항)
            
            if (username == null || password == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "아이디와 비밀번호를 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("로그인 요청: username={}, sessionId={}", username, sessionId);
            
            Map<String, Object> result = mobileAuthService.mobileStudentLogin(username, password, sessionId);
            
            // 로그인 성공 시 세션에 사용자 정보 저장
            if ((Boolean) result.get("success")) {
                Map<String, Object> userInfo = (Map<String, Object>) result.get("userInfo");
                session.setAttribute("mobileUserInfo", userInfo);
                session.setAttribute("mobileLoggedIn", true);
                session.setMaxInactiveInterval(3600); // 1시간 세션 유지
                
                log.info("세션에 사용자 정보 저장 완료: userId={}, sessionId={}", 
                    userInfo.get("userId"), session.getId());
            }
            
            log.info("모바일 학생 로그인 성공: {}", username);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("모바일 학생 로그인 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 2. 모바일 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> mobileLogout(HttpSession session) {
        log.info("=== 모바일 로그아웃 요청 ===");
        
        try {
            // 세션에서 사용자 정보 제거
            session.removeAttribute("mobileUserInfo");
            session.removeAttribute("mobileLoggedIn");
            session.invalidate();
            
            Map<String, Object> result = mobileAuthService.mobileLogout();
            log.info("모바일 로그아웃 성공: sessionId={}", session.getId());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("모바일 로그아웃 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "로그아웃에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 3. 모바일 로그인 상태 확인
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkMobileLoginStatus(HttpSession session) {
        log.info("=== 모바일 로그인 상태 확인 요청 ===");
        
        try {
            Boolean isLoggedIn = (Boolean) session.getAttribute("mobileLoggedIn");
            Map<String, Object> userInfo = (Map<String, Object>) session.getAttribute("mobileUserInfo");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isLoggedIn", isLoggedIn != null && isLoggedIn);
            response.put("userInfo", userInfo);
            response.put("sessionId", session.getId());
            
            if (isLoggedIn != null && isLoggedIn && userInfo != null) {
                response.put("message", "로그인된 상태입니다.");
            } else {
                response.put("message", "로그인되지 않은 상태입니다.");
            }
            
            log.info("모바일 로그인 상태 확인 성공: isLoggedIn={}, sessionId={}", isLoggedIn, session.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("모바일 로그인 상태 확인 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "로그인 상태 확인에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 4. 세션에서 사용자 정보 조회
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getMobileUserInfo(HttpSession session) {
        log.info("=== 모바일 사용자 정보 조회 요청 ===");
        
        try {
            Map<String, Object> userInfo = (Map<String, Object>) session.getAttribute("mobileUserInfo");
            Boolean isLoggedIn = (Boolean) session.getAttribute("mobileLoggedIn");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isLoggedIn", isLoggedIn != null && isLoggedIn);
            response.put("userInfo", userInfo);
            response.put("sessionId", session.getId());
            
            if (isLoggedIn != null && isLoggedIn && userInfo != null) {
                response.put("message", "사용자 정보 조회 성공");
            } else {
                response.put("message", "로그인되지 않은 상태입니다.");
            }
            
            log.info("모바일 사용자 정보 조회 성공: sessionId={}", session.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("모바일 사용자 정보 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사용자 정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 