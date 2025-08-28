package com.jakdang.labs.api.lnuyasha.controller;

import com.jakdang.labs.api.lnuyasha.dto.ExamDTO;
import com.jakdang.labs.api.lnuyasha.service.CourseExamService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/staff/courses")
@RequiredArgsConstructor
public class CourseExamController {

    private final CourseExamService courseExamService;
    private final JwtUtil jwtUtil;

    /**
     * 과정별 시험 목록 조회 API
     * GET /api/staff/courses/{courseId}/exams
     */
    @GetMapping("/{courseId}/exams")
    public ResponseEntity<ResponseDTO<List<ExamDTO>>> getCourseExams(
            @PathVariable String courseId,
            HttpServletRequest request) {
        
        try {
            // JWT 토큰에서 강사 ID 추출 (검증용)
            String instructorId = extractInstructorIdFromRequest(request);
            List<ExamDTO> exams = courseExamService.getCourseExams(courseId);
            
            return ResponseEntity.ok(ResponseDTO.<List<ExamDTO>>builder()
                    .resultCode("200")
                    .resultMessage("성공")
                    .data(exams)
                    .build());
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.<List<ExamDTO>>builder()
                    .resultCode("400")
                    .resultMessage("과정별 시험 목록 조회 실패: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ResponseDTO.<List<ExamDTO>>builder()
                    .resultCode("500")
                    .resultMessage("과정별 시험 목록 조회 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * JWT 토큰에서 강사 ID 추출
     */
    private String extractInstructorIdFromRequest(HttpServletRequest request) {
        log.info("=== 강사 시험 목록 JWT 토큰 추출 시작 ===");

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!token.isEmpty() && !token.equals("undefined")) {
                    return jwtUtil.getUserId(token);
                }
            }

            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName()) || "access_token".equals(cookie.getName()) || "refresh".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.isEmpty() && !token.equals("undefined")) {
                            return jwtUtil.getUserId(token);
                        }
                    }
                }
            }

            try {
                org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    if (authentication.getPrincipal() instanceof com.jakdang.labs.api.auth.dto.CustomUserDetails) {
                        com.jakdang.labs.api.auth.dto.CustomUserDetails userDetails =
                            (com.jakdang.labs.api.auth.dto.CustomUserDetails) authentication.getPrincipal();
                        String userId = userDetails.getUserId();
                        return userId;
                    }
                }
            } catch (Exception e) {
                log.error("SecurityContext에서 사용자 정보 추출 실패: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("강사 시험 목록: JWT 토큰에서 강사 ID 추출 중 오류: {}", e.getMessage(), e);
        }

        throw new IllegalArgumentException("JWT 토큰을 찾을 수 없습니다.");
    }
} 