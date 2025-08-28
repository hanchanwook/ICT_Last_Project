package com.jakdang.labs.api.gemjjok.DTO;

import com.jakdang.labs.api.file.dto.FileEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataRequest {
    private String key;
    private String name;
    private Long size;
    private FileEnum type;
    private String ownerId;
    private String memberType;
    private String userId;
} 