package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 세부과목 정보와 문제 목록을 함께 담는 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubDetailWithQuestionsDTO {
    
    private String subDetailId;           // 세부과목 ID
    private String subDetailName;         // 세부과목명
    private String subDetailInfo;         // 세부과목 설명
    private Integer subDetailActive;      // 활성화 상태
    private String subjectId;             // 상위 과목 ID
    private String subjectName;           // 상위 과목명
    private String educationId;           // 교육기관 ID
    private List<QuestionInfoDTO> questions; // 해당 세부과목의 문제 목록
    private Integer questionCount;        // 문제 개수
    
    /**
     * 문제 정보 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionInfoDTO {
        private String questionId;        // 문제 ID
        private String questionText;      // 문제 내용
        private String questionType;      // 문제 유형 (객관식, 주관식, 서술형)
        private String questionAnswer;    // 정답
        private String explanation;       // 해설
        private String codeLanguage;      // 코드 언어 (코드형 문제인 경우)
        private String memberId;          // 등록한 강사 ID
        private String memberName;        // 등록한 강사명
        private Integer questionActive;   // 문제 활성화 상태
        private String createdAt;         // 생성일시
    }
} 