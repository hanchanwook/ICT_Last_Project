package com.jakdang.labs.api.lnuyasha.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 문제 수정 요청 DTO
 * 문제 수정 시 필요한 정보를 담는 DTO
 * 프론트엔드 요구사항에 맞춰 필드 구성
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionUpdateRequestDTO {
    
    private String questionId;         // 문제 ID (프론트엔드에서 전송)
    private String type;               // 문제 유형 ("객관식", "서술형", "코드형")
    private String question;           // 문제 내용
    private String correctAnswer;      // 정답
    private String codeLanguage;       // 코드 언어 (코드형만)
    private String subject;            // 과목명
    private List<String> options;      // 선택지 배열 (객관식만) - 하위 호환성
    private List<QuestionOptionDTO> questionOptions; // 선택지 DTO 배열 (객관식만) - 새로운 방식
    private String instructorId;       // 수정하는 강사 ID
    
    // 기존 필드들 (하위 호환성 유지)
    private String questionType;       // 문제 유형 (기존)
    private String questionText;       // 문제 내용 (기존)
    private String questionAnswer;     // 정답 (기존)
    private String explanation;        // 해설 (기존)
    private String subDetailId;        // 세부과목 ID (기존)
    private Integer points;            // 배점 (기존)
    
    /**
     * 프론트엔드 요구사항에 맞는 필드 반환
     */
    public String getQuestionTypeForUpdate() {
        return type != null ? type : questionType;
    }
    
    public String getQuestionTextForUpdate() {
        return question != null ? question : questionText;
    }
    
    public String getQuestionAnswerForUpdate() {
        return correctAnswer != null ? correctAnswer : questionAnswer;
    }
} 