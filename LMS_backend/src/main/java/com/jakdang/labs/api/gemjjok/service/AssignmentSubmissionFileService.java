package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.api.file.dto.RequestFileDTO;
import com.jakdang.labs.api.file.FileServiceClient;
import com.jakdang.labs.api.gemjjok.DTO.AssignmentSubmissionFileDTO;
import com.jakdang.labs.api.gemjjok.repository.AssignmentSubmissionFileRepository;
import com.jakdang.labs.entity.AssignmentSubmissionFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentSubmissionFileService {

    private final AssignmentSubmissionFileRepository assignmentSubmissionFileRepository;
    private final FileServiceClient fileServiceClient;

    /**
     * 과제 제출 파일 저장 (submissionId 없이)
     */
    public AssignmentSubmissionFileDTO saveSubmissionFile(
            String submissionId,
            String assignmentId,
            String courseId,
            String studentId,
            String externalFileId,
            String fileKey,
            String fileName,
            Long fileSize,
            String fileType) {

        log.info("과제 제출 파일 저장 - submissionId: {}, assignmentId: {}, studentId: {}, externalFileId: {}", 
                submissionId, assignmentId, studentId, externalFileId);

        AssignmentSubmissionFile file = AssignmentSubmissionFile.builder()
                .id(externalFileId)
                .submissionId(submissionId)
                .assignmentId(assignmentId)
                .courseId(courseId)
                .studentId(studentId)
                .fileKey(fileKey)
                .fileName(fileName)
                .fileSize(fileSize)
                .fileType(fileType)
                .isActive(true)
                .build();

        AssignmentSubmissionFile savedFile = assignmentSubmissionFileRepository.save(file);
        log.info("과제 제출 파일 저장 완료 - id: {}", savedFile.getId());

        return convertToDTO(savedFile);
    }

    /**
     * 과제 제출 시 submissionId 업데이트
     */
    @Transactional
    public void updateSubmissionId(String assignmentId, String studentId, String submissionId) {
        log.info("과제 제출 파일 submissionId 업데이트 - assignmentId: {}, studentId: {}, submissionId: {}", 
                assignmentId, studentId, submissionId);

        try {
            // 해당 과제와 학생의 submissionId가 null인 파일들을 찾아서 업데이트
            List<AssignmentSubmissionFile> files = assignmentSubmissionFileRepository
                .findByAssignmentIdAndStudentIdAndSubmissionIdIsNull(assignmentId, studentId);
            
            for (AssignmentSubmissionFile file : files) {
                file.setSubmissionId(submissionId);
                assignmentSubmissionFileRepository.save(file);
                log.info("파일 submissionId 업데이트 완료 - fileId: {}, fileName: {}", 
                        file.getId(), file.getFileName());
            }
            
            log.info("과제 제출 파일 submissionId 업데이트 완료 - 총 {}개 파일", files.size());
            
        } catch (Exception e) {
            log.error("과제 제출 파일 submissionId 업데이트 실패 - error: {}", e.getMessage(), e);
            throw new RuntimeException("submissionId 업데이트에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 과제 ID와 학생 ID로 제출 파일 목록 조회 (학생용)
     */
    public List<AssignmentSubmissionFileDTO> getSubmissionFilesByAssignmentAndStudent(String assignmentId, String studentId) {
        log.info("과제 제출 파일 목록 조회 (학생용) - assignmentId: {}, studentId: {}", assignmentId, studentId);

        List<AssignmentSubmissionFile> files = assignmentSubmissionFileRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
        
        return files.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 과제 ID로 모든 학생의 제출 파일 목록 조회 (강사용)
     */
    public List<AssignmentSubmissionFileDTO> getSubmissionFilesByAssignment(String assignmentId) {
        log.info("과제 제출 파일 목록 조회 (강사용) - assignmentId: {}", assignmentId);

        List<AssignmentSubmissionFile> files = assignmentSubmissionFileRepository.findByAssignmentId(assignmentId);
        
        return files.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 제출 ID로 파일 목록 조회
     */
    public List<AssignmentSubmissionFileDTO> getSubmissionFilesBySubmissionId(String submissionId) {
        log.info("제출 ID로 파일 목록 조회 - submissionId: {}", submissionId);

        List<AssignmentSubmissionFile> files = assignmentSubmissionFileRepository.findBySubmissionId(submissionId);
        
        return files.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 파일 다운로드
     */
    public byte[] downloadSubmissionFile(String fileId) {
        log.info("과제 제출 파일 다운로드 - fileId: {}", fileId);

        try {
            // 1. DB에서 파일 정보 조회
            Optional<AssignmentSubmissionFile> fileOpt = assignmentSubmissionFileRepository.findById(fileId);
            if (!fileOpt.isPresent()) {
                log.warn("파일을 찾을 수 없습니다 - fileId: {}", fileId);
                throw new RuntimeException("파일을 찾을 수 없습니다.");
            }

            AssignmentSubmissionFile file = fileOpt.get();
            log.info("파일 정보 조회 성공 - fileKey: {}, fileName: {}", file.getFileKey(), file.getFileName());

            // 2. 외부 파일 서비스에서 파일 다운로드
            RequestFileDTO requestFileDTO = RequestFileDTO.builder()
                    .key(file.getFileKey())
                    .build();

            byte[] fileData = fileServiceClient.downloadFile(requestFileDTO);
            log.info("파일 다운로드 완료 - fileId: {}, size: {} bytes", fileId, fileData.length);

            return fileData;

        } catch (Exception e) {
            log.error("파일 다운로드 실패 - fileId: {}, error: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("파일 다운로드에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 삭제
     */
    public void deleteSubmissionFile(String fileId) {
        log.info("과제 제출 파일 삭제 - fileId: {}", fileId);

        try {
            // 1. DB에서 파일 정보 조회
            Optional<AssignmentSubmissionFile> fileOpt = assignmentSubmissionFileRepository.findById(fileId);
            if (!fileOpt.isPresent()) {
                log.warn("삭제할 파일을 찾을 수 없습니다 - fileId: {}", fileId);
                throw new RuntimeException("파일을 찾을 수 없습니다.");
            }

            AssignmentSubmissionFile file = fileOpt.get();
            log.info("파일 정보 조회 성공 - fileKey: {}, fileName: {}", file.getFileKey(), file.getFileName());

            // 2. 외부 파일 서비스에서 파일 삭제
            try {
                fileServiceClient.deleteFile(fileId);
                log.info("외부 파일 서비스에서 파일 삭제 완료 - fileId: {}", fileId);
            } catch (Exception e) {
                log.warn("외부 파일 서비스에서 파일 삭제 실패 - fileId: {}, error: {}", fileId, e.getMessage());
                // 외부 파일 삭제 실패 시에도 메타데이터는 삭제 진행
            }

            // 3. DB에서 메타데이터 삭제
            assignmentSubmissionFileRepository.delete(file);
            log.info("DB에서 파일 메타데이터 삭제 완료 - fileId: {}", fileId);

        } catch (Exception e) {
            log.error("파일 삭제 실패 - fileId: {}, error: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("파일 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * Entity를 DTO로 변환
     */
    private AssignmentSubmissionFileDTO convertToDTO(AssignmentSubmissionFile entity) {
        return AssignmentSubmissionFileDTO.builder()
                .id(entity.getId())
                .submissionId(entity.getSubmissionId())
                .assignmentId(entity.getAssignmentId())
                .courseId(entity.getCourseId())
                .studentId(entity.getStudentId())
                .fileKey(entity.getFileKey())
                .fileName(entity.getFileName())
                .fileSize(entity.getFileSize())
                .fileType(entity.getFileType())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }
} 