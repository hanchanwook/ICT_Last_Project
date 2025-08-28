package com.jakdang.labs.api.gemjjok.DTO;

import com.jakdang.labs.api.file.dto.FileEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentMaterialUploadRequest {
    private String fileId;        // File_Service에서 반환된 파일 ID
    private String fileKey;       // S3 파일 키
    private String fileName;      // 파일명
    private String title;         // 과제 자료 제목
    private Long fileSize;        // 파일 크기
    
    @NotNull(message = "파일 타입은 필수입니다")
    private FileEnum fileType;    // 파일 타입 (image, file, audio, video)
    
    private String thumbnailUrl;  // 썸네일 URL
    private String memberId;      // 업로더 ID
    private String memberType;    // 업로더 타입
} 