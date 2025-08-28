package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시험-문제 연결 매핑 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionMappingDTO {
    private String questionId;  // 문제 ID
    private Integer score;      // 배점
} 