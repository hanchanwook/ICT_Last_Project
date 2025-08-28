package com.jakdang.labs.api.cottonCandy.evaluation.dto;

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
public class ResponseStudentEvaluationDetailDTO {
    private int questionTemplateNum; // 템플릿 번호
    private String questionTemplateName; // 템플릿 이름
    private List<EvaluationResponseItem> data;
    private String templateGroupId; // 템플릿 그룹 ID
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationResponseItem {
        private String evalQuestionId; // 평가 항목 ID
        private int score; // 점수 (객관식인 경우)
        private String answerText; // 답변 텍스트 (주관식인 경우)
        private String questionText; // 질문 내용
        private int questionType; // 질문 타입 (0: 주관식, 1: 객관식)
    }
} 