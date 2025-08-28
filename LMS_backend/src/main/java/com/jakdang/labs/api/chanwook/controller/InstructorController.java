package com.jakdang.labs.api.chanwook.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.jakdang.labs.api.chanwook.service.InstructorService;
import org.springframework.security.core.context.SecurityContextHolder;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;

// 강사용 과정 관리 컨트롤러
@RestController
@RequestMapping("/api/instructor")
@Slf4j
@RequiredArgsConstructor
public class InstructorController {

    private final InstructorService instructorService;



    // 강사가 담당하는 학생 목록 조회
    @GetMapping("/students")
    public ResponseEntity<Map<String, Object>> getInstructorStudents(@RequestParam Map<String, String> params, @RequestParam String userId) {
        log.info("=== 강사 담당 학생 목록 조회 요청 === params: {}, userId: {}", params, userId);
        try {
            Map<String, Object> response = instructorService.getInstructorStudents(userId, params);
            log.info("강사 담당 학생 목록 조회 성공: {} 건", ((List<?>) response.get("students")).size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("강사 담당 학생 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 학생 상세 정보 조회
    @GetMapping("/students/{userId}")
    public ResponseEntity<Map<String, Object>> getStudentDetail(@PathVariable String userId) {
        log.info("=== 학생 상세 정보 조회 요청 === userId: {}", userId);
        try {
            Map<String, Object> studentDetail = instructorService.getStudentDetail(userId);
            log.info("학생 상세 정보 조회 성공: {}", userId);
            return ResponseEntity.ok(studentDetail);
        } catch (RuntimeException e) {
            log.error("학생 상세 정보 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("학생 상세 정보 조회 중 예상치 못한 오류: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "서버 내부 오류가 발생했습니다");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 강사 대시보드 통계 조회
    @GetMapping("/dashboard/academic")
    public ResponseEntity<Map<String, Object>> getInstructorDashboard() {
        log.info("=== 강사 대시보드 통계 조회 요청 ===");
        try {
            // JWT에서 인증된 사용자 정보 가져오기
            CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String userId = userDetails.getUserEntity().getId();
            log.info("인증된 강사 ID: {}", userId);
            
            Map<String, Object> dashboard = instructorService.getInstructorDashboard(userId);
            log.info("강사 대시보드 통계 조회 성공");
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("강사 대시보드 통계 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 특정 학생 성적 조회
    @GetMapping("/students/grades")
    public ResponseEntity<Map<String, Object>> getStudentGrades(@RequestParam String userId, @RequestParam Map<String, String> params) {
        log.info("=== 특정 학생 성적 조회 요청 ===");
        log.info("   - userId: {}", userId);
        log.info("   - params: {}", params);
        log.info("   - courseId: {}", params.get("courseId"));
        log.info("   - educationId: {}", params.get("educationId"));
        
        try {
            Map<String, Object> grades = instructorService.getStudentGrades(userId, params);
            log.info("학생 성적 조회 성공: {}", userId);
            return ResponseEntity.ok(grades);
        } catch (RuntimeException e) {
            log.error("학생 성적 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("학생 성적 조회 중 예상치 못한 오류: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "서버 내부 오류가 발생했습니다");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}