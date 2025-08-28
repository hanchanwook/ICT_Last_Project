package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과목 정보 DTO
 * 문제가 있는 과목들의 정보를 담는 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectInfoDTO {
    
    private String subjectId;          // 상위 과목 ID
    private String subjectName;        // 상위 과목명
    private String subjectInfo;        // 상위 과목 정보
    private String subDetailId;        // 세부과목 ID
    private String subDetailName;      // 세부과목명
    private int questionCount;         // 해당 과목의 문제 수
    private String educationId;        // 학원 ID
    private String educationName;      // 학원명
} 