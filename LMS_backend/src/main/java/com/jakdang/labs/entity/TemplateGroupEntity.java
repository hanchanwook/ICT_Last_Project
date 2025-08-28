package com.jakdang.labs.entity;

import java.time.LocalDate;
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
@Table(name = "templategroup")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateGroupEntity extends BaseEntity {

    @Id
    @Column(length = 100)
    private String templateGroupId;


    @Column(length = 100, nullable = false)
    private String courseId;

    @Column(nullable = false)
    private LocalDate openDate; // 평가 시작일
    
    @Column(nullable = false)
    private LocalDate closeDate; // 평가 종료일

    @Column(nullable = false)
    private int questionTemplateNum;

    @PrePersist
    public void generateUUID() {
        if (this.templateGroupId == null) {
            this.templateGroupId = UUID.randomUUID().toString();
        }
    }
}