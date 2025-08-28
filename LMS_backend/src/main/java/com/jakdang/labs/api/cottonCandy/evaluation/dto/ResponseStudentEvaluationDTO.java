package com.jakdang.labs.api.cottonCandy.evaluation.dto;

import java.time.LocalDate;
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
public class ResponseStudentEvaluationDTO {
    
        // 과정 평가 정보
        private String templateGroupId; // 해당 과정 평가 템플릿 그룹 ID(과정+템플릿)
        private LocalDate openDate; // 평가 시작일
        private LocalDate closeDate; // 평가 종료일
        private String questionTemplateName; // 템플릿 이름
        private int questionTemplateNum; // 템플릿 번호
    
        private boolean response; // 평가 답변 여부
        
        // 답변 등록 응답용 필드들
        private String memberId; // 회원 ID
        private String memberName; // 회원 이름
        private String courseId; // 과정 ID
        private String courseName; // 과정 이름
        private int answerCount; // 등록된 답변 개수
        
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
        }
        
}
