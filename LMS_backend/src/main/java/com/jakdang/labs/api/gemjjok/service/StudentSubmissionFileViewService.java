package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.api.gemjjok.DTO.StudentSubmissionFileViewDTO;
import com.jakdang.labs.api.gemjjok.entity.StudentSubmissionFile;
import com.jakdang.labs.api.gemjjok.repository.StudentSubmissionFileViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudentSubmissionFileViewService {

    private final StudentSubmissionFileViewRepository studentSubmissionFileViewRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 과제 상세 모달용 - 학생 제출 파일 목록 조회
     */
    public List<StudentSubmissionFileViewDTO> getSubmissionFilesForDetail(String assignmentId, String studentId) {
        log.info("과제 상세 모달 - 학생 제출 파일 목록 조회 - assignmentId: {}, studentId: {}", assignmentId, studentId);
        
        List<StudentSubmissionFile> files = studentSubmissionFileViewRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
        
        return files.stream()
            .map(this::convertToViewDTO)
            .collect(Collectors.toList());
    }

    /**
     * 과제 상세 모달용 - 특정 제출 파일 상세 정보 조회
     */
    public Optional<StudentSubmissionFileViewDTO> getSubmissionFileDetail(String assignmentId, String studentId, Long fileId) {
        log.info("과제 상세 모달 - 제출 파일 상세 정보 조회 - assignmentId: {}, studentId: {}, fileId: {}", assignmentId, studentId, fileId);
        
        Optional<StudentSubmissionFile> file = studentSubmissionFileViewRepository.findByIdAndActive(fileId);
        
        if (file.isPresent()) {
            StudentSubmissionFile submissionFile = file.get();
            // 권한 확인: 해당 학생의 파일인지 확인
            if (!submissionFile.getAssignmentId().equals(assignmentId) || !submissionFile.getStudentId().equals(studentId)) {
                log.warn("권한 없음 - 파일 접근 시도: assignmentId={}, studentId={}, fileId={}", assignmentId, studentId, fileId);
                return Optional.empty();
            }
            return Optional.of(convertToViewDTO(submissionFile));
        }
        
        return Optional.empty();
    }

    /**
     * 과제 상세 모달용 - 제출 파일 통계 정보 조회
     */
    public Map<String, Object> getSubmissionFileStats(String assignmentId, String studentId) {
        log.info("과제 상세 모달 - 제출 파일 통계 조회 - assignmentId: {}, studentId: {}", assignmentId, studentId);
        
        Long totalFiles = studentSubmissionFileViewRepository.countByAssignmentIdAndStudentId(assignmentId, studentId);
        Long totalSize = studentSubmissionFileViewRepository.getTotalFileSizeByAssignmentIdAndStudentId(assignmentId, studentId);
        List<Object[]> fileTypeCounts = studentSubmissionFileViewRepository.getFileTypeCountByAssignmentIdAndStudentId(assignmentId, studentId);
        
        // 파일 타입별 개수를 Map으로 변환
        Map<String, Integer> fileTypeCountMap = fileTypeCounts.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> ((Number) row[1]).intValue()
            ));
        
        // 파일 타입 목록
        List<String> fileTypes = fileTypeCounts.stream()
            .map(row -> (String) row[0])
            .collect(Collectors.toList());
        
        return Map.of(
            "assignmentId", assignmentId,
            "studentId", studentId,
            "totalFiles", totalFiles.intValue(),
            "totalSize", totalSize != null ? totalSize : 0L,
            "totalSizeFormatted", formatFileSize(totalSize != null ? totalSize : 0L),
            "fileTypes", fileTypes,
            "fileTypeCount", fileTypeCountMap
        );
    }

    /**
     * 과제 상세 모달용 - 최근 제출 파일 조회
     */
    public List<StudentSubmissionFileViewDTO> getRecentSubmissionFiles(String assignmentId, String studentId) {
        log.info("과제 상세 모달 - 최근 제출 파일 조회 - assignmentId: {}, studentId: {}", assignmentId, studentId);
        
        List<StudentSubmissionFile> files = studentSubmissionFileViewRepository.findRecentFilesByAssignmentIdAndStudentId(assignmentId, studentId);
        
        return files.stream()
            .map(this::convertToViewDTO)
            .collect(Collectors.toList());
    }

    /**
     * 파일 키로 제출 파일 조회
     */
    public Optional<StudentSubmissionFileViewDTO> getSubmissionFileByFileKey(String fileKey) {
        log.info("파일 키로 제출 파일 조회 - fileKey: {}", fileKey);
        
        Optional<StudentSubmissionFile> file = studentSubmissionFileViewRepository.findByFileKey(fileKey);
        
        return file.map(this::convertToViewDTO);
    }

    /**
     * 파일 다운로드를 위한 파일 데이터 조회
     */
    public byte[] downloadSubmissionFile(String fileKey) {
        log.info("제출 파일 다운로드 - fileKey: {}", fileKey);
        
        Optional<StudentSubmissionFile> file = studentSubmissionFileViewRepository.findByFileKey(fileKey);
        
        if (file.isPresent()) {
            // 여기서 실제 파일 다운로드 로직 구현
            // S3나 다른 스토리지에서 파일을 가져오는 로직
            log.info("파일 다운로드 성공 - fileName: {}", file.get().getFileName());
            return new byte[0]; // 실제 구현에서는 파일 데이터 반환
        } else {
            throw new RuntimeException("파일을 찾을 수 없습니다: " + fileKey);
        }
    }

    /**
     * Entity를 ViewDTO로 변환
     */
    private StudentSubmissionFileViewDTO convertToViewDTO(StudentSubmissionFile entity) {
        return StudentSubmissionFileViewDTO.builder()
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
            .fileSizeFormatted(formatFileSize(entity.getFileSize()))
            .uploadDateFormatted(entity.getCreatedAt() != null ? entity.getCreatedAt().format(dateFormatter) : "")
            .fileType(getFileType(entity.getContentType()))
            .fileIcon(getFileIcon(entity.getContentType()))
            .build();
    }

    /**
     * 파일 크기 포맷팅
     */
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes < 1024) return (bytes != null ? bytes : 0) + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 파일 타입 추출
     */
    private String getFileType(String contentType) {
        if (contentType == null) return "unknown";
        if (contentType.contains("pdf")) return "PDF";
        if (contentType.contains("word") || contentType.contains("document")) return "Word";
        if (contentType.contains("excel") || contentType.contains("spreadsheet")) return "Excel";
        if (contentType.contains("powerpoint") || contentType.contains("presentation")) return "PowerPoint";
        if (contentType.contains("image")) return "Image";
        if (contentType.contains("video")) return "Video";
        if (contentType.contains("audio")) return "Audio";
        if (contentType.contains("zip") || contentType.contains("compressed")) return "Archive";
        if (contentType.contains("text")) return "Text";
        return "Other";
    }

    /**
     * 파일 아이콘 결정
     */
    private String getFileIcon(String contentType) {
        if (contentType == null) return "file";
        if (contentType.contains("pdf")) return "file-pdf";
        if (contentType.contains("word") || contentType.contains("document")) return "file-word";
        if (contentType.contains("excel") || contentType.contains("spreadsheet")) return "file-excel";
        if (contentType.contains("powerpoint") || contentType.contains("presentation")) return "file-powerpoint";
        if (contentType.contains("image")) return "file-image";
        if (contentType.contains("video")) return "file-video";
        if (contentType.contains("audio")) return "file-audio";
        if (contentType.contains("zip") || contentType.contains("compressed")) return "file-archive";
        if (contentType.contains("text")) return "file-text";
        return "file";
    }
} 