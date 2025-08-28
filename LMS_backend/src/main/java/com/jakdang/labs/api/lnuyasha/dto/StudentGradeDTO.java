package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeDTO {
    private String courseId;
    private String courseName;
    private String courseCode;
    private String instructor;
    private List<ExamGradeDTO> exams;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamGradeDTO {
        private String examId;
        private String examName;
        private String examDate;
        private Double totalScore;
        private Double maxScore;
        private String grade;
        private QuestionTypesDTO questionTypes;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionTypesDTO {
        private ScoreDTO objective;
        private ScoreDTO subjective;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDTO {
        private Double score;
        private Double maxScore;
    }
} 