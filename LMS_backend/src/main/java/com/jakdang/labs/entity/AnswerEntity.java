package com.jakdang.labs.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 학생의 답안 정보를 담는 Entity 클래스
 */
@Entity
@Table(name = "answer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerEntity {
    @Id
    @Column(name = "answerId")
    private String answerId;
    
    @Column(name = "answerText")
    private String answerText;                // 답안 내용

    @Column(name = "answerScore")
    private int answerScore;                  // 답안 점수

    @Column(name = "teacherComment")
    private String teacherComment;            // 교사 코멘트

    @Column(name = "createdAt")
    private LocalDateTime createdAt;          // 생성일시(제출일시)

    @Column(name = "answerGradedAt")
    private LocalDateTime answerGradedAt;     // 채점 완료일시

    @Column(name = "answerGradeUpdatedAt")
    private LocalDateTime answerGradeUpdatedAt; // 채점 수정일시

    @Column(name = "answerActive")
    private int answerActive;                 // 삭제 여부 (1: 활성, 0: 비활성)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "templateQuestionId", referencedColumnName = "templateQuestionId")
    private com.jakdang.labs.entity.TemplateQuestionEntity templateQuestion; // 템플릿 질문

    @Column(name = "memberId")
    private String memberId;                    // 회원 ID FK

    @PrePersist
    protected void onCreate(){
        if (this.answerId == null) {
            this.answerId = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }
} 