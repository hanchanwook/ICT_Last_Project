package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseListResponseDTO {
    private String courseId;
    private String memberId;
    private String educationId;
    private String courseName;
    private String courseCode;
    private int maxCapacity;
    private int minCapacity;
    private String classId;
    private LocalDate courseStartDay;
    private LocalDate courseEndDay;
    private String courseDays;
    private String startTime;
    private String endTime;
    private int courseActive;
    @Builder.Default
    private List<String> materials = new ArrayList<>();
} 