package com.jakdang.labs.api.gemjjok.DTO;

import lombok.Data;
import java.util.List;

@Data
public class SyllabusRequestDTO {
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
    private List<WeeklyPlanDTO> weeklyPlan;
} 