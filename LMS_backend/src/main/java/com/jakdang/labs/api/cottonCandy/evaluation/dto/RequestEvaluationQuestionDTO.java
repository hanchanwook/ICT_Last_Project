package com.jakdang.labs.api.cottonCandy.evaluation.dto;

import lombok.Builder;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestEvaluationQuestionDTO {
    private String evalQuestionId;

    private int evalQuestionType;
    private String evalQuestionText;
    private String userId;

}