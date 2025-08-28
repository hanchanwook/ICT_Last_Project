package com.jakdang.labs.api.chanwook.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.jakdang.labs.api.chanwook.service.AttendanceService;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;

// 학생용 출석 관련 컨트롤러
@RestController
@RequestMapping("/api/attendance")
@Slf4j
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 학생 본인 출석 기록 조회 (학생용)
    @GetMapping("/my-attendances")
    public ResponseEntity<Map<String, Object>> getMyAttendances(@RequestParam Map<String, String> params) {
        log.info("=== 학생 본인 출석 기록 조회 요청 === params: {}", params);
        
        try {
            // JWT에서 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                log.error("인증되지 않은 사용자입니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "인증이 필요합니다.");
                return ResponseEntity.status(401).body(errorResponse);
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUserEntity().getId();
            
            log.info("JWT에서 추출한 사용자 ID: {}", userId);
            
            List<Map<String, Object>> attendances = attendanceService.getAttendancesByUserId(userId, params);
            Map<String, Object> response = new HashMap<>();
            response.put("attendances", attendances);
            response.put("totalCount", attendances.size());
            log.info("학생 본인 출석 기록 조회 성공: {} 건", attendances.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("학생 본인 출석 기록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // PC용 출석 제출
    @PostMapping("/submit-pc")
    public ResponseEntity<Map<String, Object>> submitPcAttendance(@RequestBody Map<String, Object> request) {
        log.info("=== PC용 출석 제출 요청 ===");
        log.info("요청 데이터: {}", request);

        try {
            // JWT에서 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                log.error("인증되지 않은 사용자입니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "인증이 필요합니다.");
                errorResponse.put("status", "AUTHENTICATION_REQUIRED");
                return ResponseEntity.status(401).body(errorResponse);
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUserEntity().getId();
            
            log.info("=== JWT 인증 정보 확인 ===");
            log.info("사용자 ID: {}", userId);
            log.info("사용자 이메일: {}", userDetails.getUserEntity().getEmail());

            if (request.get("courseId") == null || request.get("classroomId") == null) {
                log.error("과목 ID 또는 강의실 ID가 누락되었습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "과목 ID 또는 강의실 ID가 누락되었습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 출석 서비스 호출 (JWT에서 가져온 사용자 정보 사용)
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userId);
            userInfo.put("name", userDetails.getUserEntity().getName());
            userInfo.put("email", userDetails.getUserEntity().getEmail());
            userInfo.put("role", userDetails.getUserEntity().getRole().name());

            Map<String, Object> result = attendanceService.submitPcAttendance(request, userInfo);

            log.info("PC용 출석 제출 성공: 사용자 ID {}", userId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("PC용 출석 제출 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "출석 제출에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

     // PC용 퇴실 제출
      @PostMapping("/checkout-pc")
      public ResponseEntity<Map<String, Object>> submitPcCheckOut(@RequestBody Map<String, Object> request) {
          log.info("=== PC용 퇴실 제출 요청 ===");
          log.info("요청 데이터: {}", request);

          try {
              // JWT에서 사용자 정보 가져오기
              Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
              if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                  log.error("인증되지 않은 사용자입니다.");
                  Map<String, Object> errorResponse = new HashMap<>();
                  errorResponse.put("success", false);
                  errorResponse.put("message", "인증이 필요합니다.");
                  errorResponse.put("status", "AUTHENTICATION_REQUIRED");
                  return ResponseEntity.status(401).body(errorResponse);
              }

              CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
              String userId = userDetails.getUserEntity().getId();
              
              log.info("=== JWT 인증 정보 확인 ===");
              log.info("사용자 ID: {}", userId);
              log.info("사용자 이메일: {}", userDetails.getUserEntity().getEmail());

              // 퇴실 시간이 없으면 현재 시간으로 설정
              if (request.get("checkOutTime") == null) {
                  log.info("퇴실 시간이 없어 현재 시간으로 설정합니다.");
                  request.put("checkOutTime", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
              }

              // 퇴실 서비스 호출 (JWT에서 가져온 사용자 정보 사용)
              Map<String, Object> userInfo = new HashMap<>();
              userInfo.put("userId", userId);
              userInfo.put("name", userDetails.getUserEntity().getName());
              userInfo.put("email", userDetails.getUserEntity().getEmail());
              userInfo.put("role", userDetails.getUserEntity().getRole().name());

              Map<String, Object> result = attendanceService.submitPcCheckOut(request, userInfo);

              log.info("PC용 퇴실 제출 성공: 사용자 ID {}", userId);
              return ResponseEntity.ok(result);

          } catch (Exception e) {
              log.error("PC용 퇴실 제출 실패: {}", e.getMessage(), e);

              Map<String, Object> errorResponse = new HashMap<>();
              errorResponse.put("success", false);
              errorResponse.put("message", "퇴실 제출에 실패했습니다: " + e.getMessage());
              return ResponseEntity.badRequest().body(errorResponse);
          }
      }

    // PC용 오늘 출석 상태 조회 (과정별)
    @GetMapping("/status-pc/{userId}")
    public ResponseEntity<Map<String, Object>> getPcTodayAttendanceStatus(@RequestParam String courseId) {
        log.info("=== PC용 오늘 출석 상태 조회 요청 === courseId: {}", courseId);
        
        try {
            // JWT에서 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                log.error("인증되지 않은 사용자입니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "인증이 필요합니다.");
                return ResponseEntity.status(401).body(errorResponse);
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUserEntity().getId();
            
            log.info("JWT에서 추출한 사용자 ID: {}", userId);
            
            Map<String, Object> result = attendanceService.getPcTodayAttendanceStatus(userId, courseId);
            log.info("PC용 오늘 출석 상태 조회 성공: userId={}, action={}", userId, result.get("action"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("PC용 오늘 출석 상태 조회 실패: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "출석 상태 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    


    // 학생 수강 과목 및 강의실 정보 조회 (PC 출석용)
    @GetMapping("/courses-and-classrooms")
    public ResponseEntity<Map<String, Object>> getStudentCoursesAndClassrooms() {
        log.info("=== 학생 수강 과목 및 강의실 정보 조회 요청 ===");
        
        try {
            // JWT에서 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                log.error("인증되지 않은 사용자입니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "인증이 필요합니다.");
                return ResponseEntity.status(401).body(errorResponse);
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String studentId = userDetails.getUserEntity().getId();
            
            log.info("JWT에서 추출한 학생 ID: {}", studentId);
            
            Map<String, Object> result = attendanceService.getStudentCoursesAndClassrooms(studentId);
            log.info("학생 수강 과목 및 강의실 정보 조회 성공: studentId={}, courses={}", 
                    studentId, result.get("courses"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("학생 수강 과목 및 강의실 정보 조회 실패: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "수강 과목 및 강의실 정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
