package com.jakdang.labs.api.gemjjok.DTO;

import java.util.List;

public class LecturePlanRequestDTO {
    private String planId;
    private String courseId;
    private String planTitle;
    private String planContent;
    private String courseGoal;
    private String learningMethod;
    private String evaluationMethod;
    private String textbook;
    private Integer weekCount;
    private String assignmentPolicy;
    private String latePolicy;
    private String etcNote;
    private Byte isLocked;
    private Integer planActive;
    private List<WeeklyPlanRequest> weeklyPlan;
    
    // 기본 생성자
    public LecturePlanRequestDTO() {}
    
    // Getter 메서드들
    public String getPlanId() { return planId; }
    public String getCourseId() { return courseId; }
    public String getPlanTitle() { return planTitle; }
    public String getPlanContent() { return planContent; }
    public String getCourseGoal() { return courseGoal; }
    public String getLearningMethod() { return learningMethod; }
    public String getEvaluationMethod() { return evaluationMethod; }
    public String getTextbook() { return textbook; }
    public Integer getWeekCount() { return weekCount; }
    public String getAssignmentPolicy() { return assignmentPolicy; }
    public String getLatePolicy() { return latePolicy; }
    public String getEtcNote() { return etcNote; }
    public Byte getIsLocked() { return isLocked; }
    public Integer getPlanActive() { return planActive; }
    public List<WeeklyPlanRequest> getWeeklyPlan() { return weeklyPlan; }
    
    // Setter 메서드들
    public void setPlanId(String planId) { this.planId = planId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setPlanTitle(String planTitle) { this.planTitle = planTitle; }
    public void setPlanContent(String planContent) { this.planContent = planContent; }
    public void setCourseGoal(String courseGoal) { this.courseGoal = courseGoal; }
    public void setLearningMethod(String learningMethod) { this.learningMethod = learningMethod; }
    public void setEvaluationMethod(String evaluationMethod) { this.evaluationMethod = evaluationMethod; }
    public void setTextbook(String textbook) { this.textbook = textbook; }
    public void setWeekCount(Integer weekCount) { this.weekCount = weekCount; }
    public void setAssignmentPolicy(String assignmentPolicy) { this.assignmentPolicy = assignmentPolicy; }
    public void setLatePolicy(String latePolicy) { this.latePolicy = latePolicy; }
    public void setEtcNote(String etcNote) { this.etcNote = etcNote; }
    public void setIsLocked(Byte isLocked) { this.isLocked = isLocked; }
    public void setPlanActive(Integer planActive) { this.planActive = planActive; }
    public void setWeeklyPlan(List<WeeklyPlanRequest> weeklyPlan) { this.weeklyPlan = weeklyPlan; }
} 