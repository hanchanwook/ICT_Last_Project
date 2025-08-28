package com.jakdang.labs.api.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageThumbnailDTO {
    private String url;
    private String key;
    private Long width;
    private Long height;
} 