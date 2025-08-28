package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 통합된 문제 정보 DTO
 * ExamQuestionDTO, TemplateQuestionDTO, QuestionDetailResponseDTO, QuestionStatsDTO, ExamQuestionStatsDTO를 통합
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO {

    // 기본 문제 정보
    private String questionId;           // 문제 ID
    private String questionText;         // 문제 내용
    private String questionType;         // 문제 유형 (객관식, 서술형, 코드형)
    private String questionAnswer;       // 정답
    private String explanation;          // 해설
    private Integer questionScore;       // 문제 배점
    private String questionActive;       // 문제 활성화 상태
    
    // 문제 옵션 (객관식용)
    private List<QuestionOptionDTO> options;
    
    // 과목 정보
    private String subDetailId;          // 세부과목 ID
    private String subDetailName;        // 세부과목명
    private String subjectId;            // 과목 ID
    private String subjectName;          // 과목명
    
    // 작성자 정보
    private String instructorId;         // 강사 ID
    private String instructorName;       // 강사명
    private String educationId;          // 교육기관 ID
    
    // 코드형 문제 관련
    private String codeLanguage;         // 프로그래밍 언어
    
    // 템플릿 연결 정보
    private String templateId;           // 템플릿 ID
    private Integer templateQuestionScore; // 템플릿에서의 문제 배점
    
    // 시간 정보
    private LocalDateTime createdAt;     // 생성일시
    private LocalDateTime updatedAt;     // 수정일시
    
    // 문제 통계 정보 (QuestionStatsDTO에서 통합)
    private Integer questionNumber;      // 문제 번호
    private Integer correctCount;        // 정답자 수
    private Integer incorrectCount;      // 오답자 수
    private Integer noAnswerCount;       // 미응답자 수
    private Double correctRate;          // 정답률 (%)
    
    // 시험별 통계 정보 (ExamQuestionStatsDTO에서 통합)
    private String examId;               // 시험 ID
    private String examName;             // 시험명
    private List<QuestionDTO> questionStats; // 문제별 통계 목록 (재귀적 구조)
    private Integer totalQuestions;      // 총 문제 수
    private Double avgCorrectRate;       // 평균 정답률 (%)
    
    // 템플릿 관련 필드 (TemplateService에서 사용)
    private String templateQuestionId;   // 템플릿 문제 ID
    
    // 학생 답안 관련 필드 (시험 결과 조회용)
    private String studentAnswer;        // 학생 답안
    private Double studentScore;         // 학생이 받은 점수
    private String correctAnswer;        // 정답 (객관식, 참거짓의 경우)
    private Boolean isCorrect;           // 정답 여부 (객관식, 참거짓의 경우)
    private Integer isChecked;           // 성적 확인 여부 (scorestudent 테이블의 isChecked 값)
} 