package com.jakdang.labs.api.jaegyeom.registration.dto;

import lombok.Data;

@Data
public class GetCourseDto {
    private String courseId;
    private String courseName;
    private String createdAt;
    private String updatedAt;
    private String classId;
    private String courseActive;
    private String courseCode;
    private String courseDays;
    private String courseEndDay;
    private String courseStartDay;
    private String educationId;
    private String startTime;
    private String endTime;
    private String maxCapacity;
    private String minCapacity;
    private String memberId;
}
