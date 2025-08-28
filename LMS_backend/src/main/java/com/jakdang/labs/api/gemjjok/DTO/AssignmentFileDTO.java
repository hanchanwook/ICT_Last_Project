package com.jakdang.labs.api.gemjjok.DTO;

import com.jakdang.labs.api.file.dto.FileEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentFileDTO {
    private String fileId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private FileEnum fileType;
    private LocalDate uploadedAt;
    private String uploadedBy;
} 