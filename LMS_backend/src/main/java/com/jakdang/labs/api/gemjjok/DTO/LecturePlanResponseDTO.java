package com.jakdang.labs.api.gemjjok.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import com.jakdang.labs.api.gemjjok.DTO.WeeklyPlanDTO;

@Data
public class LecturePlanResponseDTO {
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
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private Integer planActive;
    private List<WeeklyPlanDTO> weeklyPlan;
} 