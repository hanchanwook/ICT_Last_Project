package com.jakdang.labs.api.gemjjok.DTO;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureFileDTO {
    private Long id;
    private String planId;
    private String fileId;
    private String fileKey;
    private LocalDateTime createdAt;
} 