package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시험 생성 통합 API 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamCreateCompleteRequestDTO {
    
    // 기존 구조 (examData 객체)
    private ExamDataDTO examData;
    private List<NewQuestionDTO> newQuestions;
    private List<BankQuestionDTO> bankQuestions;
    private String userId; // 사용자 ID (선택사항)
    
    // 클라이언트 호환성을 위한 플랫한 구조 필드들
    private String templateName;        // 시험 제목
    private String memberId;            // 강사ID
    private String subGroupId;          // 과정-과목 연결 ID (기존 호환성)
    private String courseId;            // 과정 ID (새로 추가)
    private String subjectId;           // 과목 ID (새로 추가)
    private Integer templateNumber;     // 템플릿 번호
    private Integer templateActive;     // 템플릿 활성화 상태
    private String educationId;         // 교육기관ID
    private Integer templateTime;       // 시험시간
    private LocalDateTime templateOpen; // 시험 시작 시간
    private LocalDateTime templateClose; // 시험 종료 시간
    
    /**
     * examData가 null인 경우 플랫한 필드들로부터 examData 객체를 생성
     */
    public ExamDataDTO getExamData() {
        if (examData == null) {
            examData = ExamDataDTO.builder()
                    .templateName(this.templateName)
                    .memberId(this.memberId)
                    .subGroupId(this.subGroupId)
                    .courseId(this.courseId)
                    .subjectId(this.subjectId)
                    .templateNumber(this.templateNumber)
                    .templateActive(this.templateActive)
                    .educationId(this.educationId)
                    .templateTime(this.templateTime)
                    .templateOpen(this.templateOpen)
                    .templateClose(this.templateClose)
                    .build();
        }
        return examData;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExamDataDTO {
        private String templateName;        // 시험 제목
        private String memberId;            // 강사ID
        private String subGroupId;          // 과정-과목 연결 ID (기존 호환성)
        private String courseId;            // 과정 ID (새로 추가)
        private String subjectId;           // 과목 ID (새로 추가)
        private Integer templateNumber;     // 템플릿 번호
        private Integer templateActive;     // 템플릿 활성화 상태
        private String educationId;         // 교육기관ID
        private Integer templateTime;       // 시험시간
        private LocalDateTime templateOpen; // 시험 시작 시간
        private LocalDateTime templateClose; // 시험 종료 시간
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectedSubDetailDTO {
        private String subDetailId;
        private String subDetailName;
    }
    
                        @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NewQuestionDTO {
        private String questionText;
        private String questionType;
        private String questionAnswer;
        private String correctAnswer;  // 클라이언트 호환성을 위한 필드
        private String explanation;
        private String subDetailId;
        private String subDetailName;  // 클라이언트 호환성을 위한 필드
        private String codeLanguage;
        private Integer questionActive;
        private String memberId;            // 강사ID (필수)
        private String educationId;         // 교육기관ID (필수)
        private Integer templateQuestionScore; // 배점 (templateQuestions 테이블용)
        private Integer score;  // 클라이언트 호환성을 위한 필드
        private List<QuestionOptionDTO> options; // 객관식 선택지 (객관식 문제일 때만)
        
        /**
         * questionAnswer가 null이고 correctAnswer가 있으면 correctAnswer를 questionAnswer로 설정
         */
        public String getQuestionAnswer() {
            if (questionAnswer == null && correctAnswer != null) {
                questionAnswer = correctAnswer;
            }
            return questionAnswer;
        }
        
        /**
         * templateQuestionScore가 null이고 score가 있으면 score를 templateQuestionScore로 설정
         */
        public Integer getTemplateQuestionScore() {
            if (templateQuestionScore == null && score != null) {
                templateQuestionScore = score;
            }
            return templateQuestionScore;
        }
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionOptionDTO {
        private String optText;
        private Integer optIsCorrect;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BankQuestionDTO {
        private String questionId;
        private Integer templateQuestionScore; // 배점 (templateQuestions 테이블용)
        private Integer score;  // 클라이언트 호환성을 위한 필드
        
        /**
         * templateQuestionScore가 null이고 score가 있으면 score를 templateQuestionScore로 설정
         */
        public Integer getTemplateQuestionScore() {
            if (templateQuestionScore == null && score != null) {
                templateQuestionScore = score;
            }
            return templateQuestionScore;
        }
    }
} 