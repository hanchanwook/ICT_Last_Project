package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 과정에 연결된 세부과목 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSubDetailDTO {
    private String subDetailId;      // 세부과목 고유 ID
    private String subDetailName;    // 세부과목명
    private int subDetailActive;     // 세부과목 활성화 상태 (0: 활성, 1: 비활성)
} 