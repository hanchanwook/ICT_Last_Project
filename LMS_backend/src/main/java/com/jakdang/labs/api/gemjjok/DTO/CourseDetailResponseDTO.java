package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDetailResponseDTO {
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
    private Instant createdAt;
    private Instant updatedAt;
    @Builder.Default
    private List<String> materials = new ArrayList<>();
    @Builder.Default
    private List<StudentResponseDTO> students = new ArrayList<>();
    private int currentStudentCount;
    private double attendanceRate;
} 