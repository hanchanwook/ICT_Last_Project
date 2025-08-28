package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 통합된 답안 정보 DTO
 * AnswerDTO, StudentExamAnswerDTO.AnswerDetail, StudentAnswerResponseDTO, GradeConfirmationDTO, ExamSubmissionDTO, ExamScoreDTO, GradingResultDTO, QuestionGradingDetailDTO를 통합
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerDTO {

    // 기본 답안 정보
    private String answerId;           // 답안 ID
    private String questionId;         // 문제 ID
    private String memberId;           // 회원 ID
    private String answerContent;      // 답안 내용
    private String studentAnswer;      // 학생 답안 (호환성)

    // 채점 결과
    private String answerResult;       // 답안 결과 (정답/오답)
    private Boolean isCorrect;         // 정답 여부 (호환성)
    private Integer answerScore;       // 답안 점수
    private Integer score;             // 점수 (호환성)
    private String comment;            // 코멘트

    // 문제 정보 (조인용)
    private String questionText;       // 문제 내용
    private String questionType;       // 문제 유형
    private Integer questionScore;     // 문제 배점
    private String correctAnswer;      // 정답

    // 회원 정보 (조인용)
    private String memberName;         // 회원 이름
    private String memberEmail;        // 회원 이메일

    // 시간 정보
    private LocalDateTime answerDate;  // 답안 작성일
    private LocalDateTime createdAt;   // 생성일시
    private LocalDateTime updatedAt;   // 수정일시
    private LocalDateTime submittedAt; // 제출 시간
    private LocalDateTime gradedAt;    // 채점 시간

    // StudentAnswerResponseDTO에서 통합된 필드들
    private String templateId;         // 템플릿 ID
    private Map<String, String> answers; // 답안 맵 (문제ID -> 답안내용)
    private Integer timeSpent;         // 소요 시간 (분)
    private Integer answeredQuestions; // 답안한 문제 수
    private Integer totalQuestions;    // 전체 문제 수

    // GradeConfirmationDTO에서 통합된 필드들
    private Integer isChecked;         // 확인 상태 (0: 학생 확인 완료, 1: 채점 완료/학생 미확인)
    private Integer graded;            // 채점 완료 여부 (0: 채점 완료, 1: 미채점)

    // ExamSubmissionDTO에서 통합된 필드들
    private String studentName;        // 학생 이름 (호환성)
    private String grade;              // 등급
    private String status;             // 상태 ("제출완료", "채점완료", "미제출")

    // ExamScoreDTO에서 통합된 필드들
    private String totalComment;       // 전체 피드백

    // GradingResultDTO에서 통합된 필드들
    private String scoreStudentId;     // 점수 학생 ID

    // QuestionGradingDetailDTO에서 통합된 필드들
    private String questionGradingId;  // 문제별 채점 ID
    
    // confirm-grade API용 필드들
    private String courseId;           // 과정 ID
    private String userId;             // 사용자 ID (id 컬럼)
} 