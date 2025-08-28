package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시험 템플릿 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponseDTO {
    private String templateId;           // 시험ID
    private String templateName;         // 시험명
    private Integer templateTime;        // 시험시간
    private LocalDateTime templateOpen;  // 시험 시작 시간
    private LocalDateTime templateClose; // 시험 종료 시간
    private Integer templateNumber;      // 문제수
    private Integer templateActive;      // 템플릿 활성화 상태
    private String memberId;             // 강사ID
    private String subGroupId;           // 과정+과목ID
    private String educationId;          // 교육기관ID
    private String courseId;             // 과정ID
    private String courseName;           // 과정명
    private String courseCode;           // 과정코드
    private LocalDateTime createdAt;     // 생성일시
    private LocalDateTime updatedAt;     // 수정일시
    private List<QuestionDTO> questions;  // 연결된 문제 목록
    private Integer totalQuestions;      // 총 문제 수
    
    // 학생 응시 현황 정보
    private Integer participants;        // 참여자 수
    private Integer submittedCount;      // 제출한 학생 수
    private Integer totalStudents;       // 전체 학생 수
    private String submissionRate;       // 제출률 (예: "80%")
    private Integer gradedCount;         // 채점 완료된 답안 수
    private String gradingRate;          // 채점률 (예: "60%")
    private Double averageScore;         // 평균 점수
    private Integer unsubmittedCount;    // 미제출 학생 수
    private Integer students;            // 해당 과정에 등록된 학생 수
    private Integer participantCount;    // 실제 수강생 수 (totalStudents와 동일)
    private Integer waitingForGradingCount; // 채점 대기 학생 수
} 