package com.jakdang.labs.api.lnuyasha.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.lnuyasha.service.MyCourseService;
import com.jakdang.labs.api.lnuyasha.dto.CourseDTO;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.security.jwt.utils.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class MyCourseController {
    
    private final MyCourseService courseService;
    private final JwtUtil jwtUtil;
    
    /**
     * 내 과정 목록 조회
     * GET /api/courses/my?userId={userId}
     */
    @GetMapping("/my")
    public ResponseDTO<List<CourseDTO>> getMyCourses(
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        try {
            log.info("내 과정 목록 조회 요청 - userId: {}", userId);
            
            String email = null;
            
            // 1. userId 파라미터가 있으면 우선 사용
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("userId 파라미터로 이메일 조회 시도: {}", userId);
                // userId로 이메일을 조회하는 로직이 필요할 수 있지만, 
                // 현재는 JWT 토큰에서 이메일을 추출하는 방식으로 진행
            }
            
            // 2. JWT 토큰에서 이메일 추출
            if (email == null) {
                try {
                    email = extractEmailFromToken(request);
                    log.info("JWT 토큰에서 추출한 email: {}", email);
                } catch (Exception e) {
                    log.error("JWT 토큰에서 이메일 추출 실패: {}", e.getMessage());
                    return ResponseDTO.createErrorResponse(401, "인증 토큰이 유효하지 않습니다.");
                }
            }
            
            // 3. 내 과정 목록 조회
            List<CourseDTO> courses = courseService.getMyCourses(email);
            
            return ResponseDTO.<List<CourseDTO>>builder()
                .resultCode("200")
                .resultMessage("내 과정 목록 조회가 완료되었습니다.")
                .data(courses)
                .build();
                
        } catch (Exception e) {
            log.error("내 과정 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseDTO.<List<CourseDTO>>builder()
                .resultCode("500")
                .resultMessage("내 과정 목록 조회 중 오류가 발생했습니다: " + e.getMessage())
                .data(List.of())
                .build();
        }
    }
    
    /**
     * 디버깅용: subgroup 테이블 데이터 확인
     */
    @GetMapping("/debug/subgroups")
    public ResponseDTO<List<Object>> getSubGroups() {
        try {
            List<Object> subGroups = courseService.getAllSubGroups();
            return ResponseDTO.<List<Object>>builder()
                .resultCode("SUCCESS")
                .resultMessage("subGroup 데이터 조회 성공")
                .data(subGroups)
                .build();
        } catch (Exception e) {
            return ResponseDTO.<List<Object>>builder()
                .resultCode("ERROR")
                .resultMessage("subGroup 데이터 조회 실패: " + e.getMessage())
                .data(List.of())
                .build();
        }
    }
    
    /**
     * JWT 토큰에서 이메일 추출
     */
    private String extractEmailFromToken(HttpServletRequest request) {
        try {
            // Authorization 헤더에서 토큰 추출
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!token.isEmpty() && !token.equals("undefined")) {
                    return jwtUtil.getUserEmail(token);
                }
            }
            
            // 쿠키에서 토큰 추출
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName()) || "access_token".equals(cookie.getName()) || "refresh".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.isEmpty() && !token.equals("undefined")) {
                            return jwtUtil.getUserEmail(token);
                        }
                    }
                }
            }
            
            throw new RuntimeException("JWT token not found in Authorization header or Cookie");
        } catch (Exception e) {
            throw new RuntimeException("JWT token not found in Authorization header or Cookie");
        }
    }
} 