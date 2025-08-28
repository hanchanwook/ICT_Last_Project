package com.jakdang.labs.api.gemjjok.DTO;

import java.time.LocalDate;
import java.util.List;

public class WeeklyPlanDTO {
    private String weeklyPlanId;
    private String planId;
    private Integer weekNumber;
    private String weekTitle;
    private String weekContent;
    private String subjectId;
    private String subDetailId;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    
    // 기본 생성자
    public WeeklyPlanDTO() {}
    
    // Getter 메서드들
    public String getWeeklyPlanId() { return weeklyPlanId; }
    public String getPlanId() { return planId; }
    public Integer getWeekNumber() { return weekNumber; }
    public String getWeekTitle() { return weekTitle; }
    public String getWeekContent() { return weekContent; }
    public String getSubjectId() { return subjectId; }
    public String getSubDetailId() { return subDetailId; }
    public LocalDate getCreatedAt() { return createdAt; }
    public LocalDate getUpdatedAt() { return updatedAt; }
    
    // Setter 메서드들
    public void setWeeklyPlanId(String weeklyPlanId) { this.weeklyPlanId = weeklyPlanId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }
    public void setWeekTitle(String weekTitle) { this.weekTitle = weekTitle; }
    public void setWeekContent(String weekContent) { this.weekContent = weekContent; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public void setSubDetailId(String subDetailId) { this.subDetailId = subDetailId; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
} 