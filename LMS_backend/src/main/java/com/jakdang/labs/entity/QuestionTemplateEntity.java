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
@Table(name = "questiontemplate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionTemplateEntity extends BaseEntity {

    @Id
    @Column(length = 100)
    private String templateId;

    @Column(length = 100, nullable = false)
    private String evalQuestionId;

    @Column(length = 50, nullable = false)
    private String questionTemplateName;

    @Column(nullable = false)
    private int questionTemplateNum;

    @Column(nullable = false)
    private int questionNum;

    @Column(nullable = false)
    private int templateActive;

    @Column(length = 100, nullable = false)
    private String educationId;
    
    @PrePersist
    public void generateUUID() {
        if (this.templateId == null) {
            this.templateId = UUID.randomUUID().toString();
        }
    }
}
