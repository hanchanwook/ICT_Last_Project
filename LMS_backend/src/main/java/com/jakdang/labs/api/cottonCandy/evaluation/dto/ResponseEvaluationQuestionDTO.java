package com.jakdang.labs.api.cottonCandy.evaluation.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.jakdang.labs.entity.EvaluationQuestionEntity;

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
public class ResponseEvaluationQuestionDTO {
    private String evalQuestionId;
    private int evalQuestionType;
    private String evalQuestionText;
    private Instant createdAt;

    private int useEvalQuestion; // 평가 질문 사용 횟수
    private LocalDate templateCreatedAt; // 최근 사용 템플릿 생성일

    public static ResponseEvaluationQuestionDTO fromEntity(EvaluationQuestionEntity entity) {
        return ResponseEvaluationQuestionDTO.builder()
                .evalQuestionId(entity.getEvalQuestionId())
                .evalQuestionType(entity.getEvalQuestionType())
                .evalQuestionText(entity.getEvalQuestionText())
                .build();
    }
}
