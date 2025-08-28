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
public class CourseMaterialRequest {
    private String fileId;
    private String fileKey;
    private String title;
    private String fileName;
    private Long fileSize;
    
    @NotNull(message = "파일 타입은 필수입니다")
    private FileEnum fileType;
    
    private String memberId;
    private String memberType;
    private boolean s3Uploaded;
    private String s3Url;
} 