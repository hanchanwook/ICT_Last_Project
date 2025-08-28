package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricItemDTO {
    private String rubricItemId;
    private String itemTitle; // 항목명 (예: 정확성, 표현력, 창의성)
    private Integer maxScore; // 배점
    private String description; // 상세 기준
    private Integer itemOrder; // 항목 순서
} 