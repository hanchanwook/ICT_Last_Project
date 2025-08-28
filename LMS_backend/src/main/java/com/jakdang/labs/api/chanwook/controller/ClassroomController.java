package com.jakdang.labs.api.chanwook.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.multipart.MultipartFile;

import com.jakdang.labs.api.chanwook.service.ClassroomService;
import com.jakdang.labs.api.chanwook.DTO.ClassroomDTO;
import com.jakdang.labs.api.common.EducationId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// 직원용 강의실 관리 컨트롤러
@RestController
@RequestMapping("/api/classroom")
@Slf4j
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;
    private final EducationId educationId;

    // 직원 - 1 - 모든 교실 조회
    @GetMapping("/all")
    public ResponseEntity<List<ClassroomDTO>> getAllClassrooms(@RequestParam Map<String, String> params) {
        log.info("=== 교실 목록 조회 요청 === params: {}", params);
        try {
            // userId 파라미터 추출
            String userId = params.get("userId");
            if (userId == null || userId.isEmpty()) {
                log.error("userId 파라미터가 누락되었습니다.");
                return ResponseEntity.badRequest().build();
            }
            
            // EducationId 컴포넌트를 사용하여 사용자 정보 검증
            var educationInfo = educationId.findById(userId);
            if (educationInfo.isEmpty()) {
                log.error("사용자 ID에 해당하는 교육기관 정보를 찾을 수 없음: {}", userId);
                return ResponseEntity.badRequest().build();
            }
            
            // 검증된 educationId를 params에 추가
            String verifiedEducationId = educationInfo.get().getEducationId();
            params.put("educationId", verifiedEducationId);
            log.info("검증된 educationId를 params에 추가: {}", verifiedEducationId);
            
            List<ClassroomDTO> classrooms = classroomService.getAllClassrooms(params);
            log.info("교실 목록 조회 성공: {} 건", classrooms.size());
            return ResponseEntity.ok(classrooms);
        } catch (Exception e) {
            log.error("교실 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 직원 - 2 - 교실 생성
    @PostMapping("/create")
    public ResponseEntity<ClassroomDTO> createClassroom(@RequestBody ClassroomDTO classroomDTO) {
        log.info("=== 교실 생성 요청 ===");
        log.info("교실 데이터: {}", classroomDTO);
        
        try {
            ClassroomDTO createdClassroom = classroomService.createClassroom(classroomDTO);
            log.info("교실 생성 성공: {}", createdClassroom.getClassId());
            return ResponseEntity.ok(createdClassroom);
        } catch (Exception e) {
            log.error("교실 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 직원 - 2-1 - EducationId 조회 API 
    @GetMapping("/education-id")
    public ResponseEntity<Map<String, Object>> getEducationId(@RequestParam(required = false) String memberId) {
        log.info("=== EducationId 조회 요청 ===");
        
        // JWT에서 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = null;
        
        if (authentication != null && authentication.getPrincipal() instanceof com.jakdang.labs.api.auth.dto.CustomUserDetails) {
            com.jakdang.labs.api.auth.dto.CustomUserDetails userDetails = 
                (com.jakdang.labs.api.auth.dto.CustomUserDetails) authentication.getPrincipal();
            currentUserId = userDetails.getUserEntity().getId();
            log.info("JWT에서 추출한 사용자 ID: {}", currentUserId);
        }
        
        // memberId 파라미터가 없으면 JWT의 사용자 ID 사용
        if (memberId == null || memberId.isEmpty()) {
            if (currentUserId != null) {
                memberId = currentUserId;
                log.info("파라미터 memberId가 없어서 JWT의 사용자 ID 사용: {}", memberId);
            } else {
                log.error("memberId 파라미터와 JWT 사용자 정보가 모두 없습니다.");
                            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사용자 정보를 찾을 수 없습니다.");
            errorResponse.put("memberId", null);
            errorResponse.put("educationId", null);
            errorResponse.put("educationName", null);
                return ResponseEntity.badRequest().body(errorResponse);
            }
        }
        
        log.info("최종 사용할 memberId: {}", memberId);
        
        try {
            String educationIdValue = educationId.getEducationIdByUserIdOrNull(memberId);
            String educationName = null;
            
            // educationName 조회
            if (educationIdValue != null) {
                educationName = educationId.getEducationNameByEducationId(educationIdValue);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("memberId", memberId);
            response.put("educationId", educationIdValue);
            response.put("educationName", educationName);
            
            if (educationIdValue != null) {
                log.info("EducationId 조회 성공: memberId={}, educationId={}, educationName={}", memberId, educationIdValue, educationName);
                response.put("message", "EducationId 조회 성공");
            } else {
                log.warn("EducationId를 찾을 수 없음: memberId={}", memberId);
                response.put("message", "해당 memberId의 EducationId를 찾을 수 없습니다");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("EducationId 조회 실패: memberId={}, error={}", memberId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "EducationId 조회 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("memberId", memberId);
            errorResponse.put("educationId", null);
            errorResponse.put("educationName", null);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 직원 - 3 - 교실 수정 
    @PutMapping("/update/{classId}")
    public ResponseEntity<ClassroomDTO> updateClassroomWithPath(@PathVariable String classId, @RequestBody ClassroomDTO classroomDTO, @RequestParam(required = false) String userId) {
        log.info("=== 교실 수정 요청 (update 경로) ===");
        log.info("교실 ID: {}, userId: {}, 수정 데이터: {}", classId, userId, classroomDTO);
        
        try {
            // userId가 제공된 경우에만 검증
            if (userId != null && !userId.isEmpty()) {
                // EducationId 컴포넌트를 사용하여 사용자 정보 검증
                var educationInfo = educationId.findById(userId);
                if (educationInfo.isEmpty()) {
                    log.error("사용자 ID에 해당하는 교육기관 정보를 찾을 수 없음: {}", userId);
                    return ResponseEntity.badRequest().build();
                }
            }
            
            ClassroomDTO updatedClassroom = classroomService.updateClassroom(classId, classroomDTO);
            log.info("교실 수정 성공: {}", updatedClassroom.getClassId());
            return ResponseEntity.ok(updatedClassroom);
        } catch (Exception e) {
            log.error("교실 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 직원 - 4 - 교실 상세 조회
    @GetMapping("/{classId}")
    public ResponseEntity<ClassroomDTO> getClassroomDetail(@PathVariable String classId, @RequestParam String userId) {
        log.info("=== 교실 상세 조회 요청 ===");
        log.info("교실 ID: {}, userId: {}", classId, userId);
        
        try {
            // EducationId 컴포넌트를 사용하여 사용자 정보 검증
            var educationInfo = educationId.findById(userId);
            if (educationInfo.isEmpty()) {
                log.error("사용자 ID에 해당하는 교육기관 정보를 찾을 수 없음: {}", userId);
                return ResponseEntity.badRequest().build();
            }
            
            // 검증된 educationId 추출
            String verifiedEducationId = educationInfo.get().getEducationId();
            log.info("검증된 educationId: {}", verifiedEducationId);
            
            // 교실 조회 및 권한 검증
            ClassroomDTO classroom = classroomService.getClassroomById(classId);
            
            // 해당 교실이 사용자의 교육기관에 속하는지 확인
            if (!verifiedEducationId.equals(classroom.getEducationId())) {
                log.error("해당 교실에 대한 접근 권한이 없습니다. 요청 educationId: {}, 교실 educationId: {}", 
                         verifiedEducationId, classroom.getEducationId());
                return ResponseEntity.badRequest().build();
            }
            log.info("교실 상세 조회 성공: {}", classId);
            return ResponseEntity.ok(classroom);
        } catch (Exception e) {
            log.error("교실 상세 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 직원 - 5 - 교실 삭제(강의실 삭제)
    @DeleteMapping("/delete/{classId}")
    public ResponseEntity<Void> deleteClassroom(@PathVariable String classId) {
        log.info("=== 교실 삭제 요청 ===");
        log.info("교실 ID: {}", classId);
        
        try {
            classroomService.deleteClassroom(classId);
            log.info("교실 삭제 성공: {}", classId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("교실 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 직원 - 6 - 강의실 파일 업로드 (CSV/Excel 통합)
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadClassroomsFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "educationId", required = false) String educationId) {
        log.info("=== 강의실 파일 업로드 요청 ===");
        log.info("파일명: {}, 크기: {} bytes, educationId: {}", 
                file.getOriginalFilename(), file.getSize(), educationId);
        
        if (file == null || file.isEmpty()) {
            log.error("파일이 제공되지 않았습니다.");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "파일이 제공되지 않았습니다.");
            errorResponse.put("successCount", 0);
            errorResponse.put("errorCount", 1);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            String fileName = file.getOriginalFilename();
            String fileExtension = fileName != null ? 
                fileName.toLowerCase().substring(fileName.lastIndexOf(".") + 1) : "";
            
            Map<String, Object> result;
            
            if ("csv".equals(fileExtension)) {
                log.info("CSV 파일로 처리합니다.");
                result = classroomService.processCSVFile(file, educationId);
            } else if ("xlsx".equals(fileExtension) || "xls".equals(fileExtension)) {
                log.info("Excel 파일로 처리합니다.");
                result = classroomService.processExcelFile(file, educationId);
            } else {
                log.error("지원하지 않는 파일 형식입니다: {}", fileExtension);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "지원하지 않는 파일 형식입니다. CSV 또는 Excel 파일을 업로드해주세요.");
                errorResponse.put("successCount", 0);
                errorResponse.put("errorCount", 1);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("강의실 파일 업로드 성공: 성공={}건, 실패={}건", 
                    result.get("successCount"), result.get("errorCount"));
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("강의실 파일 업로드 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("successCount", 0);
            errorResponse.put("errorCount", 1);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
