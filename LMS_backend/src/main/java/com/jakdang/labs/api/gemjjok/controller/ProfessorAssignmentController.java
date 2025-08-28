package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.AssignmentSubmissionFileDTO;
import com.jakdang.labs.api.gemjjok.service.AssignmentSubmissionFileService;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/instructor/assignments"})
@Slf4j
public class ProfessorAssignmentController {

    private final AssignmentSubmissionFileService assignmentSubmissionFileService;

    /**
     * 강사용 - 과제의 모든 학생 제출 파일 목록 조회
     * GET /api/professor/assignments/{assignmentId}/submission-files
     */
    @GetMapping("/{assignmentId}/submission-files")
    public ResponseEntity<?> getAssignmentSubmissionFiles(@PathVariable("assignmentId") String assignmentId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String professorId = currentUser.getUserId();
            
            log.info("강사 과제 제출 파일 목록 조회 - assignmentId: {}, professorId: {}", assignmentId, professorId);

            // TODO: 여기서 강사가 해당 과제에 대한 권한이 있는지 확인하는 로직 추가 필요
            // 예: 과제를 생성한 강사인지, 해당 과제를 담당하는 강사인지 등

            List<AssignmentSubmissionFileDTO> files = assignmentSubmissionFileService.getSubmissionFilesByAssignment(assignmentId);

            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("과제 제출 파일 목록 조회 성공", files));

        } catch (Exception e) {
            log.error("강사 과제 제출 파일 목록 조회 실패 - assignmentId: {}, error: {}", assignmentId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "파일 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 강사용 - 특정 학생의 과제 제출 파일 목록 조회
     * GET /api/professor/assignments/{assignmentId}/students/{studentId}/submission-files
     */
    @GetMapping("/{assignmentId}/students/{studentId}/submission-files")
    public ResponseEntity<?> getStudentSubmissionFiles(
            @PathVariable("assignmentId") String assignmentId,
            @PathVariable("studentId") String studentId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String professorId = currentUser.getUserId();
            
            log.info("강사 학생별 과제 제출 파일 목록 조회 - assignmentId: {}, studentId: {}, professorId: {}", 
                assignmentId, studentId, professorId);

            // TODO: 여기서 강사가 해당 과제와 학생에 대한 권한이 있는지 확인하는 로직 추가 필요

            List<AssignmentSubmissionFileDTO> files = assignmentSubmissionFileService.getSubmissionFilesByAssignmentAndStudent(assignmentId, studentId);

            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("학생별 과제 제출 파일 목록 조회 성공", files));

        } catch (Exception e) {
            log.error("강사 학생별 과제 제출 파일 목록 조회 실패 - assignmentId: {}, studentId: {}, error: {}", 
                assignmentId, studentId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "파일 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 강사용 - 과제 제출 파일 다운로드
     * GET /api/professor/assignments/submission-files/{fileId}/download
     */
    @GetMapping("/submission-files/{fileId}/download")
    public ResponseEntity<?> downloadSubmissionFile(@PathVariable("fileId") String fileId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String professorId = currentUser.getUserId();
            
            log.info("강사 과제 제출 파일 다운로드 - fileId: {}, professorId: {}", fileId, professorId);

            // TODO: 여기서 강사가 해당 파일에 대한 권한이 있는지 확인하는 로직 추가 필요

            byte[] fileData = assignmentSubmissionFileService.downloadSubmissionFile(fileId);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"submission_file\"")
                    .body(fileData);

        } catch (Exception e) {
            log.error("강사 과제 제출 파일 다운로드 실패 - fileId: {}, error: {}", fileId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "파일 다운로드 실패: " + e.getMessage()));
        }
    }

    /**
     * 현재 로그인한 사용자 정보 가져오기
     */
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
    }
} 