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
@Table(name = "evaluationresponse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResponseEntity extends BaseEntity {

    @Id
    @Column(length = 100)
    private String evalResultId;

    @Column(length = 100, nullable = false)
    private String templateGroupId;

    @Column(length = 100, nullable = false)
    private String memberId;

    @Column(length = 100, nullable = false)
    private String evalQuestionId;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answerText;
    
    @Column(length = 100, nullable = false)
    private String educationId;

    @PrePersist
    public void generateUUID() {
        if (this.evalResultId == null) {
            this.evalResultId = UUID.randomUUID().toString();
        }
    }
}

