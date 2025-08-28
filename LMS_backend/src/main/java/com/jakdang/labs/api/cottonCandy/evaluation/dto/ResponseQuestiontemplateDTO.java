package com.jakdang.labs.api.cottonCandy.evaluation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseQuestiontemplateDTO {
    private String questionTemplateName;
    private int questionTemplateNum;
    private String createdAt; // 템플릿 생성일

    private List<QuestionItem> questionList; // 항목 리스트

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
        private String createdAt; // 항목 생성일
    }
}
