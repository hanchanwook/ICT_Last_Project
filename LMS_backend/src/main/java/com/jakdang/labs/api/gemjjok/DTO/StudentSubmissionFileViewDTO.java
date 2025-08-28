package com.jakdang.labs.api.gemjjok.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubmissionFileViewDTO {
    
    private Long id;
    private String assignmentId;
    private String studentId;
    private String fileKey;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String title;
    private String materialId;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String uploadedBy;
    
    // 상세 모달용 추가 필드
    private String downloadUrl;
    private String fileSizeFormatted;
    private String uploadDateFormatted;
    private String fileType;
    private String fileIcon;
}

// 파일 목록 조회 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class StudentSubmissionFileViewListResponse {
    private String assignmentId;
    private String studentId;
    private Integer totalFiles;
    private Long totalSize;
    private String totalSizeFormatted;
    private java.util.List<StudentSubmissionFileViewDTO> files;
}

// 파일 상세 정보 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class StudentSubmissionFileViewDetailResponse {
    private Long id;
    private String fileName;
    private String fileKey;
    private Long fileSize;
    private String contentType;
    private String title;
    private LocalDateTime uploadDate;
    private String assignmentId;
    private String studentId;
    private String downloadUrl;
    private String fileSizeFormatted;
    private String uploadDateFormatted;
    private String fileType;
    private String fileIcon;
}

// 파일 통계 정보 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class StudentSubmissionFileViewStatsResponse {
    private String assignmentId;
    private String studentId;
    private Integer totalFiles;
    private Long totalSize;
    private String totalSizeFormatted;
    private java.util.List<String> fileTypes;
    private java.util.Map<String, Integer> fileTypeCount;
} 