package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.gemjjok.DTO.*;
import com.jakdang.labs.api.gemjjok.service.AssignmentService;
import com.jakdang.labs.api.gemjjok.service.CourseListService;
import com.jakdang.labs.api.gemjjok.service.RubricService;
import com.jakdang.labs.api.gemjjok.repository.AssignmentRepository;
import com.jakdang.labs.api.gemjjok.util.AssignmentUuidMapper;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/instructor")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
public class AssignmentController {
    
    private final AssignmentService assignmentService;
    private final CourseListService courseService;
    private final RubricService rubricService;
    private final AssignmentRepository assignmentRepository;
    private final com.jakdang.labs.api.gemjjok.service.AssignmentMaterialService assignmentMaterialService;
    
    // JWT 토큰에서 사용자 정보 추출
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.");
    }

    // 강사가 담당하는 과제 목록 조회
    @GetMapping("/assignments")
    public ResponseEntity<List<AssignmentListResponseDTO>> getInstructorAssignments() {
        CustomUserDetails currentUser = getCurrentUser();
        String memberId = currentUser.getUserId();
        List<AssignmentListResponseDTO> assignments = assignmentService.getAllAssignmentsByMemberId(memberId);
        return ResponseEntity.ok(assignments);
    }
    
    // 과제 통계 정보 조회 (구체적인 경로를 먼저 정의)
    @GetMapping("/assignments/stats")
    public ResponseEntity<AssignmentStatsResponseDTO> getAssignmentStats() {
        CustomUserDetails currentUser = getCurrentUser();
        String memberId = currentUser.getUserId();
        log.info("강사 {}의 과제 통계 조회", memberId);
        AssignmentStatsResponseDTO stats = assignmentService.getAssignmentStats(memberId);
        return ResponseEntity.ok(stats);
    }
    
    // 과제 등록 시 과정 선택을 위한 강사별 강의 목록 조회 (구체적인 경로를 먼저 정의)
    @GetMapping("/assignments/courses")
    public ResponseEntity<List<CourseListResponseDTO>> getInstructorCourses() {
        CustomUserDetails currentUser = getCurrentUser();
        String memberId = currentUser.getUserId();
        log.info("강사 {}의 강의 목록(과제 등록용, 활성 강의만) 조회", memberId);
        List<CourseListResponseDTO> courses = courseService.getActiveCoursesByMemberId(memberId);
        return ResponseEntity.ok(courses);
    }
    
    // 특정 강의의 학생 목록 조회 (과제용)
    @GetMapping("/assignments/courses/{courseId}/students")
    public ResponseEntity<?> getAssignmentCourseStudents(@PathVariable("courseId") String courseId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String instructorId = currentUser.getUserId();
            log.info("과제용 강의 {}의 학생 목록 조회 요청 - 강사 ID: {}", courseId, instructorId);
            
            Map<String, Object> result = assignmentService.getCourseStudents(courseId, instructorId);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("강의 학생 목록 조회 성공", result));
        } catch (Exception e) {
            log.error("강의 학생 목록 조회 실패: courseId={}, error={}", courseId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "학생 목록 조회 실패: " + e.getMessage()));
        }
    }
    

    
    // 과제별 제출 현황 조회 (구체적인 경로를 먼저 정의)
    @GetMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<?> getAssignmentSubmissions(@PathVariable("assignmentId") String assignmentId) {
        try {
            List<AssignmentSubmissionDTO> submissions = assignmentService.getAssignmentSubmissions(assignmentId);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("과제 제출 현황 조회 성공", submissions));
        } catch (Exception e) {
            System.err.println("제출 현황 조회 에러: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "제출 현황 조회 실패: " + e.getMessage()));
        }
    }
    
    // 과제 파일 업로드 (구체적인 경로를 먼저 정의)
    @PostMapping("/assignments/{assignmentId}/files")
    public ResponseEntity<?> uploadAssignmentFile(
            @PathVariable("assignmentId") String assignmentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileInfo") String fileInfoJson) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String userId = currentUser.getUserId();
            
            // 파일 정보 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> fileInfo = objectMapper.readValue(fileInfoJson, Map.class);
            fileInfo.put("uploadedBy", userId);
            
            // 파일 업로드 처리
            assignmentService.uploadAssignmentFile(assignmentId, file, fileInfo);
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("파일 업로드 성공", "파일이 성공적으로 업로드되었습니다."));
        } catch (Exception e) {
            log.error("파일 업로드 실패: ", e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "파일 업로드 실패: " + e.getMessage()));
        }
    }
    
    // 과제 파일 삭제 (구체적인 경로를 먼저 정의)
    @DeleteMapping("/assignments/{assignmentId}/files/{fileId}")
    public ResponseEntity<?> deleteAssignmentFile(@PathVariable("assignmentId") String assignmentId, @PathVariable("fileId") String fileId) {
        try {
            // AssignmentMaterialService를 사용하여 파일 삭제
            boolean deleted = assignmentMaterialService.deleteAssignmentMaterial(Long.parseLong(fileId));
            
            if (deleted) {
                return ResponseEntity.ok(ResponseDTO.createSuccessResponse("파일 삭제 성공", "파일이 성공적으로 삭제되었습니다."));
            } else {
                return ResponseEntity.status(404).body(ResponseDTO.createErrorResponse(404, "파일을 찾을 수 없습니다."));
            }
        } catch (Exception e) {
            log.error("파일 삭제 실패: ", e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "파일 삭제 실패: " + e.getMessage()));
        }
    }
    
    // 특정 과제 상세 정보 조회 (변수 경로를 나중에 정의)
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<?> getAssignmentDetail(@PathVariable("assignmentId") String assignmentId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String userId = currentUser.getUserId();
            AssignmentDetailResponseDTO detail = assignmentService.getAssignmentDetail(assignmentId, userId);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("과제 상세 조회 성공", detail));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ResponseDTO.createErrorResponse(404, "과제 상세 조회 실패: " + e.getMessage()));
        }
    }
    
    // 과제 등록 (JSON 지원)
    @PostMapping("/assignments")
    public ResponseEntity<?> createAssignment(@RequestBody AssignmentRequestDTO requestDTO) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String userId = currentUser.getUserId();
            requestDTO.setMemberId(userId);
            
            // 과제 정보를 assignment 테이블에 저장
            AssignmentDetailResponseDTO createdAssignment = assignmentService.createAssignment(requestDTO);
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("과제 등록 성공", createdAssignment));
        } catch (Exception e) {
            log.error("과제 등록 실패: ", e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "과제 등록 실패: " + e.getMessage()));
        }
    }
    
    // 과제 수정
    @PutMapping("/assignments/{assignmentId}")
    public ResponseEntity<?> updateAssignment(
            @PathVariable("assignmentId") String assignmentId,
            @RequestBody AssignmentRequestDTO requestDTO) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String userId = currentUser.getUserId();
            requestDTO.setMemberId(userId); // 사용자 ID 설정
            AssignmentDetailResponseDTO updatedAssignment = assignmentService.updateAssignment(assignmentId, requestDTO);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("과제 수정 성공", updatedAssignment));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "과제 수정 실패: " + e.getMessage()));
        }
    }
    
    // 과제 삭제
    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<?> deleteAssignment(@PathVariable("assignmentId") String assignmentId) {
        try {
            assignmentService.deleteAssignment(assignmentId);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("과제 삭제 성공", "과제가 성공적으로 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "과제 삭제 실패: " + e.getMessage()));
        }
    }
    
    // ===== 루브릭 관련 엔드포인트 =====
    
    // 과제별 루브릭 조회
    @GetMapping("/assignments/{assignmentId}/rubric")
    public ResponseEntity<?> getAssignmentRubric(@PathVariable("assignmentId") String assignmentId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String userId = currentUser.getUserId();
            RubricDTO rubric = rubricService.getRubricByAssignmentIdAndUserId(assignmentId, userId);
            if (rubric == null) {
                return ResponseEntity.status(404).body(ResponseDTO.createErrorResponse(404, "루브릭을 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("루브릭 조회 성공", rubric));
        } catch (Exception e) {
            System.err.println("루브릭 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "루브릭 조회 실패: " + e.getMessage()));
        }
    }
    
    // 과제별 루브릭 생성/수정
    @PostMapping("/assignments/{assignmentId}/rubric")
    public ResponseEntity<?> saveAssignmentRubric(
            @PathVariable("assignmentId") String assignmentId,
            @RequestBody RubricDTO rubricDTO) {
        try {
            // UUID 패턴 확인
            boolean isUuid = assignmentId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            
            String actualAssignmentId = assignmentId;
            
            if (isUuid) {
                // UUID인 경우, 실제 데이터베이스의 ID로 매핑
                actualAssignmentId = AssignmentUuidMapper.mapUuidToActualId(assignmentId, assignmentRepository);
            }
            
            RubricDTO savedRubric = rubricService.saveRubric(
                actualAssignmentId,
                rubricDTO.getRubricTitle() != null ? rubricDTO.getRubricTitle() : "채점 기준",
                rubricDTO.getRubricitem()
            );
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("루브릭 저장 성공", savedRubric));
        } catch (Exception e) {
            System.err.println("루브릭 저장 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "루브릭 저장 실패: " + e.getMessage()));
        }
    }
    
    // 과제별 루브릭 삭제
    @DeleteMapping("/assignments/{assignmentId}/rubric")
    public ResponseEntity<?> deleteAssignmentRubric(@PathVariable("assignmentId") String assignmentId) {
        try {
            // UUID 패턴 확인
            boolean isUuid = assignmentId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            
            String actualAssignmentId = assignmentId;
            
            if (isUuid) {
                // UUID인 경우, 실제 데이터베이스의 ID로 매핑
                actualAssignmentId = AssignmentUuidMapper.mapUuidToActualId(assignmentId, assignmentRepository);
            }
            
            rubricService.deleteRubric(actualAssignmentId);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("루브릭 삭제 성공", "루브릭이 성공적으로 삭제되었습니다."));
        } catch (Exception e) {
            System.err.println("루브릭 삭제 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "루브릭 삭제 실패: " + e.getMessage()));
        }
    }
    
    // 피드백/점수 수정 API (Map 방식)
    @PatchMapping("/assignmentsubmission/{submissionId}")
    public ResponseEntity<?> updateFeedbackAndScore(
            @PathVariable("submissionId") String submissionId,
            @RequestBody java.util.Map<String, Object> body) {
        try {
            String feedback = (String) body.get("feedback");
            Integer score = body.get("score") != null ? ((Number) body.get("score")).intValue() : null;
            assignmentService.updateSubmissionFeedbackAndScore(submissionId, feedback, score);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("피드백/점수 수정 성공", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "피드백/점수 수정 실패: " + e.getMessage()));
        }
    }

} 