package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureDetailDTO {
    private String id;
    private String title;
    private String code;
    private String description;
    private String courseStartDay;
    private String courseEndDay;
    private String year;
    private String status;
    private String instructorName;
    private String instructorId;
    private Integer totalStudents;
    private Integer completedStudents;
    private Integer dropoutStudents;
    private Double averageScore;
    private Integer passRate;
    private Integer totalClasses;
    private Integer completedClasses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StudentInfoDTO> students;
    private List<ClassInfoDTO> classes;
    private ScoreDistributionDTO scoreDistribution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfoDTO {
        private String studentId;
        private String studentName;
        private String status; // 수강중, 완료, 중도탈락
        private Double score;
        private Integer attendanceRate;
        private LocalDateTime enrolledAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInfoDTO {
        private Long classId;
        private String className;
        private LocalDateTime classDate;
        private String status; // 진행완료, 진행중, 예정
        private Integer attendanceCount;
        private String notes;
        private String templateId;
        private String templateName;
        private LocalDateTime templateOpen;
        private LocalDateTime templateClosed;
    }
    
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