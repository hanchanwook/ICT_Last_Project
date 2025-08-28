package com.jakdang.labs.api.chanwook.controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.jakdang.labs.api.chanwook.service.QrCodeService;
import java.util.HashMap;
    
// 강사용 QR 출석 관리 컨트롤러

@RestController
@RequestMapping("/api/instructor/qr")
@RequiredArgsConstructor
@Slf4j
public class QrCodeController {
    
    private final QrCodeService qrCodeService;

    // 강사별 강의실 목록 조회 (강사가 담당하는 과정의 강의실만 조회)
    @GetMapping("/classrooms/{userId}")
    public ResponseEntity<Map<String, Object>> getInstructorClassrooms(@PathVariable String userId) {
        log.info("=== 강사별 강의실 목록 조회 요청 === userId: {}", userId);
        
        try {
            Map<String, Object> response = qrCodeService.getInstructorClassrooms(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("강사별 강의실 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 과정 정보 불러오기
    @GetMapping("/courses")
    public ResponseEntity<Map<String, Object>> getAllCourses() {
        log.info("=== 과정 전체 목록 조회 요청 ===");
        try {
            Map<String, Object> response = qrCodeService.getAllCourses();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("과정 전체 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // QR 출석 세션 생성
    @PostMapping("/session")
    public ResponseEntity<Map<String, Object>> createQRSession(@RequestBody Map<String, Object> sessionData) {
        log.info("=== QR 출석 세션 생성 요청 === data: {}", sessionData);
        try {
            // 실제 QR 출석 세션 생성 로직 구현
            Map<String, Object> response = qrCodeService.createQRSession(sessionData);
            
            log.info("QR 출석 세션 생성 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("QR 출석 세션 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // QR 출석 세션 조회
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getQRSession(@PathVariable String sessionId) {
        log.info("=== QR 출석 세션 조회 요청 === sessionId: {}", sessionId);
        try {
            // 실제 QR 출석 세션 조회 로직 구현
            Map<String, Object> response = qrCodeService.getQRSession(sessionId);
            
            log.info("QR 출석 세션 조회 성공: {}", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("QR 출석 세션 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // QR 출석 세션 종료
    @PutMapping("/session/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endQRSession(@PathVariable String sessionId) {
        log.info("=== QR 출석 세션 종료 요청 === sessionId: {}", sessionId);
        try {
            // 실제 QR 출석 세션 종료 로직 구현
            Map<String, Object> response = qrCodeService.endQRSession(sessionId);
            
            log.info("QR 출석 세션 종료 성공: {}", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("QR 출석 세션 종료 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 특정 강의실의 QR 출석 현황 조회 (classroomId 기반) - 강사 권한 확인 포함
    @GetMapping("/classroom/{classroomId}/attendance")
    public ResponseEntity<Map<String, Object>> getClassroomQRAttendance(
            @PathVariable String classroomId, 
            @RequestParam Map<String, String> params) {
        long startTime = System.currentTimeMillis();
        log.info("=== 특정 강의실 QR 출석 현황 조회 요청 === classroomId: {}, params: {}", classroomId, params);
        try {
            // userId가 params에 없으면 에러 응답
            if (!params.containsKey("userId") || params.get("userId").trim().isEmpty()) {
                log.error("강사 권한 확인을 위해 userId가 필요합니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "강사 권한 확인을 위해 userId가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 실제 특정 강의실 QR 출석 현황 조회 로직 구현 (권한 확인 포함)
            Map<String, Object> response = qrCodeService.getClassroomQRAttendance(classroomId, params);
            
            long endTime = System.currentTimeMillis();
            log.info("특정 강의실 QR 출석 현황 조회 성공: {} (응답시간: {}ms)", classroomId, (endTime - startTime));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("특정 강의실 QR 출석 현황 조회 실패: {} (응답시간: {}ms)", e.getMessage(), (endTime - startTime));
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
