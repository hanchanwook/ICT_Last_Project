package com.jakdang.labs.api.lnuyasha.controller;

import com.jakdang.labs.api.lnuyasha.dto.*;
import com.jakdang.labs.api.lnuyasha.service.LectureHistoryService;
import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/instructor/lectures")
public class LectureHistoryController {
    
    private final LectureHistoryService lectureHistoryService;
    private final JwtUtil jwtUtil;
    
    public LectureHistoryController(LectureHistoryService lectureHistoryService, JwtUtil jwtUtil) {
        this.lectureHistoryService = lectureHistoryService;
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * 강의 이력 목록 조회 API
     * GET /api/instructor/lectures/history
     */
    @GetMapping("/history")
    public ResponseEntity<ResponseDTO<List<LectureHistoryDTO>>> getLecturesHistory(HttpServletRequest request) {
        try {
            // JWT 토큰에서 강사 ID 추출 (실제 구현에서는 JWT 유틸리티 사용)
            String instructorId = extractInstructorIdFromRequest(request);
            
            List<LectureHistoryDTO> lectures = lectureHistoryService.getLecturesHistory(instructorId);
            
            return ResponseEntity.ok(ResponseDTO.<List<LectureHistoryDTO>>builder()
                    .resultCode(200)
                    .resultMessage("성공")
                    .data(lectures)
                    .build());
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.<List<LectureHistoryDTO>>builder()
                    .resultCode(400)
                    .resultMessage("강의 이력 조회 실패: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * 필터링된 강의 목록 조회 API
     * GET /api/instructor/lectures/history?search={searchTerm}&status={status}
     */
    @GetMapping("/history/filter")
    public ResponseEntity<ResponseDTO<List<LectureHistoryDTO>>> getFilteredLectures(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        try {
            String instructorId = extractInstructorIdFromRequest(request);
            
            List<LectureHistoryDTO> lectures = lectureHistoryService.getFilteredLectures(instructorId, search, status);
            
            return ResponseEntity.ok(ResponseDTO.<List<LectureHistoryDTO>>builder()
                    .resultCode(200)
                    .resultMessage("성공")
                    .data(lectures)
                    .build());
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.<List<LectureHistoryDTO>>builder()
                    .resultCode(400)
                    .resultMessage("필터링된 강의 목록 조회 실패: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * 전체 통계 조회 API
     * GET /api/instructor/lectures/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ResponseDTO<LectureStatisticsDTO>> getLecturesStatistics(HttpServletRequest request) {
        try {
            String instructorId = extractInstructorIdFromRequest(request);
            
            LectureStatisticsDTO statistics = lectureHistoryService.getLecturesStatistics(instructorId);
            
            return ResponseEntity.ok(ResponseDTO.<LectureStatisticsDTO>builder()
                    .resultCode(200)
                    .resultMessage("성공")
                    .data(statistics)
                    .build());
                    
        } catch (Exception e) {
            log.error("강의 통계 조회 실패", e);
            return ResponseEntity.badRequest().body(ResponseDTO.<LectureStatisticsDTO>builder()
                    .resultCode(400)
                    .resultMessage("강의 통계 조회 실패: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * 개별 강의 상세 정보 조회 API
     * GET /api/instructor/lectures/{lectureId}/detail
     */
    @GetMapping("/{lectureId}/detail")
    public ResponseEntity<ResponseDTO<LectureDetailDTO>> getLectureDetail(
            @PathVariable String lectureId,
            HttpServletRequest request) {
        try {
            String instructorId = extractInstructorIdFromRequest(request);
            
            LectureDetailDTO lectureDetail = lectureHistoryService.getLectureDetail(lectureId, instructorId);
            
            return ResponseEntity.ok(ResponseDTO.<LectureDetailDTO>builder()
                    .resultCode(200)
                    .resultMessage("성공")
                    .data(lectureDetail)
                    .build());
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.<LectureDetailDTO>builder()
                    .resultCode(400)
                    .resultMessage("강의 상세 정보 조회 실패: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ResponseDTO.<LectureDetailDTO>builder()
                    .resultCode(500)
                    .resultMessage("강의 상세 정보 조회 실패: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * 시험 강의 상세 정보 조회 API (새로운 요구사항)
     * GET /api/instructor/lectures/{lectureId}/exam-detail
     */
    // @GetMapping("/{lectureId}/exam-detail")
    // public ResponseEntity<ResponseDTO<ExamLectureDetailDTO>> getExamLectureDetail(
    //         @PathVariable String lectureId,
    //         HttpServletRequest request) {
    //     try {
    //         String instructorId = extractInstructorIdFromRequest(request);
            
    //         ExamLectureDetailDTO lectureDetail = lectureHistoryService.getExamLectureDetail(lectureId, instructorId);
            
    //         return ResponseEntity.ok(ResponseDTO.<ExamLectureDetailDTO>builder()
    //                 .resultCode(200)
    //                 .resultMessage("성공")
    //                 .data(lectureDetail)
    //                 .build());
                    
    //     } catch (IllegalArgumentException e) {
    //         return ResponseEntity.badRequest().body(ResponseDTO.<ExamLectureDetailDTO>builder()
    //                 .resultCode(400)
    //                 .resultMessage("시험 강의 상세 정보 조회 실패: " + e.getMessage())
    //                 .build());
    //     } catch (Exception e) {
    //         return ResponseEntity.internalServerError().body(ResponseDTO.<ExamLectureDetailDTO>builder()
    //                 .resultCode(500)
    //                 .resultMessage("시험 강의 상세 정보 조회 실패: " + e.getMessage())
    //                 .build());
    //     }
    // }
    
    /**
     * JWT 토큰에서 강사 ID 추출 (lnuyasha 전용 - 간단한 버전)
     */
    private String extractInstructorIdFromRequest(HttpServletRequest request) {
        
        try {
            // Authorization 헤더에서 토큰 추출
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!token.isEmpty() && !token.equals("undefined")) {
                    return jwtUtil.getUserId(token);
                }
            }
            
            // 쿠키에서 토큰 추출
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
            
            // SecurityContext에서 사용자 정보 추출 시도
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
            }
            
        } catch (Exception e) {
        }
        
        // lnuyasha 전용: 모든 방법 실패 시 기본 강사 ID 사용
        return "error";
    }
} 