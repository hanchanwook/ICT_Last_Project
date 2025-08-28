package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 시험-문제 연결 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateQuestionCreateRequestDTO {
    private String templateId;                    // 시험ID
    private List<QuestionMappingDTO> newQuestions;      // 새로 생성된 문제들
    private List<QuestionMappingDTO> bankQuestions;     // 기존 문제은행 문제들
} 