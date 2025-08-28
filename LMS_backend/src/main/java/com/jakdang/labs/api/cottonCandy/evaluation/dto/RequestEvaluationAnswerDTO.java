package com.jakdang.labs.api.cottonCandy.evaluation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestEvaluationAnswerDTO {
    private String courseId; // 선택된 과정의 ID
    private String templateGroupId; // 해당 과정 평가 템플릿 그룹 ID(과정+템플릿)
    
    private List<AnswerItem> answerList; // 답변 리스트
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerItem {
        private String evalQuestionId; // 평가항목 ID
        private int score; // 점수
        private String answerText; // 답변 텍스트
    }
}
