package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureStatisticsDTO {
    private Integer totalLectures;           // 총 강의 수
    private Integer completedLectures;       // 완료된 강의 수
    private Integer totalStudents;           // 총 수강생 수
    private Double overallAverageScore;      // 전체 평균 성적
} 