package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 객관식 문제 선택지 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionDTO {
    
    /**
     * 선택지 ID
     */
    private String optId;
    
    /**
     * 선택지 내용
     */
    private String optText;
    
    /**
     * 정답 여부 (0: 오답, 1: 정답)
     */
    private Integer optIsCorrect;
} 