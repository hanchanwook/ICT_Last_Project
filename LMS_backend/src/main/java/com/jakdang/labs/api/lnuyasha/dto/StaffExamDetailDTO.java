package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 강사용 시험별 상세 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffExamDetailDTO {
    
    private String examId;           // 시험 ID
    private String examName;         // 시험명
    private Double avgScore;         // 평균 점수
    private Double passRate;         // 합격률 (%)
    private Integer participantCount; // 응시자 수
    private Map<String, Integer> gradeDistribution; // 등급별 분포 (A, B, C, D, F)
    private String examStatus;       // 시험 상태 (BEFORE_START, IN_PROGRESS, ENDED, NO_SCHEDULE)
    private String examStatusDescription; // 시험 상태 설명
    private Integer questionCount;   // 문제 개수
} 