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
public class RequestQuestiontemplateDTO {
    private String userId; // 사용자 ID
    private String questionTemplateName; // 템플릿 이름 (String이 더 자연스러움)
    private int questionTemplateNum; // 템플릿 번호 (수정 시 사용)

    private List<QuestionItem> questionList; // 항목 리스트

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionItem {
        private int questionNum; // 항목 번호(순서)
        private String evalQuestionId; // 평가항목 ID
    }
}
