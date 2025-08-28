package com.jakdang.labs.entity;

import java.util.UUID;

import com.jakdang.labs.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evaluationquestion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationQuestionEntity extends BaseEntity {

    @Id
    @Column(length = 100)
    private String evalQuestionId;

    @Column(length = 100, nullable = false)
    private String educationId;

    @Column(nullable = false)
    private int evalQuestionType; // 0-객관식, 1-서술형

    @Column(length = 100, nullable = false)
    private String evalQuestionText;

    @Column(nullable = false)
    @Builder.Default
    private int evalQuestionActive = 0;

    @PrePersist
    public void generateUUID() {
        if (this.evalQuestionId == null) {
            this.evalQuestionId = UUID.randomUUID().toString();
        }
    }
}
