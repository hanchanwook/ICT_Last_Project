package com.jakdang.labs.api.gemjjok.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubmissionFileDTO {
    
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
}

// 파일 업로드 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class StudentSubmissionFileUploadRequest {
    private String fileKey;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String title;
} 