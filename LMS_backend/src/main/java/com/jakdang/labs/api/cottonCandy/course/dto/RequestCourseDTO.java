package com.jakdang.labs.api.cottonCandy.course.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestCourseDTO {
    private String courseId;
    private String memberId;
    private String educationId;
    private String courseName;
    private int maxCapacity;
    private int minCapacity;
    private String classId;
    private String courseStartDay;
    private String courseEndDay;
    private List<String> courseDays;
    private String startTime;
    private String endTime;
    private String userId;
    
    // 과목 정보 리스트
    private List<SubjectInfo> subjects;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubjectInfo {
        private String subjectId;
        private int subjectTime;    
    }
}
