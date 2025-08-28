package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCourseResponseDTO {
    private String courseId;
    private String courseCode;
    private String courseName;
    private int maxCapacity;
    private int minCapacity;
    private String classId;
    private String classCode; // 강의실 코드 (예: A101)
    private LocalDate courseStartDay;
    private LocalDate courseEndDay;
    private String courseDays;
    private String startTime;
    private String endTime;
    private int courseActive;
    private String memberId; // 강사 ID
    private String educationId;
    private Integer materialsCount; // 강의 자료 개수
} 