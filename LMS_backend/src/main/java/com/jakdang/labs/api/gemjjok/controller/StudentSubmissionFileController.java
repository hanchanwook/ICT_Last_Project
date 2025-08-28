package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.StudentSubmissionFileDTO;
import com.jakdang.labs.api.gemjjok.service.StudentSubmissionFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/assignments")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
public class StudentSubmissionFileController {

    private final StudentSubmissionFileService studentSubmissionFileService;

    // JWT 토큰에서 사용자 정보 추출
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.");
    }

    /**
     * 학생 제출 파일 저장 (4단계: LMS 백엔드에 파일 정보 저장)
     * POST /api/student/assignments/{assignmentId}/submission-files
     */
    @PostMapping("/{assignmentId}/submission-files")
    public ResponseEntity<?> saveSubmissionFile(
            @PathVariable("assignmentId") String assignmentId,
            @RequestBody Map<String, Object> request) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String studentId = currentUser.getUserId();
            
            log.info("학생 제출 파일 저장 요청 - assignmentId: {}, studentId: {}", assignmentId, studentId);
            log.info("요청 데이터: {}", request);

            String fileKey = (String) request.get("fileKey");
            String fileName = (String) request.get("fileName");
            Object fileSizeObj = request.get("fileSize");
            String contentType = (String) request.get("contentType");
            String title = (String) request.get("title");

            if (fileKey == null || fileName == null) {
                return ResponseEntity.badRequest().body(ResponseDTO.createErrorResponse(400, "fileKey와 fileName은 필수입니다."));
            }

            // fileSize 처리 (Integer 또는 Long일 수 있음)
            Long fileSize = null;
            if (fileSizeObj != null) {
                if (fileSizeObj instanceof Integer) {
                    fileSize = ((Integer) fileSizeObj).longValue();
                } else if (fileSizeObj instanceof Long) {
                    fileSize = (Long) fileSizeObj;
                } else if (fileSizeObj instanceof String) {
                    fileSize = Long.parseLong((String) fileSizeObj);
                }
            }

            StudentSubmissionFileDTO savedFile = studentSubmissionFileService.saveStudentSubmissionFile(
                assignmentId, studentId, fileKey, fileName, fileSize, contentType, title);

            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("파일 정보 저장 성공", savedFile));

        } catch (Exception e) {
            log.error("학생 제출 파일 저장 실패 - assignmentId: {}, error: {}", assignmentId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "파일 정보 저장 실패: " + e.getMessage()));
        }
    }

    /**
     * 학생 제출 파일 목록 조회
     * GET /api/student/assignments/{assignmentId}/submission-files
     */
    @GetMapping("/{assignmentId}/submission-files")
    public ResponseEntity<?> getSubmissionFiles(
            @PathVariable("assignmentId") String assignmentId,
            @RequestParam(value = "submissionId", required = false) String submissionId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String studentId = currentUser.getUserId();
            
            log.info("학생 제출 파일 목록 조회 - assignmentId: {}, studentId: {}, submissionId: {}", 
                assignmentId, studentId, submissionId);

            List<StudentSubmissionFileDTO> files;
            
            if (submissionId != null && !submissionId.isEmpty()) {
                // submissionId가 있으면 해당 submission의 파일들만 조회
                files = studentSubmissionFileService.getStudentSubmissionFilesBySubmissionId(assignmentId, studentId, submissionId);
            } else {
                // submissionId가 없으면 기존 방식으로 조회
                files = studentSubmissionFileService.getStudentSubmissionFiles(assignmentId, studentId);
            }

            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("파일 목록 조회 성공", files));

        } catch (Exception e) {
            log.error("학생 제출 파일 목록 조회 실패 - assignmentId: {}, error: {}", assignmentId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "파일 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 학생 제출 파일 삭제
     * DELETE /api/student/assignments/{assignmentId}/submission-files/{fileId}
     */
    @DeleteMapping("/{assignmentId}/submission-files/{fileId}")
    public ResponseEntity<?> deleteSubmissionFile(
            @PathVariable("assignmentId") String assignmentId,
            @PathVariable("fileId") Long fileId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String studentId = currentUser.getUserId();
            
            log.info("학생 제출 파일 삭제 요청 - assignmentId: {}, studentId: {}, fileId: {}", 
                assignmentId, studentId, fileId);

            boolean deleted = studentSubmissionFileService.deleteStudentSubmissionFile(assignmentId, studentId, fileId);

            if (deleted) {
                return ResponseEntity.ok(ResponseDTO.createSuccessResponse("파일 삭제 성공", null));
            } else {
                return ResponseEntity.status(404).body(ResponseDTO.createErrorResponse(404, "파일을 찾을 수 없습니다."));
            }

        } catch (Exception e) {
            log.error("학생 제출 파일 삭제 실패 - assignmentId: {}, fileId: {}, error: {}", 
                assignmentId, fileId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "파일 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * 학생 제출 파일 다운로드
     * GET /api/student/assignments/{assignmentId}/submission-files/{fileId}/download
     */
    @GetMapping("/{assignmentId}/submission-files/{fileId}/download")
    public ResponseEntity<byte[]> downloadSubmissionFile(
            @PathVariable("assignmentId") String assignmentId,
            @PathVariable("fileId") Long fileId) {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String studentId = currentUser.getUserId();
            
            log.info("학생 제출 파일 다운로드 요청 - assignmentId: {}, studentId: {}, fileId: {}", 
                assignmentId, studentId, fileId);

            // 파일 정보 조회하여 fileKey 얻기
            List<StudentSubmissionFileDTO> files = studentSubmissionFileService.getStudentSubmissionFiles(assignmentId, studentId);
            StudentSubmissionFileDTO targetFile = files.stream()
                .filter(file -> file.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

            byte[] fileData = studentSubmissionFileService.downloadSubmissionFile(targetFile.getFileKey());
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + targetFile.getFileName() + "\"")
                .body(fileData);

        } catch (Exception e) {
            log.error("학생 제출 파일 다운로드 실패 - assignmentId: {}, fileId: {}, error: {}", 
                assignmentId, fileId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
} 