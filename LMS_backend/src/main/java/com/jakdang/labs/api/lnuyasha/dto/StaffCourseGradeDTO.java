package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.List;

/**
 * 강사용 과정 성적 통계 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffCourseGradeDTO {
    
    private Double avgScore;                    // 전체 평균 점수
    private Double passRate;                    // 합격률 (%)
    private Integer examCount;                  // 시험 개수
    private Integer studentCount;               // 수강생 수
    private Map<String, Integer> gradeDistribution; // 등급별 분포 (A, B, C, D, F)
    private List<StaffExamDetailDTO> examDetails;    // 시험별 상세 정보
} 