package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시험 열기/닫기 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUpdateRequestDTO {
    
    /**
     * 시험 열기 시간 (ISO 8601 형식)
     */
    private LocalDateTime templateOpen;
    
    /**
     * 시험 닫기 시간 (ISO 8601 형식)
     */
    private LocalDateTime templateClose;
} 