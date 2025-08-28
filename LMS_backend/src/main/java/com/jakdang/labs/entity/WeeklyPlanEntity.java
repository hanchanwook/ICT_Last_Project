package com.jakdang.labs.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Index;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "weeklyplan")
public class WeeklyPlanEntity {
    @Id
    @Column(name = "weeklyPlanId", length = 100, nullable = false)
    private String weeklyPlanId;

    @Column(name = "planId", length = 100, nullable = false)
    private String planId;

    @Column(name = "weekNumber", nullable = false)
    private Integer weekNumber;

    @Column(name = "weekTitle", length = 255, nullable = false)
    private String weekTitle;

    @Column(name = "weekContent", columnDefinition = "TEXT")
    private String weekContent;

    // 단일 값 컬럼 (실제 데이터베이스 스키마에 맞춤)
    @Column(name = "subjectId", length = 100)
    private String subjectId;
    
    @Column(name = "subDetailId", length = 100)
    private String subDetailId;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDate createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDate updatedAt;

    // UUID 자동 생성
    @PrePersist
    public void generateUUID() {
        if (this.weeklyPlanId == null) {
            this.weeklyPlanId = UUID.randomUUID().toString();
        }
    }

    public String getWeeklyPlanId() { return weeklyPlanId; }
    public void setWeeklyPlanId(String weeklyPlanId) { this.weeklyPlanId = weeklyPlanId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }

    public String getWeekTitle() { return weekTitle; }
    public void setWeekTitle(String weekTitle) { this.weekTitle = weekTitle; }

    public String getWeekContent() { return weekContent; }
    public void setWeekContent(String weekContent) { this.weekContent = weekContent; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubDetailId() { return subDetailId; }
    public void setSubDetailId(String subDetailId) { this.subDetailId = subDetailId; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
} 
