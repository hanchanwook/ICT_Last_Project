package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.api.file.FileServiceClient;
import com.jakdang.labs.api.file.dto.RequestFileDTO;
import com.jakdang.labs.api.gemjjok.DTO.StudentSubmissionFileDTO;
import com.jakdang.labs.api.gemjjok.entity.StudentSubmissionFile;
import com.jakdang.labs.api.gemjjok.repository.StudentSubmissionFileRepository;
import com.jakdang.labs.entity.AssignmentSubmissionFile;
import com.jakdang.labs.api.gemjjok.repository.AssignmentSubmissionFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentSubmissionFileService {

    private final StudentSubmissionFileRepository studentSubmissionFileRepository;
    private final AssignmentSubmissionFileRepository assignmentSubmissionFileRepository;
    private final FileServiceClient fileServiceClient;

    /**
     * 학생 제출 파일 저장
     */
    @Transactional
    public StudentSubmissionFileDTO saveStudentSubmissionFile(String assignmentId, String studentId, 
                                                              String fileKey, String fileName, 
                                                              Long fileSize, String contentType, 
                                                              String title) {
        try {
            // log.info("학생 제출 파일 저장 시작 - assignmentId: {}, studentId: {}, fileKey: {}", 
            //     assignmentId, studentId, fileKey);

            StudentSubmissionFile submissionFile = StudentSubmissionFile.builder()
                .assignmentId(assignmentId)
                .studentId(studentId)
                .fileKey(fileKey)
                .fileName(fileName)
                .fileSize(fileSize)
                .contentType(contentType)
                .title(title)
                .materialId(fileKey) // fileKey를 materialId로도 사용
                .isActive(1)
                .uploadedBy(studentId)
                .build();

            StudentSubmissionFile savedFile = studentSubmissionFileRepository.save(submissionFile);
            // log.info("학생 제출 파일 저장 완료 - id: {}", savedFile.getId());

            return convertToDTO(savedFile);
        } catch (Exception e) {
            // log.error("학생 제출 파일 저장 실패 - assignmentId: {}, studentId: {}, error: {}", 
            //     assignmentId, studentId, e.getMessage(), e);
            throw new RuntimeException("파일 정보 저장에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 특정 과제의 특정 학생 제출 파일 목록 조회
     */
    public List<StudentSubmissionFileDTO> getStudentSubmissionFiles(String assignmentId, String studentId) {
        try {
            // log.info("학생 제출 파일 목록 조회 - assignmentId: {}, studentId: {}", assignmentId, studentId);
            
            List<StudentSubmissionFile> files = studentSubmissionFileRepository
                .findByAssignmentIdAndStudentIdAndIsActive(assignmentId, studentId, 1);
            
            // log.info("조회된 학생 제출 파일 수: {}", files.size());
            
            return files.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // log.error("학생 제출 파일 목록 조회 실패 - assignmentId: {}, studentId: {}, error: {}", 
            //     assignmentId, studentId, e.getMessage(), e);
            throw new RuntimeException("파일 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * submissionId로 학생 제출 파일 목록 조회
     */
    public List<StudentSubmissionFileDTO> getStudentSubmissionFilesBySubmissionId(String assignmentId, String studentId, String submissionId) {
        try {
            log.info("submissionId로 학생 제출 파일 목록 조회 - assignmentId: {}, studentId: {}, submissionId: {}", 
                assignmentId, studentId, submissionId);
            
            // assignment_submission_file 테이블에서 해당 submissionId의 파일들을 조회
            List<AssignmentSubmissionFile> files = assignmentSubmissionFileRepository
                .findByAssignmentIdAndStudentIdAndSubmissionId(assignmentId, studentId, submissionId);
            
            log.info("조회된 assignment_submission_file 수: {}", files.size());
            
            return files.stream()
                .map(this::convertAssignmentSubmissionFileToDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("submissionId로 학생 제출 파일 목록 조회 실패 - assignmentId: {}, studentId: {}, submissionId: {}, error: {}", 
                assignmentId, studentId, submissionId, e.getMessage(), e);
            throw new RuntimeException("파일 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 학생 제출 파일 삭제
     */
    @Transactional
    public boolean deleteStudentSubmissionFile(String assignmentId, String studentId, Long fileId) {
        try {
            // log.info("학생 제출 파일 삭제 시작 - assignmentId: {}, studentId: {}, fileId: {}", 
            //     assignmentId, studentId, fileId);

            StudentSubmissionFile submissionFile = studentSubmissionFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

            // 권한 확인 (본인의 파일인지 확인)
            if (!submissionFile.getStudentId().equals(studentId) || 
                !submissionFile.getAssignmentId().equals(assignmentId)) {
                throw new RuntimeException("파일 삭제 권한이 없습니다.");
            }

            // 1. 파일 서비스에서 실제 파일 삭제
            try {
                // log.info("파일 서비스에서 파일 삭제 시도 - fileKey: {}", submissionFile.getFileKey());
                fileServiceClient.deleteFile(submissionFile.getFileKey());
                // log.info("파일 서비스에서 파일 삭제 완료");
            } catch (Exception e) {
                // log.warn("파일 서비스에서 파일 삭제 실패 (계속 진행): {}", e.getMessage());
            }

            // 2. 데이터베이스에서 논리적 삭제 (isActive = 0)
            submissionFile.setIsActive(0);
            studentSubmissionFileRepository.save(submissionFile);

            // log.info("학생 제출 파일 삭제 완료 - fileId: {}", fileId);
            return true;

        } catch (Exception e) {
            // log.error("학생 제출 파일 삭제 실패 - assignmentId: {}, studentId: {}, fileId: {}, error: {}", 
            //     assignmentId, studentId, fileId, e.getMessage(), e);
            throw new RuntimeException("파일 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 다운로드
     */
    public byte[] downloadSubmissionFile(String fileKey) {
        try {
            // log.info("학생 제출 파일 다운로드 - fileKey: {}", fileKey);
            
            RequestFileDTO requestFileDTO = RequestFileDTO.builder()
                .key(fileKey)
                .build();
            
            byte[] fileData = fileServiceClient.downloadFile(requestFileDTO);
            // log.info("파일 다운로드 완료 - fileKey: {}, size: {} bytes", fileKey, fileData.length);
            
            return fileData;
        } catch (Exception e) {
            // log.error("파일 다운로드 실패 - fileKey: {}, error: {}", fileKey, e.getMessage(), e);
            throw new RuntimeException("파일 다운로드에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * Entity를 DTO로 변환
     */
    private StudentSubmissionFileDTO convertToDTO(StudentSubmissionFile entity) {
        return StudentSubmissionFileDTO.builder()
            .id(entity.getId())
            .assignmentId(entity.getAssignmentId())
            .studentId(entity.getStudentId())
            .fileKey(entity.getFileKey())
            .fileName(entity.getFileName())
            .fileSize(entity.getFileSize())
            .contentType(entity.getContentType())
            .title(entity.getTitle())
            .materialId(entity.getMaterialId())
            .isActive(entity.getIsActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .uploadedBy(entity.getUploadedBy())
            .build();
    }

    /**
     * AssignmentSubmissionFile Entity를 StudentSubmissionFileDTO로 변환
     */
    private StudentSubmissionFileDTO convertAssignmentSubmissionFileToDTO(AssignmentSubmissionFile entity) {
        return StudentSubmissionFileDTO.builder()
            .id((long) entity.getId().hashCode()) // String ID를 hashCode로 변환하여 Long으로 매핑
            .assignmentId(entity.getAssignmentId())
            .studentId(entity.getStudentId())
            .fileKey(entity.getFileKey())
            .fileName(entity.getFileName())
            .fileSize(entity.getFileSize())
            .contentType(entity.getFileType()) // fileType을 contentType으로 매핑
            .title(entity.getFileName()) // fileName을 title로 사용
            .materialId(entity.getFileKey()) // fileKey를 materialId로 사용
            .isActive(entity.getIsActive() ? 1 : 0) // Boolean을 Integer로 변환
            .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
            .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
            .uploadedBy(entity.getStudentId()) // studentId를 uploadedBy로 사용
            .build();
    }
} 