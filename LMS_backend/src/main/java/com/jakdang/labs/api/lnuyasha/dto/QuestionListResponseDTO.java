package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 통합된 문제 목록 응답 DTO
 * QuestionBankResponseDTO와 QuestionListResponseDTO를 통합
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionListResponseDTO {
    
    private List<QuestionSummaryDTO> questions;  // 문제 목록
    private int totalCount;                      // 전체 문제 수
    private int currentPage;                     // 현재 페이지
    private int limit;                           // 페이지당 항목 수
    private int totalPages;                      // 전체 페이지 수
    private boolean hasNext;                     // 다음 페이지 존재 여부
    private boolean hasPrevious;                 // 이전 페이지 존재 여부
    private String message;                      // 메시지
    private QuestionBankStats stats;             // 통계 정보
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionSummaryDTO {
        private String questionId;           // 문제 ID
        private String questionType;         // 문제 유형
        private String questionText;         // 문제 내용 (미리보기)
        private String questionAnswer;       // 정답 (미리보기)
        private String correctAnswer;        // 정답 (호환성)
        private String explanation;          // 해설 (미리보기)
        private LocalDateTime createdAt;     // 생성일
        private LocalDateTime updatedAt;     // 수정일
        private String memberId;             // 등록한 강사 ID
        private String memberName;           // 강사명
        private String memberEmail;          // 강사 이메일 (호환성)
        private String userId;               // 강사의 user ID (호환성)
        private String subDetailId;          // 세부과목 ID
        private String subDetailName;        // 세부과목명
        private String subjectId;            // 상위 과목 ID
        private String subjectName;          // 상위 과목명
        private String subjectInfo;          // 상위 과목 정보 (호환성)
        private String educationId;          // 학원 ID
        private String educationName;        // 학원명
        private String status;               // 문제 상태 (호환성)
        private int questionActive;          // 활성화 상태
        private int optionCount;             // 보기 개수 (객관식인 경우)
        private String previewText;          // 미리보기 텍스트
        private Integer points;              // 배점
        
        // 객관식 문제의 보기 옵션들 (배열)
        private List<QuestionOption> options;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionOption {
        private String optId;
        private String optText;
        private int optIsCorrect;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionBankStats {
        private int totalQuestions;          // 전체 문제 수
        private int activeQuestions;         // 활성 문제 수
        private int inactiveQuestions;       // 비활성 문제 수
        private int objectiveCount;          // 객관식 문제 수
        private int descriptiveCount;        // 서술형 문제 수
        private int codeCount;               // 코드형 문제 수
    }
} 