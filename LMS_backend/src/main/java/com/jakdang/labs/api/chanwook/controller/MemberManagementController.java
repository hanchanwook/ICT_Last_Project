package com.jakdang.labs.api.chanwook.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.jakdang.labs.api.chanwook.service.MemberManagementService;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import java.util.ArrayList;

// 직원용 회원원 관리 컨트롤러

@RestController
@RequestMapping("/api/members")
@Slf4j
@RequiredArgsConstructor
public class MemberManagementController {

    private final MemberManagementService memberManagementService;

    // 현재 로그인한 사용자의 userId 가져오기
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    return userDetails.getUserId();
                }
            }
        } catch (Exception e) {
            log.error("현재 로그인한 사용자 정보 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    // 현재 로그인한 사용자의 역할 가져오기
    private String getCurrentUserRole() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    return userDetails.getUserEntity().getRole().toString();
                }
            }
        } catch (Exception e) {
            log.error("현재 로그인한 사용자 역할 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    // 직원 - 1 - 전체 회원 목록 조회
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMemberList(@RequestParam Map<String, String> params) {
        log.info("=== 전체 회원 목록 조회 요청 === params: {}", params);
        try {
            List<Map<String, Object>> members = memberManagementService.getAllMembers(params);
            
            // 역할별 통계 계산
            Map<String, Long> roleStats = members.stream()
                .collect(Collectors.groupingBy(
                    member -> (String) member.get("memberRole"),
                    Collectors.counting()
                ));
            
            Map<String, Object> response = new HashMap<>();
            response.put("members", members);
            response.put("totalCount", members.size());
            response.put("roleStats", roleStats);
            
            log.info("전체 회원 목록 조회 성공: {} 건 (학생: {}, 강사: {}, 직원: {})", 
                members.size(), 
                roleStats.getOrDefault("ROLE_STUDENT", 0L),
                roleStats.getOrDefault("ROLE_INSTRUCTOR", 0L),
                roleStats.getOrDefault("ROLE_STAFF", 0L));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("전체 회원 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 직원 - 2 - 특정 회원 상세 정보 조회
    @GetMapping("/detail/{userId}")
    public ResponseEntity<Map<String, Object>> getMemberDetail(
            @PathVariable String userId,
            @RequestParam(required = false) String currentUserId) {
        log.info("=== 특정 회원 상세 정보 조회 요청 === userId: {}, currentUserId: {}", userId, currentUserId);
        
        // undefined 체크 추가
        if (userId == null || userId.isEmpty() || "undefined".equals(userId)) {
            log.error("유효하지 않은 userId: {}", userId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "회원 ID가 제공되지 않았습니다. 올바른 회원 ID를 입력해주세요.");
            errorResponse.put("message", "userId parameter is required");
            errorResponse.put("userId", userId);
            errorResponse.put("currentUserId", currentUserId);
            errorResponse.put("status", "BAD_REQUEST");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            Map<String, Object> memberDetail = memberManagementService.getMemberDetail(userId);
            
            log.info("특정 회원 상세 정보 조회 성공: {}", userId);
            return ResponseEntity.ok(memberDetail);
        } catch (Exception e) {
            log.error("특정 회원 상세 정보 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "회원 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("userId", userId);
            errorResponse.put("currentUserId", currentUserId);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 직원 - 3 - 회원 정보 수정
    @PutMapping("/update/{userId}")
    public ResponseEntity<Map<String, Object>> updateMember(
            @PathVariable String userId,
            @RequestBody Map<String, Object> memberData) {
        log.info("=== 회원 정보 수정 요청 === userId: {}, data: {}", userId, memberData);
        try {
            Map<String, Object> updatedMember = memberManagementService.updateMember(userId, memberData);
            
            log.info("회원 정보 수정 성공: {}", userId);
            return ResponseEntity.ok(updatedMember);
        } catch (Exception e) {
            log.error("회원 정보 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            }
        }

    // 직원 - 4 - 학생의 추가 수강 신청 가능 과정 목록 조회
    @GetMapping("/checkcourse")
    public ResponseEntity<Map<String, Object>> getAvailableCourses(@RequestParam String userId) {
        log.info("=== 학생의 추가 수강 신청 가능 과정 목록 조회 요청 === userId: {}", userId);
        
        if (userId == null || userId.isEmpty() || "undefined".equals(userId)) {
            log.error("유효하지 않은 userId: {}", userId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사용자 ID가 제공되지 않았습니다.");
            errorResponse.put("userId", userId);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            Map<String, Object> availableCourses = memberManagementService.getAvailableCourses(userId);
            
            log.info("학생의 추가 수강 신청 가능 과정 목록 조회 성공: userId={}, 과정 수={}", 
                userId, availableCourses.get("courseCount"));
            return ResponseEntity.ok(availableCourses);
        } catch (Exception e) {
            log.error("학생의 추가 수강 신청 가능 과정 목록 조회 실패: {}", e.getMessage());
            
            // 에러가 발생해도 기본 응답 제공
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "수강 신청 가능 과정 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("userId", userId);
            errorResponse.put("availableCourses", new ArrayList<>());
            errorResponse.put("courseCount", 0);
            errorResponse.put("existingCourseCount", 0);
            errorResponse.put("educationId", null);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.ok(errorResponse); // 200 상태코드로 반환하여 프론트엔드에서 처리 가능하도록 함
        }
    }

    // 직원 - 5 - 학생의 추가 과정 신청
    @PostMapping("/addcourse")
    public ResponseEntity<Map<String, Object>> registerCourse(@RequestBody Object courseData) {
        log.info("=== 학생의 추가 과정 신청 요청 ===");
        log.info("전체 courseData 타입: {}", courseData.getClass().getSimpleName());
        log.info("전체 courseData 내용: {}", courseData);

        try {
            Map<String, Object> courseDataMap;

            // 배열인 경우 첫 번째 요소 사용
            if (courseData instanceof List) {
                List<?> courseDataList = (List<?>) courseData;
                log.info("배열 크기: {}", courseDataList.size());
                if (courseDataList.isEmpty()) {
                    log.error("과정 데이터가 비어있습니다.");
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "과정 데이터가 비어있습니다.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                // 배열의 모든 요소를 로그로 출력
                log.info("=== 배열의 모든 요소 분석 ===");
                for (int i = 0; i < courseDataList.size(); i++) {
                    Object element = courseDataList.get(i);
                    log.info("배열 요소 [{}]: {}", i, element);
                    if (element instanceof Map) {
                        Map<String, Object> elementMap = (Map<String, Object>) element;
                        log.info("  요소 [{}] 상세 분석:", i);
                        for (Map.Entry<String, Object> entry : elementMap.entrySet()) {
                            log.info("    키: '{}', 값: '{}' (타입: {})", 
                                entry.getKey(), 
                                entry.getValue(), 
                                entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
                        }
                    }
                }
                
                courseDataMap = (Map<String, Object>) courseDataList.get(0);
                log.info("배열에서 첫 번째 요소 추출: {}", courseDataMap);
            } else if (courseData instanceof Map) {
                courseDataMap = (Map<String, Object>) courseData;
                log.info("Map 형태로 받음: {}", courseDataMap);
            } else {
                log.error("지원하지 않는 데이터 형식입니다: {}", courseData.getClass());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "지원하지 않는 데이터 형식입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // courseDataMap의 모든 키와 값을 상세히 로그
            log.info("=== courseDataMap 상세 분석 ===");
            for (Map.Entry<String, Object> entry : courseDataMap.entrySet()) {
                log.info("키: '{}', 값: '{}' (타입: {})", 
                    entry.getKey(), 
                    entry.getValue(), 
                    entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
            }

            // 필수 파라미터 검증
            String targetUserId = (String) courseDataMap.get("userId");
            String courseId = (String) courseDataMap.get("courseId");
            
            if (targetUserId == null || targetUserId.isEmpty()) {
                log.error("userId가 누락되었습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "사용자 ID가 누락되었습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (courseId == null || courseId.isEmpty()) {
                log.error("courseId가 누락되었습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "과정 ID가 누락되었습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 현재 로그인한 사용자 정보 가져오기
            String currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                log.error("현재 로그인한 사용자 정보를 가져올 수 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "인증 정보를 확인할 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("과정 신청 권한 검증: 현재 사용자={}, 대상 사용자={}", currentUserId, targetUserId);
            
            // 권한 검증: 직원이 학생의 과정을 대신 신청할 수 있도록 허용
            // 1. 같은 사용자인 경우 허용
            // 2. 직원이 학생의 과정을 대신 신청하는 경우 허용
            if (!currentUserId.equals(targetUserId)) {
                // 현재 사용자의 역할 확인
                String currentUserRole = getCurrentUserRole();
                log.info("현재 사용자 역할: {}", currentUserRole);
                
                // 직원 역할인 경우에만 다른 사용자의 과정 신청 허용
                if (currentUserRole == null || 
                    (!currentUserRole.equals("ROLE_DIRECTOR") && 
                     !currentUserRole.equals("ROLE_STAFF") && 
                     !currentUserRole.equals("ROLE_INSTRUCTOR"))) {
                    log.warn("권한 없음: 현재 사용자({}, 역할: {})가 다른 사용자({})의 과정을 신청하려 함", 
                            currentUserId, currentUserRole, targetUserId);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "다른 사용자의 과정을 신청할 권한이 없습니다.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                log.info("직원 권한으로 학생 과정 신청 허용: 현재 사용자({}, 역할: {}), 대상 사용자({})", 
                        currentUserId, currentUserRole, targetUserId);
            }
            
            Map<String, Object> result = memberManagementService.registerCourse(courseDataMap);
            
            log.info("학생의 추가 과정 신청 성공: userId={}, courseId={}", targetUserId, courseId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("학생의 추가 과정 신청 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "과정 신청 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

}
