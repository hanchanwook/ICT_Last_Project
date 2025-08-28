package com.jakdang.labs.api.lnuyasha.controller;

import com.jakdang.labs.api.lnuyasha.dto.CourseDTO;
import com.jakdang.labs.api.lnuyasha.dto.StaffCourseGradeDTO;
import com.jakdang.labs.api.lnuyasha.dto.StaffExamListDTO;
import com.jakdang.labs.api.lnuyasha.service.StaffExamService;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 강사용 시험 시스템 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/staff/exam")
@RequiredArgsConstructor
public class StaffExamController {
    
    private final StaffExamService staffExamService;
    private final JwtUtil jwtUtil;
    
    /**
     * 강사 과정 정보 조회
     * GET /api/staff/exam/courses?userId={userId}
     */
    @GetMapping("/courses")
    public ResponseEntity<?> getStaffCourses(
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        try {
            log.info("강사 과정 정보 조회 요청");
            
            // 1. 쿠키에서 refresh 토큰 추출
            String refreshToken = extractRefreshToken(request);
            if (refreshToken == null) {
                return ResponseEntity.status(401).body("Refresh 토큰이 없습니다.");
            }
            
            // 2. refresh 토큰으로 사용자 이메일 조회
            String email = jwtUtil.getUserEmail(refreshToken);
            if (email == null) {
                return ResponseEntity.status(401).body("토큰에서 이메일을 추출할 수 없습니다.");
            }
            
            // 3. 강사 과정 정보 조회
            List<CourseDTO> courses = staffExamService.getStaffCourses(email);
            
            // 4. 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", courses);
            response.put("message", "강사 과정 정보 조회 성공");
            response.put("totalCount", courses.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("강사 과정 정보 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }
    
    /**
     * 특정 과정의 시험 목록 조회
     * GET /api/staff/exam/courses/{courseId}/exams?userId={userId}
     */
    @GetMapping("/courses/{courseId}/exams")
    public ResponseEntity<?> getCourseExams(
            @PathVariable String courseId,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        try {
            log.info("과정 시험 목록 조회 요청: courseId = {}", courseId);
            
            // 1. 쿠키에서 refresh 토큰 추출
            String refreshToken = extractRefreshToken(request);
            if (refreshToken == null) {
                return ResponseEntity.status(401).body("Refresh 토큰이 없습니다.");
            }
            
            // 2. refresh 토큰으로 사용자 이메일 조회
            String email = jwtUtil.getUserEmail(refreshToken);
            if (email == null) {
                return ResponseEntity.status(401).body("토큰에서 이메일을 추출할 수 없습니다.");
            }
            
            // 3. 과정 시험 목록 조회
            List<StaffExamListDTO> exams = staffExamService.getCourseExams(courseId);
            
            // 4. 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", exams);
            response.put("message", "과정 시험 목록 조회 성공");
            response.put("totalCount", exams.size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("과정 시험 목록 조회 실패 (잘못된 요청): {}", e.getMessage());
            return ResponseEntity.status(400).body("잘못된 요청: " + e.getMessage());
        } catch (Exception e) {
            log.error("과정 시험 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }
    
    /**
     * 특정 과정의 성적 통계 조회
     * GET /api/staff/exam/courses/{courseId}/grades?userId={userId}
     */
    @GetMapping("/courses/{courseId}/grades")
    public ResponseEntity<?> getCourseGrades(
            @PathVariable String courseId,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        try {
            log.info("과정 성적 통계 조회 요청: courseId = {}", courseId);
            
            // 1. 쿠키에서 refresh 토큰 추출
            String refreshToken = extractRefreshToken(request);
            if (refreshToken == null) {
                return ResponseEntity.status(401).body("Refresh 토큰이 없습니다.");
            }
            
            // 2. refresh 토큰으로 사용자 이메일 조회
            String email = jwtUtil.getUserEmail(refreshToken);
            if (email == null) {
                return ResponseEntity.status(401).body("토큰에서 이메일을 추출할 수 없습니다.");
            }
            
            // 3. 과정 성적 통계 조회
            StaffCourseGradeDTO courseGrades = staffExamService.getCourseGrades(courseId);
            
            // 4. 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", courseGrades);
            response.put("message", "과정 성적 통계 조회 성공");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("과정 성적 통계 조회 실패 (잘못된 요청): {}", e.getMessage());
            return ResponseEntity.status(400).body("잘못된 요청: " + e.getMessage());
        } catch (Exception e) {
            log.error("과정 성적 통계 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }
    
    /**
     * 시험별 문제 통계 조회
     * GET /api/staff/exam/{templateId}/question-stats?userId={userId}
     */
    // @GetMapping("/{templateId}/question-stats")
    // public ResponseEntity<?> getExamQuestionStats(
    //         @PathVariable String templateId,
    //         @RequestParam(required = false) String userId,
    //         HttpServletRequest request) {
    //     try {
    //         log.info("시험별 문제 통계 조회 요청: templateId = {}", templateId);
            
    //         // 1. 쿠키에서 refresh 토큰 추출
    //         String refreshToken = extractRefreshToken(request);
    //         if (refreshToken == null) {
    //             return ResponseEntity.status(401).body("Refresh 토큰이 없습니다.");
    //         }
            
    //         // 2. refresh 토큰으로 사용자 이메일 조회
    //         String email = jwtUtil.getUserEmail(refreshToken);
    //         if (email == null) {
    //             return ResponseEntity.status(401).body("토큰에서 이메일을 추출할 수 없습니다.");
    //         }
            
    //         // 3. 시험별 문제 통계 조회
    //         QuestionDTO questionStats = staffExamService.getExamQuestionStats(templateId);
            
    //         // 4. 응답 데이터 구성
    //         Map<String, Object> response = new HashMap<>();
    //         response.put("success", true);
    //         response.put("data", questionStats);
    //         response.put("message", "문제별 통계 조회 성공");
            
    //         return ResponseEntity.ok(response);
            
    //     } catch (IllegalArgumentException e) {
    //         log.warn("시험별 문제 통계 조회 실패 (잘못된 요청): {}", e.getMessage());
    //         return ResponseEntity.status(400).body("잘못된 요청: " + e.getMessage());
    //     } catch (Exception e) {
    //         log.error("시험별 문제 통계 조회 실패: {}", e.getMessage(), e);
    //         return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
    //     }
    // }
    
    /**
     * 쿠키에서 refresh 토큰 추출
     * @param request HTTP 요청
     * @return refresh 토큰 또는 null
     */
    private String extractRefreshToken(HttpServletRequest request) {
        // 1. 쿠키에서 토큰 찾기
        Cookie[] cookies = request.getCookies();
        log.info("쿠키 추출 시작 - cookies: {}", (Object) cookies);
        
        if (cookies != null) {
            log.info("총 {}개의 쿠키 발견", cookies.length);
            for (Cookie cookie : cookies) {
                log.info("쿠키 이름: {}, 값: {}", cookie.getName(), cookie.getValue());
                if ("refresh".equals(cookie.getName())) {
                    log.info("refresh 쿠키 발견: {}", cookie.getValue());
                    return cookie.getValue();
                }
            }
        } else {
            log.warn("쿠키가 없습니다.");
        }
        
        // 2. Authorization 헤더에서 토큰 찾기
        String authHeader = request.getHeader("Authorization");
        log.info("Authorization 헤더: {}", authHeader);
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("Authorization 헤더에서 토큰 발견: {}", token);
            return token;
        }
        
        log.warn("refresh 토큰을 찾을 수 없습니다. (쿠키와 Authorization 헤더 모두 확인됨)");
        return null;
    }
}
