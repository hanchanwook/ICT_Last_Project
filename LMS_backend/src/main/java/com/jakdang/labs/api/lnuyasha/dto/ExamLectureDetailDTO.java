package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamLectureDetailDTO {
    private String id;
    private String title;
    private String code;
    private String period; // 기간 (예: 2024.01.15 - 2024.04.15)
    private String status; // 완료|진행중|예정
    private Integer totalStudents;
    private Integer completedStudents;
    private Integer dropoutStudents;
    private Double averageScore;
    private Integer passRate;
    private Double satisfactionScore; // 만족도 점수
    private Integer totalClasses;
    private Integer completedClasses;
    private String category; // 분야
    private String level; // 수준
    private ScoreDistributionDTO scoreDistribution;
    private List<ExamResultDTO> examResults; // 시험 결과 목록
    private List<StudentInfoDTO> students; // 학생 목록
    private Map<String, List<StudentSubjectDTO>> studentSubjects; // 학생별 과목 성적
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfoDTO {
        private String id; // 학생 고유 ID
        private String name; // 학생명
        private String status; // 상태 (수강중|완료|중도탈락)
        private Double finalScore; // 최종 성적
        private Double assignmentScore; // 과제 성적
        private String grade; // 등급 (탁월|우수|양호|보통|노력)
        private Integer rank; // 석차
        private Integer submittedAssignments; // 제출한 과제 수
        private Integer totalAssignments; // 전체 과제 수
        private Integer lateSubmissions; // 지연 제출 수
        private Double participationScore; // 참여도 점수
        private Integer passRate; // 합격률
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamResultDTO {
        private String examName; // 시험명
        private String subject; // 과목명
        private Double average; // 평균 점수
        private Integer passCount; // 합격자 수
        private Integer totalCount; // 전체 수
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSubjectDTO {
        private String subject; // 과목명
        private Double score; // 점수
        private String grade; // 등급
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