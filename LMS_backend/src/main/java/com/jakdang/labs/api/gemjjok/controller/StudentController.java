package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.gemjjok.DTO.StudentCourseResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.AssignmentListResponseDTO;
import com.jakdang.labs.api.gemjjok.service.CourseListService;
import com.jakdang.labs.api.gemjjok.service.AssignmentService;
import com.jakdang.labs.api.gemjjok.service.RubricService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
public class StudentController {
    private final CourseListService courseService;
    private final AssignmentService assignmentService;
    private final RubricService rubricService;

    // JWT 토큰에서 사용자 정보 추출
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.");
    }

    // 학생이 수강하는 모든 강의 목록 조회
    @GetMapping("/courses")
    public ResponseEntity<List<StudentCourseResponseDTO>> getStudentCourses() {
        CustomUserDetails currentUser = getCurrentUser();
        String id = currentUser.getUserId();
        List<StudentCourseResponseDTO> courses = courseService.getStudentCourses(id);
        return ResponseEntity.ok(courses);
    }

    // 학생별 과제 목록 조회
    @GetMapping("/assignments")
    public ResponseEntity<?> getStudentAssignments() {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String id = currentUser.getUserId();
            log.info("학생 {}의 과제 목록 조회", id);
            List<AssignmentListResponseDTO> assignments = assignmentService.getAssignmentsByStudentId(id);
            return ResponseEntity.ok(
                ResponseDTO.createSuccessResponse("학생 과제 목록 조회 성공", assignments)
            );
        } catch (Exception e) {
            log.error("학생 과제 목록 조회 실패", e);
            return ResponseEntity.badRequest().body(
                ResponseDTO.createErrorResponse(500, "학생 과제 목록 조회 실패: " + e.getMessage())
            );
        }
    }
    
    // 학생용 과제 상세 조회
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<?> getStudentAssignmentDetail(@PathVariable("assignmentId") String assignmentId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String studentId = currentUser.getUserId();
            log.info("학생 {}의 과제 상세 조회: {}", studentId, assignmentId);
            var detail = assignmentService.getStudentAssignmentDetail(assignmentId, studentId);
            return ResponseEntity.ok(
                ResponseDTO.createSuccessResponse("학생 과제 상세 조회 성공", detail)
            );
        } catch (Exception e) {
            log.error("학생 과제 상세 조회 실패", e);
            return ResponseEntity.status(404).body(
                ResponseDTO.createErrorResponse(404, "학생 과제 상세 조회 실패: " + e.getMessage())
            );
        }
    }

    @PostMapping("/assignmentsubmission")
    public ResponseEntity<?> submitAssignment(
        @RequestParam("assignmentId") String assignmentId,
        @RequestParam(value = "answerText", required = false) String answerText,
        @RequestParam(value = "submissionType", required = false) String submissionType,
        @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String memberId = currentUser.getUserId();
            log.info("학생 {}의 과제 제출: {}", memberId, assignmentId);
            assignmentService.submitAssignment(assignmentId, memberId, answerText, submissionType, file);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("과제 제출 성공", null));
        } catch (Exception e) {
            log.error("과제 제출 실패", e);
            return ResponseEntity.badRequest().body(ResponseDTO.createErrorResponse(500, "과제 제출 실패: " + e.getMessage()));
        }
    }

    @PutMapping("/assignmentsubmission/{submissionId}")
    public ResponseEntity<?> updateAssignmentSubmission(
        @PathVariable("submissionId") String submissionId,
        @RequestParam(value = "answerText", required = false) String answerText,
        @RequestParam(value = "submissionType", required = false) String submissionType,
        @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {
            if (answerText == null && submissionType == null && (file == null || file.isEmpty())) {
                return ResponseEntity.badRequest().body(ResponseDTO.fail("수정할 내용이 없습니다."));
            }
            assignmentService.updateAssignmentSubmission(submissionId, answerText, submissionType, file);
            return ResponseEntity.ok(ResponseDTO.success("과제 제출 수정 성공", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ResponseDTO.error("과제 제출 수정 실패: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @DeleteMapping("/assignmentsubmission/{submissionId}")
    public ResponseEntity<?> deleteAssignmentSubmission(@PathVariable("submissionId") String submissionId) {
        try {
            assignmentService.deleteAssignmentSubmission(submissionId);
            return ResponseEntity.ok(ResponseDTO.success("과제 제출 삭제 성공", null));
        } catch (IllegalArgumentException e) {
            // 존재하지 않는 제출 내역 등 구체적 메시지, 404 반환
            System.err.println("[과제 제출 삭제 실패] " + e.getMessage());
            return ResponseEntity.status(404).body(ResponseDTO.fail("존재하지 않는 제출입니다: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("[과제 제출 삭제 실패] " + e.getMessage());
            return ResponseEntity.status(500).body(ResponseDTO.error("과제 제출 삭제 실패: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @GetMapping("/assignmentsubmission")
    public ResponseEntity<?> getMyAssignmentSubmission(@RequestParam(value = "assignmentId", required = false) String assignmentId) {
        CustomUserDetails currentUser = getCurrentUser();
        String userId = currentUser.getUserId();
        if (assignmentId == null) {
            var submissions = assignmentService.getSubmissionsByUserId(userId);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("내 전체 과제 제출 목록 조회 성공", submissions));
        }
        var submission = assignmentService.getSubmissionByUserIdAndAssignmentId(userId, assignmentId);
        if (submission == null) {
            return ResponseEntity.status(404).body(ResponseDTO.createErrorResponse(404, "제출 내역이 없습니다."));
        }
        return ResponseEntity.ok(ResponseDTO.createSuccessResponse("내 과제 제출 조회 성공", submission));
    }

    @GetMapping("/assignmentsubmission/all")
    public ResponseEntity<?> getAllAssignmentSubmissions() {
        var submissions = assignmentService.getAllAssignmentSubmissions();
        return ResponseEntity.ok(ResponseDTO.createSuccessResponse("전체 과제 제출 목록 조회 성공", submissions));
    }
    
    // 학생용 과제 루브릭 조회
    @GetMapping("/assignments/{assignmentId}/rubric")
    public ResponseEntity<?> getStudentAssignmentRubric(@PathVariable("assignmentId") String assignmentId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String studentId = currentUser.getUserId();
            log.info("학생 {}의 과제 루브릭 조회: {}", studentId, assignmentId);
            
            // 먼저 학생이 해당 과제에 접근할 권한이 있는지 확인
            var detail = assignmentService.getStudentAssignmentDetail(assignmentId, studentId);
            
            // 루브릭 정보 조회
            var rubric = rubricService.getRubricByAssignmentId(assignmentId);
            if (rubric == null) {
                return ResponseEntity.status(404).body(
                    ResponseDTO.createErrorResponse(404, "루브릭을 찾을 수 없습니다.")
                );
            }
            
            return ResponseEntity.ok(
                ResponseDTO.createSuccessResponse("학생 과제 루브릭 조회 성공", rubric)
            );
        } catch (Exception e) {
            log.error("학생 과제 루브릭 조회 실패", e);
            return ResponseEntity.status(404).body(
                ResponseDTO.createErrorResponse(404, "학생 과제 루브릭 조회 실패: " + e.getMessage())
            );
        }
    }
} 