package com.jakdang.labs.api.gemjjok.DTO;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LecturePlanListResponseDTO {
    private String planId;
    private String courseId;
    private String planTitle;
    private String courseGoal;
    private Integer weekCount;
    private Byte isLocked;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private Integer planActive;
} 