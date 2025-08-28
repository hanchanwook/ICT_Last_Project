package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 통합된 시험 DTO
 * CourseExamDTO, StudentExamDTO, CloseExamResponseDTO, CloseExamRequestDTO, ExamEndResponseDTO, ExamCreateCompleteResponseDTO를 통합
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamDTO {
    
    // 기본 시험 정보
    private String templateId;         // 시험 템플릿 ID
    private String templateName;       // 시험명
    private String subGroupId;         // 소그룹 ID
    private String memberId;           // 강사 ID
    private String memberName;         // 강사 이름
    private String courseId;           // 과정 ID
    private String courseName;         // 과정명
    private String courseCode;         // 과정코드
    
    // 시험 시간 정보
    private LocalDateTime templateOpen;    // 시험 시작 시간
    private LocalDateTime templateClose;   // 시험 종료 시간
    private Integer templateTime;      // 시험 시간 (분)
    private Integer templateActive;    // 시험 활성화 상태
    
    // 시험 상태
    private String status;             // 시험 상태 (available, ongoing, completed, expired)
    private Boolean graded;            // 채점 완료 여부
    
    // 개인 성적 정보 (학생용)
    private Integer myScore;           // 내 점수
    private String grade;              // 등급
    private Integer attempts;          // 응시 횟수
    private Integer maxAttempts;       // 최대 응시 횟수
    private Boolean submitted;         // 시험 제출 여부
    private LocalDateTime submittedAt; // 제출 시간
    private Integer isChecked;         // 성적 확인 여부 (0: 학생 확인 완료, 1: 채점 완료/학생 미확인)
    
    // 통계 정보 (강사용)
    private Double avgScore;           // 평균 점수
    private Double passRate;           // 합격률
    private String examDate;           // 시험 날짜
    private Integer participantCount;  // 참가자 수
    
    // 시험 종료 관련 필드들 (CloseExamResponseDTO, CloseExamRequestDTO, ExamEndResponseDTO에서 통합)
    private Boolean success;           // 시험 종료 성공 여부
    private String message;            // 시험 종료 메시지
    private Integer unsubmittedCount;  // 미제출 학생 수
    private List<String> studentMemberIds; // 학생 ID 목록 (종료 요청용)
    private Integer autoSubmittedCount; // 자동 제출된 학생 수
    private Integer totalStudents;     // 전체 학생 수
    
    // 시험 생성 관련 필드들 (ExamCreateCompleteResponseDTO에서 통합)
    private Integer newQuestions;      // 새로 생성된 문제 수
    private Integer bankQuestions;     // 문제은행에서 가져온 문제 수
    private Integer totalQuestions;    // 전체 문제 수 (시험 생성용)
} 