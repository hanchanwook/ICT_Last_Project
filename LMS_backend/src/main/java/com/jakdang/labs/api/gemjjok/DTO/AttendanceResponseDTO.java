package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponseDTO {
    private String courseId;
    private String date;
    private int totalStudents;
    private int presentStudents;
    private int absentStudents;
    private double attendanceRate;
} 