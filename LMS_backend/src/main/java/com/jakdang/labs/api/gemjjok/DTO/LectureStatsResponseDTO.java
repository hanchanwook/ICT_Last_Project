package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureStatsResponseDTO {
    private int totalCourses;
    private int completedCourses;
    private int ongoingCourses;
    private int upcomingCourses;
} 