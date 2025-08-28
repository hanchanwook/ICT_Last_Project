package com.jakdang.labs.api.cottonCandy.course.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseCourseDTO {
    private String courseId;
    private String memberName;
    private String educationId;
    private String courseName;
    private String courseCode;
    private int maxCapacity;
    private int minCapacity;
    private String classNumber;
    private String classId;
    private LocalDate courseStartDay;
    private LocalDate courseEndDay;
    private String courseDays;
    private String startTime;
    private String endTime;
    private LocalDate createdAt;

    private int studentCount;


    private List<SubjectList> subjects;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubjectList {
        private String subjectId;
        private String subjectName;
        private String subjectInfo;
        private int subjectTime;
    }
}
