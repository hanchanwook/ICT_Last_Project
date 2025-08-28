package com.jakdang.labs.api.chanwook.controller;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import com.jakdang.labs.api.chanwook.DTO.AttendanceDTO;
import com.jakdang.labs.api.chanwook.service.AttendanceManagementService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.HashMap;

import java.util.List;

// 직원용 출석 기록 관리 컨트롤러  
@RestController
@RequestMapping("/api/attendance/management")
@RequiredArgsConstructor
@Slf4j
public class AttendanceManagementController {

    private final AttendanceManagementService attendanceManagementService;



    // 9. 출석 기록 수정 (courseId 기반 구분)
    @PutMapping("/{attendanceId}")
    public ResponseEntity<AttendanceDTO> updateAttendance(
            @PathVariable String attendanceId,
            @RequestParam(required = false) String courseId,
            @RequestBody AttendanceDTO attendanceDTO) {
        log.info("=== 출석 기록 수정 요청 === attendanceId: {}, courseId: {}, data: {}", attendanceId, courseId, attendanceDTO);
        try {
            // courseId 우선순위: RequestParam > RequestBody
            String finalCourseId = courseId;
            if (finalCourseId == null || finalCourseId.trim().isEmpty() || "undefined".equals(finalCourseId)) {
                // RequestParam에 courseId가 없으면 RequestBody에서 가져오기
                finalCourseId = attendanceDTO.getCourseId();
                log.info("RequestParam에서 courseId를 찾을 수 없어 RequestBody에서 가져옴: {}", finalCourseId);
            }
            
            // courseId가 여전히 null이거나 비어있는 경우 에러 처리
            if (finalCourseId == null || finalCourseId.trim().isEmpty() || "undefined".equals(finalCourseId)) {
                log.error("courseId가 제공되지 않았습니다. 출석 기록 수정을 위해서는 courseId가 필요합니다.");
                return ResponseEntity.badRequest().build();
            }
            
            // courseId를 DTO에 설정
            attendanceDTO.setCourseId(finalCourseId);
            log.info("최종 courseId 설정: {}", finalCourseId);
            
            // 서비스에서 강화된 검증 로직 사용 (courseId만 검증)
            AttendanceDTO updatedAttendance = attendanceManagementService.updateAttendanceWithValidation(attendanceId, null, finalCourseId, attendanceDTO);
            log.info("출석 기록 수정 성공: attendanceId={}, courseId={}", attendanceId, finalCourseId);
            return ResponseEntity.ok(updatedAttendance);
        } catch (RuntimeException e) {
            log.error("출석 기록 수정 실패 (NotFound): {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("출석 기록 수정 실패 (BadRequest): {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 10. 전체 학생 출석 기록 조회 (직원)
    @GetMapping("/all")
    public ResponseEntity<List<AttendanceDTO>> getAllAttendances(@RequestParam Map<String, String> params) {
        log.info("=== 전체 학생 출석 기록 조회 요청 === params: {}", params);
        try {
            List<AttendanceDTO> attendances = attendanceManagementService.getAllAttendances(params);
            log.info("전체 학생 출석 기록 조회 성공: {} 건", attendances.size());
            return ResponseEntity.ok(attendances);
        } catch (Exception e) {
            log.error("전체 학생 출석 기록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 11. 학생 출석 기록 상세 조회 (courseId 기반 구분)
    @GetMapping("/students/{userId}")
    public ResponseEntity<Map<String, Object>> getStudentAttendance(
            @PathVariable String userId,
            @RequestParam Map<String, String> params) {
        log.info("=== 학생 출석 기록 조회 요청 === userId: {}, params: {}", userId, params);
        try {
            // undefined 체크
            if (userId == null || userId.isEmpty() || "undefined".equals(userId) || "[object Object]".equals(userId)) {
                log.error("유효하지 않은 userId: {}", userId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "유효하지 않은 학생 ID입니다.");
                errorResponse.put("userId", userId);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // courseId 파라미터 추출
            String courseId = params.get("courseId");
            log.info("추출된 courseId: {}", courseId);
            
            List<AttendanceDTO> attendances;
            
            if (courseId != null && !courseId.trim().isEmpty() && !"undefined".equals(courseId)) {
                // courseId가 있으면 특정 과정의 출석 기록만 조회
                log.info("특정 과정 출석 기록 조회: userId={}, courseId={}", userId, courseId);
                attendances = attendanceManagementService.getAttendancesByUserIdAndCourseId(userId, courseId);
            } else {
                // courseId가 없으면 전체 출석 기록 조회 (과정별로 구분됨)
                log.info("전체 출석 기록 조회: userId={}", userId);
                attendances = attendanceManagementService.getAttendancesByUserId(userId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("attendances", attendances);
            response.put("totalCount", attendances.size());
            response.put("userId", userId);
            response.put("courseId", courseId);
            
            log.info("학생 출석 기록 조회 성공: {} 건", attendances.size());
            log.info("프론트엔드로 전송할 데이터: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("학생 출석 기록 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "학생 출석 기록 조회 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("userId", userId);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
