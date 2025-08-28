package com.jakdang.labs.entity;

import java.time.Instant;

import com.jakdang.labs.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questionoption")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOptEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)     // UUID 생성
    @Column(name = "optId", columnDefinition = "VARCHAR(100)")
    private String optId;

    @Column(name = "optText", columnDefinition = "VARCHAR(50)")
    private String optText; // 보기 내용

    @Column(name = "optIsCorrect", columnDefinition = "int default 0")
    private int optIsCorrect; // 보기 정답 여부 (0: 오답, 1: 정답)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionId", referencedColumnName = "questionId")
    private QuestionEntity question; // 문제 엔티티와의 관계 (객관식 문제일 때만 사용)

    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
