package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureHistoryDTO {
    private String id;
    private String title;
    private String code;
    private String courseStartDay;
    private String courseEndDay;
    private String year;
    private String status; // 완료, 진행중, 예정
    private Integer totalStudents;
    private Integer completedStudents;
    private Integer dropoutStudents;
    private Double averageScore;
    private Integer passRate;
    private Integer totalClasses;
    private Integer completedClasses;
    private ScoreDistributionDTO scoreDistribution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDistributionDTO {
        private Integer outstanding;    // 90점 이상 - 탁월
        private Integer excellent;     // 80-89점 - 우수
        private Integer good;          // 70-79점 - 양호
        private Integer average;       // 60-69점 - 보통
        private Integer needsImprovement; // 60점 미만 - 노력
    }
} 