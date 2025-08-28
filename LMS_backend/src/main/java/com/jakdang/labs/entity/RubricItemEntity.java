package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "rubricitem")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricItemEntity {
    @Id
    @Column(name = "rubricItemId", length = 100, nullable = false)
    private String rubricItemId;

    @Column(name = "assignmentId", length = 100, nullable = false)
    private String assignmentId; // 과제 ID

    @Column(name = "itemTitle", length = 100, nullable = false)
    private String itemTitle; // 항목명 (예: 정확성, 표현력, 창의성)

    @Column(name = "maxScore", nullable = false)
    private Integer maxScore; // 배점

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 상세 기준

    @Column(name = "itemOrder", nullable = false)
    private Integer itemOrder; // 항목 순서

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDate createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDate updatedAt;

    // UUID 자동 생성
    @PrePersist
    public void generateUUID() {
        if (this.rubricItemId == null) {
            this.rubricItemId = UUID.randomUUID().toString();
        }
    }
} 