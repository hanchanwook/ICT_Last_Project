package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 시험 템플릿 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateCreateRequestDTO {
    private String templateName;       // 시험명 (templateName)
    private Integer templateTime;      // 시험시간 (templateTime)
    private LocalDateTime templateOpen; // 시험 시작 시간 (templateOpen)
    private LocalDateTime templateClose; // 시험 종료 시간 (templateClose)
    private Integer templateNumber;    // 문제수 (templateNumber)
    private Integer templateActive;    // 템플릿 활성화 상태 (templateActive)
    private String memberId;           // 강사ID (memberId)
    private String subGroupId;         // 과정+과목ID (subGroupId)
    private String educationId;        // 교육기관ID (educationId)
} 