package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 과목별 세부과목 목록 DTO
 * 평면화된 구조로 과목명과 세부과목 정보를 함께 제공
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectDetailDTO {
    private String subjectName;    // 과목명
    private String subDetailName;  // 세부과목명
    private String subDetailId;    // 세부과목 ID
    private int subDetailActive; // 세부과목 활성화 여부
} 