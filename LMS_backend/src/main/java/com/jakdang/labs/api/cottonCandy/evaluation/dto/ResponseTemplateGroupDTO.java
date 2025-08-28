package com.jakdang.labs.api.cottonCandy.evaluation.dto;

import java.time.LocalDate;
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
public class ResponseTemplateGroupDTO {
    private String templateGroupId;
    private String courseId;
    private String courseCode; // 과정 코드 추가
    private String memberId;
    private String memberName; // 강사 이름 추가
    private String educationId;
    private String courseName;
    private int maxCapacity;
    private int studentCount; // 해당 강의를 듣는 학생 수 추가
    private LocalDate courseStartDay;
    private LocalDate courseEndDay;
    private List<QuestionItem> questionList; // 질문 리스트
    private List<AnswerItem> answerList; // 답변 리스트
    
    // 과정별 여러 평가 템플릿을 포함하기 위한 리스트
    private List<EvaluationTemplate> evaluationTemplates;
    

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationTemplate {
        private String templateGroupId; // 템플릿 그룹 ID
        private LocalDate openDate; // 평가 시작일
        private LocalDate closeDate; // 평가 종료일
        private int questionTemplateNum; // 템플릿 번호
        private String questionTemplateName; // 템플릿 이름
        private List<QuestionItem> questionList; // 해당 템플릿의 질문 리스트
        private List<AnswerItem> answerList; // 해당 템플릿의 답변 리스트
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionItem {
        private int questionNum; // 항목 번호(순서)
        private String evalQuestionId; // 평가항목 ID
        private String evalQuestionText; // 항목 내용
        private int evalQuestionType; // 항목 타입
        private String questionTemplateName; // 템플릿 이름
        private int questionTemplateNum; // 템플릿 번호
        private String createdAt; // 항목 생성일
        private LocalDate openDate; // 평가 시작일
        private LocalDate closeDate; // 평가 종료일
        private String templateGroupId; // 템플릿 그룹 ID
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerItem {
        private String responseId; // 답변 ID
        private String memberId; // 학생 ID
        private String memberName; // 학생 이름
        private String evalQuestionId; // 평가항목 ID
        private int questionNum; // 항목 번호 (몇번째 질문인지)
        private String evalQuestionText; // 평가항목 내용
        private int evalQuestionType; // 평가항목 타입 (0-객관식, 1-서술형)
        private String answerText; // 답변 내용
        private int score; // 점수 (객관식인 경우)
        private String createdAt; // 답변 생성일
        private String templateGroupId; // 템플릿 그룹 ID
    }
    

}
