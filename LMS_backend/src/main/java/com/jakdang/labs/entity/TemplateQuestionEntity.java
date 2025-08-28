package com.jakdang.labs.entity;

import com.jakdang.labs.global.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "templatequestion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateQuestionEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // UUID 생성
    @Column(name = "templateQuestionId", columnDefinition = "VARCHAR(100)")
    private String templateQuestionId;  // 템플릿 질문 UUID
    
    @Column(name = "templateQuestionScore", columnDefinition = "int")
    private int templateQuestionScore; // 배점점
    
    @Column(name = "questionId", columnDefinition = "VARCHAR(100)")
    private String questionId; // 문제 UUID

    @Column(name = "templateId", columnDefinition = "VARCHAR(100)")
    private String templateId; // 시험 UUID

    @Version
    private Integer version; // 낙관적 락을 위한 버전 필드
}
